# RTBus 续接交接

更新日期：2026-11-26。用途：新任务恢复项目状态；不是完整对话副本。以实际 Git、代码和验证结果为准，发现记录过时先核对再更新。

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

## 7. 唯一明确的下一步

下一独立阶段是 Android 真机保留功能验收，可先使用现有 debug 签名包；release 的真实签名安装/升级和应用内安装器验收需用户控制的签名配置与同签名新版本包，不生成或提交密钥。iOS 编译/运行需在 macOS/Xcode 环境完成。release APK 构建和静态产物检查已通过，不等同于设备验收。不要重做已完成的 Key/隐私、SDK/CocoaPods 或规划 UI/数据链删除；保留全国城市、普通公交及辅助功能。自己的远程更新源、发布 workflow 和元数据等第一版可用、用户确认远程后再接入。不配置 remote，不推送。

## 8. 记录维护

每个阶段完成或准备切换任务时：更新本文件的已完成/未完成/验证结果/下一步，以及 todo.md；只记录有效状态，不复制全量对话和大型日志，不写敏感信息。与代码一起作合理本地提交，并在回复中给出提交号和未解决事项。

更多资料：plan.md = 完整方案与许可核查；todo.md = 实施清单；preflight.md = 初始环境与构建尝试；branding.md = 更名范围和验证。
