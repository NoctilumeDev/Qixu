# VeriTrail acceptance `m9-engineering-bc4fdd5dfe7442aaaa4b4a1719013c95`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m9-engineering@2`
- Plan SHA-256: `05bfd1f401f0f1a16d5a9c5599b0040ae71bdf3c05abfd23359fd756592f10da`
- Created at: `2026-10-04T02:48:10.762255Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| engineering | qixu\.engineering\.observation | RESOLVED | 53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| source | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":"f0ef8e0523b4441fbd710796f75d881f2476e4f1"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"f0ef8e0523b4441fbd710796f75d881f2476e4f1"\} |
| distinct | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/distinct","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-fresh\_public | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/fresh\_public","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-bytes\_checked | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/bytes\_checked","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-native\_181 | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/native\_181","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-frontend\_61 | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/frontend\_61","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-future\_batch | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/future\_batch","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-all\_pages | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/all\_pages","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-sql\_supported | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/sql\_supported","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-same\_db\_restart | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/same\_db\_restart","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-1\-owned\_cleanup | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/0/owned\_cleanup","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-fresh\_public | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/fresh\_public","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-bytes\_checked | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/bytes\_checked","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-native\_181 | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/native\_181","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-frontend\_61 | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/frontend\_61","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-future\_batch | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/future\_batch","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-all\_pages | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/all\_pages","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-sql\_supported | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/sql\_supported","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-same\_db\_restart | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/same\_db\_restart","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| run\-2\-owned\_cleanup | ASSERTION | PASS | \{"evidence\_sha256":"53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589","kind":"EVIDENCE","path":"/facts/runs/1/owned\_cleanup","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.engineering.observation` — `53b690d5313878db45d2bbf33732b63c7e6859f30a33d558bec6eb6527028589` (5483 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m9","source_ref":"github:NoctilumeDev/Qixu","version":"f0ef8e0523b4441fbd710796f75d881f2476e4f1"}`
- Question: `Can two fresh public checkouts independently rebuild, execute the bounded demo and retain actual UI/SQL through a normal same-database restart?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:engineering-adapter","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":3600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["engineering-candidate"],"level":"L2_CONTRACT","owner":"Qixu M9"}`

## Reproduction and cleanup

1. Seal before two serial public GitHub clones; build with locked dependencies and owned MySQL8\.0\.44\.
2. Use child native/frontend/live sealed Bundles; operate seven declared actual CUA pages and save raw triplets\.
3. Stop the app, restart the same DB/journal normally, read health and authenticated own reservations; Core checks all child bytes and facts\.

Cleanup:

1. Stop only retained Popen handles after executable/arguments ownership checks; do not adopt shared ports\.
2. Keep immutable first failures, Bundles and captures; remove only rebuildable instances after public qualification\.
