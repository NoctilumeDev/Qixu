"""Focused PM11 real-page proof; never rewrites the prior M8 Plan/verdict."""
from pathlib import Path
from datetime import datetime,timezone
from urllib.parse import urlsplit
import hashlib,json,re,subprocess,sys,uuid
from veritrail.acceptance_plan import seal_acceptance_plan,observation_spec_digest
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT=Path(__file__).resolve().parents[1]
CHECKS={
 'cold-initial-error':['无法确认登录身份','去登录'],
 'student-current-identity':['陈同学','student2','退出当前登录'],
 'event-waitlisted':['我的状态：候补','预约截止','递补停止'],
 'own-arrangements':['没有短期预约','候补'],
 'event-venue-profile':['R-A','研讨室 A','容量8人'],
}
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):
 with p.open('x',encoding='utf-8',newline='\n') as f:json.dump(v,f,ensure_ascii=False,indent=2);f.write('\n')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT,text=True).strip()
def rule(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'browser','path':'/facts'+path},'operator':'eq','right':value}

if sys.argv[1]=='seal':
 assert not git('status','--porcelain')
 native,front=[(ROOT/p).resolve() for p in sys.argv[2:4]]
 source=git('rev-parse','HEAD')
 for p in (native,front):
  assert p.is_relative_to(ROOT/'artifacts/local') and read(p/'bundle/acceptance-report.json')['verdict']=='PASS'
  assert read(p/'evidence.json')['facts']['source_sha']==source
 assert read(native/'evidence.json')['source']=='qixu-native/0.22'
 assert read(front/'evidence.json')['source']=='qixu-frontend/0.8'
 out=ROOT/'artifacts/local'/('m8-identity-browser-'+uuid.uuid4().hex);out.mkdir()
 req={'source_sha':source,'collector':'qixu-m8-browser/0.3','native_evidence_sha256':digest(native/'evidence.json'),'frontend_evidence_sha256':digest(front/'evidence.json'),'observer_sha256':digest(Path(__file__)),'fixture_sha256':digest(ROOT/'scripts/veritrail_m7_browser_fixture.py'),'contract_sha256':digest(ROOT/'docs/contracts/m8-repairs.md'),'tasks_enabled':False}
 spec={'id':'browser','contract':{'id':'qixu-m8-identity-browser','version':'0.1'},'evidence_type':'qixu.m8.identity.browser','coordinates':req,'projections':['source_sha','source_clean','captures','sql','cleanup'],'canonicalization_profile':'veritrail-json-c14n/1'}
 assertions=[rule('source','/source_sha',source),rule('clean','/source_clean',True),rule('static','/sql/static_bytes_checked',True),rule('cleanup','/cleanup',True)]
 for name,words in CHECKS.items():
  for flag in ('fresh','mobile','no_overflow'):assertions.append(rule(name+'-'+flag,'/captures/'+name+'/'+flag,True))
  for i in range(len(words)):assertions.append(rule(name+'-text-'+str(i),'/captures/'+name+'/text/'+str(i),True))
  if name!='cold-initial-error':assertions.append(rule(name+'-no-old-identity-error','/captures/'+name+'/old_identity_error_absent',True))
 for flag in ('both_participation_states','short_kept'):assertions.append(rule(flag,'/sql/'+flag,True))
 plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m8-identity-browser','version':1,'subject':{'id':'qixu-m8-identity-browser','version':source,'source_ref':'github:NoctilumeDev/Qixu'},'question':'Does the declared cold identity error disappear only after valid Bearer login and stay absent on the three original PM11 routes, with actual participation and short reservation facts retained? Not all UI routes, async schedules, native devices or M10 design.','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:identity-browser-observer','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'browser','observation_spec_id':'browser','cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'browser','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu PM11 focused repair','consumers':['M8-review']},'reproduction_steps':['Seal on clean exact source before owned fixture installation.','CUA: same browser admin Cookie; unauthenticated student cold page then valid student login; event waitlist, own arrangements and public venue.','Preserve raw DOM/JPEG/URL/UTC/window plus actual SQL and owned cleanup; no browser fetch/evaluate mutations.'],'cleanup_steps':['Stop only fixture-owned processes/threads and close temporary tabs; retain first failure and original plans.']})
 write(out/'request.json',req);write(out/'sealed-plan.json',plan);write(out/'capture-contract.json',{'sealed_at':datetime.now(timezone.utc).isoformat(),'checks':CHECKS})
 print(json.dumps({'identity':out.name,'source_sha':source,'state':'SEALED_BEFORE_INSTALLATION'}))
