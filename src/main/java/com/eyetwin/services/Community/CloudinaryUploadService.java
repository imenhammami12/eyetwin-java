package com.eyetwin.services.Community;

import com.eyetwin.config.CloudinaryConfig;
import com.eyetwin.entities.Community.MessageAttachment;
import com.eyetwin.tools.CommunityFileValidator;
import com.eyetwin.tools.MultipartBodyPublisher;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

public class CloudinaryUploadService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public MessageAttachment upload(File file) throws IOException, InterruptedException {
        CommunityFileValidator.validate(file);

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        String boundary = "Boundary-" + UUID.randomUUID();

        HttpRequest.BodyPublisher bodyPublisher =
                MultipartBodyPublisher.build(file, CloudinaryConfig.getUploadPreset(), mimeType, boundary);

        String endpoint = "https://api.cloudinary.com/v1_1/"
                + CloudinaryConfig.getCloudName()
                + "/auto/upload";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Cloudinary upload failed: HTTP "
                    + response.statusCode() + " | " + response.body());
        }

        JSONObject json = new JSONObject(response.body());

        MessageAttachment attachment = new MessageAttachment();
        attachment.setPublicId(json.optString("public_id", null));
        attachment.setUrl(json.optString("secure_url", null));
        attachment.setResourceType(json.optString("resource_type", null));
        attachment.setFormat(json.optString("format", null));
        attachment.setOriginalName(file.getName());
        attachment.setMimeType(mimeType);
        attachment.setBytes(json.optLong("bytes", file.length()));

        return attachment;
    }
}