# VeriTrail acceptance `m7-restore-96c40b10e55547159489bacc8a73d930`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m7-restore@3`
- Plan SHA-256: `53a5ffe7f2085ab6bca8288d688126a35a546a7c0f1116f910363e0ec8d386cb`
- Created at: `2026-10-03T05:26:14.776987Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| restore | qixu\.m7\.restore | RESOLVED | fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| restore\-0 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"88df403d9df6bac79d8943f063dc48d81f7fc93e"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"88df403d9df6bac79d8943f063dc48d81f7fc93e"\} |
| restore\-1 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-2 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-3 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| restore\-4 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal\_after\_restart","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-5 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/world/original\_notification\_observed","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-6 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/world/witness\_unchanged\_after\_restore","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-7 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-8 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-9 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/competitor\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-10 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-11 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/replay\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-12 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/reservations\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-13 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/restored/inbox\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-14 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-15 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-16 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/missing\_journal/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-17 | ASSERTION | PASS | \{"evidence\_sha256":"fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.restore` — `fc5a98b9d578a55224b05f06070ad1f077195dff4d616c4a4ceb6545cfe9010b` (3669 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-restore","source_ref":"github:NoctilumeDev/Qixu","version":"88df403d9df6bac79d8943f063dc48d81f7fc93e"}`
- Question: `Does a restored database or missing independent ledger quarantine authority rather than silently replay committed rights?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-restore-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-restore"],"level":"L2_CONTRACT","owner":"Qixu F12 single-instance recovery"}`

## Reproduction and cleanup

1. Use exact clean source and its original native0\.16/Plan4 PASS Bundle with fresh jar\.
2. Run this collector with the pinned Windows Java/MySQL binaries; it creates a new owned data directory and schema, seals before initialization, captures T0 and independent T1 witness, stops owned processes, restores T0 and probes quarantine\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
