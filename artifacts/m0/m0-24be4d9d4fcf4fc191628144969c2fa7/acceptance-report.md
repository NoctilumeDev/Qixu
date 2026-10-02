# VeriTrail acceptance `m0-24be4d9d4fcf4fc191628144969c2fa7`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m0-documents@1`
- Plan SHA-256: `c5e2460e3bbba43c30ba54eab20e171863ae7c17d7ccdd7d1acc698b4dd3c694`
- Created at: `2026-10-02T17:51:59.881752Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| docs\-evidence | qixu\.documents\.observation | RESOLVED | d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete\-capture | SUFFICIENCY | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":"d2274ef9c35dc662024cba64afcccd03aefc014d"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"d2274ef9c35dc662024cba64afcccd03aefc014d"\} |
| source\-clean | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| required\-documents | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/required\_file\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":13\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":13\} |
| observed\-documents | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/observed\_file\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":13\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":13\} |
| no\-document\-errors | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/error\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| command\-completed | ASSERTION | PASS | \{"evidence\_sha256":"d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de","kind":"EVIDENCE","path":"/facts/command\_exit","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |

## Evidence

- `qixu.documents.observation` — `d1418cde2ddd9c1d87edad2d6db648c5aa06cb2911aa54a71ba4c9ab339600de` (2253 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m0","source_ref":"github:NoctilumeDev/Qixu","version":"d2274ef9c35dc662024cba64afcccd03aefc014d"}`
- Question: `Are the declared M0 documents present, linked, and observed at the exact clean source coordinate?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m0-adapter","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":30,"max_artifact_bytes":1048576}`
- Change scope: `{"consumers":["document-gates"],"level":"L2_CONTRACT","owner":"Qixu M0"}`

## Reproduction and cleanup

1. Install the public Core 0\.13\.0 wheel with its documented digest\.
2. Run this adapter from an exact clean Qixu checkout with a fresh observation identity\.

Cleanup:

1. Retain this immutable bundle for review; no services or databases are created\.
