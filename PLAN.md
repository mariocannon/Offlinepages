# PocketPage — Plan

## Goal
Hold a landing page in a printed QR code or an NFC tag. Scan/tap → the page shows on the phone with no internet.

## Data format (shared with the web app — don't change without bumping the prefix)
- **payload** = raw DEFLATE (no zlib header) of the page's UTF-8 HTML
- **QR** = `PP1:` + Base45(payload) (RFC 9285). All characters are in the QR alphanumeric set, so the QR uses alphanumeric mode (~97% efficient vs raw bytes).
- **Link** = `https://mariocannon.github.io/Offlinepages/#p=` + base64url(payload), no padding. Opens on any phone with no app: the hosted viewer (`web/`, GitHub Pages) decodes the part after `#` on the phone. Browsers never send `#…` to the server. First visit needs internet; the service worker caches the viewer so later scans work offline.
- **Book page** (optional) = `<meta name="pocketpage-book" content="Title;page;total">` inside the HTML. Not part of the transport format, so old apps and the web viewer just show the page. Same title (any case) + same total = same book.
- **NFC** = one NDEF MIME record, type `application/x-pp`, data = payload. Android dispatches `NDEF_DISCOVERED` for this type to `ViewerActivity`, so tags open with the app closed.

## Architecture
- `codec/PageCodec.kt` — compress/decompress, Base45, QR text, NFC size maths. Pure Kotlin, unit-tested.
- `nfc/TagIo.kt` — read from reader-mode `Tag` or `NDEF_DISCOVERED` intent; write (formats blank tags).
- `qr/QrRenderer.kt` — ZXing `Encoder` → crisp Bitmap + version info.
- `MainActivity` — hosts two Compose pages: `ReadScreen` (default; QR scan via ZXing `ScanContract`, tap a tag) and `WriteScreen` (editor, QR make, tag write). NFC reader mode while open.
- `ViewerActivity` — WebView renders the page from memory. Links open in the browser. No file/content access.
- `codec/BookMeta.kt` — read/write the book tag, page `<title>`. Pure Kotlin, unit-tested.
- `library/Library.kt` — saved pages, one payload file each in `filesDir/library` (`page-<hash>.pp`, `book-<key>-<n>.pp`). Pure Kotlin, unit-tested.
- `ViewerActivity` bottom bar: Save for single pages; book pages auto-save and get ‹ › through collected pages + a link to the book.
- `ui/LibraryScreen` — My library (books with progress, single pages) and one book (all pages, missing ones locked).
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
- [x] Library: save single pages, collect book pages (app only)
- [ ] Library tested on a real phone
- [ ] M2
- [ ] M3
- [ ] M4
