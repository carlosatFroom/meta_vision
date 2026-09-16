# SMS Vision

Read your SMS messages through your Meta Ray-Ban glasses — no Meta AI app restrictions, no contact uploads, no cloud dependency.

## Why this exists

The Meta AI companion app only mirrors notifications from Facebook, Instagram, and Messenger. It also requires access to your contacts to use phone features. SMS Vision sidesteps all of that by working as a standalone Android app that reads your text messages aloud through your glasses' Bluetooth speakers.

## How it works

```
SMS arrives
    │
    ▼
NotificationListenerService
captures the notification
    │
    ▼
ContactResolver
resolves phone numbers to names
using your LOCAL contacts database
    │
    ▼
GlassReaderState
queues the message for TTS
    │
    ▼
TextToSpeech engine
speaks through Bluetooth SCO
(Hands-Free Profile audio)
    │
    ▼
Meta Ray-Ban speakers
"Message from Mom: running 10 min late"
```

### Components

| File | Role |
|------|------|
| `SmsNotificationListener` | Android `NotificationListenerService` that intercepts SMS notifications from messaging apps (Google Messages, Samsung Messages, stock MMS, OnePlus Messages) |
| `ContactResolver` | Resolves phone numbers to contact display names via `ContactsContract.PhoneLookup` — entirely on-device |
| `GlassReaderState` | Singleton coordinating the message queue, TTS engine, and Bluetooth SCO audio routing |
| `MainActivity` | Compose UI with master toggle, permission status indicators, and a message log |

### Audio routing

The glasses connect to your phone as a standard Bluetooth HFP (Hands-Free Profile) device. The app routes TTS output through Bluetooth SCO (Synchronous Connection-Oriented link), which is the same audio channel used for phone calls. This means the voice comes through the glasses' open-ear speakers, not your phone.

### Privacy model

- **Zero internet permissions** — nothing can leave your device
- **Contacts are read locally** via `ContactsContract` — never uploaded, never cached outside the system
- **Notification access** is scoped to SMS packages only — other notifications are ignored
- All processing happens on-device: notification capture, contact lookup, text-to-speech

## Setup

### Prerequisites

- Android phone (minSdk 29 / Android 10+) — bumped from 26 to meet the DAT 0.8.0 requirement (the Meta AI app itself requires Android 10+)
- Meta Ray-Ban **Display** glasses paired via Bluetooth (for the card path; TTS works on any Ray-Ban Meta variant)
- Meta AI app v272+ with glasses firmware v125+ and Developer Mode enabled
- JDK 17 for building
- A GitHub personal access token with `read:packages` scope (for the DAT SDK on GitHub Packages)

### Build

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Permissions

The app will guide you through granting these on first launch:

| Permission | Why | Required? |
|-----------|-----|-----------|
| Notification Access | Read SMS notifications as they arrive | Yes |
| Bluetooth Connect | Route TTS audio to your glasses | Yes |
| Read Contacts | Resolve phone numbers to names | Optional — falls back to showing the number |

## Display on glasses (DAT 0.8.0)

The app now renders incoming SMS as a notification card on **Meta Ray-Ban Display** glasses via Meta's Wearables Device Access Toolkit (DAT 0.8.0), in parallel with the existing TTS read-aloud path.

### How it works

```
SMS arrives
    │
    ▼
NotificationListenerService captures the notification
    │
    ▼
ContactResolver resolves phone numbers to names
    │
    ▼
GlassReaderState.onSmsReceived()
    ├──> DisplaySessionManager.showMessage()  ->  glasses display card
    └──> messageQueue -> TTS -> Bluetooth SCO -> glasses speakers
```

When the glasses display is connected (`DisplaySessionManager.connectionState == DISPLAY_READY`), each incoming SMS is pushed to the glasses as a card built with the DAT `FlexBox` declarative layout DSL — a "New message" caption, the sender, the body, a timestamp, and a Dismiss button. The TTS read-aloud path runs in parallel, so the wearer both sees and hears the message. If the display isn't connected, only TTS runs (the original behavior).

### Components

| File | Role |
|------|------|
| `DisplaySessionManager` | Owns the Wearables session + display capability lifecycle: registration with Meta AI, session start, display attach, state observation, teardown |
| `SmsCardView` | Builds the glasses-display layout tree for an incoming SMS — `FlexBox` column with `Text`/`Button` leaves, sent via `Display.sendContent { ... }` |
| `GlassReaderApp` | Initializes `Wearables.initialize(this)` at process start |
| `MainActivity` | "Register"/"Connect" button drives `DisplaySessionManager.connect(this)`; status row shows display connection state |

### Setup

The Wearables Device Access Toolkit needs to be installed on the glasses and the app needs to register with the Meta AI app:

1. Update the Meta AI app to v272+ and glasses firmware to v125+.
2. Enable **Developer Mode** in the Meta AI app (Settings → App Info → tap App Version 5×). This lets the app register and run without published credentials.
3. Build and install the app — see Build below.
4. Open the app, tap **Register**. This deeplinks to the Meta AI app to confirm the connection, then returns via the `metavision://` callback scheme.
5. Tap **Connect** to start a Wearables session and attach the display capability. The status row flips to "Ready — cards will appear".
6. Send yourself an SMS. The card renders on the glasses and the message is read aloud.

### Credentials

For local development with Developer Mode enabled, `APPLICATION_ID` and `CLIENT_TOKEN` are both set to `"0"` in `app/build.gradle.kts` via `manifestPlaceholders`. To distribute the app outside Developer Mode, register a project in the [Wearables Developer Center](https://wearables.developer.meta.com/) and replace those placeholders with your real credentials.

### Gradle access

The DAT SDK is hosted on GitHub Packages, so Gradle needs a personal access token with `read:packages` scope to download it. Add this to `local.properties` (do not commit it):

```
github_token=ghp_yourPersonalAccessToken
```

Or export `GITHUB_TOKEN` in your shell. `settings.gradle.kts` reads from either source.

### Display design notes

The Ray-Ban Display is an additive-light waveguide — it can only brighten, never occlude. Dark pixels render transparent. The card is intentionally sparse (bright text on the default background) so it reads well against any scene. Each `sendContent()` call replaces the entire display; there are no incremental updates. The back gesture (two-finger tap on the temple) ends the display session.

### Privacy model

- **Zero cloud dependency for SMS content** — capture, contact lookup, and TTS all stay on-device, as before
- **The glasses display receives the rendered card over Bluetooth** — sender, body, and timestamp are transmitted to the glasses via the DAT SDK to render locally; no third-party server is in the path
- **`INTERNET` permission is now required** by the DAT SDK for its transport; SMS content itself never leaves the device over it
- **Contacts are still read locally** via `ContactsContract` — never uploaded

## License

Personal use. Not affiliated with Meta.
