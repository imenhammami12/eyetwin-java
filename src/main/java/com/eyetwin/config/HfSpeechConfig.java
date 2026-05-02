package com.eyetwin.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class HfSpeechConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = HfSpeechConfig.class.getClassLoader().getResourceAsStream("speech.properties")) {
            if (input == null) {
                throw new IllegalStateException("speech.properties not found in resources");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load speech.properties: " + e.getMessage());
        }
    }

    private HfSpeechConfig() {
    }

    private static String getRequired(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    public static String getToken() {
        return getRequired("speech.hf.token");
    }

    public static String getAsrModel() {
        return getRequired("speech.hf.asr.model");
    }

    public static String getTtsModel() {
        return getRequired("speech.hf.tts.model");
    }
}