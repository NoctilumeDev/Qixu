# 2026-10-27 · postcss-selector-parser 公告范围校准

## 范围与停止条件

入口为 `main@3bade57b49cd50d8d60dff8f3675e9b48167f22d`。[PR #37](https://github.com/NoctilumeDev/Qixu/pull/37) 已合并，但[主线检查 40395809639](https://github.com/NoctilumeDev/Qixu/actions/runs/40395809639)拒绝了一项随后发生的公告详情变化。本批只校准该项来源数据；业务、依赖版本、锁文件、扫描器和比较规则保持原样，全部风险状态保留。

退出要求：以两份真实运行附件核对差异、原门禁正反例通过、固定候选 CI 与两角色独立复核完成；合并及新主线验证另行记录。没有这些事实不能提前宣布第三轮收口，不创建 Release。

## 唯一变化与保留边界

[GHSA-rj75-hqrm-r3gf](https://github.com/advisories/GHSA-rj75-hqrm-r3gf) 于 2026-10-26 更新，现将受影响范围分列为 `>=7.0.0 <7.1.6` 与 `<6.1.5`，修复版本为 `7.1.6` 和 `6.1.5`。这不是公告撤回或项目升级。

本仓两次 npm 报告中，`postcss-selector-parser` 汇总范围由 `<=7.1.5` 变成 `<=6.1.4`；对应 `via` 公告的来源号由 `1241232` 变成 `1243379`，范围由 `<7.1.6` 变成 `<6.1.5`。该 URL、标题、评级和 CVSS 均未改变，另一条递归公告也未变化。

当前根锁条目为 `6.1.2`，仍在上述受影响范围；两个嵌套条目为 `7.1.6`。npm 本次返回的范围不能被当成上游全部版本范围，也不能由公告更新推导项目已修复或实际攻击可达。该条目继续 **UNRESOLVED**，所有者及后续核查动作不变。

三套清单、锁文件、包分母、记录集合及其他详情均未变化。此次仅更新这一项详情摘要和观察坐标；54 条依赖记录、163 条静态候选继续保留，`risk_accepted=false`。不新增忽略项、放宽阈值、改变比较合同或自动同步来源数据。

## 历史与证据

- [校准前的 54 条账册](https://github.com/NoctilumeDev/Qixu/blob/3bade57b49cd50d8d60dff8f3675e9b48167f22d/docs/governance/dependency-review.json)绑定已合并提交；[原 59 条归档](dependency-review-20261023.json)保持不变，不重复覆盖历史。
- PR 候选成功运行：[40354165020](https://github.com/NoctilumeDev/Qixu/actions/runs/40354165020)，实际检出 `497bd6d9e343a5fcbaf9065832b472db310c8680`；附件 `12665617111`，ZIP SHA-256 `cd1e8c8052c60922b1edcdbda75935d426a9b4d42ad31048838ae702310f1dc4`。
- 新主线失败运行：[40395809639](https://github.com/NoctilumeDev/Qixu/actions/runs/40395809639)，检出上述 `3bade57`；附件 `12686676911`，ZIP SHA-256 `bcf2c613d1cd9a4166c9a8c4a38cd6943cd99e0a1a5334dcb478f30d3ab149bd`。
- 两次检出干净、文件树相同。主线 37 项单测、144 项真实 MySQL 集成测试通过，静态检查通过；整个后端 job 因依赖详情变化失败，不能称为全绿。
- 两份原始 ZIP 已按 GitHub 摘要核对并另行留存；公开运行附件有保留期，链接与摘要不保证永久下载。

此前公告撤回和 body-parser 记录见[上一批校准](advisory-reconciliation-20261026.md)。本批不改变其结论，也不授予页面、真机、生产或新发行资格。
