package com.eyetwin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("EyeTwin E-Sport Platform");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setResizable(true);
        navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    public static void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(fxmlPath)
            );
            Scene scene = new Scene(loader.load(), 1280, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("EyeTwin — " + title);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("❌ Erreur : " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        try {
            // J'ai commenté ces lignes pour empêcher les tests CRUD console 
            // de s'exécuter à chaque démarrage, permettant ainsi à l'interface GUI de se lancer.
            /*
            System.out.println("====== TEST CRUD TOURNOI & MATCHES (Console) ======");
            com.eyetwin.interfaces.ITournoiService tournoiService = new com.eyetwin.services.TournoiServiceImpl();
            com.eyetwin.interfaces.IMatchService matchService = new com.eyetwin.services.MatchServiceImpl();

            // 1. Ajouter un Tournoi
            com.eyetwin.entities.Tournoi tournoi = new com.eyetwin.entities.Tournoi(
                    0, "Tournoi Test Console", new java.sql.Date(System.currentTimeMillis()), 
                    new java.sql.Date(System.currentTimeMillis() + 86400000L), "Test depuis MainApp", 
                    "test.img", "SOLO", 150.0
            );
            tournoiService.add(tournoi);
            
            // 2. Récupérer et Afficher les Tournois
            java.util.List<com.eyetwin.entities.Tournoi> tournois = tournoiService.getAll();
            System.out.println("Tournois en base :");
            for (com.eyetwin.entities.Tournoi t : tournois) {
                System.out.println(" - " + t);
            }

            // ... (suite des tests)
            */

            System.out.println("🚀 Lancement de l'application EyeTwin...");
            launch(args); 
            
        } catch (Exception e) {
            System.err.println("❌ Erreur fatale lors du démarrage : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}