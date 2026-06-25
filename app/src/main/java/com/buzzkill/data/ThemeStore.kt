package com.buzzkill.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Appearance (light/dark/system) preference. Backed by SharedPreferences and mirrored
 * into a StateFlow so the theme switches instantly (no Activity recreate needed).
 */
object ThemeStore {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    val options = listOf(SYSTEM, LIGHT, DARK)

    private const val PREFS = "buzzkill_theme"
    private const val KEY = "theme_mode"

    private val flow = MutableStateFlow(SYSTEM)
    private var loaded = false

    val mode: StateFlow<String> = flow

    fun ensureLoaded(context: Context) {
        if (loaded) return
        flow.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM
        loaded = true
    }

    fun set(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode).apply()
        flow.value = mode
    }
}
