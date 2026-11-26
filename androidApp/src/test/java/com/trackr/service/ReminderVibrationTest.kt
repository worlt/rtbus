package com.trackr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderVibrationTest {
    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private lateinit var audio: AudioManager
    private lateinit var vibrator: Vibrator
    private val channelId = "test_reminder_vibration"

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        shadowOf(vibrator).setHasVibrator(true)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        manager.createNotificationChannel(NotificationChannel(channelId, "Test", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
        })
        audio.setStreamVolume(AudioManager.STREAM_RING, 5, 0)
        vibrator.cancel()
    }

    private fun trigger(posted: Boolean = true, enabled: Boolean = true) = ReminderVibration.vibrate(
        context, channelId, enabled, VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE), posted)

    @Test fun silentPostedReminderRetainsNavigationAudioAttributes() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        trigger()
        assertTrue(shadowOf(vibrator).isVibrating)
        assertNavigationVibrationAttributes(vibrator)
    }

    @Test fun vendorSilentWithNormalReportedModeAndZeroRingVolumeStillRequestsVibration() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        audio.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        trigger()
        assertTrue(shadowOf(vibrator).isVibrating)
    }

    @Test fun normalAndVibratePostedNotificationsDoNotVibrateTwice() {
        for (mode in listOf(AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE)) {
            audio.ringerMode = mode
            trigger()
            assertFalse(shadowOf(vibrator).isVibrating)
        }
    }

    @Test fun liveUpdateFallbackAlsoUsesNavigationGuidanceWhenNotSilent() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        trigger(posted = false)
        assertTrue(shadowOf(vibrator).isVibrating)
        assertNavigationVibrationAttributes(vibrator)
    }

    @Test fun everyActiveDndModeBlocksSilentManualVibration() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        for (filter in listOf(NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY, NotificationManager.INTERRUPTION_FILTER_ALARMS)) {
            manager.setInterruptionFilter(filter)
            trigger()
            trigger(posted = false)
            assertFalse(shadowOf(vibrator).isVibrating)
        }
    }

    @Test fun disabledAppSwitchBlocksSilentManualVibration() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        trigger(enabled = false)
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun disabledNotificationsBlockSilentManualVibration() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        shadowOf(manager).setNotificationsEnabled(false)
        trigger()
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun channelVibrationDisabledByUserIsRespected() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        val channel = manager.getNotificationChannel(channelId)
        channel.enableVibration(false)
        // Robolectric retains this channel instance, simulating a user change in system settings.
        trigger()
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun blockedChannelIsRespected() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        val channel = manager.getNotificationChannel(channelId)
        channel.importance = NotificationManager.IMPORTANCE_NONE
        trigger()
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun testReportDistinguishesRequestedFeedbackFromPhysicalVibration() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        val report = ReminderVibration.test(context, channelId, enabled = true)
        assertTrue(shadowOf(vibrator).isVibrating)
        assertTrue(report.contains("导航引导兼容版 v2"))
        assertTrue(report.contains("到站反馈：确认到站与主动引导 v4"))
        assertTrue(report.contains("铃声模式：静音"))
        assertTrue(report.contains("请求结果：REQUESTED"))
        assertTrue(report.contains("以手机是否实际震动为准"))
    }

    @Test fun testReportNamesTheBlockingSettingWithoutVibrating() {
        assertTrue(ReminderVibration.test(context, channelId, enabled = false).contains("请求结果：SWITCH_OFF"))
        assertFalse(shadowOf(vibrator).isVibrating)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        assertTrue(ReminderVibration.test(context, channelId, enabled = true).contains("请求结果：DND"))
        assertFalse(shadowOf(vibrator).isVibrating)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        manager.getNotificationChannel(channelId).enableVibration(false)
        assertTrue(ReminderVibration.test(context, channelId, enabled = true).contains("请求结果：CHANNEL_VIBRATION_OFF"))
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun missingVibratorIsSafe() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        shadowOf(vibrator).setHasVibrator(false)
        trigger()
        assertFalse(shadowOf(vibrator).isVibrating)
    }
}
