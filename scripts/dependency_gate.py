"""Bounded Maven/npm inventory and advisory change gate, not risk acceptance."""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / 'docs/governance/dependency-review.json'
OSV = '2.7.0'
BINARIES = {
    ('Windows', 'AMD64'): ('osv-scanner_windows_amd64.exe', '9964ed64ec9a387391eb4cc006ad5c531154821aef29b793e02c448de4686438'),
    ('Linux', 'x86_64'): ('osv-scanner_linux_amd64', '160c6253e2922e9e0a6a9771c5c4c40a4b3c6090b34a3cdd5e80c96cc2e4218b'),
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, 'duplicate JSON field')
        result[key] = value
    return result


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'), object_pairs_hook=unique)


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()).hexdigest()


def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def tool(name):
    executable = shutil.which(name + '.cmd' if os.name == 'nt' else name)
    require(executable, f'missing {name}')
    return executable


def maven_inventory(tree, bom):
    require(bom.get('bomFormat') == 'CycloneDX' and bom.get('specVersion') == '1.6', 'invalid SBOM')
    scopes = {}

    def visit(node):
        parts = [node.get(x) for x in ('groupId', 'artifactId', 'version')]
        require(all(isinstance(x, str) and x for x in parts), 'unresolved Maven coordinate')
        require(node.get('scope') in {'compile', 'runtime', 'test', 'provided', 'system'}, 'missing scope')
        key = f'{parts[0]}:{parts[1]}@{parts[2]}'
        scopes.setdefault(key, set()).add(node['scope'])
        for child in node.get('children', []):
            visit(child)

    for node in tree.get('children', []):
        visit(node)
    require(scopes, 'empty Maven tree')
    inventory = {}
    for item in bom.get('components', []):
        key = f"{item.get('group')}:{item.get('name')}@{item.get('version')}"
        require(key in scopes and key not in inventory, 'unexpected or duplicate SBOM component')
        licenses = []
        for entry in item.get('licenses', []):
            data = entry.get('license', {})
            label = entry.get('expression') or data.get('id') or data.get('name')
            require(isinstance(label, str) and label, 'invalid license declaration')
            licenses.append(label)
        inventory[key] = {'scopes': sorted(scopes[key]), 'licenses': sorted(licenses)}
    require(set(inventory) == set(scopes), 'SBOM coverage differs from resolved tree')
    return inventory


def osv_findings(report, inventory, exit_code):
    require(exit_code in (0, 1), 'OSV execution failed')
    results = report.get('results')
    require(isinstance(results, list) and len(results) == 1, 'missing OSV source')
    require(results[0].get('source', {}).get('type') == 'sbom', 'unexpected OSV input')
    seen, findings = set(), {}
    for item in results[0].get('packages', []):
        p = item.get('package', {})
        key = f"{p.get('name')}@{p.get('version')}"
        require(p.get('ecosystem') == 'Maven' and key in inventory and key not in seen, 'invalid OSV package coverage')
        seen.add(key)
        for advisory in item.get('vulnerabilities', []):
            identifier = advisory.get('id')
            require(isinstance(identifier, str) and identifier.startswith(('GHSA-', 'CVE-')), 'missing advisory identity')
            finding = key + '/' + identifier
            require(finding not in findings, 'duplicate advisory')
            fields = ('id', 'aliases', 'summary', 'details', 'affected', 'severity', 'database_specific', 'withdrawn')
            findings[finding] = {k: advisory.get(k) for k in fields}
    require(seen == set(inventory), 'OSV did not report all resolved packages')
    require(exit_code == (1 if findings else 0), 'OSV exit/report disagreement')
    return findings


