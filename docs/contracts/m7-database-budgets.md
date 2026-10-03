# M7 F15 · 数据库停滞预算，版本1

先冻结观察标准，再注入故障。连接池获取、已借出连接的网络读、数据库资源锁等待、业务截止是不同时间合同。当前配置的3000ms取连接时间不能代替SQL或网络读预算。

## 范围与原始对照

仅新建本轮owned MySQL进程、数据目录和loopback TCP中继；固定clean SHA、新native0.17/Plan5 producer与同字节JAR。端口6975/6976/6977须事前空闲。进程按持有的Popen、exe、本轮my.ini/JAR及监听owner核对；不注入共享3306，不按端口杀他人进程。Core0.13.0执行前seal。

1. 正常短约成立，原key查询回执一致，独立SQL一条使用权/一份回执。
2. 独立SQL事务持有floor100 guard；HTTP短约在真实MySQL等待同一锁。最多15秒的固定观察窗内应返回结构化503 DATABASE_UNAVAILABLE，guard仍保持，副作用为零。观察窗结束仍未完成记明确FAIL，不能把HTTP客户端等待截止认作业务回滚。之后释放guard、读回结果；同key重试只能得到一条事实/同一回执。
3. 预热登录和查询，记录中继已建立连接。只丢弃server→client后续回应，client→server仍转发，统计原连接请求/被丢回应字节，确认不是单纯连接池饱和或初次建连。一个带身份的只读HTTP请求应在35秒窗内结构化结束，禁止新业务副作用。恢复中继后，正常读和已存在回执应可查询；客户端超时只记UNKNOWN。

每个窗以monotonic耗时测量；固定窗之外的真实完成时间继续留证，不放宽窗。停止故障和控制请求是原合同步骤，不抹除失败。SQL、请求原文、relay计数、进程坐标留私有；公开Core事实脱敏。夹具建立/等待锁/原连接被击中无法证明时ERROR/PENDING，不猜PASS。

## 最小修复的允许边界

若原候选FAIL，先保留Bundle及分类，再落实有界connect/socket/lock配置。初始目标：连接获取3000ms、建连3000ms、网络读30000ms、InnoDB锁等待10秒。15/35秒是本轮外部观察窗，允许数据库和API错误整理的开销；不是全部API的端到端SLA。实际依赖版本从新JAR读回；锁预算从app连接的真实session变量、网络读预算从被阻断原连接耗时确认，YAML文字不授予资格。不得静默接受URL/socketTimeout=0绕过预算。

锁超时/已知回滚不应制造COMMIT marker；未知commit仍保留PREPARE并隔离。取消HTTP不授予自动重放权；同actor/key查询或重试保持业务至多一次，截止以服务器重新取得锁后的事实为准。保留原native166与恢复22标准。

## 退出与未知

保留首败→最小修复→原15/35秒标准复验；正常/故障解除/唯一效果及清理都观察。此段不证明所有API总时长、真正网络分区、物理磁盘失败、多节点、生产容量或驱动commit返回丢失。后者需针对COMMIT边界的协议故障合同、marker与独立账册对账、固定源重入；维护者负责，不能从只读断回应升级。公共错题来源只是机制启发：

- [Hikari Rapid Recovery](https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery)：借出连接需驱动级socketTimeout。
- [Connector/J Networking](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-networking.html)：connectTimeout/socketTimeout单位与默认值。
- [Connector/J troubleshooting](https://dev.mysql.com/doc/connector-j/en/connector-j-usagenotes-troubleshooting.html)：通信失败不裁决commit是否成立。

本合同不关闭M7整体；真实页面、其他组合、M8双角色及M10仍按各自义务推进。
