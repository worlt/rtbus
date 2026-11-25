# RTBus 续接交接

更新日期：2026-11-25。用途：新任务恢复项目状态；不是完整对话副本。以实际 Git、代码和验证结果为准，发现记录过时先核对再更新。

## 1. 当前项目与不可扩大范围

- 实际工作目录 / Git 根：C:\D\A_code\tracker\Trackr-main；父目录 C:\D\A_code\tracker 不是 Git 根。不要因为目录仍叫 Trackr-main 就重新下载或重建项目。
- 中文名“实时公交”，英文名“Real-Time Bus”，简称 RTBus。原项目是 5151561/Trackr，保留原 MIT LICENSE 和 busonroad contributors 版权。
- 用户只要求移除高德地图、起终点提示/公交算路、Key/初始化/SDK/相关专属配置。
- 保留原有全国城市选择、城市切换、线路/站点搜索范围；不得做单城市版或添加城市/线路白名单。少量城市/线路只是可选验收样本，不是支持范围，也不是用户必须先提供的开工条件。
- 保留乘车码、小组件、收藏/分组、追踪、一起等、到站/定时提醒、语音、通知/厂商适配、快捷方式、长辈版、主题和应用内更新等现有好用功能；不能以“精简”为由删除。
- feature/route 同时含高德规划和普通路线详情/追踪。RouteAlignmentService 也有混合职责；先解耦再删高德，不能直接删除整个模块。RouteProjectionMath 等仍被公交解析/追踪使用的工具保留。
- 不顺带换技术栈、重写数据库、合并所有模块、移除 KMP/iOS，或全局替换源码包名。

## 2. Git 和发布约定

- 用户明确要求：只在本地 Git 小步提交，第一版完整可用且用户确认后再处理远程。当前无 remote；不创建远程仓库、不配置 remote、不推送，除非之后明确授权。
- 每个合理增量验证后独立提交。先查 git status，不能覆盖、回退或混入来源不明的用户改动。不要用“写了代码”代替测试通过。
- 原始 ZIP：C:\D\A_code\tracker\Trackr-main.zip。SHA-256：3CA1088CDDBDC5C4F4960FDA430F85FEA183F9FF685D7E2F779445BC67E490BD。
- 已有提交：3c50980 = 700 个原始文件的快照；f0a2569 = 方案及本地流程；b8c7633 = 品牌更名。之后可能有交接文档提交，实际顺序以 git log 为准。
- 3c50980 是可回溯的源码快照，不是已经编译/测试通过的基线。

## 3. 已完成和尚未完成

已完成：
- 初次核查原 ZIP 中 700 个文件与源码内容一致、无缺失；许可证与范围核查见 plan.md。
- Android 中文显示名“实时公交”、英文“Real-Time Bus”；中文调试版“实时公交.D”、英文“RTBus.D”。共享关于页、中文提示/通知、iOS 默认显示名和小组件文案已同步。Gradle rootProject.name 为 RTBus。
- 关于页保留原版权，并将原仓库链接明确标为上游项目；README 标明衍生和本地开发状态，没有把上游 release 当作 RTBus 发行版。
- 离线检查 `python .github/scripts/test_branding.py` 的 6 项检查已重跑通过；`git diff --check` 通过。
- 已补齐 JDK 21 / SDK 37 工具链并实际完成 Android debug 构建：`BUILD SUCCESSFUL`、335 个任务。随附 16 KB native 对齐检查通过。已生成 arm64-v8a / armeabi-v7a 两个裁剪前基线 APK（不是发行版）；命令、哈希及限制见 `baseline.md`。
- 已有 Fake/桩数据定向 JVM 测试：`:core:domain` 215、`:core:data` 41、`:core:navigation` 70、`:feature:bus` 53、`:feature:search` 30、`:feature:route` 4，合计 **413 项通过，0 失败**；未连接真实公交接口。

