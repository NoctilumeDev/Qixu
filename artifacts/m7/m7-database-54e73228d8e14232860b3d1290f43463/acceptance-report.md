# VeriTrail acceptance `m7-database-54e73228d8e14232860b3d1290f43463`

- Execution status: `COMPLETED`
- Verdict: `FAIL`
- Plan: `qixu-m7-database@1`
- Plan SHA-256: `e9efd4552a4a4fc9c21969b56759eff37e7decb36c494ef9c121c1733c9632a2`
- Created at: `2026-10-03T06:10:18.408716Z`

## Reasons

- `ACCEPTANCE_ASSERTION_FAILED` — At least one decisive assertion is false\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| database | qixu\.m7\.database | RESOLVED | 9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| database\-0 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"f46cf774c7a6d1048e39e1479ff806c4ed4ac1ec"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"f46cf774c7a6d1048e39e1479ff806c4ed4ac1ec"\} |
| database\-1 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-2 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-3 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-4 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-5 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/normal/unique\_sql","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-6 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/wait\_observed","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-7 | ASSERTION | FAIL | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/response\_within\_15s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-8 | ASSERTION | FAIL | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":\-1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-9 | ASSERTION | FAIL | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":null\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-10 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/no\_business\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-11 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/no\_marker\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-12 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/retry\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-13 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/retry\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-14 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/lock/unique\_effect","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-15 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/borrowed\_flow\_hit","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-16 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/response\_within\_35s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-17 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-18 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-19 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/no\_mutation","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-20 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/recovery\_read\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-21 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/io/recovery\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-22 | ASSERTION | FAIL | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/configuration/app\_lock\_wait\_values","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":\[50\]\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":\[10\]\} |
| database\-23 | ASSERTION | PASS | \{"evidence\_sha256":"9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.database` — `9f0aae16dbf6e55cdf91b8789cca042ea3219cb48033f6e3a922c6af20115936` (3878 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-database","source_ref":"github:NoctilumeDev/Qixu","version":"f46cf774c7a6d1048e39e1479ff806c4ed4ac1ec"}`
- Question: `Do owned resource waits and borrowed network reads end within frozen observation budgets without duplicate business authority?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-database-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-database"],"level":"L2_CONTRACT","owner":"Qixu F15 owned database stalls"}`

## Reproduction and cleanup

1. Fresh exact native0\.17/Plan5 producer\. Seal before own MySQL and opaque loopback relay\. Normal booking; real floor lock wait with15s observation then release/same\-key replay; drop only replies on existing borrowed connection with35s observation, then remove fault and verify authoritative reads\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
