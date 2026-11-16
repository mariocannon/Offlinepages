package com.mariocannon.pocketpage.codec

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * The PocketPage data format. Pure Kotlin (no Android imports) so it's unit-tested.
 * Same format as the PocketPage web app, so codes and tags work in both.
 *
 *   payload = raw DEFLATE of the page's UTF-8 HTML
 *   QR      = "PP1:" + base45(payload)      -> QR alphanumeric mode
 *   NFC     = MIME record "application/x-pp", bytes = payload
 */
object PageCodec {
    const val QR_PREFIX = "PP1:"
    const val MIME = "application/x-pp"
    private const val MAX_HTML = 2 * 1024 * 1024 // zip-bomb guard

    fun compress(html: String): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION, true)
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
    fun fromQrText(text: String): ByteArray? =
        if (text.startsWith(QR_PREFIX)) Base45.decode(text.substring(QR_PREFIX.length)) else null

    // ---- NFC sizing ----
    /** Bytes a tag needs for this payload: MIME record + NDEF TLV wrapper + terminator. */
    fun nfcBytesNeeded(payloadSize: Int): Int {
        val header = if (payloadSize < 256) 3 else 6
        val record = header + MIME.length + payloadSize
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
