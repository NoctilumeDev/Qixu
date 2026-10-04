"""Two fresh public checkouts; owned Windows fixture, native Core and actual CUA evidence.

This is a bounded project adapter, not generic database hosting. Commands: seal,
run (one serial fixture held for CUA until lowercase stop), finish. Secrets stay
in process memory; every output identity is append-only.
"""
from __future__ import annotations
import argparse, hashlib, http.client, json, os, secrets, shutil, socket
import subprocess, sys, threading, time, uuid
from datetime import datetime, timezone
from importlib.metadata import version
from pathlib import Path, PurePosixPath
from urllib.parse import quote
from veritrail.acceptance_plan import observation_spec_digest, seal_acceptance_plan, verify_sealed_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT = Path(__file__).resolve().parents[1]
COLLECTOR = 'qixu-engineering/0.1'
REMOTE = 'https://github.com/NoctilumeDev/Qixu.git'
PORTS = (6980, 6967, 6968, 6969)
CAPTURES = {
    'student-space': {'port':6968, 'route':'/pages/space/index', 'width':390, 'height':844, 'words':['A001','空间']},
    'student-short': {'port':6968, 'route':'/pages/mine/index', 'width':390, 'height':844, 'words':['A001','到场期限','短期预约']},
    'student-long': {'port':6968, 'route':'/pages/batch/index', 'width':390, 'height':844, 'words':['结果已公布','暂离不丢失长期使用权','本轮结果']},
    'student-event': {'port':6968, 'route':'/pages/event/index', 'width':390, 'height':844, 'words':['M9 演示读书会','我的状态：已确认']},
    'student-feedback': {'port':6968, 'route':'/pages/report/index', 'width':390, 'height':844, 'words':['已解决','Explicit synthetic restored facts']},
    'admin-venue': {'port':6969, 'route':'/venues', 'width':1280, 'height':720, 'words':['已批准','M9 演示读书会']},
    'admin-repair-mobile': {'port':6969, 'route':'/repairs', 'width':390, 'height':844, 'words':['复验关闭','Explicit synthetic restored facts']},
}

def read(p): return json.loads(Path(p).read_text(encoding='utf-8'))
def write(p, v):
    with Path(p).open('x', encoding='utf-8', newline='\n') as f:
        json.dump(v, f, ensure_ascii=False, sort_keys=True, indent=2); f.write('\n')
