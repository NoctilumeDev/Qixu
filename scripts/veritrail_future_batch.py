"""Sealed real future-round probe. Requires an exact built candidate and a dedicated test DB. No implicit resets."""
from __future__ import annotations
import hashlib,json,os,re,socket,subprocess,sys,time,uuid
from pathlib import Path, PurePosixPath
from datetime import datetime,timedelta,timezone
import urllib.request,urllib.error
from veritrail.acceptance_plan import observation_spec_digest,seal_acceptance_plan,verify_sealed_acceptance_plan
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
parser.add_argument('--stage',choices=('m3','m4','m9'),default='m3')
parser.add_argument('--hold-for-browser',action='store_true',help='M9 only: keep the exact installed app until stdin receives stop; parent Plan binds actual CUA captures')
parser.add_argument('--stop-agent',type=Path,help='M9 only: exact owned stdin graceful JVM stop agent')
parser.add_argument('--java',default=str(Path(os.environ.get('JAVA_HOME',''))/'bin'/('java.exe' if os.name=='nt' else 'java')) if os.environ.get('JAVA_HOME') else 'java')
parser.add_argument('--node',default=os.environ.get('QIXU_NODE','node'))
parser.add_argument('--mysql',default='mysql')
parser.add_argument('--proxy',default=os.environ.get('QIXU_RANDOM_PROXY',''))
parser.add_argument('--producer-bundle',type=Path,required=True,help='Original native Bundle: M3 Plan8/native0.8, M4 Plan2/native0.10, or explicit M9 entry using M8 Plan3/native0.22; exact SHA and freshly built jar')
args=parser.parse_args()
if args.hold_for_browser and args.stage!='m9':raise RuntimeError('Browser hold belongs only to the new M9 contract')
if args.stage=='m9' and (not args.stop_agent or not args.stop_agent.is_file()):raise RuntimeError('M9 requires its bound owned graceful stop agent')
if args.stage!='m9' and args.stop_agent:raise RuntimeError('Stop agent belongs only to the new M9 contract')
producer_stage='m8' if args.stage=='m9' else args.stage
producer_version={'m3':8,'m4':2,'m9':3}[args.stage]
producer_collector={'m3':'qixu-native/0.8','m4':'qixu-native/0.10','m9':'qixu-native/0.22'}[args.stage]
live_version={'m3':'0.3','m4':'0.4','m9':'0.5'}[args.stage]
live_collector='qixu-live-observation/'+live_version
url=os.environ.get('QIXU_TEST_DB_URL','');parsed=urllib.parse.urlparse(url.removeprefix('jdbc:'))
if parsed.scheme!='mysql' or parsed.path not in ('/qixu_test','/qixu_ci') or not parsed.hostname or parsed.username or parsed.password or not os.environ.get('QIXU_TEST_DB_PASSWORD'):
    raise RuntimeError('Only explicitly supplied dedicated qixu_test/qixu_ci credentials are accepted')
db={'url':url,'username':os.environ.get('QIXU_TEST_DB_USER','qixu_test_app'),'password':os.environ['QIXU_TEST_DB_PASSWORD']}

producer=args.producer_bundle.resolve();manifest=producer/'acceptance-bundle-manifest.json'
data=json.loads(manifest.read_text(encoding='utf-8'));total=0;seen=set()
for entry in data['files']:
    relative=PurePosixPath(entry['path'])
    if relative.is_absolute() or '..' in relative.parts or '\\' in entry['path'] or entry['path'] in seen or type(entry['size']) is not int or entry['size']<0:raise RuntimeError('Unsafe or duplicated producer entry')
    seen.add(entry['path']);file=(producer/relative).resolve();total+=entry['size']
    if not file.is_relative_to(producer) or file.is_symlink() or entry['size']>2097152 or total>4194304 or file.stat().st_size!=entry['size'] or sha(file)!=entry['sha256']:raise RuntimeError('Producer Bundle bytes or size drifted')
