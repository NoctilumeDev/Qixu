# VeriTrail acceptance `m5-browser-remeasure-a55953076fd24982b9f90f3756521a1b`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m5-fixed-browser-remeasurement@1`
- Plan SHA-256: `e1983d86bf3118c9c0949a5e38102171e5862156fb491a78c3a81ebd1486a6ff`
- Created at: `2026-10-03T01:49:09.804579Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| captures | qixu\.browser\.remeasurement | RESOLVED | ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| source | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":"07ece2f2b936ac9ee1649bc11dd73eff7e4d005b"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"07ece2f2b936ac9ee1649bc11dd73eff7e4d005b"\} |
| clean | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| immutable\-raw | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/all\_original\_bytes\_unchanged","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| cookie\-other\-document | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/cookie\-other\-document/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| event\-published | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/event\-published/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| repair\-closed | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/repair\-closed/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| repair\-missing\-fact | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/repair\-missing\-fact/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| repair\-work\-done | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/repair\-work\-done/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-event\-canceled | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-event\-canceled/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-event\-confirmed | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-event\-confirmed/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-favorites | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-favorites/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-inbox | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-inbox/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-map\-context | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-map\-context/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-report\-photo | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-report\-photo/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-report\-resolved | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-report\-resolved/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-result | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-result/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-short\-canceled | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-short\-canceled/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| student\-space | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/student\-space/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| teacher\-no\-admin | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/teacher\-no\-admin/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| teacher\-venue\-approved | ASSERTION | PASS | \{"evidence\_sha256":"ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6","kind":"EVIDENCE","path":"/facts/captures/teacher\-venue\-approved/accepted","requirement\_id":"captures","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.browser.remeasurement` — `ccf4c86c980469cb6df77e5fac936c824b1e04e518e624fe37f534be11b816c6` (6255 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m5-fixed-browser-captures","source_ref":"github:NoctilumeDev/Qixu","version":"07ece2f2b936ac9ee1649bc11dd73eff7e4d005b"}`
- Question: `Do unchanged captures collected under original presealed M5 Plan support declared page states after correcting two unsupported literal text expectations? This is retained-capture remeasurement, not a fresh browser execution.`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:browser-observer","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M5 retained installed page state evidence"],"level":"L2_CONTRACT","owner":"Qixu minimal observer literal correction"}`

## Reproduction and cleanup

1. Verify original FAIL, original sealed\-before\-capture timing and every original raw byte digest\.
2. Keep fifteen original page\-state measurements; remeasure missing\-recovery\-fact refusal plus preserved draft and teacher\-only denial from actual DOM\.
3. Do not edit product behavior or claim fresh browser execution; independently read SQL and inspect images\.

Cleanup:

1. Retain original FAIL unchanged\.
