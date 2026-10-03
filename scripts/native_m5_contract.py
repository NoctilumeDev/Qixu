"""Explicit M5 additions; removing a witness never silently shrinks the Plan."""
M5_CASES = {
    "FoundationIT": ["cookieReadBindingRejectsAnotherDocumentSessionAndSameActorRotation"],
    "FeedbackIT": ["repairActionsAcceptEmptyReferencesButOnlyLinkActionCanAttachReports"],
    "AdminProjectionIT": [
        "studentsAndTeachersCannotReadAnyManagementProjection",
        "blockPagesReflectCurrentScopeAndEnforceExplicitFloor",
        "onlyKnownAuthorizedEventsContributeToOutboxCounts",
        "auditUnknownEntitiesDoNotGrantGlobalAccessAndDetailsNeverLeak",
        "mixedFloorBatchCannotExposeWholeBatchThroughOneAuthorizedSeat",
        "pageBoundsAreRejectedWithoutWritingFacts",
        "entitlementProjectionUsesCurrentFloorScopeAndPublicWhitelist",
        "personalApplicationPagesIgnorePublicRecencyAndCannotSelectAnotherActor",
    ],
    "IntentRecoveryIT": [
        "stopBeforeOriginalMakesLateOriginalImpossibleAndIsRepeatable",
        "existingOriginalAlwaysWinsAndStopCannotCancelBusinessFacts",
        "anotherActorHasSeparateKeyNamespaceAndCannotStopThisOriginal",
        "unknown404IsNotProofButBarrierIsARealPersistentFact",
        "expiredSessionAndInvalidKeyDoNotCreateBarrier",
        "racingOriginalAndStopProduceOneFactAndExactlyTheCorrespondingEffect",
    ],
}
M5_MEASURES = {
    "repairActionsAcceptEmptyReferencesButOnlyLinkActionCanAttachReports": {"emptyReferenceClosedRepairs": 1},
    "stopBeforeOriginalMakesLateOriginalImpossibleAndIsRepeatable": {"receipts": 1, "reports": 0},
    "existingOriginalAlwaysWinsAndStopCannotCancelBusinessFacts": {"receipts": 2, "reports": 1, "favorites": 1},
    "anotherActorHasSeparateKeyNamespaceAndCannotStopThisOriginal": {"receipts": 2, "reports": 1},
    "unknown404IsNotProofButBarrierIsARealPersistentFact": {"receipts": 1, "reports": 0},
    "expiredSessionAndInvalidKeyDoNotCreateBarrier": {"receipts": 0, "reports": 0},
    "racingOriginalAndStopProduceOneFactAndExactlyTheCorrespondingEffect": {"receipts": 20},
}
