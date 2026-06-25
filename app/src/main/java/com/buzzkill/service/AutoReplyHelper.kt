package com.buzzkill.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification

/**
 * Sends an automatic reply by filling and firing the inline RemoteInput action a
 * messaging app attaches to its notification. No-op when the notification carries
 * no reply action.
 */
object AutoReplyHelper {

    fun reply(context: Context, sbn: StatusBarNotification, message: String): Boolean {
        if (message.isEmpty()) return false
        val action = findReplyAction(sbn.notification) ?: return false
        val remoteInputs = action.remoteInputs ?: return false

        val intent = Intent()
        val bundle = android.os.Bundle()
        for (remoteInput in remoteInputs) {
            bundle.putCharSequence(remoteInput.resultKey, message)
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

        return try {
            action.actionIntent.send(context, 0, intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        // Prefer an action that declares a free-form text RemoteInput.
        return actions.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
    }
}
