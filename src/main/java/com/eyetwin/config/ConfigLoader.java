package com.eyetwin.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = ConfigLoader.class
                .getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
            else System.err.println("[ConfigLoader] config.properties introuvable !");
        } catch (Exception e) {
            System.err.println("[ConfigLoader] Erreur chargement : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}