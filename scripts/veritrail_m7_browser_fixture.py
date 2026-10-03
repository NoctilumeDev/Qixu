"""Owned installed browser fixture. stdin controls faults; never adopts a port."""
from pathlib import Path
import argparse,hashlib,json,os,secrets,shutil,socket,subprocess,sys,threading,time,http.client
from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from urllib.parse import quote,urlsplit
from m7_producer_binding import bind,digest

ROOT=Path(__file__).resolve().parents[1]
PORTS=(6980,6981,6982,6983)
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def write(p,v):
    with Path(p).open('x',encoding='utf-8',newline='\n') as f:json.dump(v,f,ensure_ascii=False,indent=2);f.write('\n')
def main():
    p=argparse.ArgumentParser();p.add_argument('--observation',type=Path,required=True);p.add_argument('--native',type=Path,required=True);p.add_argument('--frontend',type=Path,required=True);a=p.parse_args()
    source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    if subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip():raise RuntimeError('Clean source required')
    out=a.observation.resolve();native=a.native.resolve();front=a.frontend.resolve();coord=read(out/'request.json')
    assert out.is_relative_to(ROOT/'artifacts/local') and coord['source_sha']==source
    producer=bind(native/'bundle',source,ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar')
    fe=read(front/'evidence.json');assert fe['facts']['source_sha']==source and read(front/'bundle/acceptance-report.json')['verdict']=='PASS'
    assert digest(front/'evidence.json')==coord['frontend_evidence_sha256'] and digest(native/'evidence.json')==coord['native_evidence_sha256']
    roots={6982:front/'work/student/dist/build/h5',6983:front/'work/admin/dist'}
    for port,name in [(6982,'h5'),(6983,'admin')]:
        for relative,meta in fe['facts']['artifacts'][name]['files'].items():
            file=roots[port]/relative;assert digest(file)==meta['sha256'] and file.stat().st_size==meta['bytes']
    for port in PORTS:
        with socket.socket() as s:s.bind(('127.0.0.1',port))
    installation=out/'installation';installation.mkdir();data=installation/'mysql-data';data.mkdir()
    jar=installation/'qixu-api.jar';shutil.copyfile(ROOT/'backend/target/qixu-api-0.1.0-SNAPSHOT.jar',jar);assert digest(jar)==producer['jar_sha256']
    base=Path('D:/MySQL_8.0/MySQL_server');mysql=base/'bin/mysql.exe';mysqld=base/'bin/mysqld.exe';java=Path('D:/IDEA/JDK17/bin/java.exe')
    ini=installation/'my.ini';ini.write_bytes(('[mysqld]\nbasedir='+base.as_posix()+'\ndatadir='+data.as_posix()+'\nport=6980\nbind-address=127.0.0.1\nmysqlx=0\ninnodb_buffer_pool_size=64M\nmax_connections=16\nskip-log-bin\nlog-error='+str(installation/'mysql.private.log').replace('\\','/')+'\n').encode())
    root_password='';app_password=secrets.token_hex(24);children=[];logs=[];servers=[];threads=[];lock=threading.Lock();fault={'armed':False,'records':[]};requests=[];cleanup=[]
    def launch(command,label,env=None):
        log=(installation/(label+'.stdout.private.log')).open('xb');logs.append(log)
        child=subprocess.Popen(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW);children.append((child,command,label))
        return child
    def sql(statement):
        r=subprocess.run([str(mysql),'--protocol=TCP','-h','127.0.0.1','-P','6980','-u','root','--batch','--skip-column-names','--default-character-set=utf8mb4'],input=statement.encode(),env=dict(os.environ,MYSQL_PWD=root_password),capture_output=True,timeout=20)
        if r.returncode:raise RuntimeError('Owned SQL failed; sensitive output withheld')
        return r.stdout.decode('utf-8').strip()
    def ready(child,probe,budget=55):
        limit=time.monotonic()+budget
        while time.monotonic()<limit:
            if child.poll() is not None:raise RuntimeError('Owned child exited before ready')
            try:
                if probe():return
            except (OSError,ValueError,RuntimeError,http.client.HTTPException):pass
            time.sleep(.4)
        raise RuntimeError('Ready budget exhausted')
    def health():
        c=http.client.HTTPConnection('127.0.0.1',6981,timeout=2)
        try:c.request('GET','/api/health');r=c.getresponse();r.read();return r.status==200
        finally:c.close()
    def facts():
        counts=sql("SELECT recipient_id,COUNT(*),SUM(read_at IS NULL) FROM qixu_browser.inbox GROUP BY recipient_id ORDER BY recipient_id;")
        favorites=sql("SELECT COUNT(*) FROM qixu_browser.favorite_space WHERE user_id=1 AND space_id=2000;")
        receipts=[]
        for r in fault['records']:
            key=r['key'];assert len(key)<150 and all(c.isalnum() or c in '-_' for c in key)
            count=sql("SELECT COUNT(*) FROM qixu_browser.idempotency_receipt WHERE actor_id=1 AND request_key='"+key+"';")
            receipts.append({'key_sha256':hashlib.sha256(key.encode()).hexdigest(),'count':int(count),'status':r['status'],'path':r['path']})
        review={}
        if coord.get('collector')=='qixu-m8-browser/0.1':
            review={
                'outlet_true_facts':int(sql("SELECT COUNT(*) FROM qixu_browser.space_fact WHERE space_id=2000 AND feature_key='outlet' AND value_json=CAST('true' AS JSON);")),
                'outlet_false_facts':int(sql("SELECT COUNT(*) FROM qixu_browser.space_fact WHERE space_id=2000 AND feature_key='outlet' AND value_json=CAST('false' AS JSON);")),
                'published_events':int(sql("SELECT COUNT(*) FROM qixu_browser.campus_event WHERE status='PUBLISHED';")),
                'confirmed_parts':int(sql("SELECT COUNT(*) FROM qixu_browser.event_participation WHERE status='CONFIRMED';")),
                'waitlisted_parts':int(sql("SELECT COUNT(*) FROM qixu_browser.event_participation WHERE status='WAITLISTED';")),
                'kept_short':int(sql("SELECT COUNT(*) FROM qixu_browser.short_reservation WHERE user_id=1 AND status IN ('PENDING','CHECKED_IN');")),
                'exited_applications':int(sql("SELECT COUNT(*) FROM qixu_browser.preparation_application WHERE user_id=1 AND status='WITHDRAWN';")),
            }
        return {'source_sha':source,'inbox':counts,'favorite_count':int(favorites),'lost_responses':receipts,'request_count':len(requests),'static_bytes_checked':True,'m8':review}
    class Handler(SimpleHTTPRequestHandler):
        def __init__(self,*args,**kwargs):super().__init__(*args,directory=str(args[2].document_root),**kwargs)
        def log_message(self,*_):pass
        def proxy(self):
            size=int(self.headers.get('Content-Length','0'))
            if not 0<=size<=2097152:self.send_error(413);return
            body=self.rfile.read(size) if size else None
            headers={k:v for k,v in self.headers.items() if k.lower() not in ('host','connection','transfer-encoding','content-length')};headers['Host']='127.0.0.1:6981'
            c=http.client.HTTPConnection('127.0.0.1',6981,timeout=40)
            try:
                c.request(self.command,self.path,body,headers);r=c.getresponse();raw=r.read();response_headers=r.getheaders()
                with lock:
                    requests.append({'method':self.command,'path':self.path,'status':r.status})
                    drop=fault['armed'] and self.command=='POST' and self.path=='/api/v1/favorites'
                    if drop:
                        fault['armed']=False;fault['records'].append({'key':self.headers.get('Idempotency-Key'),'path':self.path,'status':r.status});write(installation/'lost-response.private.json',json.loads(raw))
                if drop:raise OSError('One committed response deliberately withheld')
                self.send_response(r.status)
                for key,value in response_headers:
                    if key.lower() not in ('connection','transfer-encoding','content-length'):self.send_header(key,value)
                self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
            except (OSError,http.client.HTTPException):
                raw=b'{"error":{"code":"DEV_BACKEND_UNAVAILABLE","message":"Submission result remains unknown; query the original receipt."}}'
                self.send_response(503);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
            finally:c.close()
        def do_GET(self):
            if urlsplit(self.path).path.startswith('/api/'):self.proxy();return
            if not Path(self.translate_path(self.path)).exists() and not Path(urlsplit(self.path).path).suffix:self.path='/index.html'
            super().do_GET()
        def do_POST(self):
            if urlsplit(self.path).path.startswith('/api/'):self.proxy()
            else:self.send_error(405)
    try:
        with (installation/'initialize.private.log').open('xb') as log:subprocess.run([str(mysqld),'--defaults-file='+str(ini),'--initialize-insecure'],stdout=log,stderr=subprocess.STDOUT,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW,check=True)
        db=launch([str(mysqld),'--defaults-file='+str(ini),'--no-monitor'],'mysql');ready(db,lambda:sql('SELECT 1;')=='1')
        password=secrets.token_hex(24)
        sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '"+password+"';CREATE DATABASE qixu_browser CHARACTER SET utf8mb4;CREATE USER 'qixu_browser_app'@'127.0.0.1' IDENTIFIED BY '"+app_password+"';GRANT ALL PRIVILEGES ON qixu_browser.* TO 'qixu_browser_app'@'127.0.0.1';");root_password=password
        env={k:v for k,v in os.environ.items() if not k.startswith(('QIXU_','MYSQL_','DEEPSEEK_'))}
        env.update(QIXU_DB_URL='jdbc:mysql://127.0.0.1:6980/qixu_browser?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8&sslMode=DISABLED&serverRSAPublicKeyFile='+quote((data/'public_key.pem').as_posix()),QIXU_DB_USERNAME='qixu_browser_app',QIXU_DB_PASSWORD=app_password,QIXU_PORT='6981',QIXU_BIND='127.0.0.1',SPRING_PROFILES_ACTIVE='demo',QIXU_TASKS_ENABLED='false',QIXU_COOKIE_SECURE='false',QIXU_ALLOWED_ORIGINS='http://127.0.0.1:6982,http://127.0.0.1:6983',QIXU_RECOVERY_JOURNAL=str(installation/'transactions.journal'))
        app=launch([str(java),'-jar',str(jar)],'app',env);ready(app,health)
        statements=[]
        for index in range(1,152):
            for uid in [1,4]:
                title=f'M7-USER{uid}-{index:03}'
                statements.append("INSERT INTO qixu_browser.notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at,delivered_at) VALUES("+str(uid)+",'"+title+"','"+title+"','M7 isolated result notification','SPACE',2000,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6));INSERT INTO qixu_browser.inbox(outbox_id,recipient_id,title,message,entity_type,entity_id,created_at) SELECT id,recipient_id,title,message,entity_type,entity_id,created_at FROM qixu_browser.notification_outbox WHERE recipient_id="+str(uid)+" AND event_key='"+title+"';")
            if index<=5:
                for uid in [2,5]:
                    title=f'M7-USER{uid}-{index:03}';statements.append("INSERT INTO qixu_browser.notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at,delivered_at) VALUES("+str(uid)+",'"+title+"','"+title+"','M7 other isolated actor','SPACE',2000,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6));INSERT INTO qixu_browser.inbox(outbox_id,recipient_id,title,message,entity_type,entity_id,created_at) SELECT id,recipient_id,title,message,entity_type,entity_id,created_at FROM qixu_browser.notification_outbox WHERE recipient_id="+str(uid)+" AND event_key='"+title+"';")
        sql(''.join(statements))
        for port,folder in roots.items():
            server=ThreadingHTTPServer(('127.0.0.1',port),Handler);server.document_root=folder;servers.append(server);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start();threads.append(thread)
        write(installation/'ready.private.json',facts());print(json.dumps({'state':'OWNED_BROWSER_READY','source_sha':source,'ports':PORTS}),flush=True)
        for command in sys.stdin:
            command=command.strip()
            if command=='arm-favorite':
                with lock:assert not fault['armed'] and not fault['records'];fault['armed']=True
                print('{"state":"NEXT_FAVORITE_RESPONSE_ARMED"}',flush=True)
            elif command=='facts':print(json.dumps(facts()),flush=True)
            elif command=='stop':break
            else:raise RuntimeError('Unknown fixture command')
        write(out/'fixture-facts.json',facts())
    finally:
        for server in servers:server.shutdown();server.server_close()
        for thread in threads:thread.join(3)
        for child,command,label in reversed(children):
            if child.poll() is None:
                ps="$p=Get-CimInstance Win32_Process -Filter 'ProcessId="+str(child.pid)+"';$p|Select-Object ProcessId,ExecutablePath,CommandLine|ConvertTo-Json -Compress"
                row=read_json=subprocess.check_output(['powershell','-NoProfile','-Command',ps],text=True);row=json.loads(row)
                assert Path(row['ExecutablePath']).resolve()==Path(command[0]).resolve() and all(value in row['CommandLine'] for value in command[1:]);write(installation/('stop-'+label+'.private.json'),row)
                child.terminate();child.wait(20)
            cleanup.append({'label':label,'stopped':child.poll() is not None})
        for log in logs:log.close()
        free={}
        for port in PORTS:
            with socket.socket() as s:free[str(port)]=s.connect_ex(('127.0.0.1',port))!=0
        write(out/'fixture-cleanup.json',{'owned':cleanup,'ports_free':free,'threads_stopped':all(not t.is_alive() for t in threads)})
        print(json.dumps({'state':'OWNED_BROWSER_STOPPED','ports_free':free}),flush=True)
if __name__=='__main__':main()
