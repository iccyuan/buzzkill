package com.buzzkill.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Helpers for checking and requesting notification-listener access. */
object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val component = ComponentName(context, BuzzKillListenerService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
