# RTBus 更新发行流程

## 仓库和身份

- 源码：`worlt/rtbus`，私有。
- 安装包与清单：`worlt/rtbus-releases`，公开。
- 默认清单：`https://raw.githubusercontent.com/worlt/rtbus-releases/main/update.json`。
- 测试清单：同目录 `update-beta.json`，仅用户启用“接收测试版”时一起查询。
- 每次公开发行的 versionCode 必须高于此前所有已发出的默认 / 测试版。仅推送源码不会触发应用更新。
- 当前连续升级渠道为 release 优化的 validation 包，保留 `.debug` 安装身份及原调试证书。独立生产身份/证书另行规划，不能直接替换证书后声称仍可覆盖旧版。

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

官方参考：[GitHub Release 管理](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)、[Release 资产 API](https://docs.github.com/en/rest/releases/assets)、[ADB 安装参数](https://developer.android.com/tools/adb)。
