# VeriTrail acceptance `m3-live-a9c15c1bd9d44879aacc737c3be57bf4`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m3-live@1`
- Plan SHA-256: `a0ab039c213a44a3fa4fe1773defc305897ec20e39685ca3d3f200240b2806b4`
- Created at: `2026-10-02T20:09:15.952846Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| live | qixu\.live\.batch | RESOLVED | 88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"fdbfae8f2820a3330553a0d76a2485c4b5e6fab2"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"fdbfae8f2820a3330553a0d76a2485c4b5e6fab2"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| installed\-jar | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/jar\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"cac14a8e1063827062cecdf67b3e68e92f6ad69d7499c3bf35204896253b7b8d"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"cac14a8e1063827062cecdf67b3e68e92f6ad69d7499c3bf35204896253b7b8d"\} |
| full\-result | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/database/formal\_results","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":1\} |
| all\-outcomes | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/database/outcomes","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| offers | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/database/offers","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| all\-notifications | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/database/result\_notices","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| independent\-reproduction | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/reproduction/independent\_bytes\_equal","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| actual\-bls | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/reproduction/signature\_verified","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| maximum | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/reproduction/maximum","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| owned\-runtime\-stopped | ASSERTION | PASS | \{"evidence\_sha256":"88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23","kind":"EVIDENCE","path":"/facts/cleanup/stopped","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.live.batch` — `88b08fbcd46608176dc389b346286d67c91c996bae4bb24760f169ce0020de23` (5456 bytes, redactions: 0)

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
