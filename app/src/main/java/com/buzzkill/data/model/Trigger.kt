package com.buzzkill.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A trigger decides whether an incoming notification is a candidate for a rule.
 * A rule's [Rule.triggerLogic] combines multiple triggers with ALL/ANY semantics.
 */
@Serializable
sealed class Trigger {
    abstract val id: String

    /** Human readable one-line description for the editor list. */
    abstract fun summary(): String

    /** Match text in a notification field, optionally capturing regex groups. */
    @Serializable
    @SerialName("text")
    data class TextTrigger(
        override val id: String,
        val field: NotificationField = NotificationField.ANY,
        val mode: MatchMode = MatchMode.CONTAINS,
        val query: String = "",
        val caseSensitive: Boolean = false,
        val negate: Boolean = false,
    ) : Trigger() {
        override fun summary(): String {
            val not = if (negate) "NOT " else ""
            return "$not${field.label} ${mode.label} \"$query\""
        }
    }

    /** Match based on whether the notification is ongoing (e.g. music, downloads). */
    @Serializable
    @SerialName("ongoing")
    data class OngoingTrigger(
        override val id: String,
        val mustBeOngoing: Boolean = false,
    ) : Trigger() {
        override fun summary(): String =
            if (mustBeOngoing) "Notification is ongoing" else "Notification is dismissible"
    }

    /** Match when the notification carries an inline reply action (chats). */
    @Serializable
    @SerialName("hasReply")
    data class HasReplyTrigger(
        override val id: String,
        val mustHaveReply: Boolean = true,
    ) : Trigger() {
        override fun summary(): String =
            if (mustHaveReply) "Has an inline reply action" else "Has no reply action"
    }
}