def npm_findings(report, lock, exit_code):
    require(exit_code in (0, 1) and not report.get('error'), 'npm audit execution failed')
    require(lock.get('lockfileVersion') == 3, 'unsupported npm lockfile')
    packages = lock.get('packages', {})
    require(isinstance(packages, dict) and '' in packages and len(packages) > 1, 'empty npm lockfile')
    require(report.get('auditReportVersion') == 2, 'unsupported npm audit report')
    metadata = report.get('metadata', {})
    require(metadata.get('dependencies', {}).get('total') == len(packages) - 1, 'npm audit dependency denominator differs from lockfile')
    findings = report.get('vulnerabilities')
    require(isinstance(findings, dict), 'missing npm findings')
    levels = Counter()
    for name, finding in findings.items():
        require(finding.get('name') == name, 'npm finding identity mismatch')
        require(finding.get('severity') in {'info', 'low', 'moderate', 'high', 'critical'}, 'invalid npm severity')
        levels[finding['severity']] += 1
        nodes = finding.get('nodes')
        require(isinstance(nodes, list) and nodes and len(set(nodes)) == len(nodes), 'missing or duplicate npm nodes')
        require(all(node in packages for node in nodes), 'npm finding outside lockfile')
        via = finding.get('via')
        require(isinstance(via, list) and via, 'missing npm advisory origin')
        for source in via:
            if isinstance(source, str):
                require(source in findings, 'dangling npm advisory propagation')
            else:
                require(isinstance(source, dict) and source.get('source') and source.get('url') and source.get('title'), 'incomplete npm advisory')
    totals = metadata.get('vulnerabilities', {})
    require(totals.get('total') == len(findings), 'npm finding total mismatch')
    require(all(totals.get(k) == levels[k] for k in ('info', 'low', 'moderate', 'high', 'critical')), 'npm severity totals mismatch')
    require(exit_code == (1 if findings else 0), 'npm exit/report disagreement')
    # Keep raw reports. Remediation recommendations are not current risk facts.
    # npm's mutable reverse-effects set can differ while its complete advisory
    # 'via' relation is unchanged; reconstruct the inverse without losing edges.
    normalized = {}
    for name, finding in findings.items():
        value = dict(finding)
        value.pop('fixAvailable', None)
        value['effects'] = sorted(parent for parent, detail in findings.items() if name in detail['via'])
        for field in ('nodes', 'effects', 'via'):
            if isinstance(value.get(field), list):
                value[field] = sorted(value[field], key=lambda x: json.dumps(x, sort_keys=True))
        normalized[name] = value
    return normalized


def review(observation, policy):
    require(policy.get('schema') == 'qixu-dependency-review/2', 'missing dependency review')
    require(isinstance(policy.get('source_revision'), str) and re.fullmatch('[0-9a-f]{40}', policy['source_revision']), 'missing review source')
    require(policy.get('owner') == 'NoctilumeDev/Qixu maintainers' and policy.get('risk_accepted') is False, 'missing owner or unauthorized risk acceptance')
    require(policy.get('inputs') == observation['inputs'], 'dependency manifest or lockfile changed; review required')
    expected = policy.get('sources', {})
    require(set(expected) == set(observation['sources']), 'missing dependency source')
    for name, current in observation['sources'].items():
        previous = expected[name]
        require(previous.get('inventory_sha256') == current['inventory_sha256'], name + ': inventory or license declarations changed')
        require(previous.get('package_count') == current['package_count'], name + ': package denominator changed')
        retained = previous.get('findings', {})
        require(set(retained) == set(current['findings']), name + ': new or disappeared finding; reconcile explicitly')
        for key, value in current['findings'].items():
            record = retained[key]
            require(record.get('sha256') == digest(value), name + ': advisory details changed: ' + key)
            require(record.get('status') == 'UNRESOLVED', 'this initial gate cannot grant inapplicability or accept/close risk')
            require(isinstance(record.get('next_action'), str) and record['next_action'].strip(), 'missing follow-up')
    return sum(len(x['findings']) for x in observation['sources'].values())


def scanner():
    name, sha = BINARIES.get((platform.system(), platform.machine()), (None, None))
    require(name, 'unsupported OSV platform')
    directory = ROOT / '.tools' / ('osv-scanner-' + OSV)
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / name
    if not path.exists():
        with urllib.request.urlopen(f'https://github.com/google/osv-scanner/releases/download/v{OSV}/{name}', timeout=60) as response:
            data = response.read()
        require(hashlib.sha256(data).hexdigest() == sha, 'downloaded OSV checksum mismatch')
        path.write_bytes(data)
        if os.name != 'nt':
            path.chmod(0o755)
    require(hashlib.sha256(path.read_bytes()).hexdigest() == sha, 'cached OSV checksum mismatch')
    return path


