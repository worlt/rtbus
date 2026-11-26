package com.trackr.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import com.trackr.TrackrApp
import com.trackr.core.domain.tracking.TrackingEngine
import com.trackr.core.model.TrackingSessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ArrivalAlertManagerTest {

    private lateinit var app: TrackrApp
    private lateinit var notificationManager: NotificationManager
    private var liveEnabled = true

    /** App 启动时 Koin 建好的实例（原来经 object 转发） */
    private val alertNotifier: ArrivalAlertNotifier get() = GlobalContext.get().get()
    private val liveNotifier: LiveProgressNotifier get() = GlobalContext.get().get()

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        notificationManager = app.getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
        liveEnabled = true
        alertNotifier.liveNotificationEnabledProvider = { liveEnabled }
        alertNotifier.liveUpdateActiveChecker = { true }
    }

    @After
    fun tearDown() {
        alertNotifier.resetLiveUpdateActiveChecker()
        alertNotifier.resetLiveNotificationEnabledProvider()
    }

    @Test
    fun testTriggerApproachingAlert_skipsStandaloneNotificationWhenLiveUpdateIsActive() {
        val state = activeTrackingState(stopsRemaining = 2)

        alertNotifier.triggerApproachingAlert(app, state)

        assertNull(
            "实时活动已展示同一趟车时不应再发一条内容重复的强提醒",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING)
        )
    }

    @Test
    fun testTriggerArrivedAlert_skipsStandaloneNotificationWhenLiveUpdateIsActive() {
        val state = activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true)

        alertNotifier.triggerArrivedAlert(app, state)

        assertNull(
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        )
    }

    @Test
    fun testTriggerArrivedAlert_clearsStaleApproachingNotification() {
        alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2))
        alertNotifier.triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))

        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING))
    }

    @Test
    fun testTriggerApproachingAlert_postsNotificationWhenLiveUpdateDisabled() {
        liveEnabled = false

        alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING)
        val text = posted?.extras?.getCharSequence("android.text")?.toString().orEmpty()
        assertEquals(true, text.contains("还有 2 站"))
    }

    @Test
    fun testTriggerApproachingAlert_postsNotificationWhenSystemDoesNotSupportPromotion() {
        alertNotifier.liveUpdateActiveChecker = { false }

        alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING)
        assertNotNull("当系统不支持实时活动胶囊时，应降级发送独立强提醒", posted)
        val text = posted?.extras?.getCharSequence("android.text")?.toString().orEmpty()
        assertEquals(true, text.contains("还有 2 站"))
    }

    @Test
    fun testStandaloneAlerts_useReminderCategory() {
        liveEnabled = false

        alertNotifier.triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertNotNull(posted)
        assertEquals(android.app.Notification.CATEGORY_REMINDER, posted?.category)
    }

    @Test
    fun testTransferAlert_cancelsArrivedAndApproachingOfSameStop() {
        liveEnabled = false
        alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 1))
        alertNotifier.triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))

        alertNotifier.triggerTransferAlert(app, "珞雄路雄楚大道", "M492")

        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))
        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING))
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER))
    }

    @Test
    fun testLiveNotificationFallback_isSameForBothHelpers() {
        alertNotifier.resetLiveNotificationEnabledProvider()
        liveNotifier.resetLiveNotificationEnabledProvider()
        assertEquals(
            liveNotifier.liveNotificationEnabledProvider(),
            alertNotifier.liveNotificationEnabledProvider()
        )
    }

    @Test
    fun testTrackingExpired_usesOwnNotificationIdAndDoesNotOverwriteTransferAlert() {
        alertNotifier.triggerTransferAlert(app, "珞雄路雄楚大道", "M492")
        assertNotNull("换乘提醒应先发出来", shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER))

        alertNotifier.triggerTrackingExpired(app, activeTrackingState(stopsRemaining = 3))

        // 两条通知各占各的 id，不能互相顶掉
        assertNotNull(
            "换乘提醒不该被「追踪已停止」盖掉",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER)
        )
        assertNotNull(
            "「追踪已停止」应用自己的通知 id",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_EXPIRED)
        )
    }

    @Test
    fun testTrackingInterrupted_usesOwnNotificationIdAndDoesNotOverwriteTransferAlert() {
        alertNotifier.triggerTransferAlert(app, "珞雄路雄楚大道", "M492")

        alertNotifier.triggerTrackingInterrupted(app, activeTrackingState(stopsRemaining = 3))

        assertNotNull(
            "换乘提醒不该被「追踪已中断」盖掉",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER)
        )
        assertNotNull(
            "「追踪已中断」应用自己的通知 id",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_INTERRUPTED)
        )
    }

    @Test
    fun testEndedAfterBoarding_tellsUserAndCancelsArrivalAlert() {
        // 没有实时活动时才会发独立到站提醒；这条用例要看「撤掉到站提醒」
        alertNotifier.liveUpdateActiveChecker = { false }
        val state = activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true, lineName = "59路")
        alertNotifier.triggerArrivedAlert(app, state)
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))

        alertNotifier.triggerEndedAfterBoarding(app, state)

        // 上了车再看「车已到站」没意义，撤掉；换成一条说清楚追踪为什么结束了
        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))
        val posted = requireNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_BOARDED))
        val text = posted.extras?.getCharSequence("android.text")?.toString().orEmpty()
        assertTrue(text, text.contains("59路"))
    }

    @Test
    fun testTrackingExpired_textFollowsMaxTrackingDurationConstant() {
        alertNotifier.triggerTrackingExpired(app, activeTrackingState(stopsRemaining = 3))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_EXPIRED)
        val text = posted?.extras?.getCharSequence("android.text")?.toString().orEmpty()
        val expectedHours = TrackingEngine.MAX_TRACKING_DURATION_MILLIS / 3_600_000L
        assertTrue("文案里的时长应与常量一致，实际：$text", text.contains("$expectedHours 小时"))
    }

    @Test
    fun expiredAlightingNotificationDoesNotClaimArrivalOrThreeHoursOfTracking() {
        alertNotifier.triggerTrackingExpired(app, activeTrackingState(stopsRemaining = 3).copy(pinnedBusId = "mine"))
        val posted = requireNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_EXPIRED))
        val text = posted.extras.getCharSequence("android.text").toString()
        assertTrue(text, text.contains("未能确认到达") && !text.contains("小时"))
        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))
    }

    @Test
    fun testMissedBusAlert_clearsStaleArrivedNotificationAndSaysNextBus() {
        alertNotifier.triggerArrivedAlert(
            app,
            activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true),
            forceStandalone = true
        )
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))

        alertNotifier.triggerMissedBusAlert(app, activeTrackingState(stopsRemaining = 1).copy(etaSeconds = 480))

        assertNull(
            "车都开走了，别再挂着「车已到站」",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        )
        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_MISSED)
        assertNotNull(posted)
        val text = posted?.extras?.getCharSequence("android.text")?.toString().orEmpty()
        assertTrue("要说下一班还有多久，实际：$text", text.contains("下一班"))
        assertEquals(Notification.CATEGORY_REMINDER, posted?.category)
    }

    /**
     * #28：提醒要自己消失。
     *
     * `setAutoCancel(true)` 只是「点一下才消失」，用户不点就一直挂在通知栏和锁屏上；
     * 追踪结束后常驻通知已经撤了，这条「车辆已到站」还得手动点掉。给每条提醒配一个 timeout。
     */
    @Test
    fun testStandaloneAlerts_timeOutOnTheirOwn() {
        alertNotifier.liveUpdateActiveChecker = { false }
        fun timeoutOf(id: Int): Long {
            val posted = shadowOf(notificationManager).getNotification(id)
            assertNotNull("提醒 $id 没发出来", posted)
            return posted!!.timeoutAfter
        }

        // 每种提醒单独发一次、当场断言：后一条会撤掉前一条（到站撤临近、换乘撤到站）
        alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2))
        assertEquals(true, timeoutOf(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING) > 0)

        alertNotifier.triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))
        val arrived = timeoutOf(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        // 到站只有一次机会：留够时间给人看，但别按小时算
        assertEquals(true, arrived in 1..(10 * 60_000L))

        alertNotifier.triggerTransferAlert(app, "珞雄路雄楚大道", "M492")
        assertEquals(true, timeoutOf(ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER) > 0)

        alertNotifier.triggerMissedBusAlert(app, activeTrackingState(stopsRemaining = 1).copy(etaSeconds = 480))
        assertEquals(true, timeoutOf(ArrivalAlertNotifier.NOTIFICATION_ID_MISSED) > 0)
    }

    /** #28：追踪结束时，这一项留下的到站 / 临近 / 换乘 / 错过提醒一起撤掉 */
    @Test
    fun testCancelSessionAlerts_clearsEveryAlertOfThatSession() {
        alertNotifier.liveUpdateActiveChecker = { false }
        val mine = "session-a"
        val other = "session-b"
        val arrived = activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true)

        // 四种提醒互相会顶掉（到站撤临近、换乘撤到站…），所以一种一种验：发两份，只清自己那份
        val cases = listOf<Pair<Int, (String) -> Unit>>(
            ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING to { id ->
                alertNotifier.triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2), sessionId = id)
            },
            ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED to { id ->
                alertNotifier.triggerArrivedAlert(app, arrived, forceStandalone = true, sessionId = id)
            },
            ArrivalAlertNotifier.NOTIFICATION_ID_TRANSFER to { id ->
                alertNotifier.triggerTransferAlert(app, "换乘站", "M492", sessionId = id)
            },
            ArrivalAlertNotifier.NOTIFICATION_ID_MISSED to { id ->
                alertNotifier.triggerMissedBusAlert(app, activeTrackingState(stopsRemaining = 1), sessionId = id)
            }
        )
        cases.forEach { (base, post) ->
            post(mine)
            post(other)

            ArrivalAlertNotifier.cancelSessionAlerts(app, mine)

            assertNull(
                "追踪结束后提醒不该留着，id=$base",
                shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.alertNotificationId(base, mine))
            )
            assertNotNull(
                "别把同时追的另一项一起撤了，id=$base",
                shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.alertNotificationId(base, other))
            )
            ArrivalAlertNotifier.cancelSessionAlerts(app, other)
        }
    }

    /** 「已停止 / 已中断」是解释性的状态消息，不挂在某一份会话上，清场时不能顺手抹掉 */
    @Test
    fun testCancelSessionAlerts_keepsStatusNotifications() {
        alertNotifier.triggerTrackingExpired(app, activeTrackingState(stopsRemaining = 3))

        ArrivalAlertNotifier.cancelSessionAlerts(app, "session-a")

        assertNotNull(
            "「追踪已停止」要留到它自己过期",
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_EXPIRED)
        )
    }

    /** 旧版本发出去的通知没带 sessionId，算不出 id；给空 id 时空转，不能把固定 id 那几条误伤 */
    @Test
    fun testCancelSessionAlerts_ignoresBlankSessionId() {
        alertNotifier.liveUpdateActiveChecker = { false }
        alertNotifier.triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))

        ArrivalAlertNotifier.cancelSessionAlerts(app, "")

        assertNotNull(
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        )
    }

    /**
     * D3：强提醒的锁屏可见性跟「我的 → 锁屏显示」走。
     * 不设的话 `Notification.visibility` 默认 PRIVATE，用户把系统锁屏设成「隐藏敏感内容」时，
     * 到站提醒在锁屏上只剩「内容已隐藏」，震动照响但看不到是哪条线哪个站。
     */
    private fun alertsWithLockScreen(setting: () -> Boolean) =
        ArrivalAlertNotifier(liveNotifier, showOnLockScreenSetting = setting).apply {
            liveNotificationEnabledProvider = { true }
            liveUpdateActiveChecker = { false }
        }

    @Test
    fun testStandaloneAlert_lockScreenOn_isPublic() {
        alertsWithLockScreen { true }
            .triggerArrivedAlert(app, activeTrackingState(stopsRemaining = 0).copy(isBusArrivedAtStop = true))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertNotNull(posted)
        assertEquals(Notification.VISIBILITY_PUBLIC, posted!!.visibility)
    }

    @Test
    fun testStandaloneAlert_lockScreenOff_isPrivateWithoutPublicVersion() {
        alertsWithLockScreen { false }
            .triggerApproachingAlert(app, activeTrackingState(stopsRemaining = 2))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING)
        assertNotNull(posted)
        assertEquals(Notification.VISIBILITY_PRIVATE, posted!!.visibility)
        assertNull("关掉锁屏显示时不给公开版本，锁屏上不该出现站名", posted.publicVersion)
    }

    @Test
    fun testStandaloneAlert_lockScreenSettingThrows_fallsBackToPublic() {
        alertsWithLockScreen { error("读不到设置") }
            .triggerTrackingExpired(app, activeTrackingState(stopsRemaining = 3))

        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_EXPIRED)
        assertNotNull(posted)
        assertEquals(Notification.VISIBILITY_PUBLIC, posted!!.visibility)
    }

    @Test
    fun vibrationSwitchSelectsChannelWithoutDisablingNotifications() {
        var vibration = true
        val notifier = ArrivalAlertNotifier(liveNotifier, vibrationEnabledProvider = { vibration }).apply {
            liveNotificationEnabledProvider = { true }
            liveUpdateActiveChecker = { false }
        }
        notifier.triggerArrivedAlert(app, activeTrackingState(0))
        assertTrue(notificationManager.getNotificationChannel(notifier.alertChannelId).shouldVibrate())
        vibration = false
        notifier.triggerArrivedAlert(app, activeTrackingState(0))
        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertNotNull(posted)
        assertEquals(ArrivalAlertNotifier.CHANNEL_ID_ARRIVAL_GUIDANCE, posted.channelId)
        org.junit.Assert.assertFalse(notificationManager.getNotificationChannel(posted.channelId).shouldVibrate())
        assertEquals(0, posted.defaults and Notification.DEFAULT_VIBRATE)
        vibration = true
        notifier.triggerArrivedAlert(app, activeTrackingState(0))
        assertEquals(ArrivalAlertNotifier.CHANNEL_ID_ARRIVAL_GUIDANCE,
            shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED).channelId)
    }

    @Test
    fun liveUpdateFallbackVibrationHonorsIndependentSwitch() {
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val shadow = shadowOf(vibrator)
        shadow.setHasVibrator(true)
        var enabled = false
        val notifier = ArrivalAlertNotifier(liveNotifier, vibrationEnabledProvider = { enabled }).apply {
            liveNotificationEnabledProvider = { true }
            liveUpdateActiveChecker = { true }
        }
        vibrator.cancel()
        notifier.triggerApproachingAlert(app, activeTrackingState(2))
        org.junit.Assert.assertFalse(shadow.isVibrating)
        enabled = true
        notifier.triggerApproachingAlert(app, activeTrackingState(2))
        assertTrue(shadow.isVibrating)
    }

    @Test
    fun actualArrivalExplicitlyRequestsHapticsForEveryPublicRingerMode() {
        val audio = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        shadowOf(vibrator).setHasVibrator(true)
        val notifier = alertsWithLockScreen { true }
        for (mode in listOf(android.media.AudioManager.RINGER_MODE_NORMAL,
            android.media.AudioManager.RINGER_MODE_VIBRATE, android.media.AudioManager.RINGER_MODE_SILENT)) {
            audio.ringerMode = mode
            if (mode == android.media.AudioManager.RINGER_MODE_NORMAL)
                audio.setStreamVolume(android.media.AudioManager.STREAM_RING, 5, 0)
            vibrator.cancel()
            notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true)
            assertTrue("posted ARRIVED must request guidance haptics; mode=$mode", shadowOf(vibrator).isVibrating)
            assertNavigationVibrationAttributes(vibrator)
        }
    }

    @Test
    fun arrivalDeliveryKeepsSoundButDoesNotDoubleTheDefaultVibration() {
        val notifier = alertsWithLockScreen { true }
        notifier.ensureNotificationChannel(app)
        val canonical = notificationManager.getNotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ALERT)
        notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true)
        val delivery = notificationManager.getNotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ARRIVAL_GUIDANCE)
        assertEquals(canonical.sound, delivery.sound)
        assertEquals(canonical.importance, delivery.importance)
        org.junit.Assert.assertFalse(delivery.shouldVibrate())
        assertTrue(canonical.shouldVibrate())
        val posted = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertEquals(0, posted.defaults and Notification.DEFAULT_VIBRATE)
        assertNotNull("Android O+ notification sound is owned by the delivery channel", delivery.sound)
        // 后续确保渠道不能重置用户静音/降低重要级别的选择。
        delivery.importance = NotificationManager.IMPORTANCE_LOW
        delivery.setSound(null, null)
        notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true)
        assertEquals(NotificationManager.IMPORTANCE_LOW, delivery.importance)
        assertNull(delivery.sound)
    }

    @Test
    fun arrivalHapticsRespectBothChannelsPermissionDndAndTheAppSwitch() {
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        shadowOf(vibrator).setHasVibrator(true)
        val notifier = alertsWithLockScreen { true }
        notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true)
        val canonical = notificationManager.getNotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ALERT)
        val delivery = notificationManager.getNotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ARRIVAL_GUIDANCE)
        fun expectBlocked(expected: ReminderVibration.Result) {
            vibrator.cancel()
            assertEquals(expected, notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true))
            org.junit.Assert.assertFalse(shadowOf(vibrator).isVibrating)
        }
        delivery.importance = NotificationManager.IMPORTANCE_NONE
        expectBlocked(ReminderVibration.Result.CHANNEL_OFF)
        delivery.importance = NotificationManager.IMPORTANCE_HIGH
        canonical.enableVibration(false)
        expectBlocked(ReminderVibration.Result.CHANNEL_VIBRATION_OFF)
        canonical.enableVibration(true)
        canonical.importance = NotificationManager.IMPORTANCE_NONE
        expectBlocked(ReminderVibration.Result.CHANNEL_OFF)
        canonical.importance = NotificationManager.IMPORTANCE_HIGH
        shadowOf(notificationManager).setNotificationsEnabled(false)
        expectBlocked(ReminderVibration.Result.NOTIFICATIONS_OFF)
        shadowOf(notificationManager).setNotificationsEnabled(true)
        shadowOf(notificationManager).setNotificationPolicyAccessGranted(true)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        expectBlocked(ReminderVibration.Result.DND)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        val off = ArrivalAlertNotifier(liveNotifier, vibrationEnabledProvider = { false })
        vibrator.cancel()
        assertEquals(ReminderVibration.Result.SWITCH_OFF, off.triggerArrivedAlert(app, activeTrackingState(0), true))
        org.junit.Assert.assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test
    fun realArrivalTestHasItsOwnNotificationAndDoesNotOverwriteRealHistory() {
        val notifier = alertsWithLockScreen { true }
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        shadowOf(vibrator).setHasVibrator(true)
        notifier.triggerApproachingAlert(app, activeTrackingState(1), sessionId = "active")
        val before = ReminderFeedbackLog.report(app)
        val report = notifier.testArrival(app)
        assertTrue(report.contains("确认到站与主动引导 v4"))
        assertTrue(report.contains("请求结果：REQUESTED"))
        assertEquals(before, ReminderFeedbackLog.report(app))
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVAL_TEST))
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.alertNotificationId(
            ArrivalAlertNotifier.NOTIFICATION_ID_APPROACHING, "active")))
        assertNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))
    }

    @Test
    fun silentModePostedArrivalUsesReminderVibration() {
        val audio = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audio.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val shadow = shadowOf(vibrator)
        shadow.setHasVibrator(true)
        vibrator.cancel()
        alertsWithLockScreen { true }.triggerArrivedAlert(app, activeTrackingState(0))
        assertNotNull(shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED))
        assertTrue("静音时已发布通知也要单独请求提醒震动", shadow.isVibrating)
        assertNavigationVibrationAttributes(vibrator)
    }

    @Test
    fun silentArrivalUsesTheSameFiniteWaveformAsTheWorkingImmediateTest() {
        val audio = app.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audio.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
        val vibrator = app.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val shadow = shadowOf(vibrator)
        shadow.setHasVibrator(true)
        val notifier = alertsWithLockScreen { true }
        notifier.ensureNotificationChannel(app)
        ReminderVibration.test(app, ArrivalAlertNotifier.CHANNEL_ID_ALERT, enabled = true)
        val testedPattern = shadow.pattern.copyOf()
        vibrator.cancel()

        notifier.triggerArrivedAlert(app, activeTrackingState(0), forceStandalone = true)

        org.junit.Assert.assertArrayEquals("真实到站使用已验证的分段震动，不再单独走单次长震", testedPattern, shadow.pattern)
        assertEquals(-1, shadow.repeat)
        assertNavigationVibrationAttributes(vibrator)
    }

    @Test
    fun pinnedWaitingAndOnboardNotificationsUseCorrectAction() {
        val notifier = alertsWithLockScreen { true }
        val pinned = activeTrackingState(0).copy(pinnedBusId = "selected", isOnboard = false)
        notifier.triggerArrivedAlert(app, pinned)
        val waiting = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertTrue(waiting.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("请上车"))
        notifier.triggerArrivedAlert(app, pinned.copy(isOnboard = true))
        val onboard = shadowOf(notificationManager).getNotification(ArrivalAlertNotifier.NOTIFICATION_ID_ARRIVED)
        assertTrue(onboard.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("请下车"))
    }

    private fun activeTrackingState(stopsRemaining: Int) = TrackingSessionState(
        isActive = true,
        lineName = "788",
        endStopName = "珞雄路公交场站",
        targetStopName = "珞雄路雄楚大道",
        targetOrder = 11,
        startTrackingOrder = 10,
        currentBusOrder = 11,
        currentBusProgress = 0.4f,
        stopsRemaining = stopsRemaining,
        etaSeconds = 420
    )
}
