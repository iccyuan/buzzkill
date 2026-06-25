package com.buzzkill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.buzzkill.ui.theme.IOSColors
import com.buzzkill.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * All Haze (frosted-glass) usage is funnelled through this file. The blur source is a
 * very faint coloured backdrop ([GlassBackdrop]); the cards, bars and dialogs frost it.
 * A smooth/empty backdrop blurs to nothing, so a touch of colour is what makes the
 * glass actually read — kept deliberately subtle here.
 */

/** The single backdrop blur source provided to bars/dialogs. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Same source, exposed to cards (kept separate so call sites read clearly). */
val LocalCardHazeState = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun rememberAppHazeState(): HazeState = remember { HazeState() }

/** Marks a composable as the blur source. */
fun Modifier.hazeSourceLayer(state: HazeState): Modifier = this.haze(state)

/** Frosted-glass material over whatever [state] captured (used by bars and dialogs). */
@Composable
fun Modifier.frostedOverlay(state: HazeState): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    return this.hazeChild(state = state, style = HazeMaterials.thin(container))
}

/** Card surface as translucent frosted glass over the faint backdrop. */
@Composable
fun Modifier.cardFrost(): Modifier {
    val dark = LocalIsDarkTheme.current
    val container = if (dark) IOSColors.SurfaceDark else IOSColors.SurfaceLight
    val state = LocalCardHazeState.current
    return if (state != null) {
        this.hazeChild(state = state, style = HazeMaterials.ultraThin(container))
    } else {
        this.background(container)
    }
}

/**
 * The blur source: a soft gradient plus a few VERY FAINT coloured circles. Drawn in a
 * child [Canvas] so Haze captures it. Faint enough not to be a loud wallpaper, but the
 * hard circle edges still give the frosted glass something to soften.
 */
@Composable
fun GlassBackdrop(state: HazeState, modifier: Modifier = Modifier) {
    val dark = LocalIsDarkTheme.current
    val grad = if (dark) {
        listOf(IOSColors.GradientTopDark, IOSColors.GradientBottomDark)
    } else {
        listOf(IOSColors.GradientTopLight, IOSColors.GradientBottomLight)
    }
    val a = if (dark) 0.24f else 0.18f
    val blue = IOSColors.Blue.copy(alpha = a)
    val purple = IOSColors.Purple.copy(alpha = a)
    val pink = Color(0xFFFF5C7A).copy(alpha = a)
    val teal = Color(0xFF32D6C8).copy(alpha = a)
    val orange = IOSColors.Orange.copy(alpha = a)
    Box(modifier.fillMaxSize().hazeSourceLayer(state)) {
        // Heavily blur the circles into a soft, dreamy colour wash.
        Canvas(Modifier.fillMaxSize().blur(60.dp, BlurredEdgeTreatment.Rectangle)) {
            drawRect(Brush.verticalGradient(grad))
            fun disc(color: Color, cx: Float, cy: Float, r: Float) {
                drawCircle(color, size.minDimension * r, Offset(size.width * cx, size.height * cy))
            }
            disc(blue, 0.16f, 0.08f, 0.34f)
            disc(purple, 0.88f, 0.12f, 0.36f)
            disc(pink, 0.28f, 0.40f, 0.30f)
            disc(teal, 0.82f, 0.50f, 0.32f)
            disc(orange, 0.14f, 0.66f, 0.30f)
            disc(purple, 0.74f, 0.78f, 0.32f)
            disc(blue, 0.30f, 0.94f, 0.30f)
            disc(pink, 0.90f, 0.95f, 0.28f)
        }
    }
}
