# RIDGE — Wi-Fi Walkie-Talkie

Built for the dead zone. Group voice for trekking parties in cell-signal blackspots.

## M1 (current — UI + plumbing)
- 7 Compose screens pixel-faithful to v2 design (splash/perms, empty, talk-hold,
  talk-vox, audio-sheet, group-setup, settings, sos)
- Day + Night theme (amber primary at night to preserve dark vision)
- Foreground service for screen-off operation
- BT + Wi-Fi Direct discovery wired (finds nearby phones)
- Audio output routing (earpiece / speaker / BT headset / wired)
- PTT haptic + visual feedback
- Settings persistence ready (DataStore dep included; M2 wiring)
- Permission flow: mic + BT scan/connect + nearby Wi-Fi + notifications

## M2 (next session)
- Real voice transport over UDP between phones
- Opus encoder integration
- SOS broadcast with GPS
- QR code generator + scanner (currently visual placeholder)
- Group join handshake protocol
- DataStore persistence of settings

## Build
GitHub Actions builds the debug APK on every push. Local build needs Android
SDK + JDK 17 + Gradle 8.5.

## Demo paths inside the app
- Tap **channel name** ("Annapurna Base") on Talk screen → opens SOS overlay
- Tap **gear icon** top-right of Talk → Settings
- Tap **output pill** ("Speaker") → audio output sheet
- Tap **Hands-free** in segmented tabs → VOX listening ring

## Project layout
```
app/src/main/java/app/ridge/
├── MainActivity.kt              # NavHost, perms, lifecycle
├── RidgeApp.kt                  # Notification channels
├── core/
│   ├── AppState.kt              # Single StateFlow store
│   ├── AudioRouter.kt           # AudioManager + BT routing
│   ├── Discovery.kt             # BLE + Wi-Fi P2P discovery
│   └── RadioService.kt          # Foreground service
└── ui/
    ├── theme/Tokens.kt          # Day/Night color/type/shape tokens
    ├── components/
    │   ├── Components.kt        # Card, Chip, Button, Toggle, Seg, etc.
    │   ├── PttButton.kt         # Hero hold-to-talk dial
    │   └── RidgeLogo.kt         # Triple-arc SVG-equivalent in Canvas
    └── screens/
        ├── Splash.kt
        ├── Empty.kt
        ├── Talk.kt              # Both Hold + VOX states
        ├── AudioSheet.kt
        ├── GroupSetup.kt
        ├── Settings.kt
        └── Sos.kt
```
