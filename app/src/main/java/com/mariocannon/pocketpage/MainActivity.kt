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
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.mariocannon.pocketpage.codec.PageCodec
import com.mariocannon.pocketpage.library.Library
import com.mariocannon.pocketpage.nfc.TagIo
import com.mariocannon.pocketpage.qr.QrRenderer
import com.mariocannon.pocketpage.ui.BookActions
import com.mariocannon.pocketpage.ui.BookScreen
import com.mariocannon.pocketpage.ui.LibraryActions
import com.mariocannon.pocketpage.ui.LibraryScreen
import com.mariocannon.pocketpage.ui.Page
import com.mariocannon.pocketpage.ui.PocketActions
import com.mariocannon.pocketpage.ui.PocketState
import com.mariocannon.pocketpage.ui.PocketTheme
import com.mariocannon.pocketpage.ui.ReadActions
import com.mariocannon.pocketpage.ui.ReadScreen
import com.mariocannon.pocketpage.ui.WriteScreen

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    companion object {
        /** Open straight to this book (key) — used by the viewer's book bar. */
        const val EXTRA_OPEN_BOOK = "openBook"
    }

    private var nfc: NfcAdapter? = null
    private val library by lazy { Library.at(filesDir) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfc = NfcAdapter.getDefaultAdapter(this)
        if (PocketState.html.isEmpty()) PocketState.setPage(asset("tiny.html"))
        if (savedInstanceState == null) handleOpenBook(intent)

        setContent {
            PocketTheme {
                val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
                    result.contents?.let { openFromQr(it) }
                }
                val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let { loadFile(it) }
                }
                val version = PocketState.libraryVersion
                when (PocketState.page) {
                    Page.Read -> ReadScreen(
                        hasNfc = nfc != null,
                        actions = ReadActions(
                            onScan = {
                                scanner.launch(
                                    ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt("Point at a PocketPage QR code")
                                        .setBeepEnabled(false)
                                        .setOrientationLocked(false)
                                )
                            },
                            onNfcSettings = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                            onLibrary = { showPage(Page.Library) },
                            onWrite = { showPage(Page.Write) },
                        ),
                    )
                    Page.Write -> WriteScreen(
                        hasNfc = nfc != null,
                        actions = PocketActions(
                            onFullSample = { PocketState.setPage(asset("full.html")) },
                            onTinySample = { PocketState.setPage(asset("tiny.html")) },
                            onOpenFile = { filePicker.launch("text/*") },
                            onPreview = { openViewer(PageCodec.compress(PocketState.html), preview = true) },
                            onMakeQr = { makeQr(link = false) },
                            onMakeLinkQr = { makeQr(link = true) },
                            onSaveQr = ::saveQr,
                            onWriteTag = { startWrite(link = false) },
                            onWriteLinkTag = { startWrite(link = true) },
                            onCancelWrite = ::cancelWrite,
                            onBack = { showPage(Page.Read) },
                        ),
                    )
                    Page.Library -> {
                        val entries = remember(version) { library.entries() }
                        LibraryScreen(
                            books = remember(entries) { library.books(entries) },
                            pages = remember(entries) { library.singles(entries) },
                            actions = LibraryActions(
                                onBack = { showPage(Page.Read) },
                                onOpenBook = ::showBook,
                                onOpenPage = ::openSaved,
                                onDeletePage = {
                                    library.delete(it)
                                    PocketState.libraryVersion++
                                },
                            ),
                        )
                    }
                    Page.Book -> BookScreen(
                        book = remember(version, PocketState.openBook) { PocketState.openBook?.let(library::book) },
                        actions = BookActions(
                            onBack = { showPage(Page.Library) },
                            onOpenPage = ::openSaved,
                            onDelete = {
                                library.deleteBook(it)
                                PocketState.libraryVersion++
                                showPage(Page.Library)
                            },
                        ),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenBook(intent)
    }

    override fun onResume() {
        super.onResume()
        PocketState.libraryVersion++ // the viewer may have saved pages
        PocketState.nfcOn = nfc?.isEnabled == true
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
    private fun showPage(page: Page) {
        cancelWrite()
        PocketState.nfcStatus = ""
        PocketState.page = page
    }

    private fun showBook(key: String) {
        PocketState.openBook = key
        showPage(Page.Book)
    }

    private fun handleOpenBook(intent: Intent) {
        intent.getStringExtra(EXTRA_OPEN_BOOK)?.let(::showBook)
        intent.removeExtra(EXTRA_OPEN_BOOK)
    }

    private fun asset(name: String): String =
        assets.open(name).use { it.readBytes().toString(Charsets.UTF_8) }.trim()

    private fun loadFile(uri: Uri) {
        val text = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        if (text != null) PocketState.setPage(text) else toast("Couldn't read that file")
    }

    private fun openViewer(payload: ByteArray, preview: Boolean = false) {
        startActivity(
            Intent(this, ViewerActivity::class.java)
                .putExtra(ViewerActivity.EXTRA_PAYLOAD, payload)
                .putExtra(ViewerActivity.EXTRA_PREVIEW, preview)
        )
    }

    private fun openSaved(id: String) {
        startActivity(Intent(this, ViewerActivity::class.java).putExtra(ViewerActivity.EXTRA_SAVED_ID, id))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    // ---------- QR ----------
    private fun makeQr(link: Boolean) {
        val payload = PageCodec.compress(PocketState.html)
        val text = if (link) PageCodec.toLink(payload) else PageCodec.toQrText(payload)
        try {
            PocketState.qr = QrRenderer.render(text)
            PocketState.qrIsLink = link
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
    private fun startWrite(link: Boolean) {
        if (nfc?.isEnabled != true) {
            PocketState.nfcStatus = "Turn NFC on in Settings first."
            return
        }
        val payload = PageCodec.compress(PocketState.html)
        PocketState.pendingWrite = if (link) TagIo.linkMessage(payload) else TagIo.message(payload)
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
