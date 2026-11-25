# RTBus Android 构建与测试基线

日期：2026-11-25。对象：`f79d005` 的工作树（品牌更名后、高德裁剪前）。
本轮不改业务代码、依赖版本、应用身份或存储结构；不连接真实公交接口。

## 工具链实查与补齐

- 项目要求来自 `build-logic/convention/src/main/kotlin/com/trackr/buildlogic/TrackrBuild.kt` 和 `build-logic/convention/build.gradle.kts`：JDK toolchain 21、compileSdk 37、targetSdk 36、Java 字节码目标 17。
- 已安装 Temurin JDK **21.0.13.1+2-LTS** 到 `C:\E\dev-tools\jdk-21`。从 Adoptium API 获取 Windows x64 HotSpot ZIP，解压前比对官方 API SHA-256：`bbebea1469996b276683c9299c227b294f85c847039c82633218186a4ad4fbf2`，一致。
- 已安装 SDK `platforms;android-37.0` revision 2 与 `build-tools;37.0.0`。AGP 配置阶段实际选用 Build Tools **36.0.0**，并基于本机已接受的 SDK 许可自动安装；不强制覆盖其默认版本。保留原 SDK 34/35 及原 Build Tools。
- Command-line Tools 已从 12.0 补齐到 **23.0**。安装器先放在 `latest-2`，随后将旧版保留到 `cmdline-tools/12.0`、新版归位到 `cmdline-tools/latest`；移动前核对目录边界、版本和目标不存在，不删除旧工具。新版 `--list_installed` 已确认平台/Build Tools 安装，且不再报 SDK XML 或安装位置不一致；仅提示新版推荐使用 Android CLI。
- 使用本机已有 SDK 许可状态安装；没有自动输入许可同意。
- Gradle wrapper **9.8.2** 的 `--version` 成功，Launcher/Daemon JVM 均为 JDK 21。
- 不改全局 `JAVA_HOME` / PATH，仅给本次构建进程设置；全局仍可能指向 JDK 17，后续不可直接假定默认 `java` 可用。
- Gradle 用户缓存：`C:\E\dev-tools\gradle-home`，不提交本机配置、缓存或下载包。

## Windows 调用方式

工程无 `gradlew.bat`，从实际 Git 根 `C:\D\A_code\tracker\Trackr-main` 使用 wrapper 主类：

```powershell
$env:JAVA_HOME = 'C:\E\dev-tools\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME = 'C:\E\dev-tools\android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = 'C:\E\dev-tools\gradle-home'
& "$env:JAVA_HOME\bin\java.exe" -classpath gradle/wrapper/gradle-wrapper.jar `
    org.gradle.wrapper.GradleWrapperMain :androidApp:assembleDebug `
    --no-daemon --console=plain --stacktrace
```

## 验证结果

- `python .github/scripts/test_branding.py`：6 项通过，仅品牌静态检查。
- `git diff --check`：通过。
- `:androidApp:assembleDebug --no-daemon --console=plain --stacktrace`：**BUILD SUCCESSFUL in 17m 17s**，335 个任务执行，退出码 0。项目附带的 `checkDebug16kbPageAlignment` 报告 6 个 64 位共享库全部通过。首次依赖解析耗时不是源码失败。
- Kotlin / Android JVM 定向测试：**BUILD SUCCESSFUL in 5m 49s**。按模块结果见下；所选测试全部失败 0 / error 0。不能据此声称全量测试或真机验收。
- Windows 不能运行 iOS 模拟器测试；不修改 KMP 目标以隐藏该限制。

## 构建产物（仅裁剪前基线，不是发行版）

目录：`androidApp/build/outputs/apk/debug/`（构建产物不入 Git）。

| APK | 字节数 | SHA-256 |
| --- | ---: | --- |
| androidApp-arm64-v8a-debug.apk | 78150301 | 2155a1a6029640c928d5f0e6dab880bb24f310066c574829cfc57c573611a044 |
| androidApp-armeabi-v7a-debug.apk | 66908957 | 5addc4ef445a5ef24782721719ff4a8a9b2019ebe29c5d968bdd4cd43b13425d |

`output-metadata.json`：applicationId=`com.github.u5151561.trackr.debug`，versionCode=21，versionName=0.1.13。
高德 SDK、上游身份与更新端点尚未改造，不得把这两个包当作最终 RTBus 发行版，或据此宣称独立身份/与原调试版并存。
尚未安装到真机/模拟器，未做运行验收、release 构建或升级安装验证。
构建存在原有 Kotlin 警告、native 符号未剥离提示及 Gradle 10 弃用提示；本轮不进行无关清理。

## 保留功能基线测试范围

已启动以下已有测试（Fake / 桩数据），不用真实公交业务接口：

- `:feature:bus:testAndroidHostTest`：53 项通过（BusHomeViewModelTest、CityPickerRowsTest）。
- `:feature:search:testAndroidHostTest`：30 项通过（SearchViewModelTest、SearchRowsTest）。
- `:feature:route:testAndroidHostTest`：4 项通过（RouteDetailViewModelTest）。
- `:core:data:testAndroidHostTest`：41 项通过（含 CountyLevelCityMatchTest、TransitProviderCoordinatorTest、RealtimeRepositoryLineDetailCacheTest）。
- `:core:domain:testAndroidHostTest`：215 项通过；`:core:navigation:testAndroidHostTest`：70 项通过。

这不是全量回归或真机验收。选取的城市仅是测试数据，不代表支持城市白名单。

**合计：413 项通过，0 失败、0 error。**这是针对首页/城市选择、搜索、路线详情、数据源与导航的定向 JVM 基线，不是全量回归。

测试采用现有 Fake/桩数据；没有配置或调用真实公交业务 API。Windows 未运行 iOS 测试。

## 日志与下一步

构建日志在本机 `%TEMP%/rtbus-baseline-jdk21-build-20261125.log`，测试日志在 `%TEMP%/rtbus-baseline-jdk21-tests-20261125.log`；不提交完整 daemon 环境/诊断日志。
Android 构建与定向 JVM 基线已明确。接下来解耦路线规划与路线详情；全国城市选择、搜索、收藏与追踪的范围不变。
