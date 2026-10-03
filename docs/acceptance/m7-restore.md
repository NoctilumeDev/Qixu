# M7 F12 · 独立账册与旧库恢复事实

当时状态：M7 IN_PROGRESS。本段限定单实例、独占MySQL物理停止/重启与逻辑SQL恢复，不是全工程、灾备或微信送达资格。基线PR#17按保护规则合入main@a918a2a877895e10eba871303e6d963f2f0e06b5，其[exact-main CI37097881018](https://github.com/NoctilumeDev/Qixu/actions/runs/37097881018)三项及步骤success，运行资格另按候选证据判定。

以下按执行顺序保留原失败和限定复验；后续[M7主线有限退出](m7.md)及[当前里程碑](../milestones.md)另行记录，不将本段历史状态当作当前施工状态。

## 原失败及修复

[合同1.4](../contracts/m7-restore-fence.md)保存各次版本顺序。原观察器的Windows监控父子进程PENDING、产品旧库恢复首FAIL、修复中的框架commit歧义首FAIL、缺账册控制前提混杂均在[错题本](../failure-notebook.md)单独分类，原字节不改。

原产品97a1d14的[restore0.2/Plan2 FAIL](../../artifacts/m7/m7-restore-1a767fd49106407d96d639211f9219eb/acceptance-report.md)：A取得正式回执并读到站内通知，恢复T0后B同位同时段又获200，A原回执404/重放409，新增预约和消息各1。数据库当下未超卖，却与库外已交付承诺矛盾。不能用普通重启PASS掩盖。

88df403d9df6bac79d8943f063dc48d81f7fc93e修复：V10新增generation/同事务marker，库外账册强制持久PREPARE/已知终态，启动完整对账；未知或缺损进入503 NOT_RECONCILED，阻断入口、身份采纳、演示初始化及授予/释放/通知任务。commit已准许后出现异常，即使随后rollback调用成功也保留UNKNOWN；不得将回调名称当成确定未提交。

## 复验与单故障控制

候选ec31a60f475d14835d37958a668513deaf094010只补独立观察前提，产品修复未变：

- [fresh native0.16/Plan4 PASS162](../../artifacts/m7/m7-79dc36b8f9914d519173345021cf9f44/acceptance-report.md)：原147见证加15文件/受控DB视图及实际Spring框架机制；不称162条真实MySQL。
- [fresh restore0.4/Plan4 PASS22](../../artifacts/m7/m7-restore-26c39280202643e386b1a13b0734a991/acceptance-report.md)：独占6975/6976、新数据目录与qixu_restore，绑定同source原producer/JAR、V1–V10摘要、Java17/MySQL8.0.44字节，先seal后初始化。原18断言未降低，新增4个前提控制。

正常同库新PID回执完全相同。T0恢复后健康、竞争申请、原回执查询和原键重放均503 NOT_RECONCILED；任务窗口后预约0、inbox0，库外world见证摘要不变。

缺账册控制先恢复正常重启后的新T1，实际健康200、原回执相同；停止应用后独立解析完整ASCII顺序/哈希链/状态，SQL完整marker/generation与7个COMMIT一致、无未决PREPARE。只移走真实存在的账册，不再回退DB，重启健康及回执503 NOT_RECONCILED。原0.3/Plan3 PASS及其混杂前提记录保留，不倒改。

全部owned子句柄按exe及本轮数据目录/JAR核对停止；6975/6976无监听。宿主3306/6947/7897的PID和启动时间与演练前一致，未停止其他服务。私有dump、HTTP原文、完整通知、SQL、CIM和world保留于每次运行身份目录，公共包只含脱敏事实。

## 未证明与重新进入

