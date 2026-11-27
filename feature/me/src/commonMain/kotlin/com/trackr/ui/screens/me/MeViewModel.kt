package com.trackr.ui.screens.me

import com.trackr.core.model.CityChangeMode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackr.core.data.repository.SettingsRepository
import com.trackr.core.domain.platform.BoardingTraceLog
import com.trackr.core.domain.platform.LocationSource
import com.trackr.core.model.EngineMode
import com.trackr.core.model.ScheduledReminder
import com.trackr.core.model.TransitCodeProvider
import com.trackr.core.model.RailMode
import com.trackr.core.model.ThemeMode
import com.trackr.core.model.UiMode
import com.trackr.data.repository.IScheduledReminderRepository
import com.trackr.ui.platform.LiveUpdateAccess
import com.trackr.ui.platform.LiveUpdateStatus
import com.trackr.ui.platform.NotificationHealth
import com.trackr.ui.platform.NotificationHealthAccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 「我的」页状态（v3 分组：出行、通知、关于）。
 *
 * @param alertThresholdStops 到站前提醒：车离上车站还有几站时提醒（1–3）
 * @param remindersSubtitle 「定时提醒」那一行的副标题（最早一条开着的提醒）；没有提醒为 null
 * @param liveUpdate 实时活动通知支持没支持、开没开；平台没有这项（iOS）或实时通知关着时为 null，不显示那一行
 * @param notificationHealth 通知可靠性体检；全正常或平台没有这项（iOS）时为 null，不显示那一行
 * @param versionName 「关于」那一行显示的版本号；读不到为 null
 * @param showTransitCodeButton 「乘车码」按钮显不显示：首页悬浮按钮和线路页、站点页底栏的圆钮都听它（`SettingsRepository.showTransitCodeButton`）
 * @param autoStopAfterBoarding 上车后自动结束追踪（issue #32）：单线路和一起等判出已上车就结束，路线不受影响
 * @param canExportBoardingTrace 有没有「导出上车判定记录」那一行（issue #49）：平台没有判定记录（iOS）时不显示
 * @param voiceAlerts 语音播报提醒（issue #52）：追踪提醒同时朗读出来
 * @param elderMode 长辈版开关（`SettingsRepository.uiMode` 是不是 Elder）
 */
data class MeUiState(
    val engineMode: EngineMode = EngineMode.CHELAILE,
    val preferredTransitCode: TransitCodeProvider = TransitCodeProvider.ASK_ALWAYS,
    val showTransitCodeButton: Boolean = true,
    val autoStopAfterBoarding: Boolean = true,
    /** 后台定位（「始终允许」）：true = 已开启，false = 未开启，null = 不需要显示（未开自动结束或平台不需要） */
    val backgroundLocationGranted: Boolean? = null,
    val canExportBoardingTrace: Boolean = false,
    val canTransferFavorites: Boolean = false,
    val favoritesTransferBusy: Boolean = false,
    val voiceAlerts: Boolean = false,
    val vibrationAlerts: Boolean = true,
    val canTestVibration: Boolean = false,
    val canTestArrival: Boolean = false,
    val vibrationTestResult: String? = null,
    val alertThresholdStops: Int = 2,
    val reminders: List<ScheduledReminder> = emptyList(),
    val remindersSubtitle: String? = null,
    val isLiveNotificationEnabled: Boolean = true,
    val showOnLockScreen: Boolean = true,
    val liveUpdate: LiveUpdateStatus? = null,
    /** 只在系统那边确实断了（没权限 / 渠道被降级）时才有值，正常时 null */
    val notificationHealth: NotificationHealth? = null,
    val versionName: String? = null,
    val elderMode: Boolean = false,
    val railMode: RailMode = RailMode.Horizontal,
    val themeMode: ThemeMode = ThemeMode.System,
    val cityChangeMode: CityChangeMode = CityChangeMode.Ask,
    val userMessages: List<UserMessage> = emptyList()
)

