"""Seal an exact installed UI observation before boot; CUA supplies real captures."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,uuid,sys
from urllib.parse import urlsplit
from veritrail.acceptance_plan import seal_acceptance_plan,observation_spec_digest
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
ROOT=Path(__file__).resolve().parents[1]
CHECKS={
 'student-first':['151条未读','共151条','1 / 8','M7-USER1-151'],
 'student-old':['8 / 8','M7-USER1-001'],
 'student-read':['8 / 8','150条未读','已读'],
 'admin-first':['151条未读','共151条','1 / 8','M7-USER4-151'],
 'admin-old':['8 / 8','M7-USER4-001'],
 'admin-read':['8 / 8','150条未读','已读'],
 'student-unknown':['提交结果待确认','查询回执'],
 'student-reloaded-unknown':['提交结果待确认','查询回执'],
 'student-recovered':['原请求已确认'],
 'cookie-stopped':['会话已变更','停止读写','重新加载并核对身份'],
 'cookie-reloaded':['三楼管理员','空间地图'],
 'student-other':['5条未读','共5条','M7-USER2-005'],
 'map-return':['空间地图','安静'],
 'admin-mobile':['我的站内通知','M7-USER5-005'],
}
def digest(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def read(p):return json.loads(Path(p).read_text(encoding='utf-8'))
def write(p,v):
 with Path(p).open('x',encoding='utf-8',newline='\n') as f:json.dump(v,f,ensure_ascii=False,indent=2);f.write('\n')
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT,text=True).strip()
def rule(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'browser','path':'/facts'+path},'operator':'eq','right':value}
if sys.argv[1]=='seal':
 assert not git('status','--porcelain')
 native,front=[(ROOT/value).resolve() for value in sys.argv[2:4]]
 for producer in [native,front]:assert producer.is_relative_to(ROOT/'artifacts/local') and read(producer/'bundle/acceptance-report.json')['verdict']=='PASS'
 ne,fe=read(native/'evidence.json'),read(front/'evidence.json');sha=git('rev-parse','HEAD');assert ne['facts']['source_sha']==fe['facts']['source_sha']==sha
 assert ne['source']=='qixu-native/0.19' and fe['source']=='qixu-frontend/0.4'
 out=ROOT/'artifacts/local'/('m7-browser-'+uuid.uuid4().hex);out.mkdir()
 req={'source_sha':sha,'collector':'qixu-m7-browser/0.2','native_evidence_sha256':digest(native/'evidence.json'),'frontend_evidence_sha256':digest(front/'evidence.json'),'jar_sha256':ne['facts']['package']['sha256'],'frontend_manifests':{k:v['manifest_sha256'] for k,v in fe['facts']['artifacts'].items()},'observer_sha256':digest(__file__),'fixture_sha256':digest(ROOT/'scripts/veritrail_m7_browser_fixture.py'),'contract_sha256':digest(ROOT/'docs/contracts/m7-browser.md')}
 spec={'id':'browser','contract':{'id':'qixu-m7-browser','version':'0.2'},'evidence_type':'qixu.m7.browser','coordinates':req,'projections':['source_sha','source_clean','captures','sql','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
 assertions=[rule('source','/source_sha',sha),rule('clean','/source_clean',True)]
 for key,words in CHECKS.items():
  assertions.append(rule(key+'-fresh','/captures/'+key+'/fresh',True))
  for i,word in enumerate(words):assertions.append(rule(key+'-text-'+str(i),'/captures/'+key+'/text/'+str(i),True))
 for key in ['student-first','student-old','student-read']:assertions.append(rule(key+'-private','/captures/'+key+'/foreign_absent',True))
 for key in ['admin-first','admin-old','admin-read']:assertions.append(rule(key+'-private','/captures/'+key+'/foreign_absent',True))
 assertions+=[rule('recovered-no-unknown','/captures/student-recovered/pending_absent',True),rule('other-no-old','/captures/student-other/foreign_absent',True),rule('mobile-width','/captures/admin-mobile/mobile_width',True),rule('mobile-overflow','/captures/admin-mobile/no_document_overflow',True),rule('sql-inbox','/sql/inbox',{'1':[151,150],'2':[5,5],'4':[151,150],'5':[5,5]}),rule('sql-favorite','/sql/favorite_count',1),rule('sql-lost-status','/sql/lost_status',200),rule('sql-lost-count','/sql/lost_count',1),rule('sql-receipt','/sql/receipt_count',1),rule('static-bytes','/sql/static_bytes_checked',True),rule('cleanup','/cleanup',True)]
 plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m7-browser','version':2,'subject':{'id':'qixu-m7-browser','version':sha,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Do actual bound pages preserve old notifications, committed unknown intentions and current actor context in declared isolated browser traces?','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:browser-observer','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'browser','observation_spec_id':'browser','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'browser','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu M7 installed browser','consumers':['M7-browser']},'reproduction_steps':['Use fresh exact native0.19 and frontend0.4 original PASS; seal before fixture initialization.','Launch owned fixture; use CUA to perform named states and save DOM/JPEG/URL/viewport with fresh timestamps.','Stop fixture after SQL readback; judge captures, counters and positive owned cleanup.'],'cleanup_steps':['Stop only retained child handles with exe/command verification; never touch old shared ports.','Preserve raw private captures and original bundle regardless of verdict.']})
 write(out/'sealed-plan.json',plan);write(out/'request.json',req);write(out/'capture-contract.json',{'sealed_at':datetime.now(timezone.utc).isoformat(),'checks':CHECKS});print(json.dumps({'identity':out.name,'source_sha':sha,'state':'SEALED_BEFORE_INSTALLATION'}))
else:
 out=(ROOT/sys.argv[2]).resolve();assert out.is_relative_to(ROOT/'artifacts/local');plan=read(out/'sealed-plan.json');req=read(out/'request.json');contract=read(out/'capture-contract.json');assert req['observer_sha256']==digest(__file__) and req['fixture_sha256']==digest(ROOT/'scripts/veritrail_m7_browser_fixture.py')
 captures={};sealed=datetime.fromisoformat(contract['sealed_at'])
 for key,words in contract['checks'].items():
  meta,dom,shot=[out/(key+suffix) for suffix in ['.json','.txt','.jpg']]
  if not all(p.is_file() for p in [meta,dom,shot]):continue
  m=read(meta);t=dom.read_text(encoding='utf-8');raw=shot.read_bytes()
  foreign=('M7-USER2-' not in t and 'M7-USER4-' not in t) if key.startswith('student-') and key!='student-other' else ('M7-USER1-' not in t and 'M7-USER5-' not in t) if key.startswith('admin-') else True
  if key=='student-other':foreign='M7-USER1-' not in t
  captures[key]={'fresh':datetime.fromisoformat(m['capturedAt'].replace('Z','+00:00'))>=sealed and urlsplit(m['url']).netloc in ['127.0.0.1:6982','127.0.0.1:6983'] and raw.startswith(b'\xff\xd8') and len(raw)>1000,'text':[word in t for word in words],'foreign_absent':foreign,'pending_absent':'提交结果待确认' not in t,'mobile_width':m['viewport']['width']==390,'no_document_overflow':m['viewport']['scrollWidth']<=m['viewport']['width']+1,'dom_sha256':digest(dom),'screenshot_sha256':digest(shot),'url':m['url'],'viewport':m['viewport']}
 fixture_complete=(out/'fixture-facts.json').is_file() and (out/'fixture-cleanup.json').is_file();measured=read(out/'fixture-facts.json') if (out/'fixture-facts.json').is_file() else {'inbox':'','favorite_count':None,'lost_responses':[],'static_bytes_checked':False};cleanup=read(out/'fixture-cleanup.json') if (out/'fixture-cleanup.json').is_file() else {'owned':[],'ports_free':{},'threads_stopped':False};rows=measured['inbox'].splitlines();inbox={}
 for row in rows:
  uid,total,unread=row.split('\t');inbox[uid]=[int(total),int(unread)]
 lost=measured['lost_responses'];sql={'inbox':inbox,'favorite_count':measured['favorite_count'],'lost_count':len(lost),'lost_status':lost[0]['status'] if len(lost)==1 else None,'receipt_count':lost[0]['count'] if len(lost)==1 else None,'static_bytes_checked':measured['static_bytes_checked']}
 facts={'source_sha':git('rev-parse','HEAD'),'source_clean':not bool(git('status','--porcelain')),'captures':captures,'sql':sql,'cleanup':len(cleanup['owned'])==2 and len(cleanup['ports_free'])==4 and all(p['stopped'] for p in cleanup['owned']) and all(cleanup['ports_free'].values()) and cleanup['threads_stopped'],'boundary':'DECLARED_REAL_H5_ADMIN_BROWSER_SQL_NOT_NATIVE_DEVICE_ALL_STORAGE_POLICIES_OR_M10_VISUAL'}
 spec=plan['observation_specs'][0];e={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-m7-browser/0.2','captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(req),'collection_session_id':out.name,'collector_role':'qixu-browser-observer','coverage':'COMPLETE' if fixture_complete and len(captures)==len(CHECKS) else 'ERROR','normalization_semantics_version':'qixu-m7-browser/0.2','facts_digest':sha256_json(facts)}}}
 write(out/'evidence.json',e);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status='COMPLETED' if fixture_complete and len(captures)==len(CHECKS) else 'ERROR');print(json.dumps({'identity':out.name,'verdict':report['verdict'],'captures':len(captures),'sql':sql,'cleanup':facts['cleanup']}));sys.exit(0 if report['verdict']=='PASS' else 1)
