package com.buzzkill.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

/** Exposes whether the current theme is dark to non-color-scheme consumers (glass tints). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val IOSDarkColors = darkColorScheme(
    primary = IOSColors.BlueDark,
    onPrimary = Color.White,
    secondary = IOSColors.GreenDark,
    error = IOSColors.RedDark,
    background = IOSColors.GroupedBgDark,
    onBackground = IOSColors.LabelDark,
    surface = IOSColors.SurfaceDark,
    onSurface = IOSColors.LabelDark,
    surfaceVariant = IOSColors.ElevatedDark,
    onSurfaceVariant = IOSColors.SecondaryLabelDark,
    outline = IOSColors.SeparatorDark,
)

private val IOSLightColors = lightColorScheme(
    primary = IOSColors.Blue,
    onPrimary = Color.White,
    secondary = IOSColors.Green,
    error = IOSColors.Red,
    background = IOSColors.GroupedBgLight,
    onBackground = IOSColors.LabelLight,
    surface = IOSColors.SurfaceLight,
    onSurface = IOSColors.LabelLight,
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = IOSColors.SecondaryLabelLight,
    outline = IOSColors.SeparatorLight,
)

/** Rounded, iOS-like continuous-corner approximation. */
private val IOSShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BuzzKillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) IOSDarkColors else IOSLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = IOSShapes,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}
