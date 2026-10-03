# VeriTrail acceptance `m7-restore-53bac83347604e8e881a9b6b524e02e5`

- Execution status: `ERROR`
- Verdict: `PENDING`
- Plan: `qixu-m7-restore@1`
- Plan SHA-256: `6ff90f8d168e3636600415bb616425be6aeb90cbc950102e2e4fb0d54e767168`
- Created at: `2026-10-03T04:59:23.977030Z`

## Reasons

- `ACCEPTANCE_EVIDENCE_PENDING` — Execution or required Evidence is not complete enough for a verdict\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| restore | qixu\.m7\.restore | RESOLVED | 9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | FAIL | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| restore\-0 | ASSERTION | PASS | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"70f49ed2cef3f4f43870c68be8748b255f172973"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"70f49ed2cef3f4f43870c68be8748b255f172973"\} |
| restore\-1 | ASSERTION | PASS | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-2 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-3 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal\_after\_restart","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-4 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/world/original\_notification\_observed","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-5 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/world/witness\_unchanged\_after\_restore","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-6 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/health\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-7 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/health\_code","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-8 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/competitor\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-9 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/receipt\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-10 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/replay\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-11 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/reservations\_after","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-12 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/restored/inbox\_after","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-13 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-14 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_code","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-15 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/missing\_journal/receipt\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-16 | ASSERTION | PASS | \{"evidence\_sha256":"9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.restore` — `9b1d25f1d67cf9e2ce903a00ac429329a231bef19751b80e3888987494531d4f` (1507 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-restore","source_ref":"github:NoctilumeDev/Qixu","version":"70f49ed2cef3f4f43870c68be8748b255f172973"}`
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
