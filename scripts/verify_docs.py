"""M0 structural observation; this does not qualify product functionality."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

REQUIRED = (
    "README.md", "AGENTS.md", "LICENSE", "docs/m0.md", "docs/milestones.md",
    "docs/decisions.md", "docs/failure-notebook.md", "docs/verification.md",
    "docs/contracts/allocation.md", "docs/contracts/waitlist.md",
    "docs/contracts/venue.md", "docs/contracts/conflicts.md",
)


def observe(root: Path) -> dict:
    errors: list[str] = []
    identities: dict[str, str] = {}
    for name in REQUIRED:
        path = root / name
        if not path.is_file():
            errors.append(f"MISSING:{name}")
            continue
        raw = path.read_bytes()
        identities[name] = hashlib.sha256(raw).hexdigest()
        if not raw.strip():
            errors.append(f"EMPTY:{name}")
        text = raw.decode("utf-8-sig")
        for target in re.findall(r"\]\(([^)]+)\)", text):
            if "://" in target or target.startswith("#"):
                continue
            destination = target.split("#", 1)[0].strip("<>")
            if not destination:
                continue
            candidate = (path.parent / destination).resolve()
            if not candidate.is_relative_to(root):
                errors.append(f"OUTSIDE_LINK:{name}:{destination}")
            elif not candidate.exists():
                errors.append(f"BROKEN_LINK:{name}:{destination}")
    return {
        "contract": "qixu-doc-observation/0.1",
        "required_file_count": len(REQUIRED),
        "observed_file_count": len(identities),
        "error_count": len(errors),
        "errors": errors,
        "files": identities,
        "boundary": "DOCUMENT_STRUCTURE_ONLY_NOT_PRODUCT_ACCEPTANCE",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = observe(args.root.resolve())
    rendered = json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        if args.output.exists():
            raise FileExistsError("Observation outputs must use a fresh identity")
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 1 if result["error_count"] else 0


if __name__ == "__main__":
    sys.exit(main())
