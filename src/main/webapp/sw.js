const CACHE_NAME = 'sec-voting-v3';
const ASSETS_TO_CACHE = [
  'assets/css/style.css',
  'assets/js/admin.js',
  'assets/js/theme.js',
  'assets/js/user.js',
  'assets/images/hero.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('[SW] Caching critical assets');
        // Add assets individually to prevent one failure from stopping all
        return Promise.allSettled(
          ASSETS_TO_CACHE.map(url => 
            cache.add(url).catch(err => console.warn(`[SW] Failed to cache: ${url}`, err))
          )
        );
      })
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => Promise.all(
      keys.map(key => {
        if (key !== CACHE_NAME) return caches.delete(key);
      })
    )).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const requestUrl = new URL(event.request.url);
  const isSameOrigin = requestUrl.origin === self.location.origin;
  const isCacheableAsset = isSameOrigin
    && event.request.method === 'GET'
    && requestUrl.pathname.includes('/assets/');

  if (!isCacheableAsset) {
    event.respondWith(fetch(event.request));
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then(response => response || fetch(event.request))
  );
});
