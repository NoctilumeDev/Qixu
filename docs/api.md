# HTTP API合同 0.1

状态：M1–M6已取得各自限定资格，API/MySQL、两端浏览器和外部接入的具体证明边界见acceptance记录；M7–M10、真机及全工程交付尚未完成。

## 统一语义

前缀`/api/v1`；健康为`/api/health`。时间ISO8601含偏移；ID为服务器整数，不作为权限凭据。响应成功`{data,requestId}`；错误`{error:{code,message},requestId}`及对应HTTP状态。分页`page=1,size<=50`、总数与筛选均在服务器约束，排序白名单。

写业务请求使用`Idempotency-Key`，版本编辑有`version`。同key同操作同body返回既有业务结果；同key不同意图409。`GET /receipts/{key}`仅actor本人可读提交回执。敏感图片走授权下载；公开demo品牌图片另路由。

M2的行式响应保留snake_case数据库字段，追加展示字段为camelCase；写DTO固定camelCase，不接受任意表字段。收到成功结果包含`receipt:{key,operation,acceptedAt,status:COMMITTED}`；GET receipt另含result。JSON摘要由固定DTO字段序列生成（原意图，包括时间偏移表示），不是任意Map或客户端提交摘要。语义相同但表示不同的正文也不得改用已提交键；重放保留原DTO。

## M1端点

| 方法/路径 | 主体与结果 |
| --- | --- |
| GET /api/health | 无凭据；只返回应用就绪，不透露库地址/密码 |
| GET /api/v1/auth/options | Demo开关为真时返回演示身份摘要；生产关闭 |
| POST /api/v1/auth/login | username/password；Bearer客户端显式mode，浏览器默认cookie；返回主体/CSRF，Bearer仅返回token |
| GET /api/v1/auth/session | 当前主体、权限投影、cookie会话CSRF；不返回token摘要/密码 |
| POST /api/v1/auth/logout | 只撤当前会话；过期不扩大删除范围 |
| GET /api/v1/floors | 可见楼层/区域；不把管理范围当成公共档案过滤 |
| GET /api/v1/spaces | floor/kind/tag/search分页；未知设施显式unknown |
| GET /api/v1/spaces/{id} | 档案/图坐标/图片来源/设施和规则，不泄露当前使用者身份 |
| GET /api/v1/admin/scope | 当前管理floor列表；无管理身份403，用于验证服务器边界 |

## M2端点

| 方法/路径（均在 /api/v1 下） | 内容 |
| --- | --- |
| GET /booking-rules | 服务器时间、上海时区、8–22开放、4小时窗口/上限和10分钟宽限 |
| GET /spaces/{id}/availability?start=...&end=... | UTC时段、asOf、层级权利冲突，非物理有人/无人；不透露拥有者 |
| GET/POST /reservations | 本人最近100记录；Create={spaceId,startsAt,endsAt} |
| GET /reservations/{id}；POST /reservations/{id}/actions | 本人详情及effectiveStatus；Change={version,action:CHECK_IN/CANCEL/END} |
| GET/POST /venue-requests；GET /venue-requests/{id} | 本人场地申请；Create={spaceId,startsAt,endsAt,people,purpose,description,contact} |
| GET /admin/venue-requests | 管理范围内最近200申请，不表示无限分页能力 |
| POST /venue-requests/{id}/actions | {version,action:APPROVE/REJECT/CANCEL,reason}，审批/本人取消权限分别检查 |
| GET /events；GET /events/{id} | 已公开/取消活动、阶段、剩余量和本人参与；草稿仅组织权限可读 |
| GET/POST /organizer/events | 本人草稿/活动；Create含venueRequestId/title/eventType/speaker/description/notice/capacity/opensAt/closesAt/promotionUntil |
| POST /organizer/events/{id}/actions | {version,action:PUBLISH/REBIND/CANCEL,venueRequestId?,reason}；换地保留参与，已开始不得重绑定 |
| POST /events/{id}/participation；GET /participations | {action:JOIN/CANCEL}；固定队列，本人参与/候补 |
| GET/POST /favorites | 本人最多6项；{spaceId,selected}，对比读取真实公开档案 |
| GET /inbox；POST /inbox/{id}/read | 站内持久消息/未读量，本人已读；没有微信发送声明 |
| GET /receipts/{key} | 本人已提交收据，404不是在途请求未发生的证明 |

