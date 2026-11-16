package com.mariocannon.pocketpage.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

data class QrImage(val bitmap: Bitmap, val version: Int, val modules: Int, val chars: Int) {
    /** ~0.5 mm per module scans reliably off paper with a phone. */
    val minPrintCm: Int get() = (modules * 0.5 / 10).toInt() + 1
    val difficulty: String
        get() = when {
            version <= 20 -> "easy scan"
            version <= 30 -> "print it large"
            else -> "very dense, hard to scan"
        }
}

object QrRenderer {
    /** Throws com.google.zxing.WriterException if the text is too big for a QR code. */
    fun render(text: String, quietZone: Int = 4, scale: Int = 10): QrImage {
        // All-alphanumeric text, so ZXing picks the compact alphanumeric mode.
        val code = Encoder.encode(text, ErrorCorrectionLevel.M)
        val m = code.matrix
        val n = m.width + quietZone * 2
        val px = IntArray(n * n) { Color.WHITE }
        for (y in 0 until m.height) for (x in 0 until m.width) {
            if (m.get(x, y).toInt() == 1) px[(y + quietZone) * n + (x + quietZone)] = Color.BLACK
        }
        val small = Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createScaledBitmap(small, n * scale, n * scale, false)
        return QrImage(bitmap, code.version.versionNumber, m.width, text.length)
    }
}
