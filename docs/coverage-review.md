# 十六条反例、公开错题与历史经验的覆盖复核

2026-10-03。**有代表性见证，不等于全部风险排列已测试。** 本表恢复实际证据，区分历史限定资格、新独立反例、已修复的API/模型和仍待验的页面。文档不是新执行证据。

历史M7主线 `c876d1a35b11dfdf4fda74b67be9451aef8b3a49` 的原native173中，以下35处引用对应的34个不同具名键均在保留Evidence中为true；另有物理COMMIT、旧库恢复、数据库预算、实际浏览器及真实暗室来源的独立证据。不能把这些历史记录直接升级成M8通过。见[M7原资格](acceptance/m7.md)、[未知重入](m7-unknowns.md)。

最近固定修复候选 `4fb184ba2a77daecb0cc33436d6e29ef326e52be` 的native181、frontend59及真实页面18命名捕获各有预封Plan和原Core PASS；两名独立角色报告及产品反馈处置已交付。范围是API/MySQL/受控Clock、实际Client/Vue源码模型/构建及指定实页/SQL，**受保护主线新资格与M8整体退出尚未闭合**。见[M8候选事实](acceptance/m8.md)及[修复合同](contracts/m8-repairs.md)；不将历史或候选资格升级成新main通过。

## 用户十六条