def command(args, cwd, output, stem, allowed=(0,)):
    with (output / (stem + '.stdout')).open('wb') as out, (output / (stem + '.stderr')).open('wb') as err:
        result = subprocess.run(list(map(str, args)), cwd=cwd, stdout=out, stderr=err, timeout=600)
    save(output / (stem + '.exit.json'), {'exit_code': result.returncode})
    require(result.returncode in allowed, stem + ': command failed; see retained output')
    return result.returncode


def input_hashes():
    # The complete source manifests are bound, not only the vulnerable package list.
    names = ['backend/pom.xml', 'package.json', 'package-lock.json', 'randomness/package.json', 'randomness/package-lock.json',
             'admin/package.json', 'student/package.json', 'packages/client/package.json']
    return {name: digest(read(ROOT / name)) if name.endswith('.json') else hashlib.sha256((ROOT / name).read_text(encoding='utf-8').encode()).hexdigest() for name in names}


def collect(output):
    inputs = input_hashes()
    observation = {'inputs': inputs, 'sources': {}}
    maven = tool('mvn')
    command([maven, '-B', '-ntp', 'org.apache.maven.plugins:maven-dependency-plugin:3.11.0:tree', '-DoutputType=json', f'-DoutputFile={output / "tree.json"}',
             'org.cyclonedx:cyclonedx-maven-plugin:2.9.3:makeAggregateBom', '-DincludeTestScope=true', '-DincludeLicenseText=false', '-DschemaVersion=1.6',
             '-DoutputFormat=json', '-DoutputName=bom.cdx', f'-DoutputDirectory={output}'], ROOT / 'backend', output, 'maven')
    inventory = maven_inventory(read(output / 'tree.json'), read(output / 'bom.cdx.json'))
    (output / 'osv.toml').write_text('# No ignored packages or advisories.\n', encoding='utf-8')
    code = command([scanner(), 'scan', 'source', '--sbom', output / 'bom.cdx.json', '--config', output / 'osv.toml', '--all-packages', '--format', 'json',
                    '--output-file', output / 'osv.json'], output, output, 'osv', (0, 1))
    findings = osv_findings(read(output / 'osv.json'), inventory, code)
    save(output / 'maven-inventory.json', inventory)
    observation['sources']['maven'] = {'inventory_sha256': digest(inventory), 'package_count': len(inventory), 'findings': findings}
    for name, directory in [('npm-workspaces', ROOT), ('npm-randomness', ROOT / 'randomness')]:
        lock = read(directory / 'package-lock.json')
        code = command([tool('npm'), 'audit', '--package-lock-only', '--ignore-scripts', '--include=dev', '--include=optional', '--include=peer',
                        '--audit-level=info', '--json', '--registry=https://registry.npmjs.org'], directory, output, name, (0, 1))
        findings = npm_findings(read(output / (name + '.stdout')), lock, code)
        observation['sources'][name] = {'inventory_sha256': digest(lock), 'package_count': len(lock['packages']) - 1, 'findings': findings}
    require(inputs == input_hashes(), 'dependency inputs changed while scanning')
    save(output / 'observation.json', observation)
    return observation


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--collect-only', action='store_true', help='retain a fresh observation; never passes the change gate')
    args = parser.parse_args()
    parent = ROOT / 'test-results/governance/dependencies'
    parent.mkdir(parents=True, exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix='run-', dir=parent))
    print('Dependency evidence: ' + str(output), flush=True)
    record = {'source_revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
              'working_tree_changed': bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip()), 'state': 'STARTED'}
    save(output / 'run.json', record)
    try:
        observation = collect(output)
        if args.collect_only:
            record['state'] = 'OBSERVATION_ONLY_NOT_GATE_PASS'
            print('Collected fresh inventory and reports. Not reviewed, not a gate PASS.')
        else:
            count = review(observation, read(POLICY))
            record.update(state='CHANGE_GATE_PASS', unresolved=count)
            print(f'Dependency change gate: PASS; {count} retained UNRESOLVED records. Not zero vulnerabilities or risk acceptance.')
        return 0
    except (OSError, ValueError, KeyError, TypeError, AttributeError, subprocess.SubprocessError) as error:
        record['state'] = 'ERROR_OR_REVIEW_REQUIRED'
        print('Dependency gate rejected: ' + str(error), file=sys.stderr)
        return 1
    finally:
        save(output / 'run.json', record)


if __name__ == '__main__':
    sys.exit(main())
