# M9 两次干净工程复现

状态：IN_PROGRESS，尚未取得工程候选资格。范围以[交付合同](contracts/engineering-delivery.md)为准，M10等待用户参与。

使用公开固定的VeriTrail Core0.13.0及Python3.10、Java17、MySQL8.0.44、Maven3.9.11、Node24.14.0/npm11.9.0。安装依赖和工具路径见[运行说明](running.md)。本采集器为Windows单实例项目适配器，不是验迹提供通用多服务托管能力。

先在受保护、干净exact main执行：

```text
python scripts/veritrail_m9.py seal --mysql-bin <MySQL/bin> --java-home <JDK17> --maven <mvn> --node <node> --npm <npm> [--git-proxy <origin>] [--random-proxy <origin>]
python scripts/veritrail_m9.py run --output <新父观察目录> --run 1
python scripts/veritrail_m9.py run --output <同父观察目录> --run 2
python scripts/veritrail_m9.py finish --output <同父观察目录>
```

两次run串行，各从公共GitHub重新clone。端口6980、6967–6969已有服务即拒绝，不接管现有实例。新库仅schema账号，凭据仅驻留进程内存。native181及frontend61保留独立原Bundle；native测试世界随后与demo世界分离，两个journal不共用。真实未来round、维修复验和通知权限链完成后打印`M9_CUA_READY`。

此时由CUA沿正常页面建立场地申请、批准、活动草稿、发布和学生参与，并保存父Plan预定的七个页面原triplet。学生短约按当天服务器开放窗口，长期结果、本人反馈与手机维修页读取本轮正式事实。每个triplet包含完整DOM文字、只读URL/viewport/UTC元数据及实际JPEG，不能用接口代造页面。照片与空间均为demo，不等于现实场馆或微信手机。

确认采集完成后向run输入小写`stop`。应用正常停止；同一数据库、同一独立账册正常启动后重新登录和读取本人预约，再核对SQL不变。只停止持有句柄且exe/参数匹配的进程，四个端口必须恢复空闲。

父finish核原Bundle所有字节、封存Plan/collector/source、确切JAR、三端静态文件、页面尺寸/文字/时间、SQL及重启事实，再交Core裁决。任一缺失或失败保留ERROR/FAIL，不覆盖原观察；修正边界后使用新父身份重新运行。

fresh指两份全新工作区、依赖实例、独占MySQL和运行数据，不宣称清空宿主共享下载缓存、无网构建、字节可复现JAR、生产容量、多节点、真实校园SSO、现实维修或微信设备。公开工程候选和最终视觉资格分开。
