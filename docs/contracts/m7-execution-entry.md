# M7执行入口 · 版本1

基线为受保护主线 `4832eb3bfde4aefb2c66199cdd9f0ea431374d8d`；运行资格仍绑定M6的 `5ca0b5e`。本合同在新增攻击实现前固定，不把旧142例或31例升级为M7资格。迁移V1–V9保持原字节，执行采集器绑定实际候选SHA、合同和迁移摘要；修改合同必须追加版本和原因。

## 范围和顺序

入口账册F01–F15全部保留。分段执行只是控制宿主资源：先F08/F09/F14/F15的协议、主体及投影反例，再F01–F07/F10–F13的独立模型、真实事务组合和恢复。各段都保留首败；段通过不代表M7闭合。真实浏览器和同库/备份恢复另有安装观察，不能由客户端单测代替。

第一段保护以下行为：

- JSON写入只能接受单一、无重复对象键的文档；包括转义后同名键。重复字段和尾随JSON必须422，零业务行、零回执；合法文档加空白仍成立。请求标识和安全错误信封保留。只用小型有界请求，不做外部扫描或大体积攻击。
- 恢复元数据以 `(actorId, key)` 为坐标。同名key在不同账号下合法；原A响应晚到不得擦掉B的UNKNOWN。查询、重放和停止均定位当前账号自己的记录。匹配回执只能清自己的记录。
- 存储读、扫描、迁移、删除故障不得变成“没有待确认请求”；不能靠吞错误继续发新写。构造期可进入受限状态，业务写和恢复需明确的本地存储错误。注销/换主体仍先清内存敏感正文和当前主体，存储删除故障不能阻止服务器注销尝试。恢复凭据删不掉时保留原键，不冒称已经清理。
- 所有站内消息均可访问，不能把最新100条投影称作完整集合。第一页保持既有 `/inbox` 调用兼容；显式页码/页大小、总量与未读总量、稳定id倒序，超范围为空，非法范围422。分页采用一次主库事务快照；不同页之间新增通知可触发刷新，不能宣称跨HTTP冻结集合。已读动作仍幂等、不影响他人；旧未读结果可以被找到。页面加载/失败时不显示虚假的“0条未读”。

## 第一段固定trace与oracle

| trace | 机制 | 预先判定 |
| --- | --- | --- |
| JSON-DUP | F15 | 重复spaceId、重复selected、转义后同名键；真实HTTP均422，SQL收藏/回执0 |
| JSON-TAIL | F15 | 对象后对象/布尔/数组均422；合法对象与空白控制200/唯一回执 |
| INBOX-151 | F10/F14 | 本人151、他人5条交错；逐页并集等于本人原始SQL，未读151→150；他人不可读 |
| ACTOR-KEY | F08/F09 | 两个账号相同文本key；B恢复/停止只清B，A迟到200只清A；存储与可见主体分别核对 |
| STORAGE-DENIED | F08/F15 | 构造迁移拒绝、扫描拒绝、删除拒绝×换主体；写前拒绝、敏感正文先清，原key仍可追踪 |

原生观察0.13/Plan1只约束第一段新增3个HTTP/MySQL见证与原有回归；前端观察0.2/Plan1约束新增机制用例与干净构建。它们不证明浏览器实际存储策略、备份恢复、整体M7、微信设备或生产容量。采集前封存Plan、原Bundle只追加。

## 公开错题来源

2026-10-03读取的主来源只提供启发，不能迁移结论：

- [Jackson #1073](https://github.com/FasterXML/jackson-core/issues/1073)已关闭，维护者解释严格重复/尾随选项；期序是Jackson3，必须实测自身HTTP转换器。
- [Spring Boot #36666](https://github.com/spring-projects/spring-boot/issues/36666)标记invalid/external-project，不作为已确认漏洞，也不执行其巨量请求脚本。
- [Excalidraw #11962](https://github.com/excalidraw/excalidraw/issues/11962)与[未合并PR #11994](https://github.com/excalidraw/excalidraw/pull/11994)，提示存储异常类别差异；不声称上游已有正式修复。期序进一步检查迁移、扫描、注销和恢复删除。
- [Spring Security #8682](https://github.com/spring-projects/spring-security/issues/8682)、[Cookie Store解释](https://github.com/whatwg/cookiestore/blob/main/explainer.md)：主体代际和真实凭据副作用分别验证。
- [Cypress #34350](https://github.com/cypress-io/cypress/issues/34350)：请求证据需区分会话/重定向，不用单个工具ID代表全部事实。
- [drand #1447](https://github.com/drand/drand/issues/1447)、[#1486](https://github.com/drand/drand/issues/1486)：未证实报告/设计问题，启发round边界和先冻结后取源，不能声称外部时间证明已成立。
- [Vue Router PR #2780](https://github.com/vuejs/router/pull/2780)：上游已合并不等于锁定依赖具备同一行为，实际前进/后退另验。

## 停止和剩余义务

发现真实业务错误先分类/最小修复/原trace复验；测量错误保存后版本化最小观察边界。第一段之后F01–F15仍需逐项事实判定和独立模型，F12不能因实现困难直接写NOT_PROVEN掩盖已观察矛盾。所有未知按覆盖账册的触发/影响/缺证据/环境/重入/归属填写。M8两个角色只在M7退出后启动。

## 观察边界修订1.1 · 只补前端采集选择

原前端0.2/Plan1在e78f0b4产生PENDING：封存37个必需用例，却只执行ownership文件31个。保留原Bundle，分类COLLECTOR_CASE_SELECTION_OMISSION，不是产品PASS或产品失败。0.3/Plan2明确选择两份测试文件，六条断言及原业务标准不变；原生0.13/Plan1不变。此次修订先于重采，尚无产品修复。
