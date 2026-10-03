# 错题本

本页区分公开参考案例、待验证风险和本项目实际失败。参考案例不是期序已经发生的缺陷，修复说明也不能代替实际复验。

## 公开案例及迁移问题

| 编号 | 来源 / 原问题 | 对期序的检查 | 当前状态 |
| --- | --- | --- | --- |
| R01 | [RoomVox #8](https://github.com/nextcloud/RoomVox/issues/8)：重复会议后续日期漏查冲突 | 周期导入逐次校验；第一版不伪称支持周期课表 | 参考案例 |
| R02 | [RoomVox #7](https://github.com/nextcloud/RoomVox/issues/7)：活动保存但场地未预约且没有清晰提示 | 申请、批准和占用确认分开显示 | 待验 |
| R03 | [concurrent-seat-reservation](https://github.com/Chaity-git/concurrent-seat-reservation)：慢网络位于事务内可耗尽连接池 | 身份/随机来源/通知调用与业务锁解耦；有超时与恢复 | 待验，未运行该项目基准 |
| R04 | [restaurant-reservation-platform](https://github.com/Peggeyyy/restaurant-reservation-platform)：请求幂等与资源级并发判断 | 同键同正文重复结果，同键异正文拒绝，批准重新判冲突 | 待验，未运行该项目测试 |
| R05 | [NIST 公开随机程序](https://csrc.nist.gov/projects/interoperable-randomness-beacons/apps) | 先冻结输入及未来随机坐标，再取得值；防事后挑种子 | 待验；具体来源合同尚需 M1 |

## 本项目必须构造的反例

1. 100 个座位，80 人都只接受 10 个窗边位置：不强制安排不可接受席位。
2. 甲接受Q/N、乙只接受Q：裸贪心会制造人工稀缺。M3版本2必须在硬约束内达到最大人数，再按固定抽签排列满足偏好；未实现前不称已修。
3. 同主体跨考研/考公批次并发确认：不能获得两项重叠长期权。
4. 暂离的长期席位：不能重新分配给其他人。
5. 活动占整区与单座预约同时提交：不能同时获准冲突占用。
6. 随机值迟到/请求超时/进程重启：只能复算同一输入，不能重抽。
7. 冲突预览后新预约进入：旧处置不能遗漏影响。
8. 自报到场/预约数量：不能包装成传感器实时人数。
9. 原始学生反馈和图片：另一个学生/越区管理员不能取得。
10. 临时替代结束而另一个维护仍存在：不能恢复为可使用。
11. 手机审批后返回：筛选、日期、搜索与滚动保持。
12. 构建/上传成功但 AppID或后端目标错误：须实际入口和设备读回。
13. 100 人、110 位、安静区仅 30 位：局部公平不得以通知沉默或没有其他选择结束服务；授权兜底和较高志愿候补并存，后续换位无双权。
14. 100 人、80 个均可接受长期位：不超卖，20人明确候补；其他短位也满时诚实显示无资源。管理员统计区分物理下界、图内条件限制和待确认，初始输出不足图最大人数必须报算法/发布失败。

最新16条组合反例、统一模板、证据状态与恢复边界见 [反例矩阵](counterexamples.md)。

## 实际失败记录格式

每条实际失败保留：固定提交、环境、前置条件、最小复现、预期/实际、首次输出、失败分类、修复坐标、原合同复验、最终状态。状态允许成立、误报、边界和待验，不能把未观察的路径标为通过。

当前实际失败已进入 [M1 执行事实](acceptance/m1.md)：首次 MySQL 时间类型误转导致登录 500，采集器断言 ID 在 seal 阶段被拒绝，以及实际启动 health 提前 READY 而登录 401。首次 FAIL、恢复坐标和范围均保留。本轮完成实现后按用户要求再次搜索公开错题，再运行独立测试者和产品经理审阅。

M2首次6例失败归于测试夹具编号/楼层目标不符，保留其FAIL后修正实际夹具。网络丢响应、在途receipt404和通知异常是主动故障见证，不误记为已发生的生产事故；公开坐标见 [M2事实](acceptance/m2.md)。

## M4施工首轮 · 反馈夹具坐标错误

`m4-feedback-construction-09512bd462a246b683449da217ac1761`：真实隔离MySQL V4迁移成功，6个反馈施工见证4通过2失败，未产生M4 Core资格。第一条预期404的枚举码误写NOT_FOUND，产品已有合同为RESOURCE_NOT_FOUND；第二条把admin2当floor100之外主体，但演示权限表明确授予其三楼floor100，因此附件读取200是合法结果。夹具改为floor101并用已声明错误码；未扩大权限或放宽隐私标准。原stdout、原测试文件、XML和请求/数据库观察已独立保存，不覆盖首轮输出。

同条件恢复`m4-feedback-construction-6730e4e6ce6440bdbd70840eff88bf29`6例执行成功；这是施工见证，不是M4 Core资格。后续补充像素炸弹、等待期间撤销范围、混合事实中第二项非法导致整次回滚，保持原私有/事实合同。

扩展后`m4-feedback-construction-c405950ab1344658a9cbd9fafe8a9e09`8例执行成功，包含真实floor锁等待后范围撤销、像素炸弹和第二项非法事实的事务回滚。原6例标准保留。M4整体冲突/治理尚未完成，不升级为M4资格。

## M4施工 · 空间投影空值排除

原`m4-spatial-build-f480f1f9d03b424e92cbf8ef69e55838`在编译时发现多余括号，原ZIP和编译输出保留；之后`8d0043fa`构建成功，不作为业务资格。施工测试夹具首编译`938d076d`缺少Import引用，修正夹具。`666f9851`遗漏mysql-it profile，仅编译和单元运行、零IT，明确NO_WITNESS；施工采集增加profile与零见证检测，不把退出0当业务通过。两次显示脚本的本机编码错误不改原始二进制日志。

真实MySQL V5首次执行`m4-spatial-tests-4496b27444274d838fa468940544c569`8例中6通过2失败。产品缺陷：OPEN long_offer的entitlement_id为null；一般冲突查询excludeEntitlement也为null，Objects.equals(null,null)错误地排除了要约，从而把其已预留临时位置说成空闲。允许结果必须是到期前被占用、到期后实时释放且不依赖任务。修正为只有明确非空排除ID才能排除原权，不放宽时限/使用权标准。另一条夹具使用7字符key，被原8–80字符合同正确拒绝，修正测试输入。原源码、XML、请求/数据库及二进制输出保留，修复后需原条件复验；尚无M4 Core资格。

同条件恢复`m4-spatial-tests-b75e92cbef4c4e70a1903801b7e4c5dd`8例全部通过，原失败未覆盖。扩展`16e30573ab984a9c8894726d018bc4e1`14例中13通过1失败：夹具把“长期使用者已临时迁出”误推为“检修中的区域可以举办活动”。产品对父场地仍存在的子席MAINTENANCE正确返回SPACE_CONFLICT，不能为让测试绿灯忽略物理限制。改用活动临时位再次检修的合法链验证取消时只撤自身来源，并新增持续拒绝覆盖检修区域的独立见证。未把错误预期归为产品修复；后续仍需复验。

`85354ed5c0c340d6a1c2f0d558ef0900`15例中14通过1失败：活动参与使event.version从2推进到3，夹具仍用2取消，被正确STALE_VERSION拒绝。保留明确旧版本拒绝，再经真实GET读当前版本发起取消，未绕过版本检查。`m4-spatial-tests-f35b1df9545d4046b925cf150857c01d`15例全部通过，包括完整处置活动与原维护互不误撤、两个迁移计划争一个目标、影响集合减少、半开边界及安全无真实替代。原8例条件保持；只是M4b施工证据，整体治理/验迹资格仍未成立。
# M4 全回归首次失败 · 测试夹具边界

固定 `fb2612bfb0d689b7a488f2b7dcaf89c03d41a971` 的 native `m3-bb4df6c1518b44bf8f1ede7ce2c27707` 观察 75 项、74 项通过，Core FAIL。Foundation 的公开靠窗筛选观察到 9 个而原标准是 8 个。空间测试临时夹具 5801 留在隔离库，跨 suite 污染基线，分类为测试基础隔离缺陷，不能改原断言或解释成产品 PASS。原 Bundle 与私人 XML/source ZIP 已保留；清理在限定 qixu_test/qixu_ci 中、业务 FK 依赖清除后只删除明确测试坐标。修复 `7987ac91fd3f9cccf6d57437705dd3f6df9cd013` 的 fresh native `m3-d491d0ece1b44fa4a01f20c93e687a1a` 75项全部通过；仍是 M3 Plan8 加施工例观察，不升级为整个M4资格。
# M4 通知恢复施工 · 原失败保留

`m4-spatial-tests-1c2c9ba570c04dfba9d5a099b06f388c` 首次 V6 真实迁移及19项空间观察，18通过、1失败。退出申请的测试使用了不存在的 `/application/withdraw`，真实接口是 `/withdraw`，收到404 NOT_FOUND；分类为测试路由错误，不改产品授权/路由以适应夹具。私人原 source ZIP 摘要 `52561c2acd1d69221ff2e1e9547d09017fe82f4df3a68c8955215366f31b4bbe`、原 stdout/XML 已留存。V6 已执行，之后不改迁移字节；修正路由并增加活动解除事务与 batch/floor 等待的真实锁观察。新施工身份 `m4-spatial-tests-72b2bce4a2c14dad9bd78b220e23fde6` 21项全部通过，source ZIP `621118617745e416e8ffd6e6af53abc433f0aed8d185f212f2a43af3b37b3057`，不是整个M4资格。
# M4 设施与维修来源施工 · 首败

`m4-spatial-tests-998d71fce77a44fb9bb852e1f80663f7` 真实V7升级、空间28项及原反馈8项，35/36通过。夹具先建立同空间课程占用，验证不能关联维修后却没有撤课程，继续期待新维护成功，收到正确 EXCLUSIVE_BLOCK_CONFLICT。分类测试前提冲突，保留原 source ZIP `7745e1797a8a307d8b836f6b6dd23d889cbf85b6b4bf378bce67042eed35f8ed` 和原XML/输出。修正为显式撤课程后再建合法维修来源，不放宽独占约束。新增事实会修改演示设施画像，测试清理同时恢复明确DEMO基线，避免下一用例继承其他用例的核实结果；只操作限定测试schema和固定演示坐标。V7已执行，不改旧迁移。

同条件复验 `m4-spatial-tests-702ff32ba66b4fb0adf740844741b9d9` 的36项全部通过；私人 source ZIP `db655fad5f8d35aac82bdb4e4dd4ee9da3a521d258fa4792fd6519ee15c6837a` 绑定本次施工世界，原失败保留。范围是设施确认摘要、保留原临时要求/未知历史拒绝、维修仅解除自身来源及故障回滚；治理尚未实现，不声明M4整体资格。

## M4 维修闭环反例 · 已解决与已知损坏并存

原施工坐标 `25d28f6f4315cf48c33440f38a30d64227f298d9`。第一次探针 `m4-spatial-tests-2f3d87033b314cf0a0f6051e365ac376` 的断言读错 `/profile/features/outletCondition`，空值使其通过；这是验证器缺陷，该PASS不能证明目标性质。原ZIP `ef6be0dd14b8e2b63877fd5baf48f99e9bfe648d95951f01b17634e5dde1075c`、观察和输出保留。改读实际 `/profile/conditions/outletCondition`，并先断言BROKEN前置条件。

纠正后的真实反例 `m4-spatial-tests-3ae7ddbe1efb4edb82573b82a140a78c` FAIL：VERIFY true只提交outlet=true得到200，报告RESOLVED，但公开条件仍BROKEN。原ZIP `bba86aef06ef6427ce02fb2db4e9de693325708c10abb04f0acb651cff6ea0e4` 保留。分类产品事实矛盾；最小重开设施/维修细则0.2，要求明确修正已知坏状态，不自动推断WORKING。

修复后 `m4-spatial-tests-aeea3484eca54295a459d8337adbe9f2` 38项全部通过（原空间28/反馈8，新增同一设施闭环与已结束来源两项）。原反例条件保持并断言409 REPAIR_FACT_REQUIRED、无回执/状态变化，UNKNOWN/BROKEN/REPAIRING均不能擦除问题，明确WORKING及outlet=true才恢复。旧第二通知故障例补齐正确修复事实以继续到达其原503回滚边界；不把事实拒绝当通知故障证据。source ZIP `f1ade1af240c19a8a1fa43b70c6404e92ffa7a8a068f2b72000be05b193a1a06`、请求/SQL/原输出保留；仍非整个M4资格。

## M4 治理与当前资格 · 施工见证

实施前合同 `42dd721f20ada68ac11997a26a174ba9f769ea36`。首轮 `m4-spatial-tests-76ab1ed9a24c4fb7a54f3df90f4be5d1` 真实V8迁移及16项治理见证全部通过，source ZIP `af1fd53efd888f957d5e99b72780ab37d7d698aa201b87bc78f1d996a007446d`。实际观察私有通知/陈述、24小时与7天边界、等待中撤范围、并发裁决、独立复核、不抢夺合法递补、维修冲突、处罚到期、原回执及无人持权取消；通知故障是主动控制，不记为生产事故。

扩展 `m4-spatial-tests-7cb5a51bdc1e41148b8bc216738eb688` 20项全部通过，source ZIP `e0b3d424c1040be32571292d5d76977fbc52d179da9ff089ec014525bf8b6b0c`。新增处罚在提交之后/冻结之后/要约之后/跨周期候补期间变化：冻结排除但保留提交版本，发布整轮停止且不缩冻结集合，确认拒绝且维护将旧offer标INVALID，稳定候补当前不合法者不递补、可查当前资格原因。原16项保持，V8已执行不改迁移字节。仍为施工观察；整个M4正式Plan、全回归、公开门禁及fresh-main尚待，不继承M3资格覆盖新治理。

## M4 native测量器首败 · 处罚主键不是id

`a87a15f37bf5d2f230fcb7b8aaa02bde430f1778` 的M4 Plan1 / native0.9 `m4-3f6425d538a9441796b3cde14cf42987` 为FAIL，110项中52项通过，58项M4在AfterEach采集出现SQL错误。原V8明确处罚表PK为case_id，新采集查询错误使用ORDER BY status,id。分类验证器缺陷；不是58个产品故障，更不能把缺M4观察默认成0。原Bundle、stdout/stderr和fresh XML原文件保留。

最小修复只按status排序；同值在仅状态投影里不可区分。保留原5项SQL不变量及全部110项要求、Plan1和native0.9语义，同合同新identity重跑；不得追溯修改原FAIL。

## M4 Plan投影首败 · 中间事实与终态混用

`1056e4230e9f7d1496f80047346c46575bf85520` / `m4-7ea7ca0c04fb4b278c719a963e28482c` Maven0、110项全PASS，但Core FAIL(db-335)：计划要求最后space_blockStates为ACTIVE/REVOKED，真实最终为REVOKED/REVOKED。原用例有明确的两次解除，且各自HTTP/SQL断言正确；因此不是产品误撤另一个来源，是计划绑定错时点。

最小重开观察合同0.2/Plan2/native0.10：第一步单独测量来源仍有效及临时占用保留，第二步测量完整撤销后临时位释放；原权保持，所有其余义务不变。保留原Plan1 FAIL、输出和fresh XML，不能把110个绿灯冒充Core通过。

## M4安装探针 · 秒精度合同的正确拒绝

`559b13ca79782ad44fc6c63185ddf0b734e59763` 的native Plan2 PASS110，producer-bound `m4-live-0e54e4f2266447d3b4a0281fec8c6b58` 在真实未来round32725403冻结/发布和私有反馈、维修闭环后为execution ERROR/Core PENDING：治理通知POST422 INVALID_INPUT。探针statementUntil由Python当前微秒时间加24小时5分钟生成，违反既有Business.time的精确到秒要求；不是治理无法创建，也不放宽时间合同。原packet、请求/SQL测量、Bundle和runtime记录保留，own JVM26504已停止。

最小修复将探针的公告期限在生成时对齐秒，保留24小时安全余量；同Plan1/live0.4新identity从fresh native producer复跑。实际跨天治理仍为native受控Clock证明，安装探针只观察正常Clock下陈述/提前收回拒绝/DISMISS，不冒充一天已流逝。

## M5依赖首次失败 · 精确pin不满足官方peer

main@95be282后的两端基础施工首次npm install退出ERESOLVE：uni-app固定版本3.0.0-5020420260813003要求@dcloudio/types精确3.4.31，而本项目从官方模板的^3.4.8错误收紧为3.4.8。分类工具链选型缺陷，未产生运行产物或页面资格。原报告/调试日志留存在`.tools/m5-first-failures/npm-peer-types/`；官方npm peer元数据重新读回后，只改types为3.4.31，不使用force或legacy-peer-deps绕过契约。后续恢复结果另记，不覆盖首次失败。

依赖恢复首次安装退出0，锁文件产生；官方工具链带来phin/vue-i18n弃用警告，保留为后续依赖边界，不称所有供应链风险已消失。请求内核首10例通过后加4个施工反例，首次14例10通过4失败：旧GET异常未校验序号、无效key被送出后重载丢失、错key回执被当成提交成功、Cookie控制未知仍允许换号。原源码/测试/输出留`.tools/m5-first-failures/client-ownership/`。分类前三项为产品内核缺陷，第四项为未实现的可靠性保护；最小补充Cookie未知必须重载细则，保留原账号/未知提交合同，随后修复并按原14例复验。尚无页面或M5资格。

## M5 · 请求恢复与多文档首败

`private-recovery/`保存原合同、客户端和首轮输出。清除敏感正文后，未知请求不能在重载中凭空重放，也不能因为正文不可得就抹除原键。先补0.3恢复元数据/原子停止合同，再实现当前主体锁内屏障；原事实已成立则返回它，屏障只阻止尚未发生的原意图。

真实双管理文档`m5-ui-live-4d171ba8c83a4907a2e1194a5216fb9a`的`first-cookie-multitab-owner.txt/jpg`观察到旧文档仍以空间管理员为标题，读到共享Cookie新主体的消息。分类产品所有者漂移；JS generation不能控制浏览器共享Cookie。最小补0.5：Cookie GET绑定当前CSRF所有者，不匹配409；其他文档控制广播清私有投影并要求重载。修复后`d59b13e2`的`cookie-other-document-stopped.txt/jpg`留存真实停止页，HTTP回归覆盖不同主体与同主体轮换。

双文档恢复记录原用全量数组写回，会使旧文档清理抹掉新意图或使迟到失败复活已解决键。0.6改逐键不可变元数据、先持久化再发送、旧数组仅一次迁移；0.7区分单次4xx和整个意图结果，通过原子屏障结束确定拒绝。可控传输31项包含原键在途竞争、配额失败、迟到回写、旧主体和私有文件释放。它们是客户端机制见证，不冒充真实网络或真机。

## M5 · 真实页面投影与上下文

`m5-ui-live-d59b13e206294a16a3c0a612da850fde`保留两个首败：本人活动列表用活动PUBLISHED替代参与CONFIRMED且入口误用参与ID；管理活动行把缺space_id说成“批次资源池”。修复分别显示活动与本人参与状态，以event.id进入详情；地点/时间从合法场地绑定投影，不猜不存在的空间。后续`student-event-confirmed.jpg`、`1fa00800.../student-personal-event-corrected.jpg`与真实取消读回保留。

`4d171ba8.../first-unlabeled-fields.txt/jpg`保留字段标签不足；`recovery-f06ddef82e37414d919431b0dccc0b7b/first-post-write-availability.jpg`保留写后旧可用性提示。页面补可识别字段名、空间code/name与换位关联，写后清旧可用性再读当前事实；错误/409保留输入，加载不展示假空结果。`0d6aa0b3.../student-map-return-context.txt/jpg`实际观察390px搜索A018、座位筛选、150%地图进入详情再返回仍保持条件/缩放；离开私有报告后DOM中blob原图数为0。属于施工页面观察，不继承最终M10视觉资格。

## M5 · 私有照片运行污染与维修动作首败

`m5-ui-live-1fa00800d52143f086488b48557d00cb/first-private-photo-download.txt/jpg`：照片上传成立、服务器字节有效，刷新却INVALID_RESPONSE。临时诊断观察传输为JSON，与当前FILE源码矛盾。保留旧进程坐标和原日志后，只重启owned学生Node；当前源码实际下载并显示1200×800原图，管理员绑定下载也成立。分类开发服务器共享workspace缓存污染；两端Vite直接alias到共享源码，避免node_modules忽略HMR，诊断已移除。不能把该现象归为已证明的iOS/Android下载故障。

同身份的`first-repair-assign-empty-reports.txt/jpg`观察ASSIGN因页面发送空reports被422拒绝。分类动作字段契约缺陷；0.9明确仅LINK_REPORTS消费引用，其他动作空/缺省可用、非空拒绝，页面仅发送所需字段。新增真实HTTP/MySQL反例`repairActionsAcceptEmptyReferencesButOnlyLinkActionCanAttachReports`，非空隐藏关联422且无版本/审计变化；空列表安排→工作完成→明确复验通过闭环。

`m5-ui-live-0d6aa0b3fc184342858f37f370f0d3ba`实际页面工作完成仍待复验；缺设施恢复事实409 REPAIR_FACT_REQUIRED、输入保留，记录为正确拒绝，不删保护。补outlet=true与outletCondition=WORKING才复验关闭，学生读回RESOLVED。实际SQL终态`m5-sql-readback-a67e5c0e0a884bf3a6b31096a76a02a4`退出0：feedback1 RESOLVED/v4、repair1 VERIFIED_CLOSED/v4、公开设施WORKING，6份反馈通知均已有站内投递；不把delivered称为read，不把模拟维修称为现场维修。查询输出SHA256 `15db42f3c0be9076f5183dad2c9621be8e29e1afaa28de62c9a9d5d7fabc92b0`。

两次SQL测量器`1d71f7ad351840d6add598a1c91b34b8`、`9e0256839a7246b0be4a9f657c02f47b`错误猜notification_outbox.aggregate_key/status，退出1原输出保留；按实际DDL recipient_id/event_key/created_at/delivered_at修测量，不是产品失败，也不升级原部分观察。

## M5 · 当前施工恢复坐标

固定源码`07ece2f2b936ac9ee1649bc11dd73eff7e4d005b`原生`m5-a4998661d43e41c880c08c57b0f95ade`PASS126，前端`m5-frontend-a1f439a381504e238d9421a3fe0a32bc`PASS31及H5/微信/admin构建。安装浏览器`m5-browser-3fe31f03dadf45739f48ba448c518071`保留私有服务初始化首败（尚未建立Handler.server）、修复后的17份固定产物DOM/截图及Core FAIL：两条观察器字面预期错误。页面实际为“输入保留”和“身份或当前管理范围不允许”，不是观察器猜测的文案。原raw字节不改，另seal `m5-browser-remeasure-a55953076fd24982b9f90f3756521a1b`对固定旧捕获重测PASS；明确不是新浏览器执行、不是全部业务语义证明。

截图复核发现独立产品问题：390px管理员从维修表单底部提交，409提示位于顶部但没进入当前视口。首次`repair-missing-fact.png/txt`保留；后端正确拒绝、输入确实保留，仍不足以说明错误可见。最小修复在错误呈现后聚焦/滚动提示，学生页同步确保顶端提示可见；保留字段与业务拒绝规则。受影响前端从新固定候选构建并实页复验，不用旧PASS升级修改后页面。

正常Clock下真实UI产生A018短约取消、收藏、范围管理员批准场地3→活动2发布、活动1重约/取消、反馈2附私有图→核实→维修2安排/工作完成→缺恢复事实409→复验关闭。SQL `m5-sql-readback-73118bba5560439ebd781e8f890770ab`退出0并核对对应终态；错误查询JSON顶层outletCondition得到NULL，后续`f2502921342b4cc8a48d59c4aabb32cf`按实际完整profile读回`conditions.outletCondition=WORKING`。不得把第一次NULL冒充设施未知或产品失败。通知6份已投递站内，未把投递称为已读。模拟图片和维修不代表实际场馆或维修事实。

隔离真实MySQL施工`m5-client-tests-548496199cac43ca9bab2bfa28e7ba6d`61项执行成功，source ZIP `2de57a4247dfcd94d4ef4bcf29c3af7fe40981a7fd5b04b52475799346a000b1`。干净前端副本`m5-frontend-clean-2b9f7d8a13ac4079a9afdf6ad222ab35`npm ci、31项请求机制、两端类型检查、H5/微信/admin构建退出0，source ZIP `a19e62c36aad2c269d1377355cc1ee2a596b268af20e7dd5d6da50ff53fdb405`。此后还有加载提示与按钮小修，因此不能把该构建当最后候选；正式M5需固定提交、预封存Plan、全回归、构建及新鲜安装页面读回和公开门禁。以上均不称M5已资格闭合。
