# 质量与依赖护栏 · 本批范围

入口为 `main@74380570c422fea303700812193996dc6de7bec4`。本批补期序当前 Maven、根 npm workspace 与 randomness 独立 npm 锁文件的依赖检查、有限凭据检测、适用的静态质量检查与覆盖盲区记录。维护者负责保留扫描候选、来源、状态及后续动作；未知不等于已接受。

不改 JDBC、显式 SQL、事务隔离、随机来源、公平分配算法、业务页面或历史 Plan/Bundle。不增加 ORM、缓存、消息队列，不重开 M9/M10，不创建 Release。中间件升级和专项复验另行授权。本文件先记录范围，后续实际结果按运行身份追加，不能预填通过。

## 验收与停止线

- 依赖检查必须实际解析/读取当前依赖及锁文件并调用扫描器；工具失败、无报告、报告不完整和未审查变化不能通过。项目依赖、开发依赖与构建插件边界分别说明。
- 有限凭据检查只输出位置与规则，不回显候选值；代表性假凭据、输入失败在普通与 Python 优化模式下均须被拒绝。
- 静态检查约束已有实现，不借工具告警重构业务；覆盖率先如实测量/核对盲区，不预设百分比，也不拿单元测试替代真实 MySQL。
- 安全政策和报告渠道需维护者确认后落地，不虚构私密入口、联系人或响应时限。
- 固定候选完成原检查、新负控、公开 CI、测试与产品两角色独立复核；合并另需用户授权，合并后检查 exact-main 与保护读回。

本批退出是所选护栏有实际否决能力、剩余事项可追踪，不是零漏洞、无未知或正式部署认证。已确认且正在暴露的严重缺陷单独报告，不因后置专项而静默忽略。

## 检查如何生效

原三个必需 job 名称与 GitHub Actions 来源不改。`Document integrity` 增加当前已跟踪文本的有限凭据检查和新门禁负控；`Backend · MySQL` 保留原随机验签/真实 MySQL 验证，并增加覆盖分母核对、生产字节码静态检查和全量项目依赖的公告变化检查。PR、main push、手动及每周一 UTC 03:41 均运行同一工作流；计划存在不等于定时运行已发生。

- 依赖：Maven dependency-plugin 3.11.0 的实际解析树对照 CycloneDX 2.9.3 / 1.6 SBOM，包含测试依赖；OSV Scanner 2.7.0 固定 Windows/Linux x64 下载摘要，不设置忽略公告。根 workspace 和 `randomness` 分别运行锁文件 npm audit，包含开发/可选/peer 依赖。检查源清单、许可证声明、依赖分母、公告详情及执行退出码；新增、变化、消失均需复核。
- 静态：SpotBugs Maven 4.10.5.0 / engine 4.10.5，Max/Low，扫描生产类。独立构建目录避免重用旧字节码，也不删除 MySQL 测试/覆盖报告。分析错误、缺类、分母不符或候选位置/数量变化均失败；没有批量 exclude。
- 覆盖：JaCoCo 0.8.15 随原 `mysql-it` 执行。报告必须包含执行会话、全部生产类和有效行/分支分母。它是测量完整性检查，不是百分比门槛，不证明测试质量、浏览器或真机行为。
- 凭据：仅检查当前 Git 跟踪的 UTF-8/UTF-16 文本中的私钥头和有限 GitHub/OpenAI/Slack token 特征。只输出路径、行与规则；读取失败拒绝。Git 历史、图片、二进制、归档及不符合特征的秘密不在检测范围内，不能称为完整秘密扫描。
- 证据：每次依赖检查用新目录，失败不回退到旧成功；原始 stdout/stderr、退出码、树、SBOM、公告与观察保留。CI `always()` 上传已产生的坐标、测试、覆盖和静态/依赖报告 30 天；超时可能缺附件，期满不保证可下载。长期可寻址的候选清单在下述版本文件中，原日志不提交。

## 首次观察与待处理事项

2026-10-23，从入口提交的本治理工作区采集；精确清单摘要见[依赖账册](governance/dependency-review.json)，生产类与候选位置见[静态账册](governance/static-review.json)。它们是尚未逐项取得安全/缺陷结论的留存清单，不是“已审查为无风险”的基线。