## 后续端点与权限（M3–M4）

| 领域 | 读 | 写与约束 |
| --- | --- | --- |
| 收藏/对比 | 本人收藏与公开档案 | 本人收藏；有限数量对比 |
| 短约 | 指定时段可用性、本人记录 | 预约/到场/结束/取消；只有本人，服务端时限与冲突 |
| 场地申请 | 本人/负责空间管理员 | 提交/撤回；范围内批准/拒绝，批准检查层级影响 |
| 图书馆活动 | 公开已发布列表/详情；本人参与与候补 | 组织者创建/修改/发布，范围审批；学生报名/退出，不超卖名额 |
| 备考 | 公开批次/规则、本人资格/志愿/offer/候补解释 | 窗口内申请/志愿/确认/退出；管理配置/资格核实/冻结/确定性分配 |
| 公开复算 | 匿名输入/来源原文/算法版本/结果 | 不暴露主体映射和原始资格材料；不存在改seed接口 |
| 占用与处置 | 指定范围影响清单、本人受影响通知 | 范围内Block/Resolution/替代/到期恢复；版本和当前影响重检 |
| 反馈 | 本人和负责管理员；公开仅确认事实 | 报告/补充/核实/维修/复验/重新打开；私有图片受同权限 |
| 治理/申诉 | 本人处置/申诉；范围管理员处理 | 明确理由/期限/证据；不从签到/无人推导收权 |
| 消息/审计 | 本人收件箱，范围内审计 | 已读/恢复任务，不授予或撤销业务权 |

API实现不能接受客户端actor/role/scope授权值。请求正文、响应字段与实际端点在各阶段提交中对齐；缺端点用未实现状态，不提供假的成功按钮。
# M3新增接口（施工候选，资格见acceptance/m3.md）

- `GET/POST /api/v1/preparation-batches`：公开规则/池查询，创建仅池内全部楼层管理员。
- `GET /api/v1/preparation-batches/{id}`：批次期限、池、完整正式结果摘要；候选不是正式结果。
- `GET/POST /api/v1/preparation-batches/{id}/application`：仅本人当前/冻结版本、结果、要约/权和稳定候补；提交版本与可接受seat/rank及保留更高志愿候补选择。
- `POST /api/v1/preparation-batches/{id}/withdraw`：申请期本人撤回，正文Action=`{version,action:"WITHDRAW"}`。
- `POST /api/v1/preparation-batches/{id}/actions`：仅范围管理员FREEZE/ALLOCATE，不提供改中签者/种子动作。
- `POST /api/v1/long-offers/{id}/actions`：本人ACCEPT/DECLINE、预期offer版本；过期投影立即失效，原成功回执可恢复。
- `POST /api/v1/preparation-batches/{id}/exit`：本人EXIT、预期application版本，结束要约/候补/旧权并保留历史。
- `POST /api/v1/preparation-batches/{id}/waitlist-exit`：本人EXIT_WAITLIST、预期waitlist版本，仅退出候补并取消未确认升级，不丢已有席位。
- `GET /api/public/batches/{publicUUID}/verification`：唯一新增匿名GET，只给原始输入字节/摘要及已正式发布的证明/输出字节；不含姓名、学号、内部user/application映射。

所有写仍需当前会话、范围、同正文请求键和锁后时限。Clock用UTC，界面按Asia/Shanghai展示。查不到收据不能证明在途请求没有提交。

## M4a 反馈接口（已实现，资格见acceptance/m4.md）

均在/api/v1下，写入需要Idempotency-Key；本节及后续治理均为当前实现，阶段资格单独记录。

