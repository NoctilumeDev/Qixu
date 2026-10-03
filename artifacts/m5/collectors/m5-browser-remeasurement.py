from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, subprocess, uuid, sys
from veritrail.acceptance_plan import seal_acceptance_plan, observation_spec_digest
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

root = Path(__file__).resolve().parents[1]
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def write(p,v):
    with p.open('x',encoding='utf-8') as f: json.dump(v,f,ensure_ascii=False,indent=2,sort_keys=True)
def git(*a): return subprocess.check_output(['git',*a],cwd=root,text=True).strip()

if sys.argv[1] == 'seal':
    source = (root/sys.argv[2]).resolve()
    assert source.is_relative_to(root/'artifacts/local')
    previous = read(source/'evidence.json')
    report = read(source/'bundle/acceptance-report.json')
    assert report['verdict'] == 'FAIL' and len(previous['facts']['captures']) == 17
    assert not git('status','--porcelain') and previous['facts']['source_sha'] == git('rev-parse','HEAD')
    raw_files = {p.name:digest(p) for key in previous['facts']['captures'] for p in [source/(key+'.txt'),source/(key+'.png'),source/(key+'.json')]}
    request = {'original_identity':source.name,'source_sha':git('rev-parse','HEAD'),'original_plan_sha256':digest(source/'sealed-plan.json'),'original_evidence_sha256':digest(source/'evidence.json'),'original_contract_sha256':digest(source/'capture-contract.json'),'raw_files':raw_files,'observer_sha256':digest(Path(__file__))}
    spec = {'id':'remeasured-captures','contract':{'id':'qixu-browser-fixed-capture-remeasurement','version':'0.1'},'evidence_type':'qixu.browser.remeasurement','coordinates':request,'projections':['source_sha','source_clean','captures','all_original_bytes_unchanged'],'canonicalization_profile':'veritrail-json-c14n/1'}
    def rule(name,path,value):return {'id':name,'severity':'HARD','left':{'requirement_id':'captures','path':path},'operator':'eq','right':value}
    assertions=[rule('source','/facts/source_sha',request['source_sha']),rule('clean','/facts/source_clean',True),rule('immutable-raw','/facts/all_original_bytes_unchanged',True)]
    for key in previous['facts']['captures']:
        assertions.append(rule(key,'/facts/captures/'+key+'/accepted',True))
    plan=seal_acceptance_plan({'plan_kind':'ACCEPTANCE','schema_version':'0.1','plan_id':'qixu-m5-fixed-browser-remeasurement','version':1,'subject':{'id':'qixu-m5-fixed-browser-captures','version':request['source_sha'],'source_ref':'github:NoctilumeDev/Qixu'},'question':'Do unchanged captures collected under original presealed M5 Plan support declared page states after correcting two unsupported literal text expectations? This is retained-capture remeasurement, not a fresh browser execution.','governance':{'claim_owner_ref':'human:repository-owner','drafter_ref':'qixu:browser-observer','seal_authority_ref':'human:repository-owner:authorized-engineering-goal','seal_decision':'CONFIRMED'},'observation_specs':[spec],'evidence_requirements':[{'id':'captures','observation_spec_id':spec['id'],'cardinality':'EXACTLY_ONE'}],'sufficiency_rules':[{'id':'complete','left':{'requirement_id':'captures','path':'/metadata/veritrail_observation/coverage'},'operator':'eq','right':'COMPLETE'}],'integrity_rules':[],'assertions':assertions,'resource_budget':{'max_artifact_bytes':2097152},'change_scope':{'level':'L2_CONTRACT','owner':'Qixu minimal observer literal correction','consumers':['M5 retained installed page state evidence']},'reproduction_steps':['Verify original FAIL, original sealed-before-capture timing and every original raw byte digest.','Keep fifteen original page-state measurements; remeasure missing-recovery-fact refusal plus preserved draft and teacher-only denial from actual DOM.','Do not edit product behavior or claim fresh browser execution; independently read SQL and inspect images.'],'cleanup_steps':['Retain original FAIL unchanged.']})
    out=root/'artifacts/local'/('m5-browser-remeasure-'+uuid.uuid4().hex);out.mkdir()
    write(out/'request.json',request);write(out/'sealed-plan.json',plan)
    print(json.dumps({'identity':out.name,'state':'SEALED_BEFORE_REMEASUREMENT','original':source.name}))
