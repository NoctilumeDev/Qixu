"""Validate current production static/coverage reports without changing business rules."""
import argparse
from collections import Counter
from pathlib import Path
import sys
import tempfile
import xml.etree.ElementTree as ET

from dependency_gate import ROOT, digest, read, require, save


def compiled_names(directory):
    names = {p.relative_to(directory).with_suffix('').as_posix() for p in directory.rglob('*.class')}
    require(names and all(x.startswith('dev/noctilume/qixu/') for x in names), 'empty or unexpected compiled classes')
    return names


def static_observation(report, classes):
    require(report.tag == 'BugCollection' and report.get('version') == '4.10.5', 'wrong static engine/report')
    errors = report.find('Errors')
    require(errors is not None and errors.get('errors') == '0' and errors.get('missingClasses') == '0', 'static analysis incomplete')
    require(not list(errors), 'static errors or missing classes')
    summary = report.find('FindBugsSummary')
    require(summary is not None, 'missing static summary')
    names = [c.get('class', '').replace('.', '/') for c in summary.findall('.//ClassStats')]
    require(len(names) == len(set(names)) and set(names) == classes, 'static class denominator differs from production build')
    require(int(summary.get('total_classes', '-1')) == len(classes), 'static class count mismatch')
    findings = []
    for bug in report.findall('BugInstance'):
        primary = bug.find("Class[@primary='true']")
        require(primary is not None and primary.get('classname', '').replace('.', '/') in classes, 'missing static finding location')
        finding = {key: bug.get(key) for key in ('type', 'priority', 'category')}
        require(finding['type'] and finding['category'] and finding['priority'] in ('1', '2', '3'), 'invalid static finding identity')
        finding['class'] = primary.get('classname')
        for tag in ('Method', 'Field', 'SourceLine'):
            element = bug.find(f"{tag}[@primary='true']")
            if element is not None:
                finding[tag] = {key: element.get(key) for key in ('name', 'signature', 'sourcepath', 'start', 'end') if element.get(key) is not None}
        findings.append(finding)
    require(int(summary.get('total_bugs', '-1')) == len(findings), 'static finding count mismatch')
    return {'classes': sorted(classes), 'findings': sorted(findings, key=digest)}


def static_review(current, policy):
    require(policy.get('schema') == 'qixu-static-review/1', 'missing static review')
    require(policy.get('owner') == 'NoctilumeDev/Qixu maintainers' and policy.get('risk_accepted') is False, 'missing static owner or risk acceptance')
    require(policy.get('status') == 'UNRESOLVED' and policy.get('next_action'), 'missing static follow-up')
    require(policy.get('classes') == current['classes'], 'static class denominator changed; review required')
    # Multiset identity keeps duplicate findings and rejects unreviewed disappearance as well as addition.
    require(Counter(digest(x) for x in policy.get('findings', [])) == Counter(digest(x) for x in current['findings']), 'new/changed/disappeared static finding; review required')
    return len(current['findings'])


def coverage_counters(element):
    counters = {}
    for counter in element.findall('counter'):
        kind = counter.get('type')
        require(kind not in counters, 'duplicate coverage counter')
        missed, covered = int(counter.get('missed', '-1')), int(counter.get('covered', '-1'))
        require(missed >= 0 and covered >= 0, 'invalid coverage count')
        counters[kind] = {'missed': missed, 'covered': covered}
    return counters


def coverage_observation(report, classes):
    require(report.tag == 'report' and report.findall('sessioninfo'), 'missing coverage session')
    observed = [c.get('name') for c in report.findall('./package/class')]
    require(len(observed) == len(set(observed)) and set(observed) == classes, 'coverage class denominator differs from production build')
    counters = coverage_counters(report)
    require(all(k in counters and sum(counters[k].values()) > 0 for k in ('LINE', 'BRANCH')), 'missing coverage denominator')
    total = {k: {'missed': 0, 'covered': 0} for k in ('LINE', 'BRANCH')}
    for package in report.findall('package'):
        package_counts = coverage_counters(package)
        source_files = package.findall('sourcefile')
        names = [source.get('name') for source in source_files]
        require(names and all(names) and len(names) == len(set(names)), 'missing or duplicate coverage source')
        require(all(c.get('sourcefilename') in names for c in package.findall('class')), 'coverage class without source details')
        source_counts = {k: {'missed': 0, 'covered': 0} for k in total}
        for source in source_files:
            declared = coverage_counters(source)
            measured = {k: {'missed': 0, 'covered': 0} for k in total}
            lines = source.findall('line')
            numbers = [int(line.get('nr', '-1')) for line in lines]
            require(lines and min(numbers) > 0 and len(numbers) == len(set(numbers)), 'missing or duplicate source lines')
            for line in lines:
                mi, ci, mb, cb = (int(line.get(k, '-1')) for k in ('mi', 'ci', 'mb', 'cb'))
                require(min(mi, ci, mb, cb) >= 0 and mi + ci > 0, 'invalid source line counts')
                measured['LINE']['covered' if ci else 'missed'] += 1
                measured['BRANCH']['missed'] += mb
                measured['BRANCH']['covered'] += cb
            for kind in total:
                zero = {'missed': 0, 'covered': 0}
                require(declared.get(kind, zero) == measured[kind], 'source coverage counter differs from lines')
                for outcome in zero:
                    source_counts[kind][outcome] += measured[kind][outcome]
        # LINE must sum source files, not classes: inner classes can share a line.
        for kind in total:
            require(package_counts.get(kind, {'missed': 0, 'covered': 0}) == source_counts[kind], 'package coverage counter differs from sources')
            for outcome in total[kind]:
                total[kind][outcome] += source_counts[kind][outcome]
    require(all(counters[k] == total[k] for k in total), 'root coverage counter differs from packages')
    return counters


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('kind', choices=['static', 'coverage'])
    parser.add_argument('--collect-only', action='store_true')
    args = parser.parse_args()
    try:
        target = ROOT / 'backend/target'
        if args.kind == 'static':
            target /= 'static-analysis'
        classes = compiled_names(target / 'classes')
        parent = ROOT / 'test-results/governance/quality'
        parent.mkdir(parents=True, exist_ok=True)
        output = Path(tempfile.mkdtemp(prefix=args.kind + '-', dir=parent))
        print('Quality evidence: ' + str(output), flush=True)
        if args.kind == 'static':
            result = static_observation(ET.parse(target / 'spotbugsXml.xml').getroot(), classes)
            save(output / 'static-observation.json', result)
            if args.collect_only:
                print(f"Static observation: {len(result['findings'])} unreviewed candidates, NOT GATE PASS")
            else:
                count = static_review(result, read(ROOT / 'docs/governance/static-review.json'))
                print(f'Static change gate: PASS; {count} retained UNRESOLVED candidates, not defect clearance.')
        else:
            result = coverage_observation(ET.parse(ROOT / 'backend/target/site/jacoco/jacoco.xml').getroot(), classes)
            save(output / 'coverage-observation.json', result)
            print('Coverage report verified (measurement only, no percentage target): ' + str(result))
        return 0
    except (OSError, ValueError, TypeError, KeyError, ET.ParseError) as error:
        print('Quality check rejected: ' + str(error), file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
