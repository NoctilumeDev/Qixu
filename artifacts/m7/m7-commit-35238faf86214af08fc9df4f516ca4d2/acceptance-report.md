# VeriTrail acceptance `m7-commit-35238faf86214af08fc9df4f516ca4d2`

- Execution status: `COMPLETED`
- Verdict: `FAIL`
- Plan: `qixu-m7-commit@1`
- Plan SHA-256: `152fec8139ae6fc28f3b1088db09bbb006482489a0b5e6890b4f0bf0443ddec7`
- Created at: `2026-10-03T07:12:13.434184Z`

## Reasons

- `ACCEPTANCE_ASSERTION_FAILED` — At least one decisive assertion is false\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| commit | qixu\.m7\.commit | RESOLVED | 4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| commit\-0 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":"978983a079c0f638d90a645d28d5f909210f21e8"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"978983a079c0f638d90a645d28d5f909210f21e8"\} |
| commit\-1 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-2 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-3 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| commit\-4 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-5 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/normal/unique\_sql","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-6 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/borrowed\_flow\_hit","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-7 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/exact\_commit\_hit","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-8 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/response\_within\_35s","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-9 | ASSERTION | FAIL | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/status","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":500\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| commit\-10 | ASSERTION | FAIL | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/code","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":"INTERNAL\_ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| commit\-11 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/unique\_committed\_effect","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-12 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/one\_new\_marker","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-13 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/commit/generation\_unchanged","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-14 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/quarantine/statuses","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":\[503,503,503,503\]\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":\[503,503,503,503\]\} |
| commit\-15 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/quarantine/codes","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":\["NOT\_RECONCILED","NOT\_RECONCILED","NOT\_RECONCILED","NOT\_RECONCILED"\]\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":\["NOT\_RECONCILED","NOT\_RECONCILED","NOT\_RECONCILED","NOT\_RECONCILED"\]\} |
| commit\-16 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/quarantine/sql\_unchanged","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-17 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/uncertain\_ledger/prepare\_matches\_committed\_marker","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-18 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/uncertain\_ledger/no\_false\_terminal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-19 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/new\_pid","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-20 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/health\_status","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| commit\-21 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/generation\_unchanged","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-22 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/receipt\_equal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-23 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/replay\_equal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-24 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/unique\_effect","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-25 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/control\_receipt\_equal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-26 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/complete\_ledger\_equal","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-27 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/recovery/one\_recovered\_commit","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| commit\-28 | ASSERTION | PASS | \{"evidence\_sha256":"4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"commit","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.commit` — `4ba8a9ebd93d5060c42dee2e33e1e40af957b174394f9e8657acc1dd150ff915` (5162 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-commit","source_ref":"github:NoctilumeDev/Qixu","version":"978983a079c0f638d90a645d28d5f909210f21e8"}`
- Question: `Does actual committed SQL with a lost COMMIT reply quarantine live authority and recover the same receipt once from independent marker evidence?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-commit-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-commit"],"level":"L2_CONTRACT","owner":"Qixu F12 owned COMMIT reply loss"}`

## Reproduction and cleanup

1. Fresh exact native0\.18/Plan6 producer\. Seal before owned MySQL and loopback relay\. Exact COMMIT response drop; independently read committed SQL, observe quarantine, stop owned app and validate pending ledger, restart same DB/ledger and replay original key, validate recovered complete ledger\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
