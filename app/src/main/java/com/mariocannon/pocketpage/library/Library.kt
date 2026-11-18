package com.mariocannon.pocketpage.library

import com.mariocannon.pocketpage.codec.BookMeta
import com.mariocannon.pocketpage.codec.PageCodec
import com.mariocannon.pocketpage.codec.displayTitle
import java.io.File
import java.security.MessageDigest

/**
 * Pages saved on this phone. One file per page in [dir], holding the compressed payload.
 * Pure Kotlin (no Android imports) so it's unit-tested.
 *
 *   single page: page-<hash of payload>.pp       (saving the same page twice is a no-op)
 *   book page:   book-<book key>-<page no>.pp    (re-scanning a page replaces it)
 */
class Library(private val dir: File) {

    data class Entry(val id: String, val title: String, val book: BookMeta?, val savedAt: Long)

    data class Book(val key: String, val title: String, val total: Int, val pages: Map<Int, Entry>) {
        val collected get() = pages.size
        val complete get() = pages.size >= total
    }

    data class SaveResult(val id: String, val newlyAdded: Boolean)

    companion object {
        fun at(filesDir: File) = Library(File(filesDir, "library"))

        /** Pages with the same book title (any case) and page count belong to the same book. */
        fun bookKey(meta: BookMeta) = hash("${meta.title.lowercase()}\n${meta.total}".toByteArray())

        private val ID = Regex("[a-z0-9-]+")

        private fun hash(bytes: ByteArray) =
            MessageDigest.getInstance("SHA-256").digest(bytes).take(8).joinToString("") { "%02x".format(it) }
    }

    /** Saves the page. Throws if the payload isn't a valid page. */
    fun save(payload: ByteArray): SaveResult {
        val id = idFor(payload)
        val f = file(id)
        val existed = f.exists()
        if (!existed || !f.readBytes().contentEquals(payload)) {
            dir.mkdirs()
            val tmp = File(dir, "$id.tmp")
            tmp.writeBytes(payload)
            if (!tmp.renameTo(f)) {
                tmp.delete()
                error("Couldn't save the page")
            }
        }
        return SaveResult(id, newlyAdded = !existed)
    }

    fun isSaved(payload: ByteArray): Boolean = file(idFor(payload)).exists()

    fun payload(id: String): ByteArray? = file(id).takeIf { it.isFile }?.readBytes()

    /** Everything saved, newest first. Unreadable files are skipped. */
    fun entries(): List<Entry> =
        (dir.listFiles { f -> f.name.endsWith(".pp") } ?: emptyArray())
            .mapNotNull { runCatching { entry(it) }.getOrNull() }
            .sortedByDescending { it.savedAt }

    fun singles(entries: List<Entry> = entries()) = entries.filter { it.book == null }

    /** Books, most recently collected first. */
    fun books(entries: List<Entry> = entries()): List<Book> =
        entries.filter { it.book != null }
            .groupBy { bookKey(it.book!!) }
            .map { (key, pages) ->
                val meta = pages.first().book!!
                Book(key, meta.title, meta.total, pages.associateBy { it.book!!.page })
            }

    fun book(key: String): Book? = books().firstOrNull { it.key == key }

    fun delete(id: String) {
        file(id).delete()
    }

    fun deleteBook(key: String) {
        require(ID.matches(key)) { "bad book key" }
        dir.listFiles { f -> f.name.startsWith("book-$key-") }?.forEach { it.delete() }
    }

    private fun idFor(payload: ByteArray): String {
        val meta = BookMeta.read(PageCodec.decompress(payload))
        return if (meta == null) "page-${hash(payload)}" else "book-${bookKey(meta)}-${meta.page}"
    }

    private fun entry(f: File): Entry {
        val html = PageCodec.decompress(f.readBytes())
        val meta = BookMeta.read(html)
        val title = displayTitle(html) ?: meta?.let { "Page ${it.page}" } ?: "Untitled page"
        return Entry(f.nameWithoutExtension, title, meta, f.lastModified())
    }

    private fun file(id: String): File {
        require(ID.matches(id)) { "bad page id" }
        return File(dir, "$id.pp")
    }
}
