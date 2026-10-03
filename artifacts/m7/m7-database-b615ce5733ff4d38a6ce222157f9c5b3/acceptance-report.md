# VeriTrail acceptance `m7-database-b615ce5733ff4d38a6ce222157f9c5b3`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m7-database@5`
- Plan SHA-256: `66f63c855dc7f8aff2e5edb526591b777b7926ceb81902553d7d246a93026291`
- Created at: `2026-10-03T06:49:35.500914Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| database | qixu\.m7\.database | RESOLVED | cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| database\-0 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"e704b10e2ae739648f5551928f121b4ba4cecf46"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"e704b10e2ae739648f5551928f121b4ba4cecf46"\} |
| database\-1 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-2 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-3 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-4 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-5 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/normal/unique\_sql","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-6 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/wait\_observed","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-7 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/response\_within\_15s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-8 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-9 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-10 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/no\_business\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-11 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/no\_marker\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-12 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/retry\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-13 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/retry\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-14 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/lock/unique\_effect","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-15 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/borrowed\_flow\_hit","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-16 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/target\_query\_hit","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-17 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/response\_within\_35s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-18 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-19 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-20 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/no\_mutation","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-21 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/recovery\_read\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-22 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/io/recovery\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-23 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/configuration/app\_lock\_wait\_values","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":\[10\]\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":\[10\]\} |
| database\-24 | ASSERTION | PASS | \{"evidence\_sha256":"cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.database` — `cbeb645206836e5c021f463bea8bd5478e7abfbb0311a9c521d77d38eee1126e` (4181 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-database","source_ref":"github:NoctilumeDev/Qixu","version":"e704b10e2ae739648f5551928f121b4ba4cecf46"}`
- Question: `Do owned resource waits and borrowed network reads end within frozen observation budgets without duplicate business authority?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-database-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-database"],"level":"L2_CONTRACT","owner":"Qixu F15 owned database stalls"}`

## Reproduction and cleanup

1. Fresh exact native0\.18/Plan6 producer\. Seal before own MySQL and opaque loopback relay\. Normal booking; real floor lock wait with15s observation then release/same\-key replay; drop only replies on existing borrowed connection with35s observation, then remove fault and verify authoritative reads\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
