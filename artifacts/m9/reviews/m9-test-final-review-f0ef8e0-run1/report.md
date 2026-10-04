# M9 第一轮原件独立测试复核

结论：固定源码 f0ef8e0523b4441fbd710796f75d881f2476e4f1 的第一轮原件在本次有限核对内未发现阻断。支持第一轮实际证明的构建、已声明业务页面事实、未来随机批次、同库正常重启与 owned 收束。本报告不授予两轮 M9、公开交付或 M10 资格；父级、第二轮与交付包随后另作增量原件报告。

复核者：独立测试验收代理 /root/qixu_m8_pm11_test_recheck。从 fixed source 与 engineering-delivery/0.2 合同恢复范围，读原件、逐张看 JPEG，重算清单字节、完整隐私投影、XML、静态 ZIP、时间关系、SQL与日志。未启动服务、访问数据库/端口/浏览器、重跑产品测试、修改 tracked 或扩张 M7/M8 排列。辅助使用 fixed source 的只读 bundle 导入函数，另行验证原 XML/ZIP/页面/日志，未调用 finish 或创建 Core 裁决。

源码和父封存：实际 fresh clone .tools/919013c951 的 HEAD 为 f0ef8e0523b4441fbd710796f75d881f2476e4f1、tree 6c6c6cfa55f6bad17f9d83d90384e20db34e75ac、origin https://github.com/NoctilumeDev/Qixu.git，工作树干净。父 artifacts/local/m9-engineering-bc4fdd5dfe7442aaaa4b4a1719013c95 为 Plan2 / collector qixu-engineering/0.2 / contract0.2，seal digest 05bfd1f401f0f1a16d5a9c5599b0040ae71bdf3c05abfd23359fd756592f10da。父绑定的脚本、M9StopAgent、合同、依赖锁、V1–V10迁移与素材字节匹配 fresh clone。run-record 的 checkout_relative 与父 identity 重派生一致。第一轮 UTC 2026-10-04T02:11:47.304395+00:00 至 2026-10-04T02:32:48.274340+00:00，晚于父 seal。

|边界|本轮原 identity|Core原裁决|privacy改字段数|
|---|---|---|---|
|native|m8-98ad47b0bc6141c5861433b1e680b47e|PASS / COMPLETED|0|
|frontend|m8-frontend-c6c30927ef5d44c6ae804fd2c848d4e8|PASS / COMPLETED|2|
|live|m9-live-3bab2854a6b34b5d9916a057dd6adff1|PASS / COMPLETED|0|

三个 Bundle 全部 manifest 路径、文件集、长度、SHA256、sealed Plan digest、subject、collector、版本和原 request/coordinates 均一致。raw Evidence 完整文档经固定 Core privacy/0.1 投影后与 Bundle Evidence 全等；report retained facts_digest 等于 raw facts 重算，次数、flag、rule/spec/request/Plan元数据一致。三者 captured_at 均在本轮 start/finished 之间，identity不同。前端2个改字段为已知 required_cases 标签，未放宽业务布尔判据。

native：原21份 surefire/failsafe XML 全部 testcase 映射等于 raw tests，181/181、无 failure/error/skipped，command_exit=0。frontend：原 client-tests.xml 对 required_cases 的映射等于 raw tests，61/61，五命令均exit0。H5 117、WeChat163、admin9文件的完整实际文件集、逐文件长度/SHA、canonical manifest digest，与三原ZIP全部成员一致。source.zip SHA匹配 producer，Git archive comment绑定 fixed SHA，1755个tracked文件集合完整。原生/前端构建/真实页面是不同证明边界。

retained-package.jar、clone 实際启动 JAR、native package与live坐标同SHA256 3457b243e6bca056ef50a506255b207b74c7c07413ccc29b40e6c6946e1a6d77，26,743,520 bytes。stop-agent.jar同SHA256 4b92fda03b5ff86613b26efad1de3705d0540ec46c0d415541d93d62ced702be，record/live Plan/编译class/JAR class一致；源hash受父Plan绑定，Premain-Class为M9StopAgent。

七正式原 JPEG、DOM、metadata 已逐件查看/比对，source/parent/run1、route、CSS viewport、scroll_width、ready后且finished前时间关系均一致。6个手机CSS390×844，admin venue1280×720；全页JPEG高度不等同viewport高度。

|正式状态|JPEG像素|实际事实|
|---|---|---|
|student-space|374×13845|A001档案/容量/设施与正常恢复事实，大量展开日期年份列表|
|student-short|374×888|A001待到场，短约时间、到场期限、确认/取消|
|student-long|374×1216|结果已公布，2志愿要约/2预授权益、固定候补、接受第二志愿、暂离保权|
|student-event|374×888|明确M9演示，自己的已确认参与回执|
|student-feedback|374×1111|自己的反馈已解决；工作完成与复验恢复分别留证|
|admin-venue|1264×814|管理员会场已批准，R-A、1人/容量8、version2与回执|
|admin-repair-mobile|374×1542|复验关闭/version4，工作完成后曾待复验，明确恢复后关闭|

首次desktop-student-space三原件仍在capture-attempts/desktop-student-space，02:26:32.844Z、CSS1280×720、JPEG1264×1226；正式390复采02:27:47.757Z。不同路径和字节均保留，不计正式第八状态。student-space超长picker与其他全页截图固定导航/下方内容边界保留，不能宣称手机紧凑布局或视觉精修完成。AGENTS已冻结M10 DEFERRED_HUMAN_PARTICIPATION，本轮不扩张精修。

SQL before/after restart均[1,1,1,1,1,10,1]，依次为有效短约、批准会场、确认参与、解决反馈、复验关闭维修、10迁移、同库证明；live前六项投影全等。两人批次仅一个formal result，2outcomes/2offers/2result notices，最大匹配2。public-packet等于raw packet，input/output字节hash等于reproduction，frozen input未改，冻结早于未来drand round32758995，随后公布。原qualified live保留signature_verified/independent_bytes_equal；本次文件复核不是再次连接随机源/复跑故障。

正常重启：live owner PID9040与原JAR/source一致；restart owner PID38984明确同一个exact JAR与本轮agent。两JVM原日志均含M9_GRACEFUL_STOP_REQUESTED、Tomcat Graceful shutdown complete和Hikari Shutdown completed。live cleanup exit0/markertrue/normal_exittrue；restart normal_stop OWNED_STDIN_SYSTEM_EXIT_0/exit0/markertrue。同库SQL不变、health200/auth200/own_reservations1，日志schema up to date。live journal非空，与native journal路径不同；固定采集器同env/DB/journal重启，未DROP live库。

owned cleanup原record记restart-app/frontend-server/live/mysql全stopped，threads_stopped=true，同轮结束时6980/6967/6968/6969全free；owner记录和JVM退出日志支持归属。本次未探测端口或终止进程，free仅指第一轮finished时刻。

未证明：第二轮fresh身份、父完整20条件、public导出byteequal、交付包、公开tag/CI/release、冷缓存安装、真实微信设备、生产和M10精修。本报告不以文档/截图存在授全产品PASS。无本轮阻断，维持合同有限停止线。
