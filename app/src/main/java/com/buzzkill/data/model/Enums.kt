package com.buzzkill.data.model

import kotlinx.serialization.Serializable

/** Which part of a notification a trigger or action targets. */
@Serializable
enum class NotificationField(val label: String) {
    TITLE("Title"),
    TEXT("Text"),
    BIG_TEXT("Expanded text"),
    SUB_TEXT("Sub text"),
    INFO_TEXT("Info text"),
    TICKER("Ticker"),
    APP_NAME("App name"),
    ANY("Any field");
}

/** How a [TextTrigger] / [ReplaceTextAction] query is compared against text. */
@Serializable
enum class MatchMode(val label: String) {
    CONTAINS("contains"),
    EQUALS("equals"),
    STARTS_WITH("starts with"),
    ENDS_WITH("ends with"),
    REGEX("matches regex"),
    WILDCARD("matches wildcard");
}

/** Whether ALL or ANY triggers/conditions must hold for a rule to fire. */
@Serializable
enum class LogicMode(val label: String) {
    ALL("Match all"),
    ANY("Match any");
}

/** Notification importance levels mapped onto NotificationManager constants. */
@Serializable
enum class Importance(val label: String) {
    MIN("Min — no sound, collapsed"),
    LOW("Low — no sound"),
    DEFAULT("Default"),
    HIGH("High — peek"),
    URGENT("Urgent — peek + sound");
}

/** Vibration intensity presets for [SoundVibrationAction]. */
@Serializable
enum class VibrationPreset(val label: String, val pattern: LongArray) {
    NONE("None", longArrayOf(0)),
    SHORT("Short tick", longArrayOf(0, 40)),
    NORMAL("Normal", longArrayOf(0, 250)),
    DOUBLE("Double buzz", longArrayOf(0, 120, 100, 120)),
    LONG("Long", longArrayOf(0, 600)),
    HEARTBEAT("Heartbeat", longArrayOf(0, 100, 80, 100, 80, 300));
}

/** HTTP verb for [WebhookAction]. */
@Serializable
enum class HttpMethod { GET, POST, PUT }

/**
 * Classification of a calendar day for the holiday condition. Determined by the
 * bundled Chinese statutory-holiday calendar plus weekday/weekend fallback.
 */
@Serializable
enum class DayType {
    /** A statutory day off (法定节假日). */
    LEGAL_HOLIDAY,
    /** A weekend day that the calendar declares a working day (调休上班). */
    MAKEUP_WORKDAY,
    /** An ordinary Saturday/Sunday with no override. */
    WEEKEND,
    /** An ordinary Monday–Friday working day. */
    WORKDAY,
}
