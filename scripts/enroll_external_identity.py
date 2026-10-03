"""Explicit database-operator binding. No automatic account creation or role grant.

Credentials come from QIXU_DB_USERNAME/PASSWORD and never appear in arguments or
output. Enable requires --external-only and the expected local authorization
version. Unbinding does not silently re-enable password login.
"""
from __future__ import annotations
import argparse
import hashlib
import os
import re
import subprocess
import urllib.parse
import uuid


def quote(value: str) -> str:
    # Hex UTF-8 literals avoid SQL-mode-dependent quoting and command interpolation.
    return "CONVERT(0x" + value.encode("utf-8").hex() + " USING utf8mb4)"


def main() -> int:
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--mysql",default="mysql");p.add_argument("--schema",default="qixu")
    p.add_argument("--subject",required=True);p.add_argument("--user-id",type=int,required=True)
    p.add_argument("--expected-auth-version",type=int,required=True)
    p.add_argument("--issuer-uri",required=True);p.add_argument("--allow-loopback-http",action="store_true")
    p.add_argument("--action",choices=["enable","disable"],required=True);p.add_argument("--external-only",action="store_true")
    p.add_argument("--reason",required=True);a=p.parse_args()
    if not re.fullmatch(r"qixu(?:_test|_ci)?",a.schema) or not re.fullmatch(r"[1-9][0-9]{0,17}",a.subject) or a.user_id<=0 or a.expected_auth_version<=0 or not 4<=len(a.reason)<=500:
        p.error("Invalid explicit binding coordinates/reason")
    u=urllib.parse.urlsplit(a.issuer_uri)
    local=a.allow_loopback_http and u.scheme=="http" and u.hostname in {"127.0.0.1","::1"}
    if not (u.scheme=="https" or local) or not u.hostname or u.username or u.password or u.query or u.fragment or u.path!="/api/dark-room-library/v1/user/auth" or any(ord(c)>127 for c in a.issuer_uri):
        p.error("Issuer must be a fixed ASCII HTTPS auth URI (explicit loopback only)")
    if a.action=="enable" and not a.external_only:p.error("Enable requires explicit --external-only; existing local password access will be disabled")
    user,password=os.environ.get("QIXU_DB_USERNAME"),os.environ.get("QIXU_DB_PASSWORD")
    if not user or not password:p.error("QIXU_DB_USERNAME/PASSWORD required")
    issuer=hashlib.sha256(a.issuer_uri.encode("ascii")).hexdigest();subject=quote(a.subject)
    enabled=1 if a.action=="enable" else 0
    sql=f"""SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT id FROM identity_user WHERE id={a.user_id} FOR UPDATE;
SELECT id FROM external_identity WHERE provider='DARK_ROOM_LIBRARY' AND subject={subject} FOR UPDATE;
SET @allowed=(SELECT COUNT(*)=1 FROM identity_user WHERE id={a.user_id} AND active=TRUE AND auth_version={a.expected_auth_version});
SET @allowed=@allowed AND NOT EXISTS(SELECT 1 FROM external_identity WHERE provider='DARK_ROOM_LIBRARY' AND subject={subject} AND user_id<>{a.user_id});
INSERT INTO external_identity(provider,subject,user_id,active,issuer_hash) SELECT 'DARK_ROOM_LIBRARY',{subject},{a.user_id},FALSE,'{issuer}' WHERE @allowed AND NOT EXISTS(SELECT 1 FROM external_identity WHERE provider='DARK_ROOM_LIBRARY' AND subject={subject});
UPDATE external_identity SET active={enabled},version=version+1,issuer_hash='{issuer}' WHERE provider='DARK_ROOM_LIBRARY' AND subject={subject} AND user_id={a.user_id} AND @allowed;
UPDATE identity_user SET local_login_enabled=FALSE,auth_version=auth_version+1 WHERE id={a.user_id} AND @allowed;
INSERT INTO audit_entry(actor_id,action,entity_type,entity_id,request_id,detail_json,created_at)
SELECT NULL,'OPERATOR_EXTERNAL_BINDING','EXTERNAL_IDENTITY',CAST(id AS CHAR),'{uuid.uuid4()}',JSON_OBJECT('action','{a.action}','reason',{quote(a.reason)},'localUser',{a.user_id},'authority','database_operator'),UTC_TIMESTAMP(6)
FROM external_identity WHERE provider='DARK_ROOM_LIBRARY' AND subject={subject} AND user_id={a.user_id} AND @allowed;
SELECT IF(@allowed,'BINDING_COMMITTED','BINDING_REJECTED');
COMMIT;"""
    env=dict(os.environ,MYSQL_PWD=password)
    command=[a.mysql,"--protocol=TCP","-h","127.0.0.1","-P","3306","-u",user,"--default-character-set=utf8mb4","--batch","--skip-column-names",a.schema]
    r=subprocess.run(command,input=sql,text=True,encoding="utf-8",capture_output=True,env=env,timeout=20)
    if r.returncode:print("Binding transaction failed; no credential or SQL output published");return 1
    if "BINDING_COMMITTED" not in r.stdout:print("Binding rejected: current local identity/version or existing ownership differs");return 2
    print("Explicit external binding committed; local roles/scopes unchanged and password login disabled");return 0


if __name__=="__main__":raise SystemExit(main())