| 路径 | 内容及权限 |
| --- | --- |
| GET/POST /feedback；GET /feedback/{id} | 本人列表/原始报告与处理历史；Create={spaceId,category,description}，有效学生提交 |
| POST /feedback/{id}/supplements | 本人{version,action:SUPPLEMENT/REOPEN,message}；追加不改原文 |
| POST /feedback/{id}/attachments | 本人multipart file；PNG/JPEG实际内容≤1MiB且≤400万像素，最多3张 |
| GET /feedback/{report}/attachments/{id} | 本人或该空间管理员；private no-store二进制，无公开直链 |
| GET /spaces/{id}/facts | 仅核实后的key/value/verifiedAt；无报告、提交人或照片 |
| GET /admin/feedback；POST /admin/feedback/{id}/actions | 当前管理范围；{version,action:ACKNOWLEDGE/VERIFY/NOT_REPRODUCED/REJECT,reason,facts?} |
| GET/POST /admin/repairs；GET /admin/repairs/{id} | 范围内维修；Create={reports:[{id,version}],description}，同空间/同类已核实反馈 |
| POST /admin/repairs/{id}/actions | {version,action:ASSIGN/WORK_DONE/VERIFY/LINK_REPORTS,reason,assignee?,verified?,facts?,reports?}；只有WORK_DONE且复验true可关闭 |

分类为OUTLET/LIGHT/DESK/ENVIRONMENT/INFORMATION/OTHER；facts只接受window/outlet/quiet/accessible明确boolean，及outletCondition/lightCondition/deskCondition/environmentCondition枚举UNKNOWN/WORKING/BROKEN/REPAIRING。未核实不发布；复验失败不改为已修复事实。列表显式分页50项及total。

## M4b 空间限制接口（已实现，资格见acceptance/m4.md）

| 接口 | 当前语义 |
| --- | --- |
| POST /admin/space-blocks/preview | Plan={spaceId,kind,startsAt,endsAt,reason,venueRequestId?,venueVersion?,resolutions?}。完整授权影响、服务器impactKey、摘要、通知人数；观察不授权 |
| POST /admin/space-blocks | 同一Plan加impactHash。Resolution={impactKey,action,targetSpaceId?,replacementVenueId?}；每个非POOL影响必须明确处置。改完选项须重新preview，旧摘要409 |
| GET /admin/space-blocks/{id} | 范围内私有影响、来源、临时记录和历史；不对学生公开权主 |
| POST /admin/space-blocks/{id}/revoke | {version,reason}。一般限制只撤此来源；EVENT_BOUND要求由其场地/活动变更或取消 |
| GET /spaces/{id}/limits?startsAt=...&endsAt=... | 公开当前有效限制类型、窗口和理由，不返回原权主和原始私有报告 |
| GET /long-offers/{id}/impact | 仅本人；原归属、每段实际位置/临时不可用、impactHash、知情确认要求 |
| POST /long-offers/{id}/actions | ACCEPT在当前存在影响时增加impactHash；旧摘要409，原期限不变；无影响兼容M3旧body与幂等指纹 |

来源MAINTENANCE/SAFETY允许明确UNAVAILABLE；COURSE/EVENT不能用无替代覆盖长期权。EVENT计划精确绑定SUBMITTED场地申请、版本和窗口，完整处置与场地批准同事务。学生报名继续使用既有活动端点，活动取消只关闭自己的来源。短约MOVE关闭原记录并生成新记录/typed来源链，不延长到场期限；长期TEMPORARY留原归属，不改正式分配结果。

关联层协调、最大资源/片段/通知数、循环和新坐标停止边界见[实施细则](contracts/space-impact.md)。V5已在隔离库迁移，原始施工失败和恢复见[错题记录](failure-notebook.md)。治理及维修来源联动已追加实现，完整阶段资格仍依exact候选/主线门禁与观察。

V6施工补充：Block撤销和场地/活动取消、换地保留原通知对象，追加关联批次当前申请人；第二条站内outbox写失败整笔回滚，外部推送延迟另行重试。COURSE/EVENT在最终写入仍须早于开始时刻。相同key/body恢复历史回执，不因为临时目标后来停用而重做迁移。V6本身不证明后续维修来源联动或治理；后续V7/V8见下文。

## M4 V7/V8 · 维修来源与明确治理

