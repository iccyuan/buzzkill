package com.buzzkill.service

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.buzzkill.data.model.NotificationField

/** Extracts editable text fields from a posted notification. */
object NotificationFields {

    fun extract(sbn: StatusBarNotification): MutableMap<NotificationField, String> {
        val extras = sbn.notification.extras
        val map = mutableMapOf<NotificationField, String>()
        fun put(field: NotificationField, value: CharSequence?) {
            value?.toString()?.takeIf { it.isNotEmpty() }?.let { map[field] = it }
        }
        put(NotificationField.TITLE, extras.getCharSequence(Notification.EXTRA_TITLE))
        put(NotificationField.TEXT, extras.getCharSequence(Notification.EXTRA_TEXT))
        put(NotificationField.BIG_TEXT, extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        put(NotificationField.SUB_TEXT, extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        put(NotificationField.INFO_TEXT, extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        put(NotificationField.TICKER, sbn.notification.tickerText)
        return map
    }

    fun hasReplyAction(sbn: StatusBarNotification): Boolean =
        sbn.notification.actions?.any { it.remoteInputs?.isNotEmpty() == true } == true

    /** Resolves a user-facing app label, falling back to the package name. */
    fun appLabel(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }
}
