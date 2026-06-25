package com.buzzkill.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.buzzkill.data.model.Importance
import com.buzzkill.data.model.VibrationPreset
import com.buzzkill.engine.SoundOverride

/**
 * On Android O+ a notification's importance, sound and vibration are properties of
 * its channel, not the notification. To honour per-rule overrides we lazily create a
 * distinct channel for each unique (importance, sound, vibration, dnd) combination
 * and cache it by a signature key.
 */
class ChannelManager(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private val created = HashSet<String>()

    companion object {
        const val HISTORY_CHANNEL = "buzzkill_activity"
        const val DEFAULT_REPOST = "buzzkill_repost_default"
    }

    fun ensureBaseChannels() {
        val activity = NotificationChannel(
            HISTORY_CHANNEL,
            context.getString(com.buzzkill.R.string.channel_activity),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = context.getString(com.buzzkill.R.string.channel_activity_desc) }
        nm.createNotificationChannel(activity)
    }

    /** Returns a channel id matching the requested alerting overrides. */
    fun channelFor(
        importance: Importance?,
        sound: SoundOverride?,
        bypassDnd: Boolean,
    ): String {
        val imp = importance ?: Importance.DEFAULT
        val sig = buildString {
            append("repost_")
            append(imp.name)
            append("_silent").append(sound?.silent == true)
            append("_snd").append(sound?.soundUri ?: "default")
            append("_vib").append(sound?.vibration?.name ?: "default")
            append("_dnd").append(bypassDnd)
        }
        if (created.add(sig)) {
            createChannel(sig, imp, sound, bypassDnd)
        }
        return sig
    }

    private fun createChannel(
        id: String,
        importance: Importance,
        sound: SoundOverride?,
        bypassDnd: Boolean,
    ) {
        val channel = NotificationChannel(id, channelName(importance), importance.toPlatform())
        when {
            sound?.silent == true -> {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
            else -> {
                sound?.soundUri?.let {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    channel.setSound(Uri.parse(it), attrs)
                }
                sound?.vibration?.let { preset ->
                    if (preset == VibrationPreset.NONE) {
                        channel.enableVibration(false)
                    } else {
                        channel.enableVibration(true)
                        channel.vibrationPattern = preset.pattern
                    }
                }
            }
        }
        // Best-effort DND bypass; silently ignored without policy access.
        try {
            channel.setBypassDnd(bypassDnd || importance == Importance.URGENT)
        } catch (_: Exception) {
        }
        nm.createNotificationChannel(channel)
    }

    private fun channelName(importance: Importance) =
        context.getString(com.buzzkill.R.string.channel_modified, importance.name.lowercase())

    private fun Importance.toPlatform(): Int = when (this) {
        Importance.MIN -> NotificationManager.IMPORTANCE_MIN
        Importance.LOW -> NotificationManager.IMPORTANCE_LOW
        Importance.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
        Importance.HIGH -> NotificationManager.IMPORTANCE_HIGH
        Importance.URGENT -> NotificationManager.IMPORTANCE_HIGH
    }
}
