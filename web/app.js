/* PocketPage — offline landing pages in QR codes and NFC tags.
 *
 * Payload = raw-DEFLATE of the page's UTF-8 HTML.
 *   QR:        "PP1:" + base45(payload)   (QR alphanumeric mode, ~97% efficient)
 *   NFC app:   MIME record "application/x-pp", bytes = payload
 *   Link:      https://mariocannon.github.io/Offlinepages/#p=<base64url(payload)>
 */
'use strict';

const QR_PREFIX = 'PP1:';
const NFC_MIME = 'application/x-pp';
const $ = (id) => document.getElementById(id);

/* ---------- compression ---------- */
async function pipe(bytes, stream) {
  const out = new Blob([bytes]).stream().pipeThrough(stream);
  return new Uint8Array(await new Response(out).arrayBuffer());
}
const deflate = (str) => pipe(new TextEncoder().encode(str), new CompressionStream('deflate-raw'));
const inflate = async (bytes) => new TextDecoder().decode(await pipe(bytes, new DecompressionStream('deflate-raw')));

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

/* ---------- viewer ---------- */
function openPage(html, info, bare) {
  $('viewer').classList.toggle('bare', !!bare);
  $('vInfo').textContent = info || 'Offline page';
  $('vFrame').srcdoc = html;
  $('viewer').classList.add('on');
}
$('vClose').onclick = () => {
  $('viewer').classList.remove('on');
  $('vFrame').srcdoc = '';
  if (location.hash) history.replaceState(null, '', location.pathname);
};

/* ---------- tabs ---------- */
document.querySelectorAll('nav button').forEach((b) => (b.onclick = () => {
  document.querySelectorAll('nav button').forEach((x) => x.classList.toggle('on', x === b));
  document.querySelectorAll('main section').forEach((s) => s.classList.toggle('on', s.id === b.dataset.tab));
  if (b.dataset.tab !== 'scan') stopCam();
}));

/* ---------- create ---------- */
const SAMPLE = `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>The Tide</title><style>
body{margin:0;font:17px/1.5 system-ui,sans-serif;color:#13232b;background:#f4f1ea}
.h{background:linear-gradient(160deg,#0b3d4f,#1d7a8c);color:#fff;padding:48px 24px 64px;text-align:center}
.h h1{margin:12px 0 6px;font-size:34px}.h p{margin:0;opacity:.85}
.w{max-width:520px;margin:-36px auto 0;padding:0 16px}
.c{background:#fff;border-radius:16px;padding:20px;box-shadow:0 6px 24px #0002;margin-bottom:14px}
.c h2{margin:0 0 6px;font-size:18px}
a.b{display:block;text-align:center;background:#e2683c;color:#fff;padding:14px;border-radius:12px;text-decoration:none;font-weight:700;margin-top:12px}
ul{padding-left:20px;margin:6px 0}small{display:block;text-align:center;color:#5d6b70;padding:18px}
</style></head><body>
<div class="h"><svg width="56" height="56" viewBox="0 0 56 56"><circle cx="28" cy="28" r="27" fill="#fff2"/><path d="M8 34c6-8 12-8 18 0s12 8 22 0" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round"/><path d="M8 24c6-8 12-8 18 0s12 8 22 0" stroke="#fff8" stroke-width="3" fill="none" stroke-linecap="round"/></svg>
<h1>The Tide</h1><p>Hibiscus Coast news, every Thursday</p></div>
<div class="w"><div class="c"><h2>What you get</h2><ul><li>Local events &amp; markets</li><li>New openings &amp; deals</li><li>Council news in 2 minutes</li></ul>
<a class="b" href="https://example.com/subscribe">Subscribe free</a></div>
<div class="c"><h2>Loaded offline</h2><p>This page came from the tag or code you just scanned. No internet needed to read it.</p></div>
<small>Ōrewa · Whangaparāoa · Silverdale · Millwater · Puhoi</small></div></body></html>`;

