package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SentimentAnalysisService {

    private final String     apiKey     = ConfigLoader.get("HF_API_KEY");
    private final String     apiUrl     = "https://api-inference.huggingface.co/models/"
            + "distilbert-base-uncased-finetuned-sst-2-english";
    private final HttpClient httpClient;

    public SentimentAnalysisService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<String> analyze(String content, int rating) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String ratingContext = switch (rating) {
                    case 4, 5 -> "I am very satisfied.";
                    case 3    -> "It was okay.";
                    default   -> "I am not satisfied.";
                };
                String text = (content + " " + ratingContext).trim();

                JSONObject payload = new JSONObject();
                payload.put("inputs", text);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONArray data = new JSONArray(response.body());
                    if (data.length() > 0 && data.get(0) instanceof JSONArray) {
                        JSONArray inner    = data.getJSONArray(0);
                        JSONObject best    = null;
                        for (int i = 0; i < inner.length(); i++) {
                            JSONObject item = inner.getJSONObject(i);
                            if (best == null ||
                                    item.getDouble("score") > best.getDouble("score")) {
                                best = item;
                            }
                        }
                        if (best != null) {
                            String label = best.getString("label").toLowerCase();
                            if (label.equals("positive"))
                                return rating <= 2 ? "neutral" : "positive";
                            if (label.equals("negative"))
                                return rating >= 4 ? "neutral" : "negative";
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fallbackFromRating(rating);
        });
    }

    private String fallbackFromRating(int rating) {
        if (rating >= 4) return "positive";
        if (rating <= 2) return "negative";
        return "neutral";
    }
}