/** 「我的」页的事件。`Open*` 由 Route 处理（导航），其余交给 [MeViewModel.onEvent] */
sealed interface MeEvent {
    data class SetEngineMode(val mode: EngineMode) : MeEvent
    data class SetTransitCode(val provider: TransitCodeProvider) : MeEvent
    data class SetShowTransitCodeButton(val visible: Boolean) : MeEvent
    data class SetAutoStopAfterBoarding(val enabled: Boolean) : MeEvent
    /** 申请「始终允许」后台定位权限（issue #49） */
    data object RequestBackgroundLocation : MeEvent
    /** 把判上车的判定记录交给系统分享面板（issue #49） */
    data object ExportBoardingTrace : MeEvent
    data object ExportFavorites : MeEvent
    data object ImportFavorites : MeEvent
    data class SetVoiceAlerts(val enabled: Boolean) : MeEvent
    data class SetVibrationAlerts(val enabled: Boolean) : MeEvent
    data object TestVibration : MeEvent
    data object TestArrivalAlert : MeEvent
    data object DismissVibrationTest : MeEvent
    data class SetAlertThresholdStops(val stops: Int) : MeEvent
    data class ToggleReminder(val id: String, val enabled: Boolean) : MeEvent
    data class SetLiveNotificationEnabled(val enabled: Boolean) : MeEvent
    data class SetShowOnLockScreen(val enabled: Boolean) : MeEvent
    data object OpenLiveUpdateSettings : MeEvent

    /** 到站强提醒被系统降级 / 关掉时，那一行的「去开启」 */
    data object OpenAlertChannelSettings : MeEvent
    data class SetElderMode(val enabled: Boolean) : MeEvent
    data class SetRailMode(val mode: RailMode) : MeEvent
    data class SetThemeMode(val mode: ThemeMode) : MeEvent
    data class SetCityChangeMode(val mode: CityChangeMode) : MeEvent
    data class MessageShown(val id: Long) : MeEvent

    data class OpenReminder(val id: String) : MeEvent
    data object OpenAbout : MeEvent
    data object OpenThemeSettings : MeEvent
}

/** 点了「导出上车判定记录」但还没有记录（或分享面板打不开）时的提示 */
const val NO_BOARDING_TRACE_MESSAGE = "还没有判定记录：开着「上车后自动结束追踪」追一次车之后再导出"

/** 到站前提醒能选的站数（和通知上「切换」按钮的 1 → 2 → 3 一致） */
val ALERT_THRESHOLD_CHOICES: List<Int> = listOf(1, 2, 3)

