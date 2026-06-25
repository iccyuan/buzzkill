package com.buzzkill

import android.app.Application
import com.buzzkill.data.HolidayProvider
import com.buzzkill.service.ChannelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BuzzKillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure base channels exist before the first notification arrives.
        ChannelManager(this).ensureBaseChannels()

        // Load holidays, and refresh from the network at most once a week (best effort).
        HolidayProvider.ensureLoaded(this)
        val weekMs = 7L * 24 * 3600 * 1000
        if (System.currentTimeMillis() - HolidayProvider.lastUpdated(this) > weekMs) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { HolidayProvider.refresh(this@BuzzKillApp) }
            }
        }
    }
}
