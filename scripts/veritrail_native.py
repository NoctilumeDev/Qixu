"""Sealed native-stage observation through VeriTrail Core, not Starter hosting.

The native collector runs real MySQL/HTTP tests. Core consumes bounded facts;
it does not acquire DB lifecycle authority or prove production capacity.
"""
from __future__ import annotations
import argparse
import hashlib
import time
from datetime import datetime, timezone
from importlib.metadata import version
import json
from pathlib import Path
import subprocess
import sys
import uuid
import xml.etree.ElementTree as ET
from veritrail.acceptance_plan import observation_spec_digest, seal_acceptance_plan
from veritrail.acceptance_reporting import create_acceptance_bundle
from veritrail.canonical import sha256_json

ROOT = Path(__file__).resolve().parents[1]
FOUNDATION_CASES = [
    "bearerStoredAsDigestAndAuditContainsNoCredentials",
    "cookieSessionNeedsBoundCsrfAndLogoutOnlyRevokesOneSession",
    "originAndMalformedAuthorizationDoNotFallBackToCookie",
    "expiryActiveFlagAndAuthVersionAreRechecked",
    "claimedRoleDoesNotGrantAuthorityAndAdminFloorScopeIsEnforced",
    "invalidPasswordAttemptsSurviveRollbackAndResetOnlyAfterWindow",
    "unknownAccountAndInactiveIdentityUseSameRejection",
    "publicProfilesDoNotLeakOccupantAndFiltersAreLiteral",
    "inputAndMissingResourceErrorsHaveSafeStableEnvelopes",
    "migrationAndIsolationAreRealMysqlAndSchemaPrivilegesAreContained",
    "databaseRejectsCrossFloorParentAndInvalidGeometry",
    "externalIdentityNamesAndIdsCannotCollideAcrossProviders",
    "demoCannotSeedWithoutExplicitProfileOrInProduction",
    "initializationAndDrainDoNotAdvertiseFalseReadiness",
]
HTTP_STATUSES = {
    FOUNDATION_CASES[0]: [200, 200],
    FOUNDATION_CASES[1]: [200, 200, 403, 403, 200, 401, 200],
    FOUNDATION_CASES[2]: [200, 401, 403, 204],
    FOUNDATION_CASES[3]: [200, 401, 200, 401, 200, 401],
    FOUNDATION_CASES[4]: [200, 403, 200, 403, 200, 200, 403, 200, 200],
    FOUNDATION_CASES[5]: [401, 401, 401, 401, 401, 429, 200],
    FOUNDATION_CASES[6]: [401, 401],
    FOUNDATION_CASES[7]: [200, 200, 200, 200, 200],
    FOUNDATION_CASES[8]: [200, 422, 422, 422, 422, 422, 404, 404, 422],
    FOUNDATION_CASES[13]: [503, 503, 200, 200],
}
DATABASE_MEASURES = {
    FOUNDATION_CASES[0]: {"hashedSessionRows": 1, "rawTokenRows": 0, "rawCredentialAuditRows": 0},
    FOUNDATION_CASES[5]: {"persistedFailures": 5, "resetFailures": 0},
    FOUNDATION_CASES[6]: {"createdSessions": 0},
    FOUNDATION_CASES[7]: {"windowSeatMatches": 8, "injectedQueryMatches": 0},
    FOUNDATION_CASES[9]: {"successfulV1Migrations": 1, "transactionIsolation": "READ-COMMITTED", "outsideSchemaGrants": 0},
    FOUNDATION_CASES[10]: {"mapXAfterRejectedWrites": 65, "capacityAfterRejectedWrites": 1},
    FOUNDATION_CASES[11]: {"sameSubjectDistinctProviderRows": 2, "localRoleAfterExternalBinding": "STUDENT"},
    FOUNDATION_CASES[13]: {"sessionsBeforeReadiness": 0},
}
BOOKING_MEASURES = {
    "sameSeatAndCrossFloorPersonalConflictsSerialize": {"sameSeatRights": 1, "personalRights": 1},
    "sameKeyRecoversCommittedResultAndRejectsChangedBody": {"rights": 1, "receipts": 1},
    "adjacentIntervalsAndDeadlineUseCurrentServerTime": {"checkedInAfterOldSweep": "CHECKED_IN"},
    "lockWaitRechecksSessionAndClockBeforeWriting": {"createdRights": 0, "rightsAfterLogout": 0},
    "areaApprovalAndSeatBookingCannotBothCommit": {"exclusiveEffects": 1},
    "pendingVenueDoesNotBlockAndParallelApprovalsSerialize": {"venueRights": 1},
    "eventRequiresApprovedVenueAndCannotBypassOwnership": {"publishedEvents": 1},
    "finalEventSlotAndCancellationPreserveFixedQueue": {"confirmedAtCapacity": 1, "waitingAtCapacity": 1, "newSequence": 3, "countAfterPromotion": 1},
    "eventRebindingKeepsParticipationAndOutboxRetriesDeduplicate": {"participationAfterMove": "CONFIRMED", "activeVenueAfterMove": 1, "inboxDuplicates": 0, "pendingNotifications": 0, "participationAfterCancel": "EVENT_CANCELED", "activeVenueAfterCancel": 0},
    "notificationFailureRollsBackEffectAndSameKeyCanRecover": {"rightsAfterFailure": 0, "receiptsAfterFailure": 0, "rightsAfterRecovery": 1, "notificationEvents": 1},
    "invalidWindowsAndForeignActionsHaveNoEffects": {"rightsAfterInvalid": 0, "statusAfterForeignWrites": "PENDING"},
    "lostConfirmationResponseAfterCommitRecoversPastDeadline": {"rightAfterResponseLoss": "CHECKED_IN", "confirmReceipts": 1, "rights": 1},
    "missingReceiptDuringFlightIsNotProofOfNoCommit": {"rights": 1, "receipts": 1},
}
PREPARATION_MEASURES = {
    "roleScopeAndPoolRightsAreNotCreationPrivileges": {"batches": 1, "shorts": 0},
    "immutableVersionsFreezeExactWinnerAndRejectOfflineOverwrite": {"versions": 2, "frozenRevision": 2},
    "crossBatchConcurrentApplicationsHaveOneIntervalFact": {"applications": 1},
    "publicPacketIsCompleteReproducibleAndContainsNoIdentityMap": {"maximum": 2, "outcomes": 2, "resultNotices": 2},
    "sourceUnavailableAndResultDeadlineCannotSwitchEntropy": {"results": 0, "failedNotices": 1, "sourceCalls": 1},
    "resourceChangeInvalidatesEntireFrozenInput": {"status": "FAILED_NO_RESULT", "results": 0, "offers": 0},
    "publicationFaultRetainsCandidateButRollsBackWholeResult": {"resultsAfterRecovery": 1, "offersAfterRecovery": 2, "sourceCalls": 1, "formalRuns": 1},
    "publicationCrossingDeadlineHasNoPartialRights": {"results": 0, "offers": 0, "candidateRuns": 1},
    "allLosingApplicantsReceivePersistentResultsAndStableWaitlist": {"offers": 1, "waitlist": 1, "resultInbox": 2},
    "fallbackUpgradeIsAtomicAndDuplicateConfirmationReturnsReceipt": {"activeSeat": 2100, "activeRights": 1, "replaced": 1},
    "expiredUpgradeAndOldTaskKeepFallbackAndRejectForeignOwner": {"fallbackSeat": 2000, "openOffers": 0, "upgradeState": "EXPIRED"},
    "upgradeWriteFaultAndDeadlineNeverReleaseOldRight": {"fallbackSeat": 2000, "acceptedUpgrades": 0},
    "concurrentExitAndConfirmCannotResurrectAndCycleCloses": {"activeRights": 0, "openOffers": 0},
    "invalidManualWriteNeverObtainsCandidateAndMissedFreezeIsExplicit": {"sourceCalls": 0, "candidateRuns": 0, "missedFreezeStatus": "FAILED_NO_RESULT"},
    "residualPromotionsCannotRecreateArtificialScarcity": {"openPromotions": 2, "flexibleSeat": 2000, "constrainedSeat": 2100},
    "queuedFreezeRechecksDeadlineAndDoesNotExposeLateInput": {"frozenInputs": 0, "status": "FAILED_NO_RESULT", "failedNotices": 1},
}
PREPARATION_UNITS = {
    "AllocationGraphTest": ["flexibleFirstCannotManufactureScarcity", "allThreeByThreeGraphsAgreeWithIndependentExhaustiveOracle", "frozenOrderingIsIndependentOfInputContainerOrderAndNeverOversells", "hundredApplicantsHaveRealFallbackAndBoundedShortage", "malformedGraphAndExhaustedBudgetDoNotProduceCandidateResult"],
    "FrozenJsonTest": ["mysqlJsonKeyReorderingDoesNotChangeDomainDigestBytes"],
    "HttpRandomnessSourceTest": ["javaProcessProtocolVerifiesRetainedProofAndRejectsBadRoundAndSignature", "missingHelperIsNotAnAuthorizationToUseLocalRandomness"],
    "RandomnessClockTest": ["futureRoundUsesCeilingAndExactBoundaryIsStable"],
}


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def write_new(path: Path, value: dict) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, ensure_ascii=False, sort_keys=True, indent=2)
        stream.write("\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=["m1", "m2", "m3"], default="m1")
    parser.add_argument("--maven", default="mvn")
    args = parser.parse_args()
    if version("veritrail") != "0.13.0" or git("status", "--porcelain"):
        raise RuntimeError("Requires Core 0.13.0 and an exact clean committed source")
    sha = git("rev-parse", "HEAD")
    identity = args.stage + "-" + uuid.uuid4().hex
    output = ROOT / "artifacts/local" / identity
    output.mkdir(parents=True, exist_ok=False)
    collector = "qixu-native/0.8" if args.stage == "m3" else "qixu-native/0.6"
    request = {"source_sha": sha, "stage": args.stage, "collector": collector}
    required = ["dev.noctilume.qixu.FoundationIT." + name for name in FOUNDATION_CASES]
    if args.stage in ("m2", "m3"):
        required += ["dev.noctilume.qixu.BookingIT." + name for name in BOOKING_MEASURES]
    if args.stage == "m3":
        required += ["dev.noctilume.qixu.PreparationIT." + name for name in PREPARATION_MEASURES]
        required += ["dev.noctilume.qixu.preparation." + cls + "." + name for cls, names in PREPARATION_UNITS.items() for name in names]
    spec = {"id": "native-observation", "contract": {"id": "qixu-native", "version": collector.split("/")[1]},
            "evidence_type": "qixu.native.observation", "coordinates": request,
            "projections": ["source_sha", "source_clean", "command_exit", "tests", "observations", "package"],
            "canonicalization_profile": "veritrail-json-c14n/1"}

    def assertion(name: str, path: str, value: object) -> dict:
        return {"id": name, "severity": "HARD", "left": {"requirement_id": "native", "path": path}, "operator": "eq", "right": value}

    assertions = [assertion("exact-source", "/facts/source_sha", sha), assertion("clean-source", "/facts/source_clean", True), assertion("execution-success", "/facts/command_exit", 0)]
    if args.stage == "m3":
        assertions += [assertion("current-built-artifact", "/facts/package/current_build", True), assertion("artifact-producer-source", "/facts/package/source_sha", sha)]
    assertions += [assertion("case-" + str(i), "/facts/tests/" + name, True) for i, name in enumerate(required)]
    for case, statuses in HTTP_STATUSES.items():
        for i, status in enumerate(statuses):
            assertions.append(assertion("http-" + str(len(assertions)), f"/facts/observations/{case}/requests/{i}/status", status))
    stage_measures = dict(DATABASE_MEASURES)
    if args.stage in ("m2", "m3"):
        stage_measures.update(BOOKING_MEASURES)
    if args.stage == "m3":
        stage_measures.update(PREPARATION_MEASURES)
    for case, measures in stage_measures.items():
        for key, value in measures.items():
            assertions.append(assertion("db-" + str(len(assertions)), f"/facts/observations/{case}/database/{key}", value))
    plan = seal_acceptance_plan({
        "plan_kind": "ACCEPTANCE", "schema_version": "0.1", "plan_id": "qixu-native-" + args.stage, "version": 8 if args.stage == "m3" else 6,
        "subject": {"id": "qixu-" + args.stage, "version": sha, "source_ref": "github:NoctilumeDev/Qixu"},
        "question": "Do the declared native stage witnesses pass at the exact clean coordinate with real HTTP and dedicated MySQL?",
        "governance": {"claim_owner_ref": "human:repository-owner", "drafter_ref": "qixu:native-adapter", "seal_authority_ref": "human:repository-owner:authorized-engineering-goal", "seal_decision": "CONFIRMED"},
        "observation_specs": [spec], "evidence_requirements": [{"id": "native", "observation_spec_id": spec["id"], "cardinality": "EXACTLY_ONE"}],
        "sufficiency_rules": [{"id": "collected", "left": {"requirement_id": "native", "path": "/metadata/veritrail_observation/coverage"}, "operator": "eq", "right": "COMPLETE"}],
        "integrity_rules": [], "assertions": assertions,
        "resource_budget": {"max_artifact_bytes": 2097152, "command_timeout_seconds": 900},
        "change_scope": {"level": "L2_CONTRACT", "owner": "Qixu native stage", "consumers": ["native-stage-gates"]},
        "reproduction_steps": ["Use an exact clean source and Core0.13.0.", "Provision a dedicated qixu_test or qixu_ci schema and schema-scoped account; supply its test environment.", "Run the adapter; it seals before Maven clean verify and records every required case."],
        "cleanup_steps": ["The test server stops with the test JVM; only dedicated test data is reset.", "Retain the immutable Bundle and private raw logs; do not stop shared MySQL."],
    })
    write_new(output / "sealed-plan.json", plan)
    write_new(output / "request.json", request)
    exit_code = -1
    execution = "COMPLETED"
    command_started = time.time()
    try:
        # Fresh reports prevent a failed command from replaying stale green XML.
        completed = subprocess.run([args.maven, "-B", "-ntp", "-Pmysql-it", "clean", "verify"], cwd=ROOT / "backend", capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=900)
        exit_code = completed.returncode
        (output / "stdout.txt").write_text(completed.stdout, encoding="utf-8")
        (output / "stderr.txt").write_text(completed.stderr, encoding="utf-8")
    except subprocess.TimeoutExpired:
        execution = "ERROR"
        (output / "execution-error.txt").write_text("COMMAND_TIMEOUT", encoding="utf-8")
    observed = {}
    report_dirs = [ROOT / "backend/target/failsafe-reports"]
    if args.stage == "m3":
        report_dirs.append(ROOT / "backend/target/surefire-reports")
    for path in [p for directory in report_dirs for p in directory.glob("TEST-*.xml")]:
        for case in ET.parse(path).getroot().iter("testcase"):
            name = case.attrib["classname"] + "." + case.attrib["name"]
            passed = not any(case.find(kind) is not None for kind in ["failure", "error", "skipped"])
            # A duplicated case is ambiguous rather than silently last-one-wins.
            observed[name] = passed if name not in observed else False
    measurements_path = ROOT / "backend/target/failsafe-reports/qixu-m1-observation.json"
    measurements = json.loads(measurements_path.read_text(encoding="utf-8")) if measurements_path.exists() else {}
    if args.stage in ("m2", "m3"):
        booking_path = ROOT / "backend/target/failsafe-reports/qixu-m2-observation.json"
        if booking_path.exists():
            measurements.update(json.loads(booking_path.read_text(encoding="utf-8")))
    if args.stage == "m3":
        preparation_path = ROOT / "backend/target/failsafe-reports/qixu-m3-observation.json"
        if preparation_path.exists():
            measurements.update(json.loads(preparation_path.read_text(encoding="utf-8")))
    boundaries = {"m1": "M1_FOUNDATION_ONLY", "m2": "M2_API_MYSQL_ONLY_NOT_UI_OR_ALLOCATION", "m3": "M3_NATIVE_TRANSACTIONS_AND_OFFLINE_PROOF_NOT_FUTURE_BEACON_OR_UI"}
    facts = {"source_sha": git("rev-parse", "HEAD"), "source_clean": not bool(git("status", "--porcelain")), "command_exit": exit_code, "tests": observed, "observations": measurements, "required_cases": required, "boundary": boundaries[args.stage]}
    if args.stage == "m3":
        jar = ROOT / "backend/target/qixu-api-0.1.0-SNAPSHOT.jar"
        if jar.is_file():
            facts["package"] = {"source_sha": sha, "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "size": jar.stat().st_size, "current_build": exit_code == 0 and jar.stat().st_mtime >= command_started}
        else:
            facts["package"] = {"source_sha": sha, "current_build": False}
    evidence = {"schema_version": "0.1", "evidence_type": spec["evidence_type"], "source": collector, "captured_at": datetime.now(timezone.utc).isoformat(), "facts": facts,
                "metadata": {"veritrail_observation": {"schema_version": "0.1", "canonicalization_profile": "veritrail-json-c14n/1", "plan_digest": plan["seal"]["digest"], "observation_spec_digest": observation_spec_digest(spec), "request_seal_digest": sha256_json(request), "collection_session_id": identity, "collector_role": "qixu-native-collector", "coverage": "COMPLETE" if observed and measurements else "ERROR", "normalization_semantics_version": collector, "facts_digest": sha256_json(facts)}}}
    write_new(output / "evidence.json", evidence)
    report = create_acceptance_bundle(plan=plan, evidence_paths=[output / "evidence.json"], output=output / "bundle", acceptance_id=identity, execution_status=execution)
    print(json.dumps({"identity": identity, "source_sha": sha, "verdict": report["verdict"], "observed_cases": len(observed), "passed_cases": sum(observed.values()), "command_exit": exit_code, "bundle": str(output / "bundle"), "boundary": facts["boundary"]}, ensure_ascii=False))
    return 0 if report["verdict"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
