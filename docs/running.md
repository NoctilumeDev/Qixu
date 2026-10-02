# 本地运行与验收

当前入口只覆盖 M1 后端基础链路。预约、批次、活动和两个前端按里程碑继续施工，不能从这个启动说明推定它们已完成。

## 工具与隔离

Java17、Maven3.9、MySQL8。开发库 `qixu`，测试库 `qixu_test`；两套应用账号只有本库权限。迁移通过 Flyway 自动执行，默认不允许 clean。MySQL 全局隔离不改，应用连接与事务显式 READ_COMMITTED。

已拥有独立库时配置自己的数据库地址/账号/密码。首次创建可运行 `python scripts/provision_local.py --mysql <mysql可执行文件>`，先通过环境变量提供 `QIXU_ADMIN_USER` / `QIXU_ADMIN_PASSWORD`。脚本遇到已存在目标库或账号拒绝，不覆盖或删除；生成凭据仅保存于忽略的 `.tools/database.local.json`。生产账号和权限由部署方单独设置。

## 启动

在 IDE 运行 `dev.noctilume.qixu.QixuApplication`，或在 backend 下执行 `mvn spring-boot:run`。环境变量见 [模板](../backend/.env.example)；模板不会自动加载。必须提供 `QIXU_DB_PASSWORD`，按实际库设置 `QIXU_DB_URL` / `QIXU_DB_USER`。

显式 `SPRING_PROFILES_ACTIVE=demo` 才创建虚构空间与演示身份：student1/student2、teacher1、admin1/admin2，演示口令均为 `qixu-demo`。生产 profile 禁止同时开启 demo；默认配置不创建演示管理员。

默认只监听 127.0.0.1:6967，启动前确认端口归属；`GET /api/health` 返回就绪范围。小程序用 Bearer 登录，管理浏览器用 HttpOnly cookie 与绑定 CSRF。设置允许来源时使用完整 origin，不能用通配凭据。

## 原生验收与验迹

独立测试库配置 `QIXU_TEST_DB_URL` / `QIXU_TEST_DB_USER` / `QIXU_TEST_DB_PASSWORD`，URL 必须是 qixu_test 或 CI 临时 qixu_ci，缺配置直接失败，不替换成 H2。`mvn -B -ntp -Pmysql-it clean verify` 启动实际 HTTP 服务、运行真实迁移和 MySQL 断言；测试 JVM 结束后服务停止。

安装公开 Core0.13.0 后，从干净提交运行 `python scripts/veritrail_native.py --stage m1 --maven <mvn可执行文件>`。采集器先封存 Plan，再执行 clean verify，不能读取旧报告冒充本次成功。私有原始日志和 Bundle 保留于 artifacts/local，每次有全新身份；仅规定范围的事实进入 Core，不声称生产容量或真实校园身份已获证明。
