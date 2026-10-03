# M7 · 失败机制覆盖账册

当前为**VERIFIED_LIMITED_FAILURE_MECHANISMS**，限定退出事实见[M7](acceptance/m7.md)。各项按失败机制收束；下表的LIMITED不是整个机制、生产环境或全工程PASS。新源运行坐标在[M7页面](acceptance/m7-browser.md)及原[索引](../artifacts/m7/index.json)，首次失败不可覆盖。M8只在M7退出后开始，M10精修保留。

## 当前固定候选的代表性见证

原修复候选`6ae43a4ff112b6984fa17f68d1fab6133580e9d6`及新主线`c876d1a35b11dfdf4fda74b67be9451aef8b3a49`分别运行的native0.19/Plan7包含136项真实HTTP/MySQL集成、37项单元见证。分配/时钟/外部故障用受控依赖，不假称全部外部真实故障；65,536个小图与2,048个rank样本由独立穷举oracle比较完整个人偏好。每个原Bundle保存exact SHA、迁移/合同摘要、请求和原始SQL不变量，不能将历史别的源升级为当前资格。

| 机制 | 代表性trace与独立判定入口 | 分类及保留边界 |
| --- | --- | --- |
| F01 人工稀缺 | AllocationOracleTest完整小图/rank枚举；PreparationIT residualPromotionsCannotRecreateArtificialScarcity；最大匹配与正式行 | LIMITED_ALGORITHM/REAL_BATCH；不是大规模真实100/110批次或福利最优，U05 |
| F02 截止与版本漂移 | PreparationIT immutableVersionsFreezeExactWinnerAndRejectOfflineOverwrite、queuedFreezeRechecksDeadlineAndDoesNotExposeLateInput、publicationCrossingDeadlineHasNoPartialRights；BookingIT锁等待/确认丢回应 | LIMITED_CONTROLLED_CLOCK_MYSQL；NTP跳变及全部物理调度未证明，U07 |
| F03 来源/冻结现实变化 | sourceUnavailableAndResultDeadlineCannotSwitchEntropy、resourceChangeInvalidatesEntireFrozenInput；摘要/结果状态/逐人通知原SQL | VERIFIED_DECLARED_REFUSALS；受控源故障不迁移成公共源所有故障策略 |
| F04 部分正式世界 | publicationFaultRetainsCandidateButRollsBackWholeResult、publicationCrossingDeadlineHasNoPartialRights；真实事务中异常与COMMIT后回应丢失隔离/恢复 | LIMITED_TRANSACTION_PHYSICAL_COMMIT；发布每个指令点kill不穷举，U07 |
| F05 合法状态机组合双权 | fallbackUpgradeIsAtomicAndDuplicateConfirmationReturnsReceipt、upgradeWriteFaultAndDeadlineNeverReleaseOldRight；SpaceBlocksIT三方临时位竞争/复合来源/失败通知 | LIMITED_REAL_MYSQL；独立原始半开区间与祖先归约，不含多节点，U03 |
| F06 跨批次/来源重叠 | crossBatchConcurrentApplicationsHaveOneIntervalFact、concurrentExitAndConfirmCannotResurrectAndCycleCloses；维修只解除自己的来源与相邻时间控制 | VERIFIED_DECLARED_COMPETITIONS；不声称任何未来业务状态机组合都成立 |
| F07 观察冒充授权 | queued权限范围撤销、旧预览影响变更、两教师并发审批、活动换地/取消与层级封闭；拒绝时SQL/回执/通知无新效果 | VERIFIED_DECLARED_REFUSALS；管理员界面不是绕过事务的资格 |
| F08 未知意图丢失/复活 | IntentRecoveryIT原子停止/在途404/同键重放；45客户端恢复模型；实际收藏commit→浏览器回应丢失→重载原键→查询恢复、SQL唯一 | LIMITED_MODEL_REAL_BROWSER；首败修复留存；真实存储策略/晚Cookie未证明，U02 |
| F09 旧主体/执行者迟到 | actor+key碰撞与迟到模型；两管理文档换号旧页停止/重载新主体；学生新账号仅自己的5条 | LIMITED_MODEL_REAL_BROWSER；不将正常Cookie换号升级为所有晚Set-Cookie调度，U02 |
| F10 通知与事实混淆 | allLosingApplicantsReceivePersistentResultsAndStableWaitlist；多次第二通知故障全事务回滚、outbox去重；151条投影/已读SQL；COMMIT恢复独立world不改 | LIMITED_PERSISTENT_INBOX；已投递不等于已读，微信/现场可达未证明，U08 |
| F11 报告/核实/修复混淆 | FeedbackIT核实权限撤销/第二事实失败/私有原图；SpaceBlocksIT WORK_DONE→复验失败→重做、另一来源保留、第二恢复通知失败回滚 | VERIFIED_DECLARED_SOFTWARE_FACTS；保留原报告历史，不证明现实修好，U08 |
| F12 恢复库与外部世界矛盾 | 独立restore22原T0/完整T1/只缺账册；RecoveryTransactionsIT四个真实事务与journal/Fence15模型；物理COMMIT29真实唯一效果、实时NOT_RECONCILED、同库恢复原回执 | LIMITED_SINGLE_INSTANCE_LEDGER；旧主线与新候选资格分别注明，不含全机回滚/真掉电，U03/U04 |
| F13 接入不可用/标识碰撞 | ExternalIdentityIT12：issuer同名、租约锁等待过期、撤绑、不可用、已提交回执；M6真实暗室当前身份为历史独立资格 | VERIFIED_DECLARED_ADAPTER_BOUNDARY；不是校园SSO或所有提供方长期故障，U06 |
| F14 截断/陈旧投影伪完整 | ProtocolProjectionIT151本人/5他人HTTP集合；AdminProjectionIT8范围/个人历史；真实两端第8页/已读留页、地图筛选返回；管理端首FAIL原包 | VERIFIED_DECLARED_REAL_PAGE；原FAIL保留、原14trace复验PASS；微信设备U01 |
| F15 协议/数据库/运维边界 | 三种重复/转义JSON、尾随/空白控制；DatabaseBudgetsTest4有效属性；真实lock/socket25预算原负向与恢复；COMMIT29错误信封原FAIL/修复；fresh迁移与owned清理 | LIMITED_DECLARED_PROTOCOL_SINGLE_HOST；保留源坐标，不含生产总SLO/多实例/所有格式，U03/U05 |

## 首败与最小修复

协议重复字段、151消息截断、同主体key清理错误、存储故障与损坏元数据，见[入口](acceptance/m7-entry.md)、[元数据](acceptance/m7-metadata-oracle.md)。备份T0与commit语义、budget负向、观察器前提，见[恢复](acceptance/m7-restore.md)、[数据库](acceptance/m7-database.md)。真实COMMIT首500与管理消息无分页分别保存原FAIL；修复先版本化最小边界，旧断言不改。纯采集遗漏/启动/日志问题保存PENDING/ERROR，不能算作产品修复。

## 有限收束与未知

未知见[M7未知账册](m7-unknowns.md)：每项有触发、不变量/影响、缺证据、环境、具体重入和维护者/重开条件。没有PLANNED被自动改成PASS；没有把已观察核心失败藏进未知。现有同机制排列按代表性调度收束，新机制或独立角色反例可以最小重开。

main@c876d1a的原真实页面14和其他同源运行已通过，公开保护/CI/远端读回及源资格对齐；范围内没有已观察却未处置的核心错误。M7结束只回答攻击、击穿、修复、未知，不宣布零Bug；将坐标交M8两名独立角色重新挑战。