else:
 out=(ROOT/sys.argv[2]).resolve();assert out.is_relative_to(ROOT/'artifacts/local')
 req,plan,contract=[read(out/p) for p in ('request.json','sealed-plan.json','capture-contract.json')]
 assert req['observer_sha256']==digest(Path(__file__)) and req['fixture_sha256']==digest(ROOT/'scripts/veritrail_m7_browser_fixture.py') and req['contract_sha256']==digest(ROOT/'docs/contracts/m8-repairs.md')
 sealed=datetime.fromisoformat(contract['sealed_at']);now=datetime.now(timezone.utc);captures={}
 for name,words in contract['checks'].items():
  meta,dom,shot=[out/(name+s) for s in ('.json','.txt','.jpg')]
  if not all(p.is_file() for p in (meta,dom,shot)):continue
  m=read(meta);text=dom.read_text(encoding='utf-8');at=datetime.fromisoformat(m['capturedAt'].replace('Z','+00:00'));raw=shot.read_bytes();v=m['viewport']
  captures[name]={'fresh':sealed<=at<=now and urlsplit(m['url']).netloc=='127.0.0.1:6982' and raw.startswith(b'\xff\xd8') and len(raw)>1000,'mobile':v['width']==390,'no_overflow':v['scrollWidth']<=v['width']+1,'text':[w in text for w in words],'old_identity_error_absent':'无法确认登录身份' not in text,'url':m['url'],'viewport':v,'dom_sha256':digest(dom),'screenshot_sha256':digest(shot)}
 complete=(out/'fixture-facts.json').is_file() and (out/'fixture-cleanup.json').is_file()
 measured=read(out/'fixture-facts.json') if complete else {};cleanup=read(out/'fixture-cleanup.json') if complete else {};m=measured.get('m8',{})
 sql={'static_bytes_checked':measured.get('static_bytes_checked') is True,'both_participation_states':m.get('confirmed_parts',0)>=1 and m.get('waitlisted_parts',0)>=1,'short_kept':m.get('kept_short',0)>=1,'observed':m}
 facts={'source_sha':git('rev-parse','HEAD'),'source_clean':not bool(git('status','--porcelain')),'captures':captures,'sql':sql,'cleanup':len(cleanup.get('owned',[]))==2 and len(cleanup.get('ports_free',{}))==4 and all(p['stopped'] for p in cleanup.get('owned',[])) and all(cleanup.get('ports_free',{}).values()) and cleanup.get('threads_stopped') is True,'boundary':'FOCUSED_PM11_H5_IDENTITY_PROJECTION_AND_SQL_NOT_FULL_M8_OR_DEVICE_QUALIFICATION'}
 spec=plan['observation_specs'][0];coverage=complete and len(captures)==len(CHECKS)
 evidence={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-m8-browser/0.3','captured_at':now.isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(req),'collection_session_id':out.name,'collector_role':'qixu-m8-identity-browser-observer','coverage':'COMPLETE' if coverage else 'ERROR','normalization_semantics_version':'qixu-m8-browser/0.3','facts_digest':sha256_json(facts)}}}
 write(out/'evidence.json',evidence);report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status='COMPLETED' if coverage else 'ERROR')
 print(json.dumps({'identity':out.name,'verdict':report['verdict'],'captures':len(captures),'sql':sql,'cleanup':facts['cleanup']}));sys.exit(0 if report['verdict']=='PASS' else 1)
