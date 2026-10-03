# M7 F15 · 数据库停滞事实

合同[数据库预算](../contracts/m7-database-budgets.md)在116b3fa先于观察器。固定f46cf774c7a6d1048e39e1479ff806c4ed4ac1ec的新[native PASS166](../../artifacts/m7/m7-71ea391836c147eea3e76f6922b765d3/acceptance-report.md)绑定全新JAR，内含Connector/J9.7.0、Hikari7.0.2。随后新owned数据库/中继观察[database0.1/Plan1原FAIL](../../artifacts/m7/m7-database-54e73228d8e14232860b3d1290f43463/acceptance-report.md)，execution COMPLETED，23条断言原包保留。

## 首次分类，先于修复

- 真实MySQL floor锁等待已观察；app session lock_wait为50秒。15秒窗口结束仍无HTTP结果（采样循环真实16.48秒仍等待），guard保持期间新业务行/回执/marker均零；释放后原请求及同键重试最终唯一预约/唯一回执。产品违反事先固定的锁预算，不是重复使用权。
- 网络探针只证明已有连接被丢回应；5.282秒503是相邻存活检查路径，不能升级为已借出业务SQL的socket预算。真实业务查询是否被击中尚缺证据。观察器需精确触发COM_QUERY，正常放行ping，不扩大产品修复结论。
- guard的ROLLBACK及GUARD_RELEASED已实际执行，后接同一行quit被mysql当SQL拒绝。原结果不改；此夹具命令错误不抹除已观察锁等待，但新观察器改为ROLLBACK后EOF并要求正常退出，不能拿非零退出充当干净对照。
- owned子进程/中继均停止，6975/6976/6977无监听；共享数据库未注入故障。私有SQL/报文/原始log保留在原身份目录。

当前仅首败分类；尚无修复资格，M7未完成。
