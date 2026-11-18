// Checks the web codec (web/codec.js) against the Android app's format.
// Run: node tools/web-codec-test.mjs
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import { crc32 } from 'node:zlib';

const web = new URL('../web/', import.meta.url);
const ctx = vm.createContext({ TextEncoder, TextDecoder, atob, btoa });
vm.runInContext(readFileSync(new URL('vendor/pako.min.js', web), 'utf8'), ctx);
vm.runInContext(readFileSync(new URL('codec.js', web), 'utf8'), ctx);
const c = vm.runInContext(
  '({ PP2_DICT, deflate, inflate, applyTheme, themeOf, qrText, fromQrText, linkURL, fromLink, b45enc })', ctx);

// The dictionary is frozen. Same numbers as PageCodecTest.dictionaryIsFrozen.
assert.equal(c.PP2_DICT.length, 3807);
assert.equal(crc32(c.PP2_DICT).toString(16), 'ad40c250');

// Round trip, and the dictionary really helps on a small page.
const page = '<meta charset=utf-8><meta name=viewport content=width=device-width><h1>Kia ora Ōrewa</h1><p>Hibiscus Coast</p>';
const zip = await c.deflate(page);
assert.equal(await c.inflate(zip), page);
assert.ok(zip.length < 60, `expected a small payload, got ${zip.length}`);
assert.equal(await c.inflate(c.fromQrText(c.qrText(zip))), page);
assert.ok(c.qrText(zip).startsWith('PP2:'));
assert.ok(c.linkURL(zip).startsWith('https://mariocannon.github.io/Offlinepages/#p2='));
assert.equal(await c.inflate(c.fromLink(c.linkURL(zip))), page);

// PP1 codes and links (no dictionary) still open. Same fixtures as PageCodecTest.
assert.equal(
  await c.inflate(c.fromQrText('PP1:ZWM376: UAT9$DP%H9J87LPANKBCADV6K.8DEOAQCT/CJEPLSVAG8AUIIS8AXOJ7BIB7AITKD6C00')),
  '<h1>Kia ora Ōrewa</h1><p>Made by the web app</p>');
assert.equal(
  await c.inflate(c.fromLink('https://mariocannon.github.io/Offlinepages/#p=s8kwtPPOTFTIL0pUONpTlFqeaKOfYWhnU2DnVpSfq5CokJOZl22jX2AHAA')),
  '<h1>Kia ora Ōrewa</h1><p>From a link</p>');

// A PP2 payload made by java.util.zip.Deflater, which is what the Android app uses.
assert.equal(
  await c.inflate(c.fromQrText('PP2:-TOWI6ZIVO 6:8IKR29:I3J8WLNJ2Q0$Q8D9ZUETC2ED1D6100')),
  '<body class=pp-dark><h1>Kia ora Ōrewa</h1><p>Made by the app</p>');

assert.equal(c.fromQrText('https://example.com'), null);
assert.equal(c.fromLink('https://example.com/page'), null);
await assert.rejects(c.inflate(zip.slice(0, zip.length >> 1)));

// Themes
assert.equal(c.themeOf('<body class=pp-dark><h1>x</h1>'), 'dark');
assert.equal(c.themeOf('<BODY id=a class="hero pp-book">'), 'book');
assert.equal(c.themeOf('<body class=pp-darker>'), null);
assert.equal(c.themeOf('<div class=pp-dark>'), null);
assert.equal(c.applyTheme('<h1>plain page</h1>'), '<h1>plain page</h1>');
const themed = c.applyTheme('<!doctype html><body class=pp-plain><h1>x</h1>');
assert.ok(themed.startsWith('<!doctype html><meta name=viewport content=width=device-width><style>body{'));
assert.ok(themed.endsWith('</style><body class=pp-plain><h1>x</h1>'));
assert.ok(!c.applyTheme('<meta name=viewport content=width=device-width><body class=pp-dark>').startsWith('<meta name=viewport content=width=device-width><meta'));

console.log('web codec ok');
console.log('fixture qr  ', c.qrText(await c.deflate('<body class=pp-book><h1>Kia ora Ōrewa</h1><p>Made by the web app</p>')));
console.log('fixture link', c.linkURL(await c.deflate('<body class=pp-book><h1>Kia ora Ōrewa</h1><p>From a link</p>')));
