# RTBus 精简改造方案与许可证核查

核查日期：2026-11-25（Asia/Shanghai）。状态：方案，未开始应用代码改造。
本地范围：下载得到的 Trackr-main，不含 .git。联网核对了上游 LICENSE、README 及致谢仓库的许可证范围；未请求公交业务数据接口，未进行构建或运行验证。
核查时上游 main HEAD：31e4ed388ea8f227a2e995d84cc236b4a552cbfd。此记录不是对下载包每个文件与该提交一致性的证明。

## 1. 结论与适用边界

**可以基于 MIT 许可修改、精简、改名为 Real-Time Bus（RTBus），做独立的实时公交查询应用。**
这只是代码许可及静态架构层面的可行性结论，不是公交接口访问、数据再分发、商业上线或名称商标的法律放行。

- 原 LICENSE 是 MIT，版权行是 `Copyright (c) 2026 busonroad contributors`。允许复制、修改、分发、再许可和出售，但软件副本或实质性部分须携带原版权及许可声明。
- 最简单的发布方式是 RTBus 继续使用 MIT：完整保留原 LICENSE，在 README 明确衍生关系和主要改动，在关于页提供可实际查看的许可全文及第三方声明。自己的新增代码可补自己的版权行，不能以此替换原版权。
- MIT 不要求必须开源修改代码，也不要求运行界面一直叫 Trackr；保留致谢和上游链接是推荐做法。名称、图标、商标不能据此视为获得单独授权；建议使用自己的图标，正式发布前另查 RTBus 名称冲突。
- README 的免责声明原文是：“项目仅用于学习与技术交流。实时公交数据来自第三方公开接口，请遵守数据提供方的服务条款，不要用于商业用途或高频抓取。”MIT 的代码使用条款与这段风险声明必须分别考虑，不能把整个应用描述为已获无条件商用授权。有商业计划时，建议向作者澄清声明范围，另行取得数据方相应许可。
- ChelaileApiClient 类注释明确说明基于微信小程序协议抓包逆向；MygolbsApiClient 使用 H5 数据面协议。端点可访问不代表官方开放 API，开源作者不能替数据方授权。自用、免费和低频也不自动等于接口使用合规。
- 本次未取得或确认各数据方适用于第三方客户端的明确接口授权及服务条款。因此正式公开分发前应完成这一核验；不能确认时改接具有相应使用许可的数据源，或只做离线录制数据演示。
- 删除高德可以解除对高德功能及 SDK 的技术依赖，但不能解决车来了、掌上公交、掌上出行等其他来源的数据授权问题。

### 1.1 上游致谢项目核查

| 项目 | 实际核查结果 | RTBus 应对 |
| --- | --- | --- |
| misakano7545/yulin-bus-activity | 根目录 MIT，版权 2026 小御坂 | 若实际包含其代码/实质性内容，携带其相应声明；仅凭致谢不能判断复制范围 |
| PeanutSplash/chelaile-mcp | 根目录 MIT，版权 2026 Jan Liu | 同上，保留必要声明 |
| mage802/BusFleetTracker-GuiYang-Public-Transit | 根目录没有 LICENSE；guiyang-mygolbs-collector/LICENSE 是 MIT，版权 2026 Contributors；README 明确表示不能将子项目 MIT 扩大到整个仓库和车来了目录 | 若保留实现直接复制了无明确许可范围内的代码，需要核实来源并取得许可，或移除/独立实现；本次未完成逐段代码溯源，不能据此断言 Trackr 已违规，也不能出具全依赖合规保证 |

本地没有发现独立 THIRD_PARTY_NOTICES 或其他 LICENSE/NOTICE 文件（除 Gradle wrapper jar 外未见随源码打包的 jar/aar/so/font 文件）。这不代表 Gradle 解析到的依赖没有声明义务；发布前应按实际 release 依赖图检查直接、传递依赖和图形资源，并生成 notices。

## 2. 修订后的产品边界（用户 2026-11-25 明确）

**本轮不是“只剩查询的极简版”，而是“保留 Trackr 好用功能的无高德 RTBus”。**
上一版删除辅助功能的建议作废。用户已明确：只是不需要高德那块，其他正常运行、好用的功能不要动；应用内更新保留并切换成自己的源。

### 全国查询与验收范围

- 保留原有全国城市选择、城市切换及线路/站点搜索能力，不做单城市版，不添加城市或线路白名单；实际数据覆盖仍以原有 Provider 的服务范围为准，本次改造不主动缩减。
- 少量真实城市/线路只是可选的真机验收样本，不是产品支持范围，也不是开工必须由用户提供的信息。自动测试覆盖不同城市切换、同名线路、来源/方向/站点隔离和缓存键，避免数据串用。

### 保留，不以“辅助功能”名义裁剪

