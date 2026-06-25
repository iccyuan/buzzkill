@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.buzzkill.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buzzkill.R
import com.buzzkill.data.model.Action
import com.buzzkill.data.model.Condition
import com.buzzkill.data.model.LogicMode
import com.buzzkill.data.model.Trigger
import com.buzzkill.ui.Localize
import com.buzzkill.ui.common.LabeledTextField
import com.buzzkill.ui.components.GlassScaffold
import com.buzzkill.ui.components.HairlineDivider
import com.buzzkill.ui.components.IOSFilledButton
import com.buzzkill.ui.components.IOSRow
import com.buzzkill.ui.components.IOSSegmented
import com.buzzkill.ui.components.IOSSwitch
import com.buzzkill.ui.components.IOSTintedButton
import com.buzzkill.ui.components.InsetGroupedSection

private enum class AddKind { TRIGGER, CONDITION, ACTION }

@Composable
fun RuleEditorScreen(
    ruleId: Long,
    onDone: () -> Unit,
    vm: RuleEditorViewModel = viewModel(),
) {
    LaunchedEffect(ruleId) { vm.load(ruleId) }
    val rule by vm.rule.collectAsStateWithLifecycle()

    var editingTrigger by remember { mutableStateOf<Trigger?>(null) }
    var editingCondition by remember { mutableStateOf<Condition?>(null) }
    var editingAction by remember { mutableStateOf<Action?>(null) }
    var addKind by remember { mutableStateOf<AddKind?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    GlassScaffold(
        title = stringResource(if (ruleId == 0L) R.string.new_rule else R.string.edit_rule),
        onBack = onDone,
        actions = {
            TextButton(onClick = { vm.save(onDone) }) { Text(stringResource(R.string.save)) }
        },
        overlay = {
            EditorOverlays(
                editingTrigger = editingTrigger,
                onTriggerSave = { vm.updateTrigger(it); editingTrigger = null },
                onTriggerDelete = { editingTrigger?.let { vm.removeTrigger(it.id) }; editingTrigger = null },
                onTriggerDismiss = { editingTrigger = null },
                editingCondition = editingCondition,
                onConditionSave = { vm.updateCondition(it); editingCondition = null },
                onConditionDelete = { editingCondition?.let { vm.removeCondition(it.id) }; editingCondition = null },
                onConditionDismiss = { editingCondition = null },
                editingAction = editingAction,
                onActionSave = { vm.updateAction(it); editingAction = null },
                onActionDelete = { editingAction?.let { vm.removeAction(it.id) }; editingAction = null },
                onActionDismiss = { editingAction = null },
                addKind = addKind,
                onAddTrigger = { vm.addTrigger(it); addKind = null; editingTrigger = it },
                onAddCondition = { vm.addCondition(it); addKind = null; editingCondition = it },
                onAddAction = { vm.addAction(it); addKind = null; editingAction = it },
                onAddDismiss = { addKind = null },
                showAppPicker = showAppPicker,
                appPackages = rule.appPackages,
                onAppsConfirm = { vm.setApps(it); showAppPicker = false },
                onAppsDismiss = { showAppPicker = false },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Name
            InsetGroupedSection {
                Column(Modifier.padding(12.dp)) {
                    LabeledTextField(stringResource(R.string.rule_name), rule.name) { vm.setName(it) }
                }
            }

            // Apps
            InsetGroupedSection(header = stringResource(R.string.section_apps)) {
                IOSRow(
                    title = if (rule.appPackages.isEmpty()) stringResource(R.string.applies_all_apps)
                    else rule.appPackages.joinToString(", "),
                    onClick = { showAppPicker = true },
                )
            }

            // Triggers
            InsetGroupedSection(
                header = stringResource(R.string.section_triggers),
                footer = if (rule.triggers.isEmpty()) stringResource(R.string.no_triggers_hint) else null,
            ) {
                Column(Modifier.padding(12.dp)) {
                    IOSSegmented(
                        options = LogicMode.entries,
                        selected = rule.triggerLogic,
                        label = { stringResource(Localize.logicRes(it)) },
                        onSelect = { vm.setTriggerLogic(it) },
                    )
                }
                rule.triggers.forEach { trigger ->
                    HairlineDivider(startInset = 16.dp)
                    IOSRow(title = Localize.summary(trigger), onClick = { editingTrigger = trigger })
                }
                HairlineDivider(startInset = 16.dp)
                AddRow(stringResource(R.string.add_trigger)) { addKind = AddKind.TRIGGER }
            }

            // Conditions
            InsetGroupedSection(header = stringResource(R.string.section_conditions)) {
                rule.conditions.forEachIndexed { i, condition ->
                    if (i > 0) HairlineDivider(startInset = 16.dp)
                    IOSRow(title = Localize.summary(condition), onClick = { editingCondition = condition })
                }
                if (rule.conditions.isNotEmpty()) HairlineDivider(startInset = 16.dp)
                AddRow(stringResource(R.string.add_condition)) { addKind = AddKind.CONDITION }
            }

            // Actions
            InsetGroupedSection(
                header = stringResource(R.string.section_actions),
                footer = if (rule.actions.isEmpty()) stringResource(R.string.no_actions_hint) else null,
            ) {
                rule.actions.forEachIndexed { i, action ->
                    if (i > 0) HairlineDivider(startInset = 16.dp)
                    IOSRow(title = Localize.summary(action), onClick = { editingAction = action })
                }
                if (rule.actions.isNotEmpty()) HairlineDivider(startInset = 16.dp)
                AddRow(stringResource(R.string.add_action)) { addKind = AddKind.ACTION }
            }

            // Options
            InsetGroupedSection(header = stringResource(R.string.section_options)) {
                IOSRow(
                    title = stringResource(R.string.enabled),
                    trailing = { IOSSwitch(rule.enabled) { vm.setEnabled(it) } },
                )
                HairlineDivider(startInset = 16.dp)
                IOSRow(
                    title = stringResource(R.string.stop_processing),
                    trailing = { IOSSwitch(rule.stopProcessing) { vm.setStopProcessing(it) } },
                )
                HairlineDivider(startInset = 16.dp)
                Column(Modifier.padding(12.dp)) {
                    LabeledTextField(stringResource(R.string.notes), rule.notes, singleLine = false) {
                        vm.setNotes(it)
                    }
                }
            }

            if (ruleId != 0L) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    IOSFilledButton(
                        text = stringResource(R.string.delete),
                        destructive = true,
                        onClick = { vm.delete(onDone) },
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

}

/** All editor dialogs/sheets, rendered inside GlassScaffold's overlay slot so they frost. */
@Composable
private fun EditorOverlays(
    editingTrigger: Trigger?,
    onTriggerSave: (Trigger) -> Unit,
    onTriggerDelete: () -> Unit,
    onTriggerDismiss: () -> Unit,
    editingCondition: Condition?,
    onConditionSave: (Condition) -> Unit,
    onConditionDelete: () -> Unit,
    onConditionDismiss: () -> Unit,
    editingAction: Action?,
    onActionSave: (Action) -> Unit,
    onActionDelete: () -> Unit,
    onActionDismiss: () -> Unit,
    addKind: AddKind?,
    onAddTrigger: (Trigger) -> Unit,
    onAddCondition: (Condition) -> Unit,
    onAddAction: (Action) -> Unit,
    onAddDismiss: () -> Unit,
    showAppPicker: Boolean,
    appPackages: List<String>,
    onAppsConfirm: (List<String>) -> Unit,
    onAppsDismiss: () -> Unit,
) {
    TriggerEditorDialog(editingTrigger, onTriggerSave, onTriggerDelete, onTriggerDismiss)
    ConditionEditorDialog(editingCondition, onConditionSave, onConditionDelete, onConditionDismiss)
    ActionEditorDialog(editingAction, onActionSave, onActionDelete, onActionDismiss)

    when (addKind) {
        AddKind.TRIGGER -> AddComponentSheet(
            stringResource(R.string.add_trigger), ComponentCatalog.triggers, onAddTrigger, onAddDismiss
        )
        AddKind.CONDITION -> AddComponentSheet(
            stringResource(R.string.add_condition), ComponentCatalog.conditions, onAddCondition, onAddDismiss
        )
        AddKind.ACTION -> AddComponentSheet(
            stringResource(R.string.add_action), ComponentCatalog.actions, onAddAction, onAddDismiss
        )
        null -> Unit
    }

    if (showAppPicker) {
        AppPickerDialog(appPackages, onAppsConfirm, onAppsDismiss)
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    IOSRow(
        title = label,
        onClick = onClick,
        trailing = {
            Text("＋", color = MaterialTheme.colorScheme.primary)
        },
    )
}
