"""Verify the exact bytes of retained public bundles; does not upgrade verdicts."""
from __future__ import annotations
import hashlib
import json
import re
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    count = 0
    for stage in sorted((ROOT / "artifacts").iterdir()):
        if not stage.is_dir() or stage.name == "local":
            continue
        for manifest in sorted(stage.glob("*/acceptance-bundle-manifest.json")):
            data = json.loads(manifest.read_text(encoding="utf-8"))
            directory = manifest.parent.resolve()
            seen = set()
            for entry in data["files"]:
                relative = PurePosixPath(entry["path"])
                if relative.is_absolute() or ".." in relative.parts or entry["path"] in seen:
                    raise ValueError("Unsafe or duplicated manifest entry")
                seen.add(entry["path"])
                target = (directory / relative).resolve()
                if not target.is_relative_to(directory):
                    raise ValueError("Artifact escaped its immutable bundle")
                raw = target.read_bytes()
                if len(raw) != entry["size"] or hashlib.sha256(raw).hexdigest() != entry["sha256"]:
                    raise ValueError(f"Artifact bytes drifted: {stage.name}/{directory.name}/{relative}")
            actual = {p.relative_to(directory).as_posix() for p in directory.rglob("*") if p.is_file() and p != manifest}
            if actual != seen:
                raise ValueError("Unmanifested or missing bundle files")
            count += 1
    if count == 0:
        raise ValueError("No retained bundle was observed")
    m8 = ROOT / "artifacts/m8"
    capture_count = 0
    if m8.is_dir():
        index = json.loads((m8 / "index.json").read_text(encoding="utf-8"))
        registered = set()
        for entry in index["bundles"]:
            identity = entry["identity"]
            if not re.fullmatch(r"m8-[a-z0-9-]+", identity) or identity in registered:
                raise ValueError("Unsafe or duplicated M8 identity")
            registered.add(identity)
            directory = m8 / identity
            manifest = directory / "acceptance-bundle-manifest.json"
            report = json.loads((directory / "acceptance-report.json").read_text(encoding="utf-8"))
            plan = json.loads((directory / "sealed-acceptance-plan.json").read_text(encoding="utf-8"))
            if entry["path"] != f"artifacts/m8/{identity}" or entry["manifest_sha256"] != hashlib.sha256(manifest.read_bytes()).hexdigest():
                raise ValueError("M8 registry does not bind retained bytes")
            if report["acceptance_id"] != identity or report["subject"] != plan["subject"] or entry["source_sha"] != plan["subject"]["version"] or entry["verdict"] != report["verdict"]:
                raise ValueError("M8 registry changed an original subject or verdict")
        actual = {p.parent.name for p in m8.glob("*/acceptance-bundle-manifest.json")}
        if registered != actual:
            raise ValueError("Unregistered or missing M8 bundle")
        capture_manifest = m8 / "capture-manifest.json"
        if capture_manifest.exists():
            captures = json.loads(capture_manifest.read_text(encoding="utf-8"))
            seen = set()
            for entry in captures["files"]:
                relative = PurePosixPath(entry["path"])
                if relative.is_absolute() or ".." in relative.parts or relative.parts[0] != "captures" or "\\" in entry["path"] or entry["path"] in seen:
                    raise ValueError("Unsafe or duplicated M8 capture")
                seen.add(entry["path"])
                target = (m8 / relative).resolve()
                if not target.is_relative_to(m8.resolve()):
                    raise ValueError("M8 capture escaped its stage")
                raw = target.read_bytes()
                if len(raw) != entry["size"] or hashlib.sha256(raw).hexdigest() != entry["sha256"]:
                    raise ValueError("M8 original capture bytes drifted")
                capture_count += 1
            actual_files = {p.relative_to(m8).as_posix() for p in (m8 / "captures").rglob("*") if p.is_file()}
            if actual_files != seen:
                raise ValueError("Unregistered or missing M8 capture")
    m9_count = 0
    m9 = ROOT / "artifacts/m9"
    if m9.is_dir():
        index = json.loads((m9 / "index.json").read_text(encoding="utf-8"))
        registered = set()
        for entry in index["bundles"]:
            identity = entry["identity"]
            if not re.fullmatch(r"m[89]-[a-z0-9-]+", identity) or identity in registered:
                raise ValueError("Unsafe or duplicated M9 original identity")
            registered.add(identity)
            directory = m9 / identity
            report = json.loads((directory / "acceptance-report.json").read_text(encoding="utf-8"))
            plan = json.loads((directory / "sealed-acceptance-plan.json").read_text(encoding="utf-8"))
            if entry["path"] != f"artifacts/m9/{identity}" or entry["manifest_sha256"] != hashlib.sha256((directory / "acceptance-bundle-manifest.json").read_bytes()).hexdigest():
                raise ValueError("M9 registry does not bind original bytes")
            if report["acceptance_id"] != identity or report["subject"] != plan["subject"] or entry["source_sha"] != plan["subject"]["version"] or entry["verdict"] != report["verdict"] or entry["execution_status"] != report["execution_status"]:
                raise ValueError("M9 registry changed an original qualification")
        if registered != {p.parent.name for p in m9.glob("*/acceptance-bundle-manifest.json")}:
            raise ValueError("Unregistered or missing M9 original bundle")
        originals = json.loads((m9 / "original-file-manifest.json").read_text(encoding="utf-8"))
        seen = set()
        for entry in originals["files"]:
            relative = PurePosixPath(entry["path"])
            if relative.is_absolute() or ".." in relative.parts or "\\" in entry["path"] or relative.parts[0] not in {"captures", "runs", "incomplete-children"} or entry["path"] in seen:
                raise ValueError("Unsafe or duplicated M9 original file")
            seen.add(entry["path"])
            target = (m9 / relative).resolve()
            if not target.is_relative_to(m9.resolve()):
                raise ValueError("M9 original escaped its stage")
            raw = target.read_bytes()
            if len(raw) != entry["size"] or hashlib.sha256(raw).hexdigest() != entry["sha256"]:
                raise ValueError("M9 original file bytes drifted")
            m9_count += 1
        actual_files = {p.relative_to(m9).as_posix() for name in ("captures", "runs", "incomplete-children") for p in (m9 / name).rglob("*") if p.is_file()}
        if seen != actual_files:
            raise ValueError("Unregistered or missing M9 original file")
        parent = json.loads((m9 / index["qualified_parent"] / "acceptance-report.json").read_text(encoding="utf-8"))
        delivery = json.loads((m9 / "delivery-manifest.json").read_text(encoding="utf-8"))
        if parent["subject"]["version"] != index["qualified_source_sha"] or delivery["source_sha"] != index["qualified_source_sha"] or delivery["parent_identity"] != index["qualified_parent"]:
            raise ValueError("M9 delivery source drift")
        reviews = json.loads((m9 / "review-records.json").read_text(encoding="utf-8"))
        retained_reviews = set()
        for entry in reviews["reports"]:
            for document in entry["documents"]:
                if "public_path" not in document:
                    continue
                relative = PurePosixPath(document["public_path"])
                if relative.is_absolute() or ".." in relative.parts or "\\" in document["public_path"] or relative.parts[0] != "reviews" or document["public_path"] in retained_reviews:
                    raise ValueError("Unsafe or duplicated M9 review file")
                retained_reviews.add(document["public_path"])
                raw = (m9 / relative).read_bytes()
                if len(raw) != document["size"] or hashlib.sha256(raw).hexdigest() != document["sha256"]:
                    raise ValueError("M9 original review bytes drifted")
        if retained_reviews != {p.relative_to(m9).as_posix() for p in (m9 / "reviews").rglob("*") if p.is_file()}:
            raise ValueError("Unregistered or missing M9 review file")
    print(json.dumps({"bundles_observed": count, "m8_capture_files": capture_count, "m9_original_files": m9_count, "byte_integrity": "PRESERVED", "qualification": "UNCHANGED"}))


if __name__ == "__main__":
    main()
