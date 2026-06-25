package com.buzzkill.engine

import com.buzzkill.data.model.MatchMode
import java.util.regex.Pattern

/** Result of a text comparison, carrying any regex capture groups. */
data class MatchResult(val matched: Boolean, val groups: Map<String, String> = emptyMap())

/** Stateless comparison helpers shared by triggers and replace actions. */
object TextMatcher {

    fun evaluate(
        mode: MatchMode,
        query: String,
        value: String,
        caseSensitive: Boolean,
    ): MatchResult {
        val hay = if (caseSensitive) value else value.lowercase()
        val needle = if (caseSensitive) query else query.lowercase()
        return when (mode) {
            MatchMode.CONTAINS -> MatchResult(needle.isNotEmpty() && hay.contains(needle))
            MatchMode.EQUALS -> MatchResult(hay == needle)
            MatchMode.STARTS_WITH -> MatchResult(hay.startsWith(needle))
            MatchMode.ENDS_WITH -> MatchResult(hay.endsWith(needle))
            MatchMode.REGEX -> regex(query, value, caseSensitive)
            MatchMode.WILDCARD -> regex(wildcardToRegex(query), value, caseSensitive)
        }
    }

    private fun regex(pattern: String, value: String, caseSensitive: Boolean): MatchResult {
        return try {
            val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
            val m = Pattern.compile(pattern, flags).matcher(value)
            if (m.find()) {
                val groups = buildMap {
                    for (i in 1..m.groupCount()) {
                        put(i.toString(), m.group(i) ?: "")
                    }
                }
                MatchResult(true, groups)
            } else {
                MatchResult(false)
            }
        } catch (_: Exception) {
            // An invalid pattern simply never matches rather than crashing the listener.
            MatchResult(false)
        }
    }

    /** Convert a glob (`*`, `?`) into an anchored regex. */
    private fun wildcardToRegex(glob: String): String {
        val sb = StringBuilder("^")
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                else -> sb.append(Pattern.quote(c.toString()))
            }
        }
        return sb.append('$').toString()
    }
}
