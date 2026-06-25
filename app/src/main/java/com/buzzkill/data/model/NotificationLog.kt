package com.buzzkill.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One recorded notification the listener observed, with what the engine did to it. */
@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val time: Long,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    /** Whether any rule matched this notification. */
    val matched: Boolean,
    /** Comma-separated rule ids that fired (empty if none). */
    val firedRuleIds: String,
    /** What happened: none / modified / discarded / dismissed / snoozed. */
    val outcome: String,
) {
    companion object {
        const val OUTCOME_NONE = "none"
        const val OUTCOME_MODIFIED = "modified"
        const val OUTCOME_DISCARDED = "discarded"
        const val OUTCOME_DISMISSED = "dismissed"
        const val OUTCOME_SNOOZED = "snoozed"
    }
}