actual={p.relative_to(producer).as_posix() for p in producer.rglob('*') if p.is_file() and p!=manifest}
if actual!=seen:raise RuntimeError('Missing or unmanifested producer files')
report=json.loads((producer/'acceptance-report.json').read_text(encoding='utf-8'))
producer_plan=json.loads((producer/'sealed-acceptance-plan.json').read_text(encoding='utf-8'));verify_sealed_acceptance_plan(producer_plan)
if report['verdict']!='PASS' or report['execution_status']!='COMPLETED' or report['subject']['id']!='qixu-'+producer_stage or report['subject']['version']!=source or report['plan']['version']!=producer_version or report['plan']['sha256']!=producer_plan['seal']['digest'] or producer_plan['subject']!=report['subject']:raise RuntimeError('Native producer does not qualify this source')
entries=[e for e in report['evidence'] if e['evidence_type']=='qixu.native.observation']
if len(entries)!=1 or entries[0]['path'] not in seen:raise RuntimeError('Ambiguous native producer')
entry=entries[0];evidence_path=producer/entry['path'];producer_evidence=json.loads(evidence_path.read_text(encoding='utf-8'));producer_facts=producer_evidence['facts'];package=producer_facts.get('package',{})
if producer_evidence['source']!=producer_collector or entry['sha256']!=sha(evidence_path) or entry['facts_digest']!=sha256_json(producer_facts) or producer_evidence['metadata']['veritrail_observation']['plan_digest']!=producer_plan['seal']['digest']:raise RuntimeError('Producer report does not bind original evidence')
if producer_facts.get('source_sha')!=source or producer_facts.get('source_clean') is not True or producer_facts.get('command_exit')!=0 or package.get('current_build') is not True or package.get('source_sha')!=source or package.get('sha256')!=sha(jar) or package.get('size')!=jar.stat().st_size:raise RuntimeError('Current jar lacks exact fresh producer binding')

with socket.socket() as probe:probe.bind(('127.0.0.1',6967))
identity=args.stage+'-live-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True)
coordinate={'collector_sha256':sha(Path(__file__)),'producer_manifest_sha256':sha(manifest),'producer_acceptance_id':report['acceptance_id'], 'source_sha':source,'jar_sha256':sha(jar),'stage':args.stage+'-fixed-future-batch','collector':live_collector}
if args.stage=='m9':coordinate.update(hold_for_browser=args.hold_for_browser,contract_sha256=sha(ROOT/'docs/contracts/engineering-delivery.md'),stop_agent_sha256=sha(args.stop_agent))
spec={'id':'live-batch','contract':{'id':'qixu-live-batch','version':live_version},'evidence_type':'qixu.live.batch','coordinates':coordinate,'projections':['source_sha','source_clean','jar_sha256','producer','requests','batch','packet','database','reproduction','cleanup','installed'],'canonicalization_profile':'veritrail-json-c14n/1'}
def assertion(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'live','path':path},'operator':'eq','right':value}
assertions=[assertion('producer-bytes','/facts/producer/bytes_checked',True),assertion('producer-manifest','/facts/producer/manifest_sha256',coordinate['producer_manifest_sha256']),assertion('exact-source','/facts/source_sha',source),assertion('clean-source','/facts/source_clean',True),assertion('installed-jar','/facts/jar_sha256',coordinate['jar_sha256']),assertion('full-result','/facts/database/formal_results',1),assertion('all-outcomes','/facts/database/outcomes',2),assertion('offers','/facts/database/offers',2),assertion('all-notifications','/facts/database/result_notices',2),assertion('independent-reproduction','/facts/reproduction/independent_bytes_equal',True),assertion('actual-bls','/facts/reproduction/signature_verified',True),assertion('maximum','/facts/reproduction/maximum',2),assertion('owned-runtime-stopped','/facts/cleanup/stopped',True)]
if args.stage in ('m4','m9'):
    installed_expected={'private_report_status':404,'work_done_report':'IN_REPAIR','work_done_condition':'BROKEN','missing_repair_status':409,'missing_repair_receipts':0,'repair_status':'VERIFIED_CLOSED','report_status':'RESOLVED','condition':'WORKING','private_case_status':404,'premature_revoke_status':409,'premature_revoke_receipts':0,'notice_right_status':'ACTIVE','notice_penalties':0,'case_status':'DISMISSED','final_right_status':'ACTIVE','formal_hash_unchanged':True,'public_private_marker':False}
    assertions.extend(assertion('installed-'+name,'/facts/installed/'+name,value) for name,value in installed_expected.items())
