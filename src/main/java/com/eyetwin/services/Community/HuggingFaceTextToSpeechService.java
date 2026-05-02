package com.eyetwin.services.Community;

import com.eyetwin.config.HfSpeechConfig;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class HuggingFaceTextToSpeechService {

    private static final String BASE_URL = "https://router.huggingface.co/hf-inference/models/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public File synthesizeToFile(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text is empty.");
        }

        String url = BASE_URL + HfSpeechConfig.getTtsModel();

        JSONObject payload = new JSONObject();
        payload.put("inputs", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + HfSpeechConfig.getToken())
                .header("Content-Type", "application/json")
                .header("Accept", "audio/wav")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = new String(response.body());
            throw new IllegalStateException("HF TTS failed: HTTP " + response.statusCode() + "\n" + error);
        }

        String contentType = response.headers()
                .firstValue("content-type")
                .orElse("audio/wav")
                .toLowerCase();

        String extension = resolveExtension(contentType);

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "eyetwin-audio");
        Files.createDirectories(tempDir);

        File outputFile = tempDir.resolve("tts-" + System.currentTimeMillis() + extension).toFile();
        Files.write(outputFile.toPath(), response.body());

        return outputFile;
    }

    private String resolveExtension(String contentType) {
        if (contentType.contains("mpeg") || contentType.contains("mp3")) return ".mp3";
        if (contentType.contains("ogg")) return ".ogg";
        if (contentType.contains("wav")) return ".wav";
        return ".wav";
    }
}