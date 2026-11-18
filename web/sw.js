// Cache the whole app so it works with no signal after the first visit.
const CACHE = 'pocketpage-v4';
const FILES = ['./', 'index.html', 'app.js', 'codec.js', 'vendor/pako.min.js', 'vendor/qrcode.js', 'vendor/jsQR.js', 'manifest.webmanifest', 'icon.svg', 'icon-192.png', 'icon-512.png'];
self.addEventListener('install', (e) => { e.waitUntil(caches.open(CACHE).then((c) => c.addAll(FILES))); self.skipWaiting(); });
self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== CACHE).map((k) => caches.delete(k)))));
  self.clients.claim();
});
self.addEventListener('fetch', (e) => {
  if (e.request.method !== 'GET') return;
  const isNav = e.request.mode === 'navigate';
  e.respondWith(
    caches.match(isNav ? 'index.html' : e.request, { ignoreSearch: true }).then((hit) => hit || fetch(e.request))
  );
});