if args.stage=='m9':assertions.extend([assertion('normal-jvm-exit','/facts/cleanup/normal_exit',True),assertion('normal-stop-marker','/facts/cleanup/graceful_marker',True)])
plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-'+args.stage+'-live','version':3 if args.stage=='m3' else 1,'subject':{'id':'qixu-'+args.stage+'-live','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does this exact installed candidate freeze before a fixed future beacon, publish one full batch and permit independent public-byte reproduction with real BLS?'+(' Do installed repair and private governance notice preserve the declared real-clock facts?' if args.stage in ('m4','m9') else ''),'governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:live-adapter','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'live','observation_spec_id':spec['id'],'cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'live','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':420},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu live batch','consumers':[args.stage+'-native-stage']},'reproduction_steps':['Use the exact clean source and jar with a schema-scoped qixu demo DB and fixed official verifier dependencies.','Publish explicit future close/freeze/round/result deadlines before submitting two hard-constrained applicants.','Freeze before the fixed round; fetch that exact round, publish, download the public packet and independently verify all bytes/BLS.'],'cleanup_steps':['Stop only the java child created and retained by this probe; preserve other ports and shared MySQL.','Retain demonstration batch/history and the immutable packet and Bundle.']})
write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coordinate)
facts={'source_sha':source,'source_clean':True,'jar_sha256':coordinate['jar_sha256'],'producer':{'acceptance_id':report['acceptance_id'],'manifest_sha256':sha(manifest),'bytes_checked':True},'requests':[],'boundary':'ONE_ACTUAL_FUTURE_BATCH_NOT_FAULT_RECOVERY_CAPACITY_OR_UI'}
if args.stage in ('m4','m9'):facts['boundary']=args.stage.upper()+'_INSTALLED_FUTURE_BATCH_REPAIR_AND_REAL_CLOCK_NOTICE_NOT_DAY_LONG_GOVERNANCE_PHYSICAL_REPAIR_OR_UI'
env=dict(os.environ,SPRING_PROFILES_ACTIVE='demo',QIXU_DB_URL=db['url'],QIXU_DB_USERNAME=db['username'],QIXU_DB_PASSWORD=db['password'],QIXU_PORT='6967',QIXU_COOKIE_SECURE='false',QIXU_TASKS_ENABLED='false',QIXU_NODE=args.node,QIXU_RANDOM_VERIFIER=str(ROOT/'randomness/verify.mjs'),QIXU_RANDOM_PROXY=args.proxy)
execution='COMPLETED';child=None;owner={};base='http://127.0.0.1:6967';opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def request(method,path,body=None,token=None,key=None,expected=200,code=None):
    headers={}
    if body is not None:headers['Content-Type']='application/json'
    if token:headers['Authorization']='Bearer '+token
    if key:headers['Idempotency-Key']=key
    req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),headers=headers,method=method)
    try:
        with opener.open(req,timeout=20) as response:status=response.status;value=json.load(response)
    except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
    facts['requests'].append({'method':method,'path':path,'status':status})
    if status!=expected or (code is not None and value.get('error',{}).get('code')!=code):raise RuntimeError('HTTP '+str(status)+' '+str(value.get('error',{}).get('code','')))
    if status!=200:return value
    return value['data']
def login(name):return request('POST','/api/v1/auth/login',{'username':name,'password':'qixu-demo','mode':'BEARER'})['token']
def sql(query):
    mysql_env=dict(os.environ,MYSQL_PWD=db['password'])
    return subprocess.check_output([args.mysql,'--host='+parsed.hostname,'--port='+str(parsed.port or 3306),'--user='+db['username'],'--database='+parsed.path.removeprefix('/'),'--batch','--skip-column-names','-e',query],env=mysql_env,text=True,timeout=10).strip()

