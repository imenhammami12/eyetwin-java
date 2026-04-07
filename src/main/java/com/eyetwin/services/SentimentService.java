package com.eyetwin.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class SentimentService {

    private static final String HF_API_URL =
            "https://api-inference.huggingface.co/models/cardiffnlp/twitter-roberta-base-sentiment-latest";

    private static final double URGENT_THRESHOLD = 0.75;
    private static final double HIGH_THRESHOLD   = 0.55;

    private final String apiKey;
    private final HttpClient httpClient;

    public SentimentService() {
        this(System.getenv("HUGGINGFACE_API_KEY") != null
                ? System.getenv("HUGGINGFACE_API_KEY") : "");
    }

    public SentimentService(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    // ──────────────────────────────────────────────────────────
    //  PUBLIC API
    // ──────────────────────────────────────────────────────────

    public SentimentResult analyse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return buildResult("NEUTRAL", 0.5, "fallback");
        }
        text = text.trim();

        if (!apiKey.isEmpty()) {
            try {
                SentimentResult result = callHuggingFace(text);
                if (result != null) return result;
            } catch (Exception e) {
                System.err.println("[SentimentService] API indisponible, fallback activé : " + e.getMessage());
            }
        }

        return keywordFallback(text);
    }

    // ──────────────────────────────────────────────────────────
    //  PRIVATE — HuggingFace API (parsing avec org.json)
    // ──────────────────────────────────────────────────────────

    private SentimentResult callHuggingFace(String text) throws Exception {
        String payload = text.length() > 512 ? text.substring(0, 512) : text;
        String json = "{\"inputs\":\"" + escapeJson(payload) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body().trim();

        System.out.println("[SentimentService] HTTP " + status + " | body: " + body);

        if (status == 503) {
            // Modèle en cours de chargement sur HuggingFace — fallback
            return null;
        }
        if (status != 200) {
            throw new RuntimeException("[SentimentService] HTTP " + status + " — " + body);
        }

        // ── Parsing avec org.json ─────────────────────────────
        // Format attendu : [[{"label":"positive","score":0.9}, ...]]
        // ou parfois :     [{"label":"positive","score":0.9}, ...]
        String bestLabel = "NEUTRAL";
        double bestScore = 0.0;

        try {
            JSONArray outer = new JSONArray(body);

            // Déterminer si c'est [[...]] ou [...]
            JSONArray items;
            if (outer.length() > 0 && outer.get(0) instanceof JSONArray) {
                items = outer.getJSONArray(0);
            } else {
                items = outer;
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject obj   = items.getJSONObject(i);
                String     label = obj.getString("label");
                double     score = obj.getDouble("score");

                System.out.println("[SentimentService] candidate: " + label + " = " + score);

                if (score > bestScore) {
                    bestScore = score;
                    bestLabel = label;
                }
            }
        } catch (Exception e) {
            System.err.println("[SentimentService] Parsing JSON échoué : " + e.getMessage()
                    + " | body: " + body);
            return null; // déclenchera le fallback
        }

        // Normaliser le label (RoBERTa utilise "positive"/"negative"/"neutral"
        // ou "LABEL_0"/"LABEL_1"/"LABEL_2")
        String normalised = switch (bestLabel.toLowerCase()) {
            case "positive", "pos", "label_2" -> "POSITIVE";
            case "negative", "neg", "label_0" -> "NEGATIVE";
            default                           -> "NEUTRAL";
        };

        System.out.println("[SentimentService] résultat final : " + normalised
                + " (" + String.format("%.3f", bestScore) + ")");

        return buildResult(normalised, bestScore, "api");
    }

    // ──────────────────────────────────────────────────────────
    //  PRIVATE — Fallback mots-clés
    // ──────────────────────────────────────────────────────────

    private SentimentResult keywordFallback(String text) {
        String lower = text.toLowerCase();

        List<String> negativeWords = Arrays.asList(
                "angry", "furious", "outraged", "disgusted", "hate", "terrible", "horrible",
                "awful", "pathetic", "ridiculous", "unacceptable", "disgraceful", "incompetent",
                "useless", "broken", "scam", "fraud", "steal", "stole", "cheating", "cheat",
                "lied", "lie", "lies", "ripped off", "never works", "doesn't work", "not working",
                "failed", "failure", "worst", "bad", "poor", "urgent", "immediately", "asap",
                "lawsuit", "sue", "legal", "report", "banned", "unfair", "stolen", "lost",
                "frustrated", "disappointed", "upset", "annoyed", "can't believe",
                "wtf", "wth", "damn", "hell", "crap",
                // Français (bonus)
                "nul", "horrible", "arnaque", "volé", "escroquerie", "bug", "cassé",
                "inutile", "énervé", "déçu", "problème", "erreur", "impossible"
        );

        List<String> positiveWords = Arrays.asList(
                "thank", "thanks", "please", "appreciate", "help", "kind", "great", "good",
                "excellent", "wonderful", "fantastic", "amazing", "love", "happy", "satisfied",
                "resolved", "fixed", "working", "perfect", "awesome", "nice",
                // Français
                "merci", "super", "parfait", "excellent", "génial", "résolu"
        );

        int negScore = 0, posScore = 0;

        for (String word : negativeWords)
            if (lower.contains(word)) negScore++;
        for (String word : positiveWords)
            if (lower.contains(word)) posScore++;

        // Points d'exclamation → amplifient la négativité
        negScore += (int) (text.chars().filter(c -> c == '!').count() / 2);

        // Mots en MAJUSCULES (cris)
        int capsCount = 0;
        for (String word : text.split("\\s+"))
            if (word.length() >= 3 && word.equals(word.toUpperCase()) && word.matches("[A-Z]+"))
                capsCount++;
        negScore += capsCount;

        System.out.println("[SentimentService] fallback — negScore=" + negScore
                + " posScore=" + posScore + " caps=" + capsCount);

        if (negScore > posScore) {
            double confidence = Math.min(0.5 + negScore * 0.08, 0.95);
            return buildResult("NEGATIVE", confidence, "fallback");
        }
        if (posScore > negScore) {
            double confidence = Math.min(0.5 + posScore * 0.06, 0.90);
            return buildResult("POSITIVE", confidence, "fallback");
        }
        return buildResult("NEUTRAL", 0.55, "fallback");
    }

    // ──────────────────────────────────────────────────────────
    //  PRIVATE — Construction résultat normalisé
    // ──────────────────────────────────────────────────────────

    private SentimentResult buildResult(String label, double score, String source) {
        String emoji      = switch (label) { case "NEGATIVE" -> "😡"; case "POSITIVE" -> "😊"; default -> "😐"; };
        String badgeClass = switch (label) { case "NEGATIVE" -> "danger"; case "POSITIVE" -> "success"; default -> "warning"; };
        String textLabel  = switch (label) { case "NEGATIVE" -> "Negative"; case "POSITIVE" -> "Positive"; default -> "Neutral"; };
        String color      = switch (label) { case "NEGATIVE" -> "#ff6b7a"; case "POSITIVE" -> "#43e97b"; default -> "#ffd54f"; };

        String prioritySuggestion = null;
        if ("NEGATIVE".equals(label)) {
            if (score >= URGENT_THRESHOLD)    prioritySuggestion = "URGENT";
            else if (score >= HIGH_THRESHOLD) prioritySuggestion = "HIGH";
        }

        return new SentimentResult(label, textLabel, score, emoji, badgeClass, color,
                prioritySuggestion, source);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ──────────────────────────────────────────────────────────
    //  INNER CLASS — Résultat
    // ──────────────────────────────────────────────────────────

    public static class SentimentResult {
        public final String label;
        public final String textLabel;
        public final double score;
        public final int    scorePercent;
        public final String emoji;
        public final String badgeClass;
        public final String color;
        public final String prioritySuggestion;
        public final String source;

        public SentimentResult(String label, String textLabel, double score,
                               String emoji, String badgeClass, String color,
                               String prioritySuggestion, String source) {
            this.label              = label;
            this.textLabel          = textLabel;
            this.score              = Math.round(score * 1000.0) / 1000.0;
            this.scorePercent       = (int) Math.round(score * 100);
            this.emoji              = emoji;
            this.badgeClass         = badgeClass;
            this.color              = color;
            this.prioritySuggestion = prioritySuggestion;
            this.source             = source;
        }

        public boolean isNegative() { return "NEGATIVE".equals(label); }
        public boolean isPositive() { return "POSITIVE".equals(label); }
        public boolean isNeutral()  { return "NEUTRAL".equals(label);  }
    }
}