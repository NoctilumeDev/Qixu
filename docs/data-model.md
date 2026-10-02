# 数据模型与迁移边界 0.1

状态：M1候选。真相存MySQL InnoDB；业务时间UTC，呈现上海时区。JSON仅用于设施/展示画像、冻结快照及回执，不用JSON中的字符串状态替代有索引的业务约束。

## M1迁移

- `identity_user`：username、BCrypt、display_name、role、active、student_verified、auth_version；没有默认生产账号。
- `external_identity`：provider/subject唯一及本地主体引用；不按账号名称匹配。M1建身份边界，M6才实现外部交换。
- `auth_session`：token_hash唯一、user_id/auth_version/expires_at/csrf_token，原token不落库。
- `login_attempt`：归一账号及请求来源摘要的限速窗口与失败数，不存原始密码。
- `floor`：位置、图范围、version；同时是物理层级冲突guard。
- `space`：floor/parent/kind/code/name/capacity/use_mode/version、平面图坐标及profile_json；floor+code唯一，parent必须同floor且无环。
- `admin_scope`：actor+floor唯一，角色与范围都有效才可审批；老师组织权不等于管理范围。
- `audit_entry`：actor、action、entity、version/requestId、最少必要说明；不保存密钥或全文私有反馈。

演示初始化与生产迁移分开，空间画像source=DEMO。真实空间只由授权管理员录入，不从生成图推断容量/位置为现实事实。

## 后续独立事实表

M2 V2已实现独立short_reservation、venue_request/venue_entitlement、campus_event/event_participation、favorite_space、idempotency_receipt、notification_outbox和inbox。confirmed_count在event协调行内更新且有CHECK防超额；参与event+user唯一和数据库序号，短约区间仍依floor/user guard，不误称SQL唯一索引能排除时段重叠。M3 V3已迁移并取得限定主线资格；M4 V4反馈表族在隔离库施工验证，Block/治理表尚未实施。下面未带实际迁移号的项仍为规划。

| 阶段 | 表族 | 硬边界 |
| --- | --- | --- |
| M2 | short_reservation / venue_request / venue_entitlement / event / participation / idempotency_receipt / notification_outbox / inbox | 名额/独占权/待审分别存；有效冲突在floor+user锁内计算；确认计数与队列序号在event协调锁内变更 |
| M3 | preparation_batch / application / preference / frozen_input / allocation_result / offer / waitlist_entry / seat_entitlement / allocation_event | batch正式结果唯一；跨batch用户重叠在user锁内校验；不把没人坐解释为可分配 |
| M4 | space_block / impact_resolution / temporary_entitlement / feedback_report / private_attachment / maintenance_issue / verified_fact / governance_case / appeal | 原长期权保留；替代时段独立；Report与Fact分离；私有材料不可公开静态直链 |

基于半开区间`existing.start < new.end AND existing.end > new.start`并过滤有效生命周期，时间精度统一。调度清理只是投影/存储治理，时限授权在查询与写事务直接判断，不能依赖定时任务恰好运行。

MySQL没有通用区间排除约束；不能谎称普通唯一索引能排除任意时段重叠。floor/user协调锁、当前读取、索引与提交后事实验收共同保证。所有读取/写入参数化，管理编辑采用version条件更新。

未来迁移每个阶段追加独立V编号，实际SQL与接口、关键不变量和验收坐标同步；本表族规划不是已实施schema清单。

## M4 V4 · 报告、核实与维修

feedback_report保留不可覆盖原文，feedback_history追加各次补充/核实/重开；feedback_attachment存私有受限原始位图，不存路径或外部URL。repair_ticket与repair_history分开记录工作完成/复验；repair_report复合FK确保关联同一个空间和楼层，同类只能一个未闭维修。space_fact只由明确核实/修复复验产生，保留依据引用；公开只投影最新值/核实时点，私有来源不公开。设施画像在同一floor guard内有版本更新。

V4已实际迁移到qixu_test，后续不改旧SQL；新Block/治理结构追加迁移。尚未宣称现实维修、整个M4验迹或页面资格。
