package com.buzzkill.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A condition gates a rule on device/context state, evaluated only after the
 * triggers already matched. All conditions on a rule must hold (AND semantics).
 */
@Serializable
sealed class Condition {
    abstract val id: String
    abstract fun summary(): String

    /**
     * Active only between [startMinute] and [endMinute] (minutes from midnight) on
     * the selected [days] (1 = Monday … 7 = Sunday, ISO). A start after the end wraps
     * past midnight.
     */
    @Serializable
    @SerialName("time")
    data class TimeCondition(
        override val id: String,
        val startMinute: Int = 22 * 60,
        val endMinute: Int = 7 * 60,
        val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    ) : Condition() {
        override fun summary(): String {
            fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)
            val dayLabel = if (days.size == 7) "every day" else days.sorted()
                .joinToString(",") { DAY_ABBR[it - 1] }
            return "${fmt(startMinute)}–${fmt(endMinute)} ($dayLabel)"
        }

        companion object {
            val DAY_ABBR = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }
    }

    /** Active only while the device is (or is not) charging. */
    @Serializable
    @SerialName("charging")
    data class ChargingCondition(
        override val id: String,
        val mustBeCharging: Boolean = true,
    ) : Condition() {
        override fun summary() = if (mustBeCharging) "While charging" else "While on battery"
    }

    /** Active only while the screen is on/off. */
    @Serializable
    @SerialName("screen")
    data class ScreenCondition(
        override val id: String,
        val mustBeOn: Boolean = false,
    ) : Condition() {
        override fun summary() = if (mustBeOn) "While screen is on" else "While screen is off"
    }

    /** Active only when battery is below/above a threshold. */
    @Serializable
    @SerialName("battery")
    data class BatteryLevelCondition(
        override val id: String,
        val percent: Int = 20,
        val whenBelow: Boolean = true,
    ) : Condition() {
        override fun summary() =
            "Battery ${if (whenBelow) "below" else "above"} $percent%"
    }

    /**
     * Rate-limit: the rule may only fire once per [seconds]. Prevents repeated
     * actions from chatty apps.
     */
    @Serializable
    @SerialName("cooldown")
    data class CooldownCondition(
        override val id: String,
        val seconds: Int = 60,
    ) : Condition() {
        override fun summary() = "At most once every ${seconds}s"
    }

    /**
     * Active only when today's [DayType] (per the bundled Chinese statutory-holiday
     * calendar) is one of [dayTypes]. Combine with [TimeCondition] (AND) to express
     * e.g. "during 09:00–18:00 on legal holidays".
     */
    @Serializable
    @SerialName("holiday")
    data class HolidayCondition(
        override val id: String,
        val dayTypes: Set<DayType> = setOf(DayType.LEGAL_HOLIDAY),
    ) : Condition() {
        override fun summary(): String =
            "Day type: " + dayTypes.joinToString("/") { it.name }
    }
}
