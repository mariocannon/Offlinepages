package com.mariocannon.pocketpage.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PageCodecTest {

    @Test
    fun base45MatchesRfc9285() {
        assertEquals("BB8", Base45.encode("AB".toByteArray()))
        assertEquals("%69 VD92EX0", Base45.encode("Hello!!".toByteArray()))
        assertEquals("UJCLQE7W581", Base45.encode("base-45".toByteArray()))
        assertEquals("ietf!", String(Base45.decode("QED8WEX0")))
    }

    @Test
    fun base45RoundTripsAnyLength() {
        val rnd = Random(42)
        for (len in listOf(0, 1, 2, 3, 255, 256, 1001)) {
            val bytes = rnd.nextBytes(len)
            assertArrayEquals(bytes, Base45.decode(Base45.encode(bytes)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun base45RejectsBadCharacters() {
        Base45.decode("abc")
    }

    @Test
    fun pageRoundTripsThroughQrText() {
        val html = "<!doctype html><h1>Kia ora Ōrewa</h1>" + "<p>Hibiscus Coast</p>".repeat(20)
        val payload = PageCodec.compress(html)
        assertTrue("should compress", payload.size < html.length)
        val text = PageCodec.toQrText(payload)
        assertTrue(text.all { it in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:" })
        assertEquals(html, PageCodec.decompress(PageCodec.fromQrText(text)!!))
    }

    @Test
    fun readsQrMadeByTheWebApp() {
        val fromWeb = "PP1:ZWM376: UAT9\$DP%H9J87LPANKBCADV6K.8DEOAQCT/CJEPLSVAG8AUIIS8AXOJ7BIB7AITKD6C00"
        assertEquals(
            "<h1>Kia ora Ōrewa</h1><p>Made by the web app</p>",
            PageCodec.decompress(PageCodec.fromQrText(fromWeb)!!),
        )
    }

    @Test
    fun ignoresOtherQrCodes() {
        assertNull(PageCodec.fromQrText("https://example.com"))
    }

    @Test(expected = Exception::class)
    fun damagedPayloadThrows() {
        val payload = PageCodec.compress("<p>hello hello hello hello</p>")
        PageCodec.decompress(payload.copyOf(payload.size / 2))
    }

    @Test
    fun pageRoundTripsThroughLink() {
        val html = "<!doctype html><h1>Kia ora</h1>" + "<p>Ōrewa beach</p>".repeat(30)
        val link = PageCodec.toLink(PageCodec.compress(html))
        assertTrue(link.startsWith("https://mariocannon.github.io/Offlinepages/#p="))
        assertEquals(html, PageCodec.decompress(PageCodec.fromLink(link)!!))
    }

    @Test
    fun readsLinkMadeByTheWebApp() {
        val fromWeb = "https://mariocannon.github.io/Offlinepages/#p=s8kwtPPOTFTIL0pUONpTlFqeaKOfYWhnU2DnVpSfq5CokJOZl22jX2AHAA"
        assertEquals(
            "<h1>Kia ora Ōrewa</h1><p>From a link</p>",
            PageCodec.decompress(PageCodec.fromLink(fromWeb)!!),
        )
    }

    @Test
    fun ignoresOtherLinks() {
        assertNull(PageCodec.fromLink("https://example.com/page"))
        assertNull(PageCodec.fromLink("https://mariocannon.github.io/Offlinepages/"))
    }

    @Test
    fun nfcLinkSizing() {
        val payload = ByteArray(300) { it.toByte() }
        // 1 prefix byte + URL minus "https://" -> header 3 or 6, + type 1, + TLV, + terminator
        val uri = 1 + PageCodec.toLink(payload).removePrefix("https://").length
        val record = (if (uri < 256) 3 else 6) + 1 + uri
        assertEquals(record + (if (record < 255) 2 else 4) + 1, PageCodec.nfcLinkBytesNeeded(payload))
    }

    @Test
    fun nfcSizing() {
        // 100 B payload: 3 header + 16 type + 100 = 119 record, + 2 TLV + 1 terminator
        assertEquals(122, PageCodec.nfcBytesNeeded(100))
        // 421 B payload (tiny sample): 6 + 16 + 421 = 443, + 4 TLV + 1
        assertEquals(448, PageCodec.nfcBytesNeeded(421))
    }
}
