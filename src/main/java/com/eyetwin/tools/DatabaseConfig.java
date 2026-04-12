package com.eyetwin.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/eyetwin_platform"
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String LOGIN = "root";
    private static final String PWD   = "";

    private static DatabaseConfig instance;  // private ✅

    private Connection cnx;

    private DatabaseConfig() {
        try {
            cnx = DriverManager.getConnection(URL, LOGIN, PWD);
            System.out.println("[DatabaseConfig] Connexion établie ✅");
        } catch (SQLException e) {
            System.out.println("[DatabaseConfig] Erreur : " + e.getMessage());
        }
    }

    // ── Singleton ────────────────────────────────────────────────
    public static DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    // ── Connexion avec reconnexion automatique ───────────────────
    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                System.out.println("[DatabaseConfig] Reconnexion...");
                cnx = DriverManager.getConnection(URL, LOGIN, PWD);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConfig] Reconnexion échouée : " + e.getMessage());
        }
        return cnx;
    }

    // ── Alias statique — pour les classes qui appellent DatabaseConfig.getConnection() ──
    public static Connection getConnection() {
        return getInstance().getCnx();
    }
}