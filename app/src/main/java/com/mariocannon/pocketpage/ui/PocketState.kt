package com.mariocannon.pocketpage.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.nfc.NdefMessage
import com.mariocannon.pocketpage.qr.QrImage

enum class Page { Read, Write }

/** App-wide UI state. Lives outside the Activity so it survives rotation. */
object PocketState {
    var page by mutableStateOf(Page.Read)
    var nfcOn by mutableStateOf(false)
    var html by mutableStateOf("")
    var qr by mutableStateOf<QrImage?>(null)
    var qrError by mutableStateOf<String?>(null)
    var qrIsLink by mutableStateOf(false)
    var nfcStatus by mutableStateOf("")

    /** Set when the user taps "Write tag"; the next tag seen gets this payload. */
    @Volatile var pendingWrite: NdefMessage? = null
    var writing by mutableStateOf(false)

    fun setPage(newHtml: String) {
        html = newHtml
        qr = null
        qrError = null
    }
}
