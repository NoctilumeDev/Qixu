# VeriTrail acceptance `m7-restore-1a767fd49106407d96d639211f9219eb`

- Execution status: `COMPLETED`
- Verdict: `FAIL`
- Plan: `qixu-m7-restore@2`
- Plan SHA-256: `bef7507f9673f436747102cf06593e4b80c0fe35deff2fc4dbc0691931795371`
- Created at: `2026-10-03T05:07:12.283692Z`

## Reasons

- `ACCEPTANCE_ASSERTION_FAILED` — At least one decisive assertion is false\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| restore | qixu\.m7\.restore | RESOLVED | e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| restore\-0 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"97a1d141b027d37255458bc9d4ed7636af54901e"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"97a1d141b027d37255458bc9d4ed7636af54901e"\} |
| restore\-1 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-2 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-3 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| restore\-4 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal\_after\_restart","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-5 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/world/original\_notification\_observed","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-6 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/world/witness\_unchanged\_after\_restore","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-7 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-8 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"MISSING"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-9 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/competitor\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-10 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":404\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-11 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/replay\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":409\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-12 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/reservations\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-13 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/restored/inbox\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-14 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-15 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"MISSING"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-16 | ASSERTION | FAIL | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/missing\_journal/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-17 | ASSERTION | PASS | \{"evidence\_sha256":"e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.restore` — `e4fc6f1bf2dc6b08651efbcf04ee3bc78e714d963766de7f693f88bcbc3a0197` (3545 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-restore","source_ref":"github:NoctilumeDev/Qixu","version":"97a1d141b027d37255458bc9d4ed7636af54901e"}`
- Question: `Does a restored database or missing independent ledger quarantine authority rather than silently replay committed rights?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-restore-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-restore"],"level":"L2_CONTRACT","owner":"Qixu F12 single-instance recovery"}`

## Reproduction and cleanup

1. Use exact clean source and its original native0\.15/Plan3 PASS Bundle with fresh jar\.
2. Run this collector with the pinned Windows Java/MySQL binaries; it creates a new owned data directory and schema, seals before initialization, captures T0 and independent T1 witness, stops owned processes, restores T0 and probes quarantine\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
