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

        // 3. Ajouter un Match lié au dernier tournoi
        if (!tournois.isEmpty()) {
            int lastTournoiId = tournois.get(tournois.size() - 1).getId();
            com.eyetwin.entities.Match match = new com.eyetwin.entities.Match(
                    0, "Equipe Alpha", "Equipe Beta", 0, 
                    new java.sql.Date(System.currentTimeMillis()), "100", 
                    lastTournoiId, "BO3", "Tunis"
            );
            matchService.add(match);
        }

        // 4. Récupérer et Afficher les Matchs
        System.out.println("\nMatchs en base :");
        for(com.eyetwin.entities.Match m : matchService.getAll()) {
            System.out.println(" - " + m);
        }
        // 5. Tester l'UPDATE et le DELETE
        System.out.println("\n-> Test UPDATE et DELETE");
        java.util.List<com.eyetwin.entities.Match> allMatchs = matchService.getAll();
        if (!allMatchs.isEmpty()) {
            com.eyetwin.entities.Match lastMatch = allMatchs.get(allMatchs.size() - 1);
            
            // UPDATE Match
            lastMatch.setScore(999);
            lastMatch.setLocalisation("Stade Modifié");
            matchService.update(lastMatch);
            System.out.println("Match après Update (getById) : " + matchService.getById(lastMatch.getId()));
            
            // DELETE Match
            matchService.delete(lastMatch.getId());
            System.out.println("Match ID " + lastMatch.getId() + " supprimé.");
        }

        if (!tournois.isEmpty()) {
            com.eyetwin.entities.Tournoi lastTournoi = tournois.get(tournois.size() - 1);
            
            // UPDATE Tournoi
            lastTournoi.setNom("Tournoi RENOMMÉ");
            tournoiService.update(lastTournoi);
            System.out.println("Tournoi après Update (getById) : " + tournoiService.getById(lastTournoi.getId()));
            
            // DELETE Tournoi
            tournoiService.delete(lastTournoi.getId());
            System.out.println("Tournoi ID " + lastTournoi.getId() + " supprimé.");
        }
        
        System.out.println("====== FIN DU TEST ======");
        // J'ai commenté ces deux lignes pour empêcher l'interface JavaFX (XML) 
        // de s'ouvrir, afin que vous puissiez juste voir les résultats du CRUD
        // dans le terminal !
        // System.out.println("Lancement de l'application...");
        // launch(args); 
        
        System.exit(0); // Quitte proprement après le test console
    }
}