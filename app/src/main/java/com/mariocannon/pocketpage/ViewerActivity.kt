package com.mariocannon.pocketpage

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mariocannon.pocketpage.codec.PageCodec
import com.mariocannon.pocketpage.nfc.TagIo

/**
 * Shows a page decoded from a QR code or an NFC tag. Fully offline.
 * Also opens by itself when a PocketPage tag or link is tapped with the app closed (see the manifest).
 */
class ViewerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PAYLOAD = "payload"
    }

    private lateinit var web: WebView

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
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(web, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
        show(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent)
    }

    private fun show(intent: Intent) {
        val html = try {
            // From the app, a link (QR / browser), or a tag tapped with the app closed.
            val payload = intent.getByteArrayExtra(EXTRA_PAYLOAD)
                ?: intent.data?.let { PageCodec.fromLink(it.toString()) }
                ?: TagIo.read(intent)
            if (payload == null) {
                fail("No PocketPage found")
                return
            }
            PageCodec.decompress(payload)
        } catch (e: Exception) {
            fail("Page data is damaged")
            return
        }
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private fun fail(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        finish()
    }
}
