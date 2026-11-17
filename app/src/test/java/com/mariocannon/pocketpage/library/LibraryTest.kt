package com.mariocannon.pocketpage.library

import com.mariocannon.pocketpage.codec.BookMeta
import com.mariocannon.pocketpage.codec.PageCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun lib() = Library(tmp.root)

    private fun page(title: String, book: BookMeta? = null) =
        PageCodec.compress(BookMeta.write("<!doctype html><title>$title</title><p>$title</p>", book))

    @Test
    fun savesSinglePagesOnce() {
        val lib = lib()
        val p = page("Cafe")
        assertFalse(lib.isSaved(p))
        assertTrue(lib.save(p).newlyAdded)
        assertFalse(lib.save(p).newlyAdded)
        assertTrue(lib.isSaved(p))
        assertEquals(listOf("Cafe"), lib.singles().map { it.title })
        assertEquals(emptyList<Library.Book>(), lib.books())
    }

    @Test
    fun collectsBookPagesIntoOneBook() {
        val lib = lib()
        lib.save(page("Start", BookMeta("Trail", 1, 3)))
        lib.save(page("End", BookMeta("trail", 3, 3)))
        lib.save(page("Other", BookMeta("Trail", 1, 5))) // different page count = different book

        val books = lib.books()
        assertEquals(2, books.size)
        val trail = books.single { it.total == 3 }
        assertEquals(setOf(1, 3), trail.pages.keys)
        assertFalse(trail.complete)
        assertEquals(trail, lib.book(trail.key))
        assertEquals(emptyList<Library.Entry>(), lib.singles())
    }

    @Test
    fun rescanningABookPageReplacesIt() {
        val lib = lib()
        assertTrue(lib.save(page("Old", BookMeta("Trail", 2, 3))).newlyAdded)
        val again = lib.save(page("New", BookMeta("Trail", 2, 3)))
        assertFalse(again.newlyAdded)
        assertEquals("New", lib.books().single().pages[2]!!.title)
    }

    @Test
    fun deletes() {
        val lib = lib()
        val single = lib.save(page("Cafe")).id
        lib.save(page("A", BookMeta("Trail", 1, 2)))
        lib.save(page("B", BookMeta("Trail", 2, 2)))
        assertTrue(lib.books().single().complete)

        lib.delete(single)
        lib.deleteBook(lib.books().single().key)
        assertEquals(emptyList<Library.Entry>(), lib.entries())
        assertNull(lib.payload(single))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIdsThatEscapeTheFolder() {
        lib().payload("../secrets")
    }
}
