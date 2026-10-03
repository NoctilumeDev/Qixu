"""Bounded F12 probe: own physical MySQL process, logical T0 restore, independent world witness.

Never connects to host3306, never adopts a process by port, never uses shared DB
credentials. Restore operates only on a newly created schema inside this run.
"""
from __future__ import annotations
import argparse,hashlib,json,os,re,secrets,shutil,socket,subprocess,time,urllib.request,urllib.error,uuid
from datetime import datetime,timedelta,timezone
from pathlib import Path
from importlib.metadata import version
from veritrail.acceptance_plan import observation_spec_digest,seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
from m7_producer_binding import bind,digest

ROOT=Path(__file__).resolve().parents[1]
COLLECTOR='qixu-m7-restore/0.7'
MYSQL_PORT=6976;APP_PORT=6975;SCHEMA='qixu_restore'
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
def write(path,value):
    with Path(path).open('x',encoding='utf-8',newline='\n') as f:json.dump(value,f,ensure_ascii=False,sort_keys=True,indent=2);f.write('\n')

def independent_ledger(path,generation_rows,marker_rows):
    """Read the stopped instance's complete chain, independently of product code.

    Unreadable/malformed inputs are observation errors, never empty marker sets.
    """
    with path.open('rb') as stream:raw=stream.read(32*1024*1024+1)
    if not raw or len(raw)>32*1024*1024 or not raw.endswith(b'\n'):raise RuntimeError('Independent ledger byte budget/termination invalid')
    text=raw.decode('ascii')
    if not re.fullmatch(r'[A-Za-z0-9_|\-\n]+',text):raise RuntimeError('Independent ledger alphabet invalid')
    lines=text[:-1].split('\n')
    if len(lines)>200_000:raise RuntimeError('Independent ledger event budget exhausted')
    states={};chain='0'*64;generation=baseline=None
    for index,line in enumerate(lines):
        fields=line.split('|')
        if len(fields)!=6 or fields[0]!=str(index) or fields[4]!=chain or fields[5]!=hashlib.sha256('|'.join(fields[:5]).encode('ascii')).hexdigest():raise RuntimeError('Independent ledger chain invalid')
        if not re.fullmatch(r'[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}',fields[2]):raise RuntimeError('Independent ledger UUID invalid')
        if index==0:
            if fields[1]!='QXJ1' or not re.fullmatch(r'[a-f0-9]{64}',fields[3]):raise RuntimeError('Independent ledger header invalid')
            generation,baseline=fields[2:4]
        else:
            event,transaction=fields[1:3]
            if fields[3]!=generation:raise RuntimeError('Independent ledger generation changed')
            if event=='PREPARE':
                if transaction in states:raise RuntimeError('Independent ledger duplicate preparation')
                states[transaction]='PREPARE'
            elif event in ['COMMIT','ROLLBACK','RECOVER_COMMIT'] and states.get(transaction)=='PREPARE':states[transaction]='ROLLBACK' if event=='ROLLBACK' else 'COMMIT'
            else:raise RuntimeError('Independent ledger terminal invalid')
        chain=fields[5]
    generations=[line.split('\t') for line in generation_rows.splitlines()]
    markers=[line.split('\t') for line in marker_rows.splitlines()]
    if len(generations)!=1 or any(len(row)!=2 for row in generations+markers) or len(markers)>200_000:raise RuntimeError('Independent SQL projection malformed/incomplete')
    marker_ids={row[0] for row in markers};committed={key for key,state in states.items() if state=='COMMIT'}
    equal=(generations[0]==[generation,baseline] and len(marker_ids)==len(markers) and all(row[1]==generation for row in markers) and marker_ids==committed and 'PREPARE' not in states.values())
    return {'equal':equal,'marker_count':len(markers),'committed_count':len(committed),'rollback_count':sum(state=='ROLLBACK' for state in states.values()),'event_count':len(lines),'journal_sha256':hashlib.sha256(raw).hexdigest()}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--producer-bundle',type=Path,required=True);p.add_argument('--java',type=Path,required=True);p.add_argument('--mysql-bin',type=Path,required=True)
    a=p.parse_args();source=git('rev-parse','HEAD')
    if version('veritrail')!='0.13.0' or git('status','--porcelain') or os.name!='nt':raise RuntimeError('Windows, Core0.13.0 and exact clean source required')
    binary={name:(a.mysql_bin/(name+'.exe')).resolve() for name in ['mysql','mysqld','mysqldump']}
    if not all(path.is_file() for path in [a.java,*binary.values()]):raise RuntimeError('Pinned executable missing')
    jar=ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar';producer=bind(a.producer_bundle,source,jar)
    for port in [MYSQL_PORT,APP_PORT]:
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    now=datetime.now(timezone.utc);start=(now+timedelta(minutes=5)).replace(second=0,microsecond=0);end=start+timedelta(minutes=30)
    if start.astimezone(timezone(timedelta(hours=8))).hour<8 or end.astimezone(timezone(timedelta(hours=8))).hour>=22:raise RuntimeError('Real-clock probe requires the declared 08:00-22:00 booking window')
    identity='m7-restore-'+uuid.uuid4().hex;out=ROOT/'artifacts/local'/identity;out.mkdir(parents=True,exist_ok=False)
    data=out/'mysql-data';data.mkdir();runtime=out/'independent-world';runtime.mkdir();journal=runtime/'transactions.journal'
    ini=out/'my.ini';ini.write_bytes(('[mysqld]\nbasedir='+a.mysql_bin.parent.resolve().as_posix()+'\ndatadir='+data.as_posix()+'\nport='+str(MYSQL_PORT)+'\nbind-address=127.0.0.1\nmysqlx=0\ninnodb_buffer_pool_size=64M\nmax_connections=16\nskip-log-bin\nlog-error='+str(out/'mysql.private.log').replace('\\','/')+'\n').encode())
    installed=out/'qixu-api.jar';shutil.copy2(jar,installed)
    if digest(installed)!=producer['jar_sha256']:raise RuntimeError('Jar copy changed bytes')
    coord={'source_sha':source,'collector':COLLECTOR,'collector_sha256':digest(Path(__file__)),'producer_manifest_sha256':producer['manifest_sha256'],'jar_sha256':digest(installed),'restore_contract_sha256':digest(ROOT/'docs/contracts/m7-restore-fence.md'),'migration_sha256':{f.name:digest(f) for f in sorted((ROOT/'backend/src/main/resources/db/migration').glob('*.sql'))},'mysql_exe_sha256':digest(binary['mysqld']),'java_exe_sha256':digest(a.java)}
    spec={'id':'restore','contract':{'id':'qixu-m7-restore','version':'0.7'},'evidence_type':'qixu.m7.restore','coordinates':coord,'projections':['source_sha','producer','normal','restored','missing_journal','world','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
    expected={'/source_sha':source,'/source_clean':True,'/producer/bytes_checked':True,'/normal/create_status':200,'/normal/receipt_equal_after_restart':True,'/world/original_notification_observed':True,'/world/witness_unchanged_after_restore':True,'/restored/health_status':503,'/restored/health_code':'NOT_RECONCILED','/restored/competitor_status':503,'/restored/receipt_status':503,'/restored/replay_status':503,'/restored/reservations_after':0,'/restored/inbox_after':0,'/missing_journal/health_status':503,'/missing_journal/health_code':'NOT_RECONCILED','/missing_journal/receipt_status':503,'/cleanup/all_owned_stopped':True}
    expected.update({'/missing_journal/consistent_health_status':200,'/missing_journal/consistent_receipt_equal':True,'/missing_journal/consistent_ledger_equal':True,'/missing_journal/journal_existed_before_removal':True})
    assertions=[{'id':'restore-'+str(i),'severity':'HARD','left':{'requirement_id':'restore','path':'/facts'+path},'operator':'eq','right':value} for i,(path,value) in enumerate(expected.items())]
    plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m7-restore','version':7,'subject':{'id':'qixu-m7-restore','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does a restored database or missing independent ledger quarantine authority rather than silently replay committed rights?','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:m7-restore-collector','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'restore','observation_spec_id':'restore','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'restore','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':600},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu F12 single-instance recovery','consumers':['M7-restore']},'reproduction_steps':['Use exact clean source and its original native0.18/Plan6 PASS Bundle with fresh jar.','Run this collector with the pinned Windows Java/MySQL binaries; it creates a new owned data directory and schema, seals before initialization, captures T0 and independent T1 witness, stops owned processes, restores T0 and probes quarantine; restores a current T1, independently proves complete ledger equality, then removes only the ledger and probes quarantine.'],'cleanup_steps':['Only verified Popen child handles, exe and data-directory/jar coordinates may be stopped.','Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched.']})
    write(out/'sealed-plan.json',plan);write(out/'coordinate.json',coord)
    facts={'source_sha':source,'producer':producer,'normal':{},'restored':{},'missing_journal':{},'world':{},'requests':[],'boundary':'OWN_PHYSICAL_PROCESS_LOGICAL_T0_RESTORE_NOT_HOST_DISK_FAILURE_MULTI_NODE_OR_WECHAT'}
    children=[];logs=[];db=None;app=None;execution='COMPLETED';completed=False;root_password='';app_password=secrets.token_hex(24);sequence=0
    def progress(phase):print(json.dumps({'identity':identity,'phase':phase}),flush=True)
    def launch(command,label,match,env=None):
        log=(out/(str(len(children))+'-'+label+'.private.log')).open('xb');logs.append(log)
        child=subprocess.Popen(command,cwd=out,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
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
        nonlocal sequence
        verify_child(db,MYSQL_PORT);sequence+=1
        x=subprocess.run([str(binary['mysql']),'--protocol=TCP','-h','127.0.0.1','-P',str(MYSQL_PORT),'-u','root','--batch','--skip-column-names','--default-character-set=utf8mb4'],input=statement.encode(),capture_output=True,env=dict(os.environ,MYSQL_PWD=root_password),timeout=20)
        if x.returncode:(out/('sql-'+str(sequence)+'.private.stderr')).write_bytes(x.stderr);raise RuntimeError('Owned SQL failed; private stderr retained')
        return x.stdout.decode('utf-8').strip()
    def db_start():
        nonlocal db
        db=launch([str(binary['mysqld']),'--defaults-file='+str(ini),'--no-monitor'],'mysql',str(ini));limit=time.monotonic()+45
        while time.monotonic()<limit:
            if db.poll() is not None:raise RuntimeError('Owned MySQL exited')
            try:sql('SELECT 1;');return
            except (OSError,ValueError,RuntimeError):time.sleep(.4)
        raise RuntimeError('Owned MySQL readiness budget exhausted')
    def request(method,path,body=None,token=None,key=None):
        nonlocal sequence
        verify_child(app,APP_PORT);sequence+=1;headers={}
        if token:headers['Authorization']='Bearer '+token
        if key:headers['Idempotency-Key']=key
        if body is not None:headers['Content-Type']='application/json'
        req=urllib.request.Request('http://127.0.0.1:'+str(APP_PORT)+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
        try:
            with opener.open(req,timeout=10) as response:status=response.status;value=json.load(response)
        except urllib.error.HTTPError as e:status=e.code;value=json.load(e)
        write(out/('packet-'+str(sequence)+'.private.json'),{'status':status,'body':value});facts['requests'].append({'method':method,'path':path,'status':status,'code':value.get('error',{}).get('code')});return status,value
    def app_start():
        nonlocal app
        env={k:v for k,v in os.environ.items() if not k.startswith(('QIXU_','MYSQL_','DEEPSEEK_'))}
        env.update({'QIXU_DB_URL':f'jdbc:mysql://127.0.0.1:{MYSQL_PORT}/{SCHEMA}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8','QIXU_DB_USERNAME':'qixu_restore_app','QIXU_DB_PASSWORD':app_password,'QIXU_PORT':str(APP_PORT),'QIXU_BIND':'127.0.0.1','SPRING_PROFILES_ACTIVE':'demo','QIXU_TASKS_ENABLED':'true','QIXU_COOKIE_SECURE':'false','QIXU_RECOVERY_JOURNAL':str(journal)})
        app=launch([str(a.java.resolve()),'-jar',str(installed)],'qixu',str(installed),env);limit=time.monotonic()+55
        while time.monotonic()<limit:
            if app.poll() is not None:raise RuntimeError('Owned app exited before health observation')
            try:
                status,value=request('GET','/api/health')
                if status==200 or (status==503 and value.get('error',{}).get('code')=='NOT_RECONCILED'):return status,value
            except (urllib.error.URLError,OSError,ValueError,RuntimeError):time.sleep(.5)
        raise RuntimeError('Owned app health budget exhausted')
    def dump(label):
        verify_child(db,MYSQL_PORT);path=out/(label+'.private.sql')
        with path.open('xb') as target:
            x=subprocess.run([str(binary['mysqldump']),'--protocol=TCP','-h','127.0.0.1','-P',str(MYSQL_PORT),'-u','root','--single-transaction','--set-gtid-purged=OFF',SCHEMA],env=dict(os.environ,MYSQL_PWD=root_password),stdout=target,stderr=subprocess.PIPE,timeout=30)
        if x.returncode:(out/(label+'.private.stderr')).write_bytes(x.stderr);raise RuntimeError('Owned dump failed')
        facts.setdefault('backups',{})[label]={'sha256':digest(path),'bytes':path.stat().st_size};return path
    def restore(path):
        if app.poll() is None:raise RuntimeError('Refuse restore while owned app runs')
        sql('DROP DATABASE `qixu_restore`;CREATE DATABASE `qixu_restore` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;USE `qixu_restore`;\n'+path.read_text(encoding='utf-8'))
    try:
        progress('SEALED_BEFORE_OWNED_INSTANCE_INITIALIZATION')
        with (out/'initialize.private.log').open('xb') as log:
            x=subprocess.run([str(binary['mysqld']),'--defaults-file='+str(ini),'--initialize-insecure'],stdout=log,stderr=subprocess.STDOUT,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
        if x.returncode:raise RuntimeError('Owned MySQL initialization failed')
        db_start();password=secrets.token_hex(24)
        sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+password+"';CREATE DATABASE `qixu_restore` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;CREATE USER 'qixu_restore_app'@'127.0.0.1' IDENTIFIED BY '"+app_password+"';GRANT ALL PRIVILEGES ON `qixu_restore`.* TO 'qixu_restore_app'@'127.0.0.1';")
        root_password=password;status,_=app_start()
        if status!=200:raise RuntimeError('Fresh owned candidate is not ready')
        tokens=[]
        for user in ['student1','student2']:
            status,value=request('POST','/api/v1/auth/login',{'username':user,'password':'qixu-demo','mode':'BEARER'})
            if status!=200:raise RuntimeError('Fresh fixture login rejected')
            tokens.append(value['data']['token'])
        t0=dump('T0');body={'spaceId':2000,'startsAt':start.isoformat(),'endsAt':end.isoformat()};key='m7_restored_original_booking'
        status,value=request('POST','/api/v1/reservations',body,tokens[0],key);facts['normal']['create_status']=status
        if status!=200:raise RuntimeError('Original booking did not commit')
        original=value['data'];progress('ORIGINAL_RIGHT_COMMITTED')
        inbox=None;limit=time.monotonic()+20
        while time.monotonic()<limit:
            status,value=request('GET','/api/v1/inbox',token=tokens[0]);rows=value.get('data',{}).get('items',[])
            if status==200 and rows:inbox=rows;break
            time.sleep(.5)
        if not inbox:raise RuntimeError('Original standing notification not observed within budget')
        witness=runtime/'observed-world.json';write(witness,{'receipt':original,'inbox':inbox});world_hash=digest(witness);facts['world'].update({'original_notification_observed':True,'witness_sha256':world_hash});t1=dump('T1')
        stop(app);app_start();status,value=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['normal']['receipt_equal_after_restart']=status==200 and value.get('data',{}).get('result')==original
        stop(app);t1_current=dump('T1-after-normal-restart');stop(db);db_start();restore(t0);progress('T0_RESTORED_WITH_EXTERNAL_WORLD_RETAINED');status,value=app_start()
        facts['restored'].update({'health_status':status,'health_code':value.get('error',{}).get('code',value.get('data',{}).get('recoveryState','MISSING'))})
        status,_=request('POST','/api/v1/reservations',body,tokens[1],'m7_restored_competing_booking');facts['restored']['competitor_status']=status
        status,_=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['restored']['receipt_status']=status
        status,_=request('POST','/api/v1/reservations',body,tokens[0],key);facts['restored']['replay_status']=status
        time.sleep(12);facts['restored']['reservations_after']=int(sql('SELECT COUNT(*) FROM qixu_restore.short_reservation;'));facts['restored']['inbox_after']=int(sql('SELECT COUNT(*) FROM qixu_restore.inbox;'));facts['world']['witness_unchanged_after_restore']=digest(witness)==world_hash
        stop(app);restore(t1_current)
        progress('CONSISTENT_DB_LEDGER_POSITIVE_CONTROL');status,value=app_start()
        facts['missing_journal']['consistent_health_status']=status
        status,value=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['missing_journal']['consistent_receipt_equal']=status==200 and value.get('data',{}).get('result')==original
        stop(app)
        comparison=independent_ledger(journal,sql('SELECT generation,baseline_hash FROM qixu_restore.recovery_generation ORDER BY id;'),sql('SELECT transaction_id,generation FROM qixu_restore.recovery_marker ORDER BY transaction_id LIMIT 200001;'))
        facts['missing_journal']['consistency_control']=comparison;facts['missing_journal']['consistent_ledger_equal']=comparison['equal']
        facts['missing_journal']['journal_existed_before_removal']=journal.is_file()
        if journal.exists():journal.rename(runtime/'transactions-retained.original.journal')
        progress('ONLY_JOURNAL_REMOVED_DB_UNCHANGED');status,value=app_start();facts['missing_journal'].update({'health_status':status,'health_code':value.get('error',{}).get('code',value.get('data',{}).get('recoveryState','MISSING'))})
        status,_=request('GET','/api/v1/receipts/'+key,token=tokens[0]);facts['missing_journal']['receipt_status']=status;completed=True
    except Exception as error:
        execution='ERROR';facts['collection_error']={'kind':type(error).__name__,'message':str(error)[:200]}
    finally:
        cleanup=[]
        for child,owner in reversed(children):
            try:stop(child);cleanup.append({'pid':owner['pid'],'label':owner['label'],'stopped':child.poll() is not None,'ownership_checked':True})
            except Exception as error:execution='ERROR';cleanup.append({'pid':owner['pid'],'stopped':False,'error':type(error).__name__})
        for log in logs:log.close()
        port_free={}
        for port in [APP_PORT,MYSQL_PORT]:
            with socket.socket() as check:port_free[str(port)]=check.connect_ex(('127.0.0.1',port))!=0
        facts['cleanup']={'all_owned_stopped':all(row.get('stopped') for row in cleanup) and all(port_free.values()),'children':cleanup,'owned_ports_no_listener':port_free}
    facts['source_clean']=not bool(git('status','--porcelain'))
    ev={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':COLLECTOR,'captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(coord),'collection_session_id':identity,'collector_role':'qixu-m7-restore-collector','coverage':'COMPLETE' if completed else 'ERROR','normalization_semantics_version':COLLECTOR,'facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',ev);result=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=identity,execution_status=execution)
    print(json.dumps({'identity':identity,'source_sha':source,'verdict':result['verdict'],'execution':execution,'restored':facts['restored'],'missing_journal':facts['missing_journal'],'cleanup':facts['cleanup']['all_owned_stopped']}),flush=True)
    return 0 if result['verdict']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())
