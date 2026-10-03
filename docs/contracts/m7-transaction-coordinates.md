# M7真实事务坐标 · 版本1

固定现有V10与单实例合同，不改变业务规则。新增真实MySQL机制见证：

1. beforeCommit时同一Spring连接可见业务行与marker，独立连接均不可见；提交后同一对同时可见。只读权威观察使用数据库真实隔离，不将listener回调次数当成数据库事实。
2. REQUIRED加入当前事务不产生第二marker；REQUIRES_NEW提交后外层真实回滚，内层业务行和marker保留，外层业务行不存在。
3. 普通业务回滚未进入PREPARE时不产生marker；明确只读事务不追加marker。同库已有marker不删除。
4. 禁止savepoint嵌套不能被误当独立提交；拒绝无新行或marker，正常事务仍可完成。

原162条继续执行，各新增例用原始SQL marker集合差集、独立连接隔离及业务PK归约。测试使用qixu_test/qixu_ci，tasks关闭；不清恢复代/marker/journal。不是磁盘掉电、驱动真实断网、多节点或外部微信证明。先合同后实现，native入口版本化，F12原恢复22条不降低；新producer按新坐标重新绑定。

## 固定观察入口

本合同先于新增用例实现。native0.17/Plan5在原162例上增加4个真实MySQL事务用例，共166项，分别留marker集合差集/业务行/独立连接测量。restore0.5/Plan5仅绑定新的producer入口并保留0.4全部22断言；执行前独立核对原首FAIL的18项为子集。不删除现有恢复证据，不升级全部M7。
