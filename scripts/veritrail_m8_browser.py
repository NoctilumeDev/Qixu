"""Source-bound M8 information/transition witnesses; CUA performs the real UI actions."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,sys,uuid,re
from urllib.parse import urlsplit
from veritrail.acceptance_plan import seal_acceptance_plan,observation_spec_digest
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT=Path(__file__).resolve().parents[1]
CHECKS={
 'student-feedback':['反馈','等待核实'],
 'facility-invalid':['请明确选择有或无','未知不能作为缺失事实提交'],
 'facility-confirmed':['已核实','有插座 = 有'],
 'student-public-fact':['A001','插座','有'],
 'venue-desktop':['预计人数 / 当前容量','1 / 8人','teacher1','M8测试联系方式','查看公开空间档案'],
 'venue-mobile':['预计人数 / 当前容量','1 / 8人','teacher1','M8测试联系方式','批准并建立空间权'],
 'event-draft-error':['请填写处理说明','处理说明（必填）','场地核验后发布'],
 'event-open':['R-A','预约开放','预约截止','递补停止','有效短期预约重叠','预约参加'],
 'event-confirmed':['我的状态：已确认','查看我的已有安排'],
 'event-waitlisted':['我的状态：候补','预约截止','递补停止'],
 'admin-after-bearer-exit':['空间地图','三楼管理员'],
 'module-cleared':['公开长期批次'],
 'failed-batch':['未能在公布期限内完成冻结','FREEZE_DEADLINE_MISSED','普通空间与其他选择','明确退出本轮'],
 'failed-exit':['本轮未产生结果','普通空间与其他选择'],
 'student-feedback-list':['我的反馈','待核实','A001','西区单人位 1'],
 'repair-linked':['经核实的空间事实','当前版本','维修执行人'],
 'event-published':['M8 原条件读书会复验','预约开放'],
 'student-report-identity':['A001','西区单人位 1','查看空间档案'],
}
MOBILE={'student-feedback','student-public-fact','venue-mobile','event-open','event-confirmed','event-waitlisted','failed-batch','failed-exit','student-feedback-list','repair-linked','student-report-identity'}
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def read(path):return json.loads(Path(path).read_text(encoding='utf-8'))
def write(path,data):
 with Path(path).open('x',encoding='utf-8',newline='\n') as f:json.dump(data,f,ensure_ascii=False,indent=2);f.write('\n')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
def rule(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'browser','path':'/facts'+path},'operator':'eq','right':value}

if sys.argv[1]=='seal':
 assert not git('status','--porcelain')
 native,front=[(ROOT/x).resolve() for x in sys.argv[2:4]]
 for p in [native,front]:assert p.is_relative_to(ROOT/'artifacts/local') and read(p/'bundle/acceptance-report.json')['verdict']=='PASS'
 n,f=read(native/'evidence.json'),read(front/'evidence.json');source=git('rev-parse','HEAD')
 assert n['facts']['source_sha']==f['facts']['source_sha']==source and n['source']=='qixu-native/0.22' and f['source']=='qixu-frontend/0.7'
 out=ROOT/'artifacts/local'/('m8-browser-'+uuid.uuid4().hex);out.mkdir()
 req={'source_sha':source,'collector':'qixu-m8-browser/0.2','native_evidence_sha256':digest(native/'evidence.json'),'frontend_evidence_sha256':digest(front/'evidence.json'),'observer_sha256':digest(__file__),'fixture_sha256':digest(ROOT/'scripts/veritrail_m7_browser_fixture.py'),'contract_sha256':digest(ROOT/'docs/contracts/m8-browser.md'),'tasks_enabled':False}
 spec={'id':'browser','contract':{'id':'qixu-m8-browser','version':'0.2'},'evidence_type':'qixu.m8.browser','coordinates':req,'projections':['source_sha','source_clean','captures','sql','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
 assertions=[rule('source','/source_sha',source),rule('clean','/source_clean',True),rule('static','/sql/static_bytes_checked',True),rule('cleanup','/cleanup',True)]
 for name,words in CHECKS.items():
  assertions.append(rule(name+'-fresh','/captures/'+name+'/fresh',True))
  for i,_ in enumerate(words):assertions.append(rule(name+'-text-'+str(i),'/captures/'+name+'/text/'+str(i),True))
  if name in MOBILE:assertions+=[rule(name+'-mobile','/captures/'+name+'/mobile_width',True),rule(name+'-overflow','/captures/'+name+'/no_document_overflow',True)]
 assertions+=[rule('no-old-message','/captures/module-cleared/old_receipt_absent',True),rule('exited-button','/captures/failed-exit/exit_button_absent',True)]
 for name in ['outlet_verified_present','outlet_not_manufactured_absent','event_published','both_participation_states','short_kept','explicit_exit']:assertions.append(rule(name,'/sql/'+name,True))
 plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m8-browser','version':2,'subject':{'id':'qixu-m8-browser','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Do the declared real M8 pages show required decision facts, legal next actions, actor/route feedback and their specified SQL effects on this installed candidate? This is not all interleavings, native device or M10 visual qualification.','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:browser-observer','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'browser','observation_spec_id':'browser','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'browser','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu M8 original feedback recheck','consumers':['M8-review']},'reproduction_steps':['Bind exact native0.22/frontend0.7 original PASS and seal before installing.','Use CUA and explicit fake demo data to perform named original operations; preserve raw DOM/JPEG/URL/window viewport/UTC.','Read actual SQL effects, stop owned fixture, retain independent product disposition separately. Text/presence checks do not prove every interaction or accessibility standard.'],'cleanup_steps':['Only retained child handles after executable/command ownership verification.','Preserve original records and unknown boundaries; never stop shared services.']})
 write(out/'request.json',req);write(out/'sealed-plan.json',plan);write(out/'capture-contract.json',{'sealed_at':datetime.now(timezone.utc).isoformat(),'checks':CHECKS,'mobile':sorted(MOBILE)})
 print(json.dumps({'identity':out.name,'source_sha':source,'state':'SEALED_BEFORE_INSTALLATION'}))
else:
 out=(ROOT/sys.argv[2]).resolve();assert out.is_relative_to(ROOT/'artifacts/local')
 req,plan,contract=[read(out/name) for name in ['request.json','sealed-plan.json','capture-contract.json']]
 assert req['observer_sha256']==digest(__file__) and req['fixture_sha256']==digest(ROOT/'scripts/veritrail_m7_browser_fixture.py') and req['contract_sha256']==digest(ROOT/'docs/contracts/m8-browser.md')
 captures={};sealed=datetime.fromisoformat(contract['sealed_at']);now=datetime.now(timezone.utc)
 for name,words in contract['checks'].items():
  meta,dom,shot=[out/(name+suffix) for suffix in ['.json','.txt','.jpg']]
  if not all(p.is_file() for p in [meta,dom,shot]):continue
  m=read(meta);text=dom.read_text(encoding='utf-8');raw=shot.read_bytes();at=datetime.fromisoformat(m['capturedAt'].replace('Z','+00:00'))
  captures[name]={'fresh':sealed<=at<=now and urlsplit(m['url']).netloc in ['127.0.0.1:6982','127.0.0.1:6983'] and raw.startswith(b'\xff\xd8') and len(raw)>1000,'text':[word in text for word in words],'mobile_width':m['viewport']['width']==390,'no_document_overflow':m['viewport']['scrollWidth']<=m['viewport']['width']+1,'old_receipt_absent':'已确认回执' not in text,'exit_button_absent':not bool(re.search(r'^\s*- button \"明确退出本轮\"(?:\s|$)',text,re.MULTILINE)),'url':m['url'],'viewport':m['viewport'],'dom_sha256':digest(dom),'screenshot_sha256':digest(shot)}
 complete=(out/'fixture-facts.json').is_file() and (out/'fixture-cleanup.json').is_file()
 measured=read(out/'fixture-facts.json') if complete else {};cleanup=read(out/'fixture-cleanup.json') if complete else {};m=measured.get('m8',{})
 sql={'static_bytes_checked':measured.get('static_bytes_checked') is True,'outlet_verified_present':m.get('outlet_true_facts',0)>=1,'outlet_not_manufactured_absent':m.get('outlet_false_facts')==0,'event_published':m.get('published_events',0)>=1,'both_participation_states':m.get('confirmed_parts',0)>=1 and m.get('waitlisted_parts',0)>=1,'short_kept':m.get('kept_short',0)>=1,'explicit_exit':m.get('exited_applications',0)>=1,'observed':m}
 facts={'source_sha':git('rev-parse','HEAD'),'source_clean':not bool(git('status','--porcelain')),'captures':captures,'sql':sql,'cleanup':len(cleanup.get('owned',[]))==2 and len(cleanup.get('ports_free',{}))==4 and all(x['stopped'] for x in cleanup.get('owned',[])) and all(cleanup.get('ports_free',{}).values()) and cleanup.get('threads_stopped') is True,'boundary':'DECLARED_M8_H5_ADMIN_INFORMATION_TRANSITIONS_SQL_NOT_ALL_ASYNC_SCHEDULES_DEVICE_PRODUCTION_OR_M10_VISUAL'}
 spec=plan['observation_specs'][0];coverage=complete and len(captures)==len(CHECKS)
 evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-m8-browser/0.2','captured_at':now.isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(req),'collection_session_id':out.name,'collector_role':'qixu-m8-browser-observer','coverage':'COMPLETE' if coverage else 'ERROR','normalization_semantics_version':'qixu-m8-browser/0.2','facts_digest':sha256_json(facts)}}}
 write(out/'evidence.json',evidence);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status='COMPLETED' if coverage else 'ERROR')
 print(json.dumps({'identity':out.name,'verdict':report['verdict'],'captures':len(captures),'sql':sql,'cleanup':facts['cleanup']}));sys.exit(0 if report['verdict']=='PASS' else 1)
