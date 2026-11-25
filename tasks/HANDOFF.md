# RTBus 续接交接

更新日期：2026-11-25。用途：新任务恢复项目状态；不是完整对话副本。以实际 Git、代码和验证结果为准，发现记录过时先核对再更新。

## 1. 当前项目与不可扩大范围

- 实际工作目录 / Git 根：C:\D\A_code\tracker\Trackr-main；父目录 C:\D\A_code\tracker 不是 Git 根。不要因为目录仍叫 Trackr-main 就重新下载或重建项目。
- 中文名“实时公交”，英文名“Real-Time Bus”，简称 RTBus。原项目是 5151561/Trackr，保留原 MIT LICENSE 和 busonroad contributors 版权。
- 用户只要求移除高德地图、起终点提示/公交算路、Key/初始化/SDK/相关专属配置。
- 保留原有全国城市选择、城市切换、线路/站点搜索范围；不得做单城市版或添加城市/线路白名单。少量城市/线路只是可选验收样本，不是支持范围，也不是用户必须先提供的开工条件。
- 保留乘车码、小组件、收藏/分组、追踪、一起等、到站/定时提醒、语音、通知/厂商适配、快捷方式、长辈版、主题和应用内更新等现有好用功能；不能以“精简”为由删除。
- feature/route 同时含高德规划和普通路线详情/追踪。RouteAlignmentService 已拆出独立的 AmapSchemeAlignmentService；清理时继续保留普通匹配、整组对齐、返程、缓存和追踪能力，不能直接删除整个模块。RouteProjectionMath 等仍被公交解析/追踪使用的工具保留。
- 不顺带换技术栈、重写数据库、合并所有模块、移除 KMP/iOS，或全局替换源码包名。

## 2. Git 和发布约定

- 用户明确要求：只在本地 Git 小步提交，第一版完整可用且用户确认后再处理远程。当前无 remote；不创建远程仓库、不配置 remote、不推送，除非之后明确授权。
- 每个合理增量验证后独立提交。先查 git status，不能覆盖、回退或混入来源不明的用户改动。不要用“写了代码”代替测试通过。
- 原始 ZIP：C:\D\A_code\tracker\Trackr-main.zip。SHA-256：3CA1088CDDBDC5C4F4960FDA430F85FEA183F9FF685D7E2F779445BC67E490BD。
- 已有提交：3c50980 = 700 个原始文件的快照；f0a2569 = 方案及本地流程；b8c7633 = 品牌更名。后续增量以 git log 为准；本次测试恢复提交为 93a05c2（DataStore）、3300d2d（更新取消）、2961bd0（Application 终止清理）；6de71ab 为根平台壳脱离地图会话。
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
- 高德尚未彻底拆除：规划入口/应用 DI 和 Android 启动绑定已移除，地图/SDK/Key 与规划源码仍有残留；普通查询/追踪逻辑保留。
- applicationId、源码包名/类名、Bundle ID、URL scheme、数据库/偏好文件名、签名配置和图标仍沿用工程现状；应用 ID 尚未独立，不能宣称能与原版并存。
- 原版更新端点仍在代码中，尚未切断或替换。用户没有自己的远程仓库，这是正常的本地开发阶段，不阻止其他改造。
- 裁剪前品牌版本的 Android debug 构建与 413 项定向 JVM 测试成功；并未单独构建原始快照。当前裁剪版本完整 Android/JVM 主机测试已恢复全绿（1,623 项，0 失败/跳过，见第 6 节）；release APK、真机和 iOS 验收仍未完成。

## 4. 环境和构建状态（续接先重新核对）

- 工具链与 Android 构建/保留功能定向测试基线已建立；现在按边界小步移除高德，不重新搭建或降低依赖版本。
- shell：PowerShell。读写文件显式 UTF-8；必要时进程内设置 PYTHONIOENCODING=utf-8。
- 已安装并校验 Temurin **21.0.13.1+2-LTS**：`C:\E\dev-tools\jdk-21`。工程要求 toolchain 21，产物 Java 目标仍是 17；不要混淆。
- 只在构建进程里指定 JDK 21，未改全局 JAVA_HOME / PATH；默认环境仍可能指向 JDK 17，裸 `java` 可能不可用。
- ANDROID_HOME / ANDROID_SDK_ROOT=`C:\E\dev-tools\android-sdk`。已补齐 Platform `android-37.0` revision 2、Build Tools 37.0.0；AGP 实际选用并自动安装了 Build Tools **36.0.0**（本机已有许可）。没有降低 compileSdk 或依赖版本。
- Command-line Tools 23.0 位于 `cmdline-tools/latest`；旧版 12.0 保留在 `cmdline-tools/12.0`。SDK 列表检查已通过，安装位置不一致警告已处理。
- Gradle wrapper **9.8.2** 在 JDK 21 下 `--version` 成功。本轮 GRADLE_USER_HOME=`C:\E\dev-tools\gradle-home`。
- 工程没有 gradlew.bat；通过 JAVA_HOME 下 java.exe 调用项目的 GradleWrapperMain。可复现命令和本机日志位置见 `baseline.md`。
- JDK 21 下 Android debug 构建（335 个任务）和定向 JVM 测试（413 项）成功，见 `baseline.md`；品牌静态检查 6 项通过。
- 旧 preflight 中主动停止的 daemon/PID 仅为历史记录，不要重用旧 PID 操作新进程。Windows 不能运行 iOS 模拟器测试，不据此移除 KMP/iOS。

