# BuzzKill

A native Android notification-automation app: define **rules** that match incoming
notifications and run **actions** on them — replace/rewrite text, mute, change
importance, auto-reply, read aloud, snooze, forward to a webhook, run a Tasker task,
and more.

This is a clean-room implementation written from scratch in Kotlin + Jetpack Compose.
It contains no third-party app's code or assets — only an original realization of the
same feature set.

## Features

A rule = **apps** + **triggers** + **conditions** + **actions**.

**Triggers** (with ALL/ANY logic; empty = match every notification)
- Text match on any field (title, text, expanded text, sub/info text, ticker, app name)
  with `contains / equals / starts / ends / regex / wildcard`, case sensitivity and NOT.
- Ongoing vs dismissible state.
- Has an inline reply action (chat notifications).

**Conditions** (AND-gated context checks)
- Time of day + day-of-week window (wraps past midnight).
- Charging / on battery.
- Screen on / off.
- Battery above/below a threshold.
- Cooldown (rate-limit how often a rule fires).

**Actions** (run in order)
- Replace text (literal or regex with `$1` back-refs).
- Set a field from a template.
- Discard (hide entirely) / Dismiss (optionally delayed) / Snooze.
- Change importance + bypass Do Not Disturb.
- Sound & vibration override (silence or custom buzz pattern).
- Auto-reply via the notification's RemoteInput.
- Read aloud (text-to-speech).
- Wake screen / Show toast.
- Set a variable for later use.
- Run a Tasker task / Fire a webhook (GET/POST/PUT).
- Mute the source app for N minutes.

**Templates** — action text supports placeholders:
`{title} {text} {bigtext} {subtext} {ticker} {app} {package}`, regex captures
`{1}…{9}`, and user variables `{var:name}`.

**Other**
- Master on/off switch; per-rule enable; "stop processing" to short-circuit later rules.
- Per-rule fire counters.
- Export/import all rules as JSON (via clipboard).
- Dynamic-color (Material You) theming.

## Architecture

```
data/        Room entities + DAO, sealed Trigger/Condition/Action models
             (kotlinx.serialization, stored as JSON columns), repository, settings
engine/      Pure, Android-free rule evaluation: RuleEngine, TextMatcher,
             TemplateEngine, VariableStore, Decision/MatchContext types
service/     NotificationListenerService, notification rebuild/repost, channel
             manager, TTS, side-effect executor, auto-reply, device-state sampling
ui/          Compose: rule list, rule editor (trigger/condition/action dialogs +
             app picker), settings, theming, navigation
```

How modification works: a `NotificationListenerService` cannot edit another app's
notification in place, so a matched notification is **rebuilt under our own channel**
(to control alerting), the original is cancelled, and the rebuilt copy is reposted —
preserving icon, content intent, actions and styling, and showing the original app's
name via the substitute-name extra.

## Build

Requires the Android SDK (API 35) and JDK 17+.

1. Open the project in **Android Studio** (Ladybug / 2024.2+), or create
   `local.properties` with `sdk.dir=/path/to/Android/Sdk`.
2. Build & run the `app` configuration, or from a terminal:
   ```
   ./gradlew assembleDebug
   ```
3. On the device, open the app and tap **Grant access** to enable
   *Settings → Notifications → Notification access → BuzzKill*.

The Gradle wrapper targets Gradle 8.10.2 / AGP 8.7.2 / Kotlin 2.0.21.

> Note: this repository was scaffolded without a local Android SDK, so the project
> has not been compiled here. Open it in Android Studio to build the APK.

## Permissions

- **Notification access** (the core; granted in system settings).
- `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK` — for reposted/alerting notifications.
- `INTERNET` — webhook action only.
- `QUERY_ALL_PACKAGES` — to list apps in the rule's app picker.