const TINY = `<!doctype html><meta name=viewport content="width=device-width"><title>The Tide</title><style>body{margin:0;font:18px system-ui;background:#f4f1ea;color:#13232b;text-align:center}header{background:#0b3d4f;color:#fff;padding:40px 20px}h1{margin:0}p{padding:0 20px}a{display:block;margin:20px;padding:14px;background:#e2683c;color:#fff;border-radius:12px;text-decoration:none;font-weight:700}</style><header><h1>〰 The Tide</h1>Hibiscus Coast news, every Thursday</header><p>Events, openings, deals &amp; council news in 2 minutes.</p><a href="https://example.com/subscribe">Subscribe free</a><p><small>Loaded offline from an NFC tag</small></p>`;

let current = { html: '', zip: null };
let qrSvg = '';

function minify(h) {
  return h.replace(/<!--[\s\S]*?-->/g, '').replace(/\n\s*/g, '\n').replace(/>\s+</g, '><').replace(/\s{2,}/g, ' ').trim();
}

const TAGS = [
  ['NTAG213', 144], ['NTAG215', 504], ['NTAG216', 888], ['NTAG I2C plus 2K', 1912], ['DESFire / ST25 8K', 7600],
];
// NDEF overhead: TLV (4) + terminator (1) + long record header (6) + type
const appNeed = (n) => n + 4 + 1 + 6 + NFC_MIME.length;
// Links always point at the hosted viewer, so tags/QRs made anywhere open the same place.
const LINK_BASE = 'https://mariocannon.github.io/Offlinepages/';
const linkURL = (zip) => LINK_BASE + '#p=' + b64uenc(zip);
const linkNeed = (zip) => linkURL(zip).replace(/^https:\/\//, '').length + 4 + 1 + 6 + 1 + 1;

let t;
$('html').addEventListener('input', () => { clearTimeout(t); t = setTimeout(refresh, 250); });

async function refresh() {
  const html = $('html').value;
  current.html = html;
  current.zip = html ? await deflate(html) : null;
  const raw = new TextEncoder().encode(html).length, z = current.zip ? current.zip.length : 0;
  $('sRaw').textContent = raw.toLocaleString();
  $('sZip').textContent = z.toLocaleString();
  $('sRatio').textContent = raw ? (raw / Math.max(z, 1)).toFixed(1) + '× smaller' : '';
  // QR estimate
  const qlen = z ? QR_PREFIX.length + Math.ceil(z / 2) * 3 : 0;
  const cap = $('ecl').value === 'L' ? 4296 : 3391; // alphanumeric chars at v40
  const pct = Math.min(100, (qlen / cap) * 100);
  $('qBytes').textContent = z ? `${qlen.toLocaleString()} chars (${z} B)` : '–';
  $('qBar').style.width = pct + '%';
  $('qBar').style.background = pct > 100 ? 'var(--bad)' : pct > 60 ? 'var(--warn)' : 'var(--ok)';
  // tag table
  $('tagTbl').innerHTML = TAGS.map(([n, c]) => {
    const a = z ? appNeed(z) : 0, l = z ? linkNeed(current.zip) : 0;
    const p = (need) => !z ? '–' : need <= c ? `<span class="pill">✓ ${need}</span>` : `<span class="pill bad">✗ ${need}</span>`;
    return `<tr><td><b>${n}</b> <span class="muted">${c} B</span></td><td>app ${p(a)}</td><td>link ${p(l)}</td></tr>`;
  }).join('');
}

$('btnSample').onclick = () => { $('html').value = SAMPLE; refresh(); };
$('btnTiny').onclick = () => { $('html').value = TINY; refresh(); };
$('btnMinify').onclick = () => { $('html').value = minify($('html').value); refresh(); };
$('btnPreview').onclick = () => current.html && openPage(current.html, 'Preview');
$('ecl').onchange = refresh;

$('btnQR').onclick = async () => {
  await refresh();
  if (!current.zip) return;
  const text = QR_PREFIX + b45enc(current.zip);
  try {
    const qr = qrcode(0, $('ecl').value);
    qr.addData(text, 'Alphanumeric');
    qr.make();
    const ver = (qr.getModuleCount() - 17) / 4;
    qrSvg = qr.createSvgTag({ cellSize: 4, margin: 4, scalable: true });
    $('qrOut').innerHTML = qrSvg;
    const pill = ver <= 20 ? ['', 'easy scan'] : ver <= 30 ? ['warn', 'print large'] : ['bad', 'very dense'];
    $('qPill').innerHTML = `<span class="pill ${pill[0]}">v${ver} · ${pill[1]}</span>`;
    $('qNote').textContent = `Version ${ver} (${qr.getModuleCount()}×${qr.getModuleCount()} modules). Print at least ${Math.ceil(qr.getModuleCount() * 0.5 / 10)} cm wide for phone scanning.`;
    $('btnSaveQR').disabled = false;
  } catch (e) {
    $('qrOut').innerHTML = '';
    $('qPill').innerHTML = '<span class="pill bad">too big</span>';
    $('qNote').textContent = 'Page is too big for one QR code. Minify it, trim CSS, or use error level L.';
  }
};
$('btnLinkQR').onclick = async () => {
  await refresh();
  if (!current.zip) return;
  try {
    const qr = qrcode(0, $('ecl').value);
    qr.addData(linkURL(current.zip), 'Byte');
    qr.make();
    const ver = (qr.getModuleCount() - 17) / 4;
    qrSvg = qr.createSvgTag({ cellSize: 4, margin: 4, scalable: true });
    $('qrOut').innerHTML = qrSvg;
    $('qPill').innerHTML = `<span class="pill ${ver <= 20 ? '' : ver <= 30 ? 'warn' : 'bad'}">v${ver} · link</span>`;
    $('qNote').textContent = `Link QR, version ${ver}. Any phone camera opens it. First scan on a phone needs internet; after that it works offline.`;
    $('btnSaveQR').disabled = false;
  } catch (e) {
    $('qrOut').innerHTML = '';
    $('qNote').textContent = 'Too big for a link QR. Use the app-only QR or trim the page.';
  }
};
$('btnSaveQR').onclick = () => {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([qrSvg], { type: 'image/svg+xml' }));
  a.download = 'pocketpage-qr.svg'; a.click();
};