def installed_m4(admin,one,two,bid,packet):
    observed={};facts['installed']=observed
    mine=request('GET',f'/api/v1/preparation-batches/{bid}/application',token=one)
    offer=next(item for item in mine['offers'] if item['status']=='OPEN')
    accepted=request('POST',f"/api/v1/long-offers/{offer['id']}/actions",{'version':offer['version'],'action':'ACCEPT'},one,'installed-long-accept')
    # Read the authoritative receipt-independent original entitlement after confirmation.
    mine=request('GET',f'/api/v1/preparation-batches/{bid}/application',token=one)
    entitlement=next(item for item in mine['entitlements'] if item['status']=='ACTIVE');sid=entitlement['space_id']
    report=request('POST','/api/v1/feedback',{'spaceId':sid,'category':'OUTLET','description':'DEMO_PRIVATE_M4_REPORT: installed synthetic outlet issue'},one,'installed-feedback')
    rid=report['id'];request('GET',f'/api/v1/feedback/{rid}',token=two,expected=404,code='RESOURCE_NOT_FOUND');observed['private_report_status']=facts['requests'][-1]['status']
    verified=request('POST',f'/api/v1/admin/feedback/{rid}/actions',{'version':report['version'],'action':'VERIFY','reason':'Installed synthetic verification','facts':[{'key':'outlet','value':False},{'key':'outletCondition','value':'BROKEN'}]},admin,'installed-verify')
    repair=request('POST','/api/v1/admin/repairs',{'reports':[{'id':rid,'version':verified['version']}],'description':'Installed synthetic repair, not physical work'},admin,'installed-repair')
    tid=repair['id']
    assigned=request('POST',f'/api/v1/admin/repairs/{tid}/actions',{'version':repair['version'],'action':'ASSIGN','reason':'Synthetic assignment','assignee':'DEMO'},admin,'installed-assign')
    done=request('POST',f'/api/v1/admin/repairs/{tid}/actions',{'version':assigned['version'],'action':'WORK_DONE','reason':'Synthetic completion'},admin,'installed-work-done')
    observed['work_done_report']=sql(f'SELECT status FROM feedback_report WHERE id={rid}')
    observed['work_done_condition']=request('GET',f'/api/v1/spaces/{sid}',token=one)['profile']['conditions']['outletCondition']
    request('POST',f'/api/v1/admin/repairs/{tid}/actions',{'version':done['version'],'action':'VERIFY','reason':'Missing fact control','verified':True},admin,'installed-missing-repair-fact',expected=409,code='REPAIR_FACT_REQUIRED')
    observed['missing_repair_status']=facts['requests'][-1]['status'];observed['missing_repair_receipts']=int(sql("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='installed-missing-repair-fact'"))
    request('POST',f'/api/v1/admin/repairs/{tid}/actions',{'version':done['version'],'action':'VERIFY','reason':'Explicit synthetic restored facts','verified':True,'facts':[{'key':'outlet','value':True},{'key':'outletCondition','value':'WORKING'}]},admin,'installed-repair-restored')
    observed['repair_status']=sql(f'SELECT status FROM repair_ticket WHERE id={tid}');observed['report_status']=sql(f'SELECT status FROM feedback_report WHERE id={rid}')
    observed['condition']=request('GET',f'/api/v1/spaces/{sid}',token=one)['profile']['conditions']['outletCondition']
    # Actual clock, generous announced notice interval, no synthetic day passage.
    notice=request('POST','/api/v1/admin/governance-cases',{'entitlementId':entitlement['id'],'entitlementVersion':entitlement['version'],'reasonCode':'EXPLICIT_RULE_VIOLATION','evidence':'DEMO_PRIVATE_M4_CASE: synthetic notice only','statementUntil':iso(datetime.now(timezone.utc).replace(microsecond=0)+timedelta(hours=24,minutes=5))},admin,'installed-notice')
    cid=notice['id'];request('GET',f'/api/v1/governance-cases/{cid}',token=two,expected=404,code='RESOURCE_NOT_FOUND');observed['private_case_status']=facts['requests'][-1]['status']
    statement=request('POST',f'/api/v1/governance-cases/{cid}/statements',{'version':notice['version'],'message':'Synthetic student statement, retain notice rights'},one,'installed-statement')
    request('POST',f'/api/v1/admin/governance-cases/{cid}/decisions',{'version':statement['version'],'action':'REVOKE','reason':'Premature real clock control'},admin,'installed-premature-revoke',expected=409,code='STATEMENT_WINDOW_OPEN')
    observed['premature_revoke_status']=facts['requests'][-1]['status'];observed['premature_revoke_receipts']=int(sql("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='installed-premature-revoke'"))
    observed['notice_right_status']=sql(f"SELECT status FROM seat_entitlement WHERE id={entitlement['id']}");observed['notice_penalties']=int(sql(f'SELECT COUNT(*) FROM long_application_penalty WHERE case_id={cid}'))
    request('POST',f'/api/v1/admin/governance-cases/{cid}/decisions',{'version':statement['version'],'action':'DISMISS','reason':'Close synthetic notice without revocation'},admin,'installed-dismiss')
    observed['case_status']=sql(f'SELECT status FROM governance_case WHERE id={cid}');observed['final_right_status']=sql(f"SELECT status FROM seat_entitlement WHERE id={entitlement['id']}")
    observed['formal_hash_unchanged']=sql(f'SELECT output_hash FROM allocation_result WHERE batch_id={bid}')==packet['result']['output_hash']
    public=request('GET',f'/api/v1/spaces/{sid}',token=two);public_facts=request('GET',f'/api/v1/spaces/{sid}/facts',token=two)
    observed['public_private_marker']='DEMO_PRIVATE_M4' in json.dumps([public,public_facts])
def wait_until(instant):
    while datetime.now(timezone.utc)<instant:
        if child.poll() is not None:raise RuntimeError('Owned JVM stopped early')
        time.sleep(min(1.0,max(0.01,(instant-datetime.now(timezone.utc)).total_seconds())))

try:
    log=(out/'runtime.log').open('wb');child=subprocess.Popen([args.java]+(['-javaagent:'+str(args.stop_agent.resolve())] if args.stage=='m9' else [])+['-jar',str(jar)],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,stdin=subprocess.PIPE if args.stage=='m9' else None,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
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
    if args.stage in ('m4','m9'):installed_m4(admin,one,two,bid,packet)
    if args.hold_for_browser:
        print(json.dumps({'identity':identity,'phase':'M9_BROWSER_READY','batch_id':bid}),flush=True)
        for line in sys.stdin:
            if line.strip()=='stop':break
            raise RuntimeError('M9 hold accepts only lowercase stop')
        else:raise RuntimeError('M9 browser hold ended without explicit stop')
        raw=sql("SELECT (SELECT COUNT(*) FROM short_reservation WHERE user_id=1 AND status IN ('PENDING','CHECKED_IN')),(SELECT COUNT(*) FROM campus_event WHERE status='PUBLISHED'),(SELECT COUNT(*) FROM event_participation WHERE user_id=1 AND status='CONFIRMED'),(SELECT COUNT(*) FROM feedback_report WHERE status='RESOLVED'),(SELECT COUNT(*) FROM repair_ticket WHERE status='VERIFIED_CLOSED'),(SELECT COUNT(*) FROM flyway_schema_history WHERE success=1);")
        facts['m9_browser_sql']=dict(zip(['shorts','events','participations','resolved_reports','verified_repairs','migrations'],map(int,raw.split('\t'))))
except Exception as e:
    execution='ERROR';facts['failure']={'type':type(e).__name__,'message':str(e)};write(out/'failure.json',facts['failure'])
finally:
    if child is not None:
        # The Popen handle belongs to this exact child, never a lookup by port or an unrelated PID.
        normal=False
        if args.stage=='m9':
            try:
                if child.poll() is None:
                    if os.name=='nt':
                        ps="$p=Get-CimInstance Win32_Process -Filter 'ProcessId="+str(child.pid)+"';$p|Select-Object ExecutablePath,CommandLine|ConvertTo-Json -Compress"
                        row=json.loads(subprocess.check_output(['powershell','-NoProfile','-Command',ps],text=True))
                        if Path(row['ExecutablePath']).resolve()!=Path(args.java).resolve() or str(jar) not in row['CommandLine'] or str(args.stop_agent.resolve()) not in row['CommandLine']:raise RuntimeError('M9 Java process ownership drift')
                    child.stdin.write(b'qixu-m9-graceful-stop\n');child.stdin.flush()
                child.wait(timeout=35);normal=child.returncode==0
            except (OSError,subprocess.TimeoutExpired):
                execution='ERROR'
                if child.poll() is None:child.terminate();child.wait(timeout=10)
        else:
            if child.poll() is None:child.terminate()
            try:child.wait(timeout=15)
            except subprocess.TimeoutExpired:child.kill();child.wait(timeout=5)
        facts['cleanup']={'processId':child.pid,'stopped':child.poll() is not None,'stoppedUtc':utc()};log.close()
        if args.stage=='m9':facts['cleanup'].update(normal_exit=normal,exit_code=child.returncode,method='OWNED_STDIN_SYSTEM_EXIT_0' if normal else 'NOT_NORMAL',graceful_marker='M9_GRACEFUL_STOP_REQUESTED' in (out/'runtime.log').read_text(encoding='utf-8',errors='replace'))
        write(out/'runtime-cleanup.json',facts['cleanup'])
    facts['source_clean']=not bool(git('status','--porcelain'));facts['source_sha']=git('rev-parse','HEAD')
    evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':live_collector,'captured_at':utc(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coordinate),'collection_session_id':identity,'collector_role':'qixu-live-collector','coverage':'COMPLETE' if execution=='COMPLETED' else 'ERROR','normalization_semantics_version':live_collector,'facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',evidence);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
    print(json.dumps({'identity':identity,'source_sha':source,'verdict':report['verdict'],'execution_status':execution,'boundary':facts['boundary']}),flush=True)
sys.exit(0 if report['verdict']=='PASS' else 1)
