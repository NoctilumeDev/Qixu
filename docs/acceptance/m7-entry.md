# M7第一段 · 协议、恢复主体和消息投影

状态：**施工候选限定复验**，M7整体IN_PROGRESS。合同先于测试：入口08c90b7；第一次测试e78f0b4，前端采集选择纠正10c2a96；修复源 `4dbf3642a4cdb812de3681b0ad09d1cff24b33b4`。本记录没有主线M7、实际浏览器、灾备或设备资格。

## 首败和修复对应

| trace | 原始观察 | 修复与原标准复验 |
| --- | --- | --- |
| JSON-DUP | 三种重复/转义同名字段全200，3回执/1收藏 | Spring HTTP JsonMapper严格重复检测；三种均422，收藏/回执0 |
| JSON-TAIL | 对象后对象/布尔/数组均422 | 正确拒绝保留；合法空白200，唯一收藏/回执 |
| INBOX-151 | 页码忽略、每次最近100、total缺失；151本人记录并集只有100 | 页码/大小约束，单请求主库REPEATABLE_READ快照；8页并集与本人原始SQL151相等，旧消息已读151→150，其他5条不可读 |
| ACTOR-KEY | B被A同名记录阻挡；A迟到200擦掉B的UNKNOWN | 查询/停止/清理全部绑定actor+key；B可恢复而A记录保留，A迟到只清A，B当前主体和key不漂移 |
| STORAGE-DENIED | 构造/扫描/删除原生Error；本地删除异常提前中断注销 | 受限构造，读写故障明确503、无新传输；删除失败保留key；先清主体和敏感正文后仍尝试服务器注销 |
| COLLECTOR-OMISSION | 封37用例却只执行31，Core PENDING | 先修订观察边界1.1，再用frontend0.3/Plan2明确选择两份文件；断言不放宽 |

全部原Bundle字节保持，索引见[artifacts/m7](../../artifacts/m7/index.json)。首次原生[FAIL](../../artifacts/m7/m7-3a768c84c51f4c57a6dc0092e2223919/acceptance-report.md)、采集遗漏[PENDING](../../artifacts/m7/m7-frontend-bc89491def23465ba8ddc6139a56c195/acceptance-report.md)、真实客户端机制[FAIL](../../artifacts/m7/m7-frontend-0939b9fc2e294245897292ec688b68e8/acceptance-report.md)分别保留，不追溯改成PASS。

## 固定修复候选

- 新原生[native0.13/Plan1 PASS](../../artifacts/m7/m7-9d0c92074a054c9c8183dda757c2559d/acceptance-report.md)：145/145、命令0，142项旧回归与3项新增HTTP/MySQL见证。隔离qixu_test、READ_COMMITTED写事务；分页查询单独REPEATABLE_READ。V1–V9原字节不变。
- 新前端[frontend0.3/Plan2 PASS](../../artifacts/m7/m7-frontend-c65075cd30d647999afaf66a7cbf653a/acceptance-report.md)：37/37、五个命令0，干净git archive、锁依赖、H5/WeChat/Admin新产物。Transport/Storage故障可控；不是浏览器真实策略或微信设备。
- 学生消息加入20条分页和独立未读/总量，加载/失败不显示虚假零。此页面改动只有构建证据，**实际操作仍待M7安装观察**。

复现命令：在同一干净提交/Core0.13.0、专属qixu_test或qixu_ci环境，运行 `python scripts/veritrail_native.py --stage m7 --maven <mvn>`，随后 `python scripts/veritrail_frontend.py --stage m7 --node <node> --npm <npm>`。新身份、新Plan，不能覆写历史；具体运行秘密只从私有环境提供。

## 后续入口

F01–F15总账册尚未闭合。下一步包括真实页面翻到旧结果、存储部分成功后异常/损坏元数据、截止/版本/权限/三方使用权组合、进程停止和通知故障、独立备份交付世界对账。已观察核心缺陷不得改写为NOT_PROVEN；未知按触发/影响/缺证据/重入/归属保存。M8两名独立角色在M7之后，M10精修仍保留。
