"""Current maintenance checks. Read-only hygiene; never deletes local state."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
GENERATED = {"node_modules", "target", "dist", "__pycache__", "coverage",
             "playwright-report", "test-results", "runtime", "uploads", ".tools"}
PRIVATE = {"local.ps1", "project.private.config.json", "database.local.json"}


def bound_log(name):
    target = (ROOT / name).resolve()
    if not target.is_relative_to(ROOT) or not target.is_file():
        return False
    for directory in target.parents:
        if directory == ROOT:
            break
        manifest = directory / "acceptance-bundle-manifest.json"
        if manifest.is_file():
            try:
                entries = json.loads(manifest.read_text(encoding="utf-8"))["files"]
                entry = next(e for e in entries if e["path"] == target.relative_to(directory).as_posix())
                raw = target.read_bytes()
                return entry["size"] == len(raw) and entry["sha256"] == hashlib.sha256(raw).hexdigest()
            except (ValueError, KeyError, StopIteration, TypeError):
                return False
    return False


def residual_reason(name):
    path = Path(name)
    if GENERATED.intersection(path.parts):
        return "tracked generated/runtime directory"
    if path.name in PRIVATE or (path.name.startswith(".env") and path.name != ".env.example"):
        return "tracked private environment"
    # Formal logs are retained by their original binding verifier. This never
    # exempts a cache directory or private environment inside artifacts.
    if (PROTECTED and path.parts and path.parts[0] in PROTECTED
            and path.suffix.lower() == ".log" and bound_log(name)):
        return None
    if path.suffix.lower() in {".pyc", ".log", ".pid", ".tmp"}:
        return "tracked transient file"
    return None


def link_errors(root, documents):
    errors = []
    for name in documents:
        document = root / name
        if not document.is_file():
            errors.append(f"missing current document: {name}")
            continue
        text = document.read_text(encoding="utf-8")
        targets = re.findall(r"!?\[[^\]\n]*\]\((<[^>]+>|[^\s)]+)", text)
        targets += re.findall(r"^\[[^\]]+\]:\s*(\S+)", text, re.M)
        for target in targets:
            target = target.strip("<>")
            if target.startswith("#") or urlsplit(target).scheme:
                continue
            relative = unquote(target.split("#", 1)[0].split("?", 1)[0])
            if relative and not (document.parent / relative).exists():
                errors.append(f"{name}: broken local link: {target}")
    return errors


def hygiene():
    files = subprocess.check_output(
        ["git", "ls-files", "-z", "--", "."], cwd=ROOT).decode("utf-8").split("\0")
    files = [name for name in files if name]
    errors = [f"{name}: {reason}" for name in files
              if (reason := residual_reason(name))]
    errors += link_errors(ROOT, CURRENT_DOCS)
    for error in errors:
        print(error, file=sys.stderr)
    if errors:
        return 1
    print(f"Repository hygiene: PASS ({len(files)} tracked paths; current local links)")
    print("Scope: known tracked residues only. Image consumers, retained evidence and")
    print("unique local state require stage review; LOCAL_DORMANT is not certified.")
    return 0


def executable(name):
    found = shutil.which(name + ".cmd") if os.name == "nt" else None
    found = found or shutil.which(name)
    if not found:
        raise RuntimeError(f"Missing {name}; install the documented toolchain first")
    return found


def run(command, cwd=ROOT, env=None):
    print("Run: " + " ".join(map(str, command)), flush=True)
    subprocess.run(command, cwd=cwd, env=env, check=True)


def mysql_environment(prefix, allowed):
    names = [prefix + suffix for suffix in DB_SUFFIXES]
    values = [os.environ.get(name, "") for name in names]
    if not all(values):
        raise RuntimeError("MySQL checks require " + ", ".join(names))
    match = re.fullmatch(r"jdbc:mysql://[^/?]+/([a-z_]+)(?:\?[^#]*)?", values[0])
    if not match or match[1] not in allowed:
        raise RuntimeError("Refusing non-test database URL")
    return os.environ.copy()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("group", choices=GROUPS)
    args = parser.parse_args()
    try:
        for group in EXPAND.get(args.group, [args.group]):
            check(group)
    except (RuntimeError, subprocess.CalledProcessError) as error:
        print(f"Check failed: {error}", file=sys.stderr)
        return 1
    return 0


PROTECTED = {"artifacts"}
CURRENT_DOCS = ["README.md", "docs/running.md", "docs/milestones.md", "docs/architecture.md"]
DB_SUFFIXES = ["_URL", "_USER", "_PASSWORD"]
GROUPS = ["hygiene", "docs", "frontend", "backend", "all"]
EXPAND = {"all": ["hygiene", "docs", "frontend", "backend"]}


def check(group):
    if group == "hygiene":
        if hygiene():
            raise RuntimeError("Repository hygiene rejected")
        run([sys.executable, "-B", "-m", "unittest", "discover", "-s", "scripts/tests"])
    elif group == "docs":
        for script in ["verify_docs.py", "verify_artifacts.py"]:
            run([sys.executable, "-B", str(ROOT / "scripts" / script)])
    elif group == "frontend":
        for task in ["test:client", "check", "build"]:
            run([executable("npm"), "run", task])
    elif group == "backend":
        env = mysql_environment("QIXU_TEST_DB", {"qixu_test", "qixu_ci"})
        run([executable("npm"), "test", "--prefix", "randomness"])
        run([executable("mvn"), "-B", "-ntp", "-Pmysql-it", "clean", "verify"], ROOT / "backend", env)


if __name__ == "__main__":
    sys.exit(main())
