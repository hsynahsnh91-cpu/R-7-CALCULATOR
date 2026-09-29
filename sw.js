/**
 * R-7 Service Worker
 * Version: r7-v1.2.0
 * Cache-first offline support
 */

const CACHE_NAME = "r7-v1.2.0";

const ASSETS = [
  "./",
  "./index.html",
  "./css/r7.css",
  "./js/engine.js",
  "./js/display.js",
  "./js/keypad.js",
  "./js/history.js",
  "./js/converter.js",
  "./js/programmer.js",
  "./js/settings.js",
  "./js/audio.js",
  "./js/i18n.js",
  "./manifest.json"
];

self.addEventListener("install", (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  e.respondWith(
    caches.match(e.request).then((cached) => {
      if (cached) return cached;
      return fetch(e.request).catch(() => caches.match("./index.html"));
    })
  );
});
