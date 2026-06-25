package com.buzzkill.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.buzzkill.data.LanguageStore
import java.util.Locale

/** Wraps a base context with the user-selected locale (for Activity.attachBaseContext). */
object LocaleManager {

    fun localeFor(language: String): Locale = when (language) {
        LanguageStore.ENGLISH -> Locale.ENGLISH
        LanguageStore.CHINESE -> Locale.SIMPLIFIED_CHINESE
        else -> Resources.getSystem().configuration.locales[0]
    }

    fun wrap(base: Context): Context {
        val locale = localeFor(LanguageStore.get(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

/**
 * Re-localizes the whole composition for [language] WITHOUT recreating the Activity:
 * provides a locale-configured Context/Configuration so every stringResource re-reads
 * in the chosen language on the next recomposition.
 */
@Composable
fun ProvideAppLocale(language: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val locale = LocaleManager.localeFor(language)
    val localizedContext = remember(language, baseContext) {
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        baseContext.createConfigurationContext(config)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
    ) {
        content()
    }
}
