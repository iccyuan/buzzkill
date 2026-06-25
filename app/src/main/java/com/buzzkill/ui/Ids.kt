package com.buzzkill.ui

import java.util.UUID

/** Short stable ids for rule components created in the editor. */
object Ids {
    fun next(): String = UUID.randomUUID().toString().take(8)
}
