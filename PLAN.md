# PocketPage — Plan

## Goal
Hold a landing page in a printed QR code or an NFC tag. Scan/tap → the page shows on the phone with no internet.

## Data format (shared with the web app — don't change without bumping the prefix)
- **payload** = raw DEFLATE (no zlib header) of the page's UTF-8 HTML
- **QR** = `PP1:` + Base45(payload) (RFC 9285). All characters are in the QR alphanumeric set, so the QR uses alphanumeric mode (~97% efficient vs raw bytes).
- **NFC** = one NDEF MIME record, type `application/x-pp`, data = payload. Android dispatches `NDEF_DISCOVERED` for this type to `ViewerActivity`, so tags open with the app closed.

## Architecture
- `codec/PageCodec.kt` — compress/decompress, Base45, QR text, NFC size maths. Pure Kotlin, unit-tested.
- `nfc/TagIo.kt` — read from reader-mode `Tag` or `NDEF_DISCOVERED` intent; write (formats blank tags).
- `qr/QrRenderer.kt` — ZXing `Encoder` → crisp Bitmap + version info.
- `MainActivity` — Compose editor screen; NFC reader mode while open; QR scan via ZXing `ScanContract`.
- `ViewerActivity` — WebView renders the page from memory. Links open in the browser. No file/content access.
- `ui/PocketState` — app-wide state object (survives rotation).

## Capacity
- QR v40-L ≈ 2,860 B payload, but aim for ≤ v25 for easy phone scanning (~1 KB payload).
- NTAG215 504 B, NTAG216 888 B. DEFLATE gives ~2–3× on small HTML.

## Milestones
- **M1** Editor, compress, QR make/save, QR scan → viewer. NFC write + read (reader mode + app-closed tap).
- **M2** Real-world test: printed QR at a few sizes, NTAG215/216 tags, iPhone-readable link-tag option.
- **M3** Nicer authoring: simple template form (title, blurb, button link, colours) that outputs minimal HTML.
- **M4** Optional: brotli/custom dictionary for better compression; multi-QR pages.

## Status
- [x] M1 code written
- [ ] M1 tested on a real phone
- [ ] M2
- [ ] M3
- [ ] M4
