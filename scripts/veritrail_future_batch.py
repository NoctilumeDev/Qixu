"""Sealed real future-round probe. Requires an exact built candidate and a dedicated test DB. No implicit resets."""
from __future__ import annotations
import hashlib,json,os,re,socket,subprocess,sys,time,uuid
from pathlib import Path
from datetime import datetime,timedelta,timezone
import urllib.request,urllib.error
from veritrail.acceptance_plan import observation_spec_digest,seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'scripts'))
from verify_allocation import verify

def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
def write(path,value):
    with path.open('x',encoding='utf-8',newline='\n') as stream:json.dump(value,stream,ensure_ascii=False,sort_keys=True,indent=2);stream.write('\n')
def utc():return datetime.now(timezone.utc).isoformat()
def iso(dt):return dt.astimezone(timezone.utc).isoformat().replace('+00:00','Z')
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

if git('status','--porcelain'):raise RuntimeError('Refuse dirty source')
source=git('rev-parse','HEAD');jar=ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar'
import argparse,urllib.parse
parser=argparse.ArgumentParser(description='Real future-round observation on a dedicated test schema. Never resets or kills shared services.')
parser.add_argument('--java',default=str(Path(os.environ.get('JAVA_HOME',''))/'bin/java') if os.environ.get('JAVA_HOME') else 'java')
parser.add_argument('--node',default=os.environ.get('QIXU_NODE','node'))
parser.add_argument('--mysql',default='mysql')
parser.add_argument('--proxy',default=os.environ.get('QIXU_RANDOM_PROXY',''))
args=parser.parse_args()
url=os.environ.get('QIXU_TEST_DB_URL','');parsed=urllib.parse.urlparse(url.removeprefix('jdbc:'))
if parsed.scheme!='mysql' or parsed.path not in ('/qixu_test','/qixu_ci') or not parsed.hostname or parsed.username or parsed.password or not os.environ.get('QIXU_TEST_DB_PASSWORD'):
    raise RuntimeError('Only explicitly supplied dedicated qixu_test/qixu_ci credentials are accepted')
db={'url':url,'username':os.environ.get('QIXU_TEST_DB_USER','qixu_test_app'),'password':os.environ['QIXU_TEST_DB_PASSWORD']}

with socket.socket() as probe:probe.bind(('127.0.0.1',6967))
identity='m3-live-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True)
coordinate={'collector_sha256':sha(Path(__file__)), 'source_sha':source,'jar_sha256':sha(jar),'stage':'m3-fixed-future-batch','collector':'qixu-live-observation/0.2'}
spec={'id':'live-batch','contract':{'id':'qixu-live-batch','version':'0.2'},'evidence_type':'qixu.live.batch','coordinates':coordinate,'projections':['source_sha','source_clean','jar_sha256','requests','batch','packet','database','reproduction','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
def assertion(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'live','path':path},'operator':'eq','right':value}
assertions=[assertion('exact-source','/facts/source_sha',source),assertion('clean-source','/facts/source_clean',True),assertion('installed-jar','/facts/jar_sha256',coordinate['jar_sha256']),assertion('full-result','/facts/database/formal_results',1),assertion('all-outcomes','/facts/database/outcomes',2),assertion('offers','/facts/database/offers',2),assertion('all-notifications','/facts/database/result_notices',2),assertion('independent-reproduction','/facts/reproduction/independent_bytes_equal',True),assertion('actual-bls','/facts/reproduction/signature_verified',True),assertion('maximum','/facts/reproduction/maximum',2),assertion('owned-runtime-stopped','/facts/cleanup/stopped',True)]
plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m3-live','version':2,'subject':{'id':'qixu-m3-live','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does this exact installed candidate freeze before a fixed future beacon, publish one full batch and permit independent public-byte reproduction with real BLS?','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:live-adapter','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'live','observation_spec_id':spec['id'],'cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'live','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':420},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu live batch','consumers':['m3-native-stage']},'reproduction_steps':['Use the exact clean source and jar with a schema-scoped qixu demo DB and fixed official verifier dependencies.','Publish explicit future close/freeze/round/result deadlines before submitting two hard-constrained applicants.','Freeze before the fixed round; fetch that exact round, publish, download the public packet and independently verify all bytes/BLS.'],'cleanup_steps':['Stop only the java child created and retained by this probe; preserve other ports and shared MySQL.','Retain demonstration batch/history and the immutable packet and Bundle.']})
write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coordinate)
facts={'source_sha':source,'source_clean':True,'jar_sha256':coordinate['jar_sha256'],'requests':[],'boundary':'ONE_ACTUAL_FUTURE_BATCH_NOT_FAULT_RECOVERY_CAPACITY_OR_UI'}
env=dict(os.environ,SPRING_PROFILES_ACTIVE='demo',QIXU_DB_URL=db['url'],QIXU_DB_USERNAME=db['username'],QIXU_DB_PASSWORD=db['password'],QIXU_PORT='6967',QIXU_COOKIE_SECURE='false',QIXU_TASKS_ENABLED='false',QIXU_NODE=args.node,QIXU_RANDOM_VERIFIER=str(ROOT/'randomness/verify.mjs'),QIXU_RANDOM_PROXY=args.proxy)
execution='COMPLETED';child=None;owner={};base='http://127.0.0.1:6967';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def request(method,path,body=None,token=None,key=None):
    headers={}
    if body is not None:headers['Content-Type']='application/json'
    if token:headers['Authorization']='Bearer '+token
    if key:headers['Idempotency-Key']=key
    req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),headers=headers,method=method)
    try:
        with opener.open(req,timeout=20) as response:status=response.status;value=json.load(response)
    except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
    facts['requests'].append({'method':method,'path':path,'status':status})
    if status!=200:raise RuntimeError('HTTP '+str(status)+' '+str(value.get('error',{}).get('code','')))
    return value['data']