| 编号 / 问题 | 历史代表性见证与保留边界 | M8复核 |
| --- | --- | --- |
| C01 人工稀缺 | 小图独立穷举及真实批次已验；100人/110实际座位规模未验，U05<br>`dev.noctilume.qixu.PreparationIT.residualPromotionsCannotRecreateArtificialScarcity` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C02 截止、修订与冻结版本 | 受控Clock、真实锁与MySQL已验；时钟跳变未验，U07<br>`dev.noctilume.qixu.PreparationIT.immutableVersionsFreezeExactWinnerAndRejectOfflineOverwrite`<br>`dev.noctilume.qixu.PreparationIT.queuedFreezeRechecksDeadlineAndDoesNotExposeLateInput` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C03 冻结后空间现实变化 | 软件资源/资格版本变化拒绝整轮发布已验；真实漏电由现场核实，U08<br>`dev.noctilume.qixu.PreparationIT.resourceChangeInvalidatesEntireFrozenInput`<br>`dev.noctilume.qixu.GovernanceCasesIT.penaltyAfterFreezeStopsWholePublicationInsteadOfSilentlyShrinkingFrozenPeople` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C04 随机源缺失/迟到/伪签名 | 受控不可用/截止已验，官方签名与固定未来round另见M3/M6；未证明全部外部故障<br>`dev.noctilume.qixu.PreparationIT.sourceUnavailableAndResultDeadlineCannotSwitchEntropy` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C05 半个正式结果 | 真实事务异常/截止回滚已验，物理COMMIT丢回应另验；发布每个指令点kill未穷举，U07<br>`dev.noctilume.qixu.PreparationIT.publicationFaultRetainsCandidateButRollsBackWholeResult`<br>`dev.noctilume.qixu.PreparationIT.publicationCrossingDeadlineHasNoPartialRights` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C06 候补升级与兜底双占 | 原子升级/重放/失败保原权已验；M8新发现另列<br>`dev.noctilume.qixu.PreparationIT.fallbackUpgradeIsAtomicAndDuplicateConfirmationReturnsReceipt`<br>`dev.noctilume.qixu.PreparationIT.upgradeWriteFaultAndDeadlineNeverReleaseOldRight` | 新增维护扫描与候补独立版本真实回归已通过；已确认兜底保持。页面模型通过，当前实页复验待执行。 |
| C07 跨批次重叠 | 声明的真实竞争已验；多节点不支持，U03<br>`dev.noctilume.qixu.PreparationIT.crossBatchConcurrentApplicationsHaveOneIntervalFact`<br>`dev.noctilume.qixu.PreparationIT.concurrentExitAndConfirmCannotResurrectAndCycleCloses` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C08 管理员手改归属 | 无任意送席端点；限定治理/独立申诉纠正、审计通知与拒绝已验；不承诺通用任意override<br>`dev.noctilume.qixu.GovernanceCasesIT.administratorLosingScopeWhileWaitingCannotRevokeTheRight`<br>`dev.noctilume.qixu.GovernanceCasesIT.independentAppealCanRestoreAnActuallyFreeOriginalSeat`<br>`dev.noctilume.qixu.GovernanceCasesIT.appealCannotStealASeatAlreadyAcceptedByTheStableWaitlist` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C09 活动/场地/参与混同 | 合法场地先成立、换地保参与及冲突处置已验；M8实页缺口另列<br>`dev.noctilume.qixu.BookingIT.eventRequiresApprovedVenueAndCannotBypassOwnership`<br>`dev.noctilume.qixu.BookingIT.eventRebindingKeepsParticipationAndOutboxRetriesDeduplicate` | 新增活动地点、三个窗口、本人短约重叠/相邻及他人隔离真实回归通过；重叠仍可报名。当前实页待验。 |
| C10 观察空闲当批准权 | 真实并发批准/层级占用已验；M8手机缺口另列<br>`dev.noctilume.qixu.BookingIT.pendingVenueDoesNotBlockAndParallelApprovalsSerialize`<br>`dev.noctilume.qixu.BookingIT.areaApprovalAndSeatBookingCannotBothCommit` | 新增授权详情人数、当前容量、申请人、联系信息通过；其他学生仍403。当前手机决定页待验。 |
| C11 旧读或404误导重提 | 主库回执/在途404/同键恢复及实际浏览器丢回应已验；M8内部恢复缺口另列；无从库部署声明<br>`dev.noctilume.qixu.BookingIT.missingReceiptDuringFlightIsNotProofOfNoCommit`<br>`dev.noctilume.qixu.BookingIT.sameKeyRecoversCommittedResultAndRejectsChangedBody` | 内部recover及Vue旧投影、两端恢复消费者回归通过；当前实际浏览器故障复验待执行。 |
| C12 通知失败/已读混同 | 持久结果、outbox/inbox去重、已读分离已验；微信真实送达未验，U08<br>`dev.noctilume.qixu.PreparationIT.allLosingApplicantsReceivePersistentResultsAndStableWaitlist`<br>`dev.noctilume.qixu.BookingIT.notificationFailureRollsBackEffectAndSameKeyCanRecover`<br>`dev.noctilume.qixu.BookingIT.eventRebindingKeepsParticipationAndOutboxRetriesDeduplicate` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C13 学生报告污染事实 | 后端隐私/核实/修复复验已验；M8实页UNKNOWN反例另列；非现场维修证明<br>`dev.noctilume.qixu.FeedbackIT.reportsArePrivateAndNeverBecomeFactsWithoutScopedVerification`<br>`dev.noctilume.qixu.FeedbackIT.malformedSecondFactCannotPartiallyPublishTheFirstFactOrDecision`<br>`dev.noctilume.qixu.FeedbackIT.workDoneIsNotResolvedAndFailedVerificationCanContinue` | 存在性未知不能制造false的真实Vue源码模型通过；原实页反例保留，当前实页/公开事实读回待验。 |
| C14 外部身份不可用/撤权 | 受控协议/租约/撤绑及M6真实暗室接入已验；校园SSO/长故障未验，U06<br>`dev.noctilume.qixu.ExternalIdentityIT.proofExpiresAfterActualFloorWaitWithoutNetworkInsideLocks`<br>`dev.noctilume.qixu.ExternalIdentityIT.bindingRemovalDuringFloorWaitRollsBackOriginal`<br>`dev.noctilume.qixu.ExternalIdentityIT.providerOutageDoesNotEraseCommittedReceiptOrDuplicateRecoveredWrite` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |
| C15 时间依赖与晚任务 | UTC Clock/半开区间/锁后时限/晚任务代表性已验；M8期限/维护反例另列；NTP未验<br>`dev.noctilume.qixu.BookingIT.adjacentIntervalsAndDeadlineUseCurrentServerTime`<br>`dev.noctilume.qixu.BookingIT.lockWaitRechecksSessionAndClockBeforeWriting`<br>`dev.noctilume.qixu.PreparationIT.expiredUpgradeAndOldTaskKeepFallbackAndRejectForeignOwner` | 任务暂停时错过freeze/result立即释放申请阻挡与池保护；独立补freeze已过、result未到控制通过。原输入/失败通知保持。 |
| C16 恢复旧库而外部世界未回滚 | 物理旧库+保留独立账册/完整恢复/缺账册隔离已验；全主机一起回滚/真掉电未验，U04<br>`dev.noctilume.qixu.RecoveryTransactionsIT.preparedMarkerAndBusinessShareTheRealCommitVisibilityBoundary`<br>`dev.noctilume.qixu.recovery.RecoveryFenceTest.oldDatabaseMissingKnownCommitMarkerQuarantinesAllAdmission` | 原限定范围保持；不以此声明所有故障或生产环境已证明。 |

