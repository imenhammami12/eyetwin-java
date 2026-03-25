package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.dao.StatsDAO;
import com.eyetwin.model.User;
import com.eyetwin.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HomeController {

    // ── Navbar ──
    @FXML private HBox loggedInZone;   // coins + profile + logout (caché si visiteur)
    @FXML private HBox guestZone;      // sign in + get started (caché si connecté)
    @FXML private Label coinsNavLabel;
    @FXML private Button userButton;
    @FXML private Button logoutButton;

    // ── Hero CTA ──
    @FXML private HBox heroCTAGuest;   // "Get Started" + "Sign In"
    @FXML private HBox heroCTAUser;    // "Upload Video" + "Watch Live"

    // ── Event section ──
    @FXML private Button eventRegisterBtn;

    // ── CTA bottom ──
    @FXML private VBox ctaBottomGuest; // section "Ready to Dominate?" — visiteur uniquement

    // ── Stats ──
    @FXML private Label statPlayers;
    @FXML private Label statTournaments;
    @FXML private Label statTeams;
    @FXML private Label statCoaches;
    @FXML private Label eventTeams;
    @FXML private Label eventTournaments;

    private final StatsDAO statsDAO = new StatsDAO();

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();

        if (user != null) {
            // ── Connecté ──

            // Navbar : afficher zone connectée, masquer zone visiteur
            setVisible(loggedInZone, true);
            setVisible(guestZone,    false);

            // Nom du bouton profile : prénom > username > "PROFILE"
            String name = "PROFILE";
            if (user.getFirstName() != null && !user.getFirstName().isBlank())
                name = user.getFirstName().toUpperCase();
            else if (user.getUsername() != null && !user.getUsername().isBlank())
                name = user.getUsername().toUpperCase();
            if (userButton    != null) userButton.setText(name);
            if (coinsNavLabel != null) coinsNavLabel.setText(String.valueOf(user.getCoins()));

            // Hero : boutons connecté
            setVisible(heroCTAUser,  true);
            setVisible(heroCTAGuest, false);

            // CTA bottom : masqué si connecté (comme le Twig {% if not app.user %})
            setVisible(ctaBottomGuest, false);

            // Bouton Register Now → redirige vers tournois si connecté
            if (eventRegisterBtn != null)
                eventRegisterBtn.setOnAction(e -> goToTournois());

        } else {
            // ── Visiteur ──

            // Navbar
            setVisible(loggedInZone, false);
            setVisible(guestZone,    true);

            // Hero : boutons visiteur
            setVisible(heroCTAGuest, true);
            setVisible(heroCTAUser,  false);

            // CTA bottom visible
            setVisible(ctaBottomGuest, true);
        }

        loadStats();
    }

    /**
     * Helper : visible + managed en même temps
     * managed=false = l'élément ne prend plus de place dans le layout
     */
    private void setVisible(javafx.scene.Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ===== STATS =====
    private void loadStats() {
        int players     = statsDAO.countPlayers();
        int tournaments = statsDAO.countTournaments();
        int teams       = statsDAO.countTeams();
        int coaches     = statsDAO.countCoaches();

        animateTo(statPlayers,      players,     1200);
        animateTo(statTournaments,  tournaments, 1200);
        animateTo(statTeams,        teams,       1200);
        animateTo(statCoaches,      coaches,     1200);
        animateTo(eventTeams,       teams,       1200);
        animateTo(eventTournaments, tournaments, 1200);
    }

    private void animateTo(Label label, int target, int durationMs) {
        if (label == null) return;
        int steps = 40;
        Timeline tl = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final int val = (int) Math.round(target * (double) i / steps);
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis((double) durationMs * i / steps),
                    e -> label.setText(String.valueOf(val))
            ));
        }
        tl.play();
    }

    // ===== NAVIGATION =====
    @FXML public void goToProfile() {
        if (SessionManager.isLoggedIn())
            MainApp.navigateTo("/com/eyetwin/views/profile.fxml", "Profile");
        else
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    @FXML public void goToLogin() {
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    @FXML public void goToRegister() {
        MainApp.navigateTo("/com/eyetwin/views/register.fxml", "Register");
    }

    @FXML public void goToVideos() {
        if (SessionManager.isLoggedIn())
            MainApp.navigateTo("/com/eyetwin/views/videos.fxml", "Videos");
        else
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    @FXML public void goToPlanning() {
        if (SessionManager.isLoggedIn())
            MainApp.navigateTo("/com/eyetwin/views/planning.fxml", "Planning");
        else
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    public void goToTournois() {
        MainApp.navigateTo("/com/eyetwin/views/tournois.fxml", "Tournois");
    }

    /**
     * Logout — même logique que security.yaml : logout target: app_login
     */
    @FXML public void handleLogout() {
        SessionManager.logout();
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }
}