"""Create dedicated development databases once; never alter an existing database.

Requires a local MySQL admin supplied through QIXU_ADMIN_USER/PASSWORD. Secrets
are saved only under ignored .tools and are never printed. No delete operation.
"""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql", default="mysql")
    args = parser.parse_args()
    username, password = os.environ.get("QIXU_ADMIN_USER"), os.environ.get("QIXU_ADMIN_PASSWORD")
    if not username or not password:
        raise SystemExit("QIXU_ADMIN_USER and QIXU_ADMIN_PASSWORD are required")
    target = ROOT / ".tools/database.local.json"
    if target.exists():
        raise SystemExit("A provisioning record exists; reuse its dedicated databases, do not reprovision")
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    command = [args.mysql, "--protocol=TCP", "-h", "127.0.0.1", "-P", "3306", "-u", username, "--batch", "--skip-column-names"]

    def sql(statement: str) -> str:
        result = subprocess.run(command, input=statement, text=True, encoding="utf-8", capture_output=True, env=env, timeout=20)
        if result.returncode:
            # Never echo a statement containing credentials on a failure path.
            raise RuntimeError("MySQL operation failed; no credential output is permitted")
        return result.stdout.strip()

    if sql("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('qixu','qixu_test');") != "0":
        raise SystemExit("An intended schema already exists; refused to touch it")
    if sql("SELECT COUNT(*) FROM mysql.user WHERE user IN ('qixu_app','qixu_test_app');") != "0":
        raise SystemExit("An intended account already exists; refused to touch it")
    data = {}
    for schema, user in [("qixu", "qixu_app"), ("qixu_test", "qixu_test_app")]:
        generated = secrets.token_hex(24)
        sql(f"CREATE DATABASE `{schema}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
        sql(f"CREATE USER '{user}'@'127.0.0.1' IDENTIFIED BY '{generated}';")
        sql(f"GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX,REFERENCES ON `{schema}`.* TO '{user}'@'127.0.0.1';")
        data[schema] = {"username": user, "password": generated, "url": f"jdbc:mysql://127.0.0.1:3306/{schema}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=utf8"}
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("x", encoding="utf-8") as stream:
        json.dump(data, stream, indent=2)
    print("Created qixu and qixu_test with separate schema-scoped accounts; credentials are ignored locally")


if __name__ == "__main__":
    main()
