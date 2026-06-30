package com.iccyuan.hush.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import com.iccyuan.hush.util.Logger

/**
 * 监听器看门狗：部分 OEM（ColorOS/MIUI 等）的省电策略会杀掉通知监听进程且不再自动恢复，
 * 表现为「通知服务总是自动断开」。这里用一个**自我续期**的精确闹钟周期性把进程唤醒：
 * 拉起前台保活服务，并在「已授权但未连接」时请求系统重新绑定监听器，尽快自动恢复。
 *
 * 注意：这是尽力而为的补救。根因在系统侧，仍建议用户把本应用加入电池优化白名单 / 允许自启动。
 */
object ListenerWatchdog {

    private const val ACTION = "com.iccyuan.hush.action.LISTENER_WATCHDOG"
    private const val INTERVAL_MS = 15 * 60 * 1000L // 15 分钟

    /** 安排下一次看门狗唤醒（每次触发后自我续期，从而长期存活）。 */
    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        }.onFailure { Logger.w("watchdog schedule failed: ${it.message}") }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val i = Intent(context, ListenerWatchdogReceiver::class.java).setAction(ACTION)
        return PendingIntent.getBroadcast(
            context, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** 看门狗闹钟回调：重启前台保活、按需重新绑定监听器，并续期下一次。 */
class ListenerWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching { KeepAliveService.start(context) }
        runCatching { HushListenerService.requestRebindIfNeeded(context) }
        ListenerWatchdog.schedule(context)
    }
}
