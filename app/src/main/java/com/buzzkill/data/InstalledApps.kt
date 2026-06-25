package com.buzzkill.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/** A user-installed (or notifying) app shown in the rule's app picker. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

object InstalledApps {

    /**
     * Returns launchable + notifying apps sorted by label. Excludes ourselves.
     * Loaded off the main thread by callers.
     */
    fun load(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
        return installed
            .asSequence()
            .filter { it.packageName != context.packageName }
            // Keep apps the user can see: launchable, or non-system, or updated-system.
            .filter { info ->
                pm.getLaunchIntentForPackage(info.packageName) != null ||
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                    (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            .map { info ->
                AppInfo(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
