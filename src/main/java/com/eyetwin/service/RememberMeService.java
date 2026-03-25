package com.eyetwin.service;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Gère le "Remember Me" en sauvegardant l'email dans
 * ~/.eyetwin/remember.properties  (comme un cookie persistant)
 */
public class RememberMeService {

    private static final String DIR_NAME  = ".eyetwin";
    private static final String FILE_NAME = "remember.properties";
    private static final String KEY_EMAIL = "email";

    private static Path getFilePath() {
        return Paths.get(System.getProperty("user.home"), DIR_NAME, FILE_NAME);
    }

    /** Sauvegarde l'email si remember=true, supprime le fichier sinon */
    public static void save(String email, boolean remember) {
        if (remember) {
            try {
                Path dir = Paths.get(System.getProperty("user.home"), DIR_NAME);
                Files.createDirectories(dir);

                Properties props = new Properties();
                props.setProperty(KEY_EMAIL, email);

                try (OutputStream out = new FileOutputStream(getFilePath().toFile())) {
                    props.store(out, "EyeTwin Remember Me");
                }
                System.out.println("✅ Remember Me saved: " + email);
            } catch (IOException e) {
                System.err.println("❌ RememberMe save error: " + e.getMessage());
            }
        } else {
            clear();
        }
    }

    /** Retourne l'email sauvegardé ou null */
    public static String load() {
        Path path = getFilePath();
        if (!Files.exists(path)) return null;

        try (InputStream in = new FileInputStream(path.toFile())) {
            Properties props = new Properties();
            props.load(in);
            String email = props.getProperty(KEY_EMAIL);
            System.out.println("✅ Remember Me loaded: " + email);
            return email;
        } catch (IOException e) {
            System.err.println("❌ RememberMe load error: " + e.getMessage());
            return null;
        }
    }

    /** Supprime le fichier remember me (appelé au logout) */
    public static void clear() {
        try {
            Files.deleteIfExists(getFilePath());
            System.out.println("✅ Remember Me cleared");
        } catch (IOException e) {
            System.err.println("❌ RememberMe clear error: " + e.getMessage());
        }
    }

    public static boolean exists() {
        return Files.exists(getFilePath());
    }
}