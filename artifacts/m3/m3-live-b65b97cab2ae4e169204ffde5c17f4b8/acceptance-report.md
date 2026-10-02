# VeriTrail acceptance `m3-live-b65b97cab2ae4e169204ffde5c17f4b8`

- Execution status: `ERROR`
- Verdict: `PENDING`
- Plan: `qixu-m3-live@1`
- Plan SHA-256: `a0ab039c213a44a3fa4fe1773defc305897ec20e39685ca3d3f200240b2806b4`
- Created at: `2026-10-02T20:06:30.330936Z`

## Reasons

- `ACCEPTANCE_EVIDENCE_PENDING` — Execution or required Evidence is not complete enough for a verdict\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| live | qixu\.live\.batch | RESOLVED | 509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | FAIL | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"fdbfae8f2820a3330553a0d76a2485c4b5e6fab2"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"fdbfae8f2820a3330553a0d76a2485c4b5e6fab2"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| installed\-jar | ASSERTION | PASS | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/jar\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"cac14a8e1063827062cecdf67b3e68e92f6ad69d7499c3bf35204896253b7b8d"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"cac14a8e1063827062cecdf67b3e68e92f6ad69d7499c3bf35204896253b7b8d"\} |
| full\-result | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/database/formal\_results","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| all\-outcomes | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/database/outcomes","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| offers | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/database/offers","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| all\-notifications | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/database/result\_notices","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| independent\-reproduction | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/reproduction/independent\_bytes\_equal","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| actual\-bls | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/reproduction/signature\_verified","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| maximum | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/reproduction/maximum","requirement\_id":"live","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| owned\-runtime\-stopped | ASSERTION | PASS | \{"evidence\_sha256":"509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d","kind":"EVIDENCE","path":"/facts/cleanup/stopped","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.live.batch` — `509a5d49057570aefc113baeec72bf40734ecf55ed8c420bf6a65ae4a3141a7d` (1478 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m3-live","source_ref":"github:NoctilumeDev/Qixu","version":"fdbfae8f2820a3330553a0d76a2485c4b5e6fab2"}`
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
