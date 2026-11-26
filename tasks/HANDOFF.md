# RTBus 续接交接

更新日期：2026-11-26。用途：新任务恢复项目状态；不是完整对话副本。以实际 Git、代码和验证结果为准，发现记录过时先核对再更新。

最新续接：v8 在「我已上车」选车弹窗增加「先到的在前」排序。见文末 v8 小节及第 7 节。

## 1. 当前项目与不可扩大范围

- 实际工作目录 / Git 根：C:\D\A_code\tracker\Trackr-main；父目录 C:\D\A_code\tracker 不是 Git 根。不要因为目录仍叫 Trackr-main 就重新下载或重建项目。
- 中文名“实时公交”，英文名“Real-Time Bus”，简称 RTBus。原项目是 5151561/Trackr，保留原 MIT LICENSE 和 busonroad contributors 版权。
- 用户只要求移除高德地图、起终点提示/公交算路、Key/初始化/SDK/相关专属配置。
- 保留原有全国城市选择、城市切换、线路/站点搜索范围；不得做单城市版或添加城市/线路白名单。少量城市/线路只是可选验收样本，不是支持范围，也不是用户必须先提供的开工条件。
- 保留乘车码、小组件、收藏/分组、追踪、一起等、到站/定时提醒、语音、通知/厂商适配、快捷方式、长辈版、主题和应用内更新等现有好用功能；不能以“精简”为由删除。
- feature/route 现仅保留普通路线详情/追踪。规划页面、ViewModel、两端地图/桥接及规划专属仓库/网关/模型/Web 客户端均已删除。RouteAlignmentService 的普通匹配、整组对齐、返程和缓存、RouteGroupSaver 与追踪能力保留，不能删除整个 feature/route 模块。RouteProjectionMath / CoordTransform 等仍被公交解析/追踪使用的共享工具保留。
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
- 高德专属实现和配置已退役：规划 UI/数据链、SDK/CocoaPods/链接、Key 页面/仓库/存储读写、专属隐私及 Android AMAP_WEB_KEY 已删除；最新 debug/release 双 ABI APK 均完成退役 SDK/Key 类与资源核查。保留旧导航序列化标识及历史 Key 文件备份排除。release APK 构建已完成，产物未签名；真机、真实签名升级和 iOS 实际验收仍未完成；普通查询、系统定位、路线详情和追踪保留。
- applicationId、源码包名/类名、Bundle ID、URL scheme、数据库/偏好文件名、签名配置和图标仍沿用工程现状；应用 ID 尚未独立，不能宣称能与原版并存。
- 正式/测试更新源已显式未配置，不请求原作者清单；应用内更新机制和关于页入口保留，自己的远程发行源及发布 workflow 仍待用户确认远程后接入。
- 裁剪前品牌版本的 Android debug 构建与 413 项定向 JVM 测试成功；并未单独构建原始快照。当前裁剪版本完整 Android/JVM 主机测试结果见第 6 节最新增量；debug/release 构建和产物核查已完成；release APK 未签名，签名、真机和 iOS 验收仍未完成。

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
5. 更新机制保留。正式/测试更新源默认空列表，返回 NotConfigured 并在关于页说明；不要编造仓库或回退原作者端点，不为测试取消 HTTPS、受信任下载源、包名、版本和签名校验。正式远程源等用户确认后再接入。

## 6. 当前增量与实际验证（2026-11-25 至 2026-11-26）

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

### 本轮续接：删除规划页面与两端地图实现
- 删除 feature/route 中规划 Route/Screen/VM/UiState、输入控制台、方案面板、收藏弹窗、地图几何/UI、Android 地图会话/SDK 管理器和 iOS RouteMap actual；同步删除 61 项仅服务退役能力的专属测试和 27 条专属资源。模块不再直接依赖高德 SDK 或 core:network，保留普通路线 entry、Screen/VM、实时匹配、整组对齐、返程、缓存与追踪。
- 移除 Swift NativeRouteMap、AppDelegate 地图安装、Xcode source/file 引用及 installIosMap 桥接；IosNativeBridge 的快照/实时活动安装保留。iOS runtime 不再订阅高德 SDK 隐私回调，hasIosMapSdk=false；Web 网关与 Key 仓库仍留待下一数据层/配置增量。CocoaPods 依赖尚未删除。
- 新增 4 项普通路线详情追踪 UI 回归：等车确认上车、乘车切下一程、末程结束追踪、其他路线会话隔离。删除前 9 项详情 UI 测试先通过；临时追踪转收藏的既有 VM 回归保留断言，改为直接调用原共享 RouteGroupSaver，不再通过规划 VM 装配。
- 新增 `.github/scripts/test_route_feature_boundary.py` 的 3 项静态边界检查。删除前失败（规划/SDK 源码、依赖及 iOS 桥接仍存在），删除后全通过；只验证源码边界，不代替运行测试。6 项品牌静态检查及 git diff --check 通过。
- 对应 4 个模块任务 `:core:data:testAndroidHostTest :feature:route:testAndroidHostTest :composeApp:testAndroidHostTest :androidApp:testDebugUnitTest`：BUILD SUCCESSFUL（2m 15s / 347 tasks），共 580 项，0 failures / errors / skipped；其中 data 232、route 14（9 UI + 4 VM + 1 线路详情 Key）、compose 20、app 314。
- 完整 `test :androidApp:assembleDebug --continue --init-script build/rtbus-validation/diagnostic.init.gradle --no-daemon --console=plain --stacktrace`：BUILD SUCCESSFUL（32s / 636 tasks；25 executed / 611 up-to-date）。当前 XML 汇总 **1,580 项，0 failures / errors / skipped**；由 1,637 减去 61 项退役能力测试、加上 4 项普通追踪回归得到，并非跳过或放宽测试。
- debug APK 与 checkDebug16kbPageAlignment 通过（6 个 64 位共享库）。实际检查 APK 仍含 libAMapSDK_MAP_v11_3_100.so / libapssdk.so；Android app 的 SDK 依赖与 CocoaPods / Key / 全局配置尚未裁剪，不是高德彻底移除或 release 验收。未执行 release、真机或 iOS 构建/链接/模拟器测试。
- 本机忽略目录 build/rtbus-validation/ 中的记录：route-ui-boundary-red.log、route-ui-preservation-baseline.log、route-ui-removal-modules.log、route-ui-removal-full-host.log、route-ui-removal-suite-summary.json。交接/待办、模块指南和 iOS 当前状态说明已同步；无 remote，不推送。

