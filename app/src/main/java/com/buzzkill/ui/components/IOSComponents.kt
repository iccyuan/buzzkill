package com.buzzkill.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buzzkill.ui.theme.IOSColors
import com.buzzkill.ui.theme.LocalIsDarkTheme

private val NavBarHeight = 44.dp

/**
 * Full-screen iOS-style container: soft gradient background, content that scrolls
 * *under* a frosted nav bar, and an optional frosted bottom bar.
 */
@Composable
fun GlassScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    /** Dialogs/sheets, drawn on top of everything but inside the blur tree so they frost. */
    overlay: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // ONE blur source — the coloured backdrop. Cards and bars are all hazeChild of
    // it. (A second haze source wrapping the content breaks hazeChild capture for the
    // cards nested inside it, which is why they rendered as flat white.)
    val haze = rememberAppHazeState()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeight = if (bottomBar != null) 64.dp else 0.dp

    Box(modifier.fillMaxSize()) {
        // 1. Gradient + coloured circles — the single blur source.
        GlassBackdrop(haze)

        // 2. Scrolling content (no haze source of its own); cards frost the backdrop.
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalHazeState provides haze,
                LocalCardHazeState provides haze,
            ) {
                content(
                    PaddingValues(
                        top = statusTop + NavBarHeight,
                        bottom = navBars + bottomBarHeight,
                    )
                )
            }
        }

        IOSNavBar(
            title = title,
            statusTop = statusTop,
            onBack = onBack,
            actions = actions,
            modifier = Modifier.frostedOverlay(haze),
        )

        if (bottomBar != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .frostedOverlay(haze)
                    .padding(bottom = navBars),
            ) { bottomBar() }
        }

        // 3. Overlays (dialogs/sheets) on top of everything, with the blur source in scope.
        CompositionLocalProvider(
            LocalHazeState provides haze,
            LocalCardHazeState provides haze,
        ) {
            overlay()
        }
    }
}

@Composable
private fun IOSNavBar(
    title: String,
    statusTop: androidx.compose.ui.unit.Dp,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.height(statusTop).fillMaxWidth())
        Box(Modifier.height(NavBarHeight).fillMaxWidth().padding(horizontal = 8.dp)) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(18.dp),
                    )
                }
            }
            Text(
                title,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                content = { actions() },
            )
        }
        HairlineDivider()
    }
}

/** A 0.5dp iOS separator. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, startInset: androidx.compose.ui.unit.Dp = 0.dp) {
    val color = if (LocalIsDarkTheme.current) IOSColors.SeparatorDark else IOSColors.SeparatorLight
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(0.5.dp)
            .background(color),
    )
}

/**
 * iOS inset-grouped list section: an optional header, then a rounded card whose
 * children are separated by hairlines.
 */
@Composable
fun InsetGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp),
            )
        }
        Column(
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .cardFrost(),
            content = { content() },
        )
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp),
            )
        }
    }
}

/** A single iOS list row: leading label, trailing content, optional tap + chevron. */
@Composable
fun IOSRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Filled, rounded iOS-style button (accent or destructive). */
@Composable
fun IOSFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val container = if (destructive) IOSColors.Red else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/** Bordered, neutral iOS-style button. */
@Composable
fun IOSTintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (LocalIsDarkTheme.current) Color(0x33787880) else Color(0x1F787880)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
}

/** iOS-green tinted switch. */
@Composable
fun IOSSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = IOSColors.Green,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = if (LocalIsDarkTheme.current) Color(0xFF39393D) else Color(0xFFE9E9EA),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** iOS segmented control. */
@Composable
fun <T> IOSSegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (LocalIsDarkTheme.current) Color(0xFF1C1C1E) else Color(0xFFE4E4E9)
    Row(
        modifier
            .clip(RoundedCornerShape(9.dp))
            .background(trackColor)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSel = option == selected
            val bg by animateColorAsState(
                if (isSel) (if (LocalIsDarkTheme.current) Color(0xFF636366) else Color.White) else Color.Transparent,
                label = "seg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(bg)
                    .clickable { onSelect(option) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

