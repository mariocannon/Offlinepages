# PocketPage (Android)

Native offline app: stores a small landing page inside a QR code or an NFC tag and opens it with no internet.

- **QR**: Make QR → Save to Photos → print → Scan QR in the app.
- **NFC**: Write page to NFC tag → tap the tag. Works with the app closed too:
  Android sees the `application/x-pp` record and opens the page straight away.
- Same data format as the PocketPage web app, so codes and tags work in both.

## Get the APK (no Android Studio needed)
1. Create a new GitHub repo and upload this folder's contents (including `.github/`).
2. Open the **Actions** tab → "Build APK" runs automatically (about 4 min).
3. Open the finished run → download **PocketPage-apk** → unzip → copy `app-debug.apk` to your phone.
4. Open it on the phone and allow "Install unknown apps" for your file manager.

## Or with Android Studio
File → Open → this folder → Run ▶ with the phone plugged in (USB debugging on).

## Tag sizes
| Tag | Holds | Fits |
|---|---|---|
| NTAG213 | 144 B | too small |
| NTAG215 | 504 B | tiny sample (~450 B) |
| NTAG216 | 888 B | ~2.5 KB of HTML |
