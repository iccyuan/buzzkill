# 静界 / Hush

A native Android notification-control app: define **rules** that match incoming
notifications and run **actions** on them — discard, silence, rewrite text, change
importance, auto-reply, read aloud, snooze, digest, show as scrolling **danmaku**,
forward to a webhook, run a Tasker task, and more. The goal is a quieter, calmer
notification shade you stay in control of.

This is a clean-room implementation written from scratch in Kotlin + Jetpack Compose.
It contains no third-party app's code or assets — only an original realization of the
same feature set.

> The package id is still `com.buzzkill` for install-continuity; the user-facing name
> is 静界 / Hush.

## Features

A rule = **apps** + **triggers** + **conditions** + **actions**.

**App scope** — pick specific apps (empty = all). The picker enumerates launchable
apps across every user profile, so **app clones (应用分身)** show as distinct "(分身)"
tiles; a "show system apps" toggle reveals non-launchable system packages.

**Triggers** (ALL/ANY logic; empty = match every notification)
- Text match on any field — title, text, expanded/sub/info text, ticker, app name,
  and the **category / channel / sender** metadata — with
  `contains / equals / starts / ends / regex / wildcard`, case sensitivity and NOT.
- Ongoing vs dismissible state.
- Has an inline reply action (chat notifications).

**Conditions** (AND-gated context checks)
- Time-of-day + day-of-week window (wraps past midnight).
- China public-holiday awareness (legal holiday / make-up workday / weekend / workday),
  with a one-tap "today is rest / work" override and network-refreshable holiday data.
- Charging / on battery; screen on / off; battery above/below a threshold.
- Cooldown (rate-limit how often a rule fires).

**Actions** (run in order)
- Discard (hide entirely) / Dismiss (optionally delayed) / Snooze.
- Replace text (literal or regex with `$1` back-refs) / Set a field from a template.
- Change importance + bypass Do Not Disturb.
- Sound & vibration override (silence or custom buzz pattern).
- Auto-reply via the notification's RemoteInput / Read aloud (TTS) / Wake screen / Toast.
- Digest — collect noisy notifications and post one rolled-up summary per window.
- Mute the source app for N minutes / Set a variable / Run a Tasker task / Fire a webhook.

**Danmaku** — a per-rule switch shows a matched notification as a translucent
scrolling overlay bar instead of the native notification (requires the Discard action
and the overlay permission). A global **Immersive danmaku** mode (off by default)
auto-suppresses native notifications and shows them as danmaku while you're fullscreen
(landscape video / games).

**Templates** — action text supports placeholders:
`{title} {text} {bigtext} {subtext} {ticker} {app} {package}`, regex captures
`{1}…{9}`, and user variables `{var:name}`.

**Other**
- Master on/off switch + Quick Settings tile; per-rule enable; "stop processing".
- Notification history (ongoing and non-clearable notifications like VPN are skipped)
  and Insights (totals, busiest apps, top rules).
- Per-rule live preview + an interactive simulator in the editor.
- Export/import all rules as JSON; in-app update check against GitHub Releases.
- Light/dark/system theme, English/中文, hide-from-recents.

## Architecture

```
data/        Room entities + DAO, sealed Trigger/Condition/Action models
             (kotlinx.serialization, stored as JSON columns), repository, settings,
             holiday provider, installed-apps + update checker
engine/      Pure, Android-free rule evaluation: RuleEngine, TextMatcher,
             TemplateEngine, VariableStore, Decision/MatchContext types
service/     NotificationListenerService, notification rebuild/repost, channel
             manager, TTS, side-effect executor, auto-reply, danmaku overlay,
             digest, device-state sampling
ui/          Compose: rule list, rule editor (trigger/condition/action dialogs +
             app picker), history, insights, settings, theming, navigation
util/        Logger and shared helpers
```

How modification works: a `NotificationListenerService` cannot edit another app's
notification in place, so a matched notification is **rebuilt under our own channel**
(to control alerting), the original is cancelled, and the rebuilt copy is reposted —
preserving icon, content intent, actions and styling, and showing the original app's
name via the substitute-name extra.

OEM note: aggressive battery managers (ColorOS / MIUI etc.) can kill the listener.
The service requests a rebind on disconnect and when the app is opened, and Settings
surfaces a live **connection status** (connected / disconnected / no access). For
reliability, exempt the app from battery optimization.

## Build

Requires the Android SDK (API 35) and JDK 17+.

1. Open the project in **Android Studio** (Ladybug / 2024.2+), or create
   `local.properties` with `sdk.dir=/path/to/Android/Sdk`.
2. Build & run the `app` configuration, or from a terminal:
   ```
   ./gradlew assembleDebug          # or installDebug to a connected device
   ```
   The APK is named `Hush-<version>-<buildType>.apk`.
3. On the device, open the app and grant **Notification access**
   (*Settings → Notifications → Notification access → 静界/Hush*). For danmaku, also
   grant "display over other apps" (and "background pop-up" on ColorOS).

## Release

Pushing a `v*` git tag triggers the GitHub Actions release workflow: it derives the
version from the tag (`v1.2.3` → `1.2.3`), builds a signed release APK, and publishes
a **Hush-`<version>`** GitHub Release with the APK attached. Signing credentials come
from repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_STORE_PASSWORD`,
`KEYSTORE_KEY_ALIAS`, `KEYSTORE_KEY_PASSWORD`).

## Permissions

- **Notification access** (the core; granted in system settings).
- `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE` — for
  reposted/alerting notifications and reliable background operation.
- `INTERNET` — holiday-data refresh, in-app update check, and the webhook action.
- `SYSTEM_ALERT_WINDOW` — the danmaku overlay.
- `RECEIVE_BOOT_COMPLETED` — re-establish state after reboot.
- `QUERY_ALL_PACKAGES` — to list apps in the rule's app picker.
