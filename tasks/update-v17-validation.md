# RTBus 独立更新通道验收（2026-11-27）

## 结果与范围

用户授权采用“源码私有、安装包公开”，并在已连接手机测试。源码 `worlt/rtbus` 继续私有；公开新仓库 `worlt/rtbus-releases` 仅包含 README、原 MIT LICENSE、更新 JSON 和 Release 资产，没有复制私有源码历史、私钥或手机数据。

- 已发布：[RTBus 0.1.16](https://github.com/worlt/rtbus-releases/releases/tag/v0.1.16)，versionCode 24。
- 默认 / beta 清单均指向 RTBus 自己的下载仓库；首次 beta 与默认版相同，不回退上游。
- 沿用包名 `com.github.u5151561.trackr.debug` 与既有调试证书，采用 release 优化、不可调试的 validation 变体；未更换独立生产身份或签名。
- 保留原 MIT 版权、关于页“本项目 / 上游项目”及现有辅助功能。旧手机卡顿调查继续暂停，iOS 未验收。

## 源码与离线验证

- `GithubUpdateChecker` 默认 / beta 地址固定为公开仓库 main 分支的 `update.json` / `update-beta.json`。新增两项默认请求回归先红后绿；显式空源仍返回 NotConfigured 且不发请求。
- 元数据生成器要求明确公开说明、版本 tag 与实际源码版本一致，读取实际 versionCode，固定 RTBus 下载仓库和 ABI 文件名；不再从私有 Git 提交自动生成公开说明。新增 5 项回归先红后绿。
- 移除不适用于私有源码 / 公开下载分离的旧 update-branch 自动同步；采用 [经校验的显式发布流程](../docs/updates.md)，未存储跨仓库令牌。
- JDK 21 / Gradle 离线运行 `test :androidApp:assembleValidation`：BUILD SUCCESSFUL，2m 22s；160 份 JUnit XML 汇总 **1,687 项，0 失败 / 错误 / 跳过**。Gradle 仍有既有弃用提示，不声称零警告。
- `.github/scripts` 全部 unittest：**31 项通过**。`git diff --check` 通过。

## 安装包与公开读取

| ABI | 字节数 | SHA-256 |
| --- | ---: | --- |
| arm64-v8a | 4,911,166 | `82914b9f2ebe17f456be53827f17bafe9a6a6773173c5c9a8d9e65dfafda7fa0` |
| armeabi-v7a | 4,909,242 | `cad8e373f71d04fce5de44834e663c1d54e8411a1df45acef7e8bc1c7f9700b8` |

双 ABI 均通过退役 SDK / manifest 边界、与实际已装旧包同证书、R8 mapping、不可调试 / 可 profile、Baseline Profile、ZIP 与 arm64 ELF 16 KB 对齐核查。实际安装和系统权限验证只在 arm64 手机进行，不把 32 位包产物检查写作 32 位真机验收。

草稿 Release 仅上传双 APK、SHA256SUMS.txt、LICENSE 四个文件，核对资产后发布。匿名重新下载四个资产并逐一比对哈希；随后才推送默认 / beta / version 三份清单，匿名读取均与本地逐字节一致。公开仓库是独立新历史，GitHub 自动源码压缩包只含公开下载仓库内容。

## 手机真实路径与证据边界

设备为已连接的 22041216C（arm64）。没有卸载 RTBus、清数据或改全局动画 / 性能设置。

1. 从手机拉取实际 v16 `0.1.14 / 22` APK，并使用同证书临时数据工具只读备份数据库 / 设置。
2. 旧 v16 无更新源，通过 ADB `install -r` 覆盖一次带源过渡版 `0.1.15 / 23`。核对版本和原 firstInstallTime；备份 5 个文件哈希及数据库逐表内容未变。
3. 从“我的 → 关于实时公交”点击“检查新版本”，真实网络发现 v0.1.16，画面包含 RTBus 公开更新说明。
4. 点击“下载并安装”，画面进入“系统还没允许实时公交安装应用”的授权提示，说明包已实际下载并通过应用校验。
5. 随后系统完成安装。中间授权 / 安装确认画面没有被脚本完整采样，未据此声称脚本自动授权或自动确认。实际安装记录为 `installerPackageName = com.miui.packageinstaller`、`originatingPackageName = com.github.u5151561.trackr.debug`，未知来源权限为 allow。**没有通过 ADB 直接安装 code 24**。
6. 安装后版本 `0.1.16 / 24`，lastUpdateTime 为 2026-11-27 18:17:39，firstInstallTime 仍为 2026-11-26 17:06:27。拉取实际已装 APK，SHA-256 与公开 arm64 资产完全一致。
7. 再次点击检查更新，画面显示“当前安装的版本 v0.1.16 已是最新，暂无更新发布”。
8. 再次只读备份，5 个文件的哈希均与升级前完全相同；数据库逐表内容、schema 不变，integrity_check = ok。

数据实际样本：2 个收藏分组、3 条搜索历史、18 条站序缓存。收藏项 / 线路 / 站点 / 提醒 / 路线表为空；不能把它们记成非空业务数据迁移验收。该次升级未引入数据库 schema 迁移。

最终 RTBus 回公交首页；移除 `com.rtbus.probe` / `com.rtbus.upgradeprobe` 两个临时工具，关闭 ADB，核验进程和 5037 监听均为 0。

## 留存与后续

原始 APK、界面节点、数据备份、安装记录、构建日志和 API 校验仅留本机忽略目录 `build/rtbus-validation/update-v17/`，不提交或公开。实现提交为 `f5a6c9f`；公开下载仓库初始提交 `9d1dfa1`，元数据提交 `26037bd`，身份均为 worlt <2516930419@qq.com>。

后续普通发版依照 docs/updates.md：提高版本 → 测试构建 → 产物校验 → 发布资产并匿名校验 → 提升元数据。GitHub / raw 在当前手机网络已成功，但没有做所有运营商网络 / 厂商 / 32 位设备覆盖，也未做独立生产签名或 iOS 发布。
