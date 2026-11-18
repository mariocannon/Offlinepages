package com.mariocannon.pocketpage.codec

/**
 * Built-in themes. A page opts in with `<body class=pp-dark>` (or pp-book, pp-plain) and gets
 * the stylesheet from the app instead of carrying it on the tag. Pages without a pp- class are
 * shown untouched. Pure Kotlin; the web viewer has the same CSS in web/codec.js.
 *
 *   pp-dark   dark green, link buttons, section headings (h2)
 *   pp-book   cream storybook, centred serif; <b> is a big picture line (emoji)
 *   pp-plain  clean black on white
 */
object Themes {
    const val VIEWPORT = "<meta name=viewport content=width=device-width>"
    const val BASE = "body{margin:0 auto;max-width:36em;padding:5vw;font:17px/1.6 system-ui,sans-serif}img,svg{max-width:100%}"
    val CSS: Map<String, String> = mapOf(
        "dark" to
            "body{background:#0e1511;color:#d5e0d8}h1{font-size:2.4em;line-height:1.1;color:#67ea94;mar" +
            "gin:.4em 0 .2em}h2{color:#67ea94;font-size:.9em;letter-spacing:.15em;text-transform:upperc" +
            "ase;border-top:1px solid #24382c;padding-top:1.4em;margin-top:2em}b{color:#fff}li{margin:." +
            "3em 0}::marker{color:#67ea94}code{background:#1c2b22;color:#67ea94;padding:.1em .4em;borde" +
            "r-radius:5px}a{display:block;margin:.5em 0;padding:.8em 1em;background:#15201a;border:1px " +
            "solid #24382c;border-radius:12px;color:#67ea94;text-decoration:none}",
        "book" to
            "body{background:#fbf6ea;color:#3a2f25;font:20px/1.6 Georgia,serif;text-align:center}h1,h2{" +
            "color:#4f7a3e}h1{font-size:2em;line-height:1.2;margin:.3em 0}b{display:block;font-size:4em" +
            ";line-height:1.3;margin-top:.3em}a{color:#4f7a3e}",
        "plain" to
            "body{background:#fff;color:#1f2a24}h1{font-size:2em;line-height:1.2}h2{margin-top:1.6em}a{" +
            "color:#b4552d}",
    )

    private val BODY_CLASS =
        Regex("""<body[^>]*\sclass=["']?[^"'>]*\bpp-(dark|book|plain)\b""", RegexOption.IGNORE_CASE)
    private val DOCTYPE = Regex("""^\s*<!doctype[^>]*>""", RegexOption.IGNORE_CASE)

    /** The theme a page asks for ("dark", "book", "plain"), or null. */
    fun of(html: String): String? = BODY_CLASS.find(html)?.groupValues?.get(1)?.lowercase()

    /** Adds the theme's stylesheet (and a viewport tag if the page has none). The page's own CSS still wins. */
    fun apply(html: String): String {
        val css = CSS[of(html) ?: return html] ?: return html
        val head = (if (html.contains("viewport", ignoreCase = true)) "" else VIEWPORT) + "<style>" + BASE + css + "</style>"
        val at = DOCTYPE.find(html)?.range?.last?.plus(1) ?: 0
        return html.substring(0, at) + head + html.substring(at)
    }
}
