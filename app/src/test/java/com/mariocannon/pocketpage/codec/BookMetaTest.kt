package com.mariocannon.pocketpage.codec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookMetaTest {

    private val page = """<!doctype html><title>Stop 3</title><h1>Hi</h1>"""

    @Test
    fun writesAndReadsBack() {
        val meta = BookMeta("Trail Guide", 3, 10)
        val html = BookMeta.write(page, meta)
        assertTrue(html.startsWith("""<!doctype html><meta name="pocketpage-book" content="Trail Guide;3;10">"""))
        assertEquals(meta, BookMeta.read(html))
    }

    @Test
    fun titleCanHoldSemicolonsQuotesAndTags() {
        val meta = BookMeta("""A; "B" & <C>""", 1, 2)
        assertEquals(meta, BookMeta.read(BookMeta.write(page, meta)))
    }

    @Test
    fun insertsInsideHead() {
        val html = BookMeta.write("<html><head><title>x</title></head></html>", BookMeta("B", 1, 1))
        assertTrue(html.startsWith("""<html><head><meta name="pocketpage-book""""))
    }

    @Test
    fun replacesAndRemoves() {
        val one = BookMeta.write(page, BookMeta("B", 1, 5))
        val two = BookMeta.write(one, BookMeta("B", 2, 5))
        assertEquals(BookMeta("B", 2, 5), BookMeta.read(two))
        assertEquals(1, Regex(BookMeta.NAME).findAll(two).count())
        assertEquals(page, BookMeta.write(two, null))
    }

    @Test
    fun readsHandWrittenTags() {
        assertEquals(BookMeta("Hunt", 2, 4), BookMeta.read("<META content='Hunt;2;4' NAME=pocketpage-book>"))
    }

    @Test
    fun ignoresPagesThatArentBookPages() {
        assertNull(BookMeta.read(page))
        assertNull(BookMeta.read("""<meta name="pocketpage-books" content="A;1;2">"""))
        assertNull(BookMeta.read("""<meta name="pocketpage-book" content="A;5;2">"""))
        assertNull(BookMeta.read("""<meta name="pocketpage-book" content="A;x;2">"""))
    }

    @Test
    fun validatesFormFields() {
        assertEquals(BookMeta("A", 1, 1), BookMeta.of(" A ", "1", "1"))
        assertNull(BookMeta.of("", "1", "1"))
        assertNull(BookMeta.of("A", "0", "1"))
        assertNull(BookMeta.of("A", "1", "1000"))
    }

    @Test
    fun readsPageTitle() {
        assertEquals("Stop 3", pageTitle(page))
        assertEquals("Fish & Chips", pageTitle("<title>\n Fish &amp;  Chips </title>"))
        assertNull(pageTitle("<h1>No title</h1>"))
    }
}