### 本轮续接：删除规划专属数据链与闲置 Web 绑定
- 删除 RoutePlanRepository / DefaultRoutePlanRepository、AmapRouteGateway / WebAmapRouteGateway、AmapSchemeAlignmentService 及其方案上限、AmapTransitApiClient / AmapLruCache、AmapTipItem / AmapTransitSchemeResult 和 FakeAmapRouteGateway。引用核查后仅删除退役规划链，不移除普通路线的数据层或共享模型。
- iOS runtime 和 Android AppModule 均不再注册闲置高德 Web 客户端/网关；保留共用 HttpClient、公交 Provider、其他仓库以及本轮范围外的 Key/隐私装配。Koin 真实 Application 启动回归加强为实时查询、追踪、普通对齐、路线保存均可解析，并禁止装配高德 Web 客户端。
- RouteAlignmentService 仅更新职责注释，普通匹配、整组对齐、返程、缓存实现未改；RouteGroupSaving、TrackingPoller、RouteProjectionMath / CoordTransform、共享路线/追踪模型、feature/route 和导航均与入场版本一致。保护性字节/差异核对见 route-data-preservation-check.json，不代替运行测试。
- 删除 27 项退役规划专属测试：data 13 项，network 14 项。原 AmapFormatJvmTest 中 2 项通用定点格式化/URL 编码测试逐字保留并迁至 remote/common/NetworkTextJvmTest；通用 URL 脱敏测试保留全部断言，仅将高德夹具地址换成保留域 api.example.test。没有跳过或弱化保留功能测试。
- 新增 test_route_data_boundary.py 的 3 项检查：共享数据层无规划类型/替身、网络层无规划客户端/缓存/端点、两端无 Web 规划绑定。删除前 3 项均失败，删除后通过。与原 3 项 feature 边界及 6 项品牌检查合计 12 项通过；只是静态边界，不是 iOS/设备验收。
- 对应 5 个模块任务 :core:network:testAndroidHostTest、:core:data:testAndroidHostTest、:feature:route:testAndroidHostTest、:composeApp:testAndroidHostTest、:androidApp:testDebugUnitTest：BUILD SUCCESSFUL（3m 7s / 361 tasks），678 项，0 failures / errors / skipped；其中 network 111、data 219、route 14、compose 20、app 314。
- 完整 test :androidApp:assembleDebug（其余参数沿用上一增量）：BUILD SUCCESSFUL（3m 14s / 636 tasks；52 executed / 584 up-to-date）。18 个模块任务的 JUnit XML 汇总 1,553 项，0 failures / errors / skipped；与上轮 1,580 项相差的 27 项均为上述专属测试，其他模块测试数不变。
- debug APK 与 checkDebug16kbPageAlignment 通过（6 个 64 位共享库）；实际检查两个 ABI 的 APK 仍含 libAMapSDK_MAP_v11_3_100.so / libapssdk.so。全局 SDK/CocoaPods、Key/隐私与构建配置未清理，不是彻底移除高德、release 或真机/iOS 验收。未连接真实公交接口、无 remote、不推送。
- 日志与汇总仍在忽略目录 build/rtbus-validation/：route-data-preservation-baseline.log、route-data-boundary-red.log、route-data-removal-modules.log、route-data-removal-module-summary.json、route-data-removal-full-host.log、route-data-removal-suite-summary.json、route-data-static-checks.log、route-data-preservation-check.json。git diff --check 通过；同步了 iOS 状态/路线图和测试文档中的退役能力记录。

### 本轮续接：全局原生 SDK / CocoaPods 清理
- 已移除 Android app 的一体化 SDK、版本目录别名、专属 Maven 镜像、原生 Key 注入/状态任务、Manifest API Key/APSService、SDK R8 规则及高德明文域名例外；保留普通定位/追踪/小组件/安装组件和朗斯城市索引 HTTP 例外。
- 已移除 iOS Podfile/锁文件、Pods workspace、库/配置/构建阶段引用和旧模拟器链接覆盖；保留 ComposeApp framework、共享构建阶段、App Group 与小组件。直接打开 xcodeproj，不再 pod install；Windows 仅能做工程/引用静态检查。
- 两端生产装配显式没有原生地图 SDK；Android 旧 Key 仍保留存储，但不能启用地图。修正 Key 页将“无 Android SDK”误当“有 iOS SDK”的判断，新增无原生 SDK 不显示任一原生 Key 槽测试。Key 页/仓库/存储/专属隐私整体清理仍属下一独立增量，Android AMAP_WEB_KEY 配置暂保留。
- 定向 :feature:me:testAndroidHostTest 与 :androidApp:testDebugUnitTest 已通过：381 项（me 66、app 315），0 failures/errors/skipped；新增 2 项运行测试，未移除或弱化保留测试。新增 9 项源码/产物检查器回归，与已有边界/品牌检查合计 21 项通过。入场源码边界和旧 APK 均被新检查器拒绝；完整 test :androidApp:assembleDebug :androidApp:processReleaseMainManifest 已通过（5m 17s，638 tasks；323 executed / 315 up-to-date）：18 个模块任务的 XML 合计 1,555 项，0 failures/errors/skipped，比上轮仅新增上述 2 项。
- 两个 ABI 的 debug APK 均只含 libandroidx.graphics.path.so 与 libdatastore_shared_counter.so；检查器已确认没有高德原生库、资源文件名或 DEX vendor 类描述符。debug/release 合并 Manifest 和两个 APK 的 aapt2 解码 Manifest 均无 API Key/APSService；普通定位、追踪、小组件、重启恢复和更新安装组件保留。
- checkDebug16kbPageAlignment 的 4 个 64 位共享库全部通过；两个 debug APK 的 zipalign -c -P 16 -v 4 均通过。debug/release runtime 完整依赖图均生成成功并通过无高德依赖检查；未构建 release APK，未做签名/升级、真机或 iOS 构建/链接/运行验收，不访问真实公交接口。
- 日志与汇总均在忽略目录 build/rtbus-validation/，前缀 native-sdk-：removal-modules.log / removal-module-summary.json、removal-full-host.log / removal-suite-summary.json、debug-dependencies.log / release-dependencies.log、artifact-checks.log / artifact-summary.json、static-checks.log、boundary-red.log / boundary-green.log、apk-red.log，以及各 ABI 的 manifest.log / zipalign.log。产物检查可复用 .github/scripts/check_native_sdk_artifacts.py（命令见 docs/testing.md）。JDK/Gradle 缓存沿用第 4 节；本轮 Gradle 使用 --no-daemon --console=plain --max-workers=2 -Dorg.gradle.parallel=false -Dorg.gradle.vfs.watch=false。git diff --check 通过；无 remote，不推送。
- 43 个保留核心源/测试文件与 af12ed4 字节一致；源 Manifest 除两个高德声明外完全一致。记录在 build/rtbus-validation/native-sdk-preservation-check.json（不是设备验收）。

