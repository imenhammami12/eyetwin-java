package com.eyetwin.tools;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IFeedbackService;
import com.eyetwin.services.FeedbackServiceImpl;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject; // si tu as la dépendance
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FeedbackHttpServer {

    private static HttpServer server;

    public static void start() {
        new Thread(() -> {
            try {
                server = HttpServer.create(
                        new InetSocketAddress(8080), 0);

                server.createContext("/feedback/form", exchange -> {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        Map<String, String> params = parseQuery(query);
                        String streamId = params.getOrDefault("streamId", "0");
                        String userId   = params.getOrDefault("userId", "0");

                        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Rate your Stream</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        background: #0a0514;
                        font-family: Arial, sans-serif;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .card {
                        background: #0d0618;
                        border: 1px solid rgba(255,60,100,0.25);
                        border-radius: 18px;
                        padding: 40px;
                        width: 100%;
                        max-width: 480px;
                    }
                    .top-bar {
                        height: 5px;
                        background: linear-gradient(to right, #ff3c64, #a78bfa, #4facfe);
                        border-radius: 18px 18px 0 0;
                        margin: -40px -40px 30px -40px;
                    }
                    h1 {
                        color: white;
                        font-size: 22px;
                        margin-bottom: 8px;
                    }
                    p.sub {
                        color: rgba(255,255,255,0.45);
                        font-size: 13px;
                        margin-bottom: 28px;
                    }
                    label {
                        color: white;
                        font-size: 14px;
                        font-weight: bold;
                        display: block;
                        margin-bottom: 8px;
                    }
                    .stars {
                        display: flex;
                        gap: 10px;
                        margin-bottom: 24px;
                    }
                    .star-btn {
                        width: 50px;
                        height: 50px;
                        border-radius: 50%;
                        border: 2px solid rgba(255,255,255,0.15);
                        background: rgba(255,255,255,0.05);
                        color: white;
                        font-size: 18px;
                        cursor: pointer;
                        transition: all 0.2s;
                    }
                    .star-btn:hover, .star-btn.selected {
                        background: rgba(255,60,100,0.20);
                        border-color: #ff3c64;
                        color: #ff3c64;
                    }
                    textarea {
                        width: 100%;
                        background: rgba(255,255,255,0.04);
                        border: 1px solid rgba(255,255,255,0.09);
                        border-radius: 10px;
                        color: white;
                        font-size: 13px;
                        padding: 12px 14px;
                        resize: vertical;
                        min-height: 100px;
                        margin-bottom: 24px;
                        font-family: Arial, sans-serif;
                    }
                    textarea::placeholder { color: rgba(255,255,255,0.28); }
                    button[type=submit] {
                        width: 100%;
                        padding: 14px;
                        background: linear-gradient(to right, #ff3c64, #c0132f);
                        border: none;
                        border-radius: 10px;
                        color: white;
                        font-size: 14px;
                        font-weight: bold;
                        cursor: pointer;
                    }
                    .success {
                        display: none;
                        text-align: center;
                        padding: 40px 0;
                    }
                    .success h2 { color: #3dd68c; font-size: 20px; margin: 16px 0 8px; }
                    .success p  { color: rgba(255,255,255,0.45); font-size: 13px; }
                    .err { color: #ff6b7a; font-size: 12px; margin-top: -16px; margin-bottom: 16px; }
                </style>
            </head>
            <body>
            <div class="card">
                <div class="top-bar"></div>
                <h1>⭐ Rate your Stream</h1>
                <p class="sub">Share your experience — it helps us improve!</p>

                <div id="formDiv">
                    <label>Your Rating</label>
                    <div class="stars">
                        <button type="button" class="star-btn" onclick="selectRating(1)">1</button>
                        <button type="button" class="star-btn" onclick="selectRating(2)">2</button>
                        <button type="button" class="star-btn" onclick="selectRating(3)">3</button>
                        <button type="button" class="star-btn" onclick="selectRating(4)">4</button>
                        <button type="button" class="star-btn" onclick="selectRating(5)">5</button>
                    </div>
                    <p id="errRating" class="err" style="display:none">Please select a rating.</p>

                    <label>Your Comment</label>
                    <textarea id="comment" placeholder="Tell us about your experience..."></textarea>

                    <button type="submit" onclick="submitFeedback()">🚀 Submit Feedback</button>
                </div>

                <div class="success" id="successDiv">
                    <div style="font-size:48px">✅</div>
                    <h2>Thank you!</h2>
                    <p>Your feedback has been recorded.</p>
                </div>
            </div>

            <script>
                let selectedRating = 0;
                const streamId = '%s';
                const userId   = '%s';

                function selectRating(n) {
                    selectedRating = n;
                    document.querySelectorAll('.star-btn').forEach((b, i) => {
                        b.classList.toggle('selected', i < n);
                    });
                    document.getElementById('errRating').style.display = 'none';
                }

                function submitFeedback() {
                    if (selectedRating === 0) {
                        document.getElementById('errRating').style.display = 'block';
                        return;
                    }
                    const comment = document.getElementById('comment').value;
                    fetch('/feedback/submit', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            streamId: streamId,
                            userId:   userId,
                            rating:   String(selectedRating),
                            comment:  comment
                        })
                    })
                    .then(r => r.json())
                    .then(data => {
                        document.getElementById('formDiv').style.display = 'none';
                        document.getElementById('successDiv').style.display = 'block';
                    })
                    .catch(err => alert('Error: ' + err));
                }
            </script>
            </body>
            </html>
            """.formatted(streamId, userId);

                        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.getResponseBody().close();

                    } catch (Exception e) {
                        exchange.sendResponseHeaders(500, -1);
                    }
                });
                server.createContext("/feedback/submit", exchange -> {
                    if ("POST".equals(exchange.getRequestMethod())) {
                        handleFeedback(exchange);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                });
                server.createContext("/health", exchange -> {
                    String response = "{\"status\":\"ok\"}";
                    exchange.getResponseHeaders()
                            .set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(
                            response.getBytes());
                    exchange.getResponseBody().close();
                });

                server.start();
                System.out.println(
                        "[FeedbackServer] ✅ Running on port 8080");

            } catch (Exception e) {
                System.err.println(
                        "[FeedbackServer] ❌ " + e.getMessage());
            }
        }, "FeedbackHttpServer").start();
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }

    private static void handleFeedback(HttpExchange exchange) {
        try {
            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            // ← Ajoute ce log
            System.out.println("[FeedbackServer] RAW body: " + body);

            Map<String, String> params = parseBody(body);
            System.out.println("[FeedbackServer] Parsed: " + params);

            int streamId = parseIntSafe(params.getOrDefault("streamId", "0"));
            int userId   = parseIntSafe(params.getOrDefault("userId", "0"));
            int rating   = parseIntSafe(params.getOrDefault("rating", "3"));
            String comment = params.getOrDefault("comment", "");

            // Crée les entités
            LiveStream ls = new LiveStream();
            ls.setId(streamId);

            User user = new User();
            user.setId(userId);

            StreamFeedback fb = new StreamFeedback();
            fb.setLiveStream(ls);
            fb.setSpectator(user);
            fb.setRating(rating);
            fb.setComment(comment);

            // Sauvegarde
            IFeedbackService svc = new FeedbackServiceImpl();
            svc.processFeedback(fb);

            // Réponse OK
            String response = "{\"status\":\"ok\"}";
            exchange.getResponseHeaders()
                    .set("Content-Type", "application/json");
            exchange.getResponseHeaders()
                    .set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();

            System.out.println(
                    "[FeedbackServer] ✅ Feedback saved — " +
                            "stream:" + streamId + " user:" + userId +
                            " rating:" + rating);

        } catch (Exception e) {
            System.err.println(
                    "[FeedbackServer] ❌ Error: " + e.getMessage());
            try {
                String err = "{\"error\":\"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, err.length());
                exchange.getResponseBody().write(err.getBytes());
                exchange.getResponseBody().close();
            } catch (Exception ignored) {}
        }
    }

    private static Map<String, String> parseBody(String body) {
        Map<String, String> map = new HashMap<>();
        try {
            body = body.trim();
            // Retire les guillemets et = des valeurs
            body = body.replaceAll("\"=\\{\\{[^}]*\\}\\}\"", "\"\"");
            body = body.replaceAll("=\\{\\{[^}]*\\}\\}", "");

            // Parse manuel simple
            body = body.substring(1, body.length() - 1);
            for (String pair : body.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                pair = pair.trim();
                int colon = pair.indexOf(":");
                if (colon > 0) {
                    String key   = pair.substring(0, colon).trim().replace("\"", "");
                    String value = pair.substring(colon + 1).trim().replace("\"", "");
                    if (!value.isEmpty()) map.put(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println("[parseBody] " + e.getMessage());
        }
        return map;
    }

    private static int parseIntSafe(String value) {
        if (value == null) return 0;
        String cleaned = value.replaceAll("[^0-9]", "").trim();
        if (cleaned.isEmpty()) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(
                            URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                    );
                } catch (Exception ignored) {}
            }
        }
        return map;
    }
}