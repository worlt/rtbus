package com.trackr.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.trackr.service.ArrivalAlertNotifier
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidNotificationHealthAccessTest {
    @Test fun testPreparesChannelAndReadsCurrentAppVibrationSwitch() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        shadowOf(vibrator).setHasVibrator(true)
        shadowOf(manager).setNotificationPolicyAccessGranted(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        var enabled = false
        var prepares = 0
        val access = AndroidNotificationHealthAccess(context, { enabled }, {
            prepares++
            manager.createNotificationChannel(NotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ALERT,
                "Test", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) })
        })
        assertTrue(access.supportsVibrationTest)
        assertTrue(access.testVibration().contains("请求结果：SWITCH_OFF"))
        assertFalse(shadowOf(vibrator).isVibrating)
        enabled = true
        assertTrue(access.testVibration().contains("请求结果：REQUESTED"))
        assertTrue(shadowOf(vibrator).isVibrating)
        assertEquals(2, prepares)
    }
}
