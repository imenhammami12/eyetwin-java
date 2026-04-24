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
            System.err.println("[DatabaseConfig] ❌ Échec de la connexion à MySQL.");
            System.err.println("Détails : " + e.getMessage());
            System.err.println("Assurez-vous que MySQL tourne sur le port 3306 et que la base 'eyetwin_platform' existe.");
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
                System.out.println("[DatabaseConfig] tentative de (re)connexion...");
                cnx = DriverManager.getConnection(URL, LOGIN, PWD);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConfig] ❌ Impossible de récupérer la connexion : " + e.getMessage());
            return null; // On force le retour null pour que l'appelant sache que ça a échoué
        }
        return cnx;
    }

    // ── Alias statique — pour les classes qui appellent DatabaseConfig.getConnection() ──
    public static Connection getConnection() {
        return getInstance().getCnx();
    }
}