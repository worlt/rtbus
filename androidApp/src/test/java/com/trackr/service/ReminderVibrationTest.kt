package com.trackr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.VibrationAttributes
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
        vibrator.cancel()
    }

    private fun trigger(posted: Boolean = true, enabled: Boolean = true) = ReminderVibration.vibrate(
        context, channelId, enabled, VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE), posted)

    @Test fun silentPostedReminderUsesAlarmUsage() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        trigger()
        assertTrue(shadowOf(vibrator).isVibrating)
        assertEquals(VibrationAttributes.USAGE_ALARM,
            (shadowOf(vibrator).getVibrationAttributesFromLastVibration() as VibrationAttributes).usage)
    }

    @Test fun normalAndVibratePostedNotificationsDoNotVibrateTwice() {
        for (mode in listOf(AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE)) {
            audio.ringerMode = mode
            trigger()
            assertFalse(shadowOf(vibrator).isVibrating)
        }
    }

    @Test fun liveUpdateFallbackStillUsesNotificationUsageWhenNotSilent() {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        trigger(posted = false)
        assertTrue(shadowOf(vibrator).isVibrating)
        assertEquals(VibrationAttributes.USAGE_NOTIFICATION,
            (shadowOf(vibrator).getVibrationAttributesFromLastVibration() as VibrationAttributes).usage)
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

    @Test fun missingVibratorIsSafe() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        shadowOf(vibrator).setHasVibrator(false)
        trigger()
        assertFalse(shadowOf(vibrator).isVibrating)
    }
}
