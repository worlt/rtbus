# 品牌更名记录

日期：2026-11-25。范围仅为名称/静态文案，不包含高德拆除和更新源切换。

- 中文显示名称：实时公交；英文显示名称：Real-Time Bus；简称：RTBus。Android 中文调试版为实时公交.D，英文调试版为 RTBus.D；iOS 默认中文显示名为实时公交。
- 已同步 Android 中英文应用名称、共享关于页、通知/提醒/安装提示、iOS 应用与小组件显示名称/文案。
- Gradle 根项目名称改为 RTBus，README 增加衍生关系及本地开发状态说明；移除容易被误认成 RTBus 发行版的上游 release 徽章。
- 关于页原仓库链接明确标为“上游项目”；原 MIT LICENSE 和 busonroad contributors 版权行完整保留。
- 源码包名、类名、applicationId、Bundle ID、URL scheme、数据库/偏好文件名、签名配置、更新地址和图标不变，避免将改名扩展成兼容性重构。
- 物理目录仍为 Trackr-main，未移动工作区；现有截图仍是上游参考，没有冒充已生成 RTBus 截图。

## 验证

运行 `python .github/scripts/test_branding.py`：6 项检查通过，覆盖 Android 正式/调试名称、中英文资源、关于页文案、iOS 应用与小组件 plist、Gradle 项目名及原版权/上游声明。

运行 `git diff --check`：通过。比对确认 LICENSE、Android 应用 ID 配置、Manifest 和更新端点文件没有修改；通知标题对应的 Kotlin 测试断言已同步为实时公交。

上述是静态资源/配置检查，不是完整 Gradle/Kotlin/Swift 测试。JDK 21 / Android SDK 37 的环境缺项仍待处理，因此尚未生成 APK，也未完成 Android 真机或 iOS 构建验收。应用 ID 尚未独立，不宣称此时可与原版并存。
