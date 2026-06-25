package com.buzzkill.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App language override. Backed by SharedPreferences (read synchronously in
 * Activity.attachBaseContext) and mirrored into a StateFlow so the UI re-localizes
 * instantly — no Activity recreate.
 */
object LanguageStore {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val CHINESE = "zh"

    val options = listOf(SYSTEM, ENGLISH, CHINESE)

    private const val PREFS = "buzzkill_lang"
    private const val KEY = "app_language"

    private val flow = MutableStateFlow(SYSTEM)
    private var loaded = false

    val language: StateFlow<String> = flow

    fun ensureLoaded(context: Context) {
        if (loaded) return
        flow.value = read(context)
        loaded = true
    }

    /** Synchronous read for attachBaseContext (before any flow could resolve). */
    fun get(context: Context): String = read(context)

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, language).apply()
        flow.value = language
    }

    private fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM
}
