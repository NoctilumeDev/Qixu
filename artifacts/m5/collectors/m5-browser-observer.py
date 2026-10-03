from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,uuid,sys
from veritrail.acceptance_plan import seal_acceptance_plan,observation_spec_digest
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
root=Path(__file__).resolve().parents[1]
checks={
 'student-map-context':['空间地图','A018','150%'],
 'student-space':['A018','查看使用时段','演示示意图片'],
 'student-favorites':['收藏与对比','A018'],
 'student-short-canceled':['短期预约','A018','已取消'],
 'teacher-venue-approved':['场地申请','已批准'],
 'event-published':['窗边共读','已发布'],
 'student-event-confirmed':['窗边共读','已确认'],
 'student-event-canceled':['窗边共读','已取消'],
 'student-report-photo':['反馈','受权限保护的照片'],
 'repair-work-done':['维修事项','工作已完成，待复验'],
 'repair-missing-fact':['请保留输入','工作已完成，待复验'],
 'repair-closed':['维修事项','复验关闭'],
 'student-report-resolved':['反馈','已解决','重新核实'],
 'student-result':['批次','候补'],
 'student-inbox':['消息','未读'],
 'teacher-no-admin':['当前身份或管理范围无此权限'],
 'cookie-other-document':['重新加载','会话'],
}
def new(p,v):
 with p.open('x',encoding='utf-8',newline='\n') as f:json.dump(v,f,ensure_ascii=False,indent=2,sort_keys=True);f.write('\n')
def git(*a):return subprocess.check_output(['git',*a],cwd=root,text=True).strip()
if sys.argv[1]=='seal':
 assert not git('status','--porcelain')
 native=root/sys.argv[2];front=root/sys.argv[3]
 for producer in [native,front]:assert json.loads((producer/'bundle/acceptance-report.json').read_text(encoding='utf-8'))['verdict']=='PASS'
 ne=json.loads((native/'evidence.json').read_text(encoding='utf-8'));fe=json.loads((front/'evidence.json').read_text(encoding='utf-8'))
 sha=git('rev-parse','HEAD');assert ne['facts']['source_sha']==fe['facts']['source_sha']==sha
 out=root/'artifacts/local'/('m5-browser-'+uuid.uuid4().hex);out.mkdir()
 req={'source_sha':sha,'collector':'qixu-manual-browser/0.1','native_evidence_sha256':hashlib.sha256((native/'evidence.json').read_bytes()).hexdigest(),'frontend_evidence_sha256':hashlib.sha256((front/'evidence.json').read_bytes()).hexdigest(),'jar_sha256':ne['facts']['package']['sha256'],'frontend_manifests':{k:v['manifest_sha256'] for k,v in fe['facts']['artifacts'].items()},'observer_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
 spec={'id':'installed-browser','contract':{'id':'qixu-manual-browser','version':'0.1'},'evidence_type':'qixu.browser.observation','coordinates':req,'projections':['source_sha','source_clean','captures'],'canonicalization_profile':'veritrail-json-c14n/1'}
 def rule(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'browser','path':path},'operator':'eq','right':value}
 assertions=[rule('source','/facts/source_sha',sha),rule('clean','/facts/source_clean',True)]
 for key,words in checks.items():
  assertions.append(rule(key+'-capture','/facts/captures/'+key+'/fresh_capture',True))
  for i,w in enumerate(words):assertions.append(rule(key+'-text-'+str(i),'/facts/captures/'+key+'/texts/'+str(i),True))
 plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m5-installed-browser','version':1,'subject':{'id':'qixu-m5-installed-browser','version':sha,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Are the declared real installed-browser states captured at bound current producers? Text and screenshot evidence do not claim native devices or final visual fidelity.','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:browser-observer','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'browser','observation_spec_id':spec['id'],'cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'collected','left':{'requirement_id':'browser','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu bounded installed page observation','consumers':['M5 installed state readback']},'reproduction_steps':['Validate both exact producer evidence and all frontend static bytes before starting own servers.','Use CUA browser interaction to follow M5 observation contract, capture fresh DOM and screenshots plus current URL/viewport after each named state.','Read actual SQL and review workflow transitions separately; DOM text presence cannot prove all business semantics.'],'cleanup_steps':['Stop only owned verified frontend/backend processes.','Retain first failures and raw captures; do not stop shared MySQL.']})
 new(out/'sealed-plan.json',plan);new(out/'request.json',req);new(out/'capture-contract.json',{'sealed_at':datetime.now(timezone.utc).isoformat(),'checks':checks})
 print(json.dumps({'identity':out.name,'source_sha':sha,'state':'SEALED_BEFORE_INSTALLATION_AND_CAPTURE'}))
else:
 out=(root/sys.argv[2]).resolve();assert out.is_relative_to(root/'artifacts/local')
 plan=json.loads((out/'sealed-plan.json').read_text(encoding='utf-8'));req=json.loads((out/'request.json').read_text(encoding='utf-8'));contract=json.loads((out/'capture-contract.json').read_text(encoding='utf-8'))
 assert req['observer_sha256']==hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
 sealed=datetime.fromisoformat(contract['sealed_at']);captures={}
 for key,words in contract['checks'].items():
  meta=out/(key+'.json');dom=out/(key+'.txt');shot=out/(key+'.png')
  if not all(p.is_file() for p in [meta,dom,shot]):continue
  m=json.loads(meta.read_text(encoding='utf-8'));t=dom.read_text(encoding='utf-8');raw=shot.read_bytes();host=m['url'].split('/')[2]
  fresh=datetime.fromisoformat(m['capturedAt'].replace('Z','+00:00'))>=sealed and host in ['127.0.0.1:6968','127.0.0.1:6969'] and len(raw)>1000
  captures[key]={'fresh_capture':fresh,'texts':[w in t for w in words],'dom_sha256':hashlib.sha256(dom.read_bytes()).hexdigest(),'screenshot_sha256':hashlib.sha256(raw).hexdigest(),'screenshot_bytes':len(raw),'url':m['url'],'viewport':m['viewport'],'captured_at':m['capturedAt']}
 facts={'source_sha':git('rev-parse','HEAD'),'source_clean':not bool(git('status','--porcelain')),'captures':captures,'boundary':'DOM_STATE_CAPTURE_AT_BOUND_INSTALLATION_NOT_ALL_BUSINESS_SEMANTICS_DEVICE_OR_M10_VISUAL'}
 spec=plan['observation_specs'][0];e={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-manual-browser/0.1','captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(req),'collection_session_id':out.name,'collector_role':'qixu-browser-observer','coverage':'COMPLETE' if len(captures)==len(contract['checks']) else 'ERROR','normalization_semantics_version':'qixu-manual-browser/0.1','facts_digest':sha256_json(facts)}}}
 new(out/'evidence.json',e);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status='COMPLETED')
 print(json.dumps({'identity':out.name,'verdict':report['verdict'],'captures':len(captures),'boundary':facts['boundary']}));sys.exit(0 if report['verdict']=='PASS' else 1)
