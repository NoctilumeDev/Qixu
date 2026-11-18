# 2026-11-19 · 期序依赖公告变化核对

本批对应 `main@f39398e6e8263586883c66f95f1e90cae5a3f078`。比较同一提交的成功运行、首次定时失败、最新定时失败及 11 月 19 日的本地复跑。清单、锁文件、许可证清单及包数一致。本批范围是记录已核对的八项差异。Node 补丁另行提交。

| 来源 | 包数 | 原记录 → 当前记录 | 检查结果 |
| --- | --- | --- | --- |
| Maven | 88 | 10 → 11 | 新增 Tomcat 一项 |
| npm 工作区 | 704 | 44 → 47 | 新增三个包条目，四个原条目详情变化 |
| npm 随机验签 | 13 | 0 → 0 | 清单与扫描结果一致 |

Maven 实际解析为 `spring-boot-starter-webmvc:4.1.1 → spring-boot-starter-tomcat → spring-boot-starter-tomcat-runtime → tomcat-embed-core:11.0.24`，属于运行包依赖。[Apache Tomcat 公告](https://tomcat.apache.org/security-11.html)列为 Moderate，[GitHub 公告](https://github.com/advisories/GHSA-f525-44xv-f2qj)列为 Critical。两者均把 `11.0.24` 列入范围，`11.0.25` 是针对该公告的修复版本。保留评级来源，下一步独立核对连接器及部署配置，评估有资格的同系列补丁。

## 工作区差异

| 包记录 | 变化与当前锁定版本 |
| --- | --- |
| `@intlify/shared`、`vue-i18n` | 学生端锁定 `vue-i18n@9.14.5` 及其 `@intlify/shared@9.14.5`。新增 [GHSA-83rm-6c8r-g728](https://github.com/advisories/GHSA-83rm-6c8r-g728)，`vue-i18n` 同时命中 [GHSA-8rc2-428q-7ww3](https://github.com/advisories/GHSA-8rc2-428q-7ww3)。候选版本需覆盖同源包并验证学生端 H5 和微信构建。 |
| `@intlify/core-base`、`@intlify/message-compiler` | uni 工具链的 `9.1.9` 与学生端 Vue I18n 引入的 `9.14.5` 同时存在。core-base 记录新增上述直接公告和传播边，message-compiler 记录新增来自 `@intlify/shared` 的传播边。锁定版本保持原样。 |
| `follow-redirects` | `centra@2.7.0` 解析到 `1.16.1`，新增 [GHSA-mrwp-f3q8-3g9p](https://github.com/advisories/GHSA-mrwp-f3q8-3g9p)。上游列明 `1.16.2` 修复，后续按独立依赖候选回归。 |
| `qs` | Express 和 body-parser 分别解析到 `6.11.0` 与 `6.13.0`，新增 [GHSA-6pq8-jhm2-2vcw](https://github.com/advisories/GHSA-6pq8-jhm2-2vcw)，扫描汇总仍为 Moderate。 |
| `vite` | 锁定 `5.2.8` 和 `8.3.2`。npm 中 [GHSA-fx2h-pf6j-xcff](https://github.com/advisories/GHSA-fx2h-pf6j-xcff) 的来源号由 `1123525` 变为 `1243432`，该公告评级由 High 调为 Moderate，条目汇总仍是 High。 |

58 条记录均保持 `UNRESOLVED`。原所有者和跟进责任保留，`risk_accepted=false`。记录数含传递依赖传播，不能按独立漏洞数量计数。是否适用于实际部署尚待核对。既有门禁继续检测包、公告或详情变化。

## 运行坐标

| 结果 | 运行及后端附件 | ZIP SHA-256 |
| --- | --- | --- |
| 原成功 | [40399301710](https://github.com/NoctilumeDev/Qixu/actions/runs/40399301710)，附件 12687713009 | `cc4e2140a48d0e5f507569adc367dc1dfbe9eebbc901d99940234c52792e9425` |
| 首次失败 | [41438425450](https://github.com/NoctilumeDev/Qixu/actions/runs/41438425450)，附件 13137433605 | `c2a77417453a5970b59afda4a705e72199967d1ce026205205ab7c41d76a1f9d` |
| 最新同类失败 | [43862418678](https://github.com/NoctilumeDev/Qixu/actions/runs/43862418678)，附件 14224611949 | `a09d4579946473867491ce1b837fbe94bbfca28bc3f66095f07be5214dfe7d52` |

三份附件均验证 SHA-256。两次失败的扫描观察与本地同版本复跑一致。本地 `observation.json` 的 SHA-256 为 `d927196b4c6900ebe38135f4c34c5a7631081ce0e4503b286b60e5fa2159b7cb`。原账册能从[固定主线](https://github.com/NoctilumeDev/Qixu/blob/f39398e6e8263586883c66f95f1e90cae5a3f078/docs/governance/dependency-review.json)读回，[59 条归档](dependency-review-20261023.json)继续保留。附件保留期以 GitHub 为准。
