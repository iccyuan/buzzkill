package com.buzzkill.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The system rebinds an enabled NotificationListenerService automatically after a
 * reboot, but this receiver gives us a hook to rebuild channels / warm state so the
 * first post-boot notification is handled without delay.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ChannelManager(context).ensureBaseChannels()
    }
}