## GitHub与官方案例迁移

借用的是失败机制，在期序构造自己的反例；没有运行所有上游项目测试，也没有接入全部上游功能。出处与后续引用见[错题本](failure-notebook.md)和[M7来源](contracts/m7-execution-entry.md)。

| 来源 / 机制 | 已执行与未执行 |
| --- | --- |
| RoomVox周期活动漏查 | 第一版未实现周期课表导入，明确范围外；不能称已运行其复现 |
| RoomVox保存活动但未占场地 | 已迁移成C09未批准不能发布/换地保报名的真实API见证 |
| 事务内慢网络/连接池 | 受控身份租约、锁外网络、真实lock/socket/COMMIT预算已验；没有运行上游基准或生产耗池压力 |
| 预约幂等/并发 | C10/C11真实同键、异正文、双批准已验；没运行其他项目测试套件 |
| NIST随机源程序 | 仅规则启发；本项目选drand固定quicknet，不伪称接入NIST |
| Jackson重复键与尾随 | 三种重复/转义HTTP首FAIL保留；严格解析修复后原标准复验 |
| Excalidraw存储故障 | Storage模型构造/扫描/删除/损坏元数据已验并有首FAIL；浏览器真实策略未验，U02 |
| Spring Security/Cookie生命周期 | 主体代际及正常双页面换号已验；M8又击穿这些机制；修复后的API/源码模型已复验，当前实页尚未闭合；晚Set-Cookie未验 |
| Cypress证据/重定向 | 改进证据归属与采样，不作为产品某功能已安全的独立证明 |
| drand round边界/未来值 | 受控拒绝、官方离线签名、固定未来round实际采集与独立复算已验；不是所有源故障 |
| Vue Router历史恢复 | 实际地图筛选→详情→返回已验；不声明所有路由/草稿/原生返回已全测 |
| Spring事务listener/MySQL通信/Hikari | 实际框架事务边界及物理COMMIT/旧库/借出连接故障已有原Bundle；不是硬件持久性或多节点证明 |

## 其他工程留下的经验

以下是迁移到期序的具体机制，不声称别的仓库通过就能替期序证明。引用坐标见[生命周期](lifecycle.md)、[施工记录](decisions.md)与M7来源记录。

| 经验 | 本项目证据范围 |
| --- | --- |
| 启动readiness/drain | Foundation真实HTTP初始化503/恢复登录与原首FAIL保留 |
| 取得锁后再看时间/身份 | Booking/Feedback/Governance/ExternalIdentity真实锁等待见证 |
| UNKNOWN、旧404与同键恢复 | API真实回执+客户端模型+实际浏览器丢回应；M8原反例保留，修复API/模型通过，当前实页待验 |
| A→B旧成功/401/页面返回 | 模型与正常实际页面已验；M8真实API/源码模型复验通过，当前实页待验 |
| 事实、执行声明、通知分离 | Feedback/SpaceBlocks/治理/通知已验；M8UNKNOWN转换模型复验通过，当前实页待验 |
| 冻结输入/旧worker/晚期限 | Preparation真实版本/事务与受控Clock见证；物理全部kill点未验 |
| 验迹固定坐标与失败留存 | 原Plan/Evidence/Bundle、protected PR与fresh-main限定资格；不将M7移植成M8 PASS |

## 首败和未证明

第一组：`a9c1b6a…`真实native175中2项失败，frontend51中5项失败。`fdda898…`修复后175/51通过。第二组：`ce29fdb…`native180中4项失败，frontend54通过；原失败`m8-41cf91f471334587b0021656a4b09d7c`未覆盖。`ca1aeb5…`最小修复后181/54通过。另三次测试编译/正向夹具错误独立保留，不计作产品缺陷或修复。

微信真机、真实通知送达、全部浏览器存储政策、多实例、生产容量、100人/110真实座位规模、全主机回滚和现实维修仍按U01–U08保留NOT_PROVEN及重入条件。新的核心反例必须重开相应机制；不能用“测过十六条”关闭这些边界。
