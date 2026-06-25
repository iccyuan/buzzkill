package com.buzzkill.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import com.buzzkill.R
import com.buzzkill.data.model.NotificationField
import com.buzzkill.engine.Decision

/**
 * Turns a [Decision] into concrete notification operations. Because a
 * NotificationListenerService cannot edit another app's notification in place, a
 * modified notification is rebuilt under our own channel (so we control alerting),
 * the original is cancelled, and the copy is reposted — preserving icon, intent,
 * actions and styling. The original app's name is shown via the substitute-name
 * extra so the rebuilt notification still looks native.
 */
class NotificationModifier(
    private val context: Context,
    private val channels: ChannelManager,
) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    /** Stable notify id derived from the original key so updates replace cleanly. */
    private fun notifyId(sbn: StatusBarNotification): Int = sbn.key.hashCode()

    fun repost(sbn: StatusBarNotification, decision: Decision, appName: String) {
        val original = sbn.notification
        val extras = original.extras

        val title = decision.fieldEdits[NotificationField.TITLE]
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = decision.fieldEdits[NotificationField.TEXT]
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = decision.fieldEdits[NotificationField.BIG_TEXT]
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = decision.fieldEdits[NotificationField.SUB_TEXT]
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val channelId = channels.channelFor(decision.importance, decision.sound, decision.bypassDnd)

        val builder = Notification.Builder(context, channelId).apply {
            setSmallIcon(original.smallIcon ?: Icon.createWithResource(context, R.drawable.ic_stat_buzzkill))
            title?.let { setContentTitle(it) }
            text?.let { setContentText(it) }
            subText?.let { setSubText(it) }
            if (!bigText.isNullOrEmpty()) {
                setStyle(Notification.BigTextStyle().bigText(bigText))
            }
            original.getLargeIcon()?.let { setLargeIcon(it) }
            original.contentIntent?.let { setContentIntent(it) }
            original.deleteIntent?.let { setDeleteIntent(it) }
            setAutoCancel((original.flags and Notification.FLAG_AUTO_CANCEL) != 0)
            setWhen(original.`when`)
            setShowWhen(true)
            if (original.color != 0) setColor(original.color)
            original.group?.let { setGroup(it) }
            // Preserve interactive actions (reply, mark-as-read, etc.).
            original.actions?.forEach { addAction(it) }
            // Make the rebuilt notification still read as the source app. DND bypass
            // and importance are governed by the chosen channel, not the builder.
            addExtras(android.os.Bundle().apply {
                // Public key value of the (hidden) EXTRA_SUBSTITUTE_APP_NAME constant.
                putString("android.substName", appName)
            })
        }

        // Post our rebuilt copy. The service cancels the source via
        // cancelNotification(key) — only the listener can dismiss another app's post.
        nm.notify(notifyId(sbn), builder.build())
    }

    /** Remove a copy we previously reposted (e.g. when its source is dismissed). */
    fun cancelReposted(sbn: StatusBarNotification) {
        nm.cancel(notifyId(sbn))
    }
}