- `POST /admin/repairs/{id}/limits`：{version,blockId,blockVersion,reason}；只关联同精确空间/层的未结束MAINTENANCE/SAFETY，绑定后不能暗换。复验成功只撤自己来源；同事务处理事实、报告、通知和回执。
- 复验既有BROKEN/REPAIRING必须明确WORKING；插座修复还须outlet=true。不能用UNKNOWN擦除已知损坏，也不能缺事实即宣称闭环。
- `GET /governance-cases`、`GET /admin/governance-cases`、`GET /governance-cases/{id}`：本人私有或当前原空间管理范围；分页50，其他学生404。私有依据/陈述不进入公共档案或复算包。
- `POST /admin/governance-cases`：{entitlementId,entitlementVersion,reasonCode,evidence,statementUntil}。四种明确理由见实施合同；至少24小时、最多7天陈述窗口，原权保持。
- `POST /governance-cases/{id}/statements`：本人{version,message}，仅严格截止前NOTICE，追加历史。
- `POST /admin/governance-cases/{id}/decisions`：{version,action:DISMISS/REVOKE,reason,penaltyUntil?}。收回须过陈述期、原权仍有效/版本一致；可附至多30天LONG_APPLICATION处罚，不禁用账号/普通预约。
- `POST /governance-cases/{id}/appeals`：本人{version,message}，原裁决后严格7天内一次。
- `POST /admin/governance-cases/{id}/reviews`：{version,action:UPHOLD/OVERTURN,reason}。不同裁决人的当前范围管理员；原空闲权恢复或CORRECTED_UNAVAILABLE，不抢别人合法递补、不改正式结果。
- `POST /preparation-batches/{id}/cancel`：{version,action:CANCEL/CANCEL_UNUSED,reason}。前者仅OPEN/FROZEN；后者已发布且无任何持权历史、无当前有效OPEN要约。保留冻结/正式结果/回执；不是批量收回旁路。

当前资格在提交/冻结/发布/确认/递补校验，`GET .../{id}/application`的已申请视图含currentEligibility。处罚自然到期不靠任务；原提交回执可按原actor/key恢复。所有时间输入含offset且精确到秒，响应秒以下精度属于真实Clock读值，客户端不能直接复制成业务输入。

## M5 两端读取与未知意图恢复（限定资格见acceptance/m5）

| 接口 / 字段 | 当前语义 |
| --- | --- |
| GET /preparation-applications?page=1 | 当前主体历史申请分页20条，页码1–10000；不从最近公开批次推断本人历史，忽略客户端userId授权主张 |
| GET /admin/space-blocks；GET /admin/long-entitlements | 当前管理楼层范围分页，page/size/floor，size≤50；老师和学生403。只投影合同白名单，不提供直接改权 |
| GET /admin/operations；GET /admin/audit | 可归属当前管理范围的通知统计与审计；未知实体不回退到全校。审计不返回detail_json/凭据 |
| POST /receipts/{key}/stop | 仅当前主体，同用户锁内返回原回执，或持久化client.intent-stop屏障；不会撤销已成立业务，迟到原请求同键409 |
| Cookie GET上的X-CSRF-Token | 除auth/session冷启动核对外，携带旧绑定与当前Cookie不同则409 SESSION_OWNER_CHANGED；无绑定传统读取仍按当前Cookie授权，不获得旧文档投影资格 |
| 短约/场地/反馈/维修授权投影code/name | 授权之后追加公开空间编号/名称及必要关联；历史回执不重写，其他主体记录不可枚举 |
| POST /admin/repairs/{id}/actions 的 reports | 仅LINK_REPORTS消费非空引用。其他动作允许缺省/空列表，非空明确422，不静默关联；安排、工作完成不等于复验关闭 |

前端回执404仍为未确认。只有原事实或原子停止屏障才能结束原意图；本地清除、GET成功和单次4xx均不代替该裁决。端点保护与两端真实页面资格分别记录。

## M6 外部身份适配（限定范围）

POST `/auth/external/dark-room`：{ticket,mode:COOKIE/BEARER}，默认COOKIE；只消费当前固定上游身份，不接受URL/subject/userId/role/scope。不存上游密码。有效身份还必须有当前启用的本地绑定、issuer/version及外部专用本地主体；未登记403、无效401、依赖未知503、入口预算429。成功响应与本地登录同形，仅BEARER含期序opaque token。

每次外部认证在事务外有界核验，写入取锁后复核当前本地会话/绑定与短证明；503不授权也不改变原请求键。Cookie仍受Origin/CSRF/文档所有权约束，Bearer拒绝不退回Cookie。GET/auth/options不列外部专用或停用账号；生产无demo时该接口404。详细协议、当前边界及实际观察见[外部合同](contracts/external-identity-and-recovery.md)和[M6证据](acceptance/m6.md)。