### 本轮续接：Key / 专属隐私退役（2026-11-26）
- 删除“我的”里的 Key/地图设置组、Key 页面/ViewModel/事件/资源、仓库与 Fake、Android 新旧存储适配和 Key 迁移；Android/iOS runtime 不再装配 Key/隐私或仅供 Key 页使用的签名展示接口。Android AMAP_WEB_KEY、读取助手及未使用的旧字段常量已删除；发布签名配置保留。
- 旧 Key 文件不再读取、创建、迁移或改写，也不主动删除历史文件。普通列表迁移不再遍历旧 Key 文件，防止其损坏备份复制到普通 legacy_backup。两份 Android 备份规则逐字未改：旧 SharedPreferences / DataStore 专用 Key 文件在旧版自动备份、云备份和设备迁移三段仍排除；普通设置与数据库继续备份。iOS private 目录的排除机制保留，未做 iOS 实际验收。
- ScreenKey.AmapKey 及其序列化登记仅供旧存档兼容；标准版各栈与长辈版恢复时先剔除该页，保留同栈其他页面及其他页签。当前“我的”页无 Key 入口，长辈版拒绝再次打开；页签可见性不再依赖 Key。既有 RoutePlan 兼容和普通路线详情/追踪保留。
- 新增/替换 10 项运行保护回归：旧文件不重开/不创建、损坏 Key 文件不影响普通设置和收藏迁移、混合旧 prefs 不向普通设置导入 Key/隐私、旧 Key 损坏备份不进入普通备份目录、标准版/长辈版旧页恢复。34 项旧 Key 行为测试退役或替换，另 2 项页签用例仅随退役语义改名；完整测试数量净减 24。普通设置、收藏、导航、启动、备份与安装器测试保留，未添加 skip 或弱化安全检查；逐项变化见 key-privacy-test-changes.json。
- 6 个模块定向任务通过：674 项（datastore 7、data 205、navigation 74、me 56、compose 20、app 312），0 failures/errors/skipped。首两次编译发现新增测试的错误快照字段和一处遗漏的旧 Key 断言，已修正；最终定向日志 key-privacy-modules-final.log 为 BUILD SUCCESSFUL。
- 最终源码执行 test :androidApp:assembleDebug :androidApp:processReleaseMainManifest：BUILD SUCCESSFUL；18 个模块的 JUnit XML 共 1,531 项，0 failures/errors/skipped。key-privacy-verified-host.log 为最终依据；早一轮完整通过记录为 key-privacy-full-host.log。未执行 iOS 原生任务，不能将 JUnit 的 0 skipped 当作 iOS 通过。
- 新增 test_key_privacy_boundary.py 的 5 项保护，与已有品牌/规划/SDK 检查合计 26 项通过。新检查在入场提交 c568b92 的生产源码上有 3 项预期失败（另两项历史备份/序列化保护通过），当前源码全通过。只是源码/检查器测试，不代替平台运行。
- 两个最新 debug APK 的所有 DEX 与 Compose 资源均未发现退役 Key 页面/仓库/存储/签名展示类或 Key 文案资源；保留的旧导航标识不误判为活跃能力。两个 APK 各仅有 graphics.path 与 datastore counter 原生库，打包 Manifest 无高德组件，zipalign -c -P 16 -v 4 均通过；Gradle 的 checkDebug16kbPageAlignment 通过。重新生成 debug/release runtime 依赖图并检查两份合并 Manifest，均无高德依赖/组件。未生成 release APK。
- SignatureUtils、AndroidAppUpdateInstaller 及各自测试、两份备份规则、RouteAlignmentService 逐字未改；LegacySettingsMigration.copySettings 普通设置迁移体未改。Android 构建、安装器签名/包名/版本/下载验证随完整套件通过，但不代表真机升级验收。未连接真实公交接口，无 remote，不推送。
- 日志/汇总均在忽略目录 build/rtbus-validation/，前缀 key-privacy-：module-summary.json、suite-summary.json、test-changes.json、preservation-check.json、function-check.json、static-final.log、boundary-baseline-red.log、verified-host.log、debug-dependencies.log / release-dependencies.log、artifact-checks.log / artifact-summary.json 及各 APK Manifest/ZIP 对齐日志。git diff --check 通过。

### 应用内更新源显式未配置（2026-11-26）

- 入场提交 fa8bc73，工作区干净、无 remote。本轮不接入远程发行源，正式/测试源默认空列表，不再包含上游元数据地址；当前所选通道无源时返回独立的 UpdateCheckResult.NotConfigured。关于页保留检查和测试通道开关，显示“更新源未配置”的说明弹窗，不误报已最新或网络失败，也不提供安装动作。
- 保留显式注入源时的并发择新、通道/ABI 选择、失败回退和取消；元数据新增请求前 HTTPS 检查。下载域名白名单与入场提交逐字一致；AndroidAppUpdateInstaller 全文件未改，包名/版本/签名、下载/授权恢复/清理及 FileProvider 流程保留。检查请求 User-Agent 改为 RTBus。
- 新增 11 项主机回归：检查器 4 项覆盖默认正式/测试通道零请求、仅测试源按开关启用、HTTP/无协议元数据拒绝；安装器 PackageManager 夹具 5 项覆盖错误包名、同版/旧版、不同签名、缺失签名拒绝及文件清理，同签名新版本可进入授权步骤并重试/丢弃；AboutViewModel 和 Compose UI 各 1 项覆盖独立状态、不启动下载、通道切换、弹窗说明与关闭。夹具不是实际签名 APK，不替代真实升级验收。
- 定向 :androidApp:testDebugUnitTest :feature:me:testAndroidHostTest :androidApp:assembleDebug 通过（app 321 + me 58 = 379 项，0 failures/errors/skipped）。随后完整 test :androidApp:assembleDebug :androidApp:processReleaseMainManifest 通过（3m20s，638 tasks），18 个模块 JUnit XML 共 1,542 项，0 failures/errors/skipped；比 fa8bc73 净增上述 11 项，无删除/跳过测试。静态/检查器 26 项通过，git diff --check 通过。
- 两个最新 debug APK 各检查 31 份 DEX，均不含上游 update/update-beta 清单地址，无高德 DEX/原生库；各仅保留 graphics.path 与 datastore counter 原生库。Gradle checkDebug16kbPageAlignment 与两个 APK 的 zipalign -c -P 16 -v 4 通过。release 合并 Manifest 任务通过；未生成 release APK，未做真机/iOS 或真实签名升级。
- README、测试指南和待办同步，清除 todo 的过时“下一步删除 Key”。本地日志/汇总在忽略目录 build/rtbus-validation/：update-source-targeted-verified.log、update-source-full-host.log、update-source-static.log、update-source-suite-summary.json、update-source-apk-summary.json 和各 APK 的 update-source-*-zipalign.log。没有调用真实公交或上游更新接口；无 remote、不推送。

