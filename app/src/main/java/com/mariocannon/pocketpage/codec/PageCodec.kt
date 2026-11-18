package com.mariocannon.pocketpage.codec

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * The PocketPage data format. Pure Kotlin (no Android imports) so it's unit-tested.
 * Same format as the PocketPage web app, so codes and tags work in both.
 *
 *   payload = raw DEFLATE of the page's UTF-8 HTML, compressed against [Pp2Dictionary]
 *   QR      = "PP2:" + base45(payload)      -> QR alphanumeric mode
 *   NFC     = external-type record "pp:2", bytes = payload
 *
 * PP1 (the first format: no dictionary, "PP1:", MIME "application/x-pp", link "#p=") still opens.
 * [decompress] reads both: it always loads the dictionary, and a PP1 stream never points into it.
 */
object PageCodec {
    const val QR_PREFIX = "PP2:"
    private const val QR_PREFIX_V1 = "PP1:"

    /** NFC external type "pp:2" (4 bytes on the tag; the PP1 MIME type took 16). */
    const val NFC_DOMAIN = "pp"
    const val NFC_EXT = "2"
    const val NFC_TYPE = "$NFC_DOMAIN:$NFC_EXT"
    /** PP1 tags. Still read, no longer written. */
    const val MIME_V1 = "application/x-pp"
    private const val MAX_HTML = 2 * 1024 * 1024 // zip-bomb guard

    fun compress(html: String): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION, true)
        d.setDictionary(Pp2Dictionary.BYTES)
        d.setInput(html.toByteArray(Charsets.UTF_8))
        d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }

    fun decompress(payload: ByteArray): String {
        val inf = Inflater(true)
        inf.setDictionary(Pp2Dictionary.BYTES) // raw streams take it up front; harmless for PP1
        inf.setInput(payload + byteArrayOf(0)) // nowrap mode wants a trailing dummy byte
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        try {
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    throw DataFormatException("data is cut short")
                }
                out.write(buf, 0, n)
                if (out.size() > MAX_HTML) throw DataFormatException("page too large")
            }
        } finally {
            inf.end()
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }

    // ---- QR text ----
    fun toQrText(payload: ByteArray): String = QR_PREFIX + Base45.encode(payload)

    /** Returns the payload, or null if the text isn't a PocketPage QR. Throws if it's damaged. */
    fun fromQrText(text: String): ByteArray? {
        val prefix = listOf(QR_PREFIX, QR_PREFIX_V1).firstOrNull { text.startsWith(it) } ?: return null
        return Base45.decode(text.substring(prefix.length))
    }

    // ---- Link (opens on any phone, no app needed) ----
    /** Hosted viewer (GitHub Pages). Everything after # stays on the phone; servers never see it. */
    const val LINK_BASE = "https://mariocannon.github.io/Offlinepages/"
    private const val LINK_KEY = "#p2="
    private const val LINK_KEY_V1 = "#p="

    fun toLink(payload: ByteArray): String =
        LINK_BASE + LINK_KEY + Base64.getUrlEncoder().withoutPadding().encodeToString(payload)

    /** Returns the payload, or null if the URL isn't a PocketPage link. Throws if it's damaged. */
    fun fromLink(url: String): ByteArray? {
        val key = listOf(LINK_KEY, LINK_KEY_V1).firstOrNull { it in url } ?: return null
        val data = url.substring(url.indexOf(key) + key.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
        if (data.isEmpty()) return null
        return Base64.getUrlDecoder().decode(data)
    }

    /** Bytes a tag needs for a link: URI record (https:// is a 1-byte prefix) + TLV + terminator. */
    fun nfcLinkBytesNeeded(payload: ByteArray): Int {
        val uriBytes = 1 + toLink(payload).removePrefix("https://").length
        val record = (if (uriBytes < 256) 3 else 6) + 1 + uriBytes
        val tlv = if (record < 255) 2 else 4
        return record + tlv + 1
    }

    // ---- NFC sizing ----
    /** Bytes a tag needs for this payload: "pp:2" record + NDEF TLV wrapper + terminator. */
    fun nfcBytesNeeded(payloadSize: Int): Int {
        val header = if (payloadSize < 256) 3 else 6
        val record = header + NFC_TYPE.length + payloadSize
        val tlv = if (record < 255) 2 else 4
        return record + tlv + 1
    }
}

/** Base45 (RFC 9285): packs bytes into the QR alphanumeric character set. */
object Base45 {
    private const val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder((bytes.size + 1) / 2 * 3)
        var i = 0
        while (i < bytes.size) {
            if (i + 1 < bytes.size) {
                val x = (bytes[i].toInt() and 0xFF) * 256 + (bytes[i + 1].toInt() and 0xFF)
                sb.append(CHARS[x % 45]).append(CHARS[(x / 45) % 45]).append(CHARS[x / 2025])
            } else {
                val a = bytes[i].toInt() and 0xFF
                sb.append(CHARS[a % 45]).append(CHARS[a / 45])
            }
            i += 2
        }
        return sb.toString()
    }

    fun decode(text: String): ByteArray {
        fun idx(c: Char): Int {
            val n = CHARS.indexOf(c)
            require(n >= 0) { "bad base45 character '$c'" }
            return n
        }
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            require(i + 1 < text.length) { "bad base45 length" }
            val c = idx(text[i])
            val d = idx(text[i + 1])
            if (i + 2 < text.length) {
                val x = c + d * 45 + idx(text[i + 2]) * 2025
                require(x <= 0xFFFF) { "bad base45 value" }
                out.write(x shr 8)
                out.write(x and 0xFF)
            } else {
                val x = c + d * 45
                require(x <= 0xFF) { "bad base45 value" }
                out.write(x)
            }
            i += 3
        }
        return out.toByteArray()
    }
}