| 来源 | 本次分母 | 观察 | 状态 |
| --- | --- | --- | --- |
| Maven | 88 个解析出的项目依赖坐标 | 10 个包—公告关联，涉及 Jackson 与 Tomcat | `UNRESOLVED` |
| 根 npm workspace | 704 个非根锁条目（含 workspace/link） | 49 个受影响包条目，含传递传播；不是 49 个独立漏洞 | `UNRESOLVED` |
| randomness npm | 13 个非根锁条目 | 本次未返回公告 | 仅本次数据库观察，不是安全证明 |
| SpotBugs | 115 个生产类 | 163 个候选：154 个 priority 2，9 个 priority 3 | `UNRESOLVED`，不是 163 个已确认缺陷 |

所有者为本仓维护者 `NoctilumeDev/Qixu maintainers`。每个依赖条目保留详情摘要、公开公告来源或传播包及后续动作；静态条目保留规则、类、方法/字段/行坐标。维护者按实际运行/构建路径和触发条件作有限归因，确认缺陷另行修复回归；不能因本 PR 收口删除事项。此次没有业务依赖升级，没有风险接受或生产授权。

**绿灯仅表示工具成功执行且没有未经复核的变化。** 现有未知仍会明确计数。扫描器报错、失去输入、出现新公告等会否决 PR；账册修改本身仍需人工复核。维护者能修改脚本/账册，平台审批人数为 0，不能宣传为独立人工审批已被强制。当前初始检查器不提供把记录切成 `NOT_APPLICABLE`、`REMEDIATED` 或风险接受的快捷通道；以后改变处置机制也须独立说明依据。

## 许可与未覆盖范围

项目源码的 [MIT](../LICENSE) 不覆盖第三方依赖。88 个 Maven 坐标的上游声明见[许可清单](governance/maven-licenses.json)，每次重新解析后比较摘要；npm 声明留在两份锁文件并绑定摘要。MySQL Connector/J 声明 GPLv2 with Universal FOSS Exception；Logback 和 Jakarta 等有多许可声明，不能笼统写成所有依赖 MIT。本批只核对来源与声明，不作所有分发方式均兼容的法律结论。

根 npm 中 `dom-walk`、`exif-parser`、`qrcode-terminal` 三个条目没有 license 字段，保留 `LICENSE_SOURCE_UNVERIFIED`，维护者后续核对上游发布包的 LICENSE，不填猜测值。[Phosphor 原许可](../student/src/static/icons/LICENSE.txt)与[六份生成演示素材的来源说明](assets.md)保留；生成图片不是真实校园证明，也不构成第三方权利的全面审查。

Maven 构建插件及其传递依赖、JDK/MySQL 服务端、操作系统、容器镜像、GitHub Actions 自身和 Git 历史不在本次项目依赖扫描内。npm audit 的结果受 registry 公告覆盖限制；OSV 数据可能更新，变化应失败并复核，不能自动“刷新为通过”。

## 安全报告渠道

用户批准现稿后，2026-10-23 仅启用本仓 Private Vulnerability Reporting。管理员 GET 返回 `enabled: true`，公开 [Security 页面](https://github.com/NoctilumeDev/Qixu/security)已出现 `Report a vulnerability` 后，才加入根 [SECURITY.md](../SECURITY.md)。这不证明文档已进入 main，也不证明通知实际送达。

账号 Watch/安全通知设置本次未读回：`NOTIFICATION_CONFIGURATION_NOT_VERIFIED`；实际投递：`DELIVERY_NOT_VERIFIED`。账号侧由维护者核实，不制造漏洞报告，不据此阻塞本轮普通治理，也不承诺响应时限。

## 验证记录

首次本地覆盖试跑：37 个 Java 单测中 1 个离线验签用例报错，原因是独立工作区尚无 `randomness/node_modules`。保留原日志及 Surefire 报告；按原锁文件 `npm ci --prefix randomness --ignore-scripts` 补齐后，4 项 Node 验签、37 项 Java 单测通过，未改测试或随机源。这一观察不是完整 MySQL 验证。

本地单测覆盖仅 22/115 类、558/5587 行、319/2813 分支，显然不能替代 HTTP/MySQL 集成覆盖。覆盖采集随后由本候选 CI 的原完整后端检查重新 clean 执行，实际最终数字取该运行附件。候选公开 CI、两角色独立复核及合并事实另按固定 SHA 留在 PR；本段不预填通过、不继承历史资格。
