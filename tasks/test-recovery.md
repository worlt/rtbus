# 主机测试恢复记录（2026-11-25）

## DataStore：Windows 文件系统边界

- 入场时已有一份未提交的主机测试草稿；原样副本保存在本机忽略目录 `build/rtbus-validation/incoming-TrackrDataStoresTest.kt`，在其基础上续接，未回退用户改动。
- 原失败不是 corruption handler 没运行：Android 版 DataStore 在 Windows 主机上用 `File.renameTo` 覆盖现有损坏文件失败，主异常仍显示原始解析错误。
- 项目工厂增加 internal Storage 注入边界。生产入口仍用原平台 `createWithPath`，没有更换生产存储后端或升级依赖。
- 损坏恢复测试注入 Okio Storage，只把 atomic move 转交 NIO 的 `ATOMIC_MOVE` + `REPLACE_EXISTING`；解析、handler、迁移参数和 scope 仍走项目工厂。没有先删目标文件或吞异常。
- 保留空 Preferences 断言，并关闭后使用生产默认工厂重新读取，证明写回了有效文件，不是仅返回空值。7 项 DataStore 测试通过。
- 反证：临时令共享 handler 抛回 CorruptionException，目标测试按预期失败；变异已恢复。日志 `build/rtbus-validation/resume-regression-red.log`。
- 限制：这覆盖项目恢复逻辑和主机上的持久化，不代替 Android 设备文件系统验收。

## 更新取消：等待被断言的生命周期

- `cancelAndJoin` 等待查询调用方结束，不等价于 MockEngine 两个 handler 的 finally 均已结束。原测试在完整 app 套件下复现取消计数 0。
- 由两个 handler 的 finally 发出完成信号，在有限超时内等待后再断言。两个请求已进入、调用方已取消、两个 handler 均被取消的断言全部保留，且在关闭 HttpClient 之前检查。
- 更新生产逻辑、HTTPS/下载域名/安装安全校验未修改。

## 小组件：清理 Application 所拥有的资源

- 原异常出现在不同小组件测试的 `runTest` 入口，是前序后台异常被转报，并非小组件收藏断言失败。
- XML suppressed 堆栈显示 Room Invalidation Tracker Refresh 访问已被 Robolectric reset 清掉的 LegacySQLite 连接（`Illegal connection pointer`）。此前只在下一个 Application.onCreate 调 stopKoin，晚于上一个 Application 的平台重置。
- TrackrApp.onTerminate 现在停止其 Koin 容器，通过现有 onClose 释放存储和应用级作用域。没有改动小组件、数据库结构或切换 SQLite 测试实现。真实 Android 进程销毁不依赖 onTerminate；此回调负责模拟进程终止时的清理。
- 新增 Application 终止回归，检查容器清空、应用 Job 不再 active。修复前按预期失败（Koin 仍存在）；修复后完整 app 套件 314 项通过。

## 验证记录

- 初始复验：DataStore 草稿 7/7；Android app 313 项中 2 失败（更新取消及落在 TrackrWidgetTest 的 Room 异步污染）。日志 `resume-initial-tests.log`。
- 定向隔离：Room 仓库 + 小组件 + 更新检查联合筛选、StorageBackend/设置 + 小组件联合筛选均通过；单次通过不足以排除跨 Application 生命周期泄漏。
- 完整主机套件 + debug APK：BUILD SUCCESSFUL，1,623 项全部通过；完整 app 套件额外强制重跑 314 项全部通过。日志 `resume-host-suite.log`、`resume-app-repeat.log`。
- 全部日志在本机忽略目录 `build/rtbus-validation/`；没有访问真实公交接口。完整主机套件与 APK 验证结果见 HANDOFF.md。
