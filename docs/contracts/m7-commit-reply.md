# M7 F12/F15 · 真正COMMIT回应丢失合同

版本1。数据库读取丢回应、框架受控doCommit异常、T0逻辑恢复分别已有证据，但都不证明真实MySQL收到COMMIT后回应丢失。这一段专门补该坐标，不重新宣称整个M7或生产灾备。

## 先声明范围

exact clean candidate + 同source的原native0.18/Plan6 PASS/JAR；Core0.13.0。全新owned MySQL8.0.44数据目录、独立schema、app6975/database6976/relay6977。禁止连接共享3306、停止青野6947或接管任何已有端口。driver9.7.0/pool7.0.2与原预算固定；只在loopback夹具关闭TLS/服务端预编译，并绑定该owned实例生成RSA公钥，不更改产品TLS。

独立观察器`veritrail_m7_commit.py`与原database25、restore22分开。Plan在实例初始化前seal；所有新包新identity，首败及ERROR/PENDING不覆盖。原观察器及标准不因新实验改写。

## 正常前提与最小trace

1. fresh迁移/demo明确启用、tasks关闭、health200；两个学生正常登录。先成功创建普通短约并读取原回执，独立SQL证明预约/回执各一条。
2. 同一学生在前一个短约结束后，申请另一个普通座位的相邻半开时段；原key全程不换。先记录marker集合及generation，已有池连接保持。
3. relay只在已有连接收到COM_QUERY且SQL恰为COMMIT时命中；支持无query-attributes及parameter_count=0/parameter_set_count=1的实际帧。正常放行鉴权、ping、业务SQL和客户端COMMIT，仅丢弃被命中连接的server回应。不得用substring或普通查询故障冒充commit命中。帧上限1MiB，私有报文不公开。
4. 按单调时钟观察35秒窗口。客户端必须得到结构化503 DATABASE_UNAVAILABLE；它表示未知/待确认，不表示回滚。若没有命中目标、没有server回应被丢或SQL未证明真实commit，前提不足，ERROR/PENDING，不能填已验证。
5. root独立TCP连接直接读真实MySQL：目标预约和原key回执各一条、notification_outbox相应效果唯一、marker新增加恰一条。SQL读回存储的response_json是恢复比较oracle，不调用被测service生成期望。保存原值/摘要私有，公开只给确定投影。
6. 解除relay故障但不重启：health、另一个学生竞争同座/同时间、原key回执和原key重试必须503 NOT_RECONCILED；SQL目标预约/回执/outbox/marker保持，不能因通信错误自行重新批准。
7. 停止positively owned app后，外部读取完整账册。独立验证ASCII、限额、序号、UUID、generation、hash chain及状态转换；唯一未完成PREPARE恰是SQL新增marker，不能把它写ROLLBACK或假COMMIT。
8. 同库、同账册、原key、同generation启动新owned PID。startup依marker对账为RECOVER_COMMIT，health200；原回执与故障前SQL存储结果完全相同，重复原key返回同结果且预约/回执/outbox仍唯一。原正常控制回执不漂移。
9. 再停本轮app，独立完整账册与SQL marker严格相符，无PREPARE残留；lost transaction恰一次RECOVER_COMMIT。允许demo重启本身另有正常commit，不把该marker当目标业务。新PID不等于新generation。
10. owned数据库/应用/relay句柄及线程全部停止，三端口无监听。清理失败ERROR，不能为了PASS按端口杀未知进程。

## 判定与停止

首响应错误、服务未隔离、假ROLLBACK、重复使用权/通知、回执丢失、恢复重复提交为范围内FAIL，保留原包及最小trace后修复并原标准重放。NORMAL前提错误、无COMMIT命中、未证明真实commit或独立读失败为采集ERROR/PENDING。UNKNOWN不能编译成rollback/404。发现共享资源受影响立即停实验并保留归属证据。

## 不证明与重入

此段只有单实例、真实TCP COMMIT回应丢失与同库重启。没有真实掉电、未确认TCP发送阻塞、整个主机/库与独立账册同时回滚、特权文件篡改、多节点、主从切换、生产容量或外部微信送达资格。这些未知归期序维护者，触发相应部署/事故/新版本时按恢复与预算合同重新冻结环境和Plan；不能用本段PASS迁移。

公开错题启发：MySQL官方[通信/commit歧义](https://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-troubleshooting.html)明确驱动无法凭通信失败判定commit结果；Hikari[Rapid Recovery](https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery)说明借出连接须独立driver预算。来源只是启发，期序是否成立由本段真实SQL/账册/HTTP判定。

## 最小修复边界1.1 · 不改原29条判定

978983a原native170 PASS和真实COMMIT观察`m7-commit-35238faf86214af08fc9df4f516ca4d2` FAIL完整公开。精确COMMIT命中、真实SQL已提交、实时隔离、唯一效果、RECOVER_COMMIT及原回执恢复均成立；首响应30.031秒为500 INTERNAL_ERROR。私有原日志定位Spring的`TransactionSystemException: JDBC rollback failed`覆盖提交通信错误；分类PRODUCT_ERROR_ENVELOPE，不是提交重复或恢复失败。

修复前固定：仅数据库原因的事务系统异常映射503 DATABASE_UNAVAILABLE，消息明确写入结果待确认并保留requestId；非数据库事务异常仍500，不返回cause/SQL/凭据。不得改变事务、隔离、账册、marker、迁移或relay，原COMMIT29断言全保留。新增三个边界见证（数据库事务/原异常/普通编程异常），native观察0.19/Plan7将170扩大至173；真实29项复验仍是主证据，单测不冒充物理COMMIT。
