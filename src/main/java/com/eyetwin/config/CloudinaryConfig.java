package com.eyetwin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class CloudinaryConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = CloudinaryConfig.class.getClassLoader()
                .getResourceAsStream("cloudinary.properties")) {

            if (input == null) {
                throw new IllegalStateException("cloudinary.properties not found");
            }

            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load cloudinary.properties: " + e.getMessage());
        }
    }

    private CloudinaryConfig() {
    }

    public static String getCloudName() {
        return getRequired("cloudinary.cloud_name");
    }

    public static String getUploadPreset() {
        return getRequired("cloudinary.upload_preset");
    }

    private static String getRequired(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }
}