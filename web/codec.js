/* PocketPage codec: the data format, shared with the Android app (codec/PageCodec.kt).
 * Plain script, no DOM, so it also loads in Node for tools/web-codec-test.mjs.
 *
 * Payload = raw DEFLATE of the page's UTF-8 HTML, compressed against PP2_DICT.
 *   QR:      "PP2:" + base45(payload)
 *   NFC app: external-type record "pp:2", bytes = payload
 *   Link:    https://mariocannon.github.io/Offlinepages/#p2=<base64url(payload)>
 * PP1 (no dictionary: "PP1:", MIME "application/x-pp", "#p=") still opens. Inflating with the
 * dictionary loaded reads both, because a PP1 stream never points back into it.
 */
'use strict';

const QR_PREFIX = 'PP2:';
const QR_PREFIXES = ['PP2:', 'PP1:'];
const NFC_TYPE = 'pp:2';
const NFC_MIME_V1 = 'application/x-pp';
const LINK_KEY = '#p2=';
const LINK_RE = /#p2?=([\w-]+)/;

/* FROZEN: every PP2 code and tag depends on these exact bytes (3807 B, CRC32 ad40c250).
 * Same text as Pp2Dictionary.kt. A better dictionary needs a new prefix (PP3). */
const PP2_DICT_TEXT =
  ' because before after about again every where there their which would could should other t' +
  'hese those people time year day week today tomorrow Monday Tuesday Wednesday Thursday Frid' +
  'ay Saturday Sunday January February March April June July August September October Novembe' +
  'r December morning afternoon evening night  welcome hello thanks thank you please contact ' +
  'phone email address website online offline free open closed hours price prices from only j' +
  'ust more most best new news info information about here there this that with without your ' +
  'you our we are is it its the and for to of in on at by or not no yes can will get got make' +
  ' made use used find see look tap scan call book now learn read page pages story the end on' +
  'ce upon a time little big small slow fast said went came saw lived  New Zealand Aotearoa A' +
  'uckland Kia ora Hibiscus Coast village river beach walk history things to do getting there' +
  ' getting started how it works what it is good for tips links menu events local community s' +
  'afety emergency instructions setup step steps guide rules wifi password #000#111#222#333#4' +
  '44#555#666#777#888#999#aaa#bbb#ccc#ddd#eee#fff#f6f1e7#2f4a3alinear-gradient(radial-gradien' +
  't(rgba(0,0,0,.box-shadow:0 2px 8px transition:transform:translate(rotate(animation:@keyfra' +
  'mes @media(min-width:600px){grid-template-columns:1fr 1fr;gap:flex-direction:column;justif' +
  'y-content:space-between;align-items:center;position:relative;absolute;sticky;top:0;left:0;' +
  'right:0;overflow:hidden;white-space:nowrap;opacity:cursor:pointer;letter-spacing:.1em;text' +
  '-transform:uppercase;font-style:italic;font-weight:600;font-weight:700;font-weight:bold;li' +
  'ne-height:1.5;list-style:none;border-top:1px solid #border-bottom:border-left:border:1px s' +
  'olid #border-radius:12px;border-radius:50%;box-sizing:border-box;width:100%;height:100%;ma' +
  'x-width:100%;min-height:100vh;text-decoration:none;text-align:left;text-align:center;displ' +
  'ay:inline-block;display:flex;display:grid;display:block;margin-top:1em;margin-bottom:margi' +
  'n:0 auto;margin:0;padding:0 padding:1em;padding:12px 16px;font-size:1.2em;font-size:14px;f' +
  'ont-family:font:17px/1.5 Georgia,serif;font:17px/1.6 system-ui,sans-serif;background:linea' +
  'r-gradient(background:#fff;color:#fff;color:#}h1{}h2{}h3{}p{}a{}b{}i{}ul{}ol{}li{}small{}h' +
  'eader{}footer{}main{}section{}nav{}div{}span{}img{}svg{}code{}.c{}.b{}*{box-sizing:border-' +
  'box}html{}body{max-width:36em;margin:auto;padding:5vw;<svg xmlns="http://www.w3.org/2000/s' +
  'vg" viewBox="0 0 100 100" width="height="fill="none" stroke="stroke-width="<path d="M<circ' +
  'le cx="cy="r="<rect x="y="<text x="</svg><script>document.getElementById(querySelector(add' +
  'EventListener(\'click\',function(){onclick=textContent=innerHTML=style.classList.toggle(</sc' +
  'ript><table><tr><th></th><td></td></tr></table><img src="data:image/svg+xml,alt="<button><' +
  'input type="<label><form><br><hr><em></em><strong></strong><i></i><code></code><small></sm' +
  'all><span></span><div class="</div><div class=<div id=<section></section><header></header>' +
  '<footer></footer><main></main><nav></nav> target=_blank rel=noopener href="tel:+64href="ma' +
  'ilto:href="sms:href="geo:href="https://maps.google.com/?q=href="https://www.facebook.com/h' +
  'ref="https://www.instagram.com/.co.nz/">.com/">.org/">.nz"></a><a href=https://www.<a href' +
  '="https://<dl><dt></dt><dd></dd></dl><ol><li></ol><ul><li><li><b></b></ul></li><li></p><p>' +
  '<b></b><p></h3><h3></h2><h2></h1><h1><meta name="pocketpage-book" content="<html lang="en"' +
  '><head></head><body></body></html><title></title><style></style><body class=pp-plain><body' +
  ' class=pp-book><body class=pp-dark><!DOCTYPE html><!doctype html><meta charset="utf-8"><me' +
  'ta name="viewport" content="width=device-width,initial-scale=1"><meta charset=utf-8><meta ' +
  'name=viewport content="width=device-width"><meta charset=utf-8><meta name=viewport content' +
  '=width=device-width><title>';
