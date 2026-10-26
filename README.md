# NothingNotes ●

**Privacy-first notes for Android** – part of the fan-made Nothing ecosystem.
Pure OLED black, dot-matrix typography, a whisper of red. And not a single byte leaves your phone.

> 🇩🇪 Deutsche Beschreibung weiter unten.

## Features

- **OLED black UI** (`#000000`) in Nothing OS style: dot-matrix titles and timestamps
  (built-in 5×7 font, no font downloads), a subtle dot-grid background, red accents.
- **Notes overview** in grid or list view, pinning, instant search.
- **Editor with Markdown**: headings, **bold**, *italic*, ~~strike~~, `code`, quotes,
  lists, code blocks, rules – with a formatting toolbar and live preview.
- **To-do lists**: `- [ ]` items, tappable checkboxes in preview, progress indicator,
  automatic list continuation on Enter.
- **English & German**: follows the system language, or pick one in Settings
  (also available in Android's per-app language settings on Android 13+).

## Privacy by design

| | |
|---|---|
| 🚫 **No internet permission** | `android.permission.INTERNET` is not requested – and explicitly stripped from the merged manifest (`tools:node="remove"`). Android blocks every socket. CI fails if it ever appears. |
| 📱 **Local only** | Notes live in a local SQLite database (Room). No accounts, no analytics, no crash reporting. |
| 🔐 **Optional AES-256** | AES-256-GCM, key generated inside the **Android Keystore** (hardware-backed where available, non-exportable). |
| 🧹 **Delete all data** | Wipes notes, settings, caches and the encryption key; SQLite `secure_delete` + `VACUUM` scrub freed pages. |
| 💾 **No backups** | `allowBackup="false"` and data-extraction rules exclude everything from cloud backup and device transfer. |
| 🙈 **Screenshot blocking** | `FLAG_SECURE` on by default (toggleable). |

The *Buy Me a Coffee* button only hands the link to your browser after asking – the app itself still has no network access.

## Project structure

```
NothingNotes/
├── build.gradle.kts / settings.gradle.kts / gradle/libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # no permissions
│       │   ├── java/com/real3rr0r303yt/nothingnotes/
│       │   │   ├── NothingNotesApp.kt       # app + manual DI container
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/                    # Room entity, DAO, DB, repositories
│       │   │   ├── security/CryptoManager.kt# AES-256-GCM via Android Keystore
│       │   │   ├── markdown/                # parser, to-do + editing helpers (pure Kotlin)
│       │   │   ├── ui/theme/                # OLED colors, typography
│       │   │   ├── ui/components/           # DotMatrixText, dot grid, top bar …
│       │   │   ├── ui/notes/                # overview (grid/list)
│       │   │   ├── ui/editor/               # editor + Markdown preview
│       │   │   ├── ui/settings/             # language, privacy, wipe, donate
│       │   │   └── ui/navigation/
│       │   └── res/ (values, values-de, xml)
│       └── test/                            # JVM unit tests
└── .github/workflows/android.yml            # build, test, INTERNET-permission check
```

## Build

Requirements: JDK 17+, Android SDK 35.

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

Min SDK 26 (Android 8.0), target SDK 35. Kotlin 2.1, Jetpack Compose (Material 3), Room, DataStore.

## Support

If you like NothingNotes: [☕ Buy Me a Coffee](https://www.buymeacoffee.com/3rr0r303yt)

---

## 🇩🇪 Deutsch

**NothingNotes** ist eine kompromisslos datenschutzfreundliche Notizen-App im Nothing-OS-Look:
tiefschwarzes OLED-Design, Dot-Matrix-Schrift, feine rote Akzente.

- **Keine Internet-Berechtigung** – technisch kann die App keine Daten übertragen.
- **Rein lokale Speicherung** (SQLite/Room), optional **AES-256-verschlüsselt** über den Android Keystore.
- **„Alle Daten löschen“** entfernt Notizen, Einstellungen, Caches und den Schlüssel.
- **Markdown & To-do-Listen**, Raster- oder Listenansicht, Anheften, Suche.
- **Deutsch & Englisch** – folgt der Systemsprache oder lässt sich in den Einstellungen umschalten.

## License

Copyright © 2026 **Real3rr0r303YT**

This program is free software: you can redistribute it and/or modify it under the terms of the
**GNU General Public License v3.0** as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version. See [LICENSE](LICENSE).

NothingNotes is an unofficial fan project and is not affiliated with Nothing Technology Limited.
