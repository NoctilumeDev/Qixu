# 仓库护栏维护

## 本轮范围 · 2026-10-23

入口为 `main@a27ccd1fefc95a6d761be0d1c92cbd44e5c22f3e`。本轮只对齐仓库护栏，不重开 M9 交付或 M10 视觉验收，不制作安装包、不创建 Release。中间件依赖升级、深入归因与专项复验在三仓本轮治理之后另开一轮；发现的风险仍由各仓维护者跟进，保留原证据与处置状态，后置不等于风险已接受。

这一批处理三个明确缺口：前端必需检查未绑定 GitHub Actions 来源；保护规则缺少可重复读回和负控；贡献入口及 CI 测试报告留存不完整。它不是整个质量、安全治理已经完成的声明。

保护现有领域合同、真实 MySQL 验收、固定随机源验签、客户端测试与三端构建。原 Plan、Bundle、失败记录、历史安装坐标、锁文件、业务代码和页面均不修改。

## 必需检查与权限

| 检查 | 继续承担的责任 | 来源 |
| --- | --- | --- |
| `Document integrity` | 脚本语法、卫生负控、文档链接与正式原件完整性 | GitHub Actions `15368` |
| `Backend · MySQL` | 随机验签器测试、后端单测和真实 MySQL/HTTP 验证 | GitHub Actions `15368` |
| `Frontend · H5 / WeChat / Admin` | 客户端测试、两端类型检查、H5/微信/管理端构建 | GitHub Actions `15368` |

最低配置见 [main-protection.json](../.github/main-protection.json)：要求最新基线、PR、失效审阅清理、解决会话、线性历史，对管理员生效，禁止强推和删主分支。现有批准人数为 **0**，因此不能声称平台强制了独立人工审批；本仓约定的独立复核另行留证。维护者拥有修改保护配置的权限，读回不是永久不可绕过的保证。

具有管理读取权限的账号执行：

```text
python -B scripts/check-branch-protection.py
python -B scripts/check-branch-protection.py --snapshot <原始GET响应.json>
```

首个命令只发 GET，不修改设置。无权限、网络失败、缺保护、缺必需检查或来源不匹配均返回失败，不能把“未读到”记为通过。第二个命令只校验所给历史快照，不证明当前远端状态。普通 PR CI 不接收管理员 token，只执行本地负控；远端配置须在设置变更、合并前后另行读回。

## 代表性反例与验收

[GitHub 官方说明](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)明确区分检查名称和来源 App；[必需检查说明](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks)还说明跳过、neutral 不等于实际执行成功。因此本批：

- 对每个必需检查分别删除、取消来源绑定、改成错误 App，读回校验器必须拒绝。
- 对管理员豁免、强推、删除、非最新基线、缺 PR、显式 PR bypass 和无权限响应分别拒绝；保留额外更强检查。
- 当前三个 CI job 不设路径过滤、条件跳过或 `continue-on-error`。合并核验须看到准确候选上三个 job 的 `success` 及实际检查步骤，不能只看汇总绿灯。
- 本地运行 `python -B scripts/check.py hygiene` 和 `docs`；负控同时以普通及 `-O` 模式运行。真实 MySQL 和三端构建由本候选的公开 CI 实际执行，不能挪用历史测试数字。
- CI 在成功或失败后保留本 job 的源码/运行坐标；后端另存 Surefire/Failsafe XML 与文本报告，保存 30 天，不上传数据库、密钥、用户资料或整个工作树。没有产生报告就保留缺失事实，不伪造结果；超时或取消可能来不及上传。
- 从固定候选分别进行测试和产品维护视角独立复核。这里不重跑产品视觉验收，也不把脚本负控称为向主分支发起了实际违规推送。

退出条件是：上述负控和已有 CI 实际通过、来源绑定 GET 读回匹配、当前候选/公开状态分别记录。范围内真实缺陷修复并复验；额外风险保留待办，不以清空全部告警为终点。合并前的通过只属于候选，合并后另查 exact-main CI。

## 后续有限治理

本批之后仍需按实际缺口核对依赖与许可、敏感信息、静态质量和覆盖盲区，以及维护/安全报告渠道；不预设覆盖率百分比，不把文件存在当作检查生效。不能直接沿用青野的漏洞结论、许可清单或安全维护承诺。中间件专项保持独立，本页不接受或关闭任何未解决风险。
