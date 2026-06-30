package com.iccyuan.hush.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.amap.api.fence.GeoFence
import com.amap.api.fence.GeoFenceClient
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.DPoint
import com.iccyuan.hush.data.HolidayProvider
import com.iccyuan.hush.data.model.Condition
import com.iccyuan.hush.data.model.DayType
import com.iccyuan.hush.data.model.GapOp
import com.iccyuan.hush.data.model.Rule
import com.iccyuan.hush.data.model.Trigger
import com.iccyuan.hush.util.Logger
import java.util.Calendar

/**
 * 用高德 [GeoFenceClient] 注册/同步规则用到的地理围栏。进出由系统级围栏监控（不轮询、省电），
 * 通过广播回调把"在/不在"写入 [GeofenceState]，规则条件再从中读取——不依赖 Google Play 服务。
 *
 * 按需省电：只为「此刻其时间/节假日条件成立」的规则注册围栏。例如规则是「9:00 且 在公司」，
 * 不到 9:00 就不会注册围栏、也不会触发任何定位；到窗口边界时由 [AlarmManager] 唤醒重新评估。
 */
object GeofenceManager {

    private const val ACTION_FENCE = "com.iccyuan.hush.action.GEOFENCE"
    private const val ACTION_RESYNC = "com.iccyuan.hush.action.GEOFENCE_RESYNC"

    private var client: GeoFenceClient? = null
    private var receiver: BroadcastReceiver? = null
    private var registeredKeys: Set<String> = emptySet()
    private var lastRules: List<Rule> = emptyList()

    /** 穿越围栏（进入/离开）那一刻的回调，由 [HushListenerService] 设置以触发位置事件规则。 */
    @Volatile
    var crossingListener: ((key: String, entered: Boolean) -> Unit)? = null

    private data class FenceSpec(val lat: Double, val lng: Double, val radius: Int, val key: String)

    /** 缓存最新规则并按当前时间重新评估需要注册的围栏。 */
    @Synchronized
    fun sync(context: Context, rules: List<Rule>) {
        lastRules = rules
        reevaluate(context.applicationContext)
    }

    /** 由窗口边界闹钟唤醒：用缓存的规则按「现在」重新评估围栏。 */
    @Synchronized
    private fun resync(app: Context) = reevaluate(app)

    private fun reevaluate(app: Context) {
        // 条件用到的位置（受时间/节假日门控——不到点就不注册、不耗电定位）。
        val condRules = lastRules.filter { rule ->
            rule.conditions.any { it is Condition.LocationCondition && (it.latitude != 0.0 || it.longitude != 0.0) }
        }
        // 触发器用到的位置（围栏本身即触发条件，始终注册）。
        val trigRules = lastRules.filter { rule ->
            rule.triggers.any { it is Trigger.LocationTrigger && (it.latitude != 0.0 || it.longitude != 0.0) }
        }
        if (condRules.isEmpty() && trigRules.isEmpty()) {
            teardown(app)
            registeredKeys = emptySet()
            GeofenceState.reset()
            return
        }

        val now = TimeCtx.now()
        // 条件围栏：仅「此刻时间/节假日条件成立」的规则才注册。
        val condFences = condRules
            .filter { ruleTimeEligible(it, now) }
            .flatMap { it.conditions.filterIsInstance<Condition.LocationCondition>() }
            .filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .map { FenceSpec(it.latitude, it.longitude, it.radiusMeters, it.fenceKey()) }
        // 触发器围栏：始终注册。
        val trigFences = trigRules
            .flatMap { it.triggers.filterIsInstance<Trigger.LocationTrigger>() }
            .filter { it.latitude != 0.0 || it.longitude != 0.0 }
            .map { FenceSpec(it.latitude, it.longitude, it.radiusMeters, it.fenceKey()) }
        val fences = (condFences + trigFences).distinctBy { it.key }
        val keys = fences.map { it.key }.toSet()

        val c = ensureSetup(app)
        if (c != null && keys != registeredKeys) {
            registeredKeys = keys
            runCatching {
                c.removeGeoFence()
                fences.forEach { f ->
                    c.addGeoFence(DPoint(f.lat, f.lng), f.radius.toFloat(), f.key)
                }
                // 不再监控的围栏，清掉其「在内」缓存，避免残留误判。
                GeofenceState.retainOnly(keys)
            }.onFailure { Logger.w("geofence sync failed: ${it.message}") }
        }

        // 仅条件围栏受时间门控、需在边界重评；触发器围栏长期有效。
        scheduleNextBoundary(app, condRules, now)
    }

