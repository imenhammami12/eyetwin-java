package com.eyetwin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PiperTtsConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = PiperTtsConfig.class.getClassLoader().getResourceAsStream("speech.properties")) {
            if (input == null) {
                throw new IllegalStateException("speech.properties not found in resources");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load speech.properties: " + e.getMessage());
        }
    }

    private PiperTtsConfig() {
    }

    private static String getRequired(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    public static String getCommand() {
        return getRequired("speech.piper.command");
    }

    public static String getEnglishModelPath() {
        return getRequired("speech.piper.en.model.path");
    }

    public static String getEnglishConfigPath() {
        return getRequired("speech.piper.en.config.path");
    }

    public static String getFrenchModelPath() {
        return getRequired("speech.piper.fr.model.path");
    }

    public static String getFrenchConfigPath() {
        return getRequired("speech.piper.fr.config.path");
    }

    public static String getArabicModelPath() {
        return getRequired("speech.piper.ar.model.path");
    }

    public static String getArabicConfigPath() {
        return getRequired("speech.piper.ar.config.path");
    }
}