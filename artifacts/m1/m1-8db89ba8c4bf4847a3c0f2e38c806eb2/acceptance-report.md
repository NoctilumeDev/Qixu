# VeriTrail acceptance `m1-8db89ba8c4bf4847a3c0f2e38c806eb2`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-native-m1@1`
- Plan SHA-256: `9b7c024ff25b7f02c6fc847acdea7010337e72a41dcea23106b73d56a44de5b3`
- Created at: `2026-10-02T18:24:35.311917Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| native | qixu\.native\.observation | RESOLVED | d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| collected | SUFFICIENCY | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| exact\-source | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":"2bb7e94d802b4bb2d0a06e45c41ed038f9483864"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"2bb7e94d802b4bb2d0a06e45c41ed038f9483864"\} |
| clean\-source | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| execution\-success | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/command\_exit","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":0\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":0\} |
| case\-0 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.bearerStoredAsDigestAndAuditContainsNoCredentials","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-1 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.cookieSessionNeedsBoundCsrfAndLogoutOnlyRevokesOneSession","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-2 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.originAndMalformedAuthorizationDoNotFallBackToCookie","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-3 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.expiryActiveFlagAndAuthVersionAreRechecked","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-4 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.claimedRoleDoesNotGrantAuthorityAndAdminFloorScopeIsEnforced","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-5 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.invalidPasswordAttemptsSurviveRollbackAndResetOnlyAfterWindow","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-6 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.unknownAccountAndInactiveIdentityUseSameRejection","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-7 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.publicProfilesDoNotLeakOccupantAndFiltersAreLiteral","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-8 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.inputAndMissingResourceErrorsHaveSafeStableEnvelopes","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-9 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.migrationAndIsolationAreRealMysqlAndSchemaPrivilegesAreContained","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-10 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.databaseRejectsCrossFloorParentAndInvalidGeometry","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-11 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.externalIdentityNamesAndIdsCannotCollideAcrossProviders","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| case\-12 | ASSERTION | PASS | \{"evidence\_sha256":"d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8","kind":"EVIDENCE","path":"/facts/tests/dev\.noctilume\.qixu\.FoundationIT\.demoCannotSeedWithoutExplicitProfileOrInProduction","requirement\_id":"native","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.native.observation` — `d99776aed9b5139e6a644ee7d19e11e757882290584b9c4d3cc1c547e6feeaf8` (3298 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m1","source_ref":"github:NoctilumeDev/Qixu","version":"2bb7e94d802b4bb2d0a06e45c41ed038f9483864"}`
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
