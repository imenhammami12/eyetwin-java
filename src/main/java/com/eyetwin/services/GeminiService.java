package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GeminiService {
    private static final String API_KEY = ConfigLoader.get("gemini.api.key");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<String> generateTournamentDescription(String name, String type, double price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = String.format(
                        "Génère une description épique, engageante et professionnelle pour un tournoi de jeu vidéo avec les détails suivants :\n" +
                        "- Nom du tournoi : %s\n" +
                        "- Type/Catégorie : %s\n" +
                        "- Frais d'inscription (Prize Pool potentiel) : %.2f DT\n\n" +
                        "La description doit inclure une introduction accrocheuse, l'esprit de la compétition, et une conclusion motivante. Réponds uniquement avec le texte de la description en français.",
                        name, type, price
                );

                JSONObject payload = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", prompt));
                content.put("parts", parts);
                contents.put(content);
                payload.put("contents", contents);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    return jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();
                } else {
                    System.err.println("Gemini API Error: " + response.body());
                    return "Erreur lors de la génération de la description (Code: " + response.statusCode() + ").";
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Impossible de générer la description pour le moment.";
            }
        });
    }
}
