# PrivacyShield

A privacy and security app for Android 14 that helps you inspect installed apps, detect risky permissions, and manage a protected-apps list.

## Features

- **Dashboard** — instant overview of dangerous permissions, accessibility services, overlay permission, screen-recording risk, and protected app count
- **App scanner** — lists all installed apps with risk level, permission details, and category filters
- **Protected apps** — mark apps as sensitive; PrivacyShield highlights other apps that may interact with them
- **Settings** — toggle scan categories and alerts; fully offline, no telemetry

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Persistence | DataStore Preferences |
| Navigation | Navigation Compose |
| Build | Gradle Kotlin DSL |
| Min SDK | 26 (Android 8) |
| Target SDK | 34 (Android 14) |

## Building

### Requirements

- JDK 17
- Android SDK with platform 34 installed
- `local.properties` containing your `sdk.dir` path (copy from `local.properties.example`)

### Debug build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions

Every push and pull request to `main` triggers the CI workflow at `.github/workflows/build.yml`, which builds the debug APK and uploads it as an artifact.

## Pushing changes

A convenience script is included:

```bash
python3 push.py "your optional commit message"
```

Set `GITHUB_PERSONAL_ACCESS_TOKEN` in your environment for authenticated HTTPS pushes:

```bash
export GITHUB_PERSONAL_ACCESS_TOKEN=ghp_xxxxxxxxxxxx
python3 push.py
```

The token is never printed or stored.

## Privacy

PrivacyShield is fully offline. It does not make any network requests, does not collect analytics, and does not transmit any data.

## License

MIT
