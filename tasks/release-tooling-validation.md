# 签名备份与本地发版工具验收（2026-11-27）

## 范围与结果

用户确认先核对现有签名备份并整理发版脚本。本轮沿用已发布 0.1.16 / code 24 的包名和证书，不改业务代码或版本，不连接手机，不发布或替换线上 APK / 清单。

- `tools/release.py` 提供 backup / prepare / verify，Python 3.11+ 标准库，不新增第三方依赖。
- `tools/protect-signing-directory.ps1` 负责 Windows 受限目录和逐文件 ACL 核验。
- [使用说明](../docs/updates.md#本地发版工具) 包括备份、环境恢复注意事项、正常发版、当前版本演练与失败处理。

## 签名备份

对照实际 0.1.16 APK、JDK keytool 导出的公开证书以及 Gradle signingReport，确认 validation 使用当前用户 `.android/debug.keystore`。公开固定指纹为：

`51efae50fd08caf43eca935ebb2110605008ba604d008775acebda42b3ffe9c6`

在当前用户 `.rtbus/signing-backups/validation-51efae50fd08caf4` 建立本机专用备份，只含 `validation.keystore` 和 `backup.json`。复制前后哈希一致，原 keystore 未变。目录阻断继承，仅授权当前 Windows 用户与 SYSTEM；两份文件从受限目录继承，逐文件 ACL 也已读回核对。私钥、备份和密码均不进入 Git。

使用备份密钥实际重签临时 APK，apksigner 验证通过且仍为固定指纹；验证临时目录已清理。重复执行 backup 验证了已存在同内容备份的复核路径，不覆盖不同内容的 keystore。

验证中遇到 PowerShell 5/7 模块路径混用，以及 Set-Acl 触发当前进程不具备的权限要求。已改为固定当前 PowerShell 的原生模块目录，并用 icacls 修改 DACL 后读回检查；没有放宽 ACL 检查、提升权限或修改原密钥。

**这是本机备份，并未验证离线 / 异地副本。** 它仍是当前调试通道密钥，不是新生产证书；当前电脑或整盘损坏的恢复仍需要用户控制的独立安全备份。没有自行上传云端或放到公共下载仓库。

## 自动准备与失败拦截

正常 prepare 必须先有可核对的备份、现有密钥与固定指纹相同、Gradle 实际 validation store 与该密钥一致、源码已提交，以及 code 高于当前远端默认 / beta 清单。缺失 / 错签名、重复 / 错 ABI、错版本 / 包名、路径越界、缺文件 / 文件篡改、命令失败和矛盾演练标识均会停止。目录拒绝 Git 工作区、磁盘根目录、用户主目录和混有无关文件的位置。

准备流程依次运行全部 Python 静态检查、完整 Gradle test / assembleValidation、运行时依赖检查及 APK 审计；检查 SDK 边界、实际包名 / 版本 / ABI、签名、R8 mapping、不可调试 / 可 profile 标志、必需权限、Baseline Profile、ZIP / arm64 ELF 16 KB 对齐。生成双 ABI APK / SHA256SUMS.txt / 原 LICENSE 四个资产及只指向 RTBus 的元数据，复用既有生成器并保留当前通道的最低版本 / 强制更新字段。

不会自动改版本、生成新 keystore、上传 GitHub、改线上清单或调用 ADB。远端版本通过匿名 GET 获取；`--offline` 仅控制 Gradle，不能绕过远端版本核查。当前清单撤回不代表历史高版本消失，维护者仍须保证高于所有已发版本。

## 实际验证

- 工具首轮缺失失败、新增目录 / 演练边界 2 项红测试均留有本机记录；最终 **15 项工具测试**通过；包含既有检查在内 **46 项静态检查**通过。
- 运行完整 `test :androidApp:assembleValidation` 入口成功；JUnit XML 汇总 **1,687 项，0 失败 / 错误 / 跳过**。本轮未改应用业务代码，Gradle 多数任务复用已有结果，没有强制重跑无变化的业务测试。保留既有 Gradle / Windows 无法运行 iOS 的提示。
- 最终演练候选位于 `build/rtbus-release/0.1.16/20261127-183525-338d4962/`，双 ABI 所有审计通过；report 标记 `rehearsal = true`、`ready_for_publication = false`。verify 独立复核成功。
- 实际执行不带 rehearsal 的同版本 prepare，被拒绝：code 24 未高于当前已发 24；没有新建候选目录、构建或发布。
- 演练包与线上同版本 APK 不是逐字节相同：ZIP 对比确认只有 `META-INF/version-control-info.textproto` 的 Git revision 内容不同，其他条目内容一致。演练包不上传，不覆盖已有资产；本轮不宣称跨源码提交的可复现构建。
- 公开下载仓库本地仍为 ff8ba5b 且干净；线上清单仍为 0.1.16 / 24。ADB 保持关闭，没有新增手机操作。

原始日志、候选资产和拒绝记录仅留本机忽略目录 `build/rtbus-validation/release-tooling/` 及 `build/rtbus-release/`。后续有实际改动时提交源码、提高版本并按文档准备新版；备份目录应另存用户控制的安全离线介质，当前没有伪称此项完成。
