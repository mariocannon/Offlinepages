package com.mariocannon.pocketpage.codec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {

    @Test
    fun findsTheThemeOnTheBodyTag() {
        assertEquals("dark", Themes.of("<body class=pp-dark><h1>x</h1>"))
        assertEquals("book", Themes.of("<BODY id=a class=\"hero pp-book\">"))
        assertEquals("plain", Themes.of("<!doctype html><body class='pp-plain'>"))
    }

    @Test
    fun ignoresLookalikes() {
        assertNull(Themes.of("<body class=pp-darker>"))
        assertNull(Themes.of("<div class=pp-dark>"))
        assertNull(Themes.of("<h1>no body tag</h1>"))
    }

    @Test
    fun pagesWithoutAThemeAreUntouched() {
        val html = "<!doctype html><style>body{color:red}</style><h1>plain page</h1>"
        assertEquals(html, Themes.apply(html))
    }

    @Test
    fun addsViewportAndStylesAfterTheDoctype() {
        val out = Themes.apply("<!doctype html><body class=pp-plain><h1>x</h1>")
        assertTrue(out.startsWith("<!doctype html>" + Themes.VIEWPORT + "<style>" + Themes.BASE))
        assertTrue(out.endsWith("</style><body class=pp-plain><h1>x</h1>"))
    }

    @Test
    fun keepsThePagesOwnViewport() {
        val out = Themes.apply(Themes.VIEWPORT + "<body class=pp-dark>")
        assertFalse(out.startsWith(Themes.VIEWPORT + "<meta"))
        assertTrue(out.startsWith("<style>"))
        assertTrue(out.contains(Themes.CSS.getValue("dark")))
    }
}