### Android release APK 构建与产物核查（2026-11-26）

- 基于 e571480，在 JDK 21 / SDK 37 / Gradle 9.8.2 下执行 `:androidApp:assembleRelease`：BUILD SUCCESSFUL，518 个任务（237 executed / 281 up-to-date）；Gradle `checkRelease16kbPageAlignment` 的 4 个 64 位共享库全部通过。
- 未发现 RELEASE_STORE_* 环境变量或 local.properties，按现有配置生成两个未签名 APK，没有创建或提交密钥。通过 JDK 21 直接运行 SDK apksigner.jar，两个 APK 均报 `Missing META-INF/MANIFEST.MF`，符合未签名状态；这些 APK 不能直接安装/升级。
- APK 元数据均为 applicationId `com.github.u5151561.trackr`、versionCode 21、versionName 0.1.13、targetSdk 36。arm64-v8a：4,770,050 字节，SHA-256 `D55DEB03FE323738EB02AB73914784872A7B3053F12E827312F118AD1E4C503C`；armeabi-v7a：4,768,126 字节，SHA-256 `4C5CF5FD0EEE9A9BA9E7DBA1511BE8A522E20D753BC344A5E004B2D84F813D26`。两个 APK 都由 `zipalign -c -P 16 -v 4` 验证通过。
- 实际 release 依赖报告、合并 Manifest 与两个 APK 通过 `.github/scripts/check_native_sdk_artifacts.py`；包内各仅有 graphics.path 和 datastore counter 原生库，无退役 SDK 依赖/组件/DEX 类。每包检查 1 个 DEX、14 份 Compose 资源及 aapt2 解码资源表，无退役 Key 页面/资源；另核查 R8 mapping 的 7,269 个原始类定义，避免混淆隐藏退役实现（旧导航序列化标识仍保留）。与 fa8bc73 的原有 6 个上游正式/测试清单地址逐一比对，DEX 中均未发现。两个实际 APK 的 Manifest 均经 aapt2 解码，核对 MainActivity、BusTrackingService、BootCompletedReceiver、BusStopWidgetProvider、FileProvider/authority 与安装权限保留，未启用 debuggable。Manifest 保留定位、后台服务、通知、精确闹钟、重启恢复与安装授权等现有权限/组件；不据此声称设备行为已验收。
- 输出位于 `androidApp/build/outputs/apk/release/`；本机日志在忽略目录 `build/rtbus-validation/`：`update-source-release-build.log`、`release-dependencies.log`、`update-source-release-arm64-zipalign.log`、`update-source-release-arm32-zipalign.log`；最终逐项汇总在 `release-audit-summary.json`，可复核脚本为 `audit-release-artifacts.py`，各 ABI 的签名/Manifest/资源表/对齐日志前缀为 `release-audit-`。复跑品牌、规划边界、SDK/Key 边界及检查器共 26 项全部通过，git diff --check 通过；本轮没有改动业务/构建代码，未重跑上一轮 1,542 项主机套件。release 构建和产物检查已完成；仍未完成真机功能/安装器验收、真实签名升级或 iOS Xcode 构建。无 remote，不推送。

### 本轮续接：语音/震动独立开关与单车上下车提醒（2026-11-26）

- 代码提交：`0cc1eb0`。普通公交线路、线路定时提醒保留；退役的是起终点规划/地图，不恢复高德能力。本轮 Android 新功能只生成并核验 debug 包，release 目录仍是上一阶段的未签名产物，不含本轮新增提醒功能。
- “我的”新增独立“震动提醒”，默认开；“语音播报提醒”仍默认关。两个设置分别保存，重启后保留四种组合。Android 追踪强提醒、实时通知兜底震动和定时通知均读取震动设置；定时通知补接语音设置。自动开始追踪成功的定时任务由后续追踪临近/到站反馈，不重复播报定时通知。普通通知提示音仍由系统渠道控制。
- Android 8+ 用原震动渠道和新增不震动渠道切换，保留用户对原渠道的重要级别、声音/震动设置；没有通过删除重建渠道强行覆盖系统偏好。手动兜底震动同样检查应用开关、通知权限和原渠道禁震/禁用状态。应用通知设置入口跟随当前选用的渠道。不能据此确认用户手机马达已恢复，系统勿扰/渠道/厂商后台仍需真机检查。
- 单车详情明确提供“追这辆车 · 到「…」提醒上车”和“我已上车 · 选择下车站”。前者锁定尚未开过候车站的指定车辆，后者固定同一辆车并显式确认下车站；取消、失联、已过站或启动失败不会凭旧状态开任务。候车提示“上车”，车上提示“下车”。共享模型新增尾部默认字段，旧序列化的指定车辆会话仍维持原有下车提醒语义。
- 下车站用统一纯函数判定：站序表示正在驶向或停靠的站，正在驶向而未停靠的当前序号仍可选；已停靠/经过的站排除。缺坐标导致的默认进度 1.0 不误判到站。弹窗刷新后失效选项不可确认。
- 同一辆车上车/修改下车站复用原任务名额，启动失败可恢复；指定车任务不会被“一起等”覆盖或吸收。单车面板优先显示正在看的车所对应的提醒，点结束只停该任务；其他车辆/线路任务保留。单线路去重键补齐城市、来源、方向和站序，避免跨城或环线同站 id 碰撞。
- 新回归先复现“一起等吞掉单车提醒、同车改站未替换、满额上车转换影响其他任务”三项失败（`boarding-session-repro.log`），修复后通过。早期新增 UI 测试缺少 LiveBus 必填字段的编译错误已补齐，旧候车目标期望已改为明确候车模式，没有跳过测试。
- 最终完整命令 `test :androidApp:assembleDebug --no-daemon --console=plain`：**BUILD SUCCESSFUL，2m 11s，636 tasks（34 executed / 602 up-to-date）**；18 模块 JUnit XML 汇总 **1,567 项，0 failures / errors / skipped**。另 26 项品牌/规划/SDK/Key 边界及检查器测试、`git diff --check` 通过。未连接真实公交数据接口；未执行 iOS 编译/链接或设备测试。
- 两个最新 debug APK：applicationId `com.github.u5151561.trackr.debug`、名称“实时公交.D”、versionName 0.1.13 / code 21；沿用既有 debug 签名，apksigner 验证成功且两个 ABI 签名证书相同。每包仅含本 ABI 的 graphics.path 与 datastore counter 两个原生库，退役 SDK 检查、新提醒代码 DEX 标记、实际 VIBRATE 权限、64 位 ELF 和 ZIP 16 KB 对齐通过。没有创建 release 密钥或更改签名配置。