    /**
     * 规则此刻是否「值得」注册围栏（省电门控）。把规则的条件表达式按从左到右求值，其中位置与
     * 其它设备状态乐观地视为「成立」，只用当前时间/节假日做判断：若即便如此表达式也不成立，说明
     * 此刻规则不可能命中，便无需耗电定位。
     */
    private fun ruleTimeEligible(rule: Rule, now: TimeCtx): Boolean {
        val conds = rule.conditions
        if (conds.isEmpty()) return true
        fun pred(c: Condition): Boolean = when (c) {
            is Condition.TimeCondition -> inTimeWindow(c, now)
            is Condition.HolidayCondition -> c.dayTypes.contains(now.dayType)
            else -> true // 位置与其它设备状态：无法廉价判断，乐观视为成立。
        }
        var result = pred(conds[0])
        for (i in 1 until conds.size) {
            val op = rule.conditionJoins.getOrNull(i - 1) ?: GapOp.AND
            val v = pred(conds[i])
            result = if (op == GapOp.AND) result && v else result || v
        }
        return result
    }

    private fun inTimeWindow(c: Condition.TimeCondition, now: TimeCtx): Boolean {
        val minute = now.minuteOfDay
        val day = now.isoDay
        return if (c.startMinute <= c.endMinute) {
            minute in c.startMinute until c.endMinute && c.days.contains(day)
        } else {
            if (minute >= c.startMinute) c.days.contains(day)
            else minute < c.endMinute && c.days.contains(if (day == 1) 7 else day - 1)
        }
    }

    /** 在下一个可能改变「资格」的时刻（最近的窗口起止边界，或次日零点）唤醒重新评估。 */
    private fun scheduleNextBoundary(app: Context, located: List<Rule>, now: TimeCtx) {
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = resyncIntent(app)

        val boundaries = sortedSetOf(0) // 零点：日期类型/节假日可能变化。
        located.forEach { r ->
            r.conditions.filterIsInstance<Condition.TimeCondition>().forEach { t ->
                boundaries.add(t.startMinute.coerceIn(0, 1439))
                boundaries.add(t.endMinute.coerceIn(0, 1439))
            }
        }
        // 没有时间条件、也没有节假日条件 → 围栏长期有效，无需定时重评。
        val hasGating = located.any { r ->
            r.conditions.any { it is Condition.TimeCondition || it is Condition.HolidayCondition }
        }
        if (!hasGating) {
            am.cancel(pi)
            return
        }
        val deltaMin = boundaries
            .map { (it - now.minuteOfDay + 1440) % 1440 }
            .map { if (it == 0) 1440 else it }
            .minOrNull() ?: return
        val triggerAt = SystemClock.elapsedRealtime() + deltaMin * 60_000L
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }.onFailure { Logger.w("geofence alarm failed: ${it.message}") }
    }

    private fun resyncIntent(app: Context): PendingIntent {
        val i = Intent(ACTION_RESYNC).setPackage(app.packageName)
        return PendingIntent.getBroadcast(
            app, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureSetup(app: Context): GeoFenceClient? {
        client?.let { return it }
        return runCatching {
            // 高德 SDK 隐私合规：必须先声明已展示隐私政策且用户同意，否则定位/围栏不工作。
            AMapLocationClient.updatePrivacyShow(app, true, true)
            AMapLocationClient.updatePrivacyAgree(app, true)

            val c = GeoFenceClient(app)
            c.setActivateAction(
                GeoFenceClient.GEOFENCE_IN or GeoFenceClient.GEOFENCE_OUT or GeoFenceClient.GEOFENCE_STAYED
            )
            c.createPendingIntent(ACTION_FENCE)

            val r = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (intent.action) {
                        ACTION_RESYNC -> resync(ctx.applicationContext)
                        ACTION_FENCE -> {
                            val b = intent.extras ?: return
                            val key = b.getString(GeoFence.BUNDLE_KEY_CUSTOMID) ?: return
                            when (b.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS)) {
                                // 「进入那一刻」用 STATUS_IN；STATUS_STAYED 仅维持在内状态，不再当作进入事件。
                                GeoFence.STATUS_IN -> {
                                    GeofenceState.enter(key)
                                    crossingListener?.invoke(key, true)
                                }
                                GeoFence.STATUS_STAYED -> GeofenceState.enter(key)
                                GeoFence.STATUS_OUT -> {
                                    GeofenceState.exit(key)
                                    crossingListener?.invoke(key, false)
                                }
                            }
                        }
                    }
                }
            }
            val filter = IntentFilter().apply { addAction(ACTION_FENCE); addAction(ACTION_RESYNC) }
            ContextCompat.registerReceiver(app, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiver = r
            client = c
            c
        }.onFailure { Logger.w("geofence setup failed: ${it.message}") }.getOrNull()
    }

    private fun teardown(app: Context) {
        runCatching { client?.removeGeoFence() }
        receiver?.let { runCatching { app.unregisterReceiver(it) } }
        (app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(resyncIntent(app))
        receiver = null
        client = null
    }

    /** 当前的时间上下文（与 RuleEngine 同一套判断口径）。 */
    private class TimeCtx(val minuteOfDay: Int, val isoDay: Int, val dayType: DayType) {
        companion object {
            fun now(): TimeCtx {
                val c = Calendar.getInstance()
                val iso = ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
                val minute = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
                val type = HolidayProvider.dayType(
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), iso,
                )
                return TimeCtx(minute, iso, type)
            }
        }
    }
}
