package com.buzzkill

import android.app.Application
import com.buzzkill.service.ChannelManager

class BuzzKillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure base channels exist before the first notification arrives.
        ChannelManager(this).ensureBaseChannels()
    }
}
