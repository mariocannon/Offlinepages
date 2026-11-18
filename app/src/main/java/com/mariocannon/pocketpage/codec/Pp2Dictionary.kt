package com.mariocannon.pocketpage.codec

/**
 * The PP2 shared dictionary: text that small pages nearly always contain (HTML tags, CSS, common
 * words). The compressor points back into it instead of storing those bytes on the tag.
 *
 * FROZEN. Every PP2 code and tag ever made depends on these exact bytes. Changing one character
 * breaks them all, so a better dictionary needs a new prefix (PP3). The web app has the same
 * text in web/codec.js; `PageCodecTest.dictionaryIsFrozen` guards both (3807 B, CRC32 ad40c250).
 * ASCII only. The most common strings sit at the end, where they're cheapest to point at.
 */
object Pp2Dictionary {
    private const val TEXT =
        " because before after about again every where there their which would could should other t" +
        "hese those people time year day week today tomorrow Monday Tuesday Wednesday Thursday Frid" +
        "ay Saturday Sunday January February March April June July August September October Novembe" +
        "r December morning afternoon evening night  welcome hello thanks thank you please contact " +
        "phone email address website online offline free open closed hours price prices from only j" +
        "ust more most best new news info information about here there this that with without your " +
        "you our we are is it its the and for to of in on at by or not no yes can will get got make" +
        " made use used find see look tap scan call book now learn read page pages story the end on" +
        "ce upon a time little big small slow fast said went came saw lived  New Zealand Aotearoa A" +
        "uckland Kia ora Hibiscus Coast village river beach walk history things to do getting there" +
        " getting started how it works what it is good for tips links menu events local community s" +
        "afety emergency instructions setup step steps guide rules wifi password #000#111#222#333#4" +
        "44#555#666#777#888#999#aaa#bbb#ccc#ddd#eee#fff#f6f1e7#2f4a3alinear-gradient(radial-gradien" +
        "t(rgba(0,0,0,.box-shadow:0 2px 8px transition:transform:translate(rotate(animation:@keyfra" +
        "mes @media(min-width:600px){grid-template-columns:1fr 1fr;gap:flex-direction:column;justif" +
        "y-content:space-between;align-items:center;position:relative;absolute;sticky;top:0;left:0;" +
        "right:0;overflow:hidden;white-space:nowrap;opacity:cursor:pointer;letter-spacing:.1em;text" +
        "-transform:uppercase;font-style:italic;font-weight:600;font-weight:700;font-weight:bold;li" +
        "ne-height:1.5;list-style:none;border-top:1px solid #border-bottom:border-left:border:1px s" +
        "olid #border-radius:12px;border-radius:50%;box-sizing:border-box;width:100%;height:100%;ma" +
        "x-width:100%;min-height:100vh;text-decoration:none;text-align:left;text-align:center;displ" +
        "ay:inline-block;display:flex;display:grid;display:block;margin-top:1em;margin-bottom:margi" +
        "n:0 auto;margin:0;padding:0 padding:1em;padding:12px 16px;font-size:1.2em;font-size:14px;f" +
        "ont-family:font:17px/1.5 Georgia,serif;font:17px/1.6 system-ui,sans-serif;background:linea" +
        "r-gradient(background:#fff;color:#fff;color:#}h1{}h2{}h3{}p{}a{}b{}i{}ul{}ol{}li{}small{}h" +
        "eader{}footer{}main{}section{}nav{}div{}span{}img{}svg{}code{}.c{}.b{}*{box-sizing:border-" +
        "box}html{}body{max-width:36em;margin:auto;padding:5vw;<svg xmlns=\"http://www.w3.org/2000/s" +
        "vg\" viewBox=\"0 0 100 100\" width=\"height=\"fill=\"none\" stroke=\"stroke-width=\"<path d=\"M<circ" +
        "le cx=\"cy=\"r=\"<rect x=\"y=\"<text x=\"</svg><script>document.getElementById(querySelector(add" +
        "EventListener('click',function(){onclick=textContent=innerHTML=style.classList.toggle(</sc" +
        "ript><table><tr><th></th><td></td></tr></table><img src=\"data:image/svg+xml,alt=\"<button><" +
        "input type=\"<label><form><br><hr><em></em><strong></strong><i></i><code></code><small></sm" +
        "all><span></span><div class=\"</div><div class=<div id=<section></section><header></header>" +
        "<footer></footer><main></main><nav></nav> target=_blank rel=noopener href=\"tel:+64href=\"ma" +
        "ilto:href=\"sms:href=\"geo:href=\"https://maps.google.com/?q=href=\"https://www.facebook.com/h" +
        "ref=\"https://www.instagram.com/.co.nz/\">.com/\">.org/\">.nz\"></a><a href=https://www.<a href" +
        "=\"https://<dl><dt></dt><dd></dd></dl><ol><li></ol><ul><li><li><b></b></ul></li><li></p><p>" +
        "<b></b><p></h3><h3></h2><h2></h1><h1><meta name=\"pocketpage-book\" content=\"<html lang=\"en\"" +
        "><head></head><body></body></html><title></title><style></style><body class=pp-plain><body" +
        " class=pp-book><body class=pp-dark><!DOCTYPE html><!doctype html><meta charset=\"utf-8\"><me" +
        "ta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta charset=utf-8><meta " +
        "name=viewport content=\"width=device-width\"><meta charset=utf-8><meta name=viewport content" +
        "=width=device-width><title>"

    val BYTES: ByteArray = TEXT.toByteArray(Charsets.US_ASCII)
}
