# PocketPage — Plan

## Goal
Hold a landing page in a printed QR code or an NFC tag. Scan/tap → the page shows on the phone with no internet.

## Data format (shared with the web app — don't change without bumping the prefix)
Current format is **PP2**. PP1 codes, links and tags still open.
- **payload** = raw DEFLATE (no zlib header) of the page's UTF-8 HTML, compressed against the **PP2 dictionary** (`codec/Pp2Dictionary.kt`, same text in `web/codec.js`). The dictionary is 14,364 B of text small pages nearly always contain: HTML tags, CSS, common English words and phrases, NZ places and te reo Māori, opening hours and prices, and words for common topics (tech and radio, outdoors, cafés and hire, trades and safety, events, kids' stories), so the compressor points into it instead of storing those bytes. It is **frozen**: changing one byte breaks every PP2 code and tag. A better one means PP3.
- **QR** = `PP2:` + Base45(payload) (RFC 9285). All characters are in the QR alphanumeric set, so the QR uses alphanumeric mode (~97% efficient vs raw bytes).
- **Link** = `https://mariocannon.github.io/Offlinepages/#p2=` + base64url(payload), no padding. Opens on any phone with no app: the hosted viewer (`web/`, GitHub Pages) decodes the part after `#` on the phone. Browsers never send `#…` to the server. First visit needs internet; the service worker caches the viewer so later scans work offline.
- **NFC** = one NDEF external-type record, type `pp:2` (4 B on the tag), data = payload. Android dispatches `NDEF_DISCOVERED` for this type to `ViewerActivity`, so tags open with the app closed.
- **PP1** (first format) = same but no dictionary, `PP1:`, `#p=`, MIME record `application/x-pp`. Readers accept both. One decoder handles both: it always loads the dictionary, and a PP1 stream never points into it. Only PP2 is written.
- **Themes** (optional) = `<body class=pp-dark>`, `pp-book` or `pp-plain`. The viewer adds that theme's stylesheet (and a viewport tag) before showing the page, so the page doesn't carry its CSS. Not part of the transport format; pages without a `pp-` class are shown untouched. CSS lives in `codec/Themes.kt` and `web/codec.js` — keep them the same.
- **Book page** (optional) = `<meta name="pocketpage-book" content="Title;page;total">` inside the HTML. Not part of the transport format, so old apps and the web viewer just show the page. Same title (any case) + same total = same book.

## Architecture
- `codec/PageCodec.kt` — compress/decompress, Base45, QR text, NFC size maths. Pure Kotlin, unit-tested.
- `codec/Pp2Dictionary.kt` — the frozen PP2 dictionary. `codec/Themes.kt` — built-in themes. Pure Kotlin, unit-tested.
- `nfc/TagIo.kt` — read from reader-mode `Tag` or `NDEF_DISCOVERED` intent; write (formats blank tags).
- `qr/QrRenderer.kt` — ZXing `Encoder` → crisp Bitmap + version info.
- `MainActivity` — hosts two Compose pages: `ReadScreen` (default; QR scan via ZXing `ScanContract`, tap a tag) and `WriteScreen` (editor, QR make, tag write). NFC reader mode while open.
- `ViewerActivity` — WebView renders the page from memory. Links open in the browser. No file/content access.
- `codec/BookMeta.kt` — read/write the book tag, page `<title>`. Pure Kotlin, unit-tested.
- `library/Library.kt` — saved pages, one payload file each in `filesDir/library` (`page-<hash>.pp`, `book-<key>-<n>.pp`). Pure Kotlin, unit-tested.
- `ViewerActivity` bottom bar: Save for single pages; book pages auto-save and get ‹ › through collected pages + a link to the book.
- `ui/LibraryScreen` — My library (books with progress, single pages) and one book (all pages, missing ones locked).
- `ui/PocketState` — app-wide state object (survives rotation).
- `web/` — static web version (creator + link viewer + service worker), deployed by `.github/workflows/pages.yml`. `web/codec.js` is the format (dictionary, themes, Base45, links); it uses vendored pako because the browser's CompressionStream can't take a dictionary. `node tools/web-codec-test.mjs` checks it against fixtures made by the app's compressor.
- Link tags and link QRs open in `ViewerActivity` when the app is installed (NDEF URI filter; VIEW filter needs "Open supported links" on Android 12+ since github.io can't be verified without an assetlinks.json at the domain root).

## Capacity
- QR v40-L ≈ 2,860 B payload, but aim for ≤ v25 for easy phone scanning (~1 KB payload).
- NTAG213 144 B, NTAG215 504 B, NTAG216 888 B. DEFLATE gives ~2–3× on small HTML; the PP2 dictionary takes a further 20–30% off (about half on pages under 200 B). A PP2 tag costs payload + 10 B (PP1: + 22 B).

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
- [x] PP2: shared dictionary, built-in themes, 4-byte NFC type (code + unit tests)
- [ ] PP2 tested on a real phone (new tag opens with app closed, old PP1 tags/QRs still open)
- [ ] M2
- [ ] M3
- [ ] M4
