package com.eyetwin.controller;

import com.eyetwin.interfaces.IStatsService;
import com.eyetwin.services.StatsServiceImpl;
import com.eyetwin.tools.SessionManager;
import com.eyetwin.entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class HomeController {

    // ── Navbar (injectée via fx:include) ──
    @FXML private NavbarController navbarController;

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

    // ── CTA bottom (guest) ──
    @FXML private VBox ctaBottomGuest;

    private final IStatsService statsService = new StatsServiceImpl();

    // ════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════
    @FXML
    public void initialize() {
        navbarController.setActivePage("home");
        User user = SessionManager.getCurrentUser();
        if (user != null) setupLoggedIn(user);
        else              setupGuest();
        new Thread(this::loadStats).start();
    }

    // ════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════
    private void setupLoggedIn(User user) {
        show(heroCTAUser);
        hide(heroCTAGuest);
        hide(ctaBottomGuest);

        if (eventRegisterBtn != null)
            eventRegisterBtn.setOnAction(e -> navigateTo("Tournois.fxml"));
    }

    private void setupGuest() {
        show(heroCTAGuest);
        hide(heroCTAUser);
        show(ctaBottomGuest);
    }

    // ════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════
    private void loadStats() {
        try {
            int players     = statsService.countPlayers();
            int tournaments = statsService.countTournaments();
            int teams       = statsService.countTeams();
            int coaches     = statsService.countCoaches();

            Platform.runLater(() -> {
                animateCounter(statPlayers,     players,     1400);
                animateCounter(statTournaments, tournaments, 1400);
                animateCounter(statTeams,       teams,       1400);
                animateCounter(statCoaches,     coaches,     1400);
                if (eventTeams != null)       eventTeams.setText(String.valueOf(teams));
                if (eventTournaments != null) eventTournaments.setText(String.valueOf(tournaments));
            });
        } catch (Exception e) {
            System.err.println("[HomeController] Stats load error: " + e.getMessage());
        }
    }

    private void animateCounter(Label label, long target, int durationMs) {
        if (label == null || target <= 0) {
            if (label != null) label.setText(String.valueOf(Math.max(0, target)));
            return;
        }
        final int  steps = 50;
        final long delay = Math.max(1, durationMs / steps);
        final long step  = Math.max(1, target / steps);
        Timeline tl = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final long val = Math.min(step * i, target);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * delay),
                    e -> label.setText(String.valueOf(val))));
        }
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(steps * delay + 60),
                e -> label.setText(String.valueOf(target))));
        tl.play();
    }

    // ════════════════════════════════════════════
    //  NAVIGATION (buttons dans home.fxml uniquement)
    // ════════════════════════════════════════════
    @FXML public void goToRegister() { navigateTo("register.fxml"); }
    @FXML public void goToLogin()    { navigateTo("login.fxml"); }
    @FXML public void goToVideos()   { navigateTo("Videos.fxml"); }
    @FXML public void goToPlanning() { navigateTo("Planning.fxml"); }
    @FXML public void goToTeams()    { navigateTo("Team.fxml"); }

    private void navigateTo(String fxml) {
        // Délègue à la navbar qui a déjà la logique complète
        navbarController.navigateTo(fxml);  // rendre public dans NavbarController
    }

    // ════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════
    private void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true);  }
    }
    private void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}