"""Real M0 document observation through the public VeriTrail Acceptance Core.

No product runtime or causal Comparison is claimed by this narrow adapter.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
from importlib.metadata import version
import json
from pathlib import Path
import subprocess
import sys
import uuid

from veritrail.acceptance_plan import observation_spec_digest, seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT = Path(__file__).resolve().parents[1]


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def write_new(path: Path, value: dict) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, ensure_ascii=False, sort_keys=True, indent=2)
        stream.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--control", choices=["none", "missing-contract"], default="none")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if version("veritrail") != "0.13.0":
        raise RuntimeError("This adapter is qualified only for public Core 0.13.0")
    if git("status", "--porcelain"):
        raise RuntimeError("Commit the final bytes before collecting evidence")
    subject_sha = git("rev-parse", "HEAD")
    run_id = "m0-" + uuid.uuid4().hex
    output = (args.output or ROOT / "artifacts/local" / run_id).resolve()
    if not output.is_relative_to(ROOT / "artifacts/local"):
        raise ValueError("Use a fresh ignored artifacts/local run directory")
    output.mkdir(parents=True, exist_ok=False)
    request = {
        "source_sha": subject_sha,
        "stage": "m0",
        "control": args.control,
        "collector": "qixu-doc-observation/0.1",
    }
    spec = {
        "id": "m0-documents",
        "contract": {"id": "qixu-doc-observation", "version": "0.1"},
        "evidence_type": "qixu.documents.observation",
        "coordinates": request,
        "projections": ["source_sha", "required_file_count", "observed_file_count", "error_count", "command_exit", "source_clean"],
        "canonicalization_profile": "veritrail-json-c14n/1",
    }

    def rule(name: str, path: str, expected: object) -> dict:
        return {"id": name, "severity": "HARD", "left": {"requirement_id": "docs-evidence", "path": path}, "operator": "eq", "right": expected}

    plan = seal_acceptance_plan({
        "plan_kind": "ACCEPTANCE", "schema_version": "0.1", "plan_id": "qixu-m0-documents", "version": 1,
        "subject": {"id": "qixu-m0", "version": subject_sha, "source_ref": "github:NoctilumeDev/Qixu"},
        "question": "Are the declared M0 documents present, linked, and observed at the exact clean source coordinate?",
        "governance": {"claim_owner_ref": "human:repository-owner", "drafter_ref": "qixu:m0-adapter", "seal_authority_ref": "human:repository-owner:authorized-engineering-goal", "seal_decision": "CONFIRMED"},
        "observation_specs": [spec],
        "evidence_requirements": [{"id": "docs-evidence", "observation_spec_id": spec["id"], "cardinality": "EXACTLY_ONE"}],
        "sufficiency_rules": [{"id": "complete-capture", "left": {"requirement_id": "docs-evidence", "path": "/metadata/veritrail_observation/coverage"}, "operator": "eq", "right": "COMPLETE"}],
        "integrity_rules": [],
        "assertions": [rule("exact-source", "/facts/source_sha", subject_sha), rule("source-clean", "/facts/source_clean", True), rule("required-documents", "/facts/required_file_count", 13), rule("observed-documents", "/facts/observed_file_count", 13), rule("no-document-errors", "/facts/error_count", 0), rule("command-completed", "/facts/command_exit", 0)],
        "resource_budget": {"max_artifact_bytes": 1048576, "command_timeout_seconds": 30},
        "change_scope": {"level": "L2_CONTRACT", "owner": "Qixu M0", "consumers": ["document-gates"]},
        "reproduction_steps": ["Install the public Core 0.13.0 wheel with its documented digest.", "Run this adapter from an exact clean Qixu checkout with a fresh observation identity."],
        "cleanup_steps": ["Retain this immutable bundle for review; no services or databases are created."],
    })
    write_new(output / "sealed-plan.json", plan)  # happens before any subject observation
    write_new(output / "request.json", request)
    subject_root = ROOT
    if args.control == "missing-contract":
        from verify_docs import REQUIRED
        subject_root = output / "controlled-documents"
        for name in REQUIRED:
            if name == "docs/contracts/conflicts.md":
                continue
            path = subject_root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes((ROOT / name).read_bytes())
    completed = subprocess.run(
        [sys.executable, str(ROOT / "scripts/verify_docs.py"), "--root", str(subject_root), "--output", str(output / "raw-observation.json")],
        cwd=ROOT, capture_output=True, text=True, encoding="utf-8", timeout=30,
    )
    (output / "stdout.txt").write_text(completed.stdout, encoding="utf-8")
    (output / "stderr.txt").write_text(completed.stderr, encoding="utf-8")
    raw_path = output / "raw-observation.json"
    facts = json.loads(raw_path.read_text(encoding="utf-8")) if raw_path.exists() else {"collection_error": "NO_STRUCTURED_OBSERVATION"}
    facts.update(source_sha=git("rev-parse", "HEAD"), source_clean=not bool(git("status", "--porcelain")), command_exit=completed.returncode, control=args.control)
    evidence = {
        "schema_version": "0.1", "evidence_type": spec["evidence_type"], "source": "qixu-doc-observation/0.1",
        "captured_at": datetime.now(timezone.utc).isoformat(), "facts": facts,
        "metadata": {"veritrail_observation": {
            "schema_version": "0.1", "canonicalization_profile": "veritrail-json-c14n/1",
            "plan_digest": plan["seal"]["digest"], "observation_spec_digest": observation_spec_digest(spec),
            "request_seal_digest": sha256_json(request), "collection_session_id": run_id,
            "collector_role": "qixu-doc-collector", "coverage": "COMPLETE" if raw_path.exists() else "ERROR",
            "normalization_semantics_version": "qixu-doc-observation/0.1", "facts_digest": sha256_json(facts),
        }},
    }
    write_new(output / "evidence.json", evidence)
    report = create_acceptance_bundle(plan=plan, evidence_paths=[output / "evidence.json"], output=output / "bundle", acceptance_id=run_id, execution_status="COMPLETED" if raw_path.exists() else "ERROR")
    print(json.dumps({"identity": run_id, "source_sha": subject_sha, "control": args.control, "verdict": report["verdict"], "bundle": str(output / "bundle"), "boundary": "M0_DOCUMENTS_ONLY"}, ensure_ascii=False))
    return 0 if report["verdict"] == ("FAIL" if args.control != "none" else "PASS") else 1


if __name__ == "__main__":
    sys.exit(main())
