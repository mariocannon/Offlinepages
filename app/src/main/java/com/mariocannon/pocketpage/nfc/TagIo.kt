package com.mariocannon.pocketpage.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import com.mariocannon.pocketpage.codec.PageCodec

/** Reading and writing PocketPage NFC tags. */
object TagIo {

    fun message(payload: ByteArray) = NdefMessage(NdefRecord.createMime(PageCodec.MIME, payload))

    fun findPayload(messages: List<NdefMessage>): ByteArray? {
        for (m in messages) for (r in m.records) {
            if (r.tnf == NdefRecord.TNF_MIME_MEDIA && String(r.type, Charsets.US_ASCII) == PageCodec.MIME) {
                return r.payload
            }
        }
        return null
    }

    /** Payload from a tag seen in reader mode, or null if it holds no PocketPage. */
    fun read(tag: Tag): ByteArray? {
        val msg = Ndef.get(tag)?.cachedNdefMessage ?: return null
        return findPayload(listOf(msg))
    }

    /** Payload from an NDEF_DISCOVERED intent (tag tapped while the app was closed). */
    fun read(intent: Intent): ByteArray? {
        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null
        val raw: Array<*>? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }
        return findPayload(raw?.filterIsInstance<NdefMessage>() ?: return null)
    }

    /** Writes the page. Returns bytes used. Throws with a readable message on failure. */
    fun write(tag: Tag, payload: ByteArray): Int {
        val msg = message(payload)
        val size = msg.byteArrayLength
        Ndef.get(tag)?.let { ndef ->
            ndef.connect()
            try {
                check(ndef.isWritable) { "Tag is locked (read-only)" }
                check(ndef.maxSize >= size) { "Too big: page needs $size B, tag holds ${ndef.maxSize} B" }
                ndef.writeNdefMessage(msg)
            } finally {
                ndef.close()
            }
            return size
        }
        NdefFormatable.get(tag)?.let { f ->
            f.connect()
            try {
                f.format(msg)
            } finally {
                f.close()
            }
            return size
        }
        error("This tag type can't store NDEF data")
    }
}
