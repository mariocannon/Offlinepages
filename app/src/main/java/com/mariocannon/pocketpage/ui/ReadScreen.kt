package com.mariocannon.pocketpage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ReadActions(
    val onScan: () -> Unit,
    val onNfcSettings: () -> Unit,
    val onLibrary: () -> Unit,
    val onWrite: () -> Unit,
)

/** Start page: scan a QR code or tap a tag to open a page. */
@Composable
fun ReadScreen(hasNfc: Boolean, actions: ReadActions) {
    Scaffold(topBar = { PocketTopBar("PocketPage") }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Read a page", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Scan a PocketPage QR code",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = actions.onScan, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text("Scan QR code", fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        !hasNfc -> "This phone has no NFC, so use a QR code."
                        !PocketState.nfcOn -> "NFC is off. Turn it on to read tags."
                        else -> "Or hold a PocketPage tag to the back of the phone."
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (hasNfc && !PocketState.nfcOn) {
                    OutlinedButton(onClick = actions.onNfcSettings) { Text("NFC settings") }
                }
                if (PocketState.nfcStatus.isNotEmpty()) {
                    Text(PocketState.nfcStatus, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = actions.onLibrary, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("My library", fontSize = 16.sp)
                }
            }
            TextButton(onClick = actions.onWrite) { Text("Write a page →") }
        }
    }
}