class MeViewModel(
    private val settings: SettingsRepository,
    private val reminderRepository: IScheduledReminderRepository,
    private val notices: MeNotices,
    private val liveUpdateAccess: LiveUpdateAccess? = null,
    private val versionName: String? = null,
    private val notificationHealthAccess: NotificationHealthAccess? = null,
    private val boardingTrace: BoardingTraceLog? = null,
    private val locationSource: LocationSource? = null,
    private val statusDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val vibrationTestResult = MutableStateFlow<String?>(null)

    private var statusRefresh: Job? = null
    private val liveUpdateStatus = MutableStateFlow<LiveUpdateStatus?>(null)

    private val polledHealth = MutableStateFlow<NotificationHealth?>(null)
    private val nativeHealth = notificationHealthAccess?.changes
    private val notificationHealthStatus: StateFlow<NotificationHealth?> = nativeHealth ?: polledHealth

    private val backgroundLocationGranted = MutableStateFlow<Boolean?>(null)

    /** 三个平台状态的合流：`combine` 的 5 参数重载已经用满，先把这几个并成一个 */
    private val platformStatus = combine(
        liveUpdateStatus,
        notificationHealthStatus,
        backgroundLocationGranted
    ) { live, health, bgLoc -> PlatformStatus(live, health, bgLoc) }

    private val settingsPart = combine(
        settings.engineMode,
        settings.preferredTransitCodeProvider,
        settings.alertThresholdStops,
        settings.isLiveNotificationEnabled,
        settings.showOnLockScreen
    ) { engine, code, stops, live, lock -> SettingsPart(engine, code, stops, live, lock) }
        .combine(settings.showTransitCodeButton) { part, visible -> part.copy(showTransitCodeButton = visible) }
        .combine(settings.autoStopAfterBoarding) { part, enabled -> part.copy(autoStopAfterBoarding = enabled) }
        .combine(settings.voiceAlerts) { part, enabled -> part.copy(voiceAlerts = enabled) }
        .combine(settings.vibrationAlerts) { part, enabled -> part.copy(vibrationAlerts = enabled) }
        .combine(settings.uiMode) { part, mode -> part.copy(elderMode = mode == UiMode.Elder) }
        .combine(settings.railMode) { part, mode -> part.copy(railMode = mode) }
        .combine(settings.themeMode) { part, mode -> part.copy(themeMode = mode) }
        .combine(settings.cityChangeMode) { part, mode -> part.copy(cityChangeMode = mode) }

    val uiState: StateFlow<MeUiState> = combine(
        settingsPart,
        reminderRepository.reminders,
        platformStatus,
        notices.messages
    ) { part, reminders, platform, messages ->
        buildState(part, reminders, platform, messages)
    }.combine(vibrationTestResult) { state, result -> state.copy(vibrationTestResult = result) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildState(
            SettingsPart(
                settings.engineMode.value,
                settings.preferredTransitCodeProvider.value,
                settings.alertThresholdStops.value,
                settings.isLiveNotificationEnabled.value,
                settings.showOnLockScreen.value,
                settings.showTransitCodeButton.value,
                settings.uiMode.value == UiMode.Elder,
                settings.railMode.value,
                settings.autoStopAfterBoarding.value,
                settings.themeMode.value,
                settings.voiceAlerts.value,
                settings.vibrationAlerts.value,
                settings.cityChangeMode.value
            ),
            reminderRepository.reminders.value,
            PlatformStatus(liveUpdateStatus.value, notificationHealthStatus.value, backgroundLocationGranted.value),
            notices.messages.value
        )
    )

    fun onFavoritesTransferMessage(message: String) = notices.post(message)

    fun onEvent(event: MeEvent) {
        when (event) {
            MeEvent.ExportFavorites, MeEvent.ImportFavorites -> Unit // 文件选择由 Route 的平台桥处理
            is MeEvent.SetEngineMode -> settings.setEngineMode(event.mode)
            is MeEvent.SetTransitCode -> viewModelScope.launch {
                settings.setPreferredTransitCodeProvider(event.provider)
            }
            is MeEvent.SetAlertThresholdStops -> settings.setAlertThresholdStops(event.stops)
            is MeEvent.SetShowTransitCodeButton -> settings.setShowTransitCodeButton(event.visible)
            is MeEvent.SetAutoStopAfterBoarding -> settings.setAutoStopAfterBoarding(event.enabled)
            MeEvent.ExportBoardingTrace -> viewModelScope.launch {
                if (boardingTrace?.share() != true) notices.post(NO_BOARDING_TRACE_MESSAGE)
            }
            is MeEvent.SetVoiceAlerts -> settings.setVoiceAlerts(event.enabled)
            is MeEvent.SetVibrationAlerts -> settings.setVibrationAlerts(event.enabled)
            MeEvent.TestVibration -> {
                val access = notificationHealthAccess
                if (access != null && access.supportsVibrationTest) {
                    vibrationTestResult.value = runCatching { access.testVibration() }
                        .getOrElse { "无法完成震动测试，请重新打开应用后重试。" }
                }
            }
            MeEvent.TestArrivalAlert -> {
                val access = notificationHealthAccess
                if (access != null && access.supportsArrivalTest) {
                    vibrationTestResult.value = runCatching { access.testArrivalAlert() }
                        .getOrElse { "无法完成到站测试，请重新打开应用后重试。" }
                }
            }
            MeEvent.DismissVibrationTest -> vibrationTestResult.value = null
            is MeEvent.ToggleReminder -> viewModelScope.launch {
                try {
                    reminderRepository.toggleReminder(event.id, event.enabled)
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    notices.post(error.message ?: "提醒更新失败，请重试")
                }
            }
            is MeEvent.SetLiveNotificationEnabled -> settings.setLiveNotificationEnabled(event.enabled)
            is MeEvent.SetShowOnLockScreen -> settings.setShowOnLockScreen(event.enabled)
            MeEvent.OpenLiveUpdateSettings -> liveUpdateAccess?.openSettings()
            MeEvent.OpenAlertChannelSettings -> notificationHealthAccess?.openAlertChannelSettings()
            // 第 30 步只换主题；长辈版导航和页面在第 31–34 步
            is MeEvent.SetElderMode -> settings.setUiMode(if (event.enabled) UiMode.Elder else UiMode.Standard)
            is MeEvent.SetRailMode -> settings.setRailMode(event.mode)
            is MeEvent.SetThemeMode -> settings.setThemeMode(event.mode)
            is MeEvent.SetCityChangeMode -> settings.setCityChangeMode(event.mode)
            is MeEvent.MessageShown -> notices.consume(event.id)
            // 导航与系统权限申请由 Route 处理
            is MeEvent.OpenReminder,
            MeEvent.OpenAbout,
            MeEvent.OpenThemeSettings,
            MeEvent.RequestBackgroundLocation -> Unit
        }
    }

    /** 回到前台时重查实时活动通知的权限、通知体检与后台定位权限（用户可能刚从系统设置回来） */
    fun onScreenResumed() {
        // 定位对象可能属于主线程（iOS）；只在页面真正可交互时读取，不在建页时重复查询。
        backgroundLocationGranted.value = locationSource?.hasBackgroundLocationPermission()
        statusRefresh?.cancel()
        statusRefresh = viewModelScope.launch {
            // Android 的厂商接口和通知渠道会跨进程查询。不要让它们堵在绘制帧上。
            val (live, health) = withContext(statusDispatcher) {
                liveUpdateAccess?.status() to if (nativeHealth == null) notificationHealthAccess?.status() else null
            }
            liveUpdateStatus.value = live
            polledHealth.value = health
        }
    }

    private fun buildState(
        part: SettingsPart,
        reminders: List<ScheduledReminder>,
        platform: PlatformStatus,
        messages: List<UserMessage>
    ) = MeUiState(
        engineMode = part.engineMode,
        preferredTransitCode = part.transitCode,
        showTransitCodeButton = part.showTransitCodeButton,
        autoStopAfterBoarding = part.autoStopAfterBoarding,
        backgroundLocationGranted = platform.backgroundLocationGranted.takeIf { part.autoStopAfterBoarding },
        canExportBoardingTrace = boardingTrace != null,
        voiceAlerts = part.voiceAlerts,
        vibrationAlerts = part.vibrationAlerts,
        canTestVibration = notificationHealthAccess?.supportsVibrationTest == true,
        canTestArrival = notificationHealthAccess?.supportsArrivalTest == true,
        alertThresholdStops = part.alertThresholdStops,
        reminders = reminders,
        remindersSubtitle = remindersRowSubtitle(reminders),
        isLiveNotificationEnabled = part.liveNotification,
        showOnLockScreen = part.showOnLockScreen,
        elderMode = part.elderMode,
        railMode = part.railMode,
        themeMode = part.themeMode,
        cityChangeMode = part.cityChangeMode,
        // 和原来一样：实时通知关着时不显示胶囊权限
        liveUpdate = platform.liveUpdate?.takeIf { part.liveNotification },
        // 只在真有问题时出现，正常时那一行不该占地方
        notificationHealth = platform.notificationHealth?.takeIf { !it.healthy },
        versionName = versionName,
        userMessages = messages
    )

    private data class PlatformStatus(
        val liveUpdate: LiveUpdateStatus?,
        val notificationHealth: NotificationHealth?,
        val backgroundLocationGranted: Boolean?
    )

    private data class SettingsPart(
        val engineMode: EngineMode,
        val transitCode: TransitCodeProvider,
        val alertThresholdStops: Int,
        val liveNotification: Boolean,
        val showOnLockScreen: Boolean,
        val showTransitCodeButton: Boolean = true,
        val elderMode: Boolean = false,
        val railMode: RailMode = RailMode.Horizontal,
        val autoStopAfterBoarding: Boolean = true,
        val themeMode: ThemeMode = ThemeMode.System,
        val voiceAlerts: Boolean = false,
        val vibrationAlerts: Boolean = true,
        val cityChangeMode: CityChangeMode = CityChangeMode.Ask
    )
}
