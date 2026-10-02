"""Verify the exact bytes of retained public bundles; does not upgrade verdicts."""
from __future__ import annotations
import hashlib
import json
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
    print(json.dumps({"bundles_observed": count, "byte_integrity": "PRESERVED", "qualification": "UNCHANGED"}))


if __name__ == "__main__":
    main()
