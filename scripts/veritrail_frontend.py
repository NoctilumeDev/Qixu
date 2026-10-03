"""Seal before a clean checkout build; Core consumes facts, never UI self-ratings."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
from importlib.metadata import version
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET
import zipfile
from veritrail.acceptance_plan import observation_spec_digest, seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json
from frontend_m5_contract import CLIENT_CASES
from frontend_m7_contract import CLIENT_M7_CASES

ROOT = Path(__file__).resolve().parents[1]
COLLECTOR = "qixu-frontend/0.1"


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def write_new(path: Path, value: dict) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as f:
        json.dump(value, f, ensure_ascii=False, sort_keys=True, indent=2)
        f.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=["m5","m7"], default="m5")
    parser.add_argument("--node", default="node")
    parser.add_argument("--npm", default="npm")
    args = parser.parse_args()
    if version("veritrail") != "0.13.0" or git("status", "--porcelain"):
        raise RuntimeError("Core0.13.0 and exact clean committed source required")
    sha = git("rev-parse", "HEAD")
    collector="qixu-frontend/0.3" if args.stage=="m7" else COLLECTOR
    cases=CLIENT_CASES+CLIENT_M7_CASES if args.stage=="m7" else CLIENT_CASES
    identity = args.stage+"-frontend-" + uuid.uuid4().hex
    output = ROOT / "artifacts/local" / identity
    output.mkdir(parents=True, exist_ok=False)
    request = {"source_sha": sha, "collector": collector}
    if args.stage=="m7": request["contract_sha256"]=hashlib.sha256((ROOT/"docs/contracts/m7-execution-entry.md").read_bytes()).hexdigest()
    spec = {"id": "frontend-build", "contract": {"id": "qixu-frontend", "version": collector.split("/")[1]},
            "evidence_type": "qixu.frontend.build", "coordinates": request,
            "projections": ["source_sha", "source_clean", "node", "npm", "commands", "tests", "artifacts"],
            "canonicalization_profile": "veritrail-json-c14n/1"}

    def assertion(name: str, path: str, value: object) -> dict:
        return {"id": name, "severity": "HARD", "left": {"requirement_id": "frontend", "path": path}, "operator": "eq", "right": value}

    assertions = [assertion("source", "/facts/source_sha", sha), assertion("clean", "/facts/source_clean", True),
                  assertion("node", "/facts/node", "v24.14.0"), assertion("npm", "/facts/npm", "11.9.0")]
    assertions += [assertion("command-" + str(i), "/facts/commands/" + str(i) + "/exit_code", 0) for i in range(5)]
    assertions += [assertion("case-" + str(i), "/facts/tests/case-" + str(i), True) for i in range(len(cases))]
    assertions += [assertion("artifact-" + name, "/facts/artifacts/" + name + "/current_build", True) for name in ("h5", "wechat", "admin")]
    plan = seal_acceptance_plan({
        "plan_kind": "ACCEPTANCE", "schema_version": "0.1", "plan_id": "qixu-"+args.stage+"-frontend", "version": 2 if args.stage=="m7" else 1,
        "subject": {"id": "qixu-"+args.stage+"-frontend", "version": sha, "source_ref": "github:NoctilumeDev/Qixu"},
        "question": "Do exact locked clean-source frontend builds and the declared request mechanism witnesses hold?",
        "governance": {"claim_owner_ref": "human:repository-owner", "drafter_ref": "qixu:frontend-adapter", "seal_authority_ref": "human:repository-owner:authorized-engineering-goal", "seal_decision": "CONFIRMED"},
        "observation_specs": [spec], "evidence_requirements": [{"id": "frontend", "observation_spec_id": spec["id"], "cardinality": "EXACTLY_ONE"}],
        "sufficiency_rules": [{"id": "collected", "left": {"requirement_id": "frontend", "path": "/metadata/veritrail_observation/coverage"}, "operator": "eq", "right": "COMPLETE"}],
        "integrity_rules": [], "assertions": assertions,
        "resource_budget": {"max_artifact_bytes": 2097152, "command_timeout_seconds": 600},
        "change_scope": {"level": "L2_CONTRACT", "owner": "Qixu M5 frontend build", "consumers": ["m5-build-gates"]},
        "reproduction_steps": ["Use Core0.13.0 and exact clean committed source.", "Run this collector with Node24.14.0 and npm11.9.0; it seals before fresh git archive and npm ci."],
        "cleanup_steps": ["No service or shared database is stopped.", "Retain private raw logs, three build archives and the immutable bundle."],
    })
    write_new(output / "sealed-plan.json", plan)
    write_new(output / "request.json", request)
    started = time.time()
    work = output / "work"
    work.mkdir()
    archive = output / "source.zip"
    subprocess.run(["git", "archive", "--format=zip", "--output=" + str(archive), sha], cwd=ROOT, check=True)
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            target = (work / item.filename).resolve()
            if not target.is_relative_to(work.resolve()):
                raise ValueError("Source archive escaped workspace")
        z.extractall(work)
    build_roots = ("student/dist/build/h5", "student/dist/build/mp-weixin", "admin/dist")
    if any((work / relative).exists() for relative in build_roots):
        raise RuntimeError("Source archive contains build output; fresh production is not established")
    env = {k: v for k, v in os.environ.items() if not k.startswith(("QIXU_", "DEEPSEEK_"))}
    node_parent = str(Path(args.node).resolve().parent) if Path(args.node).is_file() else ""
    if node_parent:
        env["PATH"] = node_parent + os.pathsep + env.get("PATH", "")
    commands = [[args.npm, "ci", "--no-audit", "--no-fund"], [args.npm, "run", "build", "-w", "@qixu/client"],
                [args.node, "--test", "--test-reporter=junit", "--test-reporter-destination=" + str(output / "client-tests.xml"), *(["packages/client/tests/ownership.test.mjs","packages/client/tests/m7-boundaries.test.mjs"] if args.stage=="m7" else ["packages/client/tests/ownership.test.mjs"])],
                [args.npm, "run", "check"], [args.npm, "run", "build"]]
    steps = []
    execution = "COMPLETED"
    print(json.dumps({"identity": identity, "source_sha": sha, "state": "SEALED_BEFORE_BUILD"}), flush=True)
    for i, command in enumerate(commands):
        try:
            with (output / f"command-{i}-stdout.bin").open("xb") as a, (output / f"command-{i}-stderr.bin").open("xb") as e:
                r = subprocess.run(command, cwd=work, env=env, stdout=a, stderr=e, timeout=600)
            steps.append({"id": i, "exit_code": r.returncode})
            print(json.dumps(steps[-1]), flush=True)
            if r.returncode:
                break
        except subprocess.TimeoutExpired:
            steps.append({"id": i, "exit_code": -1})
            execution = "ERROR"
            break
    tests = {}
    report = output / "client-tests.xml"
    if report.is_file() and report.stat().st_mtime >= started:
        observed = {}
        for case in ET.parse(report).getroot().iter("testcase"):
            name = case.attrib["name"]
            passed = not any(case.find(kind) is not None for kind in ("failure", "error", "skipped"))
            observed[name] = passed if name not in observed else False
        tests = {"case-" + str(i): observed[name] for i, name in enumerate(cases) if name in observed}
    artifacts = {}
    for name, relative, entry in (("h5", "student/dist/build/h5", "index.html"), ("wechat", "student/dist/build/mp-weixin", "app.json"), ("admin", "admin/dist", "index.html")):
        folder = work / relative
        files = {p.relative_to(folder).as_posix(): {"sha256": hashlib.sha256(p.read_bytes()).hexdigest(), "bytes": p.stat().st_size} for p in sorted(folder.rglob("*")) if p.is_file()}
        # Static copy tools may preserve source mtimes; fresh empty build roots and
        # a newly produced entry point establish this build, not every icon's age.
        current = entry in files and bool(files) and (folder / entry).stat().st_mtime >= started
        if files:
            with zipfile.ZipFile(output / (name + ".zip"), "x", zipfile.ZIP_DEFLATED) as z:
                for path in files:
                    z.write(folder / path, path)
        artifacts[name] = {"current_build": current, "source_sha": sha, "file_count": len(files), "manifest_sha256": sha256_json(files), "files": files}
    facts = {"source_sha": git("rev-parse", "HEAD"), "source_clean": not bool(git("status", "--porcelain")),
             "source_zip_sha256": hashlib.sha256(archive.read_bytes()).hexdigest(),
             "node": subprocess.check_output([args.node, "--version"], text=True).strip(), "npm": subprocess.check_output([args.npm, "--version"], text=True).strip(),
             "commands": steps, "tests": tests, "required_cases": cases, "artifacts": artifacts,
             "boundary": "M7_ENTRY_CLIENT_STORAGE_ACTOR_BUILD_NOT_WHOLE_M7_BROWSER_BACKUP_DEVICE_OR_FINAL_VISUAL" if args.stage=="m7" else "M5_FRONTEND_BUILD_AND_CONTROLLED_TRANSPORT_NOT_INSTALLED_BROWSER_NATIVE_DEVICE_OR_FINAL_VISUAL"}
    evidence = {"schema_version": "0.1", "evidence_type": spec["evidence_type"], "source": collector, "captured_at": datetime.now(timezone.utc).isoformat(), "facts": facts,
                "metadata": {"veritrail_observation": {"schema_version": "0.1", "canonicalization_profile": "veritrail-json-c14n/1", "plan_digest": plan["seal"]["digest"], "observation_spec_digest": observation_spec_digest(spec), "request_seal_digest": sha256_json(request), "collection_session_id": identity, "collector_role": "qixu-frontend-collector", "coverage": "COMPLETE" if len(steps) == 5 and tests and artifacts else "ERROR", "normalization_semantics_version": collector, "facts_digest": sha256_json(facts)}}}
    write_new(output / "evidence.json", evidence)
    result = create_acceptance_bundle(plan=plan, evidence_paths=[output / "evidence.json"], output=output / "bundle", acceptance_id=identity, execution_status=execution)
    print(json.dumps({"identity": identity, "source_sha": sha, "verdict": result["verdict"], "cases_observed": len(tests), "bundle": str(output / "bundle"), "boundary": facts["boundary"]}))
    return 0 if result["verdict"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
