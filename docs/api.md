# HTTP API合同 0.1

状态：M1基础已验收；M2后端端点已实现并进入真实MySQL候选验收；M3–M4仍是规划，不能假装可调用。

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
