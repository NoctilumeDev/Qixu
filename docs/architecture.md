# M1 架构与基础链路合同 0.1

本文件开头保留M1实施前冻结合同，前置文档事实为 `main@45241acbb71f4861c925468a56a69dcfd73e0530`；该坐标不是当前施工状态。M1基础资格见[M1记录](acceptance/m1.md)，后续扩展按下文对应阶段和[里程碑](milestones.md)区分；当前M0–M7限定资格已闭合，M8/M9未完成，M10等待用户参与。

## 范围、保护与退出条件

本阶段建立 Java 应用、MySQL迁移、独立演示数据、登录/会话、服务器权限、空间读取和基础构建。不得借已有账号、用户角色字符串、浏览器状态或外部标签授予期序审批权。保护现有 MySQL 与青野服务，使用独立 schema / 应用账号 / 端口。

不在 M1 偷做预约、分配、场地批准和活动报名。这些依 M2–M4 的事务合同实现；不能预建一个万能预约状态表当作领域完成。

退出条件：固定依赖干净构建；真实 MySQL 中迁移/登录/失效/权限/空间查询闭合；错误密码、失效会话、学生越权与跨范围管理员拒绝；demo 关闭后不再产生演示身份。保留实际数据库隔离、请求、最终行、构建与验迹证据。

实际启动发现端口已监听但 ApplicationRunner 未提交时，原健康查询提前称 READY；这是产品就绪生命周期缺口。补充就绪合同：所有 API 在应用完成初始化、进入 ACCEPTING_TRAFFIC 前返回503 APPLICATION_NOT_READY，停止接收期间也相同；连接正常不等于完整应用已就绪。保留首次启动反例，按同一标准验证503到200及随后的正常登录。

## 部署与版本

增强型单体：uni-app 学生端、Vue 管理 Web、Spring Boot Java、MySQL8。核心无 Redis/MQ 依赖；通知通过事务内 Outbox 与可重放分发，不让通知网络决定使用权。

