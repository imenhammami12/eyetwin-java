package com.eyetwin.tools;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local tile proxy for JavaFX WebView.
 * Some networks/CDNs/OSM block JavaFX WebView requests; proxying via Java HTTP client fixes it.
 */
public final class TileProxyServer {

    private static volatile HttpServer server;
    private static volatile int port;

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Simple in-memory cache (z/x/y -> bytes)
    private static final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    // Avoid bursting upstream tile servers (429 / throttling)
    private static final Semaphore UPSTREAM_LIMIT = new Semaphore(6);

    private static final AtomicInteger tileOk = new AtomicInteger();
    private static final AtomicInteger tileFail = new AtomicInteger();
    private static volatile String lastTileError = "";

    // 1x1 MAGENTA PNG (stretched by Leaflet), makes failures visible (no more "black").
    private static final byte[] ERROR_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8z8AABQMBgk6n3cQAAAAASUVORK5CYII="
    );

    private TileProxyServer() {}

    public static synchronized int ensureStarted() {
        if (server != null) return port;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/tiles/", new TilesHandler());
            server.createContext("/ping", new PingHandler());
            server.createContext("/diag", new DiagHandler());
            server.createContext("/map", new MapPageHandler());
            // Serve Leaflet assets from app resources (no CDN dependency)
            server.createContext("/leaflet/leaflet.js", new ClasspathAssetHandler(
                    "/com/eyetwin/assets/leaflet/leaflet.js",
                    "application/javascript; charset=utf-8"
            ));
            server.createContext("/leaflet/leaflet.css", new ClasspathAssetHandler(
                    "/com/eyetwin/assets/leaflet/leaflet.css",
                    "text/css; charset=utf-8"
            ));
            server.createContext("/leaflet/images/", new ClasspathAssetHandler(
                    "/com/eyetwin/assets/leaflet/images/",
                    null
            ));
            // Leaflet loads many tiles concurrently; keep enough threads to avoid stalls.
            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();
            port = server.getAddress().getPort();
            return port;
        } catch (IOException e) {
            throw new RuntimeException("Failed to start tile proxy server: " + e.getMessage(), e);
        }
    }

    private static class DiagHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String json = "{\"ok\":" + tileOk.get() + ",\"fail\":" + tileFail.get()
                    + ",\"lastError\":\"" + escapeJson(lastTileError) + "\"}";
            byte[] body = json.getBytes();
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", "application/json; charset=utf-8");
            h.set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    private static class MapPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ensureStarted();
            String html = """
                    <!doctype html>
                    <html>
                      <head>
                        <meta charset="utf-8"/>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                        <link rel="stylesheet" href="/leaflet/leaflet.css"/>
                        <style>
                          /* Minimal Leaflet CSS fallback (enough to render tiles/controls) */
                          .leaflet-container { position: relative; overflow: hidden; }
                          .leaflet-pane, .leaflet-tile, .leaflet-marker-icon, .leaflet-marker-shadow,
                          .leaflet-tile-container, .leaflet-overlay-pane, .leaflet-shadow-pane,
                          .leaflet-marker-pane, .leaflet-tooltip-pane, .leaflet-popup-pane {
                            position: absolute; left: 0; top: 0;
                          }
                          .leaflet-tile { width: 256px; height: 256px; visibility: inherit; }
                          .leaflet-zoom-box { width: 0; height: 0; box-sizing: border-box; z-index: 800; }
                          .leaflet-layer { position: absolute; left: 0; top: 0; }
                          .leaflet-control-container { position: absolute; left: 0; top: 0; width: 100%%; height: 0; z-index: 1000; }
                          .leaflet-top, .leaflet-bottom { position: absolute; z-index: 1000; pointer-events: none; }
                          .leaflet-top { top: 0; }
                          .leaflet-bottom { bottom: 0; }
                          .leaflet-left { left: 0; }
                          .leaflet-right { right: 0; }
                          .leaflet-control { pointer-events: auto; }
                          .leaflet-bar { box-shadow: 0 1px 5px rgba(0,0,0,0.65); border-radius: 4px; }
                          .leaflet-bar a {
                            background-color: #fff;
                            border-bottom: 1px solid #ccc;
                            width: 26px; height: 26px;
                            line-height: 26px;
                            display: block;
                            text-align: center;
                            text-decoration: none;
                            color: #000;
                            font-weight: 700;
                            user-select: none;
                          }
                          .leaflet-bar a:last-child { border-bottom: none; }
                          .leaflet-disabled { opacity: 0.5; pointer-events: none; }

                          html, body { height: 100%%; width: 100%%; margin: 0; padding: 0; overflow: hidden; background: #0b1220; }
                          #map-picker { height: 100%%; width: 100%%; border-radius: 12px; overflow: hidden; }
                          .leaflet-container { background: #0b1220; }
                          .leaflet-control-container { user-select: none; }
                          /* WebView compatibility overrides: make tiles always visible */
                          img.leaflet-tile { visibility: visible !important; opacity: 1 !important; mix-blend-mode: normal !important; }
                          .leaflet-tile { visibility: visible !important; opacity: 1 !important; }
                          .leaflet-tile-loaded { visibility: visible !important; opacity: 1 !important; }
                          .leaflet-pane { transform: translate3d(0,0,0); }
                          #netStatus {
                            position: absolute;
                            top: 10px;
                            right: 10px;
                            z-index: 9999;
                            padding: 6px 10px;
                            border-radius: 10px;
                            background: rgba(0,0,0,0.55);
                            border: 1px solid rgba(255,255,255,0.18);
                            color: #e5e7eb;
                            font-size: 12px;
                            line-height: 1.2;
                            max-width: 70%%;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                          }
                        </style>
                      </head>
                      <body>
                        <div id="map-picker"></div>
                        <div id="netStatus">Tiles: …</div>
                        <script src="/leaflet/leaflet.js"></script>
                        <script>
                          document.addEventListener("DOMContentLoaded", function(){
                            let map;
                            let marker;

                            const defaultLat = 36.8065;
                            const defaultLng = 10.1815;

                            const statusEl = document.getElementById('netStatus');
                            function setStatus(text, ok){
                              if (!statusEl) return;
                              statusEl.textContent = text;
                              if (ok === true) {
                                statusEl.style.background = 'rgba(16,185,129,0.25)';
                                statusEl.style.borderColor = 'rgba(16,185,129,0.45)';
                              } else if (ok === false) {
                                statusEl.style.background = 'rgba(239,68,68,0.25)';
                                statusEl.style.borderColor = 'rgba(239,68,68,0.45)';
                              }
                            }

                            window.addEventListener('error', function(ev){
                              try {
                                setStatus('JS error: ' + (ev.message || 'unknown'), false);
                              } catch(e) {}
                            });

                            if (!window.L) {
                              setStatus('Leaflet not loaded (/leaflet/leaflet.js)', false);
                              return;
                            }

                            // Fix Leaflet default icon paths to our local resources.
                            try {
                              L.Icon.Default.mergeOptions({
                                iconRetinaUrl: '/leaflet/images/marker-icon-2x.png',
                                iconUrl: '/leaflet/images/marker-icon.png',
                                shadowUrl: '/leaflet/images/marker-shadow.png'
                              });
                            } catch(e) {}

                            map = L.map('map-picker', {
                              zoomControl: true,
                              attributionControl: false,
                              zoomAnimation: false,
                              markerZoomAnimation: false,
                              fadeAnimation: false
                            }).setView([defaultLat, defaultLng], 13);
                            setStatus('Leaflet OK (v' + (L.version || '?') + ')', true);

                            // Expose a manual resize hook for JavaFX layout timing.
                            window.forceResize = function(){
                              try { map.invalidateSize(false); } catch(e) {}
                            };

                            L.tileLayer('/tiles/{z}/{x}/{y}.png', {
                              maxZoom: 19,
                              crossOrigin: true,
                              updateWhenIdle: true,
                              updateWhenZooming: false,
                              keepBuffer: 2
                            }).addTo(map);

                            marker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(map);

                            async function reverseGeocode(lat, lng){
                              try {
                                const response = await fetch(
                                  `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`,
                                  { headers: { 'Accept': 'application/json' } }
                                );
                                const data = await response.json();
                                const text = (data && data.display_name) ? data.display_name : (lat + ", " + lng);
                                try {
                                  if (window.java && window.java.setLocation) window.java.setLocation(text, lat, lng);
                                } catch(err) {}
                              } catch (error) {
                                const text = lat + ", " + lng;
                                try {
                                  if (window.java && window.java.setLocation) window.java.setLocation(text, lat, lng);
                                } catch(err) {}
                              }
                            }

                            map.on('click', function (e) {
                              const lat = e.latlng.lat;
                              const lng = e.latlng.lng;
                              marker.setLatLng([lat, lng]);
                              reverseGeocode(lat, lng);
                            });

                            marker.on('dragend', function () {
                              const position = marker.getLatLng();
                              reverseGeocode(position.lat, position.lng);
                            });

                            function safeInvalidate(){
                              try { map.invalidateSize(false); } catch(e) {}
                            }
                            setTimeout(safeInvalidate, 50);
                            setTimeout(safeInvalidate, 250);
                            setTimeout(safeInvalidate, 750);
                            // Also react to any container resizes (JavaFX often resizes after showing the node).
                            try {
                              const ro = new ResizeObserver(function(){
                                setTimeout(safeInvalidate, 30);
                              });
                              ro.observe(document.getElementById('map-picker'));
                            } catch(e) {}

                            async function updateDiag(){
                              try {
                                const r = await fetch('/diag', { cache: 'no-store' });
                                const d = await r.json();
                                const ok = d.ok || 0;
                                const fail = d.fail || 0;
                                const last = d.lastError || '';
                                const el = document.getElementById('netStatus');
                                if (!el) return;
                                if (fail === 0 && ok > 0) {
                                  setStatus('Tiles: OK (' + ok + ')', true);
                                } else if (fail > 0) {
                                  setStatus('Tiles: BLOCKED (ok ' + ok + ', fail ' + fail + ') ' + last, false);
                                } else {
                                  setStatus('Tiles: loading…', null);
                                }
                              } catch (e) { /* ignore */ }
                            }
                            setInterval(updateDiag, 800);
                            setTimeout(updateDiag, 200);

                            reverseGeocode(defaultLat, defaultLng);
                          });
                        </script>
                      </body>
                    </html>
                    """;

            byte[] body = html.getBytes();
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", "text/html; charset=utf-8");
            h.set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            byte[] body = "ok".getBytes();
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", "text/plain");
            h.set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static class TilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                // /tiles/{z}/{x}/{y}.png
                String path = ex.getRequestURI().getPath();
                String rel = path.substring("/tiles/".length());
                if (rel.contains("..")) {
                    send(ex, 400, "Bad path".getBytes(), "text/plain");
                    return;
                }

                String key = rel;
                byte[] cached = cache.get(key);
                if (cached != null) {
                    tileOk.incrementAndGet();
                    send(ex, 200, cached, "image/png");
                    return;
                }

                // Use OSM tiles (same as Symfony), with fallbacks.
                // Note: tile providers can rate-limit; this is for local dev usage.
                String[] parts = rel.split("/");
                if (parts.length != 3) {
                    send(ex, 404, "Not found".getBytes(), "text/plain");
                    return;
                }
                String z = parts[0];
                String x = parts[1];
                String y = parts[2];
                if (!y.endsWith(".png")) {
                    send(ex, 404, "Not found".getBytes(), "text/plain");
                    return;
                }
                y = y.substring(0, y.length() - 4);

                URI[] sources = new URI[] {
                        URI.create("https://tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://a.tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://b.tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://c.tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://a.tile.openstreetmap.fr/hot/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://a.tile.openstreetmap.de/" + z + "/" + x + "/" + y + ".png"),
                        URI.create("https://a.basemaps.cartocdn.com/dark_all/" + z + "/" + x + "/" + y + ".png")
                };

                byte[] body = null;
                int lastStatus = 0;
                URI lastUri = null;
                for (URI tileUri : sources) {
                    lastUri = tileUri;
                    boolean acquired = false;
                    try {
                        UPSTREAM_LIMIT.acquire();
                        acquired = true;
                        HttpRequest req = HttpRequest.newBuilder(tileUri)
                                .timeout(Duration.ofSeconds(6))
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36")
                                .header("Accept", "image/png,image/*;q=0.8,*/*;q=0.5")
                                .header("Accept-Language", "en-US,en;q=0.8,fr;q=0.7")
                                .header("Referer", "https://www.openstreetmap.org/")
                                .GET()
                                .build();

                        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                        lastStatus = resp.statusCode();
                        if (lastStatus == 200 && resp.body() != null && resp.body().length > 0) {
                            body = resp.body();
                            break;
                        }
                    } finally {
                        if (acquired) UPSTREAM_LIMIT.release();
                    }
                }

                if (body == null) {
                    // Important: Leaflet expects an image. Returning text causes blank/black tiles.
                    lastTileError = "status=" + lastStatus;
                    tileFail.incrementAndGet();
                    System.err.println("[TileProxy] Tile fetch failed for " + rel
                            + " lastStatus=" + lastStatus
                            + " lastUri=" + (lastUri != null ? lastUri : "null"));
                    Headers h = ex.getResponseHeaders();
                    h.set("X-Tile-Error", "fetch_failed_" + lastStatus);
                    send(ex, 200, ERROR_PNG, "image/png");
                    return;
                }

                // cache modestly
                if (cache.size() < 2000) cache.put(key, body);
                tileOk.incrementAndGet();
                send(ex, 200, body, "image/png");
            } catch (Exception e) {
                System.err.println("[TileProxy] Tile handler error: " + e);
                lastTileError = e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
                tileFail.incrementAndGet();
                Headers h = ex.getResponseHeaders();
                h.set("X-Tile-Error", "exception");
                send(ex, 200, ERROR_PNG, "image/png");
            }
        }

        private void send(HttpExchange ex, int status, byte[] body, String contentType) throws IOException {
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", contentType);
            h.set("Access-Control-Allow-Origin", "*");
            h.set("Cache-Control", "public, max-age=86400");
            ex.sendResponseHeaders(status, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static class ClasspathAssetHandler implements HttpHandler {
        private final String baseResourcePath;
        private final String fixedContentType;
        private volatile byte[] cached; // used only for fixed file paths

        private ClasspathAssetHandler(String baseResourcePath, String fixedContentType) {
            this.baseResourcePath = baseResourcePath;
            this.fixedContentType = fixedContentType;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                String resPath = baseResourcePath;
                if (resPath.endsWith("/")) {
                    // directory mapping: /leaflet/images/<name>
                    String reqPath = ex.getRequestURI().getPath();
                    String rel = reqPath.substring("/leaflet/images/".length());
                    if (rel.contains("..") || rel.isBlank()) {
                        send(ex, 400, "Bad path".getBytes(), "text/plain");
                        return;
                    }
                    resPath = baseResourcePath + rel;
                } else if (cached != null) {
                    send(ex, 200, cached, fixedContentType);
                    return;
                }

                try (InputStream is = TileProxyServer.class.getResourceAsStream(resPath)) {
                    if (is == null) {
                        send(ex, 404, "Not found".getBytes(), "text/plain");
                        return;
                    }
                    byte[] body = is.readAllBytes();
                    String ct = fixedContentType != null ? fixedContentType : guessContentType(resPath);
                    if (!baseResourcePath.endsWith("/")) cached = body;
                    send(ex, 200, body, ct);
                }
            } catch (Exception e) {
                send(ex, 500, ("Error: " + e.getMessage()).getBytes(), "text/plain");
            }
        }

        private String guessContentType(String resPath) {
            String p = resPath.toLowerCase();
            if (p.endsWith(".png")) return "image/png";
            if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
            if (p.endsWith(".css")) return "text/css; charset=utf-8";
            if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
            return "application/octet-stream";
        }

        private void send(HttpExchange ex, int status, byte[] body, String contentType) throws IOException {
            Headers h = ex.getResponseHeaders();
            h.set("Content-Type", contentType);
            h.set("Access-Control-Allow-Origin", "*");
            h.set("Cache-Control", "public, max-age=86400");
            ex.sendResponseHeaders(status, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }
}