- 城市选择、多数据源、线路/站点搜索、方向切换、公交文字站序、到站信息、时刻表和附近站点。
- 线路、站点及不依赖高德的既有路线收藏、分组、排序、编辑与搜索历史。
- 单线路追踪、一起等、既有路线追踪、上/下车提醒、定时提醒、语音播报，以及现有通知/厂商适配。
- 乘车码、桌面小组件、快捷方式、相关系统入口和 deep link。
- 主题、长辈版、无障碍、通知诊断等既有设置。
- 检查更新、正式/测试版通道（若原有）、下载进度、安装授权与安装流程，更新来源改为 RTBus 自己维护的来源。
- 其他现有功能原则上保持原行为；若发现依赖高德，先明确依赖并做最小解耦，不能擅自扩展删除范围。

### 移除

- 高德地图展示与地图上的规划交互。
- 高德起终点搜索/提示、公交算路和由其产生的新换乘方案入口；本轮不引入另一家地图/算路平台替代。
- 高德 Key 配置、校验/诊断、SDK 初始化、隐私同意专用流程与相关构建配置。
- 无剩余调用方的高德 SDK、Web API 客户端、平台桥接、依赖、资源和专属 Manifest 声明。

“不用路线”收窄为“不用高德路线规划”，**不等于删除公交线路、不等于清空已有路线收藏，也不等于删除路线追踪**。保留功能中若存在“重新规划”等高德专属按钮，只移除该动作；文字路线、实时信息和追踪按实际依赖验证后保留。

## 3. 拆分要点：不能按模块或名字粗删

- feature/route 同时包含 RoutePlanViewModel/RoutePlanRoute 和 RouteDetailViewModel/RouteDetailRoute。RouteEntries.android.kt 同时注册 ScreenKey.RoutePlan 和 ScreenKey.RouteDetail，因此不能直接删整个模块或全部 routeEntries。
- RouteDetailViewModel 仍依赖 RouteAlignmentService、路线仓库、实时仓库和追踪控制器。RouteAlignmentService 同时处理高德方案与普通路线的对齐、返程等操作，需要按方法/调用方拆分，不可整类删除。
- 保留路线详情与追踪的 DI，删除规划 ViewModel 与高德网关的 DI；必要时把混合绑定拆成 planning-only 与 route-detail 两部分。小范围修改优于搬迁整个模块。
- 根页面当前提前取得规划/详情 ViewModel，并有 AmapKeyRepository 和高德隐私逻辑；只隐藏 Tab 仍可能初始化高德，需清除启动及组合过程的实际引用。
- 公交数据解析使用的 RouteProjectionMath 不能因名称含 Route 就删除。坐标字段、WGS-84/GCJ-02 转换等如仍被追踪或数据解析使用，也必须保留。
- AndroidLocationSource 使用系统 LocationManager，因此附近站和追踪定位的保留不要求保留高德 SDK；具体定位/提醒行为仍需真机回归。
- 删除路线规划 Tab 后，按现有 UI 保留公交和我的页签，不顺带重做整套首页、收藏和设置。
- 保持现有 KMP/Compose、Koin、Room、DataStore、Ktor 与模块结构；本轮不升级整套依赖、不改成 Android-only、不做数据库大重构。

## 4. 应用内更新：保留机制，切换归属

已有 GithubUpdateChecker 支持正式版/测试版更新清单、镜像回退和 APK 下载白名单。AndroidAppUpdateInstaller 已有包名、版本号和签名检查；这些安全检查应保留，不为方便下载而关闭。

实施内容：
1. 按用户要求先做纯本地开发，不创建远程仓库、不配置 remote、不推送；第一版完整可用且用户确认后，才接入其指定的远程仓库/发布源。替换原 5151561/Trackr 的正式版、测试版和镜像端点时不能编造仓库，也不能回退原作者更新清单。
2. 同步修改更新元数据中的 APK 地址、项目/发布页链接，以及 .github/workflows/update-branch.yml 的发布逻辑、权限和自己维护的签名配置。
3. RTBus 使用自己的 applicationId 与发布签名；后续 APK 沿用 RTBus 自己的签名并递增 versionCode。包名/签名检查以当前安装的 RTBus 为基准，不要求与原作者相同。
4. 如果自建下载域名，显式调整受信任下载源并测试跳转；不取消 HTTPS、包名、版本号和签名验证。
5. 保留安装权限、FileProvider、安装授权设置跳转、下载缓存与进度等实际需要的组件；不可按上一版方案删除。
6. 高德原生库移除后，是否继续 ABI 分包按剩余 native 依赖和实际 APK 决定，不能假设所有 native 库均不存在。
7. 更新源尚未提供时，本地版明确显示更新源未配置，不访问原作者清单；以 MockEngine/录制元数据与本地 APK 测试夹具验证机制，不为本地验证放宽生产 HTTPS、包名或签名校验。自己更新源未配置不阻止其他功能开发，也不是删除更新功能的理由。

验收：RTBus v1 安装后可检测、下载并升级到自己的 v2；错误包名、错误签名及旧版本包被拒绝；原版更新源故障或发布与 RTBus 无关。

## 5. 实施顺序与验收

### A. 基线与功能保留清单

