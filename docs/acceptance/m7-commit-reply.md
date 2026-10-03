# M7 · 真实COMMIT回应丢失

当前为施工候选限定资格，尚非受保护main或完整M7。先冻结[合同](../contracts/m7-commit-reply.md)，原29项HARD断言不变，独占MySQL/API/TCP中继；不接管共享实例。

原源`978983a079c0f638d90a645d28d5f909210f21e8`的[Core FAIL](../../artifacts/m7/m7-commit-35238faf86214af08fc9df4f516ca4d2/acceptance-report.md)为COMPLETED：精确COM_QUERY COMMIT命中并丢server回应，独立SQL确认真实唯一提交、一个marker、同generation；首响应30.031秒却为500 INTERNAL_ERROR。随后实时NOT_RECONCILED、同库新PID恢复原回执与完整账册均正常。错误由Spring rollback失败包装覆盖原通信原因；分类PRODUCT_ERROR_ENVELOPE，不是重复业务提交。

合同1.1先于修复固定：只修数据库事务异常的503映射/未知结果文案；非数据库编程异常仍500，不泄露cause或SQL。事务、journal、marker、迁移和中继不变。修复源`18cba8543f1ed7abfec49d4833a37f4251615f9a`的[原29项PASS](../../artifacts/m7/m7-commit-ea058a4436a2487ea7dfb27ce76ef7c5/acceptance-report.md)与新增三个异常边界native173原包均保存。

当前页面修复候选`6ae43a4ff112b6984fa17f68d1fab6133580e9d6`重新生产[native173](../../artifacts/m7/m7-a3a62b4fc3224eb8b68b60212802353e/acceptance-report.md)，再次执行[COMMIT29 PASS](../../artifacts/m7/m7-commit-35fa2caf31154f0591afe2ae75b637f0/acceptance-report.md)：真实首响应30.063秒503 DATABASE_UNAVAILABLE，唯一提交/marker；四个隔离入口均503 NOT_RECONCILED且SQL不变；新PID同generation恢复原回执与重放完全相同。独立账册8 marker/8 commit/17 event，RECOVER_COMMIT恰一次；正常对照保持，所有owned和中继停止。

这些证明的是单实例真实TCP提交通信歧义与同库恢复，不证明整个主机回滚、硬件掉电、多节点、主从切换或生产容量。未知有[具体重入坐标](../m7-unknowns.md)。修复前FAIL与各新PASS按原字节公开；不能用后来的UI或CI覆盖原失败。

复验使用已安装Core虚拟环境：先在exact clean source生产native0.19/Plan7，再运行 `.tools/veritrail/Scripts/python.exe scripts/veritrail_m7_commit.py --producer-bundle <fresh-native/bundle> --java <java17> --mysql-bin <mysql8-bin>`。先sealed Plan再安装；secret只在私有环境，stdout不打印凭据。