## 5. 续接流程与更新约束

1. 读根 AGENTS.md，再读本交接和 todo.md；按需查 plan.md 的依赖边界，不要从零重新询问已确定的范围。
2. 在实际 Git 根执行 git status --short、git log -5 --oneline、git remote -v，保留用户现有改动。
3. 工具链与 Android 构建 / 定向测试基线已完成；细节见 `baseline.md`。
4. 入口/DI 与 Android 高德启动绑定的边界增量已完成；唯一明确的实施下一步见第 7 节，不重复已完成的删除。
5. 更新机制保留。后续本地阶段可显式提示正式源未配置，以 MockEngine/本地 APK 夹具验证；不能编造仓库或回退原作者端点，不为测试取消 HTTPS、包名、版本和签名校验。正式远程源等用户确认后再接入。

## 6. 当前增量与实际验证（2026-11-25）

### 已有高德边界切分（不要重做）
- Android/iOS 的 `routeEntries` 不再注册 `ScreenKey.RoutePlan`，普通路线详情入口、共享 `RouteDetailViewModel`、对齐/追踪保留。
- 根 `TrackrRoot` 不再预取规划 VM、显示规划 tab 或高德隐私弹窗；根平台壳的 disabled 地图包装及闲置高德回调已在本次续接移除（见下）。
- 规划仓库/VM 已退出应用 DI；`routeAndroidModule`、Android SDK 算路网关/服务、初始化/Key 同步绑定及对应专属测试已删除。Android 原有存储、路线/提醒仓库、小组件同步启动顺序保留。
- 根导航只创建公交/我的两条栈，避免隐藏规划栈触发 `Unknown screen RoutePlan`；旧规划 Key 保留解码兼容，旧当前页签回首页，其余栈按 Key 恢复。冷启动、旧状态恢复和 Koin 真实 Application 启动回归已覆盖。
- 日志分享测试通过可注入 URI 边界隔离 Robolectric FileProvider 根解析问题；内容与分享 Intent 有断言，不等价于设备 FileProvider 验收。

### 本次续接：主机测试恢复
- 入场未提交的 DataStore 测试草稿已保留、审查并完善：通过 internal Storage 注入复用项目恢复工厂，只修正主机 atomic move 语义；生产默认存储不变。关闭后再由生产工厂重读有效文件。共享 handler 被临时改为抛异常时测试确实失败，变异已恢复。
- 更新取消测试改为等待两个 MockEngine handler 的 finally 完成，再保持原有取消数断言；生产更新逻辑与安全检查不变。
- 小组件 `UncaughtExceptionsBeforeTest` 的 suppressed 堆栈定位到前序 Room 失效通知访问 Robolectric 已重置的 SQLite 连接。TrackrApp 在模拟进程 `onTerminate` 时停止 Koin，及时关闭其存储和应用作用域；未修改小组件断言或数据库实现。新增终止回归在修复前失败、修复后通过。
- 诊断经过、边界和初始失败复现详见 `test-recovery.md`。日志在本机忽略目录 `build/rtbus-validation/`，不提交大型日志或本机配置。

### 本次续接：根平台壳脱离地图会话
- 删除 TrackrRoot 的闲置 `onAmapPrivacyAccepted` 参数；common/Android/iOS `PlatformHost` 删除 enabled 参数，Android 不再包装 ProvideAmapMapSession，只在同一组合位置调用 content。
- 保留 Android 主题驱动的系统栏、边到边显示与导航栏对比度设置；没有删除 PermissionOnboarding、标准/长辈导航或普通路线详情。
- PlatformHostTest 不再要求高德 consent 翻转后创建地图会话，改为主题切换仍保留导航状态、Navigator、普通 remember token、已打开路线详情及跨页签返回栈。ThemeModeTest 继续检查系统栏跟随主题。
- `composeApp/src` 中已无 `onAmapPrivacyAccepted`、`PlatformHost(enabled...)`、`ProvideAmapMapSession` 引用。iOS actual 签名已同步，但 Windows 没有执行 iOS 编译/链接/模拟器测试。

