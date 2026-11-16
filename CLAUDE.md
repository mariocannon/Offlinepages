# CLAUDE.md — PocketPage

Native Android app (Kotlin + Jetpack Compose) that stores a small landing page inside a QR code or NFC tag and renders it fully offline.

**Read `PLAN.md` first.** It has the data format, architecture and milestones. Update its Status checklist as milestones land.

## Hard constraints
- **Offline.** No network calls, no backend, no analytics. Pages render in a WebView from local data only.
- **No Google Play Services.** QR scanning uses ZXing (`zxing-android-embedded`), not ML Kit.
- **$0.**
- **Don't break the data format** (see PLAN.md). It's shared with the web app; `PageCodecTest.readsQrMadeByTheWebApp` guards it.
- Keep `codec/PageCodec.kt` free of Android imports, with unit tests.

## Build
- `./gradlew assembleDebug` builds locally.
- `./gradlew test` runs the codec unit tests.
- CI: `.github/workflows/build.yml` builds a signed release APK on push to `main` and posts it to Releases.
  - Signing comes from repo secrets: `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Distribution: sideload the APK from the Actions artifact or Release.

## Working style
- The owner (Mario) prefers casual, direct communication.
- After each milestone, say what to test on a real phone with a real tag / printed QR.
