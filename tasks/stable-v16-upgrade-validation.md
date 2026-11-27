# v16 本地稳定候选版与覆盖升级验证

日期：2026-11-27（Asia/Shanghai）。用户已反馈日常功能正常，并重新连接旧手机，授权自行完成后续操作。本轮完成本地 Android 候选版准备与真实覆盖升级；未恢复卡顿定位，未作远程发布。

## 版本、包名与签名方案

- Android 版本由 `0.1.13 / 21` 升至 `0.1.14 / 22`。产品改动仅版本元数据两行；v15 的横滑、返回分发与其他业务实现保持不变，没有存储格式或数据库 schema 变更。
- 现有个人安装继续使用 `validation` 渠道、`com.github.u5151561.trackr.debug` 和既有调试证书。候选包关闭 debuggable 与覆盖率插桩，保留 R8 / 资源优化及 shell profileable；不因“稳定候选”改称正式发行签名。
- 当前选择保持安装身份连续，后续同渠道更新递增 versionCode；本轮不改包名、不切换证书、不生成或提交新密钥。独立正式发行的应用标识、生产证书和数据迁移属于另一个发布阶段，当前未实施。常规 `release` 双 ABI 构建成功，但产物未签名，仅供构建核查，不能拿来安装或替换当前候选包。
- 正式 / 测试远程更新源仍未配置；关于页检查更新实测正确显示“更新源未配置”。无 remote，不推送、不预发布。

依据：[Android 版本管理](https://developer.android.com/studio/publish/versioning)、[应用签名与升级](https://developer.android.com/studio/publish/app-signing)。

## 旧手机覆盖升级与数据验证

- 设备为 22041216C / xaga。连接后先核对原安装 APK 的 SHA-256，确认为已保留的 v15 arm64 包。没有在较新手机进行本轮测量。
- 使用与目标应用同证书的临时 instrumentation 工具，仅在应用数据目录读取数据库、设置和普通偏好文件；抑制目标 Application 初始化以获取静止快照，不改写目标数据。排除退役 Key 与追踪会话文件。快照仅保存在本机忽略目录，未提交、上传或发布。
- 升级前备份并核验 5 个文件：Room 主库 / WAL / SHM、设置 DataStore 和提醒反馈偏好。数据库 `PRAGMA integrity_check` 为 `ok`，schema user_version 为 4。
- 实际执行同签名 `adb install -r`，从 code 21 升至 22 成功。首次安装时间、用户数据目录 inode 与已授予权限保持一致；RTBus 未卸载，未清数据。
- **安装后首次启动前，5 个文件均逐文件 SHA-256 一致；打开应用完成检查后，5 个文件仍一致。** 对两个时间点的数据库逐表逐行排序比较与设置文件比较也均无变化，完整性检查仍为 `ok`。
- 本机实际数据含 2 个收藏分组、3 条搜索历史及现有设置，均已保留。收藏条目、线路收藏与定时提醒表当时为空，不将空表一致扩充为已有提醒或全部非空业务数据迁移验收。
- 另尝试不带降级参数覆盖旧 code 21 包，系统以 `INSTALL_FAILED_VERSION_DOWNGRADE` 拒绝；最终仍为 code 22。未使用强制降级选项。

备份、逐文件校验与数据库对比证据在 `build/rtbus-validation/stable-v16/`：`before-data.zip`、`after-install-data.zip`、`after-smoke-data.zip`、对应 manifest 和 `before-vs-*.json`。原始数据与 UI 快照不进入 Git。

## 升级后定向检查

`upgrade-smoke.json` 记录 10 项成功检查，涵盖覆盖安装、安装身份 / 权限保留、文件与数据库保留、首页冷启动和收藏分组、关于页 `v0.1.14`、未配置更新源提示、提醒空弹层 Back 关闭、横滑返回首页、3 条历史记录和搜索返回。

驱动最初误将搜索页首次系统 Back 当作离开页面；实机行为是先收键盘。读取当时 UI / 输入法状态后，改用工具栏返回完成检查，无产品代码修改。此项不记为应用缺陷，也不隐去初次驱动断言失败。

用户已确认的真实乘车及其他日常功能继续沿用用户反馈，不重复要求乘车或将本轮检查扩充为全部功能、锁屏 / 厂商组合及性能验收。

## 主机与产物核验

- 完整离线 `test :androidApp:assembleValidation :androidApp:assembleRelease --continue` 成功，4m5s，874 tasks（87 executed、787 up-to-date）。聚合 18 模块 1,684 项，失败 / 错误 / 跳过均为 0；不将缓存命中说成所有测试重新执行。
- 26 项 Python 静态检查全部通过；两份候选 APK 与原 v15 证书一致，非 debuggable，Baseline Profile、R8 mapping、退役 SDK 边界和 ELF / ZIP 16 KB 对齐核验通过。
- 构建仍有既有 Gradle 弃用提示，不宣称零警告；本轮没有依赖升级或工具链迁移。

目录：`build/rtbus-validation/`，均为本地忽略产物。

| APK | 字节 | SHA-256 |
| --- | ---: | --- |
| `RTBus-20261127-stable-v16-arm64.apk` | 4,911,166 | `317f4bac6f4e33dfc0a78bf848a0cae9f4acda09ac8aee2ae5ff84978b11f2e1` |
| `RTBus-20261127-stable-v16-arm32.apk` | 4,909,242 | `bdc5e8fd089153056a91aea21e77bbcb6a1604376dc92eef6945ab962e0a4ce2` |

最终从手机拉回已安装 APK，再次核对 SHA-256 与 arm64 候选包一致。构建日志、静态结果、签名 / 对齐检查及 `artifact-audit.json` 仅保留本地。

## 清理与后续边界

- 手机最终回到公交首页。只卸载本轮创建的 `com.rtbus.probe` 与 `com.rtbus.upgradeprobe` 两个校验工具，确认移除后 RTBus 仍在前台；没有创建需要清理的手机端快照文件。
- 本轮 ADB server 已关闭，核验剩余 ADB 进程为 0。没有改全局系统设置、动画、刷新率、包名或证书，也没有配置 / 推送远程仓库。
- 卡顿排查仍暂停，旧机长帧未宣称解决；iOS、生产签名发布、真实远程下载 / 应用内安装授权流程与全部异常组合仍未在本轮验收。ADB 覆盖成功不代替这些流程。
- 若以后发现回归，优先以已验证旧实现构建更高版本号、同包名同证书的修复包；不通过卸载、清数据或强制降级处理。当前本地候选版工作已完成，后续按新需求继续。