const PP2_DICT = new TextEncoder().encode(PP2_DICT_TEXT);

/* ---------- compression (pako: the browser's CompressionStream can't take a dictionary) ---------- */
const deflate = async (str) => pako.deflateRaw(new TextEncoder().encode(str), { level: 9, dictionary: PP2_DICT });
const inflate = async (bytes) => {
  const out = pako.inflateRaw(bytes, { dictionary: PP2_DICT });
  if (!out) throw new Error('data is cut short');
  return new TextDecoder().decode(out);
};

/* ---------- built-in themes (same CSS as codec/Themes.kt) ---------- */
const THEME_VIEWPORT = '<meta name=viewport content=width=device-width>';
const THEME_BASE = 'body{margin:0 auto;max-width:36em;padding:5vw;font:17px/1.6 system-ui,sans-serif}img,svg{max-width:100%}';
const THEMES = {
  dark:
    'body{background:#0e1511;color:#d5e0d8}h1{font-size:2.4em;line-height:1.1;color:#67ea94;mar' +
    'gin:.4em 0 .2em}h2{color:#67ea94;font-size:.9em;letter-spacing:.15em;text-transform:upperc' +
    'ase;border-top:1px solid #24382c;padding-top:1.4em;margin-top:2em}b{color:#fff}li{margin:.' +
    '3em 0}::marker{color:#67ea94}code{background:#1c2b22;color:#67ea94;padding:.1em .4em;borde' +
    'r-radius:5px}a{display:block;margin:.5em 0;padding:.8em 1em;background:#15201a;border:1px ' +
    'solid #24382c;border-radius:12px;color:#67ea94;text-decoration:none}',
  book:
    'body{background:#fbf6ea;color:#3a2f25;font:20px/1.6 Georgia,serif;text-align:center}h1,h2{' +
    'color:#4f7a3e}h1{font-size:2em;line-height:1.2;margin:.3em 0}b{display:block;font-size:4em' +
    ';line-height:1.3;margin-top:.3em}a{color:#4f7a3e}',
  plain:
    'body{background:#fff;color:#1f2a24}h1{font-size:2em;line-height:1.2}h2{margin-top:1.6em}a{' +
    'color:#b4552d}',
};
function themeOf(html) {
  const m = /<body[^>]*\sclass=["']?[^"'>]*\bpp-(dark|book|plain)\b/i.exec(html);
  return m ? m[1].toLowerCase() : null;
}
/* Adds the theme's stylesheet (and a viewport tag if the page has none). The page's own CSS still wins. */
function applyTheme(html) {
  const name = themeOf(html);
  if (!name) return html;
  const head = (/viewport/i.test(html) ? '' : THEME_VIEWPORT) + '<style>' + THEME_BASE + THEMES[name] + '</style>';
  const d = /^\s*<!doctype[^>]*>/i.exec(html);
  const at = d ? d[0].length : 0;
  return html.slice(0, at) + head + html.slice(at);
}

/* ---------- base45 (RFC 9285) ---------- */
const B45 = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:';
function b45enc(u8) {
  let s = '';
  for (let i = 0; i < u8.length; i += 2) {
    if (i + 1 < u8.length) {
      let x = u8[i] * 256 + u8[i + 1];
      s += B45[x % 45] + B45[Math.floor(x / 45) % 45] + B45[Math.floor(x / 2025)];
    } else {
      s += B45[u8[i] % 45] + B45[Math.floor(u8[i] / 45)];
    }
  }
  return s;
}
function b45dec(str) {
  const out = [];
  for (let i = 0; i < str.length; i += 3) {
    const c = B45.indexOf(str[i]), d = B45.indexOf(str[i + 1]);
    if (c < 0 || d < 0) throw new Error('bad base45');
    if (i + 2 < str.length) {
      const e = B45.indexOf(str[i + 2]);
      if (e < 0) throw new Error('bad base45');
      const x = c + d * 45 + e * 2025;
      out.push(x >> 8, x & 255);
    } else out.push(c + d * 45);
  }
  return new Uint8Array(out);
}

/* ---------- base64url ---------- */
function b64uenc(u8) {
  let bin = ''; u8.forEach((b) => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function b64udec(s) {
  s = s.replace(/-/g, '+').replace(/_/g, '/');
  const bin = atob(s + '==='.slice((s.length + 3) % 4));
  return Uint8Array.from(bin, (c) => c.charCodeAt(0));
}

/* ---------- QR text and links ---------- */
const qrText = (zip) => QR_PREFIX + b45enc(zip);
/* Payload bytes, or null if the text isn't a PocketPage QR. Throws if it's damaged. */
function fromQrText(text) {
  const p = QR_PREFIXES.find((x) => text && text.startsWith(x));
  return p ? b45dec(text.slice(p.length)) : null;
}
/* Links always point at the hosted viewer, so tags/QRs made anywhere open the same place. */
const LINK_BASE = 'https://mariocannon.github.io/Offlinepages/';
const linkURL = (zip) => LINK_BASE + LINK_KEY + b64uenc(zip);
function fromLink(url) {
  const m = LINK_RE.exec(url);
  return m ? b64udec(m[1]) : null;
}
