# PocketPage — Plan

## Goal
Hold a landing page in a printed QR code or an NFC tag. Scan/tap → the page shows on the phone with no internet.

## Data format (shared with the web app — don't change without bumping the prefix)
- **payload** = raw DEFLATE (no zlib header) of the page's UTF-8 HTML
- **QR** = `PP1:` + Base45(payload) (RFC 9285). All characters are in the QR alphanumeric set, so the QR uses alphanumeric mode (~97% efficient vs raw bytes).
- **Link** = `https://mariocannon.github.io/Offlinepages/#p=` + base64url(payload), no padding. Opens on any phone with no app: the hosted viewer (`web/`, GitHub Pages) decodes the part after `#` on the phone. Browsers never send `#…` to the server. First visit needs internet; the service worker caches the viewer so later scans work offline.
- **NFC** = one NDEF MIME record, type `application/x-pp`, data = payload. Android dispatches `NDEF_DISCOVERED` for this type to `ViewerActivity`, so tags open with the app closed.

## Architecture
- `codec/PageCodec.kt` — compress/decompress, Base45, QR text, NFC size maths. Pure Kotlin, unit-tested.
- `nfc/TagIo.kt` — read from reader-mode `Tag` or `NDEF_DISCOVERED` intent; write (formats blank tags).
- `qr/QrRenderer.kt` — ZXing `Encoder` → crisp Bitmap + version info.
- `MainActivity` — Compose editor screen; NFC reader mode while open; QR scan via ZXing `ScanContract`.
- `ViewerActivity` — WebView renders the page from memory. Links open in the browser. No file/content access.
- `ui/PocketState` — app-wide state object (survives rotation).
- `web/` — static web version (creator + link viewer + service worker), deployed by `.github/workflows/pages.yml`.
- Link tags and link QRs open in `ViewerActivity` when the app is installed (NDEF URI filter; VIEW filter needs "Open supported links" on Android 12+ since github.io can't be verified without an assetlinks.json at the domain root).

## Capacity
- QR v40-L ≈ 2,860 B payload, but aim for ≤ v25 for easy phone scanning (~1 KB payload).
- NTAG215 504 B, NTAG216 888 B. DEFLATE gives ~2–3× on small HTML.

## Milestones
- **M1** Editor, compress, QR make/save, QR scan → viewer. NFC write + read (reader mode + app-closed tap).
- **M2** Real-world test: printed QR at a few sizes, NTAG215/216 tags. Link mode so phones without the app can open pages.
- **M3** Nicer authoring: simple template form (title, blurb, button link, colours) that outputs minimal HTML.
- **M4** Optional: brotli/custom dictionary for better compression; multi-QR pages.

## Status
- [x] M1 code written
- [x] M1 tested on a real phone
- [x] Link mode (no app needed): web viewer on GitHub Pages, link QR, link tags
- [ ] Link mode tested on a real phone (Android + iPhone)
- [ ] M2
- [ ] M3
- [ ] M4
