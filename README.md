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

- Android phone (minSdk 26 / Android 8.0+)
- Meta Ray-Ban glasses paired via Bluetooth
- JDK 17 for building

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

## Future: display on glasses

The [Meta Wearables Device Access Toolkit](https://wearables.developer.meta.com/docs/develop/) (DAT SDK v0.5.0) currently only exposes camera streaming and photo capture. There is no third-party API to render content on the glasses display.

However, the SDK is in active developer preview and evolving quickly. When Meta opens up a display/notification API, this app is structured to support it:

- `SmsMessage` already carries sender, body, and timestamp — ready to be rendered as a notification card
- `GlassReaderState.onSmsReceived()` is the single entry point where messages arrive — a display output can be added alongside TTS
- The architecture cleanly separates **message capture** (NotificationListener + ContactResolver) from **message delivery** (currently TTS, future: glasses HUD)

The integration point would look something like:

```kotlin
// In GlassReaderState.onSmsReceived(), alongside the existing TTS path:
fun onSmsReceived(message: SmsMessage) {
    recentMessages.add(0, message)

    // Existing: read aloud via TTS
    if (isEnabled && isTtsReady) {
        messageQueue.offer(message)
        if (!isSpeaking) processNextMessage()
    }

    // Future: display on glasses when DAT SDK supports it
    // glassesDisplay?.showNotificationCard(
    //     title = message.sender,
    //     body = message.body,
    // )
}
```

Track the SDK's progress:
- [DAT SDK changelog](https://github.com/facebook/meta-wearables-dat-android/blob/main/CHANGELOG.md)
- [Feature requests & discussions](https://github.com/facebook/meta-wearables-dat-android/discussions)
- [Developer documentation](https://wearables.developer.meta.com/docs/develop/)

## License

Personal use. Not affiliated with Meta.
