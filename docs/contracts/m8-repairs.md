# M8 · 独立反例的最小修复与原条件复验

2026-10-03，实施和新执行前冻结。原独立审阅 source `156ae25110c4e9d79599c565dca0de8cd43845e4` 与 origin/main `c350a50ca7d8f8dadf9a4c47002645d864939c59` 的运行树一致；历史 M7 运行资格仍只绑定 `c876d1a…`。原报告、首次模型输出、实际页面和错误夹具不覆盖。本轮修复分支 `fix/m8-independent-findings`。

## 处置范围

| 原反馈 | 当前分类/义务 | 新见证 |
| --- | --- | --- |
| FT01 内部恢复所有权 | ACCEPTED_FIX：await 后清理/返回前再核主体与generation；合法settled后换主体不能倒改Promise | M8 recover rechecks ownership before receipt adoption；保留原键及合法settled正控制 |
| FT02 Vue旧集合回写 | ACCEPTED_FIX：每个await结果先局部保存再核页面/run/主体；离开、换主体清所有私有投影 | M8 Screen leave and generation change cannot repopulate favorites |
| FT03 固定LIMIT10维护饥饿 | 待真实API/MySQL原条件复现；11合法非重叠批次、前10无空位、末批空位有兼容候补；禁止只加大LIMIT | publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation |
| FT04 候补退出版本 | ACCEPTED_FIX：DTO使用waitlist.version；保护已有兜底，不能绕过后端版本 | M8 waitlist exit uses its own version；waitlist exit真实当前/旧版对照 |
| FT05 截止投影与资格 | 待裁决/真实见证；不得由FT03推定期限扫描永久饥饿 | 暂停任务→截止→公开状态/另一批申请/原轮退出→恢复，明确允许结果 |
| PM01 未知设施变无 | ACCEPTED_FIX：存在性只允许显式有/无；未知不能被cast false；条件枚举单独；反馈/维修同入口 | M8 boolean facts reject unknown rather than manufacturing absence；四类真实页/公开事实读回 |
| PM02 审批信息 | ACCEPTED_FIX：决定前人数/当前容量/申请人/联系方式及公开档案入口；范围不放宽 | 授权详情真实API与桌面/390px批准前同操作 |
| PM03 活动时限/地点 | ACCEPTED_FIX：开放/截止/递补停止、公开空间标识/楼层；仍区别参与名额和个人座位 | 真实公开详情及手机报名/满额候补页 |
| PM04 个人时间重叠 | ACCEPTED_FIX：提示已有有效短约半开区间重叠并给原安排入口；不新增禁止、不自动取消权 | 邻接不误报、重叠仍可报名/候补及原短约保持 |
| PM05 Bearer退出清Cookie | ACCEPTED_FIX：仅清当前实际COOKIE认证的cookie；Bearer撤销自身会话不发清cookie头 | bearerLogoutPreservesUnrelatedCookieSession；同浏览器正常换号/管理不退出 |
| PM06 旧成功跨模块 | ACCEPTED_FIX：路由/实体/主体变更清反馈；异步完成须归属当前上下文 | 管理原维修成功→批次，RequestState两端换主体/迟到对照 |
| PM07 失败原因原码 | 内容改进：人话原因保留诊断码 | 实际失败页 |
| PM08 发布必填理由 | NOT_PROVEN候选，保留真实错误DOM及无效视觉捕获；原操作有效图后再分类 | 未填理由、明确定位、填后成功 |
| PM09 反馈空间仅ID | NOT_PROVEN候选，补实际有效图再分类 | 反馈列表/详情公开code，不泄露私有报告 |

原独立测试者的安全审查中断单列 ERROR，没有最终报告，不能当完成。替代测试者和产品经理均从固定源独立审阅。产品经理自动链前提是 tasks=false，故未自动冻结/分配/通知只能 NOT_PROVEN，不是产品失败。

## 新执行合同

- native M8 `qixu-native/0.20` / Plan1，包含原 M7 的173项具名见证及新增声明；源、迁移摘要、实际HTTP/SQL、构建JAR逐字节绑定。默认测试任务关闭；维护selector见证显式调用启用的真实任务服务，在受控Clock下记录限定轮数，不伪称生产容量或墙钟时限。
- frontend M8 `qixu-frontend/0.5` / Plan1，保留原45项，加实际Client与Vue setup/模板回归。Transport/uni只为调度模型，不升级成浏览器凭据或微信设备。先封存Plan，再执行；原失败候选与修复后新identity分别留存。
- 实页复验另封当前候选及显式任务配置的新Plan。原M8 record只验证报告/四捕获/清理留存，不能表示无缺陷。私有报告/输入不直接发布；公开摘要与原证据摘要保留。
- 当前用户16条、公开来源及历史生命周期经验逐条回填对应见证及边界；仅历史已验、仅模型、真实API/SQL、实际页面和NOT_PROVEN分开。

## 保护与退出

不修改分配目标、固定来源/候补顺序、使用权唯一、原子升级、身份范围、回执/恢复及迁移旧字节。只操作专属测试schema/owned端口/进程；共享3306/6947/7897不停止。先保存首次失败，修复原操作/相邻控制；没有核心已观察缺陷未处置才允许M8退出。M9干净复现与M10精修/真机边界仍独立。
