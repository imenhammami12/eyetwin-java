package com.eyetwin.config;

import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConfig.getConnection();
            System.out.println("✅ Connexion réussie !");
            System.out.println("DB : " + conn.getCatalog());
        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }
}