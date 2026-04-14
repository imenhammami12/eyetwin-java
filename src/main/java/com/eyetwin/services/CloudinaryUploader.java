package com.eyetwin.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Local fallback uploader used by guide upload flow.
 * It mimics Cloudinary response shape so existing controller logic keeps working.
 */
public class CloudinaryUploader {

    public Map<String, Object> uploadVideo(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Video file not found.");
        }

        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "guide-videos");
        Files.createDirectories(uploadDir);

        String originalName = sourceFile.getName();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = System.currentTimeMillis() + "_" + safeName;

        Path destination = uploadDir.resolve(fileName);
        Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> result = new HashMap<>();
        result.put("secure_url", "/uploads/guide-videos/" + fileName);
        result.put("public_id", "guide-videos/" + fileName);
        result.put("resource_type", "video");
        return result;
    }
}
