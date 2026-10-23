"""Check limited credential signatures in tracked text; never print candidate values."""
import argparse
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SIGNATURES = {
    'private-key': r'-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----',
    'github-token': r'\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{40,})\b',
    'api-secret': r'\bsk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{32,}\b',
    'slack-token': r'\bxox[baprs]-[A-Za-z0-9-]{24,}\b',
}


def matches(text):
    return sorted({(text.count('\n', 0, m.start()) + 1, rule)
                   for rule, pattern in SIGNATURES.items() for m in re.finditer(pattern, text)})


def scan(root):
    root = root.resolve()
    files = subprocess.check_output(['git', 'ls-files', '-z', '--', '.'], cwd=root).decode('utf-8').split('\0')
    failures, texts, binaries = 0, 0, 0
    for name in filter(None, files):
        path = (root / name).resolve()
        if not path.is_relative_to(root):
            raise ValueError('outside repository')
        data = path.read_bytes()
        if data.startswith((b'\xff\xfe', b'\xfe\xff')):
            text = data.decode('utf-16')
        elif b'\0' in data:
            binaries += 1
            continue
        else:
            text = data.decode('utf-8-sig')
        texts += 1
        for line, rule in matches(text):
            failures += 1
            print(f'{name}:{line}: {rule}; value withheld', file=sys.stderr)
    if not texts:
        raise ValueError('empty text denominator')
    print(f'Credential signatures: {texts} tracked texts; {binaries} binary files outside scope; {failures} matches.')
    return 1 if failures else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args()
    try:
        return scan(args.root)
    except (OSError, UnicodeError, ValueError, subprocess.SubprocessError):
        print('Credential check incomplete: path/read/encoding/repository failure; not PASS.', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
