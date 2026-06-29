package com.iccyuan.hush.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iccyuan.hush.R
import com.iccyuan.hush.data.HolidayProvider
import com.iccyuan.hush.data.model.DayType
import com.iccyuan.hush.ui.MainActivity
import java.util.Calendar

/**
 * 前台保活服务：用一条常驻通知把进程提升到「前台服务」优先级，显著降低被 OEM 省电策略
 * 杀死的概率——监听器与本服务同进程，因而一并存活。
 *
 * 这条常驻通知同时**显示并可一键切换「今日休息 / 今日工作」**（覆盖节假日判定）。按设计
 * 它始终存在、不提供应用内关闭开关。每天零点由闹钟刷新，使日期类型保持当天。
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REST -> toggleOverride(HolidayProvider.OVERRIDE_REST)
            ACTION_WORK -> toggleOverride(HolidayProvider.OVERRIDE_WORK)
        }
        startForegroundCompat(buildNotification(this))
        scheduleNextMidnight(this)
        return START_STICKY
    }

    /** 再次点击当前已激活的那个即清除覆盖（与应用内「今日」卡片一致）。 */
    private fun toggleOverride(target: String) {
        val current = HolidayProvider.todayOverride(this)
        HolidayProvider.setTodayOverride(this, if (current == target) null else target)
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }
    }

    companion object {
        const val NOTIF_ID = 7001
        const val ACTION_REST = "com.iccyuan.hush.action.TODAY_REST"
        const val ACTION_WORK = "com.iccyuan.hush.action.TODAY_WORK"

        /** 启动（或刷新）保活前台服务。后台启动受限时静默失败，下次打开应用会再尝试。 */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context, Intent(context, KeepAliveService::class.java),
                )
            }
        }

        /** 仅刷新常驻通知内容（不重启服务）。供零点闹钟在进程存活时更新日期类型。 */
        fun refresh(context: Context) {
            runCatching {
                ChannelManager(context).ensureBaseChannels()
                NotificationManagerCompat.from(context).notify(NOTIF_ID, buildNotification(context))
            }
        }

        /** 在下一个零点（稍过一点）唤醒刷新常驻通知的日期类型。 */
        fun scheduleNextMidnight(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val next = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 20)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val pi = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, DayRefreshReceiver::class.java).setAction(DayRefreshReceiver.ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi) }
        }

        fun buildNotification(context: Context): Notification {
            // 标题显示今天的状态；正文解释「自动（跟随日历）/ 手动」的含义并指引操作。
            val title: String
            val hint: String
            when (HolidayProvider.todayOverride(context)) {
                HolidayProvider.OVERRIDE_REST -> {
                    title = context.getString(R.string.keepalive_today_rest)
                    hint = context.getString(R.string.keepalive_hint_manual)
                }
                HolidayProvider.OVERRIDE_WORK -> {
                    title = context.getString(R.string.keepalive_today_work)
                    hint = context.getString(R.string.keepalive_hint_manual)
                }
                else -> {
                    title = context.getString(
                        R.string.keepalive_today_auto, context.getString(todayDayTypeRes()),
                    )
                    hint = context.getString(R.string.keepalive_hint_auto)
                }
            }
            val openApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, ChannelManager.KEEPALIVE_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_hush)
                .setContentTitle(title)
                .setContentText(hint)
                .setStyle(NotificationCompat.BigTextStyle().bigText(hint))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .addAction(0, context.getString(R.string.today_rest), serviceAction(context, ACTION_REST))
                .addAction(0, context.getString(R.string.today_work), serviceAction(context, ACTION_WORK))
                .build()
        }

        /** 今天真实判定的日期类型对应的字符串资源（与节假日条件同一套判断逻辑）。 */
        private fun todayDayTypeRes(): Int {
            val c = Calendar.getInstance()
            val iso = ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            val type = HolidayProvider.dayType(
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), iso,
            )
            return when (type) {
                DayType.LEGAL_HOLIDAY -> R.string.daytype_legal_holiday
                DayType.MAKEUP_WORKDAY -> R.string.daytype_makeup_workday
                DayType.WEEKEND -> R.string.daytype_weekend
                DayType.WORKDAY -> R.string.daytype_workday
            }
        }

        private fun serviceAction(context: Context, action: String): PendingIntent {
            val intent = Intent(context, KeepAliveService::class.java).setAction(action)
            return PendingIntent.getService(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