def digest(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def utc(): return datetime.now(timezone.utc).isoformat()
def git(*args, cwd=ROOT): return subprocess.check_output(['git','--no-optional-locks',*args], cwd=cwd, text=True).strip()
def require(ok, message):
    if not ok: raise RuntimeError(message)
def vacant():
    free = {}
    for port in PORTS:
        with socket.socket() as s: free[str(port)] = s.connect_ex(('127.0.0.1', port)) != 0
    return free

def checkout_path(out, n):
    # Keep native Core's staging/evidence path below Windows MAX_PATH without
    # changing host policy or relocating historical evidence.
    work = ROOT/'.tools'/('m9w-'+out.name[-12:]+'-'+str(n))
    require(work.resolve()==work and work.is_relative_to(ROOT/'.tools'), 'Unsafe fresh checkout path')
    return work
def bound(out):
    out = Path(out).resolve()
    require(out.is_relative_to(ROOT/'artifacts/local') and out.name.startswith('m9-engineering-'), 'Unsafe observation root')
    p = read(out/'sealed-plan.json'); verify_sealed_acceptance_plan(p)
    c = read(out/'coordinate.json')
    require(c == p['observation_specs'][0]['coordinates'], 'Plan coordinate drift')
    require(c['source_sha'] == git('rev-parse','HEAD') and not git('status','--porcelain'), 'Exact clean source required')
    for rel, expected in c['bindings'].items(): require(digest(ROOT/rel)==expected, 'Binding drift: '+rel)
    return out, p, c

def seal(a):
    require(os.name=='nt' and version('veritrail')=='0.13.0', 'Windows owned adapter and Core0.13.0 required')
    require(not git('status','--porcelain') and git('rev-parse','HEAD')==git('rev-parse','origin/main'), 'Seal only exact clean main')
    bindings = {p.relative_to(ROOT).as_posix():digest(p) for p in
                [Path(__file__), ROOT/'scripts/veritrail_future_batch.py', ROOT/'scripts/veritrail_native.py',
                 ROOT/'scripts/veritrail_frontend.py', ROOT/'scripts/serve_bound_frontend.py',
                 ROOT/'docs/contracts/engineering-delivery.md', ROOT/'scripts/java/M9StopAgent.java', ROOT/'package-lock.json', ROOT/'randomness/package-lock.json',
                 *sorted((ROOT/'backend/src/main/resources/db/migration').glob('V*.sql')),
                 *sorted(p for folder in (ROOT/'student/src/static',ROOT/'admin/public/assets') for p in folder.rglob('*') if p.is_file())]}
    c = {'source_sha':git('rev-parse','HEAD'), 'tree':git('rev-parse','HEAD^{tree}'), 'remote':REMOTE,
         'collector':COLLECTOR, 'bindings':bindings, 'captures':CAPTURES, 'sealed_at':utc(),
         'tools':{k:str(Path(getattr(a,k)).resolve()) for k in ('mysql_bin','java_home','maven','node','npm')},
         'git_proxy':a.git_proxy, 'random_proxy':a.random_proxy,
         'boundary':'TWO_WINDOWS_FRESH_CHECKOUTS_LOCAL_DEMO_NOT_COLD_CACHE_WECHAT_DEVICE_PRODUCTION_OR_M10'}
    spec = {'id':'fresh-engineering','contract':{'id':'qixu-engineering','version':'0.1'},
            'evidence_type':'qixu.engineering.observation','coordinates':c,
            'projections':['source_sha','runs','distinct','boundary'], 'canonicalization_profile':'veritrail-json-c14n/1'}
    def assertion(name, path, value):
        return {'id':name,'severity':'HARD','left':{'requirement_id':'engineering','path':path},'operator':'eq','right':value}
    assertions = [assertion('source','/facts/source_sha',c['source_sha']), assertion('distinct','/facts/distinct',True)]
    for n in range(2):
        for k in ('fresh_public','bytes_checked','native_181','frontend_61','future_batch','all_pages','sql_supported','same_db_restart','owned_cleanup'):
            assertions.append(assertion(f'run-{n+1}-{k}',f'/facts/runs/{n}/{k}',True))
    plan = seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m9-engineering','version':1,
        'subject':{'id':'qixu-m9','version':c['source_sha'],'source_ref':'github:NoctilumeDev/Qixu'},
        'question':'Can two fresh public checkouts independently rebuild, execute the bounded demo and retain actual UI/SQL through a normal same-database restart?',
        'governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:engineering-adapter','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},
        'observation_specs':[spec], 'evidence_requirements':[{'id':'engineering','observation_spec_id':spec['id'],'cardinality':'EXACTLY_ONE'}],
        'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'engineering','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],
        'integrity_rules':[], 'assertions':assertions, 'resource_budget':{'max_artifact_bytes':2097152,'command_timeout_seconds':3600},
        'change_scope':{'level':'L2_CONTRACT','owner':'Qixu M9','consumers':['engineering-candidate']},
        'reproduction_steps':['Seal before two serial public GitHub clones; build with locked dependencies and owned MySQL8.0.44.',
            'Use child native/frontend/live sealed Bundles; operate seven declared actual CUA pages and save raw triplets.',
            'Stop the app, restart the same DB/journal normally, read health and authenticated own reservations; Core checks all child bytes and facts.'],
        'cleanup_steps':['Stop only retained Popen handles after executable/arguments ownership checks; do not adopt shared ports.',
            'Keep immutable first failures, Bundles and captures; remove only rebuildable instances after public qualification.']})
    out = ROOT/'artifacts/local'/('m9-engineering-'+uuid.uuid4().hex); out.mkdir()
    write(out/'coordinate.json',c); write(out/'sealed-plan.json',plan)
    print(json.dumps({'state':'M9_PARENT_SEALED','identity':out.name,'output':str(out),'source_sha':c['source_sha']}),flush=True)