def login(name):return request('POST','/api/v1/auth/login',{'username':name,'password':'qixu-demo','mode':'BEARER'})['token']
def wait_until(instant):
    while datetime.now(timezone.utc)<instant:
        if child.poll() is not None:raise RuntimeError('Owned JVM stopped early')
        time.sleep(min(1.0,max(0.01,(instant-datetime.now(timezone.utc)).total_seconds())))

try:
    log=(out/'runtime.log').open('wb');child=subprocess.Popen([args.java,'-jar',str(jar)],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
    owner={'processId':child.pid,'sourceSha':source,'jarSha256':coordinate['jar_sha256'],'executable':args.java,'jar':str(jar),'port':6967,'startedUtc':utc()};write(out/'runtime-owner.json',owner)
    deadline=time.monotonic()+40
    while True:
        if child.poll() is not None:raise RuntimeError('Application startup failed')
        try:
            with opener.open(base+'/api/health',timeout=2) as response:
                if response.status==200:break
        except (OSError,urllib.error.URLError):pass
        if time.monotonic()>deadline:raise TimeoutError('Application readiness budget')
        time.sleep(.5)
    admin,one,two=login('admin1'),login('student1'),login('student2')
    server_now=request('GET','/api/v1/booking-rules',token=admin)['serverNow']
    # Python3.10 supports six fractional digits; the Java Clock legitimately exposes nanoseconds.
    server_now=re.sub(r'(\.\d{6})\d+',r'\1',server_now)
    now=datetime.fromisoformat(server_now.replace('Z','+00:00')).replace(microsecond=0)
    close=now+timedelta(seconds=60);freeze_deadline=close+timedelta(seconds=30);random_time=close+timedelta(seconds=45);result_deadline=close+timedelta(minutes=5);confirm=result_deadline+timedelta(minutes=30);start=confirm+timedelta(minutes=10);end=start+timedelta(days=2)
    body={'title':'真实未来随机源整批验收 · '+identity[-8:],'purpose':'OTHER','startsAt':iso(start),'endsAt':iso(end),'opensAt':iso(now-timedelta(seconds=10)),'closesAt':iso(close),'freezeDeadline':iso(freeze_deadline),'randomAt':iso(random_time),'resultDeadline':iso(result_deadline),'confirmationDeadline':iso(confirm),'promotionUntil':iso(end),'promotionSeconds':600,'seatIds':[2000,2100]}
    batch=request('POST','/api/v1/preparation-batches',body,admin,'live-create-'+identity[-8:]);bid=batch['id'];facts['batch']={k:batch[k] for k in ['id','public_id','closes_at','freeze_deadline','random_at','result_deadline','source_round','algorithm']};write(out/'batch-created.json',facts['batch'])
    request('POST',f'/api/v1/preparation-batches/{bid}/application',{'version':0,'preferences':[{'seatId':2100,'rank':1},{'seatId':2000,'rank':2}],'keepWaitlist':True},one,'live-apply-one')
    request('POST',f'/api/v1/preparation-batches/{bid}/application',{'version':0,'preferences':[{'seatId':2100,'rank':1}],'keepWaitlist':True},two,'live-apply-two')
    print(json.dumps({'identity':identity,'phase':'WAIT_FOR_PRECOMMITTED_CLOSE','close':batch['closes_at'],'round':batch['source_round']}),flush=True)
    wait_until(close+timedelta(milliseconds=150));request('POST',f'/api/v1/preparation-batches/{bid}/actions',{'version':1,'action':'FREEZE'},admin,'live-freeze')
    packet=request('GET',f"/api/public/batches/{batch['public_id']}/verification");write(out/'frozen-packet.json',packet)
    print(json.dumps({'identity':identity,'phase':'FROZEN_BEFORE_FIXED_FUTURE_ROUND','inputHash':packet['input_hash'],'randomAt':batch['random_at']}),flush=True)
    wait_until(datetime.fromisoformat(batch['random_at'].replace('Z','+00:00'))+timedelta(seconds=2))
    request('POST',f'/api/v1/preparation-batches/{bid}/actions',{'version':2,'action':'ALLOCATE'},admin,'live-allocate')
    packet=request('GET',f"/api/public/batches/{batch['public_id']}/verification");write(out/'public-packet.json',packet);facts['packet']=packet
    facts['reproduction']=verify(packet,args.node)
    query=f"SELECT (SELECT COUNT(*) FROM allocation_result WHERE batch_id={bid}),(SELECT COUNT(*) FROM allocation_outcome WHERE batch_id={bid}),(SELECT COUNT(*) FROM long_offer WHERE batch_id={bid}),(SELECT COUNT(*) FROM notification_outbox WHERE event_key='batch:{bid}:result')"
    mysql_env=dict(os.environ,MYSQL_PWD=db['password']);raw=subprocess.check_output([args.mysql,'--host='+parsed.hostname,'--port='+str(parsed.port or 3306),'--user='+db['username'],'--database='+parsed.path.removeprefix('/'),'--batch','--skip-column-names','-e',query],env=mysql_env,text=True,timeout=10)
    facts['database']=dict(zip(['formal_results','outcomes','offers','result_notices'],map(int,raw.strip().split('\t'))))
except Exception as e:
    execution='ERROR';facts['failure']={'type':type(e).__name__,'message':str(e)};write(out/'failure.json',facts['failure'])
finally:
    if child is not None:
        # The Popen handle belongs to this exact child, never a lookup by port or an unrelated PID.
        if child.poll() is None:child.terminate()
        try:child.wait(timeout=15)
        except subprocess.TimeoutExpired:child.kill();child.wait(timeout=5)
        facts['cleanup']={'processId':child.pid,'stopped':child.poll() is not None,'stoppedUtc':utc()};log.close()
        write(out/'runtime-cleanup.json',facts['cleanup'])
    facts['source_clean']=not bool(git('status','--porcelain'));facts['source_sha']=git('rev-parse','HEAD')
    evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-live-observation/0.2','captured_at':utc(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coordinate),'collection_session_id':identity,'collector_role':'qixu-live-collector','coverage':'COMPLETE' if execution=='COMPLETED' else 'ERROR','normalization_semantics_version':'qixu-live-observation/0.2','facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',evidence);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
    print(json.dumps({'identity':identity,'source_sha':source,'verdict':report['verdict'],'execution_status':execution,'boundary':facts['boundary']}),flush=True)
sys.exit(0 if report['verdict']=='PASS' else 1)
