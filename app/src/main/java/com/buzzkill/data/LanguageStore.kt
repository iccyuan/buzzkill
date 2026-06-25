package com.buzzkill.data

import android.content.Context

/**
 * Synchronous (SharedPreferences-backed) store for the app language override.
 * Must be synchronous because it is read in Activity.attachBaseContext, before
 * any coroutine/DataStore could resolve.
 */
object LanguageStore {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val CHINESE = "zh"

    private const val PREFS = "buzzkill_lang"
    private const val KEY = "app_language"

    val options = listOf(SYSTEM, ENGLISH, CHINESE)

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, language).apply()
    }
}
