# VeriTrail acceptance `m0-4013f83410a143de99ec7029098f5cc7`

- Execution status: `COMPLETED`
- Verdict: `FAIL`
- Plan: `qixu-m0-documents@1`
- Plan SHA-256: `8537fb40e6dcc06dfa9d06148a2519c9bc5921b8f0e78dd155f5cc8d6ac6c4fc`
- Created at: `2026-10-02T17:52:00.653010Z`

## Reasons

- `ACCEPTANCE_ASSERTION_FAILED` — At least one decisive assertion is false\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| docs\-evidence | qixu\.documents\.observation | RESOLVED | 58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete\-capture | SUFFICIENCY | PASS | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":"d2274ef9c35dc662024cba64afcccd03aefc014d"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"d2274ef9c35dc662024cba64afcccd03aefc014d"\} |
| source\-clean | ASSERTION | PASS | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| required\-documents | ASSERTION | PASS | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/required\_file\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":13\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":13\} |
| observed\-documents | ASSERTION | FAIL | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/observed\_file\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":12\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":13\} |
| no\-document\-errors | ASSERTION | FAIL | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/error\_count","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":2\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| command\-completed | ASSERTION | FAIL | \{"evidence\_sha256":"58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec","kind":"EVIDENCE","path":"/facts/command\_exit","requirement\_id":"docs\-evidence","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |

## Evidence

- `qixu.documents.observation` — `58a9401d68f926305f81be574241cf28c7df2e38077876ada2e4fffa957d4fec` (2257 bytes, redactions: 0)

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
