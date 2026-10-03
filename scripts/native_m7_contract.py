"""M7 entry v1; not whole-stage coverage or installed/device proof."""
M7_CASES={"ProtocolProjectionIT":[
    "duplicateAndEscapedObjectKeysNeverChooseAnImplicitWinner",
    "trailingDocumentsRejectWithoutEffectsAndWhitespaceRemainsValid",
    "inboxPagingFindsOldUnreadResultsWithoutForeignRowsOrFalseCompleteness",
]}
M7_MEASURES={
    M7_CASES["ProtocolProjectionIT"][0]:{"favorites":0,"receipts":0,"rejected":3},
    M7_CASES["ProtocolProjectionIT"][1]:{"favoritesAfterInvalid":0,"receiptsAfterInvalid":0,"rejected":3,"favoritesAfterValid":1,"receiptsAfterValid":1},
    M7_CASES["ProtocolProjectionIT"][2]:{"firstPageSize":20,"reportedTotal":151,"sqlTotal":151,"observedUnique":151,"oracleEqual":True,"unreadAfter":150},
}
