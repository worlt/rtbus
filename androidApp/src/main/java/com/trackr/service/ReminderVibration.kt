package com.trackr.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat

/** Explicit transit guidance, respecting the app switch, notification channels and DND. */
internal object ReminderVibration {
    enum class Result(val explanation: String) {
        REQUESTED("已发送到站引导震动请求。请以手机是否实际震动为准。"),
        NOTIFICATION_HANDLED("本次由系统通知渠道处理震动，应用未重复请求。"),
        SWITCH_OFF("震动提醒已关闭，请先打开应用内的震动开关。"),
        NOTIFICATIONS_OFF("系统未允许本应用发送通知，请先允许通知。"),
        CHANNEL_MISSING("到站提醒渠道尚未创建，请重新打开应用后重试。"),
        CHANNEL_OFF("系统已禁用到站提醒渠道，请到系统提醒设置中开启。"),
        CHANNEL_VIBRATION_OFF("系统已关闭到站提醒渠道的震动，请到系统提醒设置中开启。"),
        DND("系统报告勿扰已开启；应用未发送额外震动请求。"),
        NO_VIBRATOR("系统没有提供可用的震动器。"),
        UNAVAILABLE("暂时无法读取系统震动状态，请重试。"),
        FAILED("调用系统震动接口失败，请保留下面的诊断信息。")
    }

    fun vibrate(
        context: Context,
        channelId: String,
        enabled: Boolean,
        effect: VibrationEffect,
        notificationPosted: Boolean,
        deliveryChannelId: String? = null
    ): Result {
        fun finish(result: Result): Result {
            Log.i("ReminderVibration", "channel=" + channelId + " result=" + result.name)
            return result
        }
        if (!enabled) return finish(Result.SWITCH_OFF)
        try {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return finish(Result.NOTIFICATIONS_OFF)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return finish(Result.UNAVAILABLE)
            val channel = manager.getNotificationChannel(channelId) ?: return finish(Result.CHANNEL_MISSING)
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) return finish(Result.CHANNEL_OFF)
            if (!channel.shouldVibrate()) return finish(Result.CHANNEL_VIBRATION_OFF)
            if (deliveryChannelId != null) {
                val delivery = manager.getNotificationChannel(deliveryChannelId)
                    ?: return finish(Result.CHANNEL_MISSING)
                if (delivery.importance == NotificationManager.IMPORTANCE_NONE) return finish(Result.CHANNEL_OFF)
            }
            if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return finish(Result.DND)
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return finish(Result.UNAVAILABLE)
            // Some vendor silent modes leave the public ringer mode NORMAL but mute the ring stream.
            val audibleRinger = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                audio.getStreamVolume(AudioManager.STREAM_RING) > 0
            if (notificationPosted && (audibleRinger || audio.ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
                return finish(Result.NOTIFICATION_HANDLED)
            }
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() != true) return finish(Result.NO_VIBRATOR)
            // Transit arrival is navigation guidance. Keep the original audio usage as well as the
            // mapped vibration usage: createForUsage alone drops the original audio usage on API 33+.
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attributes)
            return finish(Result.REQUESTED)
        } catch (error: Exception) {
            Log.w("ReminderVibration", "Unable to vibrate transit reminder", error)
            return finish(Result.FAILED)
        }
    }

    /** A foreground test uses exactly the same explicit feedback path as an actual reminder. */
    fun test(context: Context, channelId: String, enabled: Boolean): String {
        val result = vibrate(context, channelId, enabled,
            VibrationEffect.createWaveform(longArrayOf(0, 280, 120, 280, 120, 400), -1),
            notificationPosted = false)
        return report(context, channelId, enabled, result)
    }

    /** Formats an existing outcome without requesting a second, misleading test vibration. */
    fun report(context: Context, channelId: String, enabled: Boolean, result: Result): String {
        val details = runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId)
            val mode = when (audio.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "静音"
                AudioManager.RINGER_MODE_VIBRATE -> "振动"
                AudioManager.RINGER_MODE_NORMAL -> "响铃"
                else -> "未知"
            }
            val dnd = when (manager.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_ALL -> "关闭"
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "仅优先事项"
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> "仅闹钟"
                NotificationManager.INTERRUPTION_FILTER_NONE -> "全部静音"
                else -> "无法确认"
            }
            listOf(
                "反馈实现：导航引导兼容版 v2",
                "到站反馈：确认到站与主动引导 v4",
                "设备：" + Build.MANUFACTURER + " " + Build.MODEL,
                "Android：" + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT,
                "铃声模式：" + mode + "；铃声音量：" + audio.getStreamVolume(AudioManager.STREAM_RING),
                "勿扰：" + dnd,
                "应用震动：" + enabled + "；通知允许：" + NotificationManagerCompat.from(context).areNotificationsEnabled(),
                "渠道重要级别：" + channel?.importance + "；渠道震动：" + channel?.shouldVibrate(),
                "到站展示渠道重要级别：" + manager.getNotificationChannel(ArrivalAlertNotifier.CHANNEL_ID_ARRIVAL_GUIDANCE)?.importance,
                "请求结果：" + result.name
            ).joinToString("\n")
        }.getOrElse { "诊断信息读取失败：" + it.javaClass.simpleName }
        return result.explanation + "\n\n" + details + "\n\n" + ReminderFeedbackLog.report(context)
    }
}
