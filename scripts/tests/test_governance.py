"""Contract attacks on guardrails; these are not product security verdicts."""
from copy import deepcopy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import dependency_gate as d
import quality_gate as q


def npm_fixture():
    lock = {'lockfileVersion': 3, 'packages': {'': {}, 'node_modules/demo': {'version': '1.0'}}}
    report = {'auditReportVersion': 2, 'metadata': {'dependencies': {'total': 1},
        'vulnerabilities': {'info': 0, 'low': 0, 'moderate': 0, 'high': 1, 'critical': 0, 'total': 1}},
        'vulnerabilities': {'demo': {'name': 'demo', 'severity': 'high', 'nodes': ['node_modules/demo'],
            'via': [{'source': 1, 'url': 'https://example.invalid/advisory', 'title': 'fixture'}]}}}
    return report, lock


def dependency_fixture():
    current = {'inputs': {'lock': 'bound'}, 'sources': {'maven': {
        'inventory_sha256': 'inventory', 'package_count': 1, 'findings': {'demo/GHSA-fixture': {'id': 'fixture'}}}}}
    policy = {'schema': 'qixu-dependency-review/1', 'source_revision': 'a' * 40,
        'owner': 'NoctilumeDev/Qixu maintainers', 'risk_accepted': False,
        'inputs': deepcopy(current['inputs']), 'sources': deepcopy(current['sources'])}
    policy['sources']['maven']['findings']['demo/GHSA-fixture'] = {
        'sha256': d.digest({'id': 'fixture'}), 'status': 'UNRESOLVED', 'next_action': 'bounded follow-up'}
    return current, policy


def static_fixture():
    return ET.fromstring('''<BugCollection version="4.10.5"><Errors errors="0" missingClasses="0"/>
        <FindBugsSummary total_classes="1" total_bugs="1"><PackageStats>
        <ClassStats class="dev.noctilume.qixu.Demo"/></PackageStats></FindBugsSummary>
        <BugInstance type="FIXTURE" priority="2" category="CORRECTNESS">
        <Class primary="true" classname="dev.noctilume.qixu.Demo"/>
        <SourceLine primary="true" sourcepath="dev/noctilume/qixu/Demo.java" start="3" end="3"/>
        </BugInstance></BugCollection>''')


def coverage_fixture():
    return ET.fromstring('''<report><sessioninfo id="fixture"/><package>
        <class name="dev/noctilume/qixu/Demo" sourcefilename="Demo.java"/>
        <sourcefile name="Demo.java">
        <line nr="1" mi="1" ci="0" mb="2" cb="0"/><line nr="2" mi="1" ci="0" mb="0" cb="0"/>
        <line nr="3" mi="1" ci="0" mb="0" cb="0"/><line nr="4" mi="1" ci="0" mb="0" cb="0"/>
        <counter type="LINE" missed="4" covered="0"/><counter type="BRANCH" missed="2" covered="0"/>
        </sourcefile>
        <counter type="LINE" missed="4" covered="0"/><counter type="BRANCH" missed="2" covered="0"/>
        </package>
        <counter type="LINE" missed="4" covered="0"/>
        <counter type="BRANCH" missed="2" covered="0"/></report>''')