| 当轮 debug 包（历史，已由下方最新包替代） | 字节 | SHA-256 |
| --- | ---: | --- |
| `androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk` | 33,453,066 | `5BF2293D61A655934132628B73E1CB7E0156487D400A220167CBAC4CF77C679C` |
| `androidApp/build/outputs/apk/debug/androidApp-armeabi-v7a-debug.apk` | 33,451,142 | `2F25F8A4F1E0395E410731FCEF939014EE93BB79A4B3864C8F5D32813D9D82F9` |

- 本机忽略日志目录 `build/rtbus-validation/`：最终 `boarding-feedback-verified-host.log`；上一轮完整通过 `boarding-feedback-final.log`；`boarding-static-checks.log`；逐项汇总 `boarding-feedback-audit-summary.json`；可复核脚本 `audit-boarding-artifacts.py`；双 ABI 签名/对齐/Manifest 元数据日志为 `boarding-<abi>-*.log`。
- 安装与实测操作见 [提醒验收清单](boarding-feedback-acceptance.md)。`adb devices -l` 无设备，真实马达/语音、锁屏与厂商后台、真实乘车到站均未验收。保留此前 release 静态核查记录，但不能把旧 release 包当作本轮新功能包。无 remote、不推送。

### 本轮续接：标题上下留白与普通静音兼容 v2（2026-11-26）

- 代码提交 `342bd44`，本轮完整主机/包核查见下。用户实机反馈上一轮 `b94822b`“关闭静音能震、普通静音仍不震”，且展开标题上方仍有整行空白。因此上一轮主机测试不能作为静音马达通过的证据，本轮也未连接手机，不能宣称已确认该机型根因或已完成硬件验收。
- 原因已核实的 UI 部分：只降低 LargeTopAppBar 展开高度仍保留原生 64dp 上导航行。新共用 CollapsingTitleBar 使用同一 Material 原生 TwoRowsTopAppBar 双标题淡入/裁切、颜色及 exitUntilCollapsedScrollBehavior，保留原版字体和基线。普通字号裁去展开时空行中的 40dp，随收起归零；可见高度约展开 80dp / 收起 64dp，另加状态栏安全区。淡入的小标题在剩余上行中居中，避免直接裁整栏时的大字中间态裁字；大于 1.5 倍的字体逐步减少裁切，2 倍不裁。首页搜索距顶栏 12dp、“我的”列表顶部 16dp，回顶展开和下拉刷新继续保留。
- 震动兼容调整：保留应用开关、通知权限、渠道启用/震动及勿扰检查；普通静音或公开铃声模式为 NORMAL 但铃声音量为 0 时，通知已发出也主动请求。统一用保留原始导航引导 AudioAttributes 的震动重载，替代上一轮 API 33+ 的 createForUsage(ALARM)。Android 会转换为 communication-request 震动用途并保留原音频用途；Robolectric 34 断言转换后用途和原始 audio usage。普通响铃/振动已发通知时不重复请求。没有改铃声模式/音量、没有私有绕过标志、没有恢复高德 SDK；不能推断高德内部具体实现。
- Android 新增“我的 → 测试震动”（位于震动开关下方），通过同一主动反馈路径立即测试，无需设线路。可选择复制/滚动的结果弹窗提供中文阻止原因、机型、Android/API、铃声模式/音量、勿扰、通知权限和渠道状态，并可打开系统提醒设置。报告包含“导航引导兼容版 v2”；REQUESTED 只表示接口已调用，不能当作硬件成功回执。平台能力默认不支持，iOS 不显示空测试入口。
- 保留 `0709c40` 的常驻“我已上车 · 选择下车站”固定底栏、显式选车/选前方下车站、单车跟随与多任务隔离。全国城市、普通公交、语音/震动独立开关、定时提醒及辅助功能均保留。
- 回归先复现旧实现的标题高度/静音用途缺陷；修正时发现并修复直接裁整栏的中间态小标题问题。最终完整命令 `test :androidApp:assembleDebug --continue --no-daemon --console=plain`：BUILD SUCCESSFUL，3m 14s，636 tasks（50 executed / 586 up-to-date）；18 模块 JUnit XML 共 1,599 项，0 failures / errors / skipped。另 26 项静态/检查器测试、git diff --check 通过。覆盖静音/零音量、正常模式不重复、开关/权限/渠道/勿扰/无马达、诊断创建与实时开关、VM 能力与关闭弹窗、320dp 诊断 UI、反复回顶/刷新、1.5 与 2 倍字体中间过渡。
- 已检查本轮原生渲染：original-home-expanded-dark.png、original-home-collapsed-dark.png、original-me-expanded-dark.png、original-me-collapsed-dark.png；新预览 balanced-headers-preview.png；各字体中间态 balanced-title-font-*.png；诊断弹窗 balanced-vibration-test-dialog.png。均在忽略目录 build/rtbus-validation/，是 Robolectric 渲染，不是真机截图。
- 两个 APK 已用本机既有 debug 签名生成；apksigner 通过，双 ABI 证书一致，且与上一轮签名一致。Manifest/VIBRATE 权限、诊断与 boarding DEX 标记、退役 SDK 边界、ELF/ZIP 16 KB 对齐核验通过。包名 com.github.u5151561.trackr.debug，名称“实时公交.D”，版本仍为 0.1.13 / code 21，用哈希及 v2 诊断标记识别。未更新 release，未生成密钥。

