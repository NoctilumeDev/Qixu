# 2026-10-26 · 公告来源变化的有限校准

## 范围与停止条件

基于 `main@3a751dc07cf0b611211f2d49b044cddf42f1ef0c`。第三轮只读核查发现，同一提交的[定时检查 40298648984](https://github.com/NoctilumeDev/Qixu/actions/runs/40298648984)被依赖门禁拒绝；此前[39964003279](https://github.com/NoctilumeDev/Qixu/actions/runs/39964003279)通过。两次 checkout、清单及锁文件一致。本批只校准有来源依据的公告变化，不改业务、JDBC、算法、依赖版本、扫描器或比较规则，不接受剩余风险，不创建 Release。

旧账册完整保存在[2026-10-23 原件](dependency-review-20261023.json)。当前账册继续使用 `qixu-dependency-review/2`；其 `UNRESOLVED` 是待核查状态，不是风险豁免。停止条件为：逐项差异归因、原始失败保留、旧账册字节不变、原检查器正反例通过、固定候选 CI 和独立复核完成。合并及主线验证另行记录，不能提前声明。

## 两次实际观察

| 来源 | 旧观察 | 新观察 | 判断 |
| --- | --- | --- | --- |
| Maven | 88 个坐标、10 条关联 | 相同 | 清单、许可及公告详情均未变化 |
| npm workspace | 704 个锁条目、49 个受影响包条目 | 同一清单、44 个条目 | 5 个条目消失、4 个保留条目详情变化，逐项说明如下 |
| randomness npm | 13 个锁条目、无公告 | 相同 | 仅当次数据库观察 |

当前活动账册共有 54 个待核查条目，旧 59 条账册保留。数量不是独立漏洞数，减少不代表项目修复了 5 个漏洞。

### 上游撤回与传播关系

[GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm) 于 2026-10-23 被上游撤回。`braces` 原条目只含此公告；`chokidar`、`micromatch`、`fast-glob`、`unimport` 的原 `via` 链也只回到此来源。五个条目退出当前扫描集合，原清单摘要、条目哈希、`UNRESOLVED` 及后续动作保留在旧账册，不能重标为 `REMEDIATED`。

`@dcloudio/uni-cli-shared`、`@dcloudio/vite-plugin-uni` 移除了对应传播边；`@dcloudio/uni-cloud` 移除 `fast-glob` 传播边，registry 汇总评级由 high 变为 low。本批同时保留前后原始报告，不把汇总评级降低解释为项目安全结论。

### 新 body-parser 公告

保留条目 `body-parser` 新增 [GHSA-27r3-jxmh-p828](https://github.com/expressjs/body-parser/security/advisories/GHSA-27r3-jxmh-p828)，汇总评级变为 high，范围相应改变。当前锁定链为学生端开发工具 `@dcloudio/vite-plugin-uni@3.0.0-5020420260813003 → express@4.20.0 → body-parser@1.20.3`。该版本命中公告范围，但不能直接推导 Java/JDBC 业务后端存在此漏洞。

对锁文件指定的三个发布包逐一核对 SHA-512 后静态阅读：普通 uni 开发入口使用 Vite；Express 只在该工具的 `createSSRServer` 延迟构造；所查 SSR 实现没有挂载 body-parser。期序 `student/package.json` 未开启 SSR，`student/vite.config.ts` 使用回环开发服务；管理端为 Vite，既有安装验证入口使用 Python 静态服务及 Java API。未在这些入口中找到受影响解析器注册链。

本次没有运行攻击输入，也没有观察所有插件的运行时中间件注册，因此仍为 **UNRESOLVED / needs_review**。所有者为 `NoctilumeDev/Qixu maintainers`。后续若引入 SSR、Express 解析器或改变开发服务暴露方式，须核对实际请求链；若选择依赖维护，应独立验证兼容性。此次不以“开发依赖”或“仅回环”作为通用豁免，也不因新增公告就扩大本批为全面安全审计。

## 证据坐标与限制

- 原通过附件 `backend-check-39964003279-1`，artifact `12505698600`，ZIP SHA-256：`51c326791750725f9708e826e5c99c7345cefb82a94726b8ba7b4b7e9f689265`。
- 新失败附件 `backend-check-40298648984-1`，artifact `12639970815`，ZIP SHA-256：`2750451295ba8697f2af7715effedcd332b29f01b215b69ab8d9774b767a6ac6`。
- 新运行 37 项单测、144 项 MySQL 集成测试通过；失败发生于公告变化检查。这不等于整个 job 成功，也不授予新的页面、真机或生产资格。
- 原始附件、差异、静态分诊和发布包摘要另留在本次受管审查证据中。GitHub 附件保留期有限，以上运行身份和摘要不能冒充永久可下载保证。

本批只更新已明确解释的四个详情摘要并保留新增公告来源；没有忽略列表、降级阈值或自动同步基线机制。其余条目与状态不变。
