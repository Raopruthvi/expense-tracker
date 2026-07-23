const CACHE_NAME = 'spendsmart-v1';
const URLS_TO_CACHE = [
    '/expense-tracker/',
    '/expense-tracker/index.html',
    '/expense-tracker/expenses.html',
    '/expense-tracker/people.html',
    '/expense-tracker/budget.html',
    '/expense-tracker/month-end.html',
    '/expense-tracker/manifest.json'
];

// Install: cache all pages
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => cache.addAll(URLS_TO_CACHE))
    );
    self.skipWaiting();
});

// Activate: clear old caches
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        )
    );
    self.clients.claim();
});

// Fetch: network first, fallback to cache
self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);

    // For API calls: network only, no caching
    if (url.hostname === 'expense-tracker-production-8665.up.railway.app') {
        event.respondWith(
            fetch(event.request).catch(() =>
                new Response(JSON.stringify({
                    error: 'You are offline. Please connect to internet to sync data.'
                }), { headers: { 'Content-Type': 'application/json' } })
            )
        );
        return;
    }

    // For pages and assets: network first, cache fallback
    event.respondWith(
        fetch(event.request)
            .then(response => {
                const clone = response.clone();
                caches.open(CACHE_NAME).then(cache =>
                    cache.put(event.request, clone));
                return response;
            })
            .catch(() => caches.match(event.request))
    );
});