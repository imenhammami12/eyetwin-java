package com.eyetwin.controller;

import com.eyetwin.util.SessionManager;
import com.eyetwin.model.User;
import com.eyetwin.dao.StatsDAO;
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

import java.io.IOException;
import java.net.URL;

public class HomeController {

    // ── Navbar — zones ──
    @FXML private HBox loggedInZone;
    @FXML private HBox guestZone;

    // ── Navbar — coins pill ──
    @FXML private HBox  coinsBadge;
    @FXML private Label coinsNavLabel;

    // ── Navbar — notifications MenuButton ──
    @FXML private MenuButton navNotifMenu;
    @FXML private Label      navNotifBadge;
    @FXML private MenuItem   notifHeaderItem;
    @FXML private MenuItem   notifEmptyItem;

    // ── Navbar — profile MenuButton ──
    @FXML private MenuButton navProfileMenu;
    @FXML private Label      navAvatarInitial;
    @FXML private Label      navUsername;
    @FXML private MenuItem   profileHeaderItem;
    @FXML private MenuItem   profileStatsItem;
    @FXML private MenuItem   profileAdminItem;
    @FXML private SeparatorMenuItem profileAdminSep;

    // ── Navbar — uploader MenuButton ──
    @FXML private MenuButton navUploaderMenu;
    @FXML private Label      navHighlights;

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

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) setupLoggedIn(user);
        else              setupGuest();
        new Thread(this::loadStats).start();
    }

    // ════════════════════════════════════════════════════════════
    //  SETUP — LOGGED IN
    // ════════════════════════════════════════════════════════════
    private void setupLoggedIn(User user) {
        show(loggedInZone);
        hide(guestZone);

        if (coinsNavLabel != null)
            coinsNavLabel.setText(String.valueOf(user.getCoinBalance()));

        String username = user.getUsername() != null ? user.getUsername() : "?";
        if (navAvatarInitial != null)
            navAvatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());
        if (navUsername != null)
            navUsername.setText(username.toUpperCase());

        if (profileHeaderItem != null)
            profileHeaderItem.setText("👤  " + username + "\n    " + user.getEmail());

        if (profileStatsItem != null)
            profileStatsItem.setText(
                    "🪙 " + user.getCoinBalance() + " coins   |   ⚡ Rank: —   |   🏆 Wins: —"
            );

        boolean isAdmin = user.getRolesJson() != null && user.getRolesJson().contains("ROLE_ADMIN");
        if (profileAdminItem != null) profileAdminItem.setVisible(isAdmin);
        if (profileAdminSep  != null) profileAdminSep.setVisible(isAdmin);

        updateNotifBadge(0);

        show(navUploaderMenu);
        show(navHighlights);
        hide(heroCTAGuest);
        show(heroCTAUser);
        hide(ctaBottomGuest);

        if (eventRegisterBtn != null)
            eventRegisterBtn.setOnAction(e -> goToTournois());
    }

    // ════════════════════════════════════════════════════════════
    //  SETUP — GUEST
    // ════════════════════════════════════════════════════════════
    private void setupGuest() {
        hide(loggedInZone);
        show(guestZone);
        hide(navUploaderMenu);
        hide(navHighlights);
        show(heroCTAGuest);
        hide(heroCTAUser);
        show(ctaBottomGuest);
    }

    // ════════════════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ════════════════════════════════════════════════════════════
    private void updateNotifBadge(int unreadCount) {
        if (navNotifBadge == null) return;
        if (unreadCount > 0) {
            navNotifBadge.setText(String.valueOf(unreadCount));
            navNotifBadge.setVisible(true);
            navNotifBadge.setManaged(true);
            if (notifEmptyItem != null) notifEmptyItem.setVisible(false);
        } else {
            navNotifBadge.setVisible(false);
            navNotifBadge.setManaged(false);
            if (notifEmptyItem != null) notifEmptyItem.setVisible(true);
        }
    }

    public void addNotifItem(String message, String action) {
        if (navNotifMenu == null) return;
        MenuItem item = new MenuItem(message);
        if (action != null && !action.isEmpty())
            item.setOnAction(e -> handleNotifAction(action));
        int insertAt = Math.max(0, navNotifMenu.getItems().size() - 1);
        navNotifMenu.getItems().add(insertAt, item);
    }

    private void handleNotifAction(String action) {
        if (action.contains("team"))         goToTeams();
        else if (action.contains("profile")) goToProfile();
        else if (action.contains("tournoi")) goToTournois();
        else if (action.contains("support")) goToSupport();
    }

    // ════════════════════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════════════════════
    private void loadStats() {
        try {
            StatsDAO dao        = new StatsDAO();
            long players        = dao.countPlayers();
            long tournaments    = dao.countTournaments();
            long teams          = dao.countTeams();
            long coaches        = dao.countCoaches();

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

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION — handlers @FXML
    // ════════════════════════════════════════════════════════════
    @FXML public void goHome()       { navigateTo("home.fxml"); }
    @FXML public void goToLogin()    { navigateTo("login.fxml"); }
    @FXML public void goToRegister() { navigateTo("register.fxml"); }
    @FXML public void goToVideos()   { navigateTo("Videos.fxml"); }
    @FXML public void goToClips()    { navigateTo("Clips.fxml"); }
    @FXML public void goToGuides()   { navigateTo("Guides.fxml"); }
    @FXML public void goToPlanning() { navigateTo("Planning.fxml"); }
    @FXML public void goToTournois() { navigateTo("Tournois.fxml"); }
    @FXML public void goToProfile()  { navigateTo("Profile.fxml"); }
    @FXML public void goToTeams()    { navigateTo("Team.fxml"); }
    @FXML public void goToCoins()    { navigateTo("Coins.fxml"); }
    @FXML public void goToSupport()  { navigateTo("Support.fxml"); }
    @FXML public void goToAdmin()    { navigateTo("Admin.fxml"); }

    /**
     * ── 2FA Settings ──
     * Navigue vers TwoFactor.fxml (TwoFactorSettingsController).
     * Lié au MenuItem "🛡  Two-Factor Auth" dans le dropdown profil.
     *
     * Si l'utilisateur n'est pas connecté → redirige vers login.
     */
    @FXML
    public void goTo2FA() {
        // Guard : doit être connecté
        if (SessionManager.getCurrentUser() == null) {
            navigateTo("login.fxml");
            return;
        }
        navigateTo("TwoFactor.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION — méthode centrale
    //  Cherche le FXML dans 3 chemins possibles.
    //  En cas d'erreur, affiche le détail complet dans la console.
    // ════════════════════════════════════════════════════════════
    private void navigateTo(String fxml) {
        // ── Cherche dans les 3 chemins possibles ──
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };

        URL url = null;
        for (String path : paths) {
            url = getClass().getResource(path);
            if (url != null) {
                System.out.println("[HomeController] ✅ FXML trouvé : " + path);
                break;
            }
        }

        if (url == null) {
            System.err.println("[HomeController] ❌ FXML introuvable : " + fxml);
            System.err.println("  Chemins testés :");
            for (String path : paths) System.err.println("    " + path);
            System.err.println("  → Vérifiez que le fichier est dans src/main/resources/com/eyetwin/views/");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root  = loader.load();
            Stage  stage = resolveStage();
            if (stage == null) {
                System.err.println("[HomeController] ❌ Stage introuvable pour : " + fxml);
                return;
            }
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));

        } catch (IOException e) {
            // ── Affiche le vrai message d'erreur (souvent une erreur dans le Controller) ──
            System.err.println("[HomeController] ❌ Erreur chargement FXML : " + fxml);
            System.err.println("  Cause : " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("  Cause racine : " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════
    private void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true);  }
    }
    private void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                loggedInZone, guestZone, heroCTAGuest, heroCTAUser,
                navProfileMenu, navNotifMenu
        }) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}