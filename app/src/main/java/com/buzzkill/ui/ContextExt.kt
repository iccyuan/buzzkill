package com.buzzkill.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks the context-wrapper chain to find the host Activity (for recreate()). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