- 保留原下载包，建立版本控制；记录原版构建与测试结果。本地没有 gradlew.bat，Windows 使用可用兼容 shell 或补齐匹配版本的官方 wrapper 启动脚本。
- 按实际现有入口记录核心查询、收藏、追踪、提醒、乘车码、小组件、长辈版与更新行为。不能把代码存在当作功能已通过验证。
- 变更应用名、图标、applicationId 和发布签名，保留 MIT 原版权和许可；源码包名首轮不全局替换。

验收：RTBus 与原版可并存，保留清单明确、基线结果可复查。

### B. 先拆混合依赖，再删除高德

- 拆开规划与详情的导航、ViewModel、DI 和混合服务方法，保留非高德路线详情及追踪。
- 仅移除高德规划 Tab/动作及入口；移除 Key 页、启动初始化和高德专属隐私逻辑。
- 移除高德 SDK/Web 调用与无人使用的构建变量、镜像、APSService/API Key metadata；同步必要的 iOS 平台引用，但不顺带删除 iOS 支持。
- 按引用核实共享字段、缓存、仓库和数学工具，保留所有仍被查询、追踪、收藏使用的部分。

验收：无高德 Key 仍可启动；release 依赖图及 APK 无高德 SDK；合并 Manifest 无高德专属组件；根页面不解析已删除 DI；普通路线详情仍可打开。

### C. 接入自己的更新发布链

- 配置用户实际提供的源，保留原更新机制和安全校验；使用自己的签名/构建机密，不复制或公开原作者密钥。
- 生成并验证自己的更新清单及 APK 下载地址。若暂不需要测试版，只对通道做明确配置，不私自移除整个功能。

验收：用受控 v1/v2 测试更新成功、错误包拒绝、下载失败和安装授权恢复。

### D. 完整回归，最后清理无用配置

- 测试无定位权限的手动查询、附近站定位、多来源城市、换向、时刻表、收藏/分组/路线详情。
- 测试前台轮询生命周期，以及用户主动启动后的后台追踪、到站通知、定时提醒和语音；保留原功能时不能用“后台零请求”作为全局验收目标。
- 测试乘车码跳转、小组件刷新/跳转、快捷方式、长辈版与通知入口；应用 ID 改动后核查 FileProvider、PendingIntent 和硬编码包名。
- 权限按保留功能实际需求保留。后台定位、前台服务、通知、闹钟、开机接收及 APK 安装组件如仍被使用，不得一概删除；无高德 SDK 不等于无定位功能。
- 不清空既有路线收藏，不无故改表。必要存储变更使用正式 migration，并同步备份规则/测试，不做 destructive fallback。
- 更新 README、隐私说明、关于页、第三方 notices 和 CI；发布前完成原第 1 节所述数据许可核验。

验收：受影响和保留功能测试通过、Android release 可构建；真机关键入口有回归记录。不得声称本次已运行验证，也不承诺未经测量的 APK/耗电变化。

## 6. 约束与待提供信息

- **删除边界**：仅高德专属能力及确认无引用的附属配置。其他功能默认保留，发现耦合先拆而不是直接删。
- **本地提交/推送边界**：用户已要求先在本地 Git 一步步提交；原版源码快照、方案/检查记录、后续每个可验证改动分别提交。不创建远程仓库、不配置 remote、不推送；第一版完整可用后仍需用户明确确认目标与推送。
- **更新源**：目前没有远程仓库，这是正常的本地开发阶段，不是阻塞；更新机制以测试夹具验证，实际发布源等用户准备并确认后再接入。签名由维护者持有，不放入仓库或聊天。
- **数据许可**：第 1 节的限制不因保留辅助功能或移除高德改变；本轮未取得公交数据方授权证明。
- **历史数据**：新的 applicationId 不会自动取得 Trackr 沙箱数据；本轮不承诺原版原地覆盖升级。如需要迁移，另设计用户明确操作的导出/导入。
- **验收范围**：这是文档方案修订，不是功能已验证结论；尚未更改应用源码或运行构建。

## 7. 核查来源

本地关键文件：LICENSE；README.md（免责声明 320–322、致谢 326–334、许可 338–340）；settings.gradle.kts；androidApp/build.gradle.kts；androidApp/src/main/AndroidManifest.xml；AndroidLocationSource.kt；ITransitProvider.kt；DefaultRealtimeRepository.kt；TransitProviderCoordinator.kt；BusHomeViewModel.kt；TrackrRoot.kt；TrackrModules.kt；GithubUpdateChecker.kt。

联网只读核查的一级来源：
- https://raw.githubusercontent.com/5151561/Trackr/main/LICENSE
- https://raw.githubusercontent.com/5151561/Trackr/main/README.md
- https://api.github.com/repos/5151561/Trackr/commits/main
- https://api.github.com/repos/misakano7545/yulin-bus-activity/license
- https://api.github.com/repos/PeanutSplash/chelaile-mcp/license
- https://raw.githubusercontent.com/mage802/BusFleetTracker-GuiYang-Public-Transit/main/README.md
- https://raw.githubusercontent.com/mage802/BusFleetTracker-GuiYang-Public-Transit/main/guiyang-mygolbs-collector/LICENSE

以上记录只支持对应文件内容，不构成对相关接口全部适用条款或商业授权的确认。
