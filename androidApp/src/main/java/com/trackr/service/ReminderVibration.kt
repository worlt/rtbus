package com.trackr.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/** Explicit transit reminders may vibrate in ordinary silent mode when opted in.
 * Never bypass DND, the app switch, or the user's notification/channel settings.
 */
internal object ReminderVibration {
    fun vibrate(
        context: Context,
        channelId: String,
        enabled: Boolean,
        effect: VibrationEffect,
        notificationPosted: Boolean
    ) {
        if (!enabled) return
        try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = manager.getNotificationChannel(channelId) ?: return
            if (channel.importance == NotificationManager.IMPORTANCE_NONE || !channel.shouldVibrate()) return
            // An alarm usage would otherwise be allowed by some DND policies. Respect all active DND modes.
            if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val silent = audio.ringerMode == AudioManager.RINGER_MODE_SILENT
            // Outside silent mode a posted notification already supplies channel vibration.
            if (notificationPosted && !silent) return
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() != true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val usage = if (silent) VibrationAttributes.USAGE_ALARM else VibrationAttributes.USAGE_NOTIFICATION
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(usage))
            } else {
                val attributes = AudioAttributes.Builder()
                    .setUsage(if (silent) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, attributes)
            }
        } catch (e: Exception) {
            Log.w("ReminderVibration", "Unable to vibrate transit reminder", e)
        }
    }
}
