# 验迹接入与验收合同 0.1

本文件为M0时冻结的验收设计与责任边界。实际M0–M6限定资格见各阶段acceptance；M6主线证据见[M6](acceptance/m6.md)，M7–M10及整个工程仍待闭合。合同不是执行结果。

## 依据与固定坐标

2026-10-03 经 GitHub API 读取验迹远端 `main@999b6662e81cd466ce9b7f654cea5f4857932008`。本地旧检出 `d794731` 不作为当前远端事实。以下引用锁定同一提交：

- [产品边界与入口](https://github.com/NoctilumeDev/VeriTrail/blob/999b6662e81cd466ce9b7f654cea5f4857932008/START_HERE.md)
- [非因果 Acceptance Core](https://github.com/NoctilumeDev/VeriTrail/blob/999b6662e81cd466ce9b7f654cea5f4857932008/docs/81-pc1-acceptance-core-implementation.md)
- [真实项目串行验收](https://github.com/NoctilumeDev/VeriTrail/blob/999b6662e81cd466ce9b7f654cea5f4857932008/docs/23-m11-single-node-real-project-contract.md)

消费公开 Core `0.13.0` wheel，不以验迹工作树或开发版代替发行物。下载地址：

`https://github.com/NoctilumeDev/VeriTrail/releases/download/v0.13.0/veritrail-0.13.0-py3-none-any.whl`

SHA-256：`95cb00c08fa4a29c21c798c7ca5a8200bb83f71cd11b31b1dea01c19ec5a8a04`。本项目不修改验迹、不移动其标签，不调用尚未实现的 O/T 插件。

## 接入方式与权威

期序需要 MySQL、身份和两个前端，不属于 Starter 0.2 的单节点无秘密预设。不能隐藏数据库、假造依赖或把整个项目标成 Starter 支持。

采用公开 `AcceptancePlan 0.1 → Evidence 0.1 → AcceptanceReport / AcceptanceBundle` 路径。期序提供版本化、可审阅的事实采集器，Core 按封存规则独立裁决；验迹不被描述为已经拥有 Java/MySQL 专用采集插件。

| 责任 | 归属 | 不能代替 |
| --- | --- | --- |
| 要求、范围及最终处置 | 项目所有者授权的施工合同 | 测试名、AI 自评、页面漂亮程度 |
| 执行与原始观察 | 实际 Maven/MySQL/浏览器/平台工具 | 预期输出、手填测试总数 |
| 规范化与来源绑定 | 期序采集器合同 | 业务资格和批准权 |
| 充分性、完整性、断言裁决 | 公开验迹 Core | 测试是否设计正确、现实终极真相 |
| 独立复核 | 固定提交上的测试及产品审阅者 | 新建另一套放宽标准 |

## 串行门禁

1. 绑定 exact Git SHA、干净工作树、发行包摘要、工具链、数据库隔离/规模、视口及采集器版本。
2. 写并 seal 对应阶段 Plan；subject 与 observation 坐标一致。计划内要求不能在观察结果后原地修改。
3. 资源预检，记录已有服务，创建独立运行 identity；只使用本项目测试数据库及 owned 进程。
4. 顺序执行领域/真实数据库/实际浏览器证据采集，保留输出、截图和清理事实。证据不足不填零或 PASS。
5. 以 exact plan/spec digest 绑定 Evidence；调用公开 Core 生成不可覆盖的新 Bundle。
6. 负向或故障控制必须能被捕获；同标准恢复后用新 identity 重跑，保留首败。
7. 核对 PR 与新 exact-main 的 CI、安装/页面/远端读回；新的提交重新取得自己的资格。
8. 独立复核后才更新里程碑状态、README、Release。文档编号、同样内容和旧绿灯不自动授权下一步。

## 分层证据，不混成一个绿灯

- M0：真实文档结构、链接和合同文件；只证明施工基线完整。
- M1–M4：构建及真实 MySQL；冲突/唯一权/候补/影响处理/越权以数据库最终事实为证据。H2 不能替代。
- M5：真实浏览器桌面与手机视口，登录、列表/地图、申请、审批、返回上下文、错误恢复；小程序编译单独记录。未经实际微信手机观察，不宣称真机 PASS。
- M6–M8：固定随机源、适配器不可用、重放与并发、公开错题和独立反馈修复后的原合同复验。
- M9：干净检出/公开工程候选的运行、两次真实验收、逐项 Evidence 完整性及状态对齐；明确M10仍待精修。
- M10：精修后固定候选的规范化视觉比较、桌面/手机操作及受影响业务回归；最终截图、main、CI与Release重新对齐。设备未观察不得称真机PASS。

具体运行 Plan 随实现坐标生成并在执行前封存；本文件不是未来运行的预填 PASS。关键不变量必须直接绑定事实，不能只绑定采集器的 `passed` 字段。

## 历史、资源与公开边界

执行状态 `COMPLETED / ABORTED / ERROR` 与裁决 `PASS / FAIL / INCONCLUSIVE / PENDING` 分开。工具、夹具、产品、合同或宿主问题先分类；恢复通过不删除失败、不改变同一次身份。

原始私有材料保存在忽略的 `artifacts/local/`；只发布脱敏可复算材料，文件大小、SHA-256、源坐标和缺证据状态一并保留。Cookie、Authorization、外部票据、原始反馈身份、数据库密码不能进入公共 Bundle。

现有 MySQL 和其他项目进程均非本轮 owned 资源。清理不停止它们；只清理期序创建且核实身份的应用/浏览器进程和专用测试数据。达到宿主资源停止线保存现场并中止，不降低业务断言。