### 最新验证
- `test :androidApp:assembleDebug --continue --init-script build/rtbus-validation/diagnostic.init.gradle --no-daemon --console=plain --stacktrace`：**BUILD SUCCESSFUL**，3m 19s，636 个任务（55 executed / 581 up-to-date）。
- 按各模块 JUnit XML 汇总：**1,623 tests，0 failures / errors / skipped**。其中 Android app 314、DataStore 7、Compose app 20；新增 Application 终止回归使原 1,622 项增加 1 项。
- 完整 app 套件额外 `:androidApp:testDebugUnitTest --rerun` 重跑：BUILD SUCCESSFUL，1m 10s，314 项均通过；日志 `resume-app-repeat.log`。
- Android debug APK 与 `checkDebug16kbPageAlignment` 通过；APK 仍含 `libAMapSDK_MAP_v11_3_100.so` / `libapssdk.so`，不是高德彻底移除或发行版验收。
- 本轮日志：`resume-host-suite.log`；汇总：`resume-suite-summary.json`。环境与 wrapper 入口沿用第 4 节 / `baseline.md`。
- 根壳裁剪后定向复验：Compose app 20 项、Android 启动/导航/Koin 37 项均通过；debug APK/16 KB 检查通过，BUILD SUCCESSFUL（1m 5s / 386 tasks）。日志 `root-shell-detachment.log`。品牌静态检查 6 项、`git diff --check` 通过；不代替运行测试。
- 根壳裁剪后再次完整运行 `test :androidApp:assembleDebug`（其余参数同上）：**BUILD SUCCESSFUL**，1m 13s，636 tasks（20 executed / 616 up-to-date）；XML 再汇总 **1,623 项，0 失败/errors/跳过**。日志 `root-shell-final-host-suite.log`，汇总 `root-shell-final-suite-summary.json`。
### 本轮续接：拆分规划与普通路线对齐
- 新增 `AmapSchemeAlignmentService`，迁移仅供 `DefaultRoutePlanRepository` 使用的规划方案对齐；普通 `RouteAlignmentService` 保留实时线路匹配、整组对齐、返程和合成候选缓存。规划专属方案上限也随协作者隔离。
- 新增 8 项规划对齐回归和 6 项普通路线对齐/缓存/返程测试；既有规划仓库、规划 ViewModel、路线详情 ViewModel 与屏幕测试装配已改为使用各自职责的服务。
- 两个完整模块测试：`:core:data:testAndroidHostTest :feature:route:testAndroidHostTest` **BUILD SUCCESSFUL**。完整 `test :androidApp:assembleDebug` **BUILD SUCCESSFUL**（3m 39s，636 tasks；51 executed / 585 up-to-date）。JUnit XML 汇总 **1,637 项，0 failures / errors / skipped**。
- `checkDebug16kbPageAlignment` 通过，6 个 64 位共享库对齐合格；debug APK 仍包含高德 SDK 原生库，不能视作已移除高德或 release 验收。品牌检查 6 项、`git diff --check` 通过。
- 本轮日志位于忽略目录 `build/rtbus-validation/`：`alignment-split-green.log`、`alignment-split-preservation-red.log`、`alignment-split-modules.log`、`alignment-split-full-host.log`。未执行 iOS 构建、release 或真机验收。
- 尚未完成：规划界面/仓库/模型、feature/route 内部 Android/iOS 地图实现、SDK/Key/隐私存储与 iOS runtime/构建配置仍有残留；release / 真机 / iOS 验收未完成。

## 7. 唯一明确的下一步

继续删除高德专属规划与地图实现：从 feature/route 中移除规划页面、ViewModel、地图 UI / Android 与 iOS 装配，并删掉仅服务规划的仓库/接口/SDK 绑定；按引用图确认模型、共享投影/坐标工具及普通路线详情/追踪继续保留。每个小增量先跑对应模块测试，再运行完整主机套件及可用平台编译，最后本地提交。不删除整个 feature/route，不移除 KMP/iOS，不配置 remote、不推送。

## 8. 记录维护

每个阶段完成或准备切换任务时：更新本文件的已完成/未完成/验证结果/下一步，以及 todo.md；只记录有效状态，不复制全量对话和大型日志，不写敏感信息。与代码一起作合理本地提交，并在回复中给出提交号和未解决事项。

更多资料：plan.md = 完整方案与许可核查；todo.md = 实施清单；preflight.md = 初始环境与构建尝试；branding.md = 更名范围和验证。
