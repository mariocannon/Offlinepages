package com.mariocannon.pocketpage

import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mariocannon.pocketpage.codec.BookMeta
import com.mariocannon.pocketpage.codec.PageCodec
import com.mariocannon.pocketpage.library.Library
import com.mariocannon.pocketpage.nfc.TagIo
import com.mariocannon.pocketpage.ui.PocketTheme

/**
 * Shows a page decoded from a QR code or an NFC tag. Fully offline.
 * Also opens by itself when a PocketPage tag or link is tapped with the app closed (see the manifest).
 *
 * The bar at the bottom saves single pages on request. Book pages save themselves and let you
 * flip through the pages of that book collected so far.
 */
class ViewerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PAYLOAD = "payload"
        /** Writer preview: just show the page, never save it. */
        const val EXTRA_PREVIEW = "preview"
        /** Show a page from the library. */
        const val EXTRA_SAVED_ID = "savedId"
    }

    private sealed interface Bar {
        data object None : Bar
        class Single(val payload: ByteArray, val saved: Boolean) : Bar
        class InBook(val book: Library.Book, val page: Int) : Bar
    }

    private lateinit var web: WebView
    private val library by lazy { Library.at(filesDir) }
    private var bar by mutableStateOf<Bar>(Bar.None)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                // Links on the page open in the phone's browser.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }
            }
        }
        setContent {
            PocketTheme {
                Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
                    AndroidView({ web }, Modifier.weight(1f).fillMaxWidth())
                    BottomBar(bar)
                }
            }
        }
        show(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent)
    }

    private fun show(intent: Intent) {
        val savedId = intent.getStringExtra(EXTRA_SAVED_ID)
        val payload: ByteArray
        val html: String
        try {
            // From the library, the app, a link (QR / browser), or a tag tapped with the app closed.
            payload = (
                if (savedId != null) library.payload(savedId)
                else intent.getByteArrayExtra(EXTRA_PAYLOAD)
                    ?: intent.data?.let { PageCodec.fromLink(it.toString()) }
                    ?: TagIo.read(intent)
                ) ?: run {
                fail("No PocketPage found")
                return
            }
            html = PageCodec.decompress(payload)
        } catch (e: Exception) {
            fail("Page data is damaged")
            return
        }
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        bar = when {
            intent.getBooleanExtra(EXTRA_PREVIEW, false) -> Bar.None
            else -> barFor(payload, html, fromLibrary = savedId != null)
        }
    }

    private fun barFor(payload: ByteArray, html: String, fromLibrary: Boolean): Bar {
        val meta = BookMeta.read(html)
            ?: return if (fromLibrary) Bar.None else Bar.Single(payload, saved = library.isSaved(payload))
        // Book pages collect themselves.
        val added = !fromLibrary && runCatching { library.save(payload).newlyAdded }.getOrDefault(false)
        val book = library.book(Library.bookKey(meta)) ?: return Bar.None
        if (added) {
            toast(
                if (book.complete) "You've collected all ${book.total} pages of ${book.title}!"
                else "Collected page ${meta.page} of ${book.title} (${book.collected} of ${book.total})"
            )
        }
        return Bar.InBook(book, meta.page)
    }

    private fun save(payload: ByteArray) {
        try {
            library.save(payload)
            bar = Bar.Single(payload, saved = true)
            toast("Saved to your library")
        } catch (e: Exception) {
            toast("Couldn't save the page")
        }
    }

    private fun showBookPage(book: Library.Book, page: Int) {
        val id = book.pages[page]?.id ?: return
        val i = Intent(this, ViewerActivity::class.java).putExtra(EXTRA_SAVED_ID, id)
        setIntent(i) // so rotating keeps this page
        show(i)
    }

    private fun showBook(key: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_BOOK, key)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    @Composable
    private fun BottomBar(bar: Bar) {
        when (bar) {
            Bar.None -> {}
            is Bar.Single -> Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (bar.saved) {
                        Text("✓ Saved to your library", Modifier.weight(1f).padding(vertical = 12.dp))
                    } else {
                        Text("Keep this page?", Modifier.weight(1f))
                        Button(onClick = { save(bar.payload) }) { Text("Save") }
                    }
                }
            }
            is Bar.InBook -> Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                val pages = bar.book.pages.keys.sorted()
                val prev = pages.lastOrNull { it < bar.page }
                val next = pages.firstOrNull { it > bar.page }
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { prev?.let { showBookPage(bar.book, it) } }, enabled = prev != null) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                    }
                    Column(
                        Modifier.weight(1f).clickable { showBook(bar.book.key) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(bar.book.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Page ${bar.page} of ${bar.book.total} · ${bar.book.collected} collected",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { next?.let { showBookPage(bar.book, it) } }, enabled = next != null) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun fail(msg: String) {
        toast(msg)
        finish()
    }
}
