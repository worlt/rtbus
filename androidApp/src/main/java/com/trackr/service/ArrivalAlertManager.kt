package com.trackr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trackr.R
import com.trackr.core.domain.math.RouteProjectionMath
import com.trackr.core.domain.tracking.TrackingEngine
import com.trackr.core.model.TrackingSessionState
import com.trackr.service.vendor.LiveUpdatePromotion
import com.trackr.service.vendor.XiaomiFocusAdapter

/**
 * 负责管理高优先级到站/进站预警通知渠道及震动声音提醒
 *
 * 由 Koin 提供单例（原来的 object `ArrivalAlertManager` 已删，只有文件名还沿用旧名）。
 *
 * @param liveProgressNotifier 读「实时通知」开关，与常驻通知共用同一个兜底值
 * @param showOnLockScreenSetting 读「锁屏显示」开关（D3），和常驻通知同一份设置；读不到按开
 */
class ArrivalAlertNotifier(
    private val liveProgressNotifier: LiveProgressNotifier,
    private val showOnLockScreenSetting: () -> Boolean = { true },
    private val vibrationEnabledProvider: () -> Boolean = { true }
) {

    /**
     * 强提醒统一走这里建通知。
     *
     * 锁屏可见性跟「我的 → 锁屏显示」走，判定和常驻通知一致：开着用 PUBLIC（锁屏上看得到线路和站名），
     * 关着用 PRIVATE。**不能不管**：`Notification.visibility` 默认就是 PRIVATE，用户把系统锁屏设成
     * 「隐藏敏感内容」时，这五条提醒在锁屏上会被抹成「内容已隐藏」——震动照响，但人掏出手机看到一条空通知，
     * 而那一刻正是「车到站了，快上车」。
     *
     * @param timeoutMillis 这条提醒在通知栏里留多久就自己消失（见 [TIMEOUT_ARRIVED_MILLIS] 那一组）：
     *   `autoCancel` 只在用户点的时候生效，不点会一直挂着，#28 就是被这个坑到的
     */
    private fun alertNotification(
        context: Context,
        title: String,
        content: String,
        category: String,
        requestCode: Int,
        timeoutMillis: Long
    ): Notification {
        val lockScreenOn = runCatching { showOnLockScreenSetting() }.getOrDefault(true)
        return NotificationCompat.Builder(context, alertChannelId)
            .setSmallIcon(R.drawable.ic_stat_trackr)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(category)
            .setAutoCancel(true)
            // 提醒自己过期（#28）：autoCancel 只在用户点一下时生效，不点就一直挂在通知栏和锁屏上。
            // 到站只有一次机会，该弹出来的时候要弹；但弹过之后不该变成一条要手动清理的常驻消息，
            // 因为同一趟车的进度常驻通知本来就写着「到站了」。
            .setTimeoutAfter(timeoutMillis)
            .setDefaults(if (vibrationEnabled()) NotificationCompat.DEFAULT_ALL else NotificationCompat.DEFAULT_SOUND)
            .setVisibility(
                if (lockScreenOn) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE
            )
            .setContentIntent(createMainActivityPendingIntent(context, requestCode))
            .build()
    }

    fun triggerTrackingExpired(context: Context, state: TrackingSessionState) {
        ensureNotificationChannel(context)
        val notification = alertNotification(
            context = context,
            title = "追踪已停止",
            content = "${state.lineName} 追踪已超过 ${maxTrackingDurationText()}，已停止追踪。",
            category = NotificationCompat.CATEGORY_STATUS,
            requestCode = NOTIFICATION_ID_EXPIRED,
            timeoutMillis = TIMEOUT_STATUS_MILLIS
        )
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPIRED, notification) }
    }

    fun triggerTrackingInterrupted(context: Context, state: TrackingSessionState) {
        ensureNotificationChannel(context)
        val notification = alertNotification(
            context = context,
            title = "追踪已中断",
            content = "${state.lineName} 追踪未能自动接续，请打开应用重试。",
            category = NotificationCompat.CATEGORY_STATUS,
            requestCode = NOTIFICATION_ID_INTERRUPTED,
            timeoutMillis = TIMEOUT_STATUS_MILLIS
        )
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INTERRUPTED, notification) }
    }

    /**
     * 定位判断用户已经上车，这次追踪自动结束（issue #32，「上车后自动结束追踪」开关）。
     *
     * 必须说一句：常驻通知和「追踪中」会一起消失，不说的话用户会以为 App 挂了。
     * 顺手撤掉「车已到站」那条——上了车再看它没有意义。
     */
    fun triggerEndedAfterBoarding(context: Context, state: TrackingSessionState, sessionId: String = "") {
        ensureNotificationChannel(context)
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_ARRIVED, sessionId))
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_APPROACHING, sessionId))
        val notification = alertNotification(
            context = context,
            title = "已上车，追踪结束",
            content = "定位判断你已坐上 ${state.lineName}，本次追踪已自动结束。还想看车的话重新追踪一次就好。",
            category = NotificationCompat.CATEGORY_STATUS,
            requestCode = NOTIFICATION_ID_BOARDED,
            timeoutMillis = TIMEOUT_STATUS_MILLIS
        )
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BOARDED, notification) }
    }

    /** 最长追踪时长写成文案（「3 小时」），常量改了文案自动跟着变 */
    private fun maxTrackingDurationText(): String {
        val hours = TrackingEngine.MAX_TRACKING_DURATION_MILLIS / 3_600_000L
        return if (hours > 0) "$hours 小时" else "${TrackingEngine.MAX_TRACKING_DURATION_MILLIS / 60_000L} 分钟"
    }

    /**
     * 确保高优先级预警渠道创建完毕
     */
    internal val alertChannelId: String
        get() = if (vibrationEnabled()) CHANNEL_ID_ALERT else CHANNEL_ID_ALERT_NO_VIBRATION

    private fun vibrationEnabled(): Boolean = runCatching { vibrationEnabledProvider() }.getOrDefault(true)

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = notificationManager.getNotificationChannel(alertChannelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    alertChannelId,
                    if (vibrationEnabled()) CHANNEL_NAME_ALERT else "公交到站提醒（不震动）",
                    notificationManager.getNotificationChannel(CHANNEL_ID_ALERT)?.importance
                        ?: NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "当追踪的公交车即将到站或已经进站时发送高优先级浮动与震动强提醒"
                    if (vibrationEnabled()) vibrationPattern = APPROACHING_VIBRATION_PATTERN
                    enableVibration(vibrationEnabled())
                    notificationManager.getNotificationChannel(CHANNEL_ID_ALERT)?.let { base ->
                        setSound(base.sound, base.audioAttributes)
                    }
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * 当车辆距离目标候车站 stopsRemaining <= state.alertThresholdStops 时触发强提醒通知及三段式节奏震动
     */
    fun triggerApproachingAlert(context: Context, state: TrackingSessionState, sessionId: String = "") {
        ensureNotificationChannel(context)
        val notificationId = alertNotificationId(NOTIFICATION_ID_APPROACHING, sessionId)
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_ARRIVED, sessionId))

        var posted = false
        if (shouldPostStandaloneNotification(context, state)) {
            val etaText = if (state.etaSeconds > 0) {
                "（${RouteProjectionMath.formatEtaText(state.etaSeconds)}）"
            } else {
                ""
            }
            val title = "【即将到站】${state.lineName}"
            // 只追一辆车（issue #52）：多半是坐在车上等下车站，写清是哪辆
            val content = if (state.isPinnedBus) {
                "${state.pinnedPlate.ifBlank { "所追车辆" }} 距 ${state.targetStopName} 还有 ${state.stopsRemaining} 站$etaText，请准备${if (state.isOnboard) "下车" else "上车"}！"
            } else {
                "距 ${state.targetStopName} 还有 ${state.stopsRemaining} 站$etaText，请准备上车/下车！"
            }

            val notification = alertNotification(
                context = context,
                title = title,
                content = content,
                category = NotificationCompat.CATEGORY_REMINDER,
                requestCode = notificationId,
                timeoutMillis = TIMEOUT_APPROACHING_MILLIS
            )

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
                posted = areNotificationsEnabled(context)
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot post approaching alert: missing POST_NOTIFICATIONS permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post approaching alert notification", e)
            }
        }
        // 静音走显式提醒震动；其他模式的独立通知仍由渠道震动，避免重复。
        performWaveformVibration(context, APPROACHING_VIBRATION_PATTERN, posted, "APPROACHING")
    }

    /**
     * 到站时使用与临近提醒 / 即时测试相同的有限分段震动，避免到站单独采用另一种效果。
     */
    fun triggerArrivedAlert(
        context: Context,
        state: TrackingSessionState,
        forceStandalone: Boolean = false,
        sessionId: String = ""
    ) {
        ensureNotificationChannel(context)
        val notificationId = alertNotificationId(NOTIFICATION_ID_ARRIVED, sessionId)
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_APPROACHING, sessionId))

        var posted = false
        if (forceStandalone || shouldPostStandaloneNotification(context, state)) {
            val title = "【车辆已到站】${state.lineName}"
            val content = if (state.isPinnedBus) {
                "${state.pinnedPlate.ifBlank { state.lineName }} 已到达 ${state.targetStopName}！请${if (state.isOnboard) "下车" else "上车"}。"
            } else {
                "${state.lineName} 已到达 ${state.targetStopName}！请注意上车/下车。"
            }

            val notification = alertNotification(
                context = context,
                title = title,
                content = content,
                category = NotificationCompat.CATEGORY_REMINDER,
                requestCode = notificationId,
                timeoutMillis = TIMEOUT_ARRIVED_MILLIS
            )

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
                posted = areNotificationsEnabled(context)
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot post arrived alert: missing POST_NOTIFICATIONS permission", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post arrived alert notification", e)
            }
        }
        // 静音走显式提醒震动；其他模式的独立通知仍由渠道震动，避免重复。
        performWaveformVibration(context, APPROACHING_VIBRATION_PATTERN, posted, "ARRIVED")
    }

    /**
     * 当换乘行程第一程到达换乘站时触发换乘提醒及节拍震动，通知用户准备下车换乘
     */
    fun triggerTransferAlert(
        context: Context,
        transferStationName: String,
        nextLineName: String,
        sessionId: String = ""
    ) {
        ensureNotificationChannel(context)
        val notificationId = alertNotificationId(NOTIFICATION_ID_TRANSFER, sessionId)
        // 换乘站即本程下车站：只保留换乘提醒，清掉同一站的到站 / 临近通知，避免连发两条
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_ARRIVED, sessionId))
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_APPROACHING, sessionId))

        val title = "【换乘提醒】到达 $transferStationName"
        val content = "已到达换乘站 $transferStationName，已自动切换追踪下一程：$nextLineName"

        val notification = alertNotification(
            context = context,
            title = title,
            content = content,
            category = NotificationCompat.CATEGORY_REMINDER,
            requestCode = notificationId,
            timeoutMillis = TIMEOUT_TRANSFER_MILLIS
        )

        var posted = false
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            posted = areNotificationsEnabled(context)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot post transfer alert: missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post transfer alert notification", e)
        }
        // 静音走显式提醒震动；其他模式的独立通知仍由渠道震动，避免重复。
        performWaveformVibration(context, TRANSFER_VIBRATION_PATTERN, posted, "TRANSFER")
    }

    /**
     * 没赶上这班车：车已经开走，人还在原地等。
     *
     * 不提醒的话，只在后台把追踪悄悄换成下一班，用户会以为自己还在等原来那班，等到发现时已经晚了。
     * 顺手撤掉「车已到站」那条——车都走了，再挂着只会误导。
     */
    fun triggerMissedBusAlert(context: Context, state: TrackingSessionState, sessionId: String = "") {
        ensureNotificationChannel(context)
        val notificationId = alertNotificationId(NOTIFICATION_ID_MISSED, sessionId)
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_ARRIVED, sessionId))
        cancelNotification(context, alertNotificationId(NOTIFICATION_ID_APPROACHING, sessionId))

        val stop = state.targetStopName.ifBlank { "上车站" }
        val next = when {
            state.stopsRemaining == 0 -> "下一班已经到 $stop 了，快跑"
            state.etaSeconds > 0 -> "下一班约 ${RouteProjectionMath.formatEtaText(state.etaSeconds)}到"
            state.stopsRemaining > 0 -> "下一班还有 ${state.stopsRemaining} 站"
            else -> "暂时还没有开往 $stop 的车"
        }

        val notification = alertNotification(
            context = context,
            title = "错过 ${state.lineName} 了",
            content = "$next，仍在追踪 $stop",
            category = NotificationCompat.CATEGORY_REMINDER,
            requestCode = notificationId,
            timeoutMillis = TIMEOUT_MISSED_MILLIS
        )

        var posted = false
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            posted = areNotificationsEnabled(context)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot post missed-bus alert: missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post missed-bus alert notification", e)
        }
        // 静音走显式提醒震动；其他模式的独立通知仍由渠道震动，避免重复。
        performWaveformVibration(context, MISSED_VIBRATION_PATTERN, posted, "MISSED")
    }

    /**
     * 实时活动（流体云 / 焦点通知 / 状态栏胶囊）已经在通知栏常驻展示同一趟车的进度与到站状态时，
     * 不再重复发一条内容相同的强提醒通知，仅保留震动提示；
     * 若当前系统不支持提升通知或用户未授权实时活动/焦点通知，则回退为发送独立强提醒通知
     */
    internal var liveUpdateActiveChecker: (Context) -> Boolean = { context ->
        when {
            XiaomiFocusAdapter.isXiaomiRom() -> XiaomiFocusAdapter.canShowFocus(context)
            LiveUpdatePromotion.isPromotionSupported() -> LiveUpdatePromotion.canPromote(context)
            else -> false
        }
    }

    internal fun resetLiveUpdateActiveChecker() {
        liveUpdateActiveChecker = { context ->
            when {
                XiaomiFocusAdapter.isXiaomiRom() -> XiaomiFocusAdapter.canShowFocus(context)
                LiveUpdatePromotion.isPromotionSupported() -> LiveUpdatePromotion.canPromote(context)
                else -> false
            }
        }
    }

    /**
     * 是否开启常驻实时通知，以可注入 provider 的形式提供，
     * 避免直接耦合全局单例，同时保证读取失败时安全降级
     */
    internal var liveNotificationEnabledProvider: () -> Boolean = {
        liveProgressNotifier.readLiveNotificationEnabled()
    }

    internal fun resetLiveNotificationEnabledProvider() {
        liveNotificationEnabledProvider = {
            liveProgressNotifier.readLiveNotificationEnabled()
        }
    }

    private fun shouldPostStandaloneNotification(context: Context, state: TrackingSessionState): Boolean {
        if (!state.isActive) return true
        if (!liveNotificationEnabledProvider()) return true
        return !liveUpdateActiveChecker(context)
    }

    private fun areNotificationsEnabled(context: Context): Boolean =
        runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true)

    private fun createMainActivityPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setClassName(context.packageName, "com.trackr.MainActivity")
        launchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun performWaveformVibration(context: Context, pattern: LongArray, posted: Boolean, event: String) {
        val result = ReminderVibration.vibrate(context, CHANNEL_ID_ALERT, vibrationEnabled(),
            VibrationEffect.createWaveform(pattern, -1), notificationPosted = posted)
        Log.i(TAG, "event=$event notificationPosted=$posted vibration=$result")
    }

    companion object {
        private const val TAG = "ArrivalAlertManager"

        const val CHANNEL_ID_ALERT = "bus_arrival_alert"
        const val CHANNEL_ID_ALERT_NO_VIBRATION = "bus_arrival_alert_no_vibration"
        private const val CHANNEL_NAME_ALERT = "公交到站与进站强提醒"

        const val NOTIFICATION_ID_APPROACHING = 10002
        const val NOTIFICATION_ID_ARRIVED = 10003
        const val NOTIFICATION_ID_TRANSFER = 10004
        // 追踪已停止 / 已中断用独立的 id，否则会顶掉同一时刻的换乘提醒
        const val NOTIFICATION_ID_EXPIRED = 10005
        const val NOTIFICATION_ID_INTERRUPTED = 10006
        const val NOTIFICATION_ID_MISSED = 10007

        /** 定位判断已上车、追踪自动结束（issue #32） */
        const val NOTIFICATION_ID_BOARDED = 10008

        private val APPROACHING_VIBRATION_PATTERN = longArrayOf(0, 280, 120, 280, 120, 400)
        private val TRANSFER_VIBRATION_PATTERN = longArrayOf(0, 300, 100, 300, 100, 300)
        private val MISSED_VIBRATION_PATTERN = longArrayOf(0, 350, 150, 350)

        /**
         * 提醒在通知栏里留多久就自己消失（#28）。
         *
         * `setAutoCancel(true)` 只是「点一下才消失」，用户不点就一直在——追踪结束后尤其明显：
         * 常驻进度通知已经撤了，这条「车辆已到站」还挂在锁屏上，得手动点掉。
         * 时间按「人还来不来得及看」定：临近 3 分钟（马上会被到站那条顶掉）、到站 5 分钟、
         * 换乘 5 分钟、错过 10 分钟（要看完下一班还有几分钟）、已停止 / 已中断 15 分钟。
         */
        private const val TIMEOUT_APPROACHING_MILLIS = 3 * 60_000L
        private const val TIMEOUT_ARRIVED_MILLIS = 5 * 60_000L
        private const val TIMEOUT_TRANSFER_MILLIS = 5 * 60_000L
        private const val TIMEOUT_MISSED_MILLIS = 10 * 60_000L
        private const val TIMEOUT_STATUS_MILLIS = 15 * 60_000L

        /**
         * 同时追多项时每项的到站 / 临近提醒各用一组 id：不同项的提醒不互相顶掉、也不互相取消。
         * [sessionId] 为空时还是原来的固定 id（旧调用 / 没有会话的场景）。
         */
        fun alertNotificationId(base: Int, sessionId: String): Int =
            if (sessionId.isEmpty()) base else base * 4096 + (sessionId.hashCode() and 0xFFF)

        /** 撤掉一条提醒（不关心它到底在不在） */
        fun cancelNotification(context: Context, notificationId: Int) {
            try {
                NotificationManagerCompat.from(context).cancel(notificationId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cancel notification $notificationId", e)
            }
        }

        /**
         * 撤掉一份追踪留下的提醒：到站 / 即将到站 / 换乘 / 错过。
         *
         * 追踪结束了这些提醒就没有主人了，不该继续挂在通知栏和锁屏上等用户手动点掉（#28）。
         * 「已停止」「已中断」是解释性的状态消息，不挂在会话上，只能靠 [TIMEOUT_STATUS_MILLIS] 自己过期；
         * 旧版本发出去的通知没带 sessionId，算不出 id，也只能等它自己过期。
         * [keepArrived] 仅用于本轮刚产生最终到站提醒的自然完成；保留其五分钟超时，
         * 防止会话收尾马上取消通知及其尚未播放的震动。用户主动结束时保持默认清理。
         */
        fun cancelSessionAlerts(context: Context, sessionId: String, keepArrived: Boolean = false) {
            if (sessionId.isEmpty()) return
            listOf(
                NOTIFICATION_ID_APPROACHING,
                NOTIFICATION_ID_ARRIVED,
                NOTIFICATION_ID_TRANSFER,
                NOTIFICATION_ID_MISSED
            ).filterNot { keepArrived && it == NOTIFICATION_ID_ARRIVED }
                .forEach { cancelNotification(context, alertNotificationId(it, sessionId)) }
        }
    }
}