def bundle(folder, source, expected_collector, expected_plan):
    folder = Path(folder).resolve(); m = read(folder/'acceptance-bundle-manifest.json'); seen=set(); total=0
    for e in m['files']:
        rel = PurePosixPath(e['path']); f = (folder/rel).resolve(); total += e['size']
        require(not rel.is_absolute() and '..' not in rel.parts and '\\' not in e['path'] and e['path'] not in seen
                and f.is_relative_to(folder) and not f.is_symlink() and 0<=e['size']<=2097152 and total<=8388608, 'Unsafe child manifest')
        seen.add(e['path']); require(f.stat().st_size==e['size'] and digest(f)==e['sha256'], 'Child byte drift')
    require(seen=={p.relative_to(folder).as_posix() for p in folder.rglob('*') if p.is_file() and p.name!='acceptance-bundle-manifest.json'}, 'Unmanifested child files')
    r=read(folder/'acceptance-report.json'); p=read(folder/'sealed-acceptance-plan.json'); verify_sealed_acceptance_plan(p)
    expected_subject={'qixu-native/0.22':'qixu-m8','qixu-frontend/0.8':'qixu-m8-frontend','qixu-live-observation/0.5':'qixu-m9-live'}[expected_collector]
    require(r['subject']==p['subject'] and r['subject']['id']==expected_subject and r['subject']['version']==source and r['plan']['version']==expected_plan
            and r['plan']['sha256']==p['seal']['digest'] and r['verdict']=='PASS' and r['execution_status']=='COMPLETED', 'Unqualified child')
    require(len(r['evidence'])==1, 'Ambiguous child evidence'); entry=r['evidence'][0]; e=read(folder/entry['path'])
    require(entry['path'] in seen and entry['sha256']==digest(folder/entry['path']) and e['source']==expected_collector
            and entry['facts_digest']==sha256_json(e['facts']) and e['metadata']['veritrail_observation']['plan_digest']==p['seal']['digest'], 'Unbound child facts')
    return r,e['facts'],p,e

