# RTBus 更新发行流程

## 仓库和身份

- 源码：`worlt/rtbus`，私有。
- 安装包与清单：`worlt/rtbus-releases`，公开。
- 默认清单：`https://raw.githubusercontent.com/worlt/rtbus-releases/main/update.json`。
- 测试清单：同目录 `update-beta.json`，仅用户启用“接收测试版”时一起查询。
- 每次公开发行的 versionCode 必须高于此前所有已发出的默认 / 测试版。仅推送源码不会触发应用更新。
- 当前连续升级渠道为 release 优化的 validation 包，保留 `.debug` 安装身份及原调试证书。独立生产身份/证书另行规划，不能直接替换证书后声称仍可覆盖旧版。

## 本地发版工具

`tools/release.py` 使用 Python 3.11+ 标准库，当前备份 / 权限步骤针对 Windows。它提供三个命令：

- `backup`：核对固定证书、在 Git 目录外备份现有 keystore、校验文件哈希，并用备份实际签名临时 APK；随后删除临时签名验证文件。不会替换原 keystore。
- `prepare`：核对备份、Gradle 实际签名、远端默认 / beta 版本，再执行全部静态检查、完整主机测试、validation 双 ABI 构建、依赖 / SDK / 包名 / 版本 / 签名 / Profile / 16 KB 对齐检查，最后输出四个公开资产及更新清单。
- `verify`：复核候选目录中的文件集合、资产哈希、SHA256SUMS 与清单内容、版本及演练标识。它是已审计本地产物的一致性复核，不替代 `prepare` 的 SDK / 签名构建审计，也不是对不受信任 report.json 的数字签名认证。

默认 keystore 为当前用户 `.android/debug.keystore`；默认备份目录为当前用户 `.rtbus/signing-backups/validation-51efae50fd08caf4`，**不在 Git 仓库内**。备份目录和文件仅允许当前 Windows 用户与 SYSTEM，原密钥文件保持不变。脚本内只固定公开证书指纹，不包含私钥；密码通过子进程环境传递，不出现在命令参数或输出中。当前调试通道的默认密码并不构成生产密钥保护，备份也不改变现有调试证书的性质。

### 首次备份或复核备份

以下为当前电脑的工具路径，换电脑时按实际位置调整；参考 APK 必须是已校验的同签名 RTBus 安装包：

```powershell
$env:JAVA_HOME = 'C:/E/dev-tools/jdk-21'
$env:ANDROID_HOME = 'C:/E/dev-tools/android-sdk'
$env:GRADLE_USER_HOME = 'C:/E/dev-tools/gradle-home'
python -X utf8 tools/release.py backup --reference-apk build/rtbus-validation/update-v17/RTBus-0.1.16-arm64-v8a.apk
```

重复执行会复核同一份备份并重做临时签名验证，不覆盖不同内容的已有 keystore。备份拒绝 Git 工作区、磁盘根目录、用户主目录以及含无关文件的目录。当前电脑已完成本机备份与签名验证；**尚未验证离线 / 异地备份**，本机故障或整盘丢失仍需独立备份应对。备份目录和其中两个文件应一并复制到你控制的安全离线存储；不上传 GitHub、聊天附件或公共网盘。

新电脑恢复时先核对 backup.json 的 keystore 哈希及证书指纹。若默认 `.android/debug.keystore` 已存在，先辨明它是否用于其他工程，不直接覆盖不同密钥。恢复到新环境后重新执行 `backup` 验证，并由 `prepare` 的 signingReport 检查确认实际使用的是同一密钥；不要通过生成新证书、卸载 RTBus 或清数据来解决签名不一致。

### 准备下一个版本

先提高 `androidApp/build.gradle.kts` 的版本并提交源码，单独写明可公开的更新说明。默认模式要求 versionCode **高于当前远端默认和 beta 清单的最大值**，并且 Git 工作区干净；不会自动改版本。若曾撤回清单，维护者仍须依据发版记录保证高于所有已发版本，不能把撤回清单当作已安装版本的降级。

```powershell
python -X utf8 tools/release.py prepare --notes-file path/to/public-notes.md --offline
```

`--offline` 仅作用于 Gradle 依赖解析；为核对已发版本，脚本仍需匿名读取 RTBus 的远端两份清单，读取失败会停止。没有跳过测试或校验的开关；任一命令失败会停止并留下对应日志。

产物位于全新的 `build/rtbus-release/<version>/<时间-随机标识>/`：

