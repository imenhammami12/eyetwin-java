package com.eyetwin.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = ConfigLoader.class
                .getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
            else System.err.println("[ConfigLoader] config.properties introuvable (fallback env/system props only).");
        } catch (Exception e) {
            System.err.println("[ConfigLoader] Erreur chargement : " + e.getMessage());
        }
    }

    public static String get(String key) {
        if (key == null || key.isBlank()) return "";

        String fromFile = props.getProperty(key);
        if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();

        String fromSysProp = System.getProperty(key);
        if (fromSysProp != null && !fromSysProp.isBlank()) return fromSysProp.trim();

        String fromEnvExact = System.getenv(key);
        if (fromEnvExact != null && !fromEnvExact.isBlank()) return fromEnvExact.trim();

        String envKeyNormalized = key.toUpperCase().replace('.', '_');
        String fromEnvNormalized = System.getenv(envKeyNormalized);
        if (fromEnvNormalized != null && !fromEnvNormalized.isBlank()) return fromEnvNormalized.trim();

        return "";
    }
}