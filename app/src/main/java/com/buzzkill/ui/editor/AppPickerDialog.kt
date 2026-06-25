package com.buzzkill.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buzzkill.R
import com.buzzkill.data.AppInfo
import com.buzzkill.data.InstalledApps
import com.buzzkill.ui.common.LabeledTextField
import com.buzzkill.ui.components.DialogActions
import com.buzzkill.ui.components.GlassDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppPickerDialog(
    initiallySelected: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val selected = remember { mutableStateListOf<String>().apply { addAll(initiallySelected) } }
    var query by remember { mutableStateOf("") }

    val apps by produceState<List<AppInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { InstalledApps.load(context) }
    }

    GlassDialog(onDismiss = onDismiss) {
        Text(
            stringResource(R.string.select_apps),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Column {
            LabeledTextField(stringResource(R.string.search), query) { query = it }
            val list = apps
            run {
                if (list == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    val filtered = remember(query, list) {
                        if (query.isBlank()) list
                        else list.filter {
                            it.label.contains(query, true) || it.packageName.contains(query, true)
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selected.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        if (checked) selected.add(app.packageName)
                                        else selected.remove(app.packageName)
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        app.packageName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        DialogActions(
            confirmText = stringResource(R.string.done),
            onConfirm = { onConfirm(selected.toList()) },
            secondaryText = stringResource(R.string.cancel),
            onSecondary = onDismiss,
        )
    }
}