else:
    out=(root/sys.argv[2]).resolve();assert out.is_relative_to(root/'artifacts/local')
    req=read(out/'request.json');plan=read(out/'sealed-plan.json');source=root/'artifacts/local'/req['original_identity']
    assert req['observer_sha256']==digest(Path(__file__))
    unchanged=all(digest(source/name)==sha for name,sha in req['raw_files'].items()) and digest(source/'sealed-plan.json')==req['original_plan_sha256'] and digest(source/'evidence.json')==req['original_evidence_sha256'] and digest(source/'capture-contract.json')==req['original_contract_sha256']
    original=read(source/'evidence.json');contract=read(source/'capture-contract.json');sealed=datetime.fromisoformat(contract['sealed_at'])
    captures={}
    for key,cap in original['facts']['captures'].items():
        t=(source/(key+'.txt')).read_text(encoding='utf-8');m=read(source/(key+'.json'))
        fresh_under_original=datetime.fromisoformat(m['capturedAt'].replace('Z','+00:00'))>=sealed
        if key=='repair-missing-fact':
            conditions=['复验通过须明确修正这项设施的已知损坏事实' in t,'输入保留' in t,'工作已完成，待复验' in t,'text: M5反例：仅勾选复验通过但没有提交插座恢复事实，系统必须拒绝关闭并保留输入。' in t]
        elif key=='teacher-no-admin':
            conditions=['text: 周老师' in t,'generic: 老师' in t,'此入口只向有范围的空间管理员开放。 身份或当前管理范围不允许。' in t,'- alert:' in t]
        else:conditions=cap['texts']
        captures[key]={'accepted':unchanged and cap['fresh_capture'] and fresh_under_original and all(conditions),'conditions':conditions,'original_dom_sha256':cap['dom_sha256'],'original_screenshot_sha256':cap['screenshot_sha256'],'original_captured_at':m['capturedAt']}
    facts={'source_sha':git('rev-parse','HEAD'),'source_clean':not bool(git('status','--porcelain')),'all_original_bytes_unchanged':unchanged,'captures':captures,'boundary':'REMEASUREMENT_OF_UNCHANGED_PRESEALED_INSTALLED_CAPTURES_NOT_A_FRESH_BROWSER_RUN_FULL_BUSINESS_DEVICE_OR_M10_VISUAL'}
    spec=plan['observation_specs'][0]
    e={'schema_version':'0.1','evidence_type':spec['evidence_type'],'source':'qixu-browser-fixed-capture-remeasurement/0.1','captured_at':datetime.now(timezone.utc).isoformat(),'facts':facts,'metadata':{'veritrail_observation':{'schema_version':'0.1','canonicalization_profile':'veritrail-json-c14n/1','plan_digest':plan['seal']['digest'],'observation_spec_digest':observation_spec_digest(spec),'request_seal_digest':sha256_json(req),'collection_session_id':out.name,'collector_role':'qixu-fixed-browser-remeasurement','coverage':'COMPLETE' if len(captures)==17 else 'ERROR','normalization_semantics_version':'qixu-browser-fixed-capture-remeasurement/0.1','facts_digest':sha256_json(facts)}}}
    write(out/'evidence.json',e)
    report=create_acceptance_bundle(plan=plan,evidence_paths=[out/'evidence.json'],output=out/'bundle',acceptance_id=out.name,execution_status='COMPLETED')
    print(json.dumps({'identity':out.name,'verdict':report['verdict'],'original_unchanged':unchanged,'boundary':facts['boundary']}));sys.exit(0 if report['verdict']=='PASS' else 1)
