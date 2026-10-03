"""F15 owned database stall probe: real resource wait and borrowed TCP read budgets.

Never connects to host3306, never adopts a process by port, never uses shared DB
credentials. Restore operates only on a newly created schema inside this run.
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

ROOT=Path(__file__).resolve().parents[1]
COLLECTOR='qixu-m7-database/0.3'
MYSQL_PORT=6976;APP_PORT=6975;RELAY_PORT=6977;SCHEMA='qixu_fault'
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
                            if self.armed and self.target is None and payload[:1]==b'\x03' and b'FROM auth_session s JOIN identity_user' in payload[1:]:
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
    identity='m7-database-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True,exist_ok=False)
    data=out/'mysql-data';data.mkdir();runtime=out/'independent-world';runtime.mkdir();journal=runtime/'transactions.journal'
    ini=out/'my.ini';ini.write_bytes(('[mysqld]\nbasedir='+a.mysql_bin.parent.resolve().as_posix()+'\ndatadir='+data.as_posix()+'\nport='+str(MYSQL_PORT)+'\nbind-address=127.0.0.1\nmysqlx=0\ninnodb_buffer_pool_size=64M\nmax_connections=16\nskip-log-bin\nlog-error='+str(out/'mysql.private.log').replace('\\','/')+'\n').encode())
    installed=out/'qixu-api.jar';shutil.copy2(jar,installed)
    if digest(installed)!=producer['jar_sha256']:raise RuntimeError('Jar copy changed bytes')
    coord={'source_sha':source,'collector':COLLECTOR,'collector_sha256':digest(Path(__file__)),'producer_manifest_sha256':producer['manifest_sha256'],'jar_sha256':digest(installed),'database_contract_sha256':digest(ROOT/'docs/contracts/m7-database-budgets.md'),'migration_sha256':{f.name:digest(f) for f in sorted((ROOT/'backend/src/main/resources/db/migration').glob('*.sql'))},'mysql_exe_sha256':digest(binary['mysqld']),'java_exe_sha256':digest(a.java)}
    with zipfile.ZipFile(installed) as z:
        dependencies=[name.split('/')[-1] for name in z.namelist() if re.fullmatch(r'BOOT-INF/lib/(mysql-connector-j|HikariCP)-[^/]+\.jar',name)]
    if sorted(dependencies)!=['HikariCP-7.0.2.jar','mysql-connector-j-9.7.0.jar']:raise RuntimeError('Pinned driver/pool changed; contract must be reviewed')
    coord['dependencies']=dependencies
    spec={'id':'database','contract':{'id':'qixu-m7-database','version':'0.3'},'evidence_type':'qixu.m7.database','coordinates':coord,'projections':['source_sha','producer','normal','lock','io','configuration','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
    expected={'/source_sha':source,'/source_clean':True,'/producer/bytes_checked':True,'/normal/create_status':200,'/normal/receipt_equal':True,'/normal/unique_sql':True,'/lock/wait_observed':True,'/lock/response_within_15s':True,'/lock/status':503,'/lock/code':'DATABASE_UNAVAILABLE','/lock/no_business_while_guard_held':True,'/lock/no_marker_while_guard_held':True,'/lock/retry_status':200,'/lock/retry_receipt_equal':True,'/lock/unique_effect':True,'/io/borrowed_flow_hit':True,'/io/target_query_hit':True,'/io/response_within_35s':True,'/io/status':503,'/io/code':'DATABASE_UNAVAILABLE','/io/no_mutation':True,'/io/recovery_read_status':200,'/io/recovery_receipt_equal':True,'/configuration/app_lock_wait_values':[10],'/cleanup/all_owned_stopped':True}
    assertions=[{'id':'database-'+str(i),'severity':'HARD','left':{'requirement_id':'database','path':'/facts'+path},'operator':'eq','right':value} for i,(path,value) in enumerate(expected.items())]
    plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m7-database','version':3,'subject':{'id':'qixu-m7-database','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Do owned resource waits and borrowed network reads end within frozen observation budgets without duplicate business authority?','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:m7-database-collector','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'database','observation_spec_id':'database','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'database','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':600},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu F15 owned database stalls','consumers':['M7-database']},'reproduction_steps':['Fresh exact native0.17/Plan5 producer. Seal before own MySQL and opaque loopback relay. Normal booking; real floor lock wait with15s observation then release/same-key replay; drop only replies on existing borrowed connection with35s observation, then remove fault and verify authoritative reads.'],'cleanup_steps':['Only verified Popen child handles, exe and data-directory/jar coordinates may be stopped.','Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched.']})
    write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coord)
    facts={'source_sha':source,'producer':producer,'normal':{},'lock':{},'io':{},'configuration':{},'requests':[],'boundary':'OWN_RESOURCE_WAIT_AND_READ_RESPONSE_DROP_NOT_COMMIT_DROP_MULTI_NODE_OR_PRODUCTION_SLA'}
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
        env.update({'QIXU_DB_URL':f'jdbc:mysql://127.0.0.1:{RELAY_PORT}/{SCHEMA}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8&sslMode=DISABLED&useServerPrepStmts=false&serverRSAPublicKeyFile={urllib.parse.quote((data/'public_key.pem').as_posix())}','QIXU_DB_USERNAME':'qixu_fault_app','QIXU_DB_PASSWORD':app_password,'QIXU_PORT':str(APP_PORT),'QIXU_BIND':'127.0.0.1','SPRING_PROFILES_ACTIVE':'demo','QIXU_TASKS_ENABLED':'false','QIXU_COOKIE_SECURE':'false','QIXU_RECOVERY_JOURNAL':str(journal)})
        app=launch([str(a.java.resolve()),'-jar',str(installed)],'qixu',str(installed),env);limit=time.monotonic()+55
        while time.monotonic()<limit:
            if app.poll() is not None:raise RuntimeError('Owned app exited before health observation')
            try:
                status,value=request('GET','/api/health')
                if status in [200,503]:return status,value
            except (urllib.error.URLError,OSError,ValueError,RuntimeError):time.sleep(.5)
        raise RuntimeError('Owned app health budget exhausted')
    try:
        progress('SEALED_BEFORE_OWNED_INSTANCE_INITIALIZATION')
        with (out/'initialize.private.log').open('xb') as log:
            x=subprocess.run([str(binary['mysqld']),'--defaults-file='+str(ini),'--initialize-insecure'],stdout=log,stderr=subprocess.STDOUT,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
        if x.returncode:raise RuntimeError('Owned MySQL initialization failed')
        db_start();password=secrets.token_hex(24)
        sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+password+"';CREATE DATABASE `qixu_fault` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;CREATE USER 'qixu_fault_app'@'127.0.0.1' IDENTIFIED BY '"+app_password+"';GRANT ALL PRIVILEGES ON `qixu_fault`.* TO 'qixu_fault_app'@'127.0.0.1';")
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
        facts['normal']['unique_sql']=sql("SELECT (SELECT COUNT(*) FROM qixu_fault.short_reservation WHERE space_id=2000),(SELECT COUNT(*) FROM qixu_fault.idempotency_receipt WHERE request_key='m7_database_control');")=='1\t1'
        variables=sql("SELECT DISTINCT v.VARIABLE_VALUE FROM performance_schema.variables_by_thread v JOIN performance_schema.threads t ON t.THREAD_ID=v.THREAD_ID WHERE t.PROCESSLIST_USER='qixu_fault_app' AND v.VARIABLE_NAME='innodb_lock_wait_timeout' ORDER BY v.VARIABLE_VALUE;")
        if not variables:raise RuntimeError('Cannot independently observe app session lock budgets')
        facts['configuration']['app_lock_wait_values']=[int(x) for x in variables.splitlines()]
        progress('NORMAL_CONTROL_READY_REAL_LOCK_NEXT')
        guard=launch([str(binary['mysql']),'--protocol=TCP','-h','127.0.0.1','-P',str(MYSQL_PORT),'-u','root','--batch','--unbuffered','--skip-column-names'],'guard',str(MYSQL_PORT),dict(os.environ,MYSQL_PWD=root_password),True)
        guard.stdin.write(b"START TRANSACTION;SELECT id FROM qixu_fault.floor WHERE id=100 FOR UPDATE;SELECT 'GUARD_READY';\n");guard.stdin.flush()
        guard_log=out/(str(len(children)-1)+'-guard.private.log');limit=time.monotonic()+8
        while b'GUARD_READY' not in guard_log.read_bytes():
            if guard.poll() is not None or time.monotonic()>limit:raise RuntimeError('Guard did not establish its transaction')
            time.sleep(.1)
        marker_before=int(sql('SELECT COUNT(*) FROM qixu_fault.recovery_marker;'));executor=concurrent.futures.ThreadPoolExecutor(max_workers=1)
        queued={'spaceId':2001,'startsAt':start.isoformat(),'endsAt':end.isoformat()};queued_key='m7_database_queued';started=threading.Event()
        future=executor.submit(request,'POST','/api/v1/reservations',queued,tokens[1],queued_key,75,started)
        if not started.wait(8):raise RuntimeError('HTTP probe did not start')
        begin=time.monotonic();wait_observed=False
        while time.monotonic()-begin<15 and not future.done():
            n=int(sql("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks d ON w.REQUESTING_ENGINE_LOCK_ID=d.ENGINE_LOCK_ID WHERE d.OBJECT_SCHEMA='qixu_fault' AND d.OBJECT_NAME='floor';"))
            wait_observed=wait_observed or n>0;time.sleep(.1)
        facts['lock']['wait_observed']=wait_observed
        if not wait_observed:raise RuntimeError('No actual MySQL floor wait observed')
        status,value=future.result() if future.done() else (-1,{})
        facts['lock'].update({'response_within_15s':future.done() and value.get('_observer_elapsed',999)<=15,'status':status,'code':value.get('error',{}).get('code'),'observed_elapsed':value.get('_observer_elapsed'),'window_elapsed':time.monotonic()-begin})
        facts['lock']['no_business_while_guard_held']=sql("SELECT (SELECT COUNT(*) FROM qixu_fault.short_reservation WHERE space_id=2001),(SELECT COUNT(*) FROM qixu_fault.idempotency_receipt WHERE request_key='m7_database_queued');")=='0\t0'
        facts['lock']['no_marker_while_guard_held']=int(sql('SELECT COUNT(*) FROM qixu_fault.recovery_marker;'))==marker_before
        verify_child(guard);guard.stdin.write(b"ROLLBACK;SELECT 'GUARD_RELEASED';\n");guard.stdin.flush();guard.stdin.close();guard.wait(8)
        if guard.returncode!=0:raise RuntimeError('Guard did not exit cleanly after release')
        status,value=future.result(timeout=40);write(out/'lock-after-release.private.json',{'status':status,'body':value})
        status,value=request('POST','/api/v1/reservations',queued,tokens[1],queued_key);facts['lock']['retry_status']=status;retry=value.get('data')
        status,value=request('POST','/api/v1/reservations',queued,tokens[1],queued_key)
        facts['lock']['retry_receipt_equal']=status==200 and retry is not None and value.get('data')==retry
        facts['lock']['unique_effect']=sql("SELECT (SELECT COUNT(*) FROM qixu_fault.short_reservation WHERE space_id=2001),(SELECT COUNT(*) FROM qixu_fault.idempotency_receipt WHERE request_key='m7_database_queued');")=='1\t1'
        progress('LOCK_PROBE_RECORDED_BORROWED_READ_NEXT')
        status,_=request('GET','/api/v1/spaces/2000',token=tokens[0])
        if status!=200:raise RuntimeError('Read control before network fault failed')
        before=relay.snapshot();old={row['id']:row for row in before if not row['closed']}
        if not old:raise RuntimeError('No live preexisting pooled relay connection')
        counts_before=sql('SELECT (SELECT COUNT(*) FROM qixu_fault.short_reservation),(SELECT COUNT(*) FROM qixu_fault.idempotency_receipt),(SELECT COUNT(*) FROM qixu_fault.recovery_marker);')
        relay.arm(True);started=threading.Event();future=executor.submit(request,'GET','/api/v1/spaces/2000',None,tokens[0],None,85,started)
        if not started.wait(8):raise RuntimeError('Network probe did not start')
        begin=time.monotonic()
        while time.monotonic()-begin<35 and not future.done():time.sleep(.1)
        status,value=future.result() if future.done() else (-1,{})
        after=relay.snapshot();hit=any(row['id'] in old and row['client_bytes']>old[row['id']]['client_bytes'] and row['dropped_bytes']>old[row['id']]['dropped_bytes'] for row in after)
        facts['io'].update({'borrowed_flow_hit':hit,'target_query_hit':any(row['target_query_hit'] and row['id'] in old for row in after),'response_within_35s':future.done() and value.get('_observer_elapsed',999)<=35,'status':status,'code':value.get('error',{}).get('code'),'observed_elapsed':value.get('_observer_elapsed'),'window_elapsed':time.monotonic()-begin,'relay_before':before,'relay_after':after})
        if not hit:raise RuntimeError('Fault did not hit an established connection')
        facts['io']['no_mutation']=sql('SELECT (SELECT COUNT(*) FROM qixu_fault.short_reservation),(SELECT COUNT(*) FROM qixu_fault.idempotency_receipt),(SELECT COUNT(*) FROM qixu_fault.recovery_marker);')==counts_before
        relay.arm(False);facts['io']['recovery_required_owned_restart']=not future.done()
        if not future.done():stop(app);future.result(timeout=10);app_start()
        status,_=request('GET','/api/v1/spaces/2000',token=tokens[0]);facts['io']['recovery_read_status']=status
        status,value=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['io']['recovery_receipt_equal']=status==200 and value.get('data',{}).get('result')==original
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
    ev={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':COLLECTOR,'captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coord),'collection_session_id':identity,'collector_role':'qixu-m7-database-collector','coverage':'COMPLETE' if completed else 'ERROR','normalization_semantics_version':COLLECTOR,'facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',ev);result=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
    print(json.dumps({'identity':identity,'source_sha':source,'verdict':result['verdict'],'execution':execution,'lock':facts['lock'],'io':{k:v for k,v in facts['io'].items() if not k.startswith('relay')},'cleanup':facts['cleanup']['all_owned_stopped']}),flush=True)
    return 0 if result['verdict']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())
