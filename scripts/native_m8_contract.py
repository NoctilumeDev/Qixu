"""Independent review regressions; real SQL plus controlled Clock, not capacity."""
M8_CASES = {
    'FoundationIT': ['bearerLogoutPreservesUnrelatedCookieSession'],
    'PreparationIT': ['publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation','failedFrozenBatchReleasesParticipationBeforeTaskCleanup','missedFreezeBatchReleasesParticipationBeforeTaskCleanup','missedFreezeBeforeResultDeadlineReleasesParticipation','waitlistOwnVersionExitKeepsConfirmedFallback'],
    'BookingIT': ['venueDecisionDetailsContainScopedCapacityAndContact','eventDetailsExplainWindowsAndOnlyOwnShortOverlap'],
}
M8_MEASURES = {
    'bearerLogoutPreservesUnrelatedCookieSession': {'bearerSetCookieHeaders':0, 'remainingAdminSession':200, 'unrevokedSessions':1},
    'publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation': {'legalBatches':11, 'earlyActiveRights':10, 'taskScanRounds':2, 'lateOpenOffers':1, 'latePromotionNotices':1},
}
for name in ['failedFrozenBatchReleasesParticipationBeforeTaskCleanup','missedFreezeBatchReleasesParticipationBeforeTaskCleanup']:
    M8_MEASURES[name]={'newApplicationStatus':200,'oldExitStatus':200,'expiredPoolAvailable':True,'formalResults':0,'oldFailureRecorded':True,'failureNotices':1}
M8_MEASURES['missedFreezeBeforeResultDeadlineReleasesParticipation']={**M8_MEASURES['missedFreezeBatchReleasesParticipationBeforeTaskCleanup'],'beforeResultDeadline':True}
M8_MEASURES['waitlistOwnVersionExitKeepsConfirmedFallback']={'wrongVersionStatus':409,'activeFallback':1,'waitlistStatus':'EXITED'}
M8_MEASURES['venueDecisionDetailsContainScopedCapacityAndContact']={'people':1,'spaceCapacity':8,'contactPresent':True,'applicantPresent':True}
M8_MEASURES['eventDetailsExplainWindowsAndOnlyOwnShortOverlap']={'adjacentConflicts':0,'ownConflicts':1,'publicSpaceCode':'R-A','windowsPresent':True,'keptShorts':2}
