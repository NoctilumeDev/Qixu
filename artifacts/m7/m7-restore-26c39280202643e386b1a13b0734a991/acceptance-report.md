# VeriTrail acceptance `m7-restore-26c39280202643e386b1a13b0734a991`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m7-restore@4`
- Plan SHA-256: `2839b044e9ed8a08ad401533a37508fafed2ee928c748ea84c62d164ab92a9e8`
- Created at: `2026-10-03T05:36:40.215408Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| restore | qixu\.m7\.restore | RESOLVED | 4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| restore\-0 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"ec31a60f475d14835d37958a668513deaf094010"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"ec31a60f475d14835d37958a668513deaf094010"\} |
| restore\-1 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-2 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-3 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| restore\-4 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal\_after\_restart","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-5 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/world/original\_notification\_observed","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-6 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/world/witness\_unchanged\_after\_restore","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-7 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-8 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-9 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/competitor\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-10 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-11 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/replay\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-12 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/reservations\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-13 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/restored/inbox\_after","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| restore\-14 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-15 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/health\_code","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"NOT\_RECONCILED"\} |
| restore\-16 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/receipt\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| restore\-17 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-18 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_health\_status","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| restore\-19 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_receipt\_equal","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-20 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/consistent\_ledger\_equal","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| restore\-21 | ASSERTION | PASS | \{"evidence\_sha256":"4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed","kind":"EVIDENCE","path":"/facts/missing\_journal/journal\_existed\_before\_removal","requirement\_id":"restore","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.restore` — `4c31ab874c223ae42785bbd6f7621e498d9a1e71d6037f890638e3ddb04600ed` (4240 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-restore","source_ref":"github:NoctilumeDev/Qixu","version":"ec31a60f475d14835d37958a668513deaf094010"}`
- Question: `Does a restored database or missing independent ledger quarantine authority rather than silently replay committed rights?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-restore-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-restore"],"level":"L2_CONTRACT","owner":"Qixu F12 single-instance recovery"}`

## Reproduction and cleanup

1. Use exact clean source and its original native0\.16/Plan4 PASS Bundle with fresh jar\.
2. Run this collector with the pinned Windows Java/MySQL binaries; it creates a new owned data directory and schema, seals before initialization, captures T0 and independent T1 witness, stops owned processes, restores T0 and probes quarantine; restores a current T1, independently proves complete ledger equality, then removes only the ledger and probes quarantine\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
