package com.mariocannon.pocketpage.codec

/**
 * Marks a page as one page of a book. It lives inside the page's HTML, so the QR / NFC / link
 * format doesn't change and viewers that don't know about books just show the page:
 *
 *   <meta name="pocketpage-book" content="Trail Guide;3;10">   (title;page;total)
 */
data class BookMeta(val title: String, val page: Int, val total: Int) {

    companion object {
        const val NAME = "pocketpage-book"
        const val MAX_PAGES = 999

        private val TAG = Regex("""<meta\b[^>]*\bname\s*=\s*["']?$NAME(?=["'\s/>])[^>]*>""", RegexOption.IGNORE_CASE)
        private val CONTENT = Regex("""\bcontent\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
        private val HEAD = Regex("""<head\b[^>]*>""", RegexOption.IGNORE_CASE)
        private val DOCTYPE = Regex("""^\s*<!doctype\b[^>]*>""", RegexOption.IGNORE_CASE)

        /** Book info from form fields, or null if they don't make a valid book page. */
        fun of(title: String, page: String, total: String): BookMeta? {
            val t = title.trim()
            val p = page.trim().toIntOrNull()
            val n = total.trim().toIntOrNull()
            if (t.isEmpty() || p == null || n == null || n !in 1..MAX_PAGES || p !in 1..n) return null
            return BookMeta(t, p, n)
        }

        /** The page's book info, or null if it isn't part of a book. */
        fun read(html: String): BookMeta? {
            val tag = TAG.find(html)?.value ?: return null
            val m = CONTENT.find(tag) ?: return null
            val content = unescape(m.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null)
            // Split from the right so the title may contain ';'.
            val parts = content.split(';')
            if (parts.size < 3) return null
            return of(parts.dropLast(2).joinToString(";"), parts[parts.size - 2], parts.last())
        }

        /** The html with its book tag set to [meta], or removed when [meta] is null. */
        fun write(html: String, meta: BookMeta?): String {
            val tag = meta?.let { """<meta name="$NAME" content="${escape("${it.title};${it.page};${it.total}")}">""" } ?: ""
            TAG.find(html)?.let { return html.replaceRange(it.range, tag) }
            if (meta == null) return html
            val after = HEAD.find(html) ?: DOCTYPE.find(html)
            return if (after != null) html.replaceRange(after.range.last + 1, after.range.last + 1, tag) else tag + html
        }
    }
}

private val TITLE = Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

/** Text of the page's <title>, or null if it has none. */
fun pageTitle(html: String): String? =
    TITLE.find(html)?.groupValues?.get(1)?.let { unescape(it).replace(Regex("\\s+"), " ").trim() }?.ifEmpty { null }

private val HEADING = Regex("""<h([1-3])\b[^>]*>(.*?)</h\1\s*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HIDDEN = Regex("""<!--.*?-->|<(style|script|title|head)\b[^>]*>.*?</\1\s*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val TAG_RE = Regex("""<[^>]*>""")
private val SPACES = Regex("""\s+""")

/**
 * A name for the page in the library: its <title>, else its first heading (h1, then h2, h3),
 * else the start of its text. Null for a page with no text at all. Small pages often skip
 * <title> to save bytes, so the heading is usually what this finds.
 */
fun displayTitle(html: String, maxLength: Int = 60): String? {
    pageTitle(html)?.let { return it }
    val headings = HEADING.findAll(html).toList()
    for (level in 1..3) {
        val h = headings.firstOrNull { it.groupValues[1] == level.toString() } ?: continue
        plainText(h.groupValues[2])?.let { return shorten(it, maxLength) }
    }
    return plainText(HIDDEN.replace(html, " "))?.let { shorten(it, maxLength) }
}

private fun plainText(html: String): String? =
    unescape(TAG_RE.replace(html, " ")).replace("&nbsp;", " ").replace(SPACES, " ").trim().ifEmpty { null }

/** Cuts at a word break and adds "…" when the text is longer than [max]. */
private fun shorten(text: String, max: Int): String {
    if (text.length <= max) return text
    val cut = text.lastIndexOf(' ', max - 1).takeIf { it > max / 2 } ?: (max - 1)
    return text.substring(0, cut).trimEnd() + "…"
}

private fun escape(s: String) =
    s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

private fun unescape(s: String) =
    s.replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
