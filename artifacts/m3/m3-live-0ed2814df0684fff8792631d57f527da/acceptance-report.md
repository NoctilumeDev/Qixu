# VeriTrail acceptance `m3-live-0ed2814df0684fff8792631d57f527da`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m3-live@3`
- Plan SHA-256: `7a931b874586080f75b09f6c68ff904ce12f24fa4c9f6e339b8fa054e2bf1ece`
- Created at: `2026-10-02T20:29:21.511240Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| live | qixu\.live\.batch | RESOLVED | ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| producer\-bytes | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| producer\-manifest | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/producer/manifest\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"997b82d2e33b813267f0dd8ffd74c83ef85b231aac800df77754885a7577aaae"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"997b82d2e33b813267f0dd8ffd74c83ef85b231aac800df77754885a7577aaae"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"ca09396746573c0cc4a6979a30013814320c654a"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"ca09396746573c0cc4a6979a30013814320c654a"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| installed\-jar | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/jar\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"2460e7212cb0235910e009b385402e8593e3671679af2702ebdace3f52842dbf"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"2460e7212cb0235910e009b385402e8593e3671679af2702ebdace3f52842dbf"\} |
| full\-result | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/database/formal\_results","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":1\} |
| all\-outcomes | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/database/outcomes","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| offers | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/database/offers","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| all\-notifications | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/database/result\_notices","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| independent\-reproduction | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/reproduction/independent\_bytes\_equal","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| actual\-bls | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/reproduction/signature\_verified","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| maximum | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/reproduction/maximum","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| owned\-runtime\-stopped | ASSERTION | PASS | \{"evidence\_sha256":"ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad","kind":"EVIDENCE","path":"/facts/cleanup/stopped","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.live.batch` — `ad210500463a6a5be0e00dd9c86224b5ed4d572112b78b2b360095719d5f65ad` (5638 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m3-live","source_ref":"github:NoctilumeDev/Qixu","version":"ca09396746573c0cc4a6979a30013814320c654a"}`
- Question: `Does this exact installed candidate freeze before a fixed future beacon, publish one full batch and permit independent public-byte reproduction with real BLS?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:live-adapter","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":420,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["m3-native-stage"],"level":"L2_CONTRACT","owner":"Qixu live batch"}`

## Reproduction and cleanup

1. Use the exact clean source and jar with a schema\-scoped qixu demo DB and fixed official verifier dependencies\.
2. Publish explicit future close/freeze/round/result deadlines before submitting two hard\-constrained applicants\.
3. Freeze before the fixed round; fetch that exact round, publish, download the public packet and independently verify all bytes/BLS\.

Cleanup:

1. Stop only the java child created and retained by this probe; preserve other ports and shared MySQL\.
2. Retain demonstration batch/history and the immutable packet and Bundle\.
