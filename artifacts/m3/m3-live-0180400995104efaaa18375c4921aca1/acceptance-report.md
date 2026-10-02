# VeriTrail acceptance `m3-live-0180400995104efaaa18375c4921aca1`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m3-live@3`
- Plan SHA-256: `30d9c8267c49f7c0b8bafe5bafb42a0499d46d1a46e246e65f12b0aee2720aba`
- Created at: `2026-10-02T20:21:36.660631Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| live | qixu\.live\.batch | RESOLVED | d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| producer\-bytes | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| producer\-manifest | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/producer/manifest\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"04ea03ae3e777f253d6d59952d88b82daad21ba1f3232667592cac6774c1dec5"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"04ea03ae3e777f253d6d59952d88b82daad21ba1f3232667592cac6774c1dec5"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"f02c0c91188ad6c91e4660df9437cb6282e7bdae"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"f02c0c91188ad6c91e4660df9437cb6282e7bdae"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| installed\-jar | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/jar\_sha256","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":"55916f644402a26f7c8d35ad099edb368305904a40ee24e41f4f1f43b1220ebb"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"55916f644402a26f7c8d35ad099edb368305904a40ee24e41f4f1f43b1220ebb"\} |
| full\-result | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/database/formal\_results","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":1\} |
| all\-outcomes | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/database/outcomes","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| offers | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/database/offers","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| all\-notifications | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/database/result\_notices","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| independent\-reproduction | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/reproduction/independent\_bytes\_equal","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| actual\-bls | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/reproduction/signature\_verified","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| maximum | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/reproduction/maximum","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":2\} |
| owned\-runtime\-stopped | ASSERTION | PASS | \{"evidence\_sha256":"d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39","kind":"EVIDENCE","path":"/facts/cleanup/stopped","requirement\_id":"live","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.live.batch` — `d795ab29c202fe071af947d77b919e395cd1906862656594741ff05a732d9a39` (5634 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m3-live","source_ref":"github:NoctilumeDev/Qixu","version":"f02c0c91188ad6c91e4660df9437cb6282e7bdae"}`
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
