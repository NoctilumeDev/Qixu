# VeriTrail acceptance `m7-restore-0525fb9a74c84b82a28c2e989dad4c41`

- Execution status: `ERROR`
- Verdict: `PENDING`
- Plan: `qixu-m7-restore@6`
- Plan SHA-256: `37b39adf37a1bd45b5eed39f2ada86ca8569f5fa071d767a1f77919aa0f112df`
- Created at: `2026-10-03T06:35:14.906363Z`

## Reasons

- `ACCEPTANCE_EVIDENCE_PENDING` — Execution or required Evidence is not complete enough for a verdict\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| restore | qixu\.m7\.restore | RESOLVED | 7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | FAIL | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| restore\-0 | ASSERTION | PASS | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"\} |
| restore\-1 | ASSERTION | PASS | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-2 | ASSERTION | PASS | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-3 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-4 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal\_after\_restart","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-5 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/world/original\_notification\_observed","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-6 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/world/witness\_unchanged\_after\_restore","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-7 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/health\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-8 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/health\_code","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-9 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/competitor\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-10 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/receipt\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-11 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/replay\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-12 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/reservations\_after","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-13 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/restored/inbox\_after","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-14 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-15 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_code","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-16 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/receipt\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-17 | ASSERTION | PASS | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-18 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_health\_status","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-19 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_receipt\_equal","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-20 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_ledger\_equal","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| restore\-21 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da","kind":"EVIDENCE","path":"/facts/missing\_journal/journal\_existed\_before\_removal","requirement\_id":"restore","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |

## Evidence

- `qixu.m7.restore` — `7f3bdd346447c381a4fa225caa2f4072b936c337e5824ebdd2b632bced3de9da` (1705 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-restore","source_ref":"github:NoctilumeDev/Qixu","version":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"}`
- Question: `Does a restored database or missing independent ledger quarantine authority rather than silently replay committed rights?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-restore-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-restore"],"level":"L2_CONTRACT","owner":"Qixu F12 single-instance recovery"}`

## Reproduction and cleanup

1. Use exact clean source and its original native0\.18/Plan6 PASS Bundle with fresh jar\.
2. Run this collector with the pinned Windows Java/MySQL binaries; it creates a new owned data directory and schema, seals before initialization, captures T0 and independent T1 witness, stops owned processes, restores T0 and probes quarantine; restores a current T1, independently proves complete ledger equality, then removes only the ledger and probes quarantine\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
