package com.eyetwin.controller;

import com.eyetwin.entities.User;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class NavbarController {

    // ── Zones ──
    @FXML private HBox loggedInZone;
    @FXML private HBox guestZone;

    // ── Coins ──
    @FXML private HBox  coinsBadge;
    @FXML private Label coinsNavLabel;

    // ── Notifications ──
    @FXML private MenuButton navNotifMenu;
    @FXML private Label      navNotifBadge;
    @FXML private MenuItem   notifHeaderItem;
    @FXML private MenuItem   notifEmptyItem;

    // ── Profile ──
    @FXML private MenuButton        navProfileMenu;
    @FXML private Label             navAvatarInitial;
    @FXML private Label             navUsername;
    @FXML private MenuItem          profileHeaderItem;
    @FXML private MenuItem          profileStatsItem;
    @FXML private MenuItem          profileAdminItem;
    @FXML private SeparatorMenuItem profileAdminSep;

    // ── Uploader / Highlights ──
    @FXML private MenuButton navUploaderMenu;
    @FXML private Label      navHighlights;


    @FXML private Label navHome;
    @FXML private Label navPlanning;
    @FXML private Label navTournois;
    @FXML private Label navTeams;
    @FXML private Label navCommunity;

    // ════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null) setupLoggedIn(user);
        else              setupGuest();
    }

    // ════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════
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
        if (coinsBadge != null) {
            coinsBadge.setVisible(true);
            coinsBadge.setManaged(true);
        }

        boolean isAdmin = SessionManager.isAdmin();
        if (profileAdminItem != null) profileAdminItem.setVisible(isAdmin);
        if (profileAdminSep  != null) profileAdminSep.setVisible(isAdmin);

        updateNotifBadge(0);
        show(navUploaderMenu);
        show(navHighlights);
    }

    private void setupGuest() {
        hide(loggedInZone);
        show(guestZone);
        hide(navUploaderMenu);
        hide(navHighlights);
    }

    // ════════════════════════════════════════════
    //  NOTIFICATIONS — appelable depuis l'extérieur
    // ════════════════════════════════════════════
    public void updateNotifBadge(int unreadCount) {
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
        if      (action.contains("team"))    goToTeams();
        else if (action.contains("profile")) goToProfile();
        else if (action.contains("tournoi")) goToTournois();
        else if (action.contains("support")) goToSupport();
    }

    // ════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════
    @FXML public void goHome()       { navigateTo("home.fxml"); }
    @FXML public void goToLogin()    { navigateTo("login.fxml"); }
    @FXML public void goToRegister() { navigateTo("register.fxml"); }
    @FXML public void goToVideos()   { navigateTo("Videos.fxml"); }
    @FXML public void goToClips()    { navigateTo("Clips.fxml"); }
    @FXML public void goToGuides()   { navigateTo("Guides.fxml"); }
    @FXML public void goToPlanning() { navigateTo("Planning.fxml"); }
    @FXML public void goToTournois() { navigateTo("Tournois.fxml"); }
    @FXML public void goToProfile()  { navigateTo("UserProfile.fxml"); }
    @FXML public void goToTeams()    { navigateTo("Team.fxml"); }
    @FXML public void goToCoins()    { navigateTo("Coins.fxml"); }
    @FXML public void goToSupport()  { navigateTo("Support.fxml"); }

    @FXML
    public void goToAdmin() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }
        if (user.getFaceDescriptor() != null && !user.getFaceDescriptor().isBlank())
            navigateTo("FaceVerify.fxml");
        else
            navigateTo("Admin.fxml");
    }

    @FXML
    public void goTo2FA() {
        if (SessionManager.getCurrentUser() == null) { navigateTo("login.fxml"); return; }
        navigateTo("TwoFactor.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
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

    public void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        URL url = null;
        for (String path : paths) {
            url = getClass().getResource(path);
            if (url != null) break;
        }
        if (url == null) {
            System.err.println("[NavbarController] ❌ FXML introuvable : " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage == null) return;

            // ── Copier les stylesheets de la scène courante ──
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            Scene currentScene = stage.getScene();
            if (currentScene != null && !currentScene.getStylesheets().isEmpty()) {
                newScene.getStylesheets().addAll(currentScene.getStylesheets());
            }

            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[NavbarController] ❌ Erreur chargement : " + fxml);
            e.printStackTrace();
        }
    }
    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                loggedInZone, guestZone, navProfileMenu, navNotifMenu
        }) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    public void setActivePage(String page) {
        String inactive = "-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size:10; -fx-font-weight:bold; -fx-padding: 22 14 22 14; -fx-cursor: hand; -fx-border-color: transparent;";
        String active   = "-fx-text-fill: white; -fx-font-size:10; -fx-font-weight:bold; -fx-padding: 22 14 22 14; -fx-cursor: hand; -fx-border-color: transparent transparent #e8372a transparent; -fx-border-width: 0 0 2 0;";

        if (navHome      != null) navHome.setStyle(inactive);
        if (navPlanning  != null) navPlanning.setStyle(inactive);
        if (navTournois  != null) navTournois.setStyle(inactive);
        if (navTeams     != null) navTeams.setStyle(inactive);
        if (navCommunity != null) navCommunity.setStyle(inactive);

        switch (page) {
            case "home"      -> { if (navHome      != null) navHome.setStyle(active); }
            case "planning"  -> { if (navPlanning  != null) navPlanning.setStyle(active); }
            case "tournois"  -> { if (navTournois  != null) navTournois.setStyle(active); }
            case "teams"     -> { if (navTeams     != null) navTeams.setStyle(active); }
            case "community" -> { if (navCommunity != null) navCommunity.setStyle(active); }
            case "support"   -> {}
        }
    }

    @FXML
    private void goToCommunity() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/Community.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) navHome.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}