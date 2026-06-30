@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iccyuan.hush.ui.editor

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.iccyuan.hush.R
import com.iccyuan.hush.data.AppInfo
import com.iccyuan.hush.data.InstalledApps
import com.iccyuan.hush.data.model.Action
import com.iccyuan.hush.ui.components.GlassScaffold
import com.iccyuan.hush.ui.components.HairlineDivider
import com.iccyuan.hush.ui.components.IOSFilledButton
import com.iccyuan.hush.ui.components.IOSRow
import com.iccyuan.hush.ui.components.InsetGroupedSection
import com.iccyuan.hush.ui.theme.IOSColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 「打开应用 / 页面」动作的全屏编辑器：选应用 → 可选地选一个可调用的 Activity。 */
@Composable
fun LaunchAppEditorScreen(
    action: Action.LaunchAppAction,
    onSave: (Action) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(action.id) { mutableStateOf(action) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }

    // 已选应用的名称/图标。
    val appInfo by produceState<AppInfo?>(null, draft.packageName) {
        value = if (draft.packageName.isBlank()) null
        else withContext(Dispatchers.IO) { InstalledApps.infoFor(context, listOf(draft.packageName)).firstOrNull() }
    }

    GlassScaffold(
        title = stringResource(R.string.cat_act_launch),
        onBack = onDismiss,
        actions = {
            TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.save)) }
        },
        overlay = {
            if (showAppPicker) {
                AppPickerScreen(
                    initiallySelected = listOfNotNull(draft.packageName.ifBlank { null }),
                    onConfirm = { picked ->
                        // 单选语义：取与当前不同的那个（用户新点的），否则取第一个。
                        val pkg = picked.firstOrNull { it != draft.packageName } ?: picked.firstOrNull() ?: ""
                        // 换了应用就清空已选页面。
                        draft = if (pkg != draft.packageName) draft.copy(packageName = pkg, activityName = "", label = "")
                        else draft
                        showAppPicker = false
                    },
                    onDismiss = { showAppPicker = false },
                )
            }
            if (showActivityPicker && draft.packageName.isNotBlank()) {
                ActivityPickerScreen(
                    packageName = draft.packageName,
                    appLabel = appInfo?.label ?: draft.packageName,
                    onPick = { activityName, activityLabel ->
                        val appLabel = appInfo?.label ?: draft.packageName
                        val label = if (activityName.isBlank()) appLabel else "$appLabel · $activityLabel"
                        draft = draft.copy(activityName = activityName, label = label)
                        showActivityPicker = false
                    },
                    onDismiss = { showActivityPicker = false },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            InsetGroupedSection(header = stringResource(R.string.launch_app_section)) {
                val info = appInfo
                if (info == null) {
                    IOSRow(
                        title = stringResource(R.string.launch_app_choose),
                        icon = Icons.Filled.Apps,
                        iconColor = IOSColors.Blue,
                        onClick = { showAppPicker = true },
                    )
                } else {
                    val bmp = remember(info.packageName) {
                        runCatching { info.icon?.toBitmap(72, 72)?.asImageBitmap() }.getOrNull()
                    }
                    IOSRow(
                        title = info.label,
                        subtitle = info.packageName,
                        icon = Icons.Filled.Apps,
                        iconColor = IOSColors.Blue,
                        onClick = { showAppPicker = true },
                        trailing = {
                            if (bmp != null) Image(bmp, null, Modifier.size(28.dp))
                            else Icon(Icons.Filled.Android, null, Modifier.size(28.dp))
                        },
                    )
                }
            }

            // 选页面：仅在已选应用后出现。
            if (draft.packageName.isNotBlank()) {
                InsetGroupedSection(
                    header = stringResource(R.string.launch_activity_section),
                    footer = stringResource(R.string.launch_activity_hint),
                ) {
                    IOSRow(
                        title = if (draft.activityName.isBlank()) stringResource(R.string.launch_default_page)
                        else draft.activityName.substringAfterLast('.'),
                        subtitle = if (draft.activityName.isBlank()) null else draft.activityName,
                        icon = Icons.Filled.Launch,
                        iconColor = IOSColors.Blue,
                        onClick = { showActivityPicker = true },
                    )
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                IOSFilledButton(
                    text = stringResource(R.string.delete),
                    destructive = true,
                    onClick = onDelete,
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 应用内可调用页面（Activity）选择器：仅列出 exported 且 enabled 的 Activity。 */
@Composable
private fun ActivityPickerScreen(
    packageName: String,
    appLabel: String,
    onPick: (activityName: String, activityLabel: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activities by produceState<List<ActivityItem>?>(null, packageName) {
        value = withContext(Dispatchers.IO) { loadCallableActivities(context, packageName) }
    }

    GlassScaffold(
        title = stringResource(R.string.launch_activity_section),
        onBack = onDismiss,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            InsetGroupedSection(footer = stringResource(R.string.launch_activity_only_callable)) {
                // 顶部固定项：默认页面（应用启动入口）。
                IOSRow(
                    title = stringResource(R.string.launch_default_page),
                    icon = Icons.Filled.Apps,
                    iconColor = IOSColors.Blue,
                    onClick = { onPick("", appLabel) },
                )
                val list = activities
                if (list == null) {
                    HairlineDivider(startInset = 16.dp)
                    IOSRow(title = stringResource(R.string.loading))
                } else {
                    list.forEach { item ->
                        HairlineDivider(startInset = 16.dp)
                        IOSRow(
                            title = item.label,
                            subtitle = item.className,
                            icon = Icons.Filled.Launch,
                            iconColor = IOSColors.Indigo,
                            onClick = { onPick(item.className, item.label) },
                        )
                    }
                    if (list.isEmpty()) {
                        HairlineDivider(startInset = 16.dp)
                        IOSRow(title = stringResource(R.string.launch_activity_none))
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private data class ActivityItem(val className: String, val label: String)

/** 列出某应用中「可正常调用」的 Activity：exported 且 enabled，排除默认启动入口本身。 */
private fun loadCallableActivities(context: android.content.Context, pkg: String): List<ActivityItem> {
    val pm = context.packageManager
    return runCatching {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
        info.activities.orEmpty()
            .filter { it.exported && it.isEnabled }
            .map { ai ->
                val label = runCatching { ai.loadLabel(pm).toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() && it != pkg }
                    ?: ai.name.substringAfterLast('.')
                ActivityItem(ai.name, label)
            }
            .distinctBy { it.className }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}
