"""F12 owned COMMIT reply-loss probe: real commit, quarantine and same-ledger restart.

Never connects to host3306, never adopts a process by port, never uses shared DB
credentials. Probe operates only on a newly created schema inside this run.
"""
from __future__ import annotations
import threading,concurrent.futures,zipfile
import argparse,hashlib,json,os,re,secrets,shutil,socket,subprocess,time,urllib.request,urllib.error,uuid
from datetime import datetime,timedelta,timezone
from pathlib import Path
from importlib.metadata import version
from veritrail.acceptance_plan import observation_spec_digest,seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
from m7_producer_binding import bind,digest
from veritrail_m7_restore import independent_ledger

ROOT=Path(__file__).resolve().parents[1]
COLLECTOR='qixu-m7-commit/0.1'
MYSQL_PORT=6976;APP_PORT=6975;RELAY_PORT=6977;SCHEMA='qixu_commit'
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
def write(path,value):
    with Path(path).open('x',encoding='utf-8',newline='\n') as f:json.dump(value,f,ensure_ascii=False,sort_keys=True,indent=2);f.write('\n')

class Relay:
    """Opaque TCP relay. Drop only server replies; never inspect secrets/TLS."""
    def __init__(self):
        self.lock=threading.Lock();self.armed=False;self.target=None;self.closed=False;self.rows=[];self.sockets=[];self.threads=[]
        self.server=socket.socket();self.server.bind(('127.0.0.1',RELAY_PORT));self.server.listen();self.server.settimeout(.2)
        self.acceptor=threading.Thread(target=self.accept,daemon=True);self.acceptor.start()
    def accept(self):
        while not self.closed:
            try:client,_=self.server.accept()
            except socket.timeout:continue
            except OSError:return
            try:upstream=socket.create_connection(('127.0.0.1',MYSQL_PORT),timeout=3)
            except OSError:client.close();continue
            client.settimeout(.2);upstream.settimeout(.2)
            with self.lock:
                row={'id':len(self.rows),'client_bytes':0,'server_bytes':0,'dropped_bytes':0,'target_query_hit':False,'closed':False};self.rows.append(row);self.sockets.extend([client,upstream])
            for src,dst,direction in [(client,upstream,'client'),(upstream,client,'server')]:
                t=threading.Thread(target=self.pump,args=(src,dst,row,direction),daemon=True);self.threads.append(t);t.start()
    def pump(self,src,dst,row,direction):
        frames=bytearray()
        try:
            while not self.closed:
                try:packet=src.recv(65536)
                except socket.timeout:continue
                if not packet:return
                with self.lock:
                    row[direction+'_bytes']+=len(packet)
                    if direction=='client':
                        frames.extend(packet)
                        while len(frames)>=4:
                            size=int.from_bytes(frames[:3],'little')
                            if size>1048576:raise OSError('Probe packet budget exhausted')
                            if len(frames)<size+4:break
                            payload=bytes(frames[4:size+4]);del frames[:size+4]
                            query=payload[1:]
                            if query[:2]==b'\x00\x01':query=query[2:]
                            if self.armed and self.target is None and payload[:1]==b'\x03' and query.strip().upper()==b'COMMIT':
                                self.target=row['id'];row['target_query_hit']=True
                    drop=direction=='server' and self.armed and self.target==row['id']
                    if drop:row['dropped_bytes']+=len(packet)
                if not drop:dst.sendall(packet)
        except OSError:pass
        finally:
            with self.lock:row['closed']=True
            for s in [src,dst]:
                try:s.shutdown(socket.SHUT_RDWR)
                except OSError:pass
                s.close()
    def snapshot(self):
        with self.lock:return [dict(row) for row in self.rows]
    def arm(self,value):
        with self.lock:self.armed=value;self.target=None
    def close(self):
        self.closed=True;self.server.close()
        for s in self.sockets:
            try:s.shutdown(socket.SHUT_RDWR)
            except OSError:pass
            s.close()
        self.acceptor.join(2)
        for t in self.threads:t.join(2)
        return not self.acceptor.is_alive() and all(not t.is_alive() for t in self.threads)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--producer-bundle',type=Path,required=True);p.add_argument('--java',type=Path,required=True);p.add_argument('--mysql-bin',type=Path,required=True)
    a=p.parse_args();source=git('rev-parse','HEAD')
    if version('veritrail')!='0.13.0' or git('status','--porcelain') or os.name!='nt':raise RuntimeError('Windows, Core0.13.0 and exact clean source required')
    binary={name:(a.mysql_bin/(name+'.exe')).resolve() for name in ['mysql','mysqld','mysqldump']}
    if not all(path.is_file() for path in [a.java,*binary.values()]):raise RuntimeError('Pinned executable missing')
    jar=ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar';producer=bind(a.producer_bundle,source,jar)
    for port in [MYSQL_PORT,APP_PORT,RELAY_PORT]:
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    now=datetime.now(timezone.utc);start=(now+timedelta(minutes=5)).replace(second=0,microsecond=0);end=start+timedelta(minutes=30)
    if start.astimezone(timezone(timedelta(hours=8))).hour<8 or end.astimezone(timezone(timedelta(hours=8))).hour>=22:raise RuntimeError('Real-clock probe requires the declared 08:00-22:00 booking window')
    identity='m7-commit-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True,exist_ok=False)
    data=out/'mysql-data';data.mkdir();runtime=out/'independent-world';runtime.mkdir();journal=runtime/'transactions.journal'
    ini=out/'my.ini';ini.write_bytes(('[mysqld]\nbasedir='+a.mysql_bin.parent.resolve().as_posix()+'\ndatadir='+data.as_posix()+'\nport='+str(MYSQL_PORT)+'\nbind-address=127.0.0.1\nmysqlx=0\ninnodb_buffer_pool_size=64M\nmax_connections=16\nskip-log-bin\nlog-error='+str(out/'mysql.private.log').replace('\\','/')+'\n').encode())
    installed=out/'qixu-api.jar';shutil.copy2(jar,installed)
    if digest(installed)!=producer['jar_sha256']:raise RuntimeError('Jar copy changed bytes')
    coord={'source_sha':source,'collector':COLLECTOR,'collector_sha256':digest(Path(__file__)),'producer_manifest_sha256':producer['manifest_sha256'],'jar_sha256':digest(installed),'commit_contract_sha256':digest(ROOT/'docs/contracts/m7-commit-reply.md'),'database_contract_sha256':digest(ROOT/'docs/contracts/m7-database-budgets.md'),'ledger_observer_sha256':digest(ROOT/'scripts/veritrail_m7_restore.py'),'migration_sha256':{f.name:digest(f) for f in sorted((ROOT/'backend/src/main/resources/db/migration').glob('*.sql'))},'mysql_exe_sha256':digest(binary['mysqld']),'java_exe_sha256':digest(a.java)}
    with zipfile.ZipFile(installed) as z:
        dependencies=[name.split('/')[-1] for name in z.namelist() if re.fullmatch(r'BOOT-INF/lib/(mysql-connector-j|HikariCP)-[^/]+\.jar',name)]
    if sorted(dependencies)!=['HikariCP-7.0.2.jar','mysql-connector-j-9.7.0.jar']:raise RuntimeError('Pinned driver/pool changed; contract must be reviewed')
    coord['dependencies']=dependencies
    spec={'id':'commit','contract':{'id':'qixu-m7-commit','version':'0.1'},'evidence_type':'qixu.m7.commit','coordinates':coord,'projections':['source_sha','producer','normal','commit','quarantine','uncertain_ledger','recovery','configuration','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
    expected={'/source_sha':source,'/source_clean':True,'/producer/bytes_checked':True,'/normal/create_status':200,'/normal/receipt_equal':True,'/normal/unique_sql':True,
        '/commit/borrowed_flow_hit':True,'/commit/exact_commit_hit':True,'/commit/response_within_35s':True,'/commit/status':503,'/commit/code':'DATABASE_UNAVAILABLE',
        '/commit/unique_committed_effect':True,'/commit/one_new_marker':True,'/commit/generation_unchanged':True,
        '/quarantine/statuses':[503,503,503,503],'/quarantine/codes':['NOT_RECONCILED']*4,'/quarantine/sql_unchanged':True,
        '/uncertain_ledger/prepare_matches_committed_marker':True,'/uncertain_ledger/no_false_terminal':True,
        '/recovery/new_pid':True,'/recovery/health_status':200,'/recovery/generation_unchanged':True,'/recovery/receipt_equal':True,'/recovery/replay_equal':True,
        '/recovery/unique_effect':True,'/recovery/control_receipt_equal':True,'/recovery/complete_ledger_equal':True,'/recovery/one_recovered_commit':True,
        '/cleanup/all_owned_stopped':True}
    assertions=[{'id':'commit-'+str(i),'severity':'HARD','left':{'requirement_id':'commit','path':'/facts'+path},'operator':'eq','right':value} for i,(path,value) in enumerate(expected.items())]
    plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m7-commit','version':1,'subject':{'id':'qixu-m7-commit','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does actual committed SQL with a lost COMMIT reply quarantine live authority and recover the same receipt once from independent marker evidence?','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:m7-commit-collector','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'commit','observation_spec_id':'commit','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'commit','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':600},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu F12 owned COMMIT reply loss','consumers':['M7-commit']},'reproduction_steps':['Fresh exact native0.18/Plan6 producer. Seal before owned MySQL and loopback relay. Exact COMMIT response drop; independently read committed SQL, observe quarantine, stop owned app and validate pending ledger, restart same DB/ledger and replay original key, validate recovered complete ledger.'],'cleanup_steps':['Only verified Popen child handles, exe and data-directory/jar coordinates may be stopped.','Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched.']})
    write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coord)
    facts={'source_sha':source,'producer':producer,'normal':{},'commit':{},'quarantine':{},'uncertain_ledger':{},'recovery':{},'configuration':{},'requests':[],'boundary':'OWN_REAL_COMMIT_REPLY_LOSS_AND_SAME_DATABASE_RESTART_NOT_MULTI_NODE_POWER_LOSS_OR_PRODUCTION_SLA'}
    children=[];logs=[];db=None;app=None;guard=None;relay=None;executor=None;execution='COMPLETED';completed=False;root_password='';app_password=secrets.token_hex(24);sequence=0;counter=threading.Lock()
    def next_sequence():
        nonlocal sequence
        with counter:sequence+=1;return sequence
    def progress(phase):print(json.dumps({'identity':identity,'phase':phase}),flush=True)
    def launch(command,label,match,env=None,interactive=False):
        log=(out/(str(len(children))+'-'+label+'.private.log')).open('xb');logs.append(log)
        child=subprocess.Popen(command,cwd=out,env=env,stdout=log,stderr=subprocess.STDOUT,stdin=subprocess.PIPE if interactive else subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW)
        owner={'pid':child.pid,'label':label,'exe':str(Path(command[0]).resolve()),'match':match,'started_at':datetime.now(timezone.utc).isoformat()};children.append((child,owner));write(out/(str(len(children))+'-owner.private.json'),owner);return child
    def verify_child(child,port=None):
        if child is None or child.poll() is not None:raise RuntimeError('Owned child absent')
        owner=next(o for c,o in children if c is child)
        code=f"Get-CimInstance Win32_Process -Filter 'ProcessId={child.pid}' | Select-Object ProcessId,ExecutablePath,CommandLine,CreationDate | ConvertTo-Json -Compress"
        x=subprocess.run(['powershell','-NoProfile','-Command',code],capture_output=True,text=True,encoding='utf-8',timeout=10,check=True);current=json.loads(x.stdout)
        if str(current.get('ExecutablePath','')).lower()!=owner['exe'].lower() or owner['match'].lower() not in current.get('CommandLine','').lower():raise RuntimeError('Child ownership changed')
        if port:
            x=subprocess.run(['powershell','-NoProfile','-Command',f"Get-NetTCPConnection -LocalPort {port} -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess | ConvertTo-Json -Compress"],capture_output=True,text=True,encoding='utf-8',timeout=10)
            owners=json.loads(x.stdout);owners=owners if isinstance(owners,list) else [owners]
            if child.pid not in owners or any(pid!=child.pid for pid in owners):raise RuntimeError('Owned port belongs to another process')
        return current
    def stop(child):
        if child is None or child.poll() is not None:return
        current=verify_child(child);write(out/('stop-'+str(child.pid)+'.private.json'),current);child.terminate()
        try:child.wait(timeout=20)
        except subprocess.TimeoutExpired:verify_child(child);child.kill();child.wait(timeout=5)
    def sql(statement):
        verify_child(db,MYSQL_PORT);seq=next_sequence()
        x=subprocess.run([str(binary['mysql']),'--protocol=TCP','-h','127.0.0.1','-P',str(MYSQL_PORT),'-u','root','--batch','--skip-column-names','--default-character-set=utf8mb4'],input=statement.encode(),capture_output=True,env=dict(os.environ,MYSQL_PWD=root_password),timeout=20)
        if x.returncode:(out/('sql-'+str(seq)+'.private.stderr')).write_bytes(x.stderr);raise RuntimeError('Owned SQL failed; private stderr retained')
        return x.stdout.decode('utf-8').strip()
    def db_start():
        nonlocal db
        db=launch([str(binary['mysqld']),'--defaults-file='+str(ini),'--no-monitor'],'mysql',str(ini));limit=time.monotonic()+45
        while time.monotonic()<limit:
            if db.poll() is not None:raise RuntimeError('Owned MySQL exited')
            try:sql('SELECT 1;');return
            except (OSError,ValueError,RuntimeError):time.sleep(.4)
        raise RuntimeError('Owned MySQL readiness budget exhausted')
    def request(method,path,body=None,token=None,key=None,timeout=10,started=None):
        verify_child(app,APP_PORT);seq=next_sequence();headers={}
        if token:headers['Authorization']='Bearer '+token
        if key:headers['Idempotency-Key']=key
        if body is not None:headers['Content-Type']='application/json'
        req=urllib.request.Request('http://127.0.0.1:'+str(APP_PORT)+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
        begin=time.monotonic()
        if started is not None:started.set()
        try:
            with opener.open(req,timeout=timeout) as response:status=response.status;value=json.load(response)
        except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
        except (OSError,urllib.error.URLError) as error:status=-1;value={'transport_error':type(error).__name__}
        value['_observer_elapsed']=time.monotonic()-begin
        write(out/('packet-'+str(seq)+'.private.json'),{'status':status,'body':value});facts['requests'].append({'method':method,'path':path,'status':status,'code':value.get('error',{}).get('code'),'elapsed':value['_observer_elapsed']});return status,value
    def app_start():
        nonlocal app
        env={k:v for k,v in os.environ.items() if not k.startswith(('QIXU_','MYSQL_','DEEPSEEK_'))}
        env.update({'QIXU_DB_URL':f'jdbc:mysql://127.0.0.1:{RELAY_PORT}/{SCHEMA}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8&sslMode=DISABLED&useServerPrepStmts=false&serverRSAPublicKeyFile={urllib.parse.quote((data/"public_key.pem").as_posix())}','QIXU_DB_USERNAME':'qixu_commit_app','QIXU_DB_PASSWORD':app_password,'QIXU_PORT':str(APP_PORT),'QIXU_BIND':'127.0.0.1','SPRING_PROFILES_ACTIVE':'demo','QIXU_TASKS_ENABLED':'false','QIXU_COOKIE_SECURE':'false','QIXU_RECOVERY_JOURNAL':str(journal)})
        app=launch([str(a.java.resolve()),'-jar',str(installed)],'qixu',str(installed),env);limit=time.monotonic()+55
        while time.monotonic()<limit:
            if app.poll() is not None:raise RuntimeError('Owned app exited before health observation')
            try:
                status,value=request('GET','/api/health')
                if status==200 or (status==503 and value.get('error',{}).get('code')=='NOT_RECONCILED'):return status,value
            except (urllib.error.URLError,OSError,ValueError,RuntimeError):time.sleep(.5)
        raise RuntimeError('Owned app health budget exhausted')
    try:
        progress('SEALED_BEFORE_OWNED_INSTANCE_INITIALIZATION')
        with (out/'initialize.private.log').open('xb') as log:
            x=subprocess.run([str(binary['mysqld']),'--defaults-file='+str(ini),'--initialize-insecure'],stdout=log,stderr=subprocess.STDOUT,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
        if x.returncode:raise RuntimeError('Owned MySQL initialization failed')
        db_start();password=secrets.token_hex(24)
        sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+password+"';CREATE DATABASE `qixu_commit` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;CREATE USER 'qixu_commit_app'@'127.0.0.1' IDENTIFIED BY '"+app_password+"';GRANT ALL PRIVILEGES ON `qixu_commit`.* TO 'qixu_commit_app'@'127.0.0.1';")
        root_password=password
        public_key=data/'public_key.pem'
        if not public_key.is_file():raise RuntimeError('Owned MySQL RSA public key absent')
        facts['configuration']['owned_rsa_public_key_sha256']=digest(public_key)
        relay=Relay();status,_=app_start()
        if status!=200:raise RuntimeError('Fresh owned candidate not ready')
        tokens=[]
        for user in ['student1','student2']:
            status,value=request('POST','/api/v1/auth/login',{'username':user,'password':'qixu-demo','mode':'BEARER'})
            if status!=200:raise RuntimeError('Fixture login failed')
            tokens.append(value['data']['token'])
        body={'spaceId':2000,'startsAt':start.isoformat(),'endsAt':end.isoformat()};key='m7_database_control'
        status,value=request('POST','/api/v1/reservations',body,tokens[0],key);facts['normal']['create_status']=status
        if status!=200:raise RuntimeError('Normal booking prerequisite rejected')
        original=value['data'];status,value=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['normal']['receipt_equal']=status==200 and value.get('data',{}).get('result')==original
        facts['normal']['unique_sql']=sql("SELECT (SELECT COUNT(*) FROM qixu_commit.short_reservation WHERE space_id=2000),(SELECT COUNT(*) FROM qixu_commit.idempotency_receipt WHERE request_key='m7_database_control');")=='1\t1'
        generation_before=sql('SELECT generation,baseline_hash FROM qixu_commit.recovery_generation ORDER BY id;')
        markers_before=set(sql('SELECT transaction_id FROM qixu_commit.recovery_marker ORDER BY transaction_id;').splitlines())
        lost_key='m7_commit_lost_response';lost={'spaceId':2002,'startsAt':end.isoformat(),'endsAt':(end+timedelta(minutes=30)).isoformat()}
        if (end+timedelta(minutes=30)).astimezone(timezone(timedelta(hours=8))).hour>=22:raise RuntimeError('Adjacent fixture interval outside declared opening window')
        status,_=request('GET','/api/v1/spaces/2002',token=tokens[0])
        if status!=200:raise RuntimeError('Adjacent seat normal prerequisite failed')
        before=relay.snapshot();old={row['id']:row for row in before if not row['closed']}
        if not old:raise RuntimeError('No preexisting pool connection')
        progress('NORMAL_CONTROL_READY_EXACT_COMMIT_DROP_NEXT')
        relay.arm(True);executor=concurrent.futures.ThreadPoolExecutor(max_workers=1);started=threading.Event()
        future=executor.submit(request,'POST','/api/v1/reservations',lost,tokens[0],lost_key,85,started)
        if not started.wait(8):raise RuntimeError('Commit fault request did not start')
        begin=time.monotonic()
        while time.monotonic()-begin<35 and not future.done():time.sleep(.1)
        status,value=future.result() if future.done() else (-1,{})
        after=relay.snapshot()
        hit=any(row['id'] in old and row['target_query_hit'] and row['client_bytes']>old[row['id']]['client_bytes'] and row['dropped_bytes']>old[row['id']]['dropped_bytes'] for row in after)
        facts['commit'].update({'borrowed_flow_hit':hit,'exact_commit_hit':any(row['id'] in old and row['target_query_hit'] for row in after),'response_within_35s':future.done() and value.get('_observer_elapsed',999)<=35,'status':status,'code':value.get('error',{}).get('code'),'elapsed':value.get('_observer_elapsed'),'relay_before':before,'relay_after':after})
        if not hit:raise RuntimeError('No actual COMMIT response loss observed')
        stored_text=sql("SELECT response_json FROM qixu_commit.idempotency_receipt WHERE request_key='m7_commit_lost_response';")
        if not stored_text:raise RuntimeError('Real commit is not proven by independent receipt SQL')
        stored=json.loads(stored_text);write(out/'stored-receipt.private.json',stored)
        effect_sql="SELECT (SELECT COUNT(*) FROM qixu_commit.short_reservation WHERE space_id=2002),(SELECT COUNT(*) FROM qixu_commit.idempotency_receipt WHERE request_key='m7_commit_lost_response'),(SELECT COUNT(*) FROM qixu_commit.notification_outbox WHERE entity_type='SHORT' AND entity_id="+str(stored['id'])+");"
        effects_before=sql(effect_sql);facts['commit']['unique_committed_effect']=effects_before=='1\t1\t1'
        markers_after=set(sql('SELECT transaction_id FROM qixu_commit.recovery_marker ORDER BY transaction_id;').splitlines());new_markers=markers_after-markers_before
        facts['commit']['one_new_marker']=len(new_markers)==1 and markers_before.issubset(markers_after)
        if len(new_markers)!=1:raise RuntimeError('Cannot independently identify committed transaction marker')
        transaction=next(iter(new_markers));facts['commit']['transaction_coordinate_sha256']=hashlib.sha256(transaction.encode()).hexdigest()
        facts['commit']['generation_unchanged']=sql('SELECT generation,baseline_hash FROM qixu_commit.recovery_generation ORDER BY id;')==generation_before
        relay.arm(False)
        if not future.done():stop(app);future.result(timeout=10);raise RuntimeError('Commit response budget exhausted before quarantine observations')
        checks=[request('GET','/api/health'),request('POST','/api/v1/reservations',lost,tokens[1],'m7_commit_competitor'),request('GET','/api/v1/receipts/'+lost_key,token=tokens[0]),request('POST','/api/v1/reservations',lost,tokens[0],lost_key)]
        facts['quarantine']['statuses']=[status for status,_ in checks];facts['quarantine']['codes']=[value.get('error',{}).get('code') for _,value in checks]
        facts['quarantine']['sql_unchanged']=sql(effect_sql)==effects_before and set(sql('SELECT transaction_id FROM qixu_commit.recovery_marker ORDER BY transaction_id;').splitlines())==markers_after
        first_pid=app.pid;stop(app)
        generations=sql('SELECT generation,baseline_hash FROM qixu_commit.recovery_generation ORDER BY id;');marker_rows=sql('SELECT transaction_id,generation FROM qixu_commit.recovery_marker ORDER BY transaction_id LIMIT 200001;')
        control=independent_ledger(journal,generations,marker_rows)
        raw=journal.read_bytes();(out/'uncertain-ledger.private.txt').write_bytes(raw)
        events=[line.split('|') for line in raw.decode('ascii').splitlines()[1:]]
        states={}
        for event in events:states[event[2]]=event[1]
        pending={key for key,state in states.items() if state=='PREPARE'}
        facts['uncertain_ledger']={'strict_chain_control':control,'prepare_matches_committed_marker':pending=={transaction},'no_false_terminal':states.get(transaction)=='PREPARE'}
        progress('COMMIT_SQL_AND_QUARANTINE_RECORDED_RESTART_NEXT')
        status,_=app_start();facts['recovery']['health_status']=status;facts['recovery']['new_pid']=app.pid!=first_pid
        facts['recovery']['generation_unchanged']=sql('SELECT generation,baseline_hash FROM qixu_commit.recovery_generation ORDER BY id;')==generation_before
        status,value=request('GET','/api/v1/receipts/'+lost_key,token=tokens[0]);facts['recovery']['receipt_equal']=status==200 and value.get('data',{}).get('result')==stored
        status,value=request('POST','/api/v1/reservations',lost,tokens[0],lost_key);first=value.get('data')
        status,value=request('POST','/api/v1/reservations',lost,tokens[0],lost_key)
        facts['recovery']['replay_equal']=status==200 and first==stored and value.get('data')==stored
        facts['recovery']['unique_effect']=sql(effect_sql)=='1\t1\t1'
        status,value=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['recovery']['control_receipt_equal']=status==200 and value.get('data',{}).get('result')==original
        stop(app)
        final_ledger=independent_ledger(journal,sql('SELECT generation,baseline_hash FROM qixu_commit.recovery_generation ORDER BY id;'),sql('SELECT transaction_id,generation FROM qixu_commit.recovery_marker ORDER BY transaction_id LIMIT 200001;'))
        facts['recovery']['complete_ledger_equal']=final_ledger['equal'];facts['recovery']['ledger_control']=final_ledger
        final_raw=journal.read_bytes();(out/'recovered-ledger.private.txt').write_bytes(final_raw)
        facts['recovery']['one_recovered_commit']=sum(line.split('|')[1:3]==['RECOVER_COMMIT',transaction] for line in final_raw.decode('ascii').splitlines()[1:])==1
        completed=True;progress('PROBES_COMPLETE_CLEANUP_NEXT')
    except Exception as error:
        execution='ERROR';facts['collection_error']={'kind':type(error).__name__,'message':str(error)[:200]}
    finally:
        if guard is not None and guard.poll() is None:
            try:guard.stdin.write(b'ROLLBACK;\n');guard.stdin.flush();guard.stdin.close();guard.wait(5)
            except (OSError,ValueError,subprocess.TimeoutExpired):pass
        cleanup=[]
        for child,owner in reversed(children):
            try:stop(child);cleanup.append({'pid':owner['pid'],'label':owner['label'],'stopped':child.poll() is not None,'ownership_checked':True})
            except Exception as error:execution='ERROR';cleanup.append({'pid':owner['pid'],'stopped':False,'error':type(error).__name__})
        relay_stopped=True if relay is None else relay.close()
        if executor is not None:executor.shutdown(wait=True,cancel_futures=True)
        for log in logs:log.close()
        port_free={}
        for port in [APP_PORT,MYSQL_PORT,RELAY_PORT]:
            with socket.socket() as check:port_free[str(port)]=check.connect_ex(('127.0.0.1',port))!=0
        facts['cleanup']={'all_owned_stopped':all(row.get('stopped') for row in cleanup) and all(port_free.values()) and relay_stopped,'children':cleanup,'owned_ports_no_listener':port_free,'relay_threads_stopped':relay_stopped}
    facts['source_clean']=not bool(git('status','--porcelain'))
    ev={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':COLLECTOR,'captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coord),'collection_session_id':identity,'collector_role':'qixu-m7-commit-collector','coverage':'COMPLETE' if completed else 'ERROR','normalization_semantics_version':COLLECTOR,'facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',ev);result=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
    print(json.dumps({'identity':identity,'source_sha':source,'verdict':result['verdict'],'execution':execution,'commit':{k:v for k,v in facts['commit'].items() if not k.startswith('relay')},'quarantine':facts['quarantine'],'recovery':facts['recovery'],'cleanup':facts['cleanup']['all_owned_stopped']}),flush=True)
    return 0 if result['verdict']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())
