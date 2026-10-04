# M9 公开投影独立测试校对

固定公开候选：d35d79dd3461e7fe6869ca3383038f8d83d39225，tree 0d32d32f252cf868f9549e1385bccc87dee819a7，其直接父为实际两轮运行源码 f0ef8e0523b4441fbd710796f75d881f2476e4f1。有限追加复核：944fc563dd0d5e805b4c5e0db803fa24225da84a，tree ba08844c986f9b1fdd679e3c340d4a16e940f389，直接父为d35。

结论：d35保留的原包/报告/页面字节和工程预发行坐标均对齐；其docs/running.md第3行仍写“M9正在准备”，是与本候选其他投影不一致的旧文案。后续944仅一行纠正该状态，已独立只读核对，无当前未处置阻断。支持候选范围内公开投影对齐，不授d35/944新的构建、安装或产品执行事实。受保护CI/main及最后owned可重建实例清理由父继续验证。

复核角色：/root/qixu_m8_pm11_test_recheck 独立测试验收者。只读fixed Git/合同/文档/原件及GitHub API；唯一新增为本.tools报告目录内report.md和metadata.json。未编辑tracked、重新安装/构建/运行产品或追加攻击排列。静态verify_docs和verify_artifacts只是结构/保留字节检查，未创建Core裁决。

d35相对f0共157路径：9份README/docs、artifacts/m9新增原件/索引，以及已有只读scripts/verify_artifacts.py的M9登记扩展。backend/student/admin/packages业务源码、三采集器、M9StopAgent、工程合同0.2、依赖锁/迁移没有改。frontend-refinement仅追加已有P3/采样边界并明确待用户重新开启，不启动M10。944相对d35唯一差异为docs/running.md第3行：改为M1–M9限定资格与M9已完成两轮/预发行，将“当前提交新的工程”收窄为“当前提交新的安装”资格；没有改变命令/参数/产品行为/原件。复核期间根工作树从d35前进至944，均无dirty；本报告固定Git内容，不依赖移动HEAD推定d35字节。

九份公开投影全文/改动与本角色前次两轮原件报告对应：source固定f0、Plan2/collector0.2、父20HARD、六新child、两未来round32758995/32759425、SQL七项、System.exit(0)/正常hook/同库journal/owned收束与包hash一致。旧三父PENDING/ERROR/不完整child未升级；native/front/live是不同证明边界。首桌面attempt、run1长日期picker、fixed导航/关闭维修通用表单与英文synthetic等M10边界明确保留；README使用run2实际390原图，没有宣称已修日期picker。fresh不含冷缓存/无网/bit-identical。本次未重看或重跑全部产品，业务运行支持来自已保存f0两轮独立原件报告。

171个保留Bundle的855个manifest payload全部长度/SHA/完整文件集匹配，旧裁决不变；215件M8原capture、65件M9原capture/run/incomplete全部清单匹配。直接对d35的Git archive原字节与工作树对应文件比较：1536个tracked artifacts全等，包含上述原件。artifacts/** -text保留CRLF，未为消除git diff --check对CR的提示而改原文件。新增校验器只报告byte_integrity=PRESERVED/qualification=UNCHANGED，不制造PASS。12个M9 Bundle仍完整登记，包含3旧父ERROR和旧430有限child。

5份公开review原文件（4角色报告+产品勘误）与.tools原件全字节相同、长度/hash相同、文件集无缺额外文件；review-records另外20份私有入口/metadata原文件的登记摘要全部匹配。本角色run1 SQL释义错项及完整报告勘误、产品标题后缀误写round及独立勘误均保留，未覆盖旧报告。正确SQL第2项=PUBLISHED活动、第7项=本人ACTIVE长期权；正确proof round32759425，批次显示标题后缀37871709不是随机源round。Review记录不继承角色亲操CUA或亲发布声明。

结构检查独立运行：verify_docs required13/observed13、error0；边界DOCUMENT_STRUCTURE_ONLY_NOT_PRODUCT_ACCEPTANCE。另外检查9变更文档的本地链接无断链；13文档观察文件与d35对应（后续变更只在不属于其13固定表的running首段）。语义范围由原证据复核，不以结构green代替产品资格。

实际GitHub只读确认：refs/tags/v0.1.0-engineering.1为commit，完整target f0ef8e0523b4441fbd710796f75d881f2476e4f1；release id402796600、非draft、prerelease=true、target_commitish=f0。两个uploaded资产如下：

|资产|ID|大小bytes|GitHub SHA256|
|---|---|---|---|
|Qixu-v0.1.0-engineering.1.zip|609000781|26,857,726|25dce0d17754036431ba03eddedbf97bb94bf9b9d6cde4b90ef02f90c45275e2|
|delivery-manifest.json|609000779|6,310|27692ebbddf326452efb54f9a8f0073ff5887b78fdcadf9a55bd77b7dff5c54b|

API id/name/state/size/digest/download URL与artifacts/m9/release-readback.json全匹配；.tools/m9-release-readback中保留的两个真实下载文件经本次独立全字节hash重算，并与.tools/m9-delivery原包/manifest全等。不是仅采用release-readback布尔自评。本次未再下载一份资产，远端亲查为tag/API metadata，下载全字节证明来自仍在磁盘的公开读回原件。最初不带proxy的gh读API失败“error connecting to api.github.com”，保留为本次读取失败边界；使用父封存已声明的loopback proxy在单命令作用域重试后得到上述原API结果，没有因网络失败倒填。

公开源：[release](https://github.com/NoctilumeDev/Qixu/releases/tag/v0.1.0-engineering.1)，[tag ref API](https://api.github.com/repos/NoctilumeDev/Qixu/git/ref/tags/v0.1.0-engineering.1)，[release API](https://api.github.com/repos/NoctilumeDev/Qixu/releases/tags/v0.1.0-engineering.1)。Tag指actual qualified源码而非纯投影。ZIP exactJAR/三ZIP/secret exclusion已在f0 complete原报告检验；本次只查该同一公开hash/大小/原下载，未重装或再次运行。

停止线：本报告不授d35/944新的运行资格，不授M10/微信真机/现实维修/SSO/生产/多节点/冷缓存/整机恢复等未证明边界，也不宣称受保护合入和main读回已由本角色完成。944一行公开状态冲突已最小处置，无当前未处置阻断；后续protected CI/main原字节和owned清理仍由父完成，本轮不扩大产品验收。
