package com.eyetwin.services.Community;

import com.eyetwin.config.GiphyConfig;
import com.eyetwin.entities.Community.GiphyGif;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GiphyApiService {

    private static final String SEARCH_URL = "https://api.giphy.com/v1/gifs/search";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<GiphyGif> searchGifs(String query, int limit) throws Exception {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.isBlank()) {
            cleanQuery = "gaming";
        }

        String url = SEARCH_URL
                + "?api_key=" + URLEncoder.encode(GiphyConfig.getApiKey(), StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8)
                + "&limit=" + limit
                + "&rating=" + URLEncoder.encode(GiphyConfig.getRating(), StandardCharsets.UTF_8)
                + "&lang=" + URLEncoder.encode(GiphyConfig.getLang(), StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GIPHY search failed: HTTP " + response.statusCode() + "\n" + response.body());
        }

        JSONObject root = new JSONObject(response.body());
        JSONArray data = root.getJSONArray("data");

        List<GiphyGif> gifs = new ArrayList<>();

        for (int i = 0; i < data.length(); i++) {
            JSONObject gif = data.getJSONObject(i);
            JSONObject images = gif.getJSONObject("images");

            String id = gif.optString("id", "");
            String title = gif.optString("title", "GIF");

            String previewUrl = null;
            String sendUrl = null;

            if (images.has("fixed_height")) {
                previewUrl = images.getJSONObject("fixed_height").optString("url", "");
            }

            if (images.has("downsized_medium")) {
                sendUrl = images.getJSONObject("downsized_medium").optString("url", "");
            }

            if ((sendUrl == null || sendUrl.isBlank()) && images.has("original")) {
                sendUrl = images.getJSONObject("original").optString("url", "");
            }

            if ((previewUrl == null || previewUrl.isBlank()) && images.has("original")) {
                previewUrl = images.getJSONObject("original").optString("url", "");
            }

            if (previewUrl != null && !previewUrl.isBlank() && sendUrl != null && !sendUrl.isBlank()) {
                gifs.add(new GiphyGif(id, title, previewUrl, sendUrl));
            }
        }

        return gifs;
    }
}