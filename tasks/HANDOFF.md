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
- 高德尚未彻底拆除：规划入口/应用 DI 和 Android 启动绑定已移除，地图/SDK/Key 与规划源码仍有残留；普通查询/追踪逻辑保留。
- applicationId、源码包名/类名、Bundle ID、URL scheme、数据库/偏好文件名、签名配置和图标仍沿用工程现状；应用 ID 尚未独立，不能宣称能与原版并存。
- 原版更新端点仍在代码中，尚未切断或替换。用户没有自己的远程仓库，这是正常的本地开发阶段，不阻止其他改造。
- 裁剪前品牌版本的 Android debug 构建与 413 项定向 JVM 测试成功；并未单独构建原始快照。当前裁剪版本已执行完整 Android/JVM 主机测试（1,622 项，4 项失败，见第 6 节），不能称为全绿；release APK、真机和 iOS 验收仍未完成。

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

## 6. 本轮增量（2026-11-25）

- 已围绕 `feature/route` 与根 Compose 装配完成首个边界切分：Android/iOS 的 `routeEntries` 不再注册 `ScreenKey.RoutePlan`，普通 `ScreenKey.RouteDetail` 入口和共享 `RouteDetailViewModel` 保留。
- 根 `TrackrRoot` 不再预取 `RoutePlanViewModel`，不再根据高德 Key 显示路线规划 tab，也不再显示高德隐私同意弹窗；平台地图壳以 disabled 方式保留，待后续删 SDK 时清理。
- `routeDataModule` 不再提供 `RoutePlanRepository`，`routeModule` 不再提供 `RoutePlanViewModel`；路线详情、追踪、收藏继续使用 `RouteAlignmentService` / `RouteGroupSaver` 等共享能力。
- 已删除 `routeAndroidModule`、`AndroidAmapRouteGateway`、`AmapSdkTransitService`、Android Key/初始化适配器及仅供它们使用的 `AmapSdkKeyApplier` / `MapSdkInitializer` 平台接口；普通路线详情和共享对齐/追踪逻辑未删。
- 已删除 `AmapSdkKeySync` 及对应专属测试；`TrackrApp` 不再取高德初始化器/Key 同步，`MainActivity` 不再传高德隐私初始化回调。原有存储、路线/提醒仓库及小组件同步启动顺序保留。
- `KoinGraphVerifyTest` 已同步删除旧模块校验，并覆盖真实 `TrackrApp` 冷启动不依赖高德绑定、保留本地服务及路线详情定义、规划仓库/VM 不装配。
- README 明确旧高德说明仅供清理参考，测试清单不再列已删除的 SDK 适配/Key 同步测试。
- 修复全量 UI 测试实际暴露的冷启动崩溃：仅隐藏规划页签仍会让 decorator 解析隐藏栈并抛 `Unknown screen RoutePlan`。`TrackrNavApp` 现在只为公交/我的创建栈，旧 `RoutePlan` Key 继续可解码；旧当前页签回首页，其余栈按 Key 恢复。
- 新增启动器冷启动仅保留公交/我的入口、旧 3 页签状态恢复为 2 页签时路线详情/我的栈不丢的回归测试。不要恢复规划 entry 来绕过此问题。
- 删除 Android 绑定后、隐藏栈修复前的定向命令通过（不代表完整启动 UI 已通过）：`:androidApp:testDebugUnitTest --tests com.trackr.di.KoinGraphVerifyTest`、`:core:domain:testAndroidHostTest`、`:core:data:testAndroidHostTest`、`:core:navigation:testAndroidHostTest`、`:composeApp:testAndroidHostTest`、`:feature:route:testAndroidHostTest`、`:androidApp:assembleDebug`；BUILD SUCCESSFUL，445 个任务，1m 14s，16 KB 对齐检查通过。
- 隐藏栈修复后执行 `test :androidApp:assembleDebug --continue --init-script build/rtbus-validation/diagnostic.init.gradle --no-daemon --console=plain --stacktrace`：完整主机测试 **1,622 项，1,618 通过，4 失败，0 error / skipped**；组合命令退出 1，636 个任务，4m 46s。不能把其中 APK 任务完成写成整个命令 BUILD SUCCESSFUL。
- 本轮相关回归通过：MainActivityDeepLinkTest 7、TrackrAppNavigationTest 14、KoinGraphVerifyTest 15；`:core:navigation` 71（含旧规划页签恢复新增用例）、`:core:domain` 215、`:core:data` 218、`:composeApp` 20、`:feature:route` 71 均失败 0。`:androidApp:assembleDebug` 与 16 KB 检查任务完成；APK 仍含高德 native 库。
- 全量剩余失败（涉及源码/测试本轮均未修改；改造前未跑过此全量，尚不能证明是既有问题或排除关联）：
  1. `AndroidBoardingTraceLogTest.share_joinsOldAndCurrent_andOpensChooser`：`log.share()` 返回 false，需查分享/平台路径底层异常。
  2. `GithubUpdateCheckerTest.checkForUpdates_whenCancelled_cancelsAllSourceRequests`：期望取消计数 2，实际 0，需查请求/引擎取消生命周期。
  3. `FavoritesShortcutWidgetSyncTest.start_doesNotRefreshForCurrentValue`：`UncaughtExceptionsBeforeTest`，需查前序异步异常污染，不直接认定小组件业务回归。
  4. `TrackrDataStoresTest.settings_corruptFile_isReplacedWithEmpty`：`CorruptionException: Unable to parse preferences proto.`，需查损坏恢复路径。
- 续接后单项隔离与完整复验：BoardingTraceLog 单测现 4/4 通过；为避开 Robolectric 对真实 cache-path 配置根的解析异常，测试注入 content URI，仅覆盖导出内容与 chooser Intent，不等价于设备上的 FileProvider 验收。Github 更新取消测试单跑通过；小组件目标测试单跑通过，但全套中仍复现 `UncaughtExceptionsBeforeTest`。DataStore 损坏恢复目标测试单跑仍失败。随后完整运行 `test :androidApp:assembleDebug --continue --init-script build/rtbus-validation/diagnostic.init.gradle --no-daemon --console=plain --stacktrace`：1,622 项，1,620 通过、2 失败（DataStore 损坏恢复、小组件异步异常污染），0 errors/skipped；组合命令退出 1 / BUILD FAILED。debug APK/对齐任务完成不改变整条命令失败结论。日志：`build/rtbus-validation/android-host-suite-after-isolated-reruns.log`（本机忽略文件）。
- 本机日志（不入 Git）：`build/rtbus-validation/android-amap-bindings.log`（首轮定向）、`deep-link-diagnostic.log`（修复前单例复现）、`android-host-suite-after-fix.log`（修复后完整结果）、`suite-summary.json`（按 XML 汇总）。同目录诊断 init 脚本只将测试异常完整打印，不改变测试逻辑；无该脚本时可省略 `--init-script` 参数复现。环境与 wrapper 入口沿用第 4 节 / `baseline.md`。
- 品牌静态检查 6 项及 `git diff --check` 通过；不据此代替以上测试结果。
- 未完成：RoutePlanViewModel、RoutePlanRepository、混合职责的 RouteAlignmentService、AmapRouteGateway、Android/iOS 地图会话、SDK/Key/隐私存储和构建配置仍在工程中；APK 对齐日志仍列出 `libAMapSDK_MAP_v11_3_100.so` / `libapssdk.so`。本轮不是高德彻底移除，也未做 release APK/真机/iOS 验收。

## 7. 唯一明确的下一步

针对原列四项失败逐项隔离：`AndroidBoardingTraceLogTest` 原失败由 Robolectric `FileProvider.getUriForFile` 抛 `IllegalArgumentException: Failed to find configured root that contains .../cache/diagnostics/trackr_boarding_trace.txt` 导致；已给 URI 创建加可注入边界，单测以受控 content URI 验证导出内容和分享 Intent，4 项现通过。主 Manifest 与合并测试 Manifest 均含 `${applicationId}.fileprovider` 和 `@xml/update_paths`，但 Robolectric 对缓存根解析不匹配；本次不是 Android 真机 FileProvider 验收，后续真机仍需验证。`GithubUpdateCheckerTest.checkForUpdates_whenCancelled_cancelsAllSourceRequests` 单独运行通过；`FavoritesShortcutWidgetSyncTest.start_doesNotRefreshForCurrentValue` 单独运行通过，但全套仍失败于 `UncaughtExceptionsBeforeTest`，根因尚未定位。`TrackrDataStoresTest.settings_corruptFile_isReplacedWithEmpty` 单独运行仍失败。最新完整主机套件复验为 1,622 项中 1,620 通过、2 失败，未达到全绿。不得跳过测试、削弱断言/更新安全校验或移除保留功能消除失败。

续查发现 DataStore handler 实际已接管解析异常并尝试写入空 Preferences；目标测试仍失败的直接原因是 Windows 主机上替换损坏文件时，Okio/DataStore 将 `.tmp` rename 到仍存在的 `.preferences_pb` 失败（XML 中为 suppressed `IOException: Unable to rename ...`; 堆栈仍把原始 CorruptionException 作为主异常）。因此原先“handler 未接管”的判断不准确；需找到不削弱断言、同时能验证 Android 真实替换语义的方案，并区分 Windows host 文件系统限制与生产缺陷。不要仅捕获异常或跳过该测试。小组件完整套件异常尚未定位；现有记录只显示 `UncaughtExceptionsBeforeTest`，暂不能归因于 DataStore 或某个测试。
进一步验证：尝试临时将 DataStore 从 1.2.1 升到 1.3.0，目标测试仍因 Android host 上 `File.renameTo` 覆盖现有文件失败（`FileMoves_android.kt`），已恢复版本至 1.2.1，未保留无效依赖升级。随后单跑 `:androidApp:testDebugUnitTest`（313 tests）复现两项失败：更新取消测试断言 `cancelled == 2` 实得 0；小组件仍报 `UncaughtExceptionsBeforeTest`，日志 `build/rtbus-validation/android-app-tests-diagnostic-nov25.log`。将这两个测试类放在同一筛选任务中运行则通过，日志 `build/rtbus-validation/github-widget-pair-nov25.log`，因此全套/负载或测试顺序敏感，但暂时不能断言它们存在因果关系。

下一步先解决 DataStore 损坏恢复测试的 Windows 文件替换失败，并定位更新取消断言在单测套件负载下的间歇失败及小组件 `UncaughtExceptionsBeforeTest` 的前序异步异常；两项测试同任务筛选通过，暂不认定相互导致（不要直接改小组件断言）。修复后重跑完整主机套件并确认 1,622 项全绿，再按原定裁剪从 `TrackrRoot` 闲置高德参数和 `PlatformShell.android.kt` 的 disabled `ProvideAmapMapSession` 包装开始，连同 iOS 对应桥接和 `PlatformHostTest` 收敛；保留内容组合身份、普通路线详情、共享对齐与追踪。

## 8. 记录维护

每个阶段完成或准备切换任务时：更新本文件的已完成/未完成/验证结果/下一步，以及 todo.md；只记录有效状态，不复制全量对话和大型日志，不写敏感信息。与代码一起作合理本地提交，并在回复中给出提交号和未解决事项。

更多资料：plan.md = 完整方案与许可核查；todo.md = 实施清单；preflight.md = 初始环境与构建尝试；branding.md = 更名范围和验证。
