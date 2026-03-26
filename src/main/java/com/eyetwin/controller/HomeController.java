package com.eyetwin.controller;

import com.eyetwin.util.SessionManager;
import com.eyetwin.model.User;
import com.eyetwin.service.RememberMeService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.eyetwin.dao.StatsDAO;

import java.io.IOException;

/**
 * HomeController — page d'accueil EyeTwin.
 * Adapté pour le nouveau Home.fxml avec avatar initials + profile trigger.
 */
public class HomeController {

    // ── Navbar zones ──
    @FXML private HBox  loggedInZone;
    @FXML private HBox  guestZone;
    @FXML private Label coinsNavLabel;
    @FXML private Label userButton;       // username label in profile pill
    @FXML private Label avatarInitial;    // first letter of username in avatar circle

    // ── Hero CTA ──
    @FXML private HBox heroCTAGuest;
    @FXML private HBox heroCTAUser;

    // ── Stats bar ──
    @FXML private Label statPlayers;
    @FXML private Label statTournaments;
    @FXML private Label statTeams;
    @FXML private Label statCoaches;

    // ── Event section ──
    @FXML private Label  eventTeams;
    @FXML private Label  eventTournaments;
    @FXML private Button eventRegisterBtn;

    // ── CTA bottom ──
    @FXML private VBox ctaBottomGuest;

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();

        if (user != null) {
            // ── CONNECTÉ ──
            show(loggedInZone);
            hide(guestZone);

            // Coins badge
            if (coinsNavLabel != null)
                coinsNavLabel.setText(String.valueOf(user.getCoinBalance()));

            // Profile pill
            if (userButton != null)
                userButton.setText(user.getUsername().toUpperCase());

            // Avatar initial (first letter)
            if (avatarInitial != null && user.getUsername() != null && !user.getUsername().isEmpty())
                avatarInitial.setText(String.valueOf(user.getUsername().charAt(0)).toUpperCase());

            // CTA buttons
            hide(heroCTAGuest);
            show(heroCTAUser);

            // Bottom CTA guest section
            hide(ctaBottomGuest);

            // Event button → tournois page
            if (eventRegisterBtn != null)
                eventRegisterBtn.setOnAction(e -> goToTournois());

        } else {
            // ── VISITEUR ──
            hide(loggedInZone);
            show(guestZone);

            // Make sure CTA bottom is visible
            show(ctaBottomGuest);
        }

        // Load stats from DB in background thread
        new Thread(this::loadStats).start();
    }

    // ════════════════════════════════════════════════════════════
    //  STATS — load from DB + animate counters
    // ════════════════════════════════════════════════════════════
    private void loadStats() {
        try {
            StatsDAO statsDAO = new StatsDAO();
            long players     = statsDAO.countPlayers();
            long tournaments = statsDAO.countTournaments();
            long teams       = statsDAO.countTeams();
            long coaches     = statsDAO.countCoaches();

            Platform.runLater(() -> {
                animateCounter(statPlayers,     players,     1400);
                animateCounter(statTournaments, tournaments, 1400);
                animateCounter(statTeams,       teams,       1400);
                animateCounter(statCoaches,     coaches,     1400);

                // Event section badges
                if (eventTeams != null)       eventTeams.setText(String.valueOf(teams));
                if (eventTournaments != null) eventTournaments.setText(String.valueOf(tournaments));
            });
        } catch (Exception e) {
            System.err.println("[HomeController] Stats load error: " + e.getMessage());
        }
    }

    /**
     * Animates a Label counter from 0 to target over durationMs milliseconds.
     * Mirrors the Symfony JS animateCounter() function.
     */
    private void animateCounter(Label label, long target, int durationMs) {
        if (label == null || target <= 0) {
            if (label != null) label.setText(String.valueOf(target));
            return;
        }

        final int steps  = 50;
        final long delay = durationMs / steps;
        final long step  = Math.max(1, target / steps);
        final long[] current = {0};

        Timeline tl = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final long val = Math.min(current[0] + step * i, target);
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * delay),
                    e -> label.setText(String.valueOf(val))
            ));
        }
        // Ensure final value is exact
        tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(steps * delay + 60),
                e -> label.setText(String.valueOf(target))
        ));
        tl.play();
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════
    @FXML private void goToLogin()    { navigateTo("Login.fxml"); }
    @FXML private void goToRegister() { navigateTo("Register.fxml"); }
    @FXML private void goToVideos()   { navigateTo("Videos.fxml"); }
    @FXML private void goToPlanning() { navigateTo("Planning.fxml"); }
    @FXML private void goToTournois() { navigateTo("Tournois.fxml"); }
    @FXML private void goToProfile()  { navigateTo("Profile.fxml"); }
    @FXML private void goToTeams()    { navigateTo("Team.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        navigateTo("Login.fxml");
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════
    private void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true); n.setManaged(true); }
    }
    private void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }

    private void navigateTo(String fxml) {
        try {
            // Try both /view/ and /views/ paths for flexibility
            var url = getClass().getResource("/com/eyetwin/view/" + fxml);
            if (url == null)
                url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) {
                System.err.println("[HomeController] ❌ FXML not found: " + fxml);
                return;
            }

            Parent root = FXMLLoader.load(url);

            // Resolve stage from any available node
            Stage stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));

        } catch (IOException e) {
            System.err.println("[HomeController] ❌ Navigation → " + fxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage resolveStage() {
        // Try each node that might be attached to the scene graph
        for (javafx.scene.Node n : new javafx.scene.Node[]{loggedInZone, guestZone, heroCTAGuest, heroCTAUser}) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}