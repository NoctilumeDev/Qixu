# 本地运行与验收

M1–M6限定资格见各阶段acceptance。M6的固定主线身份适配及重启恢复已有隔离证据；M7–M10及全工程交付尚未完成。启动说明不等于完成证明。

## 工具与隔离

Java17、Maven3.9、MySQL8。开发库 `qixu`，测试库 `qixu_test`；两套应用账号只有本库权限。迁移通过 Flyway 自动执行，默认不允许 clean。MySQL 全局隔离不改，应用连接与事务显式 READ_COMMITTED。

已拥有独立库时配置自己的数据库地址/账号/密码。首次创建可运行 `python scripts/provision_local.py --mysql <mysql可执行文件>`，先通过环境变量提供 `QIXU_ADMIN_USER` / `QIXU_ADMIN_PASSWORD`。脚本遇到已存在目标库或账号拒绝，不覆盖或删除；生成凭据仅保存于忽略的 `.tools/database.local.json`。生产账号和权限由部署方单独设置。

## 启动

在 IDE 运行 `dev.noctilume.qixu.QixuApplication`，或在 backend 下执行 `mvn spring-boot:run`。环境变量见 [模板](../backend/.env.example)；模板不会自动加载。必须提供 `QIXU_DB_PASSWORD`，按实际库设置 `QIXU_DB_URL` / `QIXU_DB_USERNAME`。

显式 `SPRING_PROFILES_ACTIVE=demo` 才创建虚构空间与演示身份：student1/student2、teacher1、admin1/admin2，演示口令均为 `qixu-demo`。生产 profile 禁止同时开启 demo；默认配置不创建演示管理员。

默认只监听 127.0.0.1:6967，启动前确认端口归属；`GET /api/health` 返回就绪范围。小程序用 Bearer 登录，管理浏览器用 HttpOnly cookie 与绑定 CSRF。设置允许来源时使用完整 origin，不能用通配凭据。

## 两端施工入口

根目录使用锁定的Node24.14.x/npm11.9.0执行`npm ci`，再运行`npm run test:client`、`npm run check`和`npm run build`。构建分别产生学生H5、微信小程序和管理Web；构建不授予页面或真机资格。

后端正常启动后，`npm run dev:student`监听127.0.0.1:6968，`npm run dev:admin`监听127.0.0.1:6969，二者将同源`/api`代理到6967。端口占用时strictPort拒绝，不自动漂移或杀其他服务。共享客户端直接解析到`packages/client/src/index.ts`，避免workspace的node_modules链接缓存保留旧代码；跨合同更新后从冷启动核对实际页面。

微信开发者工具导入`student/dist/build/mp-weixin`，设置本项目自己的AppID；不复用青野AppID。原生端必须配置部署的HTTPS服务origin及平台合法域名；默认不关闭域名校验。H5开发代理不代表微信手机可连接，认证、真机键盘和订阅消息另行验收。

## 原生验收与验迹

独立测试库配置 `QIXU_TEST_DB_URL` / `QIXU_TEST_DB_USER` / `QIXU_TEST_DB_PASSWORD`，URL 必须是 qixu_test 或 CI 临时 qixu_ci，缺配置直接失败，不替换成 H2。`mvn -B -ntp -Pmysql-it clean verify` 启动实际 HTTP 服务、运行真实迁移和 MySQL 断言；测试 JVM 结束后服务停止。

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

## M7 恢复隔离 · 施工中

新修复尚未取得完整恢复资格。默认独立账册为进程工作目录`.qixu/transactions.journal`；部署应显式将`QIXU_RECOVERY_JOURNAL`指向MySQL数据目录及SQL恢复对象之外的持久绝对路径。文件与库内generation/marker配对，第二个进程不能共享该账册继续服务。禁止删除/裁剪历史后重新初始化，禁止把账册跟随旧SQL一起回滚。

新空库可首次初始化。已有V1–V9库的操作员在停写、保留备份与旧外部副作用边界后，显式一次性设置`QIXU_RECOVERY_BASELINE=ADOPT_PRE_V10_ONCE`；账册只留规范基线摘要，不保存个人正文。该值不能绕过已有generation的缺失/损坏账册。基线以前的通知/外部世界不因此被证明一致。

NOT_RECONCILED时健康与业务入口503，原键/凭据保留，演示初始化和调度不继续。恢复与账册相符的数据库再重启可以重新核对；不提供网页强制继续。整主机一起回退、特权SQL绕过、长时间账册轮换、多节点与灾备能力仍未证明。32MiB/200,000事件硬预算耗尽会隔离，不截断历史；现阶段需监控账册大小，不能视作无限运行容量。

M7 native0.16/Plan4执行161项（147原见证+14文件/受控DB视图单元），专属测试库账册持久保存于`.tools/runtime/native-<schema>`；真实恢复另运行`veritrail_m7_restore.py --producer-bundle <同source的原native0.16/Plan4 Bundle> --java <Java17> --mysql-bin <MySQL8.0.44 bin>`。它只初始化本轮新数据目录和6975/6976独占实例，保留T0/T1/独立world见证，宿主3306不动。0.3/Plan3与原0.2恢复业务断言一致；测试成功、页面资格和最终Release分别判定。
