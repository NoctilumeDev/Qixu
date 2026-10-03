# VeriTrail acceptance `m7-database-0997e88828a54be9aef3d749cc7f66f5`

- Execution status: `ERROR`
- Verdict: `PENDING`
- Plan: `qixu-m7-database@2`
- Plan SHA-256: `b35273dcebc3f95a8c4871b056aa43c4d7e8c44b30494dd26a9ffc4861dfc010`
- Created at: `2026-10-03T06:15:51.760449Z`

## Reasons

- `ACCEPTANCE_EVIDENCE_PENDING` — Execution or required Evidence is not complete enough for a verdict\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| database | qixu\.m7\.database | RESOLVED | 17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | FAIL | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| database\-0 | ASSERTION | PASS | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"dbdb83b399a89d091441609099077271f5e2fea4"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"dbdb83b399a89d091441609099077271f5e2fea4"\} |
| database\-1 | ASSERTION | PASS | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-2 | ASSERTION | PASS | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-3 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-4 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-5 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/normal/unique\_sql","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-6 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/wait\_observed","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-7 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/response\_within\_15s","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-8 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/status","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-9 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/code","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-10 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/no\_business\_while\_guard\_held","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-11 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/no\_marker\_while\_guard\_held","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-12 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/retry\_status","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-13 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/retry\_receipt\_equal","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-14 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/lock/unique\_effect","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-15 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/borrowed\_flow\_hit","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-16 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/target\_query\_hit","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-17 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/response\_within\_35s","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-18 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/status","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-19 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/code","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-20 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/no\_mutation","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-21 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/recovery\_read\_status","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-22 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/io/recovery\_receipt\_equal","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-23 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/configuration/app\_lock\_wait\_values","requirement\_id":"database","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| database\-24 | ASSERTION | PASS | \{"evidence\_sha256":"17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.database` — `17aa15a225433282e586fa57d7c3ff02acbb15bf153851f47c32a25a1711b044` (1672 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-database","source_ref":"github:NoctilumeDev/Qixu","version":"dbdb83b399a89d091441609099077271f5e2fea4"}`
- Question: `Do owned resource waits and borrowed network reads end within frozen observation budgets without duplicate business authority?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-database-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-database"],"level":"L2_CONTRACT","owner":"Qixu F15 owned database stalls"}`

## Reproduction and cleanup

1. Fresh exact native0\.17/Plan5 producer\. Seal before own MySQL and opaque loopback relay\. Normal booking; real floor lock wait with15s observation then release/same\-key replay; drop only replies on existing borrowed connection with35s observation, then remove fault and verify authoritative reads\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
