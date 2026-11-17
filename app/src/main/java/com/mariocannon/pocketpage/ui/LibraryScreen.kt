package com.mariocannon.pocketpage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariocannon.pocketpage.library.Library
import java.text.DateFormat
import java.util.Date

class LibraryActions(
    val onBack: () -> Unit,
    val onOpenBook: (key: String) -> Unit,
    val onOpenPage: (id: String) -> Unit,
    val onDeletePage: (id: String) -> Unit,
)

/** Everything saved on this phone: books first, then single pages. */
@Composable
fun LibraryScreen(books: List<Library.Book>, pages: List<Library.Entry>, actions: LibraryActions) {
    BackHandler(onBack = actions.onBack)
    var confirmDelete by remember { mutableStateOf<Library.Entry?>(null) }

    Scaffold(topBar = { PocketTopBar("My library", onBack = actions.onBack) }) { inner ->
        if (books.isEmpty() && pages.isEmpty()) {
            Empty(
                "Nothing saved yet.\n\nOpen a page and tap Save, or scan a page from a book to start collecting it.",
                Modifier.padding(inner),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (books.isNotEmpty()) {
                    item { Heading("Books") }
                    items(books, key = { it.key }) { book ->
                        Card(onClick = { actions.onOpenBook(book.key) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Progress(book)
                            }
                        }
                    }
                }
                if (pages.isNotEmpty()) {
                    item { Heading("Pages") }
                    items(pages, key = { it.id }) { page ->
                        Card(onClick = { actions.onOpenPage(page.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                                    Text(page.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "Saved " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(page.savedAt)),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { confirmDelete = page }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${page.title}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { page ->
        Confirm(
            title = "Delete page?",
            text = "\"${page.title}\" will be removed from your library.",
            onConfirm = { actions.onDeletePage(page.id) },
            onDismiss = { confirmDelete = null },
        )
    }
}

class BookActions(
    val onBack: () -> Unit,
    val onOpenPage: (id: String) -> Unit,
    val onDelete: (key: String) -> Unit,
)

/** One book: every page in order, with the ones not found yet locked. */
@Composable
fun BookScreen(book: Library.Book?, actions: BookActions) {
    BackHandler(onBack = actions.onBack)
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PocketTopBar(book?.title ?: "Book", onBack = actions.onBack) {
                if (book != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete book")
                    }
                }
            }
        },
    ) { inner ->
        if (book == null) {
            Empty("This book isn't in your library any more.", Modifier.padding(inner))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Progress(book) }
                items((1..book.total).toList(), key = { it }) { n ->
                    val page = book.pages[n]
                    val number = @Composable {
                        Text("$n", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    }
                    if (page != null) {
                        Card(onClick = { actions.onOpenPage(page.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                number()
                                Text(page.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                number()
                                Text("Not found yet", Modifier.weight(1f))
                                Icon(Icons.Filled.Lock, contentDescription = "Locked")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && book != null) {
        Confirm(
            title = "Delete this book?",
            text = "All ${book.collected} collected pages of \"${book.title}\" will be removed. " +
                "You can collect them again by scanning.",
            onConfirm = { actions.onDelete(book.key) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Progress(book: Library.Book) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (book.complete) "All ${book.total} pages collected ✓" else "${book.collected} of ${book.total} pages collected",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { book.collected.toFloat() / book.total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Empty(text: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Confirm(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
