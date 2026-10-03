"""Reject stale, incomplete or altered M6 producer bundles before any process starts."""
import hashlib,json
from pathlib import Path,PurePosixPath
from veritrail.acceptance_plan import verify_sealed_acceptance_plan
from veritrail.canonical import sha256_json

def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def bind(producer,source,jar):
    producer=Path(producer).resolve();jar=Path(jar).resolve();manifest=producer/'acceptance-bundle-manifest.json'
    data=json.loads(manifest.read_text(encoding='utf-8'));seen=set();total=0
    for entry in data['files']:
        relative=PurePosixPath(entry['path']);path=(producer/relative).resolve();total+=entry['size']
        if relative.is_absolute() or '..' in relative.parts or '\\' in entry['path'] or entry['path'] in seen or type(entry['size']) is not int or not 0<=entry['size']<=2097152 or total>4194304 or not path.is_relative_to(producer) or path.is_symlink() or path.stat().st_size!=entry['size'] or digest(path)!=entry['sha256']:raise RuntimeError('M6 producer manifest/bytes invalid')
        seen.add(entry['path'])
    if {p.relative_to(producer).as_posix() for p in producer.rglob('*') if p.is_file() and p!=manifest}!=seen:raise RuntimeError('M6 producer has missing or unmanifested files')
    report=json.loads((producer/'acceptance-report.json').read_text(encoding='utf-8'));plan=json.loads((producer/'sealed-acceptance-plan.json').read_text(encoding='utf-8'));verify_sealed_acceptance_plan(plan)
    if report['verdict']!='PASS' or report['execution_status']!='COMPLETED' or report['subject']!=plan['subject'] or report['subject']['id']!='qixu-m6' or report['subject']['version']!=source or report['plan']['version']!=1 or report['plan']['sha256']!=plan['seal']['digest']:raise RuntimeError('M6 producer is not an exact-source qualified native run')
    entries=[e for e in report['evidence'] if e['evidence_type']=='qixu.native.observation']
    if len(entries)!=1 or entries[0]['path'] not in seen:raise RuntimeError('M6 producer evidence ambiguous')
    entry=entries[0];ev=json.loads((producer/entry['path']).read_text(encoding='utf-8'));facts=ev['facts'];package=facts.get('package',{})
    if ev['source']!='qixu-native/0.12' or entry['sha256']!=digest(producer/entry['path']) or entry['facts_digest']!=sha256_json(facts) or ev['metadata']['veritrail_observation']['plan_digest']!=plan['seal']['digest'] or facts.get('source_sha')!=source or facts.get('source_clean') is not True or facts.get('command_exit')!=0 or package.get('current_build') is not True or package.get('source_sha')!=source or package.get('sha256')!=digest(jar) or package.get('size')!=jar.stat().st_size:raise RuntimeError('M6 current jar lacks immutable fresh producer binding')
    return {'acceptance_id':report['acceptance_id'],'manifest_sha256':digest(manifest),'bytes_checked':True,'jar_sha256':digest(jar)}
