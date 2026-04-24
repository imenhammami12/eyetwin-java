package com.eyetwin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AISummaryConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AISummaryConfig.class.getClassLoader()
                .getResourceAsStream("ai-summary.properties")) {

            if (input == null) {
                throw new IllegalStateException("ai-summary.properties not found in resources");
            }

            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load ai-summary.properties: " + e.getMessage());
        }
    }

    private AISummaryConfig() {
    }

    public static String getProvider() {
        return getRequired("ai.provider");
    }

    public static String getBaseUrl() {
        return getRequired("ai.summary.base_url");
    }

    public static String getModel() {
        return getRequired("ai.summary.model");
    }

    public static int getThreshold() {
        return getInt("ai.summary.threshold", 8);
    }

    public static int getChunkMaxMessages() {
        return getInt("ai.summary.chunk.maxMessages", 35);
    }

    public static int getChunkMaxChars() {
        return getInt("ai.summary.chunk.maxChars", 12000);
    }

    public static boolean isCacheEnabled() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("ai.summary.cache.enabled", "true"));
    }

    private static String getRequired(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    private static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(PROPERTIES.getProperty(key, String.valueOf(fallback)).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}