| 上一轮 v2 debug APK（历史原始路径，现已被 v3 构建覆盖；旧包副本见下） | 字节 | SHA-256 |
| --- | ---: | --- |
| androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk | 33,457,478 | C755E19A87FA26BA37A4059DC3723906938CD49F96B27118E31F298801611835 |
| androidApp/build/outputs/apk/debug/androidApp-armeabi-v7a-debug.apk | 33,455,554 | FE3BC18995BC77B4A8E873828D20BC3B1548AB6B075BEA7D735641AFA44ED341 |

- 便于识别的同字节副本：build/rtbus-validation/RTBus-20261126-silent-v2-arm64-debug.apk、RTBus-20261126-silent-v2-arm32-debug.apk；哈希已与原产物逐个核对。
- 本轮证据：build/rtbus-validation/balanced-vibration-repro.log（旧实现失败）；balanced-vibration-ui-final.log（专项成功）；balanced-vibration-final-host.log（最终全量）；balanced-vibration-static.log；balanced-vibration-artifact-audit.log；balanced-vibration-audit-summary.json；audit-balanced-vibration-artifacts.py；balanced-<abi>-signature/zipalign/badging.log。中间 targeted 日志含定位中的编译/断言失败，以最终成功记录为准。
- 参考 [AOSP VibrationAttributes](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/VibrationAttributes.java) 和 [AOSP VibrationSettings](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/master/services/core/java/com/android/server/vibrator/VibrationSettings.java)。系统策略按用途区分，普通静音不是全局禁震；厂商最终效果需实测，不能把本次调用选择当作该手机已验证的根因。
- adb devices -l 仍无设备。普通静音实际马达、厂商系统/后台、锁屏、语音和真实公交到站待按验收清单复测；iOS 未实际编译/运行。无 remote、不推送。

### 本轮续接：实际到站不震动，分段反馈修复 v3（2026-11-26）

- 最新用户反馈优先于旧待验收记录：v2 “测试震动”已在普通静音手机上成功，用户另反馈临近/追踪操作有震动，但实际到站没有震动。动作反馈不等同于到站通知链；没有据此宣称后台或实际到站已通过。
- 代码提交 `50d4f7b`。确认并通过回归复现的生命周期缺陷：TrackingPoller 成功提交 Finished 并移除会话后派发 ARRIVED；同一 Main 队列的会话 collector 随即清理此会话全部提醒，刚发的最终到站通知被马上撤掉。覆盖指定车两轮间越站补发、路线最后一程到站及另一条线仍在运行的场景。此缺陷会失去最终通知及其通知反馈机会，但不能单凭主机测试证明它是该手机不震的唯一原因。
- BusTrackingService 仅对“本轮 ARRIVED 效果发出时会话已自然完成”的 session 标记一次；collector 收尾消费标记，保留本轮最终到站通知原有五分钟超时，仍立即撤常驻进度和临近等旧提醒。用户手动结束、失效、服务回收中的活动会话仍沿用清理。完成后的最终通知可点击消除或自行超时，不留下永久常驻。
- 到站原为独有的单次 800ms 长震，临近/即时测试为有限分段波形。到站改用同一分段波形与导航引导调用、repeat=-1；没有证据认定该机型不支持单次长震。保留原有应用震动开关、通知权限、渠道及勿扰检查，也保留正常响铃/振动模式的通知渠道处理。ArrivalAlertManager 日志新增 APPROACHING / ARRIVED / TRANSFER / MISSED 事件、notificationPosted 和 vibration 结果，不记录线路或车牌。
- “我的 → 测试震动”诊断新增“到站反馈：分段提醒修复 v3”，原 v2 底层标记仍保留。版本仍 0.1.13 / code 21，需用 v3 诊断标记及哈希识别构建。没有改标题布局、开关 UI、上车入口或 KMP/iOS 结构，没有恢复退役 SDK/Key。
- 先写回归并实际看到 3 项断言失败（波形不一致、单车自然完成最终通知被撤、多任务最后一程最终通知被撤）。修复后定向 65 项通过；增加候车“临近 → 到站”连续轮询、关闭震动仍保留最终通知、最后会话停服务并 onDestroy 等覆盖，手动结束旧提醒清理测试继续通过。
- 最终 `test :androidApp:assembleDebug --continue --no-daemon --console=plain` BUILD SUCCESSFUL，1m 14s，636 tasks（23 executed / 613 up-to-date）；18 模块 XML 共 1,603 项，0 failures / errors / skipped。26 项 Python 静态/检查器测试、git diff --check 通过。沿用 Gradle 增量缓存；未把主机模拟当作手机马达验收。
- 双 ABI APK 签名核验通过，二者证书相同且与用户上一轮 v2 包一致；Manifest/VIBRATE、到站 v3/诊断/boarding DEX 标记、退役 SDK 边界及 ELF/ZIP 16 KB 对齐通过。包名 com.github.u5151561.trackr.debug，未生成密钥、未更新 release。

| v3 可安装 debug APK（项目内路径） | 字节 | SHA-256 |
| --- | ---: | --- |
| build/rtbus-validation/RTBus-20261126-arrival-v3-arm64-debug.apk | 33,457,478 | 4030D885BB439A91BFC996355D22DAECF3036EA1D8EFCEF1B86583743545E5C8 |
| build/rtbus-validation/RTBus-20261126-arrival-v3-arm32-debug.apk | 33,455,554 | CAF72C114596909906EA07E98E4A604537FF37CD65BB9BCBC7D2ACB0A6A49BF6 |

- 原始 APK 路径为 androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk / androidApp-armeabi-v7a-debug.apk；与具名副本已逐个核对哈希。旧 silent-v2 具名副本保留供回溯，不应再安装旧包验证本轮修复。
- 本轮证据均在忽略目录 build/rtbus-validation/：arrival-vibration-red.log（最终三项行为断言失败）、arrival-vibration-targeted.log / arrival-vibration-targeted-summary.json（65 项通过）、arrival-v3-full-host.log、arrival-v3-static.log、arrival-v3-artifact-audit.log、arrival-v3-audit-summary.json、audit-arrival-v3-artifacts.py、arrival-v3-<abi>-signature/zipalign/badging.log、arrival-v3-previous-signature.log。
- adb devices -l 无设备。用户确认的是上一轮即时测试物理成功，本轮实际到站、锁屏/后台及语音仍须复测；iOS 未实际编译/运行。只做本地提交，无 remote、不推送。

### 本轮续接：到站仍不震且未结束 → 确认到站与主动引导 v4（2026-11-26）

