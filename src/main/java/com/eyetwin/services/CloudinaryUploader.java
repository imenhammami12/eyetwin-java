package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Cloudinary-backed uploader used by the guide upload flow.
 */
public class CloudinaryUploader {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public Map<String, Object> uploadVideo(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Video file not found.");
        }

        CloudinaryConfig config = resolveCloudinaryConfig();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, String> signatureParams = new TreeMap<>();
        signatureParams.put("timestamp", timestamp);

        String signature = sign(signatureParams, config.apiSecret);
        String boundary = "----EyETwinFormBoundary" + UUID.randomUUID().toString().replace("-", "");

        byte[] body = MultipartBodyBuilder.builder(boundary)
                .field("file", sourceFile)
                .field("api_key", config.apiKey)
                .field("timestamp", timestamp)
                .field("signature", signature)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.uploadUrl()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Cloudinary upload failed (HTTP " + response.statusCode() + "): " + response.body());
            }

            Map<String, Object> payload = JsonLikeParser.parseObject(response.body());
            Map<String, Object> result = new HashMap<>();
            result.put("secure_url", payload.get("secure_url"));
            result.put("public_id", payload.get("public_id"));
            result.put("resource_type", payload.get("resource_type"));
            result.put("bytes", payload.get("bytes"));
            result.put("url", payload.get("url"));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Cloudinary upload interrupted.", e);
        }
    }

    private CloudinaryConfig resolveCloudinaryConfig() throws IOException {
        String cloudinaryUrl = firstNonBlank(
                ConfigLoader.get("CLOUDINARY_URL"),
                System.getenv("CLOUDINARY_URL"),
                System.getProperty("cloudinary.url")
        );

        if (cloudinaryUrl != null && cloudinaryUrl.startsWith("cloudinary://")) {
            return CloudinaryConfig.fromUrl(cloudinaryUrl);
        }

        String cloudName = firstNonBlank(ConfigLoader.get("CLOUDINARY_CLOUD_NAME"), System.getenv("CLOUDINARY_CLOUD_NAME"));
        String apiKey = firstNonBlank(ConfigLoader.get("CLOUDINARY_API_KEY"), System.getenv("CLOUDINARY_API_KEY"));
        String apiSecret = firstNonBlank(ConfigLoader.get("CLOUDINARY_API_SECRET"), System.getenv("CLOUDINARY_API_SECRET"));

        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IOException("Cloudinary is not configured. Set CLOUDINARY_URL or CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET.");
        }

        return new CloudinaryConfig(cloudName, apiKey, apiSecret);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sign(Map<String, String> params, String apiSecret) throws IOException {
        try {
            StringBuilder base = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (base.length() > 0) {
                    base.append('&');
                }
                base.append(entry.getKey()).append('=').append(entry.getValue());
            }
            base.append(apiSecret);

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(base.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IOException("Unable to sign Cloudinary request.", e);
        }
    }

    private record CloudinaryConfig(String cloudName, String apiKey, String apiSecret) {
        static CloudinaryConfig fromUrl(String cloudinaryUrl) throws IOException {
            try {
                String raw = cloudinaryUrl.substring("cloudinary://".length());
                int at = raw.lastIndexOf('@');
                int colon = raw.indexOf(':');
                if (colon <= 0 || at <= colon) {
                    throw new IOException("Invalid CLOUDINARY_URL format.");
                }
                String apiKey = raw.substring(0, colon);
                String apiSecret = raw.substring(colon + 1, at);
                String cloudName = raw.substring(at + 1);
                return new CloudinaryConfig(cloudName, apiKey, apiSecret);
            } catch (RuntimeException e) {
                throw new IOException("Invalid CLOUDINARY_URL format.", e);
            }
        }

        String uploadUrl() {
            return "https://api.cloudinary.com/v1_1/" + cloudName + "/video/upload";
        }
    }

    private static final class MultipartBodyBuilder {
        private final String boundary;
        private final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

        private MultipartBodyBuilder(String boundary) {
            this.boundary = boundary;
        }

        static MultipartBodyBuilder builder(String boundary) {
            return new MultipartBodyBuilder(boundary);
        }

        MultipartBodyBuilder field(String name, String value) throws IOException {
            if (value == null) {
                return this;
            }
            writeLine("--" + boundary);
            writeLine("Content-Disposition: form-data; name=\"" + name + "\"");
            writeLine("");
            writeLine(value);
            return this;
        }

        MultipartBodyBuilder field(String name, File file) throws IOException {
            writeLine("--" + boundary);
            writeLine("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName().replace("\"", "") + "\"");
            writeLine("Content-Type: application/octet-stream");
            writeLine("");
            try (InputStream in = Files.newInputStream(file.toPath())) {
                in.transferTo(output);
            }
            writeLine("");
            return this;
        }

        byte[] build() throws IOException {
            writeLine("--" + boundary + "--");
            return output.toByteArray();
        }

        private void writeLine(String value) throws IOException {
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class JsonLikeParser {
        static Map<String, Object> parseObject(String json) throws IOException {
            try {
                org.json.JSONObject object = new org.json.JSONObject(json);
                Map<String, Object> map = new HashMap<>();
                for (String key : object.keySet()) {
                    map.put(key, object.get(key));
                }
                return map;
            } catch (Exception e) {
                throw new IOException("Unable to parse Cloudinary response.", e);
            }
        }
    }
}