尚未完成，不能误报：
- 尚未拆除高德，未改业务查询/追踪逻辑。
- applicationId、源码包名/类名、Bundle ID、URL scheme、数据库/偏好文件名、签名配置和图标仍沿用工程现状；应用 ID 尚未独立，不能宣称能与原版并存。
- 原版更新端点仍在代码中，尚未切断或替换。用户没有自己的远程仓库，这是正常的本地开发阶段，不阻止其他改造。
- 当前品牌后、裁剪前版本的 Android debug 构建与 413 项定向 JVM 测试成功；并未单独构建原始快照。完整测试套件、release、真机和 iOS 验收尚未完成。

## 4. 环境和构建状态（续接先重新核对）

- 用户已确认先补齐工具链、建立 Android 构建与保留功能测试基线，再解耦高德；本轮尚未改业务代码。
- shell：PowerShell。读写文件显式 UTF-8；必要时进程内设置 PYTHONIOENCODING=utf-8。
- 已安装并校验 Temurin **21.0.13.1+2-LTS**：`C:\E\dev-tools\jdk-21`。工程要求 toolchain 21，产物 Java 目标仍是 17；不要混淆。
- 只在构建进程里指定 JDK 21，未改全局 JAVA_HOME / PATH；默认环境仍可能指向 JDK 17，裸 `java` 可能不可用。
- ANDROID_HOME / ANDROID_SDK_ROOT=`C:\E\dev-tools\android-sdk`。已补齐 Platform `android-37.0` revision 2、Build Tools 37.0.0；AGP 实际选用并自动安装了 Build Tools **36.0.0**（本机已有许可）。没有降低 compileSdk 或依赖版本。
- Command-line Tools 23.0 位于 `cmdline-tools/latest`；旧版 12.0 保留在 `cmdline-tools/12.0`。SDK 列表检查已通过，安装位置不一致警告已处理。
- Gradle wrapper **9.8.2** 在 JDK 21 下 `--version` 成功。本轮 GRADLE_USER_HOME=`C:\E\dev-tools\gradle-home`。
- 工程没有 gradlew.bat；通过 JAVA_HOME 下 java.exe 调用项目的 GradleWrapperMain。可复现命令和本机日志位置见 `baseline.md`。
- JDK 21 下 Android debug 构建（335 个任务）和定向 JVM 测试（413 项）成功，见 `baseline.md`；品牌静态检查 6 项通过。
- 旧 preflight 中主动停止的 daemon/PID 仅为历史记录，不要重用旧 PID 操作新进程。Windows 不能运行 iOS 模拟器测试，不据此移除 KMP/iOS。

## 5. 明确的下一步

1. 读根 AGENTS.md，再读本交接和 todo.md；按需查 plan.md 的依赖边界，不要从零重新询问已确定的范围。
2. 在实际 Git 根执行 git status --short、git log -5 --oneline、git remote -v，保留用户现有改动。
3. 工具链与 Android 构建 / 定向测试基线已完成；细节见 `baseline.md`。
4. 下一步：围绕 `feature/route` 与根 Compose 装配，先为“拆分规划入口但保留普通路线详情和追踪”编写/确认边界测试，再做首个约 1–5 文件的路由 / ViewModel / DI 解耦增量；每步验证并本地提交。
5. 更新机制保留。后续本地阶段可显式提示正式源未配置，以 MockEngine/本地 APK 夹具验证；不能编造仓库或回退原作者端点，不为测试取消 HTTPS、包名、版本和签名校验。正式远程源等用户确认后再接入。

## 6. 记录维护

每个阶段完成或准备切换任务时：更新本文件的已完成/未完成/验证结果/下一步，以及 todo.md；只记录有效状态，不复制全量对话和大型日志，不写敏感信息。与代码一起作合理本地提交，并在回复中给出提交号和未解决事项。

更多资料：plan.md = 完整方案与许可核查；todo.md = 实施清单；preflight.md = 初始环境与构建尝试；branding.md = 更名范围和验证。
