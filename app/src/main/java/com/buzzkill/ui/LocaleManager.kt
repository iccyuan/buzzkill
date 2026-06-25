package com.buzzkill.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.buzzkill.data.LanguageStore
import java.util.Locale

/** Wraps a base context with the user-selected locale (or the system locale). */
object LocaleManager {

    fun wrap(base: Context): Context {
        val locale = when (LanguageStore.get(base)) {
            LanguageStore.ENGLISH -> Locale.ENGLISH
            LanguageStore.CHINESE -> Locale.SIMPLIFIED_CHINESE
            else -> Resources.getSystem().configuration.locales[0]
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
