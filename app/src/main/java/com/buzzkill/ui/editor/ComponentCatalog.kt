package com.buzzkill.ui.editor

import androidx.annotation.StringRes
import com.buzzkill.R
import com.buzzkill.data.model.Action
import com.buzzkill.data.model.Condition
import com.buzzkill.data.model.Trigger
import com.buzzkill.ui.Ids

/** Definitions for the "add component" picker: localized label/desc + factory. */
data class CatalogEntry<T>(
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val create: () -> T,
)

object ComponentCatalog {

    val triggers: List<CatalogEntry<Trigger>> = listOf(
        CatalogEntry(R.string.cat_trig_text, R.string.cat_trig_text_desc) {
            Trigger.TextTrigger(Ids.next())
        },
        CatalogEntry(R.string.cat_trig_ongoing, R.string.cat_trig_ongoing_desc) {
            Trigger.OngoingTrigger(Ids.next())
        },
        CatalogEntry(R.string.cat_trig_reply, R.string.cat_trig_reply_desc) {
            Trigger.HasReplyTrigger(Ids.next())
        },
    )

    val conditions: List<CatalogEntry<Condition>> = listOf(
        CatalogEntry(R.string.cat_cond_time, R.string.cat_cond_time_desc) {
            Condition.TimeCondition(Ids.next())
        },
        CatalogEntry(R.string.cat_cond_holiday, R.string.cat_cond_holiday_desc) {
            Condition.HolidayCondition(Ids.next())
        },
        CatalogEntry(R.string.cat_cond_charging, R.string.cat_cond_charging_desc) {
            Condition.ChargingCondition(Ids.next())
        },
        CatalogEntry(R.string.cat_cond_screen, R.string.cat_cond_screen_desc) {
            Condition.ScreenCondition(Ids.next())
        },
        CatalogEntry(R.string.cat_cond_battery, R.string.cat_cond_battery_desc) {
            Condition.BatteryLevelCondition(Ids.next())
        },
        CatalogEntry(R.string.cat_cond_cooldown, R.string.cat_cond_cooldown_desc) {
            Condition.CooldownCondition(Ids.next())
        },
    )

    val actions: List<CatalogEntry<Action>> = listOf(
        CatalogEntry(R.string.cat_act_replace, R.string.cat_act_replace_desc) {
            Action.ReplaceTextAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_setfield, R.string.cat_act_setfield_desc) {
            Action.SetFieldAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_discard, R.string.cat_act_discard_desc) {
            Action.DiscardAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_dismiss, R.string.cat_act_dismiss_desc) {
            Action.DismissAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_snooze, R.string.cat_act_snooze_desc) {
            Action.SnoozeAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_importance, R.string.cat_act_importance_desc) {
            Action.MarkImportantAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_sound, R.string.cat_act_sound_desc) {
            Action.SoundVibrationAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_autoreply, R.string.cat_act_autoreply_desc) {
            Action.AutoReplyAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_readaloud, R.string.cat_act_readaloud_desc) {
            Action.ReadAloudAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_wake, R.string.cat_act_wake_desc) {
            Action.WakeScreenAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_toast, R.string.cat_act_toast_desc) {
            Action.ToastAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_setvar, R.string.cat_act_setvar_desc) {
            Action.SetVariableAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_tasker, R.string.cat_act_tasker_desc) {
            Action.RunTaskerAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_webhook, R.string.cat_act_webhook_desc) {
            Action.WebhookAction(Ids.next())
        },
        CatalogEntry(R.string.cat_act_mute, R.string.cat_act_mute_desc) {
            Action.MuteAppAction(Ids.next())
        },
    )
}
