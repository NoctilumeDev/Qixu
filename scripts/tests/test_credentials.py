import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).parents[1] / 'check_credentials.py'
spec = importlib.util.spec_from_file_location('credentials', SCRIPT)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class CredentialTests(unittest.TestCase):
    def test_all_signatures_withhold_values(self):
        for token in ['ghp_' + 'z' * 36, 'github_pat_' + 'z' * 50, 'sk-' + 'z' * 48,
                      'xoxb-' + 'z' * 30, '-----BEGIN ' + 'PRIVATE KEY-----']:
            result = m.matches('header\n' + token)
            self.assertEqual(len(result), 1)
            self.assertEqual(result[0][0], 2)
            self.assertNotIn(token, str(result))
        self.assertEqual(m.matches('${QIXU_DB_PASSWORD} qixu-ci-app-only'), [])

    def test_real_process_modes_and_failed_inputs(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(['git', 'init', '-q', directory], check=True)
            path = root / 'fixture.txt'
            path.write_text('placeholder', encoding='utf-8')
            subprocess.run(['git', 'add', 'fixture.txt'], cwd=root, check=True)
            token = 'ghp_' + 'z' * 36
            for optimized in ([], ['-O']):
                cmd = [sys.executable, *optimized, '-B', str(SCRIPT), '--root', directory]
                for data, expected in [(b'placeholder', 0), (token.encode(), 1), (token.encode('utf-16'), 1), (b'\xffbad', 1)]:
                    path.write_bytes(data)
                    result = subprocess.run(cmd, text=True, capture_output=True)
                    self.assertEqual(result.returncode, expected, result.stdout + result.stderr)
                    self.assertNotIn(token, result.stdout + result.stderr)
                path.unlink()
                self.assertEqual(subprocess.run(cmd, capture_output=True).returncode, 1)

    def test_empty_repository_is_not_success(self):
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(['git', 'init', '-q', directory], check=True)
            self.assertNotEqual(subprocess.run([sys.executable, '-B', str(SCRIPT), '--root', directory], capture_output=True).returncode, 0)


if __name__ == '__main__':
    unittest.main()
