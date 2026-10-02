# VeriTrail acceptance `m1-612a18385f7c4b27a395cf2d74ac846c`

- Execution status: `COMPLETED`
- Verdict: `FAIL`
- Plan: `qixu-native-m1@1`
- Plan SHA-256: `c1e1885c88d238e09423a00b1ea5046a97e9359843a56033015fb7a340559901`
- Created at: `2026-10-02T18:21:41.084413Z`

## Reasons

- `ACCEPTANCE_ASSERTION_FAILED` — At least one decisive assertion is false\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| native | qixu\.native\.observation | RESOLVED | 985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| collected | SUFFICIENCY | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":"c95d626c8bc1bebce1e67528a7d92a6de4fc7506"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"c95d626c8bc1bebce1e67528a7d92a6de4fc7506"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| execution\-success | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/command\_exit","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":1\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| case\-0 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.bearerStoredAsDigestAndAuditContainsNoCredentials","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-1 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.cookieSessionNeedsBoundCsrfAndLogoutOnlyRevokesOneSession","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-2 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.originAndMalformedAuthorizationDoNotFallBackToCookie","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-3 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.expiryActiveFlagAndAuthVersionAreRechecked","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-4 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.claimedRoleDoesNotGrantAuthorityAndAdminFloorScopeIsEnforced","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-5 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.invalidPasswordAttemptsSurviveRollbackAndResetOnlyAfterWindow","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-6 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.unknownAccountAndInactiveIdentityUseSameRejection","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-7 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.publicProfilesDoNotLeakOccupantAndFiltersAreLiteral","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-8 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.inputAndMissingResourceErrorsHaveSafeStableEnvelopes","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-9 | ASSERTION | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.migrationAndIsolationAreRealMysqlAndSchemaPrivilegesAreContained","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-10 | ASSERTION | FAIL | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.databaseRejectsCrossFloorParentAndInvalidGeometry","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":false\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-11 | ASSERTION | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.externalIdentityNamesAndIdsCannotCollideAcrossProviders","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-12 | ASSERTION | PASS | \{"evidence\_sha256":"985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.demoCannotSeedWithoutExplicitProfileOrInProduction","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.native.observation` — `985ff9025e7b206586229a8a06eb1d25595d107092c3fae666f6290e4721ba83` (3308 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m1","source_ref":"github:NoctilumeDev/Qixu","version":"c95d626c8bc1bebce1e67528a7d92a6de4fc7506"}`
- Question: `Do the declared native stage witnesses pass at the exact clean coordinate with real HTTP and dedicated MySQL?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:native-adapter","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":900,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["native-stage-gates"],"level":"L2_CONTRACT","owner":"Qixu native stage"}`

## Reproduction and cleanup

1. Use an exact clean source and Core0\.13\.0\.
2. Provision a dedicated qixu\_test or qixu\_ci schema and schema\-scoped account; supply its test environment\.
3. Run the adapter; it seals before Maven clean verify and records every required case\.

Cleanup:

1. The test server stops with the test JVM; only dedicated test data is reset\.
2. Retain the immutable Bundle and private raw logs; do not stop shared MySQL\.
