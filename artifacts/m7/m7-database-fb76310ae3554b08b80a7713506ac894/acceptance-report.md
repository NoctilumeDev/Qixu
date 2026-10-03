# VeriTrail acceptance `m7-database-fb76310ae3554b08b80a7713506ac894`

- Execution status: `COMPLETED`
- Verdict: `PASS`
- Plan: `qixu-m7-database@4`
- Plan SHA-256: `6bfec247fc5a6a89e33ba4596ba4abd917d5b5a8b3fe9a539653942959e78593`
- Created at: `2026-10-03T06:34:01.708963Z`

## Reasons

- `ACCEPTANCE_PASS` — All required Evidence and decisive rules pass\.

## Evidence bindings

| Requirement | Type | Status | Evidence SHA-256 |
| --- | --- | --- | --- |
| database | qixu\.m7\.database | RESOLVED | c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1 |

## Deterministic rules

| ID | Category | Status | Left | Right |
| --- | --- | --- | --- | --- |
| complete | SUFFICIENCY | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/metadata/veritrail\_observation/coverage","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"COMPLETE"\} |
| database\-0 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/source\_sha","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"\} |
| database\-1 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/source\_clean","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-2 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/producer/bytes\_checked","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-3 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/normal/create\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-4 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/normal/receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-5 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/normal/unique\_sql","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-6 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/wait\_observed","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-7 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/response\_within\_15s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-8 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-9 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-10 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/no\_business\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-11 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/no\_marker\_while\_guard\_held","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-12 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/retry\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-13 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/retry\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-14 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/lock/unique\_effect","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-15 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/borrowed\_flow\_hit","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-16 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/target\_query\_hit","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-17 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/response\_within\_35s","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-18 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":503\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":503\} |
| database\-19 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/code","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":"DATABASE\_UNAVAILABLE"\} |
| database\-20 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/no\_mutation","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-21 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/recovery\_read\_status","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":200\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":200\} |
| database\-22 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/io/recovery\_receipt\_equal","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |
| database\-23 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/configuration/app\_lock\_wait\_values","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":\[10\]\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":\[10\]\} |
| database\-24 | ASSERTION | PASS | \{"evidence\_sha256":"c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1","kind":"EVIDENCE","path":"/facts/cleanup/all\_owned\_stopped","requirement\_id":"database","resolution":"RESOLVED","resolved":true,"value":true\} | \{"evidence\_sha256":null,"kind":"LITERAL","path":null,"requirement\_id":null,"resolution":"RESOLVED","resolved":true,"value":true\} |

## Evidence

- `qixu.m7.database` — `c874acb8a672f4ce8e421d66f25cfc8e9f86ee94549cb2ad1bd6cd160902adc1` (4186 bytes, redactions: 0)

## Declared boundary

- Subject: `{"id":"qixu-m7-database","source_ref":"github:NoctilumeDev/Qixu","version":"bfd37f03c8eaa69e69d1429f0eed82e60e1df340"}`
- Question: `Do owned resource waits and borrowed network reads end within frozen observation budgets without duplicate business authority?`
- Governance: `{"claim_owner_ref":"human:repository-owner","drafter_ref":"qixu:m7-database-collector","seal_authority_ref":"human:repository-owner:authorized-engineering-goal","seal_decision":"CONFIRMED"}`
- Resource budget: `{"command_timeout_seconds":600,"max_artifact_bytes":2097152}`
- Change scope: `{"consumers":["M7-database"],"level":"L2_CONTRACT","owner":"Qixu F15 owned database stalls"}`

## Reproduction and cleanup

1. Fresh exact native0\.18/Plan6 producer\. Seal before own MySQL and opaque loopback relay\. Normal booking; real floor lock wait with15s observation then release/same\-key replay; drop only replies on existing borrowed connection with35s observation, then remove fault and verify authoritative reads\.

Cleanup:

1. Only verified Popen child handles, exe and data\-directory/jar coordinates may be stopped\.
2. Retain all dump, private packets, world witness and immutable Bundle; no host database or other service is touched\.
