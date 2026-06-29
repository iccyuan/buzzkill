package com.iccyuan.hush.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 零点闹钟接收器：刷新保活常驻通知里的「今日日期类型」，并预约下一个零点。
 * 通过 [NotificationManagerCompat.notify] 直接更新通知（无需重启前台服务）；同时尽力
 * 再拉起一次服务以确保保活仍在。
 */
class DayRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH) return
        KeepAliveService.refresh(context)
        KeepAliveService.scheduleNextMidnight(context)
        KeepAliveService.start(context) // 尽力重新拉起前台服务（后台受限时静默失败）
    }

    companion object {
        const val ACTION_REFRESH = "com.iccyuan.hush.action.REFRESH_DAY"
    }
}
