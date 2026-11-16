package com.mariocannon.pocketpage

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.mariocannon.pocketpage.codec.PageCodec
import com.mariocannon.pocketpage.nfc.TagIo
import com.mariocannon.pocketpage.qr.QrRenderer
import com.mariocannon.pocketpage.ui.PocketActions
import com.mariocannon.pocketpage.ui.PocketScreen
import com.mariocannon.pocketpage.ui.PocketState
import com.mariocannon.pocketpage.ui.PocketTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfc: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfc = NfcAdapter.getDefaultAdapter(this)
        if (PocketState.html.isEmpty()) PocketState.setPage(asset("tiny.html"))

        setContent {
            PocketTheme {
                val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
                    result.contents?.let { openFromQr(it) }
                }
                val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let { loadFile(it) }
                }
                PocketScreen(
                    hasNfc = nfc != null,
                    actions = PocketActions(
                        onFullSample = { PocketState.setPage(asset("full.html")) },
                        onTinySample = { PocketState.setPage(asset("tiny.html")) },
                        onOpenFile = { filePicker.launch("text/*") },
                        onPreview = { openViewer(PageCodec.compress(PocketState.html)) },
                        onScan = {
                            scanner.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt("Point at a PocketPage QR code")
                                    .setBeepEnabled(false)
                                    .setOrientationLocked(false)
                            )
                        },
                        onMakeQr = ::makeQr,
                        onSaveQr = ::saveQr,
                        onWriteTag = ::startWrite,
                        onCancelWrite = ::cancelWrite,
                    ),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reader mode: while this screen is open, every tag comes to onTagDiscovered.
        nfc?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    override fun onPause() {
        super.onPause()
        nfc?.disableReaderMode(this)
    }

    // ---------- page ----------
    private fun asset(name: String): String =
        assets.open(name).use { it.readBytes().toString(Charsets.UTF_8) }.trim()

    private fun loadFile(uri: Uri) {
        val text = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        if (text != null) PocketState.setPage(text) else toast("Couldn't read that file")
    }

    private fun openViewer(payload: ByteArray) {
        startActivity(Intent(this, ViewerActivity::class.java).putExtra(ViewerActivity.EXTRA_PAYLOAD, payload))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    // ---------- QR ----------
    private fun makeQr() {
        val text = PageCodec.toQrText(PageCodec.compress(PocketState.html))
        try {
            PocketState.qr = QrRenderer.render(text)
            PocketState.qrError = null
        } catch (e: WriterException) {
            PocketState.qr = null
            PocketState.qrError = "Too big for one QR code. Trim the page."
        }
    }

    private fun saveQr() {
        val bmp = PocketState.qr?.bitmap ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "pocketpage-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PocketPage")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            toast("Couldn't save")
            return
        }
        contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        toast("Saved to Pictures/PocketPage")
    }

    private fun openFromQr(text: String) {
        val payload = try {
            PageCodec.fromQrText(text)
        } catch (e: IllegalArgumentException) {
            toast("QR data is damaged")
            return
        }
        if (payload == null) toast("That QR isn't a PocketPage") else openViewer(payload)
    }

    // ---------- NFC ----------
    private fun startWrite() {
        if (nfc?.isEnabled != true) {
            PocketState.nfcStatus = "Turn NFC on in Settings first."
            return
        }
        PocketState.pendingWrite = PageCodec.compress(PocketState.html)
        PocketState.writing = true
        PocketState.nfcStatus = ""
    }

    private fun cancelWrite() {
        PocketState.pendingWrite = null
        PocketState.writing = false
    }

    /** Runs on a binder thread, not the UI thread. */
    override fun onTagDiscovered(tag: Tag) {
        val toWrite = PocketState.pendingWrite
        if (toWrite != null) {
            PocketState.pendingWrite = null
            val status = try {
                "✓ Written (${TagIo.write(tag, toWrite)} B). Tap it again to test."
            } catch (e: Exception) {
                "✗ ${e.message ?: e.javaClass.simpleName}"
            }
            runOnUiThread {
                PocketState.writing = false
                PocketState.nfcStatus = status
            }
            return
        }
        val payload = try {
            TagIo.read(tag)
        } catch (e: Exception) {
            null
        }
        runOnUiThread {
            if (payload != null) openViewer(payload) else PocketState.nfcStatus = "That tag has no PocketPage on it."
        }
    }
}
