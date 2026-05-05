package com.eyetwin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class GiphyConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = GiphyConfig.class.getClassLoader().getResourceAsStream("giphy.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load giphy.properties: " + e.getMessage());
        }
    }

    private GiphyConfig() {
    }

    private static String getRequired(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    public static String getApiKey() {
        return getRequired("giphy.api.key");
    }

    public static String getRating() {
        return PROPERTIES.getProperty("giphy.api.rating", "g").trim();
    }

    public static String getLang() {
        return PROPERTIES.getProperty("giphy.api.lang", "en").trim();
    }
}