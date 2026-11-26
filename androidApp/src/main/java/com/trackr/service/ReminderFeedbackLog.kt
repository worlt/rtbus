package com.trackr.service

import android.content.Context
import android.media.AudioManager
import com.trackr.core.model.TrackingSessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bounded, local diagnostic outcomes. No route names, vehicle IDs or coordinates. */
internal object ReminderFeedbackLog {
    private const val STORE = "reminder_feedback_v4"
    private fun prefs(context: Context) = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
    private fun time() = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(Date())

    @Synchronized
    fun record(context: Context, event: String, posted: Boolean, result: ReminderVibration.Result,
        state: TrackingSessionState? = null) {
        runCatching {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val line = "${time()} $event 通知=$posted 震动=$result 铃声模式=${audio?.ringerMode}" +
                (state?.let { " 剩余站=${it.stopsRemaining} ETA=${it.etaSeconds} 已停靠=${it.isBusArrivedAtStop} 已上车=${it.isOnboard}" } ?: "")
            val store = prefs(context)
            val entries = (store.getString("events", "").orEmpty().lines().filter { it.isNotBlank() } + line).takeLast(6)
            store.edit().putString("events", entries.joinToString("\n")).apply()
        }
    }

    fun tracking(context: Context, state: TrackingSessionState?, completed: Boolean = false) {
        runCatching {
            val text = if (completed) "${time()} 本次追踪已自动结束" else state?.let {
                "${time()} 剩余站=${it.stopsRemaining} ETA=${it.etaSeconds} 已停靠=${it.isBusArrivedAtStop} " +
                    "临近已提醒=${it.hasTriggeredApproachingAlert} 到站已提醒=${it.hasTriggeredArrivedAlert} 已上车=${it.isOnboard}"
            } ?: return
            prefs(context).edit().putString("tracking", text).apply()
        }
    }

    fun report(context: Context): String = runCatching {
        val store = prefs(context)
        "最近真实提醒（最多6条，不含测试；REQUESTED仅代表震动接口已调用，通知=true仅代表已提交发送）：\n" +
            store.getString("events", "暂无真实提醒记录") + "\n\n最近追踪状态：\n" +
            store.getString("tracking", "暂无追踪记录")
    }.getOrDefault("最近提醒记录暂时无法读取")
}