| 触发/影响 | 当前证据缺口与所需环境 | 重入/归属 |
| --- | --- | --- |
| 整主机和账册一起回滚，旧外部承诺可能消失于本地 | 当前账册独立于SQL恢复但仍同主机；需外部独立可信存储及恢复合同 | F12外部账册存储合同；期序维护者在支持异地主机时重开 |
| 特权SQL/同长度文件改写绕过管理事务 | 当前请求权限不授予DB/文件特权；不能证明特权攻击防篡改 | F12特权恢复威胁/操作员审计合同；真实权限或事故变化重开 |
| 真掉电、fsync/文件系统失效、驱动commit响应真丢失 | 现有文件IO/真实框架受控异常不等于断电；需隔离故障宿主/驱动网络代理 | F12物理故障合同，重放保留原PREPARE坐标；期序维护者 |
| 多节点共享库或长期账册耗尽 | 当前单实例独占文件锁、32MiB/200,000事件，耗尽隔离；无轮换/多节点资格 | 新部署/轮换合同与容量环境出现时重开，不截断旧历史；期序维护者 |
| 实际微信送达或学生已读 | 独立world仅证明采集器读到站内通知，不证明平台/真人 | F10微信设备及渠道合同；设备观察可用时重开，期序维护者 |

F12限定旧库/缺账册反例已修复复验；其他机制、实际页面、M8双角色与M10仍待完成，不能从本段PASS升级整个M7。

## 实际MySQL事务坐标补充

合同[真实事务坐标v1](../contracts/m7-transaction-coordinates.md)在4a8caa4先于新增用例实现。8337fe43f3464b5a0362519116f9958f8db28b87取得[native0.17/Plan5 PASS166](../../artifacts/m7/m7-6b71360aeabb4012a2a1547f8cc7daca/acceptance-report.md)，包含原162及4个实际MySQL对照：

| trace | 原始SQL观察 |
| --- | --- |
| 提交可见性 | beforeCommit本事务业务1/marker增1；独立连接业务0/marker增0；commit后分别1/1 |
| REQUIRES_NEW与外层回滚 | 内层真实提交1行/1marker；外层及REQUIRED加入事务各0行，不产生额外marker |
| 普通回滚与只读 | 原写行0，查询0，新marker0 |
| 禁止savepoint与正常恢复 | nested拒绝且行0；同外层正常写1/marker增1 |

同source的[restore0.5/Plan5 PASS22](../../artifacts/m7/m7-restore-683b6742e8bc4eff9b860c1685a46c62/acceptance-report.md)保留0.4全部22断言，仅绑定新的原producer。T0隔离、7marker/7COMMIT一致正控制、仅移走账册后的503再次成立，全部owned停止且两端口无监听。版本控制/回调故障与实际连接隔离分别保留，不升级为真断电或分布式提交证明。

## 受保护主线限定资格

PR[#18](https://github.com/NoctilumeDev/Qixu/pull/18) exact head5f34b6f的[CI37100968730](https://github.com/NoctilumeDev/Qixu/actions/runs/37100968730)三项及步骤success后，按严格门禁/enforce-admins/linear-history规则压缩合入main@3b445b9fd73b88aeaa4d5bab6e6264cd72485766，未使用admin旁路。原普通merge方式被线性历史规则拒绝，没有发生该合并，随后使用规则允许的squash。

新主线[CI37101177576](https://github.com/NoctilumeDev/Qixu/actions/runs/37101177576) exact SHA及三项/步骤success；fresh [native166](../../artifacts/m7/m7-a33774021b0f4827964e24a9ad2fd145/acceptance-report.md)与同producer新[restore22](../../artifacts/m7/m7-restore-9e1efc1ca9bc41f98496952d970aa5cf/acceptance-report.md)均Core PASS。真实旧库隔离、同库正常重启、7 marker/7 COMMIT一致正控制、仅移走账册后的隔离全部复验。owned清理与两端口无监听成立，远端main与README完整字节读回一致。

资格仅为上述F12单实例恢复/事务边界；文档后续发布是此主线资格的投影，不对新文档SHA伪造运行资格。M7其他组合/页面及M8–M10仍未闭合，已列未知不升级。

## 后续主线对齐

上述段落保留其发生时的候选/历史范围。main@c876d1a的新同源运行、公开CI及有限退出统一见[M7](m7.md)，不是把历史Bundle改写成新资格；微信设备及生产边界继续NOT_PROVEN。
