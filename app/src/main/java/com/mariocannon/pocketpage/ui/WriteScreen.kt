package com.mariocannon.pocketpage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariocannon.pocketpage.codec.BookMeta
import com.mariocannon.pocketpage.codec.PageCodec

private val TAGS = listOf("NTAG213" to 144, "NTAG215" to 504, "NTAG216" to 888)

class PocketActions(
    val onFullSample: () -> Unit,
    val onTinySample: () -> Unit,
    val onOpenFile: () -> Unit,
    val onPreview: () -> Unit,
    val onMakeQr: () -> Unit,
    val onMakeLinkQr: () -> Unit,
    val onSaveQr: () -> Unit,
    val onWriteTag: () -> Unit,
    val onWriteLinkTag: () -> Unit,
    val onCancelWrite: () -> Unit,
    val onBack: () -> Unit,
)

@Composable
fun WriteScreen(hasNfc: Boolean, actions: PocketActions) {
    BackHandler(onBack = actions.onBack)
    val html = PocketState.html
    val payload = remember(html) { PageCodec.compress(html) }
    val payloadSize = if (html.isEmpty()) 0 else payload.size
    val rawSize = remember(html) { html.toByteArray(Charsets.UTF_8).size }
    val tagNeed = PageCodec.nfcBytesNeeded(payloadSize)
    val linkTagNeed = remember(payload) { PageCodec.nfcLinkBytesNeeded(payload) }

    Scaffold(topBar = { PocketTopBar("Write a page", onBack = actions.onBack) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Section("1 · Page") {
                OutlinedTextField(
                    value = html,
                    onValueChange = { PocketState.setPage(it) },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallOutlined("Full sample", Modifier.weight(1f), actions.onFullSample)
                    SmallOutlined("Tiny sample", Modifier.weight(1f), actions.onTinySample)
                    SmallOutlined("Open file", Modifier.weight(1f), actions.onOpenFile)
                }
                Muted("Raw $rawSize B → compressed $payloadSize B")
                OutlinedButton(onClick = actions.onPreview, modifier = Modifier.fillMaxWidth()) { Text("Preview") }
            }

            BookSection(html)

            Section("3 · QR code") {
                Muted("App QR holds the most but needs PocketPage to scan. Link QR opens with any phone camera.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = actions.onMakeQr, modifier = Modifier.weight(1f)) { Text("App QR") }
                    Button(
                        onClick = actions.onMakeLinkQr,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f),
                    ) { Text("Link QR") }
                }
                OutlinedButton(
                    onClick = actions.onSaveQr,
                    enabled = PocketState.qr != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save to Photos") }
                PocketState.qrError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PocketState.qr?.let { qr ->
                    Muted(
                        (if (PocketState.qrIsLink) "Link QR · " else "App QR · ") +
                            "Version ${qr.version} (${qr.modules}×${qr.modules}) · ${qr.difficulty}. " +
                            "Print at least ${qr.minPrintCm} cm wide."
                    )
                    Image(
                        bitmap = qr.bitmap.asImageBitmap(),
                        contentDescription = "QR code holding the page",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxWidth().background(Color.White),
                    )
                }
            }

            Section("4 · NFC tag") {
                if (!hasNfc) {
                    Text("This phone has no NFC.", color = MaterialTheme.colorScheme.error)
                } else {
                    TagFit("App tag", tagNeed, payloadSize > 0)
                    TagFit("Link tag", linkTagNeed, payloadSize > 0)
                    if (PocketState.writing) {
                        Button(
                            onClick = actions.onCancelWrite,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Hold a tag to the phone…  (tap to cancel)") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = actions.onWriteTag, modifier = Modifier.weight(1f)) {
                                Text("Write app tag")
                            }
                            Button(
                                onClick = actions.onWriteLinkTag,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f),
                            ) { Text("Write link tag") }
                        }
                    }
                    if (PocketState.nfcStatus.isNotEmpty()) Text(PocketState.nfcStatus, fontSize = 14.sp)
                    Muted(
                        "App tag: needs PocketPage, opens offline straight away. " +
                            "Link tag: any phone (iPhone too) opens it in the browser; first time needs internet, then works offline. " +
                            "If PocketPage is installed, both open in the app."
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Optional: mark this page as one page of a book, so readers collect it into that book. */
@Composable
private fun BookSection(html: String) {
    val current = remember(html) { BookMeta.read(html) }
    var title by remember(current) { mutableStateOf(current?.title ?: "") }
    var page by remember(current) { mutableStateOf(current?.page?.toString() ?: "") }
    var total by remember(current) { mutableStateOf(current?.total?.toString() ?: "") }
    val meta = BookMeta.of(title, page, total)
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    Section("2 · Book (optional)") {
        Muted("Part of a book? Readers collect every page into one book as they scan them.")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Book title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = page,
                onValueChange = { page = it.filter(Char::isDigit).take(3) },
                label = { Text("Page") },
                singleLine = true,
                keyboardOptions = number,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = total,
                onValueChange = { total = it.filter(Char::isDigit).take(3) },
                label = { Text("of") },
                singleLine = true,
                keyboardOptions = number,
                modifier = Modifier.weight(1f),
            )
        }
        if (meta == null && (title.isNotBlank() || page.isNotEmpty() || total.isNotEmpty())) {
            Muted("Fill in all three. The page number must be between 1 and the total.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { PocketState.setPage(BookMeta.write(html, meta)) },
                enabled = meta != null && meta != current,
                modifier = Modifier.weight(1f),
            ) { Text(if (current == null) "Add to page" else "Update page") }
            if (current != null) {
                OutlinedButton(onClick = { PocketState.setPage(BookMeta.write(html, null)) }, modifier = Modifier.weight(1f)) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun TagFit(label: String, need: Int, hasPage: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("$label ${if (hasPage) "$need B" else ""}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TAGS.forEach { (name, cap) ->
            val ok = hasPage && need <= cap
            Text(
                "${if (ok) "✓" else "✗"} ${name.removePrefix("NTAG")}",
                fontWeight = FontWeight.SemiBold,
                color = if (ok) Color(0xFF2C7A4B) else MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SmallOutlined(label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(label, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
