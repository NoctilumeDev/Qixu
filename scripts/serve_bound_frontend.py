from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlsplit
import http.client,json,sys,threading,hashlib
root=Path(__file__).resolve().parents[1]
producer=(root/sys.argv[1]).resolve()
assert producer.is_relative_to(root/'artifacts/local')
e=json.loads((producer/'evidence.json').read_text(encoding='utf-8'))
assert e['facts']['source_clean'] and all(c['exit_code']==0 for c in e['facts']['commands'])
for name,relative in [('h5','student/dist/build/h5'),('admin','admin/dist')]:
 folder=producer/'work'/relative
 for name2,v in e['facts']['artifacts'][name]['files'].items():
  data=(folder/name2).read_bytes();assert len(data)==v['bytes'] and hashlib.sha256(data).hexdigest()==v['sha256']
class Handler(SimpleHTTPRequestHandler):
 def __init__(self,*a,**k):super().__init__(*a,directory=str(a[2].document_root),**k)
 def log_message(self,*a):pass
 def proxy(self):
  size=int(self.headers.get('Content-Length','0'))
  if size>2_097_152:self.send_error(413);return
  body=self.rfile.read(size) if size else None
  headers={k:v for k,v in self.headers.items() if k.lower() not in ('host','connection','transfer-encoding','content-length')}
  headers['Host']='127.0.0.1:6967'
  conn=http.client.HTTPConnection('127.0.0.1',6967,timeout=20)
  try:
   conn.request(self.command,self.path,body,headers);r=conn.getresponse();raw=r.read();self.send_response(r.status)
   for k,v in r.getheaders():
    if k.lower() not in ('connection','transfer-encoding','content-length'):self.send_header(k,v)
   self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
  except (OSError,http.client.HTTPException):
   raw=b'{"error":{"code":"DEV_BACKEND_UNAVAILABLE","message":"Backend not reachable; submission result remains unknown."}}';self.send_response(503);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
  finally:conn.close()
 def do_GET(self):
  if urlsplit(self.path).path.startswith('/api/'):self.proxy();return
  path=self.translate_path(self.path)
  if not Path(path).exists() and not Path(urlsplit(self.path).path).suffix:self.path='/index.html'
  super().do_GET()
 def do_POST(self):
  if urlsplit(self.path).path.startswith('/api/'):self.proxy()
  else:self.send_error(405)
servers=[]
for port,relative in [(6968,'student/dist/build/h5'),(6969,'admin/dist')]:
 s=ThreadingHTTPServer(('127.0.0.1',port),Handler);s.document_root=producer/'work'/relative;servers.append(s)
for s in servers:threading.Thread(target=s.serve_forever,daemon=True).start()
print(json.dumps({'state':'BOUND_FRONTEND_SERVERS_READY','source_sha':e['facts']['source_sha'],'ports':[6968,6969]}),flush=True)
threading.Event().wait()