/* ---------- NFC write ---------- */
const hasNFC = 'NDEFReader' in window;
async function writeTag(records, label) {
  const msg = $('nfcWriteMsg');
  if (!hasNFC) { msg.textContent = 'Web NFC needs Chrome on Android (served over HTTPS).'; return; }
  await refresh();
  if (!current.zip) { msg.textContent = 'Add some HTML first.'; return; }
  try {
    const ndef = new NDEFReader();
    msg.textContent = `Hold a tag to the back of the phone… (${label})`;
    await ndef.write({ records: records() });
    msg.innerHTML = `<span class="pill">✓ Written</span> ${label}`;
  } catch (e) {
    msg.innerHTML = `<span class="pill bad">✗</span> ${e.name}: ${e.message}`;
  }
}
$('btnWriteApp').onclick = () => writeTag(() => [{ recordType: 'mime', mediaType: NFC_MIME, data: current.zip }], 'app data');
$('btnWriteLink').onclick = () => writeTag(() => [{ recordType: 'url', data: linkURL(current.zip) }], 'link');

/* ---------- NFC read ---------- */
$('nfcSupport').textContent = hasNFC ? 'Web NFC available ✓' : 'This browser has no Web NFC (iPhone / desktop). Link tags still open by tapping the phone.';
$('btnNfcScan').disabled = !hasNFC;
$('btnNfcScan').onclick = async () => {
  try {
    const ndef = new NDEFReader();
    await ndef.scan();
    $('nfcMsg').textContent = 'Ready — tap a tag.';
    ndef.onreadingerror = () => ($('nfcMsg').textContent = 'Could not read that tag, try again.');
    ndef.onreading = async ({ message, serialNumber }) => {
      for (const r of message.records) {
        try {
          if (r.recordType === 'mime' && r.mediaType === NFC_MIME) {
            const bytes = new Uint8Array(r.data.buffer, r.data.byteOffset, r.data.byteLength);
            return openPage(await inflate(bytes), `NFC tag · ${bytes.length} B`);
          }
          if (r.recordType === 'url') {
            const url = new TextDecoder().decode(r.data);
            const m = url.match(/#p=([\w-]+)/);
            if (m) { const b = b64udec(m[1]); return openPage(await inflate(b), `NFC link · ${b.length} B`); }
          }
        } catch (e) { $('nfcMsg').textContent = 'Tag data damaged: ' + e.message; return; }
      }
      $('nfcMsg').textContent = `Tag ${serialNumber || ''} has no PocketPage on it.`;
    };
  } catch (e) { $('nfcMsg').textContent = `${e.name}: ${e.message}`; }
};

/* ---------- QR scan ---------- */
let stream = null, raf = 0, detector = null;
const canvas = document.createElement('canvas');
const ctx = canvas.getContext('2d', { willReadFrequently: true });
const log = (s) => ($('log').textContent = s);

async function handleQR(text) {
  if (!text || !text.startsWith(QR_PREFIX)) { log('Found a QR, but it isn’t a PocketPage.'); return false; }
  try {
    const bytes = b45dec(text.slice(QR_PREFIX.length));
    const html = await inflate(bytes);
    stopCam();
    openPage(html, `QR · ${bytes.length} B`);
    return true;
  } catch (e) { log('QR damaged: ' + e.message); return false; }
}

async function startCam() {
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1920 }, height: { ideal: 1080 } }, audio: false,
    });
    const v = $('video'); v.srcObject = stream; await v.play();
    if ('BarcodeDetector' in window) {
      try { detector = new BarcodeDetector({ formats: ['qr_code'] }); } catch { detector = null; }
    }
    log(detector ? 'Scanning (native detector)…' : 'Scanning (jsQR)…');
    tick();
  } catch (e) { log('Camera error: ' + e.message); }
}
function stopCam() {
  cancelAnimationFrame(raf);
  if (stream) stream.getTracks().forEach((t) => t.stop());
  stream = null;
}
let busy = false;
async function tick() {
  raf = requestAnimationFrame(tick);
  const v = $('video');
  if (busy || v.readyState < 2) return;
  busy = true;
  try {
    if (detector) {
      const r = await detector.detect(v);
      if (r.length && (await handleQR(r[0].rawValue))) return;
    } else {
      const w = v.videoWidth, h = v.videoHeight;
      const s = Math.min(w, h) * 0.85; // centre crop
      canvas.width = canvas.height = Math.round(Math.min(s, 1000));
      ctx.drawImage(v, (w - s) / 2, (h - s) / 2, s, s, 0, 0, canvas.width, canvas.height);
      const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
      const r = jsQR(img.data, img.width, img.height, { inversionAttempts: 'dontInvert' });
      if (r && (await handleQR(r.data))) return;
    }
  } catch (e) { /* keep scanning */ }
  finally { busy = false; }
}
$('btnCam').onclick = startCam;
$('btnCamStop').onclick = () => { stopCam(); log('Stopped.'); };

/* ---------- link-tag / URL entry point ---------- */
async function fromHash() {
  const m = location.hash.match(/^#p=([\w-]+)/);
  if (!m) return;
  try { const b = b64udec(m[1]); openPage(await inflate(b), `Link · ${b.length} B`, true); }
  catch (e) { alert('That link is damaged.'); }
}
window.addEventListener('hashchange', fromHash);

/* ---------- boot ---------- */
$('html').value = SAMPLE;
refresh();
fromHash();
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js');
