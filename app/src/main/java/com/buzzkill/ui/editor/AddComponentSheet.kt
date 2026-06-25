package com.buzzkill.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buzzkill.ui.components.GlassSheet

/** Frosted bottom sheet listing catalog entries of one component kind. */
@Composable
fun <T> AddComponentSheet(
    title: String,
    entries: List<CatalogEntry<T>>,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassSheet(onDismiss = onDismiss) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            entries.forEachIndexed { index, entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(entry.create()) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(entry.labelRes), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(entry.descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index < entries.lastIndex) HorizontalDivider()
            }
        }
    }
}
