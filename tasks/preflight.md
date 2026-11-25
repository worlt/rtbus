# RTBus 开工前检查记录

日期：2026-11-25。范围：备份/源码完整性、开发工具与原版构建尝试；未修改应用源码，未请求公交数据接口。

## 已核实

- 工作目录：C:\D\A_code\tracker\Trackr-main。
- 原始下载包：C:\D\A_code\tracker\Trackr-main.zip，仍保留。
- ZIP SHA-256：3CA1088CDDBDC5C4F4960FDA430F85FEA183F9FF685D7E2F779445BC67E490BD。
- 逐项读取 ZIP 内 700 个文件，并与当前目录对应文件计算 SHA-256：内容不一致 0、缺失 0。新增的 tasks 文档不属于原 ZIP。
- 初次检查时项目没有 .git；现已按用户要求初始化本地 Git，并将 ZIP 验证过的 700 个原版文件单独提交为源码快照。该快照不是编译/测试通过证明。原始 ZIP 继续保留。
- Gradle wrapper 9.8.2 已通过项目指定地址下载并实际运行 `--version` 成功。没有 gradlew.bat 不阻止本次检查：使用 JAVA_HOME 下 java.exe 调用项目自带 GradleWrapperMain。
- 当前 JAVA_HOME：C:\E\dev-tools\jdk-17，实测 Temurin 17.0.20.1。
- 当前 Android SDK：C:\E\dev-tools\android-sdk，已装平台 android-34、android-35；Build Tools 30.0.3、34.0.0、35.0.0。

## 正式裁剪前需要补齐

1. JDK 21。build-logic/convention/build.gradle.kts 与 TrackrBuild.kt 明确要求 Java toolchain 21；Java 17 字节码目标不代表只装 JDK 17 即可构建。
2. Android SDK Platform 37。TrackrBuild.kt 声明 compileSdk=37，现有平台只到 35；同时按实际 AGP 需求核实 Build Tools。不得用降低 compileSdk、升级/降级整套依赖代替补齐环境。
3. 原工程成功构建与测试基线。先完成工具链、依赖解析和 Android debug 编译，再跑保留功能及相关模块测试，记录原有失败。
4. 本地 Git 源码基线已建立；后续各项改动分步验证并提交。按用户要求不创建远程仓库、不配置 remote、不推送，第一版可用后再经用户明确确认。

## 本次构建尝试的真实状态

执行了 :androidApp:assembleDebug（无业务接口调用；首次配置会访问构建依赖仓库）。首次依赖解析等待期间，已确认 JDK/SDK 缺项，因此主动停止本次检查启动的专用 Gradle daemon（PID 32928），等待补齐环境后重跑。

因此日志中的 `Gradle build daemon disappeared unexpectedly` 是本次主动停止产生的结果，**不是已经定位到的原工程崩溃，也不能据此判断源码不能编译**。没有 APK 产物，没有完成任何项目测试。

本地构建日志：环境 TEMP 目录下 rtbus-baseline-build-20261125.log。不要把未脱敏的 daemon 环境/诊断日志作为发行物提交。

## 后续可选/发布阶段信息（不是当前开工前置条件）

- 用户目前没有远程仓库，先做本地 Git 与分步提交；第一版完整可用并确认后再准备远程及正式更新源。更新机制用本地测试夹具验证，不能回退原作者更新源。最终 applicationId 可以在本地开发阶段另行确定，无需以创建远程仓库为前提。
- 自己的发布签名通过本机/CI 安全配置使用；不要在聊天或仓库中提供私钥、keystore 密码、Token。
- 城市与线路仅是可选的真机验收样本，不限制功能范围，不要求用户先提供。全国城市切换及原有查询范围保留；测试覆盖多城市及来源/方向隔离。

保留功能与删除边界以修订后的 plan.md / todo.md 为准。本次不删除乘车码、小组件、更新、追踪提醒或其必要权限。