- `assets/`：仅双 ABI APK、SHA256SUMS.txt、LICENSE，后续只上传这里的四个文件。
- `metadata/`：默认版生成 update.json / version.json，beta 只生成 update-beta.json。不会写入公开下载仓库或覆盖现有线上清单。
- `logs/`：本机验证日志；`report.json`：版本、源提交、证书指纹、测试计数与资产哈希，均只留本机忽略目录。

产物不会自动上传、提交元数据、安装到手机或连接 ADB。完成下文“草稿上传 → 匿名校验 → 提升清单”的发布步骤后，手机才会发现新版。

### 当前版本演练与复核

当前已发布 0.1.16 不应被当作新版本重复发行。本地演练允许与远端最大 versionCode 相同，仍禁止更低版本；演练可运行于有未提交工具修改的工作区，报告固定 `rehearsal = true` / `ready_for_publication = false`：

```powershell
python -X utf8 tools/release.py prepare --notes-file path/to/public-notes.md --offline --rehearsal
python -X utf8 tools/release.py verify path/to/candidate-directory
```

演练候选目录只是验证流程，不据此替换已经发布的 0.1.16 APK 或清单；需要实际发改动时提高版本并走正常准备流程。

## 构建和校验

1. 修改 `androidApp/build.gradle.kts` 的版本号，运行完整主机测试、静态检查和 `:androidApp:assembleValidation`。
2. 检查双 ABI 包的包名、versionCode、证书、退役 SDK 边界、Baseline Profile 和 16 KB 对齐。保留已安装版本可回溯副本及数据备份，仅留本机忽略目录。
3. 将已验证产物命名为 `RTBus-<versionName>-arm64-v8a.apk` 和 `RTBus-<versionName>-armeabi-v7a.apk`。生成 `SHA256SUMS.txt`，随附原 MIT `LICENSE`。
4. 在公开下载仓库创建草稿 Release，上传且读回确认上述四个文件；公开前确认不含源码私密内容、签名私钥、手机数据或本机日志。发布后以匿名访问下载并对比 SHA-256。

## 生成与提升清单

明确填写公开版本说明；生成器不再回退私有 Git 提交消息，也不从 `GITHUB_REPOSITORY` 推断包下载地址。

PowerShell 示例（在源码根运行；版本仅为命令格式示意，须与实际构建一致）：

```powershell
$env:TAG_NAME = 'v0.1.16'
$env:RELEASE_BODY = '本次公开给用户看的改动说明'
$env:OUTPUT_DIR = 'C:/D/A_code/tracker/rtbus-releases'
python .github/scripts/update_metadata.py
```

生成器要求 tag 与源码 versionName 一致，读取实际 versionCode。默认版生成 `update.json` / `version.json`；带后缀的测试版只生成 `update-beta.json`。正式地址和 APK 名固定指向 `worlt/rtbus-releases`，不使用上游 Trackr。

先公开并验证 APK，再提升默认清单。没有独立 beta 时，首次可将已验证默认元数据复制为 beta 清单；以后 beta 由对应更高版本替换。公开仓库只提交 README、许可和元数据，APK 仅通过 Release 资产上传。

已退役源仓库的 `update-branch.yml` 自动同步流程：它会向源码仓库的 update 分支写入数据，不适用于私有源码与公开下载仓库分离的模式。当前采用校验后的显式发布，不在工作流或安装包中放维护者令牌。

## 手机验收

- 旧 v16（0.1.14 / 22）没有更新源，需手动同签名覆盖一次带源版本。
- 用较低的带源版本检查新版本，核对版本号、RTBus 说明及下载地址；实际点击下载，确认未知来源安装授权、系统安装器、最终新版本和“已是最新”。
- 对比升级前后数据库逐表内容及设置。SQLite WAL 文件重排可改变文件哈希，须区分物理文件变化与逻辑数据变化；不能把全表为空的提醒样本算作非空迁移验收。
- 不卸载 RTBus、不清数据，不用 ADB 的直接安装成功代替应用内下载安装成功。
- 验证结束移除临时校验辅助包，应用回首页并关闭 ADB。原始备份、界面节点和安装包证据仅保留本机忽略目录。

## 出现问题时

保留已发布的不可变版本资产，先停止提升更新清单。必要时撤下错误清单、恢复先前可用清单以停止继续升级；这不会让已安装的新版本自动降级。已安装版本需用修复后的更高 versionCode 同签名覆盖，不能通过卸载或清数据回退。

官方参考：[apksigner 签名 / 校验](https://developer.android.com/tools/apksigner)、[应用签名](https://developer.android.com/studio/publish/app-signing)、[Windows icacls](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/icacls)、[GitHub Release 管理](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)、[Release 资产 API](https://docs.github.com/en/rest/releases/assets)、[ADB 安装参数](https://developer.android.com/tools/adb)。
