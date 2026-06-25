package com.buzzkill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import com.buzzkill.ui.theme.IOSColors
import com.buzzkill.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All Haze (frosted-glass) usage is funnelled through this file so any future API
 * change in the library is a single-file fix. [rememberAppHazeState] is shared
 * between a screen's scrolling content (the blur source) and its translucent bars.
 */

/** Provides the screen's HazeState to descendant bars/sheets. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Provides the *background* blur source so list cards can frost the gradient behind
 * them (independent of [LocalHazeState], which the bars use to blur scrolling content).
 */
val LocalCardHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun rememberAppHazeState(): HazeState = remember { HazeState() }

@Composable
fun ProvideHaze(state: HazeState, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalHazeState provides state, content = content)

/** Marks a composable (usually the scrolling content) as the blur source. */
fun Modifier.hazeSourceLayer(state: HazeState): Modifier = this.haze(state)

/** Applies a frosted-glass material over whatever the [state] captured behind it. */
@Composable
fun Modifier.frostedOverlay(state: HazeState): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    return this.hazeChild(state = state, style = HazeMaterials.thin(container))
}

/**
 * Card surface as Gaussian-blurred frosted glass over the background gradient. Uses a
 * fairly opaque "regular" material so row text stays readable. Falls back to a solid
 * surface when no card blur source is in scope (e.g. inside dialogs).
 */
@Composable
fun Modifier.cardFrost(): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    val state = LocalCardHazeState.current
    return if (state != null) {
        // "ultraThin" is the most translucent material, so the blurred coloured
        // backdrop clearly shows through the card as frosted glass.
        this.hazeChild(state = state, style = HazeMaterials.ultraThin(container))
    } else {
        this.background(container)
    }
}

/** The soft iOS background gradient drawn behind everything. */
@Composable
fun Modifier.iosBackgroundGradient(): Modifier {
    val dark = LocalIsDarkTheme.current
    val brush = if (dark) {
        Brush.verticalGradient(listOf(IOSColors.GradientTopDark, IOSColors.GradientBottomDark))
    } else {
        Brush.verticalGradient(listOf(IOSColors.GradientTopLight, IOSColors.GradientBottomLight))
    }
    return this.fillMaxSize().background(brush)
}

/**
 * The card blur source: the gradient plus a few soft coloured "orbs". These give the
 * frosted cards something colourful to Gaussian-blur, so the glass effect is actually
 * visible (a smooth gradient alone blurs to almost nothing).
 */
@Composable
fun GlassBackdrop(state: HazeState, modifier: Modifier = Modifier) {
    val dark = LocalIsDarkTheme.current
    val grad = if (dark) {
        listOf(IOSColors.GradientTopDark, IOSColors.GradientBottomDark)
    } else {
        listOf(IOSColors.GradientTopLight, IOSColors.GradientBottomLight)
    }
    // Soft pastel intensity — calm enough not to be jarring, but the circles keep
    // hard edges so the frosted blur still reads under the cards.
    val a = if (dark) 0.42f else 0.34f
    val blue = IOSColors.Blue.copy(alpha = a)
    val purple = IOSColors.Purple.copy(alpha = a)
    val pink = Color(0xFFFF5C7A).copy(alpha = a)
    val teal = Color(0xFF32D6C8).copy(alpha = a)
    val orange = IOSColors.Orange.copy(alpha = a)
    // The drawing is a CHILD Canvas (not just draw modifiers on an empty box) so Haze
    // actually captures it as the blur source. Hard-edged circles blur visibly under
    // the cards while staying crisp in the gaps.
    Box(modifier.fillMaxSize().hazeSourceLayer(state)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(grad))
            fun disc(color: Color, cx: Float, cy: Float, r: Float) {
                drawCircle(
                    color = color,
                    radius = size.minDimension * r,
                    center = Offset(size.width * cx, size.height * cy),
                )
            }
            disc(blue, 0.18f, 0.08f, 0.30f)
            disc(purple, 0.85f, 0.12f, 0.34f)
            disc(pink, 0.30f, 0.34f, 0.26f)
            disc(teal, 0.80f, 0.45f, 0.30f)
            disc(orange, 0.15f, 0.62f, 0.28f)
            disc(purple, 0.70f, 0.74f, 0.30f)
            disc(blue, 0.30f, 0.92f, 0.30f)
            disc(pink, 0.90f, 0.95f, 0.26f)
        }
    }
}
