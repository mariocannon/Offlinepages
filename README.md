# PocketPage

Android app that stores a small landing page *inside* a QR code or an NFC tag and opens it with no internet.

- **QR**: write or load a page → Make QR → Save to Photos → print → Scan QR in the app.
- **NFC**: Write page to NFC tag → tap the tag. With the app closed, a tap opens the page directly.
- **No app?** Use a **link** QR or link tag. Any phone (iPhone too) opens it in the browser via the viewer at https://mariocannon.github.io/Offlinepages/. The first scan on a phone needs internet; after that it works offline.
- Fully offline. No Google Play Services, no backend, $0.
- Same data format as the PocketPage web app, so codes and tags work in both.
- **PP2 format**: pages are compressed against a built-in dictionary of common HTML and CSS, so small pages take about half the space. Older PP1 codes and tags still open.
- **Themes**: start a page with `<body class=pp-dark>`, `pp-book` or `pp-plain` and the app styles it, so the page carries no CSS. In `pp-book`, `<b>` is a big picture line for emoji.

## Install
Download the latest APK from **Releases** on your phone and open it.

## Sizes
| Tag | Holds | Fits |
|---|---|---|
| NTAG213 | 144 B | a heading and a few lines (~150 B of HTML) |
| NTAG215 | 504 B | ~1.5 KB of HTML (the tiny sample) |
| NTAG216 | 888 B | ~2.5 KB of HTML |
| QR (version 25, M) | ~1.4 K chars | ~3 KB of HTML (the full sample) |