def run(a):
    out, plan, c = bound(a.output); n=a.run; d=out/f'run-{n}'; d.mkdir()
    require(all(vacant().values()), 'Fixture ports in use; never adopt them')
    work=checkout_path(out,n); require(not work.exists(), 'Fresh checkout path already exists; use a new identity')
    children=[]; logs=[]; threads=[]; root_password=''; db_env={}; restart={}
    record={'run':n,'started_at':utc(),'source_sha':c['source_sha']}; execution='COMPLETED'
    tools=c['tools']; mysql=Path(tools['mysql_bin'])/'mysql.exe'; mysqld=Path(tools['mysql_bin'])/'mysqld.exe'
    java=Path(tools['java_home'])/'bin/java.exe'
    env={k:v for k,v in os.environ.items() if not k.startswith(('QIXU_','MYSQL_','DEEPSEEK_'))}
    env.update(JAVA_HOME=tools['java_home'],PATH=str(Path(tools['node']).parent)+os.pathsep+os.environ['PATH'],PYTHONDONTWRITEBYTECODE='1')
    def command(cmd, name, cwd, e=env, timeout=1000):
        with (d/(name+'.stdout.private.log')).open('xb') as so, (d/(name+'.stderr.private.log')).open('xb') as se:
            r=subprocess.run(cmd,cwd=cwd,env=e,stdout=so,stderr=se,timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
        require(r.returncode==0, 'Command failed: '+name)
    def launch(cmd, name, e=env, interactive=False, controlled=False):
        log=(d/(name+'.stdout.private.log')).open('xb'); logs.append(log)
        p=subprocess.Popen(cmd,cwd=work,env=e,stdout=subprocess.PIPE if interactive else log,stderr=subprocess.STDOUT,
                           stdin=subprocess.PIPE if interactive or controlled else subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW)
        children.append((p,cmd,name)); return p,log
    def stop(p, cmd, name):
        if p.poll() is None:
            if name=='live':
                # EOF makes the nested collector enter its own finally and stop
                # its retained Java handle. Never orphan it by killing the parent.
                p.stdin.close(); p.wait(180)
                return
            ps="$p=Get-CimInstance Win32_Process -Filter 'ProcessId="+str(p.pid)+"';$p|Select-Object ProcessId,ExecutablePath,CommandLine|ConvertTo-Json -Compress"
            row=json.loads(subprocess.check_output(['powershell','-NoProfile','-Command',ps],text=True))
            require(Path(row['ExecutablePath']).resolve()==Path(cmd[0]).resolve() and all(x in row['CommandLine'] for x in cmd[1:]), 'Process ownership drift')
            owner_path=d/(name+'.owner.private.json')
            if owner_path.exists():require(read(owner_path)==row,'Owned process changed during cleanup')
            else:write(owner_path,row)
            if name=='restart-app':
                try:
                    p.stdin.write(b'qixu-m9-graceful-stop\n');p.stdin.flush();p.wait(35)
                except (OSError,subprocess.TimeoutExpired):
                    record.setdefault('restart',{})['normal_stop']={'method':'NOT_NORMAL_OWNED_FALLBACK','exit_code':p.poll(),'marker':False}
                    if p.poll() is None:p.terminate();p.wait(25)
                    raise RuntimeError('Normal JVM stop failed; owned fallback does not qualify restart')
                marker='M9_GRACEFUL_STOP_REQUESTED' in (d/'restart-app.stdout.private.log').read_text(encoding='utf-8',errors='replace')
                require(p.returncode==0 and marker,'Restart app did not stop normally')
                record.setdefault('restart',{})['normal_stop']={'method':'OWNED_STDIN_SYSTEM_EXIT_0','exit_code':p.returncode,'marker':marker}
            else:p.terminate();p.wait(25)
    def sql(query):
        r=subprocess.run([str(mysql),'--protocol=TCP','-h','127.0.0.1','-P','6980','-u','root','--batch','--skip-column-names','--default-character-set=utf8mb4'],
             input=query.encode(),capture_output=True,env=dict(env,MYSQL_PWD=root_password),timeout=15)
        require(r.returncode==0,'Owned SQL failed; sensitive output withheld'); return r.stdout.decode('utf-8').strip()
    def req(method,path,body=None,token=None):
        conn=http.client.HTTPConnection('127.0.0.1',6967,timeout=5)
        try:
            headers={'Content-Type':'application/json'}
            if token: headers['Authorization']='Bearer '+token
            conn.request(method,path,None if body is None else json.dumps(body),headers); r=conn.getresponse(); raw=r.read()
            require(r.status==200,'Restart HTTP status '+str(r.status)); return json.loads(raw)
        finally:conn.close()
    def ready(p, check, timeout=65):
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            require(p.poll() is None, 'Owned process exited before readiness')
            try:
                if check():return
            except (OSError,RuntimeError,ValueError,http.client.HTTPException):pass
            time.sleep(.4)
        raise RuntimeError('Readiness timeout')
    def child_path(pattern):
        paths=list((work/'artifacts/local').glob(pattern)); require(len(paths)==1,'Ambiguous fresh child identity'); return paths[0]
    counts_query="SELECT (SELECT COUNT(*) FROM qixu_test.short_reservation WHERE user_id=1 AND status IN ('PENDING','CHECKED_IN')),(SELECT COUNT(*) FROM qixu_test.campus_event WHERE status='PUBLISHED'),(SELECT COUNT(*) FROM qixu_test.event_participation WHERE user_id=1 AND status='CONFIRMED'),(SELECT COUNT(*) FROM qixu_test.feedback_report WHERE status='RESOLVED'),(SELECT COUNT(*) FROM qixu_test.repair_ticket WHERE status='VERIFIED_CLOSED'),(SELECT COUNT(*) FROM qixu_test.flyway_schema_history WHERE success=1),(SELECT COUNT(*) FROM qixu_test.seat_entitlement WHERE user_id=1 AND status='ACTIVE');"
    try:
        clone=['git']+(['-c','http.proxy='+c['git_proxy']] if c['git_proxy'] else [])+['clone','--depth','1','--no-checkout',REMOTE,str(work)]
        command(clone,'clone',ROOT,timeout=150); require(git('rev-parse','HEAD',cwd=work)==c['source_sha'],'Public main moved; reseal')
        git('checkout','--detach',c['source_sha'],cwd=work)
        require(git('rev-parse','HEAD^{tree}',cwd=work)==c['tree'] and not git('status','--porcelain',cwd=work),'Fresh tree drift')
        absent=all(not list(work.rglob(name)) for name in ('target','dist','node_modules'))
        require(absent,'Fresh clone contains build instances')
        record['clone']={'origin':git('remote','get-url','origin',cwd=work),'tree':c['tree'],'source_sha':c['source_sha'],'no_initial_instances':absent,'checkout_relative':work.relative_to(ROOT).as_posix(),'created_at':utc()}
        data=d/'mysql-data'; data.mkdir(); ini=d/'my.ini'
        ini.write_text('[mysqld]\nbasedir='+Path(tools['mysql_bin']).parent.as_posix()+'\ndatadir='+data.as_posix()+'\nport=6980\nbind-address=127.0.0.1\nmysqlx=0\ninnodb_buffer_pool_size=64M\nmax_connections=30\nskip-log-bin\nlog-error='+(d/'mysql.private.log').as_posix()+'\n',encoding='utf-8')
        command([str(mysqld),'--defaults-file='+str(ini),'--initialize-insecure'],'mysql-init',work,timeout=65)
        db,_=launch([str(mysqld),'--defaults-file='+str(ini),'--no-monitor'],'mysql'); ready(db,lambda:sql('SELECT 1;')=='1')
        app_password=secrets.token_hex(24); new_root=secrets.token_hex(24)
        sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+new_root+"';CREATE DATABASE qixu_test CHARACTER SET utf8mb4;CREATE USER 'qixu_test_app'@'127.0.0.1' IDENTIFIED BY '"+app_password+"';GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX,REFERENCES ON qixu_test.* TO 'qixu_test_app'@'127.0.0.1';")
        root_password=new_root
        db_url='jdbc:mysql://127.0.0.1:6980/qixu_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8&sslMode=DISABLED&serverRSAPublicKeyFile='+quote((data/'public_key.pem').as_posix(),safe='')
        db_env=dict(env,QIXU_TEST_DB_URL=db_url,QIXU_TEST_DB_USER='qixu_test_app',QIXU_TEST_DB_PASSWORD=app_password)
        command([tools['npm'],'ci','--prefix','randomness','--ignore-scripts','--no-audit','--no-fund'],'randomness-install',work)
        command([sys.executable,'-B',str(work/'scripts/veritrail_native.py'),'--stage','m8','--maven',tools['maven']],'native',work,db_env)
        native=child_path('m8-*'); bundle(native/'bundle',c['source_sha'],'qixu-native/0.22',3)
        jar=work/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar'; shutil.copyfile(jar,d/'retained-package.jar')
        record['native']=str(native.relative_to(work)); record['jar_sha256']=digest(jar)
        print(json.dumps({'state':'M9_NATIVE_COMPLETE','run':n,'native':str(native),'source_sha':c['source_sha']}),flush=True)
        command([sys.executable,'-B',str(work/'scripts/veritrail_frontend.py'),'--stage','m8','--node',tools['node'],'--npm',tools['npm']],'frontend',work)
        front=child_path('m8-frontend-*'); bundle(front/'bundle',c['source_sha'],'qixu-frontend/0.8',4); record['frontend']=str(front.relative_to(work))
        agent_classes=d/'stop-agent-classes';agent_classes.mkdir()
        command([str(Path(tools['java_home'])/'bin/javac.exe'),'-d',str(agent_classes),str(work/'scripts/java/M9StopAgent.java')],'agent-compile',work)
        manifest=d/'agent-manifest.mf';manifest.write_text('Manifest-Version: 1.0\nPremain-Class: M9StopAgent\n\n',encoding='ascii')
        agent=d/'stop-agent.jar'
        command([str(Path(tools['java_home'])/'bin/jar.exe'),'--create','--file',str(agent),'--manifest',str(manifest),'-C',str(agent_classes),'.'],'agent-package',work)
        record['stop_agent_sha256']=digest(agent)
        # A fresh demo world has its own journal. Never delete or recycle the native journal.
        sql('DROP DATABASE qixu_test;CREATE DATABASE qixu_test CHARACTER SET utf8mb4;')
        live_env=dict(db_env,QIXU_RECOVERY_JOURNAL=str(d/'live-transactions.journal'),QIXU_ALLOWED_ORIGINS='http://127.0.0.1:6968,http://127.0.0.1:6969',QIXU_BIND='127.0.0.1')
        cmd=[sys.executable,'-B',str(work/'scripts/veritrail_future_batch.py'),'--stage','m9','--hold-for-browser','--stop-agent',str(agent),'--producer-bundle',str(native/'bundle'),'--java',str(java),'--node',tools['node'],'--mysql',str(mysql),'--proxy',c['random_proxy']]
        live, live_log=launch(cmd,'live',live_env,True); event=threading.Event()
        def drain():
            for line in iter(live.stdout.readline,b''):
                live_log.write(line); live_log.flush()
                try:
                    message=json.loads(line); print(json.dumps(message,ensure_ascii=False),flush=True)
                    if message.get('phase')=='M9_BROWSER_READY':event.set()
                except (ValueError,UnicodeDecodeError):pass
        t=threading.Thread(target=drain); t.start(); threads.append(t)
        deadline=time.monotonic()+300
        while not event.wait(.5):
            require(live.poll() is None,'Live stopped before browser readiness'); require(time.monotonic()<deadline,'Future batch readiness timeout')
        live_dir=child_path('m9-live-*'); record['live']=str(live_dir.relative_to(work))
        # Use the actual base interpreter for the Core-free server, not a
        # Windows venv redirector whose child would outlive the wrapper handle.
        front_cmd=[sys._base_executable,'-B',str(work/'scripts/serve_bound_frontend.py'),str(front)]
        server,_=launch(front_cmd,'frontend-server'); ready(server,lambda:all(not vacant()[str(p)] for p in (6968,6969)))
        (d/'captures').mkdir(); write(d/'browser-ready.json',{'source_sha':c['source_sha'],'live':record['live'],'frontend':record['frontend'],'ready_at':utc()})
        print(json.dumps({'state':'M9_CUA_READY','run':n,'captures':str(d/'captures'),'ports':[6968,6969],'live':str(live_dir)}),flush=True)
        for line in sys.stdin:
            require(line.strip()=='stop','Only lowercase stop ends CUA fixture'); break
        else:raise RuntimeError('Missing CUA completion signal')
        stop(server,front_cmd,'frontend-server'); live.stdin.write(b'stop\n'); live.stdin.flush(); live.wait(65); t.join(5)
        require(live.returncode==0,'Live Core failed'); bundle(live_dir/'bundle',c['source_sha'],'qixu-live-observation/0.5',1)
        before=sql(counts_query); record['sql_before_restart']=before
        restart_env=dict(live_env,SPRING_PROFILES_ACTIVE='demo',QIXU_DB_URL=db_url,QIXU_DB_USERNAME='qixu_test_app',QIXU_DB_PASSWORD=app_password,QIXU_PORT='6967',QIXU_COOKIE_SECURE='false',QIXU_TASKS_ENABLED='false',QIXU_NODE=tools['node'],QIXU_RANDOM_VERIFIER=str(work/'randomness/verify.mjs'),QIXU_RANDOM_PROXY=c['random_proxy'])
        app_cmd=[str(java),'-javaagent:'+str(agent),'-jar',str(jar)]; app,_=launch(app_cmd,'restart-app',restart_env,controlled=True)
        ready(app,lambda:req('GET','/api/health') is not None)
        token=req('POST','/api/v1/auth/login',{'username':'student1','password':'qixu-demo','mode':'BEARER'})['data']['token']
        own=req('GET','/api/v1/reservations',token=token)['data']; after=sql(counts_query)
        record['restart']={'health':200,'authenticated_status':200,'own_reservations':len(own),'sql_unchanged':before==after,'journal_exists':(d/'live-transactions.journal').is_file(),'observed_at':utc()}
        record['sql_after_restart']=after; stop(app,app_cmd,'restart-app')
    except Exception as e:
        execution='ERROR'; record['failure']={'type':type(e).__name__,'message':str(e)}
    finally:
        cleaned=[]
        for child,cmd,name in reversed(children):
            try:stop(child,cmd,name); cleaned.append({'name':name,'stopped':child.poll() is not None})
            except Exception as e:cleaned.append({'name':name,'stopped':False,'error':type(e).__name__}); execution='ERROR'
        for t in threads:t.join(5)
        for log in logs:log.close()
        record['cleanup']={'children':cleaned,'ports_free':vacant(),'threads_stopped':all(not t.is_alive() for t in threads)}
        record['execution_status']=execution; record['finished_at']=utc(); write(d/'run-record.json',record)
        print(json.dumps({'state':'M9_RUN_STOPPED','run':n,'execution_status':execution,'cleanup':record['cleanup']}),flush=True)
    return 0 if execution=='COMPLETED' else 1

