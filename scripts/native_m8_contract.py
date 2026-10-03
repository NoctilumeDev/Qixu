"""Independent review regressions; real SQL plus controlled Clock, not capacity."""
M8_CASES = {
    'FoundationIT': ['bearerLogoutPreservesUnrelatedCookieSession'],
    'PreparationIT': ['publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation'],
}
M8_MEASURES = {
    'bearerLogoutPreservesUnrelatedCookieSession': {'bearerSetCookieHeaders':0, 'remainingAdminSession':200, 'unrevokedSessions':1},
    'publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation': {'legalBatches':11, 'earlyActiveRights':10, 'taskScanRounds':2, 'lateOpenOffers':1, 'latePromotionNotices':1},
}
