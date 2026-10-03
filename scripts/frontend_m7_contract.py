"""M7 declared boundary witnesses; model storage is not real browser proof."""
CLIENT_M7_CASES=[
    "M7 current actor can recover and stop its same-text key without clearing another actor",
    "M7 late committed A response cannot erase B same-text unknown intent",
    "M7 constructor storage denial is explicit and blocks writes before transport",
    "M7 failed intent scan preserves known metadata and cannot authorize a fresh intention",
    "M7 local token removal failure still clears private memory and attempts remote logout",
    "M7 failed recovery deletion retains original key and reports local storage failure",
]
CLIENT_M7_METADATA_CASES=[
    "M7 corrupt addressed bytes preserve owner key and permit only receipt query or safe stop",
    "M7 mismatched or invalid payload cannot be replayed from a valid intent name",
    "M7 malformed intent coordinate blocks new writes without discarding raw metadata",
    "M7 malformed legacy collection is not marked migrated or erased",
    "M7 interrupted legacy migration retains original collection and can finish without duplicate effects",
    "M7 write then throw exposes original durable key without sending a new request",
    "M7 confirmed receipt plus verified deletion completes cleanup when remove throws after effect",
    "M7 corruption after valid memory scan clears replay body and retains recovery coordinate",
]