def finish(a):
    out,p,c=bound(a.output); facts={'source_sha':c['source_sha'],'boundary':c['boundary'],'runs':[]}; execution='COMPLETED'
    try:
        ids=[]; seal_time=datetime.fromisoformat(c['sealed_at'])
        for n in (1,2):
            d=out/f'run-{n}'; record=read(d/'run-record.json'); work=checkout_path(out,n)
            require(record['execution_status']=='COMPLETED' and record['source_sha']==c['source_sha'],'Incomplete fresh run')
            require(datetime.fromisoformat(record['started_at'])>seal_time,'Run predates parent seal')
            clone=record['clone']; require(clone['origin']==REMOTE and clone['source_sha']==c['source_sha'] and clone['tree']==c['tree'] and clone['no_initial_instances'] and clone['checkout_relative']==work.relative_to(ROOT).as_posix(),'Not fresh public checkout')
            observations={}; child_plans={}; identity={}
            for name,collector,pv in [('native','qixu-native/0.22',3),('frontend','qixu-frontend/0.8',4),('live','qixu-live-observation/0.5',1)]:
                r,f,cp,e=bundle(work/record[name]/'bundle',c['source_sha'],collector,pv)
                require(seal_time<datetime.fromisoformat(record['started_at'])<datetime.fromisoformat(e['captured_at'])<=datetime.fromisoformat(record['finished_at']), 'Child outside this sealed run')
                require(f['source_sha']==c['source_sha'] and f['source_clean'] is True, 'Child source drift')
                observations[name]=f; identity[name]=r['acceptance_id']; ids.append(r['acceptance_id'])
                child_plans[name]=cp
            native=observations['native']; front=observations['frontend']; live=observations['live']
            require(digest(d/'stop-agent.jar')==record['stop_agent_sha256']==child_plans['live']['observation_specs'][0]['coordinates']['stop_agent_sha256'], 'Owned control agent drift')
            jar=d/'retained-package.jar'; require(digest(jar)==record['jar_sha256']==native['package']['sha256']==live['jar_sha256'],'Retained package drift')
            for name,relative in [('h5','student/dist/build/h5'),('wechat','student/dist/build/mp-weixin'),('admin','admin/dist')]:
                folder=work/record['frontend']/'work'/relative
                for rel,m in front['artifacts'][name]['files'].items():require((folder/rel).stat().st_size==m['bytes'] and digest(folder/rel)==m['sha256'],'Static byte drift')
            captured={}
            for name,rule in CAPTURES.items():
                folder=d/'captures'; meta=read(folder/(name+'.json')); dom=(folder/(name+'.txt')).read_text(encoding='utf-8'); image=folder/(name+'.jpg')
                require(meta['url'].startswith('http://127.0.0.1:'+str(rule['port'])+'/') and rule['route'] in meta['url'],'Capture URL mismatch')
                require(meta['width']==rule['width'] and meta['height']==rule['height'] and meta['scroll_width']<=rule['width'],'Viewport overflow/mismatch')
                require(datetime.fromisoformat(read(d/'browser-ready.json')['ready_at'])<datetime.fromisoformat(meta['captured_at'].replace('Z','+00:00'))<=datetime.fromisoformat(record['finished_at']), 'Capture outside this running fixture')
                require(all(w in dom for w in rule['words']) and image.read_bytes()[:3]==b'\xff\xd8\xff' and image.stat().st_size>5000,'Missing actual page evidence')
                captured[name]={suffix:digest(folder/(name+suffix)) for suffix in ('.json','.txt','.jpg')}
            counts=list(map(int,record['sql_after_restart'].split('\t'))); require(counts==[1,1,1,1,1,10,1],'SQL does not support pages')
            require(live['m9_browser_sql']==dict(zip(['shorts','events','participations','resolved_reports','verified_repairs','migrations'],counts[:6])),'Live SQL projection drift')
            restart=record['restart']; cleanup=record['cleanup']
            facts['runs'].append({'run':n,'child_identities':identity,'record_sha256':digest(d/'run-record.json'),'jar_sha256':digest(jar),'capture_digests':captured,
                'fresh_public':True,'bytes_checked':True,'native_181':len(native['tests'])==181 and all(native['tests'].values()) and native['command_exit']==0,
                'frontend_61':len(front['tests'])==61 and all(front['tests'].values()) and len(front['commands'])==5 and all(x['exit_code']==0 for x in front['commands']),
                'future_batch':live['database']=={'formal_results':1,'outcomes':2,'offers':2,'result_notices':2} and live['reproduction']['maximum']==2 and live['reproduction']['signature_verified'] and live['reproduction']['independent_bytes_equal'],
                'all_pages':True,'sql_supported':True,'same_db_restart':restart['health']==200 and restart['authenticated_status']==200 and restart['own_reservations']==1 and restart['sql_unchanged'] and restart['journal_exists'] and live['cleanup']['normal_exit'] and live['cleanup']['graceful_marker'] and restart['normal_stop']=={'method':'OWNED_STDIN_SYSTEM_EXIT_0','exit_code':0,'marker':True},
                'owned_cleanup':all(x['stopped'] for x in cleanup['children']) and all(cleanup['ports_free'].values()) and cleanup['threads_stopped']})
        facts['distinct']=len(ids)==len(set(ids)) and len(ids)==6
    except Exception as e:
        execution='ERROR'; facts['failure']={'type':type(e).__name__,'message':str(e)}
    spec=p['observation_specs'][0]
    evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':COLLECTOR,'captured_at':utc(),'facts':facts,
       'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':p['seal']['digest'],
       'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(c),'collection_session_id':out.name,
       'collector_role':'qixu-engineering-collector','coverage':'COMPLETE' if execution=='COMPLETED' else 'ERROR','normalization_semantics_version':COLLECTOR,'facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',evidence); report=create_acceptance_bundle(plan=p,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status=execution)
    print(json.dumps({'identity':out.name,'verdict':report['verdict'],'execution_status':execution,'facts':facts},ensure_ascii=False),flush=True)
    return 0 if report['verdict']=='PASS' else 1

if __name__=='__main__':
    parser=argparse.ArgumentParser(); sub=parser.add_subparsers(dest='action',required=True)
    s=sub.add_parser('seal')
    for name in ('mysql-bin','java-home','maven','node','npm'):s.add_argument('--'+name,required=True)
    s.add_argument('--git-proxy',default=''); s.add_argument('--random-proxy',default='')
    for name in ('run','finish'):
        p=sub.add_parser(name); p.add_argument('--output',type=Path,required=True)
        if name=='run':p.add_argument('--run',type=int,choices=(1,2),required=True)
    args=parser.parse_args(); sys.exit({'seal':seal,'run':run,'finish':finish}[args.action](args) or 0)
