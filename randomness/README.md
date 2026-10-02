# 离线随机证明验证器

Node24+，在项目根目录执行 `npm ci --prefix randomness --ignore-scripts`，再执行 `npm test --prefix randomness`。Java通过stdin调用 `randomness/verify.mjs`，默认node来自PATH；可用QIXU_NODE/QIXU_RANDOM_VERIFIER指定部署绝对路径。不是HTTP服务，没有端口或中间件。

固定drand-client1.4.2及lockfile，固定quicknet chain、公钥与scheme。只接受指定round及beacon，不接受换公钥、latest或跳过验签。验证流程使用官方客户端BLS验证，不能用HTTPS或SHA(signature)替代。Java网络获取在数据库事务外，有独立超时；验证失败不能发布。

fixtures/quicknet-32721736.json于2026-10-03从[官方精确轮次](https://api.drand.sh/52db9ba70e0cc0f6eaf7803dd07447a1f5477735fd3f661792ba94600c84e971/public/32721736)获取，仅作为真实公开签名验签夹具，不用作生产批次种子。测试包括错误round/chain/hash、伪签名配正确伪hash以及进程输入限制；不代表未来来源获取或业务分配已验收。
