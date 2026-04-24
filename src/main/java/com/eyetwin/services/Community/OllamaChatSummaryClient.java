package com.eyetwin.services.Community;

import com.eyetwin.config.AISummaryConfig;
import com.eyetwin.entities.Community.ChatSummaryResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaChatSummaryClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public ChatSummaryResult summarizeMissedTranscript(String channelName, int missedCount, String transcript)
            throws IOException, InterruptedException {

        String systemPrompt = """
                You summarize missed messages from one gaming community channel.

                Rules:
                - Summarize ONLY what appears in the transcript.
                - Be factual, concise, and useful.
                - Mention the main topic, decisions, fixes, requests, blockers, and next steps.
                - If attachments matter, mention them.
                - If the discussion is mostly casual/noise, say that clearly.
                - Do not invent anything.
                - Return valid JSON that matches the required schema exactly.
                """;

        String userPrompt = """
                Channel: %s
                Missed message count: %d

                Transcript:
                %s
                """.formatted(channelName, missedCount, transcript);

        return requestStructuredSummary(systemPrompt, userPrompt);
    }

    public ChatSummaryResult summarizeChunkSummaries(String channelName, int missedCount, String chunkDigest)
            throws IOException, InterruptedException {

        String systemPrompt = """
                You are combining partial summaries from the same gaming community channel into one final summary.

                Rules:
                - Merge repeated points.
                - Keep only the most important information.
                - Mention the main topic, decisions, fixes, requests, blockers, and next steps.
                - Do not invent anything.
                - Return valid JSON that matches the required schema exactly.
                """;

        String userPrompt = """
                Channel: %s
                Total missed message count: %d

                Chunk summaries:
                %s
                """.formatted(channelName, missedCount, chunkDigest);

        return requestStructuredSummary(systemPrompt, userPrompt);
    }

    private ChatSummaryResult requestStructuredSummary(String systemPrompt, String userPrompt)
            throws IOException, InterruptedException {

        JSONObject body = new JSONObject();
        body.put("model", AISummaryConfig.getModel());
        body.put("stream", false);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userPrompt));
        body.put("messages", messages);

        body.put("format", buildSchema());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AISummaryConfig.getBaseUrl() + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Ollama summary request failed: HTTP "
                        + response.statusCode() + " | " + response.body());
            }

            JSONObject root = new JSONObject(response.body());
            JSONObject message = root.optJSONObject("message");
            if (message == null) {
                throw new IOException("Ollama returned no message content.");
            }

            String content = message.optString("content", "").trim();
            if (content.isBlank()) {
                throw new IOException("Ollama returned empty summary content.");
            }

            if (content.startsWith("```")) {
                content = content.replace("```json", "").replace("```", "").trim();
            }

            JSONObject summaryJson = new JSONObject(content);
            return ChatSummaryResult.fromJsonObject(summaryJson);

        } catch (ConnectException e) {
            throw new IOException("Cannot connect to Ollama. Make sure Ollama is installed, open, and running at "
                    + AISummaryConfig.getBaseUrl(), e);
        }
    }

    private JSONObject buildSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        JSONObject properties = new JSONObject();

        properties.put("title", new JSONObject().put("type", "string"));
        properties.put("overview", new JSONObject().put("type", "string"));

        properties.put("key_points", new JSONObject()
                .put("type", "array")
                .put("items", new JSONObject().put("type", "string")));

        properties.put("action_items", new JSONObject()
                .put("type", "array")
                .put("items", new JSONObject().put("type", "string")));

        properties.put("open_questions", new JSONObject()
                .put("type", "array")
                .put("items", new JSONObject().put("type", "string")));

        schema.put("properties", properties);

        schema.put("required", new JSONArray()
                .put("title")
                .put("overview")
                .put("key_points")
                .put("action_items")
                .put("open_questions"));

        return schema;
    }
}