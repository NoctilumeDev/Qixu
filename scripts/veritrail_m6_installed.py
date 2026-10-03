"""M6 real pinned DarkRoom + owned restart + response loss + fixed-round recovery.

Core adjudicates facts supplied by this collector; this is not Starter hosting.
Only dedicated qixu_test/qixu_ci and qixu_darkroom_test are accepted. Never stop
shared database services or adopt a process found merely by its port.
"""
from __future__ import annotations
import argparse,hashlib,http.client,http.server,json,os,re,socket,subprocess,sys,threading,time,urllib.error,urllib.parse,urllib.request,uuid,zipfile
from datetime import datetime,timedelta,timezone
from pathlib import Path
from importlib.metadata import version
from veritrail.acceptance_plan import observation_spec_digest,seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
from m6_producer_binding import bind,digest

ROOT=Path(__file__).resolve().parents[1]
PIN='d6e42a81d3eb83ed483553b7319bbe55dceb8c7f'
COLLECTOR='qixu-m6-installed/0.3'
AUTH='/api/dark-room-library/v1/user/auth'
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))

def git(*args,cwd=ROOT):return subprocess.check_output(['git',*args],cwd=cwd,text=True).strip()
def utc():return datetime.now(timezone.utc)
def iso(value):return value.astimezone(timezone.utc).isoformat().replace('+00:00','Z')
def write(path,value):
    with Path(path).open('x',encoding='utf-8',newline='\n') as f:json.dump(value,f,ensure_ascii=False,sort_keys=True,indent=2);f.write('\n')
def quote(value):return 'CONVERT(0x'+value.encode('utf-8').hex()+' USING utf8mb4) COLLATE utf8mb4_bin'
def published_result(packet):
    result=packet.get('result')
    if not isinstance(result,dict):raise ValueError('Public result must be an object')
    if not result:return False
    if not isinstance(result.get('proof'),dict) or type(result['proof'].get('round')) is not int or not all(k in result for k in ('input_hash','output_hash','outputBytes','maximum_count','candidate_count','published_at')):raise ValueError('Nonempty public result is incomplete')
    return True

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--producer-bundle',type=Path,required=True);p.add_argument('--darkroom-source',type=Path,required=True)
    p.add_argument('--java',required=True);p.add_argument('--maven',required=True);p.add_argument('--mysql',required=True);p.add_argument('--node',required=True);p.add_argument('--npm',required=True)
    a=p.parse_args();source=git('rev-parse','HEAD')
    if version('veritrail')!='0.13.0' or git('status','--porcelain'):raise RuntimeError('Core0.13.0 and exact clean source required')
    dark=a.darkroom_source.resolve()
    if not dark.is_relative_to((ROOT/'.tools/references').resolve()) or git('rev-parse','HEAD',cwd=dark)!=PIN or git('status','--porcelain',cwd=dark):raise RuntimeError('Requires unmodified owned pinned DarkRoom reference checkout')
    jar=ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar';producer=bind(a.producer_bundle,source,jar)
    qurl=os.environ.get('QIXU_TEST_DB_URL','');qp=urllib.parse.urlsplit(qurl.removeprefix('jdbc:'))
    durl=os.environ.get('QIXU_DARK_ROOM_TEST_URL','');dp=urllib.parse.urlsplit(durl.removeprefix('jdbc:'))
    if qp.hostname!='127.0.0.1' or qp.path not in ('/qixu_test','/qixu_ci') or qp.username or qp.password or dp.hostname!='127.0.0.1' or dp.path!='/qixu_darkroom_test' or dp.username or dp.password:raise RuntimeError('Dedicated loopback schemas only')
    for key in ('QIXU_TEST_DB_USER','QIXU_TEST_DB_PASSWORD','QIXU_DARK_ROOM_TEST_USER','QIXU_DARK_ROOM_TEST_PASSWORD','QIXU_DARK_ROOM_TEST_JWT_SECRET','QIXU_EXTERNAL_TICKET_KEY'):
        if not os.environ.get(key):raise RuntimeError('Required private environment missing: '+key)
    bootstrap=(dark/'sql/init-dark-room-library.sql').read_bytes()
    bootstrap_source=hashlib.sha256(bootstrap).hexdigest();bootstrap_transformed=hashlib.sha256(bootstrap.replace(b'`dark_room_library`',b'`qixu_darkroom_test`')).hexdigest()
    if os.environ.get('QIXU_DARK_ROOM_SQL_SOURCE_SHA256')!=bootstrap_source or os.environ.get('QIXU_DARK_ROOM_SQL_TRANSFORM_SHA256')!=bootstrap_transformed:raise RuntimeError('Dedicated bootstrap provenance does not match pinned SQL and schema-only byte transformation')
    for port in (6970,6971):
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    identity='m6-installed-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True,exist_ok=False)
    coord={'source_sha':source,'jar_sha256':producer['jar_sha256'],'producer_manifest_sha256':producer['manifest_sha256'],'upstream_source_sha':PIN,'collector':COLLECTOR,'collector_sha256':digest(Path(__file__))}
    spec={'id':'installed','contract':{'id':'qixu-m6-installed','version':'0.3'},'evidence_type':'qixu.m6.installed','coordinates':coord,'projections':['source_sha','source_clean','producer','upstream','requests','identity','recovery','allocation','reproduction','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
    expected={'/source_sha':source,'/source_clean':True,'/producer/bytes_checked':True,'/producer/jar_sha256':coord['jar_sha256'],'/upstream/source_sha':PIN,'/upstream/source_clean':True,'/upstream/build_exit':0,
        '/identity/local_role':'STUDENT','/identity/management_status':403,'/identity/local_password_status':401,'/identity/outage_status':503,'/identity/local_independent_status':200,'/identity/restored_status':200,'/identity/revoked_status':401,'/identity/new_ticket_receipt_equal':True,
        '/recovery/response_discarded_status':200,'/recovery/client_unknown':True,'/recovery/distinct_restart_pid':True,'/recovery/receipt_equal':True,'/recovery/replay_equal':True,'/recovery/changed_body_status':409,'/recovery/foreign_receipt_status':404,'/recovery/reservations':1,'/recovery/receipts':1,'/recovery/audit_events':1,'/recovery/outbox_events':1,'/recovery/pending_before_restart':1,'/recovery/inbox_after_restart':1,'/recovery/inbox_after_repeat':1,'/recovery/unread_after_restart':1,
        '/allocation/frozen_hash_unchanged':True,'/allocation/frozen_round_unchanged':True,'/allocation/formal_results':1,'/allocation/outcomes':2,'/allocation/result_notices':2,'/reproduction/independent_bytes_equal':True,'/reproduction/signature_verified':True,'/reproduction/maximum':2,'/cleanup/all_owned_stopped':True}
    assertions=[{'id':'installed-'+str(i),'severity':'HARD','left':{'requirement_id':'installed','path':'/facts'+path},'operator':'eq','right':value} for i,(path,value) in enumerate(expected.items())]
    plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m6-installed','version':3,'subject':{'id':'qixu-m6-installed','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does this fixed candidate preserve local authority, committed receipts, notification identity and frozen allocation through real isolated upstream and owned process faults?',
        'governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:m6-installed-collector','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'installed','observation_spec_id':'installed','cardinality':'EXACTLY_ONE'}],
        'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'installed','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':900},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu M6 installed','consumers':['M6-stage']},
        'reproduction_steps':['Use exact clean source, original qualified M6 native bundle, fresh bound jar and pinned DarkRoom checkout.','Supply only dedicated schema-scoped credentials and separate private ticket/JWT keys.','Run the collector; it seals before upstream build, starts owned processes, discards a real committed response and performs bounded recovery.'],
        'cleanup_steps':['Stop only the captured child handles after executable/jar/CIM readback.','Retain test fixtures, first failure, packets and immutable bundle; shared services remain running.']})
    write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coord)
    facts={'source_sha':source,'source_clean':True,'producer':producer,'upstream':{'source_sha':PIN,'bootstrap':{'source_sql_sha256':bootstrap_source,'schema_only_sql_sha256':bootstrap_transformed,'schema':'qixu_darkroom_test','provisioning':'PREEXISTING_DEDICATED_SCHEMA_OPERATOR_PROVENANCE_NOT_COLLECTOR_EXECUTED'}},'requests':[],'identity':{},'recovery':{},'allocation':{},'boundary':'ISOLATED_REAL_DARKROOM_AND_OWNED_RESTART_NOT_SSO_PRODUCTION_BACKUP_MULTI_NODE_OR_DEVICE'}
    children=[];logs=[];execution='COMPLETED';qchild=None;dchild=None;sequence=0;sql_sequence=0;base='http://127.0.0.1:6971';dbase='http://127.0.0.1:6970/api/dark-room-library/v1'
    def sql(statement,darkroom=False):
        nonlocal sql_sequence
        sql_sequence+=1
        env=dict(os.environ,MYSQL_PWD=os.environ['QIXU_DARK_ROOM_TEST_PASSWORD' if darkroom else 'QIXU_TEST_DB_PASSWORD']);parsed=dp if darkroom else qp;user=os.environ['QIXU_DARK_ROOM_TEST_USER' if darkroom else 'QIXU_TEST_DB_USER']
        r=subprocess.run([a.mysql,'--protocol=TCP','-h',parsed.hostname,'-P',str(parsed.port or 3306),'-u',user,'--batch','--skip-column-names','--default-character-set=utf8mb4',parsed.path[1:]],input=statement,text=True,encoding='utf-8',capture_output=True,env=env,timeout=15)
        if r.returncode:
            (out/('sql-'+str(sql_sequence)+'.private.stderr')).write_text(r.stderr,encoding='utf-8')
            code=re.search(r'ERROR (\d+) \(([^)]+)\)',r.stderr)
            raise RuntimeError('Dedicated SQL operation '+str(sql_sequence)+' failed'+(' ['+code[1]+'/'+code[2]+']' if code else '')+'; raw SQL/errors retained privately')
        return r.stdout.strip()
    def request(method,path,body=None,token=None,key=None,expected_status=200,darkroom=False,url_override=None,record=True):
        headers={};url=url_override or ((dbase if darkroom else base)+path)
        if body is not None:headers['Content-Type']='application/json'
        if token:headers['Authorization']='Bearer '+token
        if key:headers['Idempotency-Key']=key
        req=urllib.request.Request(url,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),headers=headers,method=method)
        try:
            with opener.open(req,timeout=12) as r:status=r.status;value=json.load(r)
        except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
        if record:facts['requests'].append({'service':'darkroom' if darkroom else 'qixu','method':method,'path':path,'status':status})
        if expected_status is not None and status!=expected_status:raise RuntimeError('Unexpected '+('upstream' if darkroom else 'qixu')+' HTTP status '+str(status))
        if status!=200:return value
        if darkroom and path.startswith('/health/'):
            if value.get('status') not in ('UP','DEGRADED'):raise RuntimeError('Upstream readiness is not accepting traffic')
            return value
        if darkroom and value.get('code')!=200:raise RuntimeError('Upstream application rejected its request')
        return value['data']
    def start(jar_path,env,label):
        nonlocal sequence
        sequence+=1;log=(out/(str(sequence)+'-'+label+'.log')).open('xb');logs.append(log)
        proc=subprocess.Popen([a.java,'-jar',str(jar_path)],cwd=out,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
        owner={'pid':proc.pid,'executable':str(Path(a.java).resolve()),'jar':str(jar_path.resolve()),'started_at':iso(utc()),'label':label,'source_sha':source if label.startswith('qixu') else PIN}
        children.append((proc,owner));write(out/(str(sequence)+'-owner.json'),owner)
        return proc
    def stop(proc):
        if proc is None or proc.poll() is not None:return
        owner=next(o for c,o in children if c is proc)
        if os.name=='nt':
            command=f"Get-CimInstance Win32_Process -Filter 'ProcessId={proc.pid}' | Select-Object ProcessId,ExecutablePath,CommandLine,CreationDate | ConvertTo-Json -Compress"
            r=subprocess.run(['powershell','-NoProfile','-Command',command],capture_output=True,text=True,encoding='utf-8',timeout=10)
            current=json.loads(r.stdout)
            if str(current.get('ExecutablePath','')).lower()!=owner['executable'].lower() or owner['jar'].lower() not in current.get('CommandLine','').lower():raise RuntimeError('Owned process identity changed; refuse termination')
            write(out/('stop-'+str(proc.pid)+'-cim.json'),current)
        proc.terminate()
        try:proc.wait(timeout=20)
        except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=5)
    def ready(proc,darkroom=False):
        end=time.monotonic()+55;path='/health/ready' if darkroom else '/api/health'
        while time.monotonic()<end:
            if proc.poll() is not None:raise RuntimeError('Owned '+('upstream' if darkroom else 'qixu')+' process exited before ready')
            try:request('GET',path,darkroom=darkroom,record=False);return
            except (urllib.error.URLError,OSError,ValueError,RuntimeError):time.sleep(.4)
        raise RuntimeError('Owned service did not become ready within budget')
    def local(name):return request('POST','/api/v1/auth/login',{'username':name,'password':'qixu-demo','mode':'BEARER'})['token']
    def dark_login(account):
        captcha=request('GET','/captcha/generate',darkroom=True);m=re.fullmatch(r'\s*(\d+)\s*([+×-])\s*(\d+)\s*=\s*\?\s*',captcha['expression'])
        if not m:raise RuntimeError('Unsupported actual upstream captcha')
        x,y=int(m[1]),int(m[3]);answer=x+y if m[2]=='+' else x-y if m[2]=='-' else x*y
        return request('POST','/user/login',{'userAccount':account,'userPwd':'DarkRoom@20606','captchaId':captcha['captchaId'],'captchaAnswer':answer},darkroom=True)['token']
    def exchange(ticket):return request('POST','/api/v1/auth/external/dark-room',{'ticket':ticket,'mode':'BEARER'})['token']
    def poll(condition,seconds):
        end=time.monotonic()+seconds
        while time.monotonic()<end:
            if condition():return
            time.sleep(.5)
        raise RuntimeError('Expected recovery fact not observed in bounded window')
    try:
        print(json.dumps({'identity':identity,'phase':'SEALED_REAL_INTEGRATION_BUILD'}),flush=True)
        pom=dark/'backend/dark-room-library-api/pom.xml'
        build=subprocess.run([a.maven,'-B','-ntp','-f',str(pom),'-DskipTests','clean','package'],cwd=dark,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=240)
        (out/'darkroom-build.stdout').write_text(build.stdout,encoding='utf-8');(out/'darkroom-build.stderr').write_text(build.stderr,encoding='utf-8');facts['upstream']['build_exit']=build.returncode
        if build.returncode:raise RuntimeError('Pinned upstream build failed')
        djar=dark/'backend/dark-room-library-api/target/dark-room-library-api-1.2.7.jar';facts['upstream']['jar_sha256']=digest(djar);facts['upstream']['source_clean']=not bool(git('status','--porcelain',cwd=dark))
        qjar=out/'qixu-api.jar';qjar.write_bytes(jar.read_bytes())
        if digest(qjar)!=producer['jar_sha256']:raise RuntimeError('Installed jar drifted before startup')
        denv=dict(os.environ,SERVER_PORT='6970',SERVER_ADDRESS='127.0.0.1',DB_URL=durl,DB_USERNAME=os.environ['QIXU_DARK_ROOM_TEST_USER'],DB_PASSWORD=os.environ['QIXU_DARK_ROOM_TEST_PASSWORD'],JWT_SECRET=os.environ['QIXU_DARK_ROOM_TEST_JWT_SECRET'],REDIS_ENABLED='false',RABBITMQ_ENABLED='false',FILE_UPLOAD_DIR=str(out/'darkroom-upload'),MAIL_USERNAME='',MAIL_PASSWORD='')
        for key in ('DEEPSEEK_API_KEY','OPENAI_API_KEY'):denv.pop(key,None)
        dchild=start(djar,denv,'darkroom');ready(dchild,True)
        qenv=dict(os.environ,QIXU_DB_URL=qurl,QIXU_DB_USERNAME=os.environ['QIXU_TEST_DB_USER'],QIXU_DB_PASSWORD=os.environ['QIXU_TEST_DB_PASSWORD'],QIXU_PORT='6971',QIXU_BIND='127.0.0.1',SPRING_PROFILES_ACTIVE='demo',QIXU_COOKIE_SECURE='false',QIXU_TASKS_ENABLED='false',QIXU_DARK_ROOM_ENABLED='true',QIXU_DARK_ROOM_AUTH_URI='http://127.0.0.1:6970'+AUTH,QIXU_DARK_ROOM_LOOPBACK='true',QIXU_NODE=a.node,QIXU_RANDOM_VERIFIER=str(ROOT/'randomness/verify.mjs'))
        qchild=start(qjar,qenv,'qixu-initial');ready(qchild)
        suffix=identity[-8:];account='drl_qixu_'+suffix
        sql("INSERT INTO user(user_account,user_name,user_pwd,user_role,auth_version,account_status,is_login,is_word) SELECT "+quote(account)+','+quote('Qixu isolated '+suffix)+",user_pwd,0,1,0,0,0 FROM user WHERE id=1;",True)
        upstream_id=int(sql('SELECT id FROM user WHERE user_account='+quote(account)+';',True))
        sql("INSERT INTO identity_user(username,password_hash,display_name,role,student_verified,local_login_enabled) SELECT "+quote('external_'+suffix)+",password_hash,'M6 isolated student','STUDENT',TRUE,FALSE FROM identity_user WHERE id=1;")
        user=int(sql('SELECT id FROM identity_user WHERE username='+quote('external_'+suffix)+';'))
        env=dict(os.environ,QIXU_DB_USERNAME=os.environ['QIXU_TEST_DB_USER'],QIXU_DB_PASSWORD=os.environ['QIXU_TEST_DB_PASSWORD'])
        enrollment=subprocess.run([sys.executable,str(ROOT/'scripts/enroll_external_identity.py'),'--mysql',a.mysql,'--schema',qp.path[1:],'--subject',str(upstream_id),'--user-id',str(user),'--expected-auth-version','1','--issuer-uri','http://127.0.0.1:6970'+AUTH,'--allow-loopback-http','--action','enable','--external-only','--reason','M6 explicit isolated test enrollment'],env=env,capture_output=True,text=True,timeout=20)
        if enrollment.returncode:raise RuntimeError('Explicit isolated binding failed')
        facts['identity']['enrollment_exit']=enrollment.returncode;ticket=dark_login(account);profile=request('GET','/user/auth',token=ticket,darkroom=True)
        facts['identity']['upstream_role']=profile['userRole'];token=exchange(ticket);session=request('GET','/api/v1/auth/session',token=token);facts['identity']['local_role']=session['actor']['role']
        request('GET','/api/v1/admin/operations',token=token,expected_status=403);facts['identity']['management_status']=facts['requests'][-1]['status']
        request('POST','/api/v1/auth/login',{'username':'external_'+suffix,'password':'qixu-demo','mode':'BEARER'},expected_status=401);facts['identity']['local_password_status']=facts['requests'][-1]['status']
        now=utc().replace(microsecond=0);body={'spaceId':2001,'startsAt':iso(now+timedelta(minutes=10)),'endsAt':iso(now+timedelta(minutes=40))};key='m6_lost_'+suffix;discard={}
        class LostResponse(http.server.BaseHTTPRequestHandler):
            def log_message(self,*_):pass
            def do_POST(self):
                raw=self.rfile.read(int(self.headers.get('Content-Length','0')))
                req=urllib.request.Request(base+self.path,data=raw,headers={'Content-Type':'application/json','Authorization':self.headers['Authorization'],'Idempotency-Key':self.headers['Idempotency-Key']},method='POST')
                try:
                    with opener.open(req,timeout=12) as response:discard['status']=response.status;discard['result']=json.load(response)['data']
                except Exception:discard['status']=-1
                finally:self.close_connection=True;self.connection.shutdown(socket.SHUT_RDWR);self.connection.close()
        proxy=http.server.HTTPServer(('127.0.0.1',0),LostResponse);thread=threading.Thread(target=proxy.handle_request,daemon=True);thread.start()
        try:
            try:request('POST','/api/v1/reservations',body,token,key,url_override='http://127.0.0.1:'+str(proxy.server_port)+'/api/v1/reservations');facts['recovery']['client_unknown']=False
            except (OSError,ValueError,http.client.HTTPException):facts['recovery']['client_unknown']=True
            thread.join(15)
        finally:proxy.server_close()
        facts['recovery']['response_discarded_status']=discard.get('status');original=discard['result'];rid=original['id']
        facts['recovery']['pending_before_restart']=int(sql(f"SELECT COUNT(*) FROM notification_outbox WHERE entity_type='SHORT' AND entity_id={rid} AND delivered_at IS NULL;"))
        old_pid=qchild.pid;stop(qchild);qenv['QIXU_TASKS_ENABLED']='true';qchild=start(qjar,qenv,'qixu-recovery');ready(qchild);facts['recovery']['distinct_restart_pid']=old_pid!=qchild.pid
        receipt=request('GET','/api/v1/receipts/'+key,token=token);facts['recovery']['receipt_equal']=receipt['result']==original;facts['recovery']['replay_equal']=request('POST','/api/v1/reservations',body,token,key)==original
        changed=dict(body,spaceId=2002);request('POST','/api/v1/reservations',changed,token,key,expected_status=409);facts['recovery']['changed_body_status']=facts['requests'][-1]['status'];other=local('student2');request('GET','/api/v1/receipts/'+key,token=other,expected_status=404);facts['recovery']['foreign_receipt_status']=facts['requests'][-1]['status']
        poll(lambda:int(sql(f"SELECT COUNT(*) FROM inbox i JOIN notification_outbox o ON o.id=i.outbox_id WHERE o.entity_type='SHORT' AND o.entity_id={rid};"))==1,25)
        query=f"SELECT (SELECT COUNT(*) FROM short_reservation WHERE id={rid}), (SELECT COUNT(*) FROM idempotency_receipt WHERE actor_id={user} AND request_key='{key}'),(SELECT COUNT(*) FROM audit_entry WHERE action='SHORT_CREATED' AND entity_id='{rid}'),(SELECT COUNT(*) FROM notification_outbox WHERE entity_type='SHORT' AND entity_id={rid}),(SELECT COUNT(*) FROM inbox i JOIN notification_outbox o ON o.id=i.outbox_id WHERE o.entity_type='SHORT' AND o.entity_id={rid}),(SELECT COUNT(*) FROM inbox i JOIN notification_outbox o ON o.id=i.outbox_id WHERE o.entity_type='SHORT' AND o.entity_id={rid} AND i.read_at IS NULL);"
        counts=list(map(int,sql(query).split('\t')));facts['recovery'].update(dict(zip(['reservations','receipts','audit_events','outbox_events','inbox_after_restart','unread_after_restart'],counts)));time.sleep(6)
        facts['recovery']['inbox_after_repeat']=int(sql(f"SELECT COUNT(*) FROM inbox i JOIN notification_outbox o ON o.id=i.outbox_id WHERE o.entity_type='SHORT' AND o.entity_id={rid};"))
        stop(qchild);qenv['QIXU_TASKS_ENABLED']='false';qchild=start(qjar,qenv,'qixu-freeze');ready(qchild);admin=local('admin1');token=exchange(ticket);other=local('student2')
        now=utc().replace(microsecond=0);close=now+timedelta(seconds=40);freeze_deadline=close+timedelta(seconds=30);random_at=close+timedelta(seconds=45);result_deadline=close+timedelta(minutes=5);confirm=result_deadline+timedelta(minutes=30);cycle=confirm+timedelta(minutes=10)
        batch=request('POST','/api/v1/preparation-batches',{'title':'M6 frozen restart '+suffix,'purpose':'OTHER','startsAt':iso(cycle),'endsAt':iso(cycle+timedelta(days=2)),'opensAt':iso(now-timedelta(seconds=10)),'closesAt':iso(close),'freezeDeadline':iso(freeze_deadline),'randomAt':iso(random_at),'resultDeadline':iso(result_deadline),'confirmationDeadline':iso(confirm),'promotionUntil':iso(cycle+timedelta(days=2)),'promotionSeconds':600,'seatIds':[2000,2100]},admin,'m6_batch_'+suffix);bid=batch['id']
        request('POST',f'/api/v1/preparation-batches/{bid}/application',{'version':0,'preferences':[{'seatId':2100,'rank':1},{'seatId':2000,'rank':2}],'keepWaitlist':True},token,'m6_apply_a_'+suffix)
        request('POST',f'/api/v1/preparation-batches/{bid}/application',{'version':0,'preferences':[{'seatId':2100,'rank':1}],'keepWaitlist':True},other,'m6_apply_b_'+suffix)
        print(json.dumps({'identity':identity,'phase':'WAITING_FOR_FIXED_FREEZE','closes_at':iso(close)}),flush=True)
        while utc()<close+timedelta(milliseconds=150):time.sleep(.25)
        request('POST',f'/api/v1/preparation-batches/{bid}/actions',{'version':1,'action':'FREEZE'},admin,'m6_freeze_'+suffix);frozen=request('GET',f"/api/public/batches/{batch['public_id']}/verification");write(out/'frozen-packet.json',frozen)
        stop(qchild);qenv['QIXU_TASKS_ENABLED']='true';qchild=start(qjar,qenv,'qixu-frozen-recovery');ready(qchild)
        packet_path=f"/api/public/batches/{batch['public_id']}/verification";packet={}
        def result_ready():
            nonlocal packet
            packet=request('GET',packet_path,record=False);return published_result(packet)
        poll(result_ready,180);write(out/'public-packet.json',packet)
        facts['allocation']['frozen_hash_unchanged']=frozen['input_hash']==packet['input_hash'];facts['allocation']['frozen_round_unchanged']=json.loads(frozen['input_bytes'])['source']['round']==packet['result']['proof']['round']
        counts=map(int,sql(f"SELECT (SELECT COUNT(*) FROM allocation_result WHERE batch_id={bid}),(SELECT COUNT(*) FROM allocation_outcome WHERE batch_id={bid}),(SELECT COUNT(*) FROM notification_outbox WHERE event_key='batch:{bid}:result');").split('\t'));facts['allocation'].update(dict(zip(['formal_results','outcomes','result_notices'],counts)))
        fresh=out/'fresh-recompute';fresh.mkdir();archive=out/'source.zip'
        with archive.open('xb') as stream:subprocess.run(['git','archive','--format=zip',source],cwd=ROOT,stdout=stream,check=True)
        with zipfile.ZipFile(archive) as z:
            for member in z.infolist():
                if not (fresh/member.filename).resolve().is_relative_to(fresh.resolve()):raise RuntimeError('Archive escaped fresh checkout')
            z.extractall(fresh)
        install=subprocess.run([a.npm,'ci','--ignore-scripts','--no-audit','--no-fund'],cwd=fresh/'randomness',capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=100)
        (out/'fresh-npm.log').write_text(install.stdout+install.stderr,encoding='utf-8')
        if install.returncode:raise RuntimeError('Fresh fixed verifier dependency installation failed')
        reproduce=subprocess.run([sys.executable,str(fresh/'scripts/verify_allocation.py'),str(out/'public-packet.json'),'--node',a.node],cwd=fresh,capture_output=True,text=True,encoding='utf-8',timeout=30)
        (out/'reproduce.stdout').write_text(reproduce.stdout,encoding='utf-8');(out/'reproduce.stderr').write_text(reproduce.stderr,encoding='utf-8')
        if reproduce.returncode:raise RuntimeError('Fresh independent public-byte reproduction failed')
        facts['reproduction']=json.loads(reproduce.stdout);facts['reproduction'].update({'source_archive_sha256':digest(archive),'fresh_dependency_exit':install.returncode})
        stop(dchild);request('GET','/api/v1/auth/session',token=token,expected_status=503);facts['identity']['outage_status']=facts['requests'][-1]['status'];independent=local('student2');request('GET','/api/v1/spaces',token=independent);facts['identity']['local_independent_status']=facts['requests'][-1]['status']
        dchild=start(djar,denv,'darkroom-restored');ready(dchild,True);request('GET','/api/v1/auth/session',token=token);facts['identity']['restored_status']=facts['requests'][-1]['status']
        sql(f'UPDATE user SET auth_version=auth_version+1,account_status=1,is_login=1 WHERE id={upstream_id};',True)
        request('GET','/api/v1/auth/session',token=token,expected_status=401);facts['identity']['revoked_status']=facts['requests'][-1]['status']
        sql(f'UPDATE user SET account_status=0,is_login=0 WHERE id={upstream_id};',True);new_ticket=dark_login(account);new_token=exchange(new_ticket);facts['identity']['new_ticket_receipt_equal']=request('GET','/api/v1/receipts/'+key,token=new_token)['result']==original
        print(json.dumps({'identity':identity,'phase':'REAL_IDENTITY_RESTART_AND_REPRODUCTION_COLLECTED'}),flush=True)
    except Exception as e:
        execution='ERROR';facts['failure']={'type':type(e).__name__,'message':str(e)[:400]};write(out/'failure.json',facts['failure'])
    finally:
        cleanup=[]
        for proc,owner in reversed(children):
            try:stop(proc);cleanup.append({'pid':proc.pid,'label':owner['label'],'stopped':proc.poll() is not None})
            except Exception as e:cleanup.append({'pid':proc.pid,'label':owner['label'],'stopped':False,'error_type':type(e).__name__});execution='ERROR'
        for log in logs:log.close()
        facts['cleanup']={'processes':cleanup,'all_owned_stopped':bool(cleanup) and all(row['stopped'] for row in cleanup)};write(out/'cleanup.json',facts['cleanup']);facts['source_sha']=git('rev-parse','HEAD');facts['source_clean']=not bool(git('status','--porcelain'))
        evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':COLLECTOR,'captured_at':iso(utc()),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coord),'collection_session_id':identity,'collector_role':'qixu-m6-installed-collector','coverage':'COMPLETE' if execution=='COMPLETED' else 'ERROR','normalization_semantics_version':COLLECTOR,'facts_digest':sha256_json(facts)}}}
        write(out/'evidence.json',evidence);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
        print(json.dumps({'identity':identity,'source_sha':source,'verdict':report['verdict'],'execution_status':execution,'boundary':facts['boundary']}),flush=True)
    return 0 if report['verdict']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())