- 代码提交 `ac7540e`。最新用户实测明确为“到站依然没有震动，没有自动结束”；因此 v3 的自然完成清理修复解释不了这次现象。没有拿到该手机日志，不能认定厂商内部策略或唯一硬件根因。
- 主机回归先复现 4 项行为失败（arrival-v4-red.log）：单线路 ETA 30 秒就发 ARRIVED 并消耗标记、跳过停靠样本后到站提醒丢失、已上车目标停靠未完成、公开 RINGER_MODE_VIBRATE 下通知已发却跳过主动震动。追加 2 项显示回归失败（arrival-v4-display-red.log）：追踪卡与常驻通知仍把短 ETA 显示为到站。
- 修正单线路到站事件与会话显示：ETA 1..45 秒仅作临近，确认停靠（上游标志/已有可信进度门槛）才发 ARRIVED；追踪卡、常驻和 Xiaomi 焦点通知统一使用单线路严格口径。普通看板的“即将进站”及路线换乘的既有 ETA 判定没有全局改写。普通候车短时越站补发保留 3 分钟/最多过 3 站及原车窗口，且不得打断前车上车确认；旧数据、其他车不拿来恢复。
- 已上车的指定车辆到目标站停靠即 ARRIVED + Completed，无需等车辆驶过站；跳站仍补发并完成。指定车候车停靠时不当成已下车，保持原过站结束；普通不指定车候车可以继续等下一班。v3 最终到站通知五分钟保留、手动结束清理、多任务隔离继续回归通过。
- ARRIVED 不再因 notify() 成功或公开铃声模式而省略主动引导震动：静音/振动/正常模式均请求同一有限分段导航引导效果。到站通知用新增 bus_arrival_guidance_v4 声音/横幅渠道（默认 enableVibration(false)，初次复制旧渠道声音/重要级别，避免默认两路震动）；不删除/重建或重置旧渠道，不覆盖新渠道后续用户设置。显式震动仍检查应用独立开关、通知权限、原渠道启用/震动、新展示渠道启用和勿扰；没有设置全局音量、私有绕过标志或恢复高德 SDK。新渠道默认配置以外的用户自定义反馈仍由系统控制。
- 新增“我的 → 测试到站提醒”，Koin 注入真实 ArrivalAlertNotifier 链，单独通知 ID，不创建/结束实际会话，也不撤掉实际临近通知；不播放 TTS，通知声音由系统设置控制。原纯“测试震动”保留；报告格式化不会偷偷再次调用 raw test 来掩盖真实链路失败。平台能力独立门控，未支持平台隐藏。
- Android 私有偏好只保留最近六条真实事件的时间/类型/通知提交/震动结果/公开铃声模式和数值状态，以及最近轮询/完成状态；不含线路、车牌、车辆 ID 或坐标，不增加上传。测试不写真实历史。REQUESTED 只表示接口已调用，notification=true 只表示提交发送，都不是硬件或实际展示回执。报告包含“到站反馈：确认到站与主动引导 v4”，底层 v2 标记仍保留。
- 最终命令 `test :androidApp:assembleDebug --continue --no-daemon --console=plain`：BUILD SUCCESSFUL，3m 30s，636 tasks（40 executed / 596 up-to-date）；18 模块 JUnit XML 共 **1,620 项，0 failures/errors/skipped**。26 项 Python 静态/检查器测试和 git diff --check 通过。另有定向回归及首轮完整通过日志；最终结论以 arrival-v4-final-host.log 为准。
- Robolectric 320dp 原生渲染检查：arrival-v4-test-entry.png、arrival-v4-test-result-dialog.png，入口可点，诊断可选择/滚动，系统设置与关闭操作可达。没有修改原版标题过渡/间距及上车入口。本地无连接安卓真机，不能把这些检查当作马达、语音、厂商后台或实际到站验收。
- 两个 debug APK apksigner 核验通过、证书互相一致且与用户 v3 包相同；VIBRATE/Manifest、v4/上车/测试入口 DEX 标记、退役 SDK/Key 边界、ELF/ZIP 16 KB 对齐通过。包名 com.github.u5151561.trackr.debug，名称“实时公交.D”，版本仍 0.1.13 / code 21，必须用 v4 报告标记/哈希识别。未新建签名密钥，未更新 release。

| v4 可安装 debug APK（项目内路径） | 字节 | SHA-256 |
| --- | ---: | --- |
| build/rtbus-validation/RTBus-20261126-arrival-v4-arm64-debug.apk | 33,458,511 | FA8F4A959F4586952C0BE8BA1C0569B59E17F8A473D0C8BB5854C69662C69D45 |
| build/rtbus-validation/RTBus-20261126-arrival-v4-arm32-debug.apk | 33,456,587 | 621DE91348BCCF5A768E520D02D51D8D34DB5EB53F87103E304DE3B4E02224B8 |

- 原产物 androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk / androidApp-armeabi-v7a-debug.apk 与具名副本逐个核对哈希一致。旧 v2/v3 具名副本保留供回溯，不用于本轮复测。
- 忽略目录 build/rtbus-validation/ 证据：arrival-v4-red.log、arrival-v4-display-red.log、arrival-v4-targeted-verified.log、arrival-v4-full-host.log（首轮）、arrival-v4-final-host.log（最终）、arrival-v4-static.log、arrival-v4-artifact-audit.log、arrival-v4-audit-summary.json、audit-arrival-v4-artifacts.py、arrival-v4-<abi>-signature/zipalign/badging.log、arrival-v4-previous-signature.log。
- adb devices -l 无设备。真实 v4 静音/锁屏/到站/语音仍待手机复测；iOS 未实际编译/运行。只做本地提交，无 remote、不推送。

### 本轮：下车任务的单车跟随与返回体验 v5（2026-11-26）

- 用户已明确反馈 v4 到站震动成功。本轮只改导航和线路详情，没有改通知、马达或自动完成判定。主机回归在修改前复现了重复结束按钮和重进时回到站点卡的问题（`follow-v5-red.log`）。
- 首页追踪卡按会话 ID 打开；普通线路页按城市、来源、方向和进行中的指定车下车任务一次性恢复。用户主动退回总览后可一键「返回单车跟随」，后续轮询不抢焦点；过期卡不恢复其他任务。
- 单车卡头显示下车站，统计取该任务的剩余站数/预计时间；行程中给目标站加文字标记。浏览其他站不改任务；短暂离线保留车牌/目的地，不冒用其他车数据。底栏保留「更改下车站」「结束追踪」，只结束显示的会话；浏览反向也不取消已上车的提醒。

