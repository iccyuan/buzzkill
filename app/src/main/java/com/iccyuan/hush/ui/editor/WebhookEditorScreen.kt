@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iccyuan.hush.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iccyuan.hush.R
import com.iccyuan.hush.data.model.Action
import com.iccyuan.hush.data.model.HttpMethod
import com.iccyuan.hush.data.model.KeyValue
import com.iccyuan.hush.data.model.WebhookBodyType
import com.iccyuan.hush.ui.common.EnumDropdown
import com.iccyuan.hush.ui.common.LabeledTextField
import com.iccyuan.hush.ui.components.GlassScaffold
import com.iccyuan.hush.ui.components.HairlineDivider
import com.iccyuan.hush.ui.components.InsetGroupedSection
import com.iccyuan.hush.ui.theme.IOSColors

/** WebhookBodyType 的友好显示名（接近 Postman 的写法）。 */
private fun bodyTypeLabel(t: WebhookBodyType): String = when (t) {
    WebhookBodyType.JSON -> "JSON (application/json)"
    WebhookBodyType.TEXT -> "Text (text/plain)"
    WebhookBodyType.FORM -> "Form (x-www-form-urlencoded)"
    WebhookBodyType.XML -> "XML (application/xml)"
}

/**
 * 全屏的 Webhook 编辑（类 Postman 的二级界面）：URL + 方法、查询参数、请求头，
 * POST 还可选请求体类型与内容。比对话框空间充裕、更好操作。
 */
@Composable
fun WebhookEditorScreen(
    action: Action.WebhookAction,
    onSave: (Action) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(action.id) { mutableStateOf(action) }

    GlassScaffold(
        title = stringResource(R.string.cat_act_webhook),
        onBack = onDismiss,
        actions = {
            TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // 请求：URL + 方法（仅 GET / POST）。
            InsetGroupedSection(header = stringResource(R.string.section_request)) {
                Column(Modifier.padding(12.dp)) {
                    val urlError = if (draft.url.isNotBlank() &&
                        !android.util.Patterns.WEB_URL.matcher(draft.url).matches()
                    ) stringResource(R.string.err_invalid_url) else null
                    LabeledTextField(stringResource(R.string.url), draft.url, error = urlError) {
                        draft = draft.copy(url = it)
                    }
                    Spacer(Modifier.height(8.dp))
                    EnumDropdown(
                        label = stringResource(R.string.method),
                        options = listOf(HttpMethod.GET, HttpMethod.POST),
                        selected = if (draft.method == HttpMethod.POST) HttpMethod.POST else HttpMethod.GET,
                        optionLabel = { it.name },
                        onSelected = { draft = draft.copy(method = it) },
                    )
                }
            }

            // 查询参数（拼到 URL，GET/POST 都可用）。
            KeyValueSection(
                header = stringResource(R.string.query_params),
                addLabel = stringResource(R.string.add_param),
                items = draft.queryParams,
                onChange = { draft = draft.copy(queryParams = it) },
            )

            // 请求头。
            KeyValueSection(
                header = stringResource(R.string.http_headers),
                addLabel = stringResource(R.string.add_header),
                items = draft.headers,
                onChange = { draft = draft.copy(headers = it) },
            )

            // 请求体（仅 POST）：类型 + 内容。
            if (draft.method == HttpMethod.POST) {
                InsetGroupedSection(header = stringResource(R.string.section_body)) {
                    Column(Modifier.padding(12.dp)) {
                        EnumDropdown(
                            label = stringResource(R.string.body_type),
                            options = WebhookBodyType.entries,
                            selected = draft.bodyType,
                            optionLabel = { bodyTypeLabel(it) },
                            onSelected = { draft = draft.copy(bodyType = it) },
                        )
                        Spacer(Modifier.height(8.dp))
                        LabeledTextField(
                            stringResource(R.string.body_template), draft.bodyTemplate, singleLine = false,
                        ) { draft = draft.copy(bodyTemplate = it) }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.webhook_get_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Text(
                stringResource(R.string.template_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            InsetGroupedSection {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDelete() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.delete), color = IOSColors.Red)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 可增删的键值对列表分区（用于查询参数与请求头）。值支持模板占位符。 */
@Composable
private fun KeyValueSection(
    header: String,
    addLabel: String,
    items: List<KeyValue>,
    onChange: (List<KeyValue>) -> Unit,
) {
    InsetGroupedSection(header = header) {
        items.forEachIndexed { i, kv ->
            if (i > 0) HairlineDivider(startInset = 16.dp)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabeledTextField(
                    stringResource(R.string.header_name), kv.name, modifier = Modifier.weight(1f),
                ) { v -> onChange(items.mapAt(i) { it.copy(name = v) }) }
                LabeledTextField(
                    stringResource(R.string.header_value), kv.value, modifier = Modifier.weight(1.3f),
                ) { v -> onChange(items.mapAt(i) { it.copy(value = v) }) }
                IconButton(onClick = { onChange(items.filterIndexed { j, _ -> j != i }) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        }
        if (items.isNotEmpty()) HairlineDivider(startInset = 16.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onChange(items + KeyValue()) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = IOSColors.Blue)
            Text(addLabel, style = MaterialTheme.typography.bodyMedium, color = IOSColors.Blue)
        }
    }
}

private fun <T> List<T>.mapAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, item -> if (i == index) transform(item) else item }
