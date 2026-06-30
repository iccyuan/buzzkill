@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iccyuan.hush.ui.editor

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iccyuan.hush.R
import com.iccyuan.hush.data.model.Action
import com.iccyuan.hush.data.model.MacroStep
import com.iccyuan.hush.data.model.MacroStepType
import com.iccyuan.hush.service.DanmakuController
import com.iccyuan.hush.service.HushAccessibilityService
import com.iccyuan.hush.service.MacroRecorder
import com.iccyuan.hush.ui.common.IntField
import com.iccyuan.hush.ui.components.DialogActions
import com.iccyuan.hush.ui.components.GlassDialog
import com.iccyuan.hush.ui.components.GlassScaffold
import com.iccyuan.hush.ui.components.HairlineDivider
import com.iccyuan.hush.ui.components.IOSFilledButton
import com.iccyuan.hush.ui.components.IOSRow
import com.iccyuan.hush.ui.components.IOSTintedButton
import com.iccyuan.hush.ui.components.InsetGroupedSection
import com.iccyuan.hush.ui.theme.IOSColors

/** 「打卡宏」动作的全屏编辑器：录制（实时捕获手势）→ 可编辑步骤列表 → 试运行。 */
@Composable
fun MacroEditorScreen(
    action: Action.RunMacroAction,
    onSave: (Action) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(action.id) { mutableStateOf(action) }
    var editingDelayIndex by remember { mutableStateOf<Int?>(null) }
    var sessionActive by remember { mutableStateOf(false) }

    val isRecording by MacroRecorder.isRecording.collectAsStateWithLifecycle()
    val recorded by MacroRecorder.steps.collectAsStateWithLifecycle()

    // 录制结束（用户在悬浮球上点「完成」）时，把录到的步骤导入草稿。
    LaunchedEffect(isRecording) {
        if (sessionActive && !isRecording) {
            sessionActive = false
            if (recorded.isNotEmpty()) {
                draft = draft.copy(
                    steps = recorded,
                    screenWidth = MacroRecorder.screenWidth,
                    screenHeight = MacroRecorder.screenHeight,
                )
            }
        }
    }

    GlassScaffold(
        title = stringResource(R.string.cat_act_macro),
        onBack = onDismiss,
        actions = {
            TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.save)) }
        },
        overlay = {
            editingDelayIndex?.let { idx ->
                val step = draft.steps.getOrNull(idx)
                if (step != null) {
                    DelayEditDialog(
                        delayMs = step.delayMs,
                        onConfirm = { newDelay ->
                            draft = draft.copy(steps = draft.steps.mapIndexed { i, s ->
                                if (i == idx) s.copy(delayMs = newDelay.toLong()) else s
                            })
                            editingDelayIndex = null
                        },
                        onDismiss = { editingDelayIndex = null },
                    )
                } else editingDelayIndex = null
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

            // 录制区。
            InsetGroupedSection(
                header = stringResource(R.string.macro_record_section),
                footer = stringResource(R.string.macro_record_hint),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRecording) {
                        Text(
                            stringResource(R.string.macro_recording),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IOSColors.Red,
                        )
                    } else {
                        IOSFilledButton(
                            text = if (draft.steps.isEmpty()) stringResource(R.string.macro_record)
                            else stringResource(R.string.macro_rerecord),
                            onClick = {
                                when {
                                    HushAccessibilityService.instance == null -> {
                                        toast(context, R.string.macro_accessibility_off)
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                    !Settings.canDrawOverlays(context) -> {
                                        toast(context, R.string.macro_need_overlay)
                                        context.startActivity(DanmakuController.overlaySettingsIntent(context))
                                    }
                                    else -> {
                                        sessionActive = true
                                        HushAccessibilityService.instance?.startRecording()
                                        toast(context, R.string.macro_record_started)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // 步骤列表。
            InsetGroupedSection(header = stringResource(R.string.macro_steps_section)) {
                if (draft.steps.isEmpty()) {
                    IOSRow(title = stringResource(R.string.macro_no_steps))
                } else {
                    draft.steps.forEachIndexed { i, step ->
                        if (i > 0) HairlineDivider(startInset = 16.dp)
                        StepRow(
                            index = i,
                            step = step,
                            onEditDelay = { editingDelayIndex = i },
                            onMoveUp = if (i > 0) {
                                { draft = draft.copy(steps = draft.steps.swap(i, i - 1)) }
                            } else null,
                            onMoveDown = if (i < draft.steps.lastIndex) {
                                { draft = draft.copy(steps = draft.steps.swap(i, i + 1)) }
                            } else null,
                            onDelete = {
                                draft = draft.copy(steps = draft.steps.filterIndexed { idx, _ -> idx != i })
                            },
                        )
                    }
                }
                HairlineDivider(startInset = 16.dp)
                IOSRow(
                    title = stringResource(R.string.macro_add_wait),
                    icon = Icons.Filled.HourglassBottom,
                    iconColor = IOSColors.Gray,
                    onClick = {
                        draft = draft.copy(steps = draft.steps + MacroStep(MacroStepType.WAIT, delayMs = 1000))
                    },
                )
            }

            // 重复次数 + 试运行。
            InsetGroupedSection(footer = stringResource(R.string.macro_test_hint)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntField(stringResource(R.string.macro_repeat), draft.repeat) {
                        draft = draft.copy(repeat = it.coerceIn(1, 50))
                    }
                    IOSTintedButton(
                        text = stringResource(R.string.macro_test_run),
                        onClick = {
                            val svc = HushAccessibilityService.instance
                            if (svc == null) {
                                toast(context, R.string.macro_accessibility_off)
                            } else if (draft.steps.isEmpty()) {
                                toast(context, R.string.macro_no_steps)
                            } else {
                                svc.playMacro(draft.steps, draft.screenWidth, draft.screenHeight, draft.repeat)
                            }
                        },
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

@Composable
private fun StepRow(
    index: Int,
    step: MacroStep,
    onEditDelay: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${index + 1}. ${stepDesc(step)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.macro_step_delay, step.delayMs.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEditDelay) { Text(stringResource(R.string.macro_edit_delay)) }
        IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) {
            Icon(Icons.Filled.ArrowUpward, stringResource(R.string.macro_move_up), Modifier.size(18.dp))
        }
        IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) {
            Icon(Icons.Filled.ArrowDownward, stringResource(R.string.macro_move_down), Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = IOSColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun stepDesc(step: MacroStep): String = when (step.type) {
    MacroStepType.TAP -> stringResource(R.string.macro_step_tap, step.x, step.y)
    MacroStepType.SWIPE -> stringResource(R.string.macro_step_swipe, step.x, step.y, step.x2, step.y2)
    MacroStepType.WAIT -> stringResource(R.string.macro_step_wait)
}

@Composable
private fun DelayEditDialog(delayMs: Long, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(delayMs.toInt()) }
    GlassDialog(onDismiss = onDismiss) {
        Text(
            stringResource(R.string.macro_edit_delay),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        IntField(stringResource(R.string.macro_step_delay_label), value) { value = it.coerceAtLeast(0) }
        DialogActions(
            confirmText = stringResource(R.string.save),
            onConfirm = { onConfirm(value) },
            secondaryText = stringResource(R.string.cancel),
            onSecondary = onDismiss,
        )
    }
}

private fun List<MacroStep>.swap(a: Int, b: Int): List<MacroStep> {
    val m = toMutableList()
    val t = m[a]; m[a] = m[b]; m[b] = t
    return m
}

private fun toast(context: android.content.Context, resId: Int) {
    Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
}
