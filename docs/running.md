# 本地运行与验收

M1–M9限定资格见各阶段acceptance；[M9](acceptance/m9.md)两次fresh工程复现与预发行版已完成，M10等待用户参与。当前阶段见[里程碑](milestones.md)，M8主线闭合见[PR #25](https://github.com/NoctilumeDev/Qixu/pull/25)与[M8记录](acceptance/m8.md)。以下说明用于启动与复现，不授予当前提交新的安装、生产或微信真机资格。

## 当前维护入口与阶段收尾

从仓库根目录运行 `python -B scripts/check.py <group>`。先按下文安装锁定依赖、准备工具链和独立测试库；入口只执行检查，不自动下载工具、不建库、不删除文件。

| group | 责任 |
| --- | --- |
| `hygiene` | Git已跟踪的已知缓存/运行态/私有配置、当前文档本地链接与门禁负控制 |
| `docs` | 原结构检查与正式Bundle/捕获原字节校验 |
| `frontend` | 实际客户端测试、两端类型检查、H5/微信包/管理端构建 |
| `backend` | 固定随机验签器测试、独立MySQL的clean verify |
| `all` | 上述四组顺序执行，任一失败即停止 |

CI复用同一入口。历史`veritrail_*`脚本仍按原阶段/producer合同复验，不再把它们当成日常维护菜单；已封存合同和Verdict不改。格式维护使用Prettier 3.6.2（单引号、100列）与google-java-format 1.24.0（保留import顺序/未使用import/长字符串/Javadoc），先看diff再验证，不格式化artifacts或冻结合同。

重要阶段退出顺序为：实现 → 测试/资格 → 文档与公开读回 → 遗留物收口 → 关闭阶段。`hygiene`是其中可自动检查的仓库部分，不能自动授予LOCAL_DORMANT。阶段负责人还须核对：

- 图片：当前页面、主页、Gallery/Release或正式manifest仍依赖的保留；只有历史设计/迭代职责且无当前消费者的才可删除。找不到引用不等于确认无用途。
- 证据：首败、关键反例、最终见证、正式回执和原绑定包保留；中间迭代只在没有证明职责时收缩。历史Plan不能换绑重建产物。
- 构建/环境：可重建的本轮实例可清；Release原件和evidence绑定原件由对应交付/证据入口保留。先确认源码、锁文件、迁移、脚本已在远端，唯一数据/账册另留存，再停止并清理本人创建的实例。共享环境与其他项目不动。
- 记录：通常只在阶段记录留检查范围、结果、必要例外；Git diff已记录删除，不再建立永久删除SHA清单。没有仓库大小指标。未检查或未知项标记REVIEW_REQUIRED，不写PASS。

## 工具与隔离

Java17、Maven3.9、MySQL8；固定验收环境使用MySQL8.0.44。开发库 `qixu`，测试库 `qixu_test`；两套应用账号只有本库权限。迁移以[Flyway V1–V10源码](../backend/src/main/resources/db/migration)为准，启动自动执行并校验，默认不允许clean；不要另导入拼接SQL或改已执行迁移。MySQL全局隔离不改，应用连接与事务显式READ_COMMITTED。

已拥有独立库时配置自己的数据库地址/账号/密码。首次创建可运行 `python scripts/provision_local.py --mysql <mysql可执行文件>`，先通过环境变量提供 `QIXU_ADMIN_USER` / `QIXU_ADMIN_PASSWORD`。脚本遇到已存在目标库或账号拒绝，不覆盖或删除；生成凭据仅保存于忽略的 `.tools/database.local.json`。生产账号和权限由部署方单独设置。

建库脚本的CREATE/GRANT不是一个原子事务；中途失败时可能已留下部分库或账号。停止自动重试，由操作员核对已创建对象与归属后处理，不以删除已有库来让脚本通过。测试会清理专属测试库业务数据，不能让其他运行实例共用该库；开发数据与验收数据必须分开。

## 启动

在 IDE 运行 `dev.noctilume.qixu.QixuApplication`，或在 backend 下执行 `mvn spring-boot:run`。环境变量见 [模板](../backend/.env.example)；模板不会自动加载。必须提供 `QIXU_DB_PASSWORD`，按实际库设置 `QIXU_DB_URL` / `QIXU_DB_USERNAME`。

先在仓库根目录安装前端workspace和独立随机验签器的锁定依赖。根目录`npm ci`不会安装randomness的依赖：

```powershell
npm ci --no-audit --no-fund
npm ci --prefix randomness --ignore-scripts --no-audit --no-fund
```

使用锁定的Node24.14.x/npm11.9.0。IDE或Maven的JVM工作目录通常是backend，必须将`QIXU_RANDOM_VERIFIER`设为本项目`randomness/verify.mjs`的绝对路径，并保证`QIXU_NODE`指向实际Node可执行文件。例如在仓库根目录取得路径后，将这些值加入IDE运行配置或当前启动进程环境：

```powershell
$env:QIXU_RANDOM_VERIFIER = (Resolve-Path -LiteralPath 'randomness/verify.mjs').Path
$env:QIXU_NODE = (Get-Command node -ErrorAction Stop).Source
```

验签器缺失或固定公开源不可用不会授权使用本地随机种子；不要通过改种子、关闭验签或重开原批次来恢复。

显式 `SPRING_PROFILES_ACTIVE=demo` 才创建虚构空间与演示身份：student1/student2、teacher1、admin1/admin2，演示口令均为 `qixu-demo`。生产 profile 禁止同时开启 demo；默认配置不创建演示管理员。

演示模板显式关闭cookie Secure仅用于本机HTTP；默认配置为Secure，部署管理端需要HTTPS。非演示环境没有自动开户/首个管理员界面；外部绑定CLI也要求已有期序本地主体。部署方须准备本地身份、资格与管理范围，不能把开启demo当作正式初始化。真实空间、身份和微信平台接入不由示意素材或演示账号证明。

默认只监听 127.0.0.1:6967，启动前确认端口归属；`GET /api/health` 返回就绪范围。小程序用 Bearer 登录，管理浏览器用 HttpOnly cookie 与绑定 CSRF。设置允许来源时使用完整 origin，不能用通配凭据。

## 两端施工入口

完成上述两套依赖安装后，在根目录运行`npm run test:client`、`npm run check`和`npm run build`。构建分别产生学生H5（`student/dist/build/h5`）、微信包（`student/dist/build/mp-weixin`）和管理Web（`admin/dist`）；构建不授予页面或真机资格。

后端正常启动后，`npm run dev:student`监听127.0.0.1:6968，`npm run dev:admin`监听127.0.0.1:6969，二者将同源`/api`代理到6967。端口占用时strictPort拒绝，不自动漂移或杀其他服务。共享客户端直接解析到`packages/client/src/index.ts`，避免workspace的node_modules链接缓存保留旧代码；跨合同更新后从冷启动核对实际页面。

微信开发者工具导入`student/dist/build/mp-weixin`，设置本项目自己的AppID；不复用青野AppID。原生端通过构建变量`VITE_API_ORIGIN`或设置中的`qixu.apiOrigin`配置部署的HTTPS服务origin（不含路径），并配置平台合法域名；默认不关闭域名校验。H5开发代理不代表微信手机可连接，认证、真机键盘和订阅消息另行验收。

## 原生验收与验迹

独立测试库配置 `QIXU_TEST_DB_URL` / `QIXU_TEST_DB_USER` / `QIXU_TEST_DB_PASSWORD`，URL必须是qixu_test或CI临时qixu_ci，缺配置直接失败，不替换成H2。在backend目录执行`mvn -B -ntp -Pmysql-it clean verify`启动实际HTTP服务、运行真实迁移和MySQL断言；测试JVM结束后服务停止。共享测试库与重型采集串行运行。

当前M8原生/构建入口如下，从同一干净提交的仓库根目录串行执行。`python`必须来自已安装[公开Core0.13.0及校验摘要](verification.md)的专用环境；不能使用未安装Core的默认Python，不能拿旧报告替代本次输出：

```powershell
python scripts/veritrail_native.py --stage m8 --maven <mvn可执行文件>
python scripts/veritrail_frontend.py --stage m8 --node <node可执行文件> --npm <npm可执行文件>
```

每次生成新的Plan、Evidence和Bundle身份；安装/浏览器采集必须绑定同一exact source的原producer、JAR与静态清单。PR #25已合入main@4c220626，当前frontend0.8/Plan4为61项；旧主线59项仅对应历史坐标，不混用producer。阶段资格还要求实际链路、独立角色、公开CI/主线读回，以上命令不单独授予M8退出。

下列M1–M6命令保留为历史阶段复现入口，其producer版本受采集器严格约束，不是将最新M8 Bundle改名传入即可复用。

安装公开 Core0.13.0 后，从干净提交运行 `python scripts/veritrail_native.py --stage m1 --maven <mvn可执行文件>`。采集器先封存 Plan，再执行 clean verify，不能读取旧报告冒充本次成功。私有原始日志和 Bundle 保留于 artifacts/local，每次有全新身份；仅规定范围的事实进入 Core，不声称生产容量或真实校园身份已获证明。

M2使用相同环境与命令改为`--stage m2`，同时验证基础及业务直接事实；可恢复的站内投递默认每5秒、每批最多32项，短约到期每轮最多100项。开发演示使用真实服务器时钟，夜间不伪造白天开放。测试另注入明确测试Clock、关闭后台调度，不将测试日期冒充真实设备时钟。

M3用`--stage m3`，另读Surefire离线与Failsafe真实HTTP/MySQL证据。运行前要求Node24及在randomness目录执行`npm ci --ignore-scripts`，依赖锁定drand-client1.4.2。IDE若工作目录为backend，需要把`QIXU_RANDOM_VERIFIER`设为项目root/randomness/verify.mjs的绝对路径；不会隐式下载安装或改用本地随机。初次验签/计算不持数据库锁，读不到固定未来round时保留原坐标，到公开截止明确失败。

已公布批次公开GET完整包后，可保存JSON并运行`python scripts/verify_allocation.py packet.json --node <node可执行文件>`。Python独立复算整批、校验声明的时间链及输入/输出原字节摘要；官方离线Node另验BLS。复算相同不单独证明服务器诚实冻结时点、生产容量或不存在管理员旁路，需配合冻结来源与项目验收证据。

调度线程池2：短约与通知、长期批次各有定时扫描。每轮最多2个冻结、2个精确轮次计算、10个期限失败/周期维护；旧任务始终在事务内读当前阶段和时点，任务延迟不延长期限。所有权利读由主库事务/当前时间投影，不靠某轮任务恰好运行。第一版无MQ依赖，MySQL outbox产生可恢复站内消息。

已构建的干净提交可运行`python scripts/veritrail_future_batch.py --producer-bundle <本提交native0.8原Bundle目录> --java <java> --node <node> --mysql <mysql>`。使用明确提供的QIXU_TEST_DB三项环境，只接受qixu_test/qixu_ci；会在6967启动并持有自己的demo JVM handle，现有端口占用直接停止，不杀占用者。默认不删任何业务数据；保留本次批次原文、正式结果、数据库计数与独立复算，停止自己的JVM。应先执行native清理验收，再执行live，避免上一轮池保护和申请仍参与下一轮；要用新候选重建事实时用新的原生验收身份，不能改旧批次/种子。

M4先运行`veritrail_native.py --stage m4`生成native0.10/M4Plan2原producer Bundle，再运行`veritrail_future_batch.py --stage m4 --producer-bundle <同exact源码原Bundle>`。保留所有环境隔离/端口归属约束；M3默认仍只接受native0.8/Plan8，不跨stage复用。M4实际安装额外检查报告/维修与正常Clock NOTICE，不冒称24小时真实流逝。

M5运行 `python scripts/veritrail_native.py --stage m5 --maven <mvn>`，随后串行运行 `python scripts/veritrail_frontend.py --node <node> --npm <npm>`。两者必须来自当前同一干净提交并保留原Bundle。构建采集器在独立git archive目录执行，不能把本机旧dist当成此次产物。

安装页面采样先运行 `python scripts/veritrail_browser.py seal <native采集目录> <frontend采集目录>`；绑定producer/JAR/静态清单后，在自有端口启动准确字节。`python scripts/serve_bound_frontend.py <frontend采集目录>`为H5/admin产物提供同源本地API代理，6968/6969占用时停止，后端须自行按上述环境在6967启动。辅助器不管理或终止其他进程，不用于公网。按新目录capture-contract采集真实DOM、PNG和URL/视口/UTC时间；维修拒绝额外保存DOM测量的错误可见性与焦点。全部采样后运行 `python scripts/veritrail_browser.py collect <browser采集目录>`，保留所有原始失败。实际资格还需真实动作及SQL读回、公开CI和新主线，不能只凑文案。

## M6 外部身份与隔离恢复

默认关闭适配。本地身份独立可用。启用需提供QIXU_DARK_ROOM_ENABLED=true、固定HTTPS的QIXU_DARK_ROOM_AUTH_URI及独立base64编码32字节QIXU_EXTERNAL_TICKET_KEY；端点必须是/api/dark-room-library/v1/user/auth。生产不允许明文，即便配置了loopback例外。显式本地集成才设QIXU_DARK_ROOM_LOOPBACK=true；不复用其他项目密钥，不提交环境文件。

由数据库操作员运行 `python scripts/enroll_external_identity.py --help`，显式指定provider主体、本地user-id、当前expected-auth-version、issuer-uri、action和原因。enable必须选择external-only，关闭该本地主体密码登录并提升authVersion；不自动建用户、不授予角色/楼层/学生资格。凭据只从QIXU_DB_USERNAME/PASSWORD读取。解除绑定不会重新开放密码；错误当前版本或已有他人归属拒绝。Web老师/管理员没有此CLI的数据库操作员授权。

外部调用方POST/auth/external/dark-room消费有效上游票据，mode为COOKIE或BEARER，取得期序opaque session。当前没有SSO交接，也不提供学生粘贴票据/上游密码的页面。外部会话每请求校验上游；停用401、依赖未知503，本地业务回执不改；正确登出不需要上游。票据只在加密会话字段保留，独立密钥丢失意味着外部会话不能再核验，部署方需保护密钥且让用户重新登录。

隔离恢复先运行 `veritrail_native.py --stage m6` 取得native0.12 / Plan1原Bundle，再运行 `veritrail_m6_installed.py --help`。要求同exact clean source的新JAR、原native清单、专属qixu_test/qixu_ci和qixu_darkroom_test、固定暗室d6e42a8专属检出及私有环境。QIXU_DARK_ROOM_TEST_URL/USER/PASSWORD/JWT_SECRET与QIXU_TEST_DB_*只用于隔离执行；SQL原文及schema-only字节转换摘要分别由QIXU_DARK_ROOM_SQL_SOURCE_SHA256/TRANSFORM_SHA256核对。预先建库权限归部署操作员，不授予采集器共享schema权限。

采集器0.3 / Plan3先seal，使用6970/6971，端口占用拒绝而不清其他进程；真实丢响应、同库重启、pending outbox重投、固定未来round恢复和干净独立复算。PENDING/失败保留在新identity，不覆盖历史。具体证据/未证明边界见[M6](acceptance/m6.md)，不是校园SSO、生产灾备或微信真机证明。

## M7 恢复隔离与部署边界

原T0与缺账册单故障已有[限定复验](acceptance/m7-restore.md)，[M7主线有限资格](acceptance/m7.md)已闭合，后续独立发现按M8处置。默认独立账册为进程工作目录`.qixu/transactions.journal`；部署应显式将`QIXU_RECOVERY_JOURNAL`指向MySQL数据目录及SQL恢复对象之外的持久绝对路径。文件与库内generation/marker配对，第二个进程不能共享该账册继续服务。禁止删除/裁剪历史后重新初始化，禁止把账册跟随旧SQL一起回滚。

新空库可首次初始化。已有V1–V9库的操作员在停写、保留备份与旧外部副作用边界后，显式一次性设置`QIXU_RECOVERY_BASELINE=ADOPT_PRE_V10_ONCE`；账册只留规范基线摘要，不保存个人正文。该值不能绕过已有generation的缺失/损坏账册。基线以前的通知/外部世界不因此被证明一致。

NOT_RECONCILED时健康与业务入口503，原键/凭据保留，演示初始化和调度不继续。恢复与账册相符的数据库再重启可以重新核对；不提供网页强制继续。整主机一起回退、特权SQL绕过、长时间账册轮换、多节点与灾备能力仍未证明。32MiB/200,000事件硬预算耗尽会隔离，不截断历史；现阶段需监控账册大小，不能视作无限运行容量。

当前`--stage m7`生成native0.19/Plan7（M7主线记录为173项），`--stage m8`生成native0.22/Plan3（既有记录为181项）；原native0.18/Plan6的170项是历史坐标。专属测试库账册持久保存于`.tools/runtime/native-<schema>`。真实恢复运行`python scripts/veritrail_m7_restore.py --producer-bundle <同exact source的原native Bundle目录> --java <Java17> --mysql-bin <MySQL8.0.44 bin>`，具体接受的stage/Plan/collector组合以[m7_producer_binding.py](../scripts/m7_producer_binding.py)为准，必须保留原PASS、完整清单及同源新JAR。它只初始化本轮新数据目录和6975/6976独占实例，保留T0/T1/独立world见证，宿主3306不动。restore0.7/Plan7保留22个业务标准；测试成功、页面资格和最终Release分别判定。

## M7数据库等待预算

单主机Connector/J连接固定connectTimeout=3000ms、socketTimeout=30000ms，Hikari取得连接3000ms、validation2000ms，每条新连接设置SESSION innodb_lock_wait_timeout=10s。启动前用实际驱动解析有效host属性，拒绝多主机、主机级超时覆盖、零/负/延长预算及缺失锁初始化；不是全API累计时限或生产SLA。URL不可自行放宽预算，生产TLS设置不受故障夹具明文配置影响。

F15运行`python scripts/veritrail_m7_database.py --producer-bundle <同exact source的原native Bundle目录> --java <Java17> --mysql-bin <MySQL8.0.44 bin>`，使用上述producer绑定规则：只用本轮新MySQL6976、app6975和loopback relay6977，固定driver9.7.0/pool7.0.2，真实floor锁及已有业务连接的COM_QUERY回应丢失。15/35秒是观察窗口；同源原25标准不靠重启或改幂等key通过。端口已有监听立即停止，绝不接管共享实例。首次FAIL、观察器PENDING和主线限定复验见[M7数据库事实](acceptance/m7-database.md)。

## M9工程候选与本机休眠

实际两轮源码f0ef8e0、原验迹与边界见[M9](acceptance/m9.md)，复现步骤见[m9-reproduction](m9-reproduction.md)。[工程预发行包](https://github.com/NoctilumeDev/Qixu/releases/tag/v0.1.0-engineering.1)交付原JAR及三端ZIP；不是一键安装器，先读包内READ-ME-FIRST。包内docs保留源码时点原文，后续状态以公开验收记录为准。

本轮结束停止owned实例并清理可重建依赖/编译实例；源码、Git、锁文件/迁移、原首败、正式Bundle/截图、原JAR/ZIP及账册保留。恢复时按本文重新安装两套依赖、配置自己的独立DB与账册，再构建/验收；不依赖本机node_modules长期存在，不清共享工具或其他项目数据。M10以后由用户参与，H5/微信编译成功不授予微信真机资格。
