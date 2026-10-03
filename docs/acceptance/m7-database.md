# M7 F15 · 数据库停滞事实

合同[数据库预算](../contracts/m7-database-budgets.md)在116b3fa先于观察器。固定f46cf774c7a6d1048e39e1479ff806c4ed4ac1ec的新[native PASS166](../../artifacts/m7/m7-71ea391836c147eea3e76f6922b765d3/acceptance-report.md)绑定全新JAR，内含Connector/J9.7.0、Hikari7.0.2。随后新owned数据库/中继观察[database0.1/Plan1原FAIL](../../artifacts/m7/m7-database-54e73228d8e14232860b3d1290f43463/acceptance-report.md)，execution COMPLETED，24条断言原包保留。

## 首次分类，先于修复

- 真实MySQL floor锁等待已观察；app session lock_wait为50秒。15秒窗口结束仍无HTTP结果（采样循环真实16.48秒仍等待），guard保持期间新业务行/回执/marker均零；释放后原请求及同键重试最终唯一预约/唯一回执。产品违反事先固定的锁预算，不是重复使用权。
- 网络探针只证明已有连接被丢回应；5.282秒503是相邻存活检查路径，不能升级为已借出业务SQL的socket预算。真实业务查询是否被击中尚缺证据。观察器需精确触发COM_QUERY，正常放行ping，不扩大产品修复结论。
- guard的ROLLBACK及GUARD_RELEASED已实际执行，后接同一行quit被mysql当SQL拒绝。原结果不改；此夹具命令错误不抹除已观察锁等待，但新观察器改为ROLLBACK后EOF并要求正常退出，不能拿非零退出充当干净对照。
- owned子进程/中继均停止，6975/6976/6977无监听；共享数据库未注入故障。私有SQL/报文/原始log保留在原身份目录。

当前仅首败分类；尚无修复资格，M7未完成。

## 0.2夹具未就绪，保留原PENDING

固定dbdb83b的[native166 PASS](../../artifacts/m7/m7-c77b660b7d2e48628e4befd3f6abb89e/acceptance-report.md)后，[database0.2/Plan2](../../artifacts/m7/m7-database-0997e88828a54be9aef3d749cc7f66f5/acceptance-report.md)为PENDING/ERROR：owned实例的sslMode=DISABLED不再满足caching_sha2_password初始RSA交换；Connector/J默认拒绝未信任的public-key retrieval，app在health前退出，未执行故障请求。此为观察夹具前提错误，不是产品借出查询结果。所有owned停止/三端口无监听。0.3先约定从本轮owned MySQL数据目录绑定自动生成的public_key.pem，记录公钥摘要；不启用任意取key，不改产品TLS或共享库。

0.3入口f9fc054先发生Python3.10 f-string语法错误；命令未正确检查退出码，随后原生166仍PASS（[原Bundle](../../artifacts/m7/m7-eb5225adaaf84fabba4f8d29e5e302f1/acceptance-report.md)），该PASS不验证观察器语法。故障采集未启动，无database0.3 Bundle。修正语法并新增CI scripts compileall，后续用check=True顺序执行，编译失败禁止提交/启动。原错误私有坐标保留，业务预算仍未修。

## 已借出查询的真实负向，先于产品修复

固定c4f572440d0ad444096384541dee2bd430a78dd5的[native166 PASS](../../artifacts/m7/m7-afb489561d914ea5957f3d8cd2f99ac6/acceptance-report.md)后，真实[database0.3/Plan3 FAIL25](../../artifacts/m7/m7-database-fa6ca6208c294463a695ca6fa68fb936/acceptance-report.md)为COMPLETED。本轮公钥绑定、正常就绪与短约控制成立；guard释放正常退出。锁等待15秒仍无响应，保持guard期间业务/回执/marker零；释放后同键效果唯一。已存在连接上的auth_session COM_QUERY实际命中并丢server回应，35秒仍无响应；独立SQL无业务/marker变化。解除故障后需停止/重启本轮app才能结束原未知读，正常读取与原回执恢复。全部owned/中继停止，三端口无监听。

分类：实际锁预算与已借出网络读预算违反合同。未知读不被宣布数据库回滚；本例只读不证明commit丢响应。按合同版本3实施最小配置/启动校验，保留25条原标准，不修改原包。

