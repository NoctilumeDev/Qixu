# M7恢复库与外部世界 · 版本1.1

基线为受保护主线 `a918a2a877895e10eba871303e6d963f2f0e06b5`。PR#17门禁通过不等于M7闭合；本合同在F12新演练与修复前固定。第一段和元数据/独立oracle原包不覆盖。V1–V9保持字节，必要的新迁移只追加。

## 要证明的事实

数据库恢复只回退库，不会撤回学生已经见到的正式回执/通知。恢复后若无法与数据库之外的独立事务账册一致，必须进入 `NOT_RECONCILED`：业务写、原键重放、安全停止屏障、身份采纳和定时授予/释放/通知任务均不得继续。健康入口明确503及状态；不能把旧库的404/空位包装成新授权。保留旧外部见证和原键，不自动重发、重分、换随机源或静默初始化一份新账册。

第一条最小trace：独占MySQL实例/新schema，学生A/B登录后做T0备份；A取得短约及站内通知，采集器独立保存其完整回执与已看到的消息；正常同库新PID重启仍能取得同一回执。然后停止owned应用和MySQL，恢复T0并重启，B申请相同座位时段、A查询/重放旧键、任务启动必须被隔离；恢复库不得形成第二份看似合法的新权。外部见证不是微信推送证明。

第二条trace：数据库仍有受跟踪事务但外部账册缺失/损坏/不属于本代，启动须NOT_RECONCILED，不能当作新项目初始化。正常重启、业务回滚、commit后丢响应、账册写失败及commit/rollback结果未知分别判定，不将本地IO错误编译成数据库回滚。

## 最小工程边界

计划采用单实例、单库的独立持久事务账册与库内同事务marker。对每个Spring管理的非只读提交，在实际commit前先持久PREPARE，再于该事务内写唯一marker；只有实际commit/rollback结果已知才追加对应终态。账册在MySQL数据目录/恢复对象之外，独占文件锁和有界校验，保存generation及链摘要，不含票据/密码/个人正文。

启动核对generation和全部已跟踪坐标：已知COMMIT缺少marker、未知PREPARE无marker、marker无合法账册、坏字节或不可读写均隔离。未完成PREPARE但marker真实存在可以认定该事务已commit并留恢复事件；不能因一个数据库查询失败猜测不存在。库内业务事实和marker必须是同一事务，不在afterCommit内另写marker。账册完成写失败可能发生在commit之后，保留已成立事实和待确认状态，停止后续动作。

这不是分布式事务、外部通知exactly-once或自动恢复使用权。正常一致时不改变原领域锁序；不以全局数据库行锁替代资源裁决。文件IO仍有预算和资源上限，耗尽保留明确边界，不截断历史再声称一致。第二个进程不能共享同一账册继续写；生产多节点另立合同。

首次新库可在空业务事实及无旧generation时初始化。已有V1–V9库接入需要显式、一次性的基线采纳与保留摘要；不能伪造此前外部历史已对账。已有generation后缺账册，任何初始化开关都不能越过隔离。恢复代的解除需将缺失事实恢复到与独立账册一致或提交明确的人工处置证据；不提供网页“强制继续”上帝按钮，不删除原失败与原代。

## 来源与推断

实际JAR是Spring7.0.9：[事务管理源码](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/support/AbstractPlatformTransactionManager.java)与[Listener](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-tx/src/main/java/org/springframework/transaction/TransactionExecutionListener.java)分别区分commit调用和之后的回调；[原设计#27479](https://github.com/spring-projects/spring-framework/issues/27479)是已关闭增强，不是期序漏洞报告。[MySQL GTID文档](https://dev.mysql.com/doc/refman/8.0/en/replication-gtids-concepts.html)描述数据库提交跟踪，不替期序证明学生已见通知与恢复库一致。上述方案是期序的工程推断，须用本项目原trace证明。

## 采集与退出

原生0.15/Plan3仍执行147条已声明见证，仅增加本合同坐标绑定，不证明物理恢复。恢复观察 `qixu-m7-restore/0.1 / Plan1` 先封存，再启动全新owned实例；绑定同SHA原生PASS与fresh JAR完整Bundle、迁移摘要、固定MySQL/Java路径及合同。观察物理停止/重启、T0 dump摘要、库外见证摘要、HTTP原文/SQL终态、资源归属和清理；不得预填实际状态或允许的403代替503。

先采原候选首败再修。整实例只在本项目新目录、loopback独占端口和捕获Popen句柄中创建；每次停止核对PID/启动/CIM/exe/数据目录或JAR，宿主3306及其他服务不动。私有dump、原日志和外部见证保留；公共Bundle只出脱敏事实。失败不能提前终止所有负向请求而丢失分类，环境失败另记ERROR/PENDING。

当前不证明整个主机和账册一起回滚/恶意特权SQL/文件篡改、多节点、磁盘真正掉电、异地灾备或外部微信送达；这些须按F12账册保留触发、影响、缺证据、环境、重入和归属。核心已观察错误不能用这些边界掩盖。M7整体及M8–M10仍待闭合。

## 版本1.1 · 仅观察生命周期修订

原0.1/Plan1首包PENDING与父进程清理证据缺口保留，见错题本；没有业务恢复结果。0.2/Plan2使用MySQL8.0.44支持的`--no-monitor`，不调用SQL RESTART，仍在每次SQL/HTTP和停止前核对Popen PID/exe/本轮my.ini或JAR。退出另检查两个owned端口无监听，且source_clean成为硬断言。恢复业务断言、原包及未知边界不改。fresh producer按新source重建并绑定修订合同，不使用旧产物升级新执行。
