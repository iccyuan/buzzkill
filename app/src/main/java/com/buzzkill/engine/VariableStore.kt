package com.buzzkill.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-lifetime runtime state for the engine: user-defined variables
 * ([SetVariableAction][com.buzzkill.data.model.Action.SetVariableAction]), per-rule
 * cooldown timestamps, and per-package mute windows. Kept in memory deliberately —
 * these are ephemeral automation state, not user data to persist.
 */
object VariableStore {
    private val variables = ConcurrentHashMap<String, String>()
    private val cooldownUntil = ConcurrentHashMap<Long, Long>()
    private val muteUntil = ConcurrentHashMap<String, Long>()

    fun setVariable(name: String, value: String) {
        if (name.isNotBlank()) variables[name] = value
    }

    fun getVariable(name: String): String? = variables[name]

    fun snapshot(): Map<String, String> = variables.toMap()

    /** Returns true if the rule is still within its cooldown window. */
    fun isInCooldown(ruleId: Long, now: Long): Boolean =
        (cooldownUntil[ruleId] ?: 0L) > now

    fun startCooldown(ruleId: Long, until: Long) {
        cooldownUntil[ruleId] = until
    }

    fun muteApp(pkg: String, until: Long) {
        muteUntil[pkg] = until
    }

    fun isAppMuted(pkg: String, now: Long): Boolean =
        (muteUntil[pkg] ?: 0L) > now

    fun clear() {
        variables.clear()
        cooldownUntil.clear()
        muteUntil.clear()
    }
}