## bfd37f0预算修复限定复验

固定bfd37f03c8eaa69e69d1429f0eed82e60e1df340的新[native0.18/Plan6 PASS170](../../artifacts/m7/m7-c9aaf7a6f0694c57a26c1528693bbba5/acceptance-report.md)保留166并加入4个实际驱动解析用例。新[database0.4/Plan4 PASS25](../../artifacts/m7/m7-database-fb76310ae3554b08b80a7713506ac894/acceptance-report.md)保留原25断言：实际app session锁预算10秒；持锁请求10.094秒结构化503、无业务/marker；恢复同key唯一。已建立连接业务COM_QUERY被阻断后30.047秒503；无业务/marker变化，解除故障无需重启同PID恢复读取与原回执。全部owned/线程停止、三端口无监听。

该坐标的[restore0.6/Plan6](../../artifacts/m7/m7-restore-0525fb9a74c84b82a28c2e989dad4c41/acceptance-report.md)却PENDING/ERROR：第一次health响应是APPLICATION_NOT_READY 503，观察器误当已稳定状态，未发任何业务/恢复请求。它不支持恢复失败或恢复通过。最小修正startup状态等待，原55秒预算与22断言不变；原Bundle保留，后续新source复验。

## e704b10同坐标复验，尚待主线资格

固定e704b10e2ae739648f5551928f121b4ba4cecf46的新[native170](../../artifacts/m7/m7-f4be5143a38d46caab9ec32cb0fe1831/acceptance-report.md)、[restore0.7/Plan7原22项](../../artifacts/m7/m7-restore-98af32b2df054f5186c33bda423aaac1/acceptance-report.md)及[database0.5/Plan5原25项](../../artifacts/m7/m7-database-b615ce5733ff4d38a6ce222157f9c5b3/acceptance-report.md)均COMPLETED/PASS。观察器只修正启动前提，不改变业务判定：APPLICATION_NOT_READY继续等候，200或明确NOT_RECONCILED才是可判定状态。

真实锁等待10.109秒返回503 DATABASE_UNAVAILABLE；guard期无预约/回执/marker，同键恢复唯一。实际已借出的auth_session COM_QUERY丢server回应30.047秒返回同码；解除故障后同PID读取及原回执恢复。恢复对照保持T0隔离、完整T1正常、仅缺账册隔离，外部world不变。所有owned及relay停止，6975/6976/6977无监听。三个原包按字节发布，96个历史包校验保持，失败未删。

仅为候选限定复验，尚待受保护合入、exact-main CI及新的原生/安装事实。它不证明COMMIT回应丢失、多节点、生产总API时限或微信设备；M7仍IN_PROGRESS。

## 受保护主线限定资格

PR#20候选c6ef1195e028152af2e4cd75bc488301c175d976的CI37104452916三项及所有步骤success，普通受保护squash合入main@ffa01ffe4e97728352cbbafc5349172d7a2ffbc3。exact-main CI37104669176三项及所有步骤success；远端main一致，README全字节读回与本地相同。

该主线全新[native170](../../artifacts/m7/m7-805001f3b21349b19ef4cf524113423f/acceptance-report.md)、[restore22](../../artifacts/m7/m7-restore-9e75cadba74749da8f8837933ce6d696/acceptance-report.md)、[database25](../../artifacts/m7/m7-database-f95a73c66a4b4edcab194455a2ea3ea7/acceptance-report.md)各有新身份且COMPLETED/PASS。锁响应10.094秒、目标业务COM_QUERY丢回应30.031秒，零故障期新增效果、同key恢复唯一、同PID恢复原回执；T0及单缺账册保持隔离。owned及relay全部停止，三端口无监听。共享3306/6947/7897的PID及启动时间与初始记录相同。

99个历史Bundle字节保持。此资格仅主线单实例数据库预算与既定恢复范围，未升级整个M7。COMMIT丢回应另按[新合同](../contracts/m7-commit-reply.md)攻击，不能由本段只读故障代替。

## 后续主线对齐

上述段落保留其发生时的候选/历史范围。main@c876d1a的新同源运行、公开CI及有限退出统一见[M7](m7.md)，不是把历史Bundle改写成新资格；微信设备及生产边界继续NOT_PROVEN。
