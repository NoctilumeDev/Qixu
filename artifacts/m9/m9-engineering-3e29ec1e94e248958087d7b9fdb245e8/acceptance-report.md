# VeriTrail acceptance `m9-engineering-3e29ec1e94e248958087d7b9fdb245e8`

- Execution status: `ERROR`
- Verdict: `PENDING`
- Plan: `qixu-m9-engineering@1`
- Plan SHA-256: `db48f135558db5e8d448b899c40d5bdd5042c6c49beace040868012d669b9da8`
- Created at: `2026-10-04T01:22:45.452014Z`

## Reasons

- `ACCEPTANCE_EVIDENCE_PENDING` — Execution or required Evidence is not complete enough for a verdict\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| engineering | qixu\.engineering\.observation | RESOLVED | ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | FAIL | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":"ERROR"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| source | ASSERTION | PASS | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"engineering","resolution":"RESOLVED","resolved":true,"value":"12bdf5d3f1794058385d374f445b5e83fbb263e0"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"12bdf5d3f1794058385d374f445b5e83fbb263e0"\} |
| distinct | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/distinct","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-fresh\_public | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/fresh\_public","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-bytes\_checked | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/bytes\_checked","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-native\_181 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/native\_181","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-frontend\_61 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/frontend\_61","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-future\_batch | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/future\_batch","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-all\_pages | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/all\_pages","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-sql\_supported | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/sql\_supported","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-same\_db\_restart | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/same\_db\_restart","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-1\-owned\_cleanup | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/0/owned\_cleanup","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-fresh\_public | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/fresh\_public","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-bytes\_checked | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/bytes\_checked","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-native\_181 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/native\_181","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-frontend\_61 | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/frontend\_61","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-future\_batch | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/future\_batch","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-all\_pages | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/all\_pages","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-sql\_supported | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/sql\_supported","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-same\_db\_restart | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/same\_db\_restart","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |
| run\-2\-owned\_cleanup | ASSERTION | NOT\_EVALUATED | \{"evidence\_sha256":"ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c","kind":"EVIDENCE","path":"/facts/runs/1/owned\_cleanup","requirement\_id":"engineering","resolution":"MISSING\_PATH","resolved":false,"value":null\} | \{"evidence\_sha256":null,"kind":"NONE","path":null,"requirement\_id":null,"resolution":"NOT\_APPLICABLE","resolved":false,"value":null\} |

## Evidence

- `qixu.engineering.observation` — `ab518eb0fb400ed10d1dbc06f5904e3b36b8c8c05f2959269d6381d094f60f6c` (1048 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m9","source_ref":"github:NoctilumeDev/Qixu","version":"12bdf5d3f1794058385d374f445b5e83fbb263e0"}`
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
