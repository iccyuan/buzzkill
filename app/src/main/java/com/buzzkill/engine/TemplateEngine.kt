package com.buzzkill.engine

import com.buzzkill.data.model.NotificationField

/**
 * Renders user-supplied templates against a [MatchContext].
 *
 * Supported placeholders:
 *  - {title} {text} {bigtext} {subtext} {ticker} {app} {package}
 *  - {1}..{9}            regex capture groups from the matching trigger
 *  - {var:name}          a user variable set by SetVariableAction
 *  - {time} {date}       not time-zone fancy; simple formatted values
 *
 * Unknown placeholders are left untouched so literal braces survive.
 */
object TemplateEngine {

    // Note: the closing brace is escaped — Android's ICU regex engine rejects a
    // bare '}' outside a {n,m} quantifier, unlike the desktop JVM.
    private val TOKEN = Regex("""\{([a-zA-Z0-9_:]+)\}""")

    fun render(template: String, ctx: MatchContext): String {
        if (template.isEmpty()) return template
        return TOKEN.replace(template) { m ->
            val key = m.groupValues[1]
            resolve(key, ctx) ?: m.value
        }
    }

    private fun resolve(key: String, ctx: MatchContext): String? = when {
        key.startsWith("var:") -> VariableStore.getVariable(key.removePrefix("var:")) ?: ""
        key == "title" -> ctx.field(NotificationField.TITLE)
        key == "text" -> ctx.field(NotificationField.TEXT)
        key == "bigtext" -> ctx.field(NotificationField.BIG_TEXT)
        key == "subtext" -> ctx.field(NotificationField.SUB_TEXT)
        key == "ticker" -> ctx.field(NotificationField.TICKER)
        key == "app" -> ctx.appName
        key == "package" -> ctx.packageName
        key.toIntOrNull() != null -> ctx.captures[key] ?: ""
        else -> null
    }
}