- 验证：18 模块 1,630 项主机测试全部通过（无跳过），26 项静态检查通过；双 ABI debug APK 与 v4 签名一致，Manifest、DEX、原生库及 ZIP 对齐核验通过。adb 未连接手机，不能把主机测试当作 v5 真机界面复测。日志 `build/rtbus-validation/follow-v5-final.log`，摘要 `follow-v5-audit-summary.json`。

| v5 安装包（项目内路径） | SHA-256 |
| --- | --- |
| `build/rtbus-validation/RTBus-20261126-follow-v5-arm32-debug.apk` | `F5D9CF98D4C158FB0426A935541970E97F500813F96E6854D2BF82BEFA8BA47A` |
| `build/rtbus-validation/RTBus-20261126-follow-v5-arm64-debug.apk` | `7CB0BB8BA4DFE08951FF1BD477289263DD8798D3149A733773263D0BB3C14CE2` |

### 本轮：过期下车提醒 v6（2026-11-26）

- v5 正常确认到站会结束；但接口失败、或线路有响应却不含指定车时，状态更新时间仍前移。只凭此时间读盘，或在进程尚存时只尝试接续中断会话，会让旧卡重新出现在首页。已用假时钟分别复现，无需实乘。
- 指定车下车提醒单独记录最近含该车的快照。失联后保留至最后的目标站 ETA 加 10 分钟；没有有效 ETA 时保留 30 分钟。若仍收到该车的新位置，使用新的预测；沿用单次追踪 3 小时总上限。失败、空刷、其他车辆、同一缓存快照和普通操作不续期。正常停靠/过站仍走原到站提醒链，不把超时算作到站。
- 冷启动发布会话前、Android 回前台及后台轮询均检查时效。只清理过期任务及其持久化记录，不清空其他追踪；v5 旧数据兼容。没有借此重新配置通知渠道、震动或定位权限。
- 保留首页点卡查看单车、点「结束」只取消这一项。失联宽限期内显示「暂无车辆位置」，不继续显示或朗读旧 ETA；收到新数据恢复，不添加清理设置。

- 最终 `test :androidApp:assembleDebug` 成功（18 模块 1,639 项，0 失败、错误或跳过）；26 项静态测试通过。双 ABI debug APK 包名及签名与 v5 一致，Manifest、DEX 和 16 KB 对齐核验通过。未连接 adb 设备，未做 v6 真机验收。日志/摘要在 `build/rtbus-validation/expiry-v6-final.log`、`expiry-v6-audit-summary.json`。

| v6 安装包（位于 `build/rtbus-validation/`） | SHA-256 |
| --- | --- |
| `RTBus-20261126-expiry-v6-arm64-debug.apk` | `DA005ABB577FEA5F5CF140A48A2D7686EA74824E9E93C07168EACB75F0FA4671` |
| `RTBus-20261126-expiry-v6-arm32-debug.apk` | `9E6AF0939162B0DAE318AECE911EFEDD029EAA0507A95D9A7D15705E45EF3231` |

### 本轮：首页过期提示与收藏前三条 v7（2026-11-26）

- 仓库在冷启动读盘、回前台清理或轮询实际移除过期下车提醒时产生提示，首页显示线路、下车站及未确认到站的原因。提示不修改会话持久化格式；不将手动结束或正常到站误报为过期，不改通知、震动或播报设置。
- 标准和长辈首页均按原顺序保留前 3 条，4 条以上点带数量的标题可展开、再点收起。筛选与编辑保留，折叠不改收藏和排序。
- `test :androidApp:assembleDebug` 成功：18 模块 1,642 项主机测试，0 失败/错误/跳过；26 项静态检查通过。双 ABI 调试 APK 已核验包名、签名（与 v6 一致）及 ZIP 对齐。无 adb 设备，未做真机验收。日志 `build/rtbus-validation/home-v7-final.log`、`home-v7-audit-summary.json`。

| v7 安装包（位于 `build/rtbus-validation/`） | SHA-256 |
| --- | --- |
| `RTBus-20261126-home-v7-arm32-debug.apk` | `38BD3543E31281C6C855EE8005283201E049D36D41E3B0C8F1CF7CEC0137EABF` |
| `RTBus-20261126-home-v7-arm64-debug.apk` | `4DEB0F0EF6A1D87D367B967F8E039EE7AECFD4ACA2AA5C6C05B5EFFC10F6FB76` |

### 本轮：「我已上车」车辆按到站先后排序 v8（2026-11-26）

- 多辆可选车时，弹窗标明当前候车站，可切换数据源原顺序或「先到的在前」。沿用首页/站点页已持久化的排序偏好；不改变下车站列表，不因排序创建提醒。
- 本站停靠优先，只用与此站匹配的预测秒数；无预测时按剩余站数估先后。已过站但仍有后续站可选的车保留在末尾，按 busId 选中。新增规则、弹窗交互和偏好同步测试。
- `test :androidApp:assembleDebug`：18 模块 1,645 项主机测试，0 失败/错误/跳过；26 项静态检查通过。双 ABI 包名、签名（与 v7 一致）、DEX 和 16 KB 对齐核验通过。无 adb 设备，v8 未做真机验收。证据：`build/rtbus-validation/boarding-v8-final.log` 与 `boarding-v8-audit-summary.json`。

| v8 安装包（位于 `build/rtbus-validation/`） | SHA-256 |
| --- | --- |
| `RTBus-20261126-boarding-v8-arm32-debug.apk` | `119C6945C6D6F8366334B44F24D5DA9AB2E973C33761FE18E8A9DD7B918316DC` |
| `RTBus-20261126-boarding-v8-arm64-debug.apk` | `3F218BFB75156D12A648B346F1652B39A9BD32A48114C7CEBBD4A5D6B9BDFA38` |

## 7. 唯一明确的下一步

覆盖安装 v8，按 boarding-feedback-acceptance.md 第 5 节 v8 小节验证实际车辆排序、切回默认和按车牌选择下车提醒。release 签名与 iOS 实机验收仍独立待办。无 remote，不推送。

## 8. 记录维护

每个阶段完成或准备切换任务时：更新本文件的已完成/未完成/验证结果/下一步，以及 todo.md；只记录有效状态，不复制全量对话和大型日志，不写敏感信息。与代码一起作合理本地提交，并在回复中给出提交号和未解决事项。

更多资料：plan.md = 完整方案与许可核查；todo.md = 实施清单；preflight.md = 初始环境与构建尝试；branding.md = 更名范围和验证。