Java17，Maven3.9；后端选官方当前稳定 Spring Boot4.1.1、Spring JDBC、Flyway、Jackson3、Validation 与 BCrypt。并非复制暗室3.5代码。官方[系统要求](https://docs.spring.io/spring-boot/system-requirements.html)确认Java17可用；3.5.16已结束OSS支持，故新项目采用4.1稳定线。实际依赖树和构建锁入证据。

M1初始选型曾拟用DCloud发布组 `3.0.0-5020620260917001` 和两端独立lockfile，该方案保留为历史计划。M5实际实现锁定学生端DCloud `3.0.0-5020420260813003`、Vue3.4.21、Vite5.2.8、types3.4.31；管理端Vue3.5.43、Vite8.3.2，两端TypeScript5.9.3。当前根目录的单一workspace [package-lock.json](../package-lock.json)管理共享客户端与两个前端，各workspace仍使用自己的依赖版本；随机验签器使用[randomness独立锁文件](../randomness/package-lock.json)。实际安装/构建见[M5记录](acceptance/m5.md)和后续frontend证据，入口见[运行说明](running.md)。

M1初始拟用 backend6967/admin6968/student-H5 6969；M5实际部署确定为 backend6967/student-H5 6968/admin6969，与运行指南一致。前一方案是历史计划，不是当前入口；启动前复核归属和占用。生产HTTPS、微信AppID与合法域名是部署参数；不复用青野AppID，不宣称小程序已经上线。时间输入含偏移，数据库统一UTC，界面Asia/Shanghai。

## 模块与事实

| 模块 | 自己拥有 | 对外只消费 |
| --- | --- | --- |
| identity | 本地主体、会话、授权版本、学生资格、空间管理范围 | 外部有效主体证明 |
| spaces | 楼层/区域/座位/场地档案、设施、平面图、开放规则、确认事实 | 经核实的反馈 |
| short-reservations | 短时请求、到场自报、取消/失效 | 空间与冲突规则 |
| preparation | 批次、资格/志愿快照、公开复算包、offer/候补/长期权 | 已验证固定未来随机源 |
| venues-events | 场地申请/绑定、一等活动、参与名额及候补 | 空间容量及冲突 |
| resolutions | Block、影响集合、替代窗口、处置/申诉 | 当前使用权，不静默覆盖 |
| feedback | 私有报告、维修事项、核实/复验记录 | 空间管理范围 |
| notifications-audit | 事务消息、恢复尝试、本人收件箱、审计 | 业务提交结果，不取得业务批准权 |

模块为包与事务边界，不拆微服务。后端是唯一业务权威，两个前端只投影结果。外部青野活动申请不导入其社团/报名事实；期序直接组织的活动才拥有自己的参与关系。

## 身份、会话与权限

本地主体用稳定ID；外部身份以 `(provider, subject)` 唯一绑定，禁止因数字相同或名称相同合并。外部有效身份不自动取得学生资格、组织权和审批权。Demo只能显式配置且禁止production启用，不悄悄创建默认管理员。

登录采用BCrypt；随机256-bit opaque session，只在数据库保留token摘要、期限、主体授权版本与CSRF值。每次请求重新核对主体active/auth_version，不缓存角色为最终授权。学生小程序使用Bearer；管理浏览器使用HttpOnly SameSite cookie，cookie写请求还需session绑定的CSRF token。允许Origin显式配置，不使用通配凭据CORS。原始token、密码、Authorization不进入日志/审计/公共证据。

错误密码统一401，失效401、权限403、缺资源404、状态/版本/幂等冲突409、校验422、依赖不可用503。返回稳定业务code和requestId，内部SQL/栈不暴露。基础登录限速按请求来源和归一账号记录，不能只靠前端禁按钮。

老师默认组织本人活动；管理员受floor范围约束，范围审批与活动组织权限分别判断。学生只读本人敏感关系/反馈。管理员不是“任意SQL”身份；权限必须位于接口与事务内，不以导航隐藏代替。

## 事务与锁序

MySQL InnoDB，业务事务显式 `READ_COMMITTED`。不复制暗室的RR选择：期序通过物理floor guard覆盖空间层级冲突，RC在等待guard/用户锁后读取当前已提交状态，避免事务早期快照隐藏另一floor的个人时间冲突。

共同顺序：涉及的批次/活动协调行（需要时）→ 全部受影响floor行按ID升序 → 全部主体行按ID升序 → 业务行。跨floor替代/换绑定先收集所有floor，再取任何用户/预约锁。普通预约不逆向取得批次或活动锁。不能持有floor/用户锁再等待另一个协调行。

同一floor内的父空间、子区域、座位和教室冲突由同一guard串行化；个人重叠通过主体锁串行化跨floor事务。它不是进程内mutex，实例间仍依数据库；但多实例必须另行实际验收，不能仅从设计宣称成立。

管理员结构编辑在有关floor锁内，带version检查。业务唯一约束作为最后防线，条件更新必须检查影响行数。幂等以actor+key+operation+规范化body摘要绑定，正文不同409；事务结果与幂等回执同提交，未知响应按key查询，不再造一单。

数据库deadlock/暂时失败明确分类并有限重试同一请求；不能重抽随机种子。外部身份/随机源网络与图像处理在锁外进行，提交前重检坐标/期限/授权，不能在锁内等待网络。

## 随机与公开可复核边界

固定quicknet chain `52db9ba70e0cc0f6eaf7803dd07447a1f5477735fd3f661792ba94600c84e971`，官方查询得到period3秒、genesis1692803367、公钥和scheme `bls-unchained-g1-rfc9380`。这段是M1技术选型观察，本身不证明分配；后续实际未来round、验签与复算的限定资格见[M3](acceptance/m3.md)、[M6](acceptance/m6.md)。

M3必须在冻结前绑定未来round，固定canonical inputs/algorithm/来源坐标；验证BLS签名、公钥/chain/round及randomness=SHA256(signature)后才能生成正式结果。普通TLS返回和hash相等不足以代替签名。禁止客户端提交任意seed，禁止latest代替future坐标，源不可用等待同一round。

通过 `RandomnessProofVerifier` 边界消费审计过的公开客户端/库；不手写密码学。M3在使用前固定其运行形态/版本/摘要、错误与预算，证明伪签名拒绝及真实beacon可验。Java/native候选与官方Node客户端尚需该阶段实测，未定且未启动分配；不能带着未验证证明器宣布公平分配完成。

## 资源与证据

数据库schema `qixu` 与验收schema `qixu_test` 分离；应用账号不获其他库权限。密码仅本地忽略配置。迁移逐阶段追加、不修改已生效历史；演示图片/测量明确标记DEMO。

M1项目原生测试按真实MySQL执行，不以H2代表隔离/锁事实。CI编译与数据库测试分开命名，失败/跳过/未观察各自保留。验迹消费明确的事实，不把测试“总PASS”作为唯一不变量。生产容量、未观察外部身份/微信行为仍NOT_PROVEN。

## M7候选 · 恢复后的授权隔离

单实例业务commit前强制持久库外PREPARE并同事务写recovery_marker；确定结果后写终态。启动完整核对generation、规范基线、marker及独立链，不猜测未知提交。NOT_RECONCILED阻断API/身份采纳/任务及demo初始化，保留原外部见证和请求键。正常域锁序不由全库锁替代。实际数据库/受控框架/物理停止后的逻辑恢复三层见[M7恢复](acceptance/m7-restore.md)，不升级为多节点或生产灾备。

## M7候选 · 实际数据库预算

Hikari取得连接时限不约束已借出查询。候选实现通过实际Connector/J解析器校验单主机有效属性，固定connect/socket及pool时限，新连接初始化SESSION锁预算；真实owned floor等待与借出COM_QUERY故障分别复验，见[预算合同](contracts/m7-database-budgets.md)及[原始事实](acceptance/m7-database.md)。这只给这些等待边界，不声称整个请求累计有30秒SLA；通信未知仍不能推断COMMIT回滚。