class DependencyGateTests(unittest.TestCase):
    def test_duplicate_or_malformed_json_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'report.json'
            for text in ['{"results":[],"results":[]}', 'not json']:
                path.write_text(text, encoding='utf-8')
                with self.assertRaises(ValueError):
                    d.read(path)

    def test_maven_denominator_scope_and_license(self):
        tree = {'children': [{'groupId': 'g', 'artifactId': 'a', 'version': '1', 'scope': 'runtime'}]}
        bom = {'bomFormat': 'CycloneDX', 'specVersion': '1.6', 'components': [
            {'group': 'g', 'name': 'a', 'version': '1', 'licenses': [{'license': {'id': 'MIT'}}]}]}
        self.assertEqual(d.maven_inventory(tree, bom), {'g:a@1': {'scopes': ['runtime'], 'licenses': ['MIT']}})
        for change in ('empty-tree', 'empty-bom', 'duplicate', 'wrong-version', 'missing-scope', 'bad-license'):
            t, b = deepcopy(tree), deepcopy(bom)
            if change == 'empty-tree': t['children'] = []
            if change == 'empty-bom': b['components'] = []
            if change == 'duplicate': b['components'] *= 2
            if change == 'wrong-version': b['components'][0]['version'] = '2'
            if change == 'missing-scope': del t['children'][0]['scope']
            if change == 'bad-license': b['components'][0]['licenses'] = [{}]
            with self.subTest(change=change), self.assertRaises(ValueError):
                d.maven_inventory(t, b)

    def test_osv_must_observe_every_package_and_exit(self):
        inventory = {'g:a@1': {}}
        report = {'results': [{'source': {'type': 'sbom'}, 'packages': [
            {'package': {'ecosystem': 'Maven', 'name': 'g:a', 'version': '1'}, 'vulnerabilities': [{'id': 'GHSA-fixture'}]}]}]}
        self.assertEqual(len(d.osv_findings(report, inventory, 1)), 1)
        for code in (0, 2):
            with self.assertRaises(ValueError): d.osv_findings(report, inventory, code)
        for change in ('empty', 'missing-package', 'duplicate', 'other-source', 'wrong-version'):
            r = deepcopy(report)
            if change == 'empty': r['results'] = []
            if change == 'missing-package': r['results'][0]['packages'] = []
            if change == 'duplicate': r['results'][0]['packages'] *= 2
            if change == 'other-source': r['results'][0]['source']['type'] = 'lockfile'
            if change == 'wrong-version': r['results'][0]['packages'][0]['package']['version'] = '2'
            with self.subTest(change=change), self.assertRaises(ValueError): d.osv_findings(r, inventory, 1)

    def test_npm_execution_and_denominator_fail_closed(self):
        report, lock = npm_fixture()
        self.assertEqual(len(d.npm_findings(report, lock, 1)), 1)
        for code in (0, 2):
            with self.assertRaises(ValueError): d.npm_findings(report, lock, code)
        for change in ('error', 'no-findings', 'denominator', 'total', 'severity', 'nodes', 'via'):
            r = deepcopy(report)
            if change == 'error': r['error'] = {'code': 'network'}
            if change == 'no-findings': del r['vulnerabilities']
            if change == 'denominator': r['metadata']['dependencies']['total'] = 0
            if change == 'total': r['metadata']['vulnerabilities']['total'] = 0
            if change == 'severity': r['metadata']['vulnerabilities']['high'] = 0
            if change == 'nodes': r['vulnerabilities']['demo']['nodes'] = ['node_modules/unknown']
            if change == 'via': r['vulnerabilities']['demo']['via'] = ['unknown']
            with self.subTest(change=change), self.assertRaises(ValueError): d.npm_findings(r, lock, 1)

    def test_npm_clean_report_is_not_missing_report(self):
        r, lock = npm_fixture()
        r['vulnerabilities'] = {}
        r['metadata']['vulnerabilities']['total'] = r['metadata']['vulnerabilities']['high'] = 0
        self.assertEqual(d.npm_findings(r, lock, 0), {})
        with self.assertRaises(ValueError): d.npm_findings(r, lock, 1)

    def test_dependency_review_rejects_changes_and_risk_acceptance(self):
        current, policy = dependency_fixture()
        self.assertEqual(d.review(current, policy), 1)
        for change in ('input', 'inventory', 'count', 'new', 'removed', 'details', 'source'):
            c = deepcopy(current)
            if change == 'input': c['inputs']['lock'] = 'changed'
            if change == 'inventory': c['sources']['maven']['inventory_sha256'] = 'changed'
            if change == 'count': c['sources']['maven']['package_count'] = 0
            if change == 'new': c['sources']['maven']['findings']['new'] = {}
            if change == 'removed': c['sources']['maven']['findings'] = {}
            if change == 'details': c['sources']['maven']['findings']['demo/GHSA-fixture']['severity'] = 'changed'
            if change == 'source': del c['sources']['maven']
            with self.subTest(change=change), self.assertRaises(ValueError): d.review(c, policy)
        for change in ('accepted', 'not-applicable', 'remediated', 'no-owner', 'no-follow-up', 'invalid-sha'):
            p = deepcopy(policy)
            item = p['sources']['maven']['findings']['demo/GHSA-fixture']
            if change == 'accepted': p['risk_accepted'] = True
            if change == 'not-applicable': item['status'] = 'NOT_APPLICABLE'
            if change == 'remediated': item['status'] = 'REMEDIATED'
            if change == 'no-owner': del p['owner']
            if change == 'no-follow-up': item['next_action'] = ''
            if change == 'invalid-sha': p['source_revision'] = 'z' * 40
            with self.subTest(change=change), self.assertRaises(ValueError): d.review(current, p)

    def test_tool_failure_preserved_and_never_uses_old_success(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            old = root / 'test-results/governance/dependencies/old'
            old.mkdir(parents=True)
            d.save(old / 'observation.json', {'old': 'success'})
            with patch.object(d, 'ROOT', root), patch.object(d, 'collect', side_effect=ValueError('fixture failed')) as collector, \
                    patch.object(d.subprocess, 'check_output', side_effect=['a' * 40, '']), patch.object(sys, 'argv', ['dependency_gate.py']):
                self.assertEqual(d.main(), 1)
                fresh = collector.call_args.args[0]
                self.assertNotEqual(fresh, old)
                self.assertEqual(d.read(fresh / 'run.json')['state'], 'ERROR_OR_REVIEW_REQUIRED')
                self.assertEqual(d.read(old / 'observation.json'), {'old': 'success'})

    def test_command_failure_retains_actual_exit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaises(ValueError):
                d.command([sys.executable, '-c', 'raise SystemExit(42)'], root, root, 'bad')
            self.assertEqual(d.read(root / 'bad.exit.json')['exit_code'], 42)


class QualityGateTests(unittest.TestCase):
    classes = {'dev/noctilume/qixu/Demo'}

    def test_static_incomplete_or_changed_denominator_rejected(self):
        self.assertEqual(len(q.static_observation(static_fixture(), self.classes)['findings']), 1)
        for change in ('engine', 'errors', 'missing', 'error-child', 'denominator', 'bug-count', 'class-count', 'location'):
            r = static_fixture()
            if change == 'engine': r.set('version', 'unknown')
            if change == 'errors': r.find('Errors').set('errors', '1')
            if change == 'missing': r.find('Errors').set('missingClasses', '1')
            if change == 'error-child': ET.SubElement(r.find('Errors'), 'MissingClass')
            if change == 'denominator': r.find('.//ClassStats').set('class', 'dev.noctilume.qixu.Other')
            if change == 'bug-count': r.find('FindBugsSummary').set('total_bugs', '0')
            if change == 'class-count': r.find('FindBugsSummary').set('total_classes', '0')
            if change == 'location': r.find('BugInstance/Class').set('primary', 'false')
            with self.subTest(change=change), self.assertRaises(ValueError): q.static_observation(r, self.classes)

    def test_static_multiset_and_owner_preserved(self):
        current = q.static_observation(static_fixture(), self.classes)
        policy = {'schema': 'qixu-static-review/1', 'owner': 'NoctilumeDev/Qixu maintainers',
            'risk_accepted': False, 'status': 'UNRESOLVED', 'next_action': 'bounded review', **deepcopy(current)}
        self.assertEqual(q.static_review(current, policy), 1)
        for change in ('add', 'remove', 'move', 'classes', 'accept', 'close'):
            p = deepcopy(policy)
            if change == 'add': p['findings'] *= 2
            if change == 'remove': p['findings'] = []
            if change == 'move': p['findings'][0]['SourceLine']['start'] = '4'
            if change == 'classes': p['classes'] = []
            if change == 'accept': p['risk_accepted'] = True
            if change == 'close': p['status'] = 'REMEDIATED'
            with self.subTest(change=change), self.assertRaises(ValueError): q.static_review(current, p)

    def test_coverage_complete_measurement_not_percentage_gate(self):
        result = q.coverage_observation(coverage_fixture(), self.classes)
        self.assertEqual(result['LINE'], {'missed': 4, 'covered': 0})
        for change in ('session', 'class', 'duplicate', 'missing', 'negative', 'zero'):
            r = coverage_fixture()
            if change == 'session': r.remove(r.find('sessioninfo'))
            if change == 'class': r.find('package').remove(r.find('package/class'))
            if change == 'duplicate': r.append(deepcopy(r.find('counter')))
            if change == 'missing': r.remove(r.find("counter[@type='BRANCH']"))
            if change == 'negative': r.find('counter').set('covered', '-1')
            if change == 'zero': r.find('counter').set('missed', '0')
            with self.subTest(change=change), self.assertRaises(ValueError): q.coverage_observation(r, self.classes)

    def test_empty_build_is_not_zero_findings(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError): q.compiled_names(Path(directory))

    def test_coverage_root_package_source_and_lines_agree(self):
        for xpath in ('counter', 'package/counter', 'package/sourcefile/counter'):
            r = coverage_fixture()
            r.find(xpath).set('missed', '0')
            r.find(xpath).set('covered', '1')
            with self.subTest(xpath=xpath), self.assertRaises(ValueError):
                q.coverage_observation(r, self.classes)
        for change in ('duplicate-line', 'negative-line', 'missing-source', 'wrong-source'):
            r = coverage_fixture()
            source = r.find('package/sourcefile')
            if change == 'duplicate-line': source.append(deepcopy(source.find('line')))
            if change == 'negative-line': source.find('line').set('cb', '-1')
            if change == 'missing-source': r.find('package').remove(source)
            if change == 'wrong-source': r.find('package/class').set('sourcefilename', 'Other.java')
            with self.subTest(change=change), self.assertRaises(ValueError): q.coverage_observation(r, self.classes)


if __name__ == '__main__':
    unittest.main()
