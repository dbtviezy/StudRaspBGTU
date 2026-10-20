const VERSION = "studrasp-v1.1-1";
const SHELL = `${VERSION}-shell`;
const STATIC = `${VERSION}-static`;
const SHELL_URLS = [
  "/",
  "/schedule.json",
  "/manifest.json",
  "/icons/mark.png",
  "/icons/icon-192.png",
  "/icons/apple-touch-icon.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(SHELL);
      await cache.addAll(SHELL_URLS);
      // Pull the hashed Next.js chunks referenced by the shell so the first offline launch works.
      try {
        const html = await (await cache.match("/")).text();
        const assets = [...new Set(html.match(/\/_next\/static\/[^"'\s)\\]+/g) || [])];
        const statics = await caches.open(STATIC);
        await Promise.all(assets.map((url) => statics.add(url).catch(() => undefined)));
      } catch {}
      await self.skipWaiting();
    })(),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(keys.filter((key) => !key.startsWith(VERSION)).map((key) => caches.delete(key)));
      await self.clients.claim();
    })(),
  );
});

async function networkFirst(request, cacheName, fallbackUrl) {
  const cache = await caches.open(cacheName);
  try {
    const response = await fetch(request);
    if (response.ok) cache.put(fallbackUrl || request, response.clone());
    return response;
  } catch (error) {
    const cached = await cache.match(fallbackUrl || request);
    if (cached) return cached;
    throw error;
  }
}

async function staleWhileRevalidate(event, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(event.request);
  const network = fetch(event.request)
    .then((response) => {
      if (response.ok) cache.put(event.request, response.clone());
      return response;
    })
    .catch(() => undefined);
  if (cached) {
    event.waitUntil(network);
    return cached;
  }
  const response = await network;
  return response || Response.error();
}

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (url.pathname.startsWith("/api/")) {
    event.respondWith(networkFirst(request, `${VERSION}-api`));
    return;
  }
  if (request.mode === "navigate") {
    event.respondWith(networkFirst(request, SHELL, "/"));
    return;
  }
  if (url.pathname === "/schedule.json" || url.pathname === "/manifest.json") {
    event.respondWith(staleWhileRevalidate(event, SHELL));
    return;
  }
  if (url.pathname.startsWith("/_next/static/") || url.pathname.startsWith("/icons/")) {
    event.respondWith(staleWhileRevalidate(event, STATIC));
  }
});
