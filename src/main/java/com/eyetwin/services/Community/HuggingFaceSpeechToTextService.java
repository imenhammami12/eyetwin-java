package com.eyetwin.services.Community;

import com.eyetwin.config.HfSpeechConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class HuggingFaceSpeechToTextService {

    private static final String BASE_URL = "https://router.huggingface.co/hf-inference/models/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String transcribe(File wavFile) throws Exception {
        if (wavFile == null || !wavFile.exists()) {
            throw new IllegalArgumentException("Audio file not found.");
        }

        String url = BASE_URL + HfSpeechConfig.getAsrModel();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + HfSpeechConfig.getToken())
                .header("Content-Type", "audio/wav")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(wavFile.toPath())))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HF STT failed: HTTP " + response.statusCode() + "\n" + response.body());
        }

        String body = response.body() == null ? "" : response.body().trim();
        if (body.isBlank()) {
            throw new IllegalStateException("Empty response from Hugging Face STT.");
        }

        String text = extractText(body);
        if (text.isBlank()) {
            throw new IllegalStateException("No speech could be recognized.");
        }

        return text;
    }

    private String extractText(String body) {
        if (body.startsWith("{")) {
            JSONObject json = new JSONObject(body);
            return json.optString("text", "").trim();
        }

        if (body.startsWith("[")) {
            JSONArray array = new JSONArray(body);
            if (!array.isEmpty() && array.get(0) instanceof JSONObject first) {
                return first.optString("text", "").trim();
            }
        }

        return body;
    }
}