package com.buzzkill.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.buzzkill.ui.theme.IOSColors
import com.buzzkill.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All Haze (frosted-glass) usage is funnelled through this file so any future API
 * change in the library is a single-file fix. The screen's scrolling content is the
 * blur source; the translucent bars and dialogs frost it.
 */

/** Provides the screen's HazeState (the content blur source) to bars/sheets/dialogs. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun rememberAppHazeState(): HazeState = remember { HazeState() }

/** Marks a composable (the scrolling content) as the blur source. */
fun Modifier.hazeSourceLayer(state: HazeState): Modifier = this.haze(state)

/** Applies a frosted-glass material over whatever the [state] captured behind it. */
@Composable
fun Modifier.frostedOverlay(state: HazeState): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    return this.hazeChild(state = state, style = HazeMaterials.thin(container))
}

/** Clean solid iOS grouped-list card surface on a plain background. */
@Composable
fun Modifier.cardFrost(): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    return this.background(container)
}
