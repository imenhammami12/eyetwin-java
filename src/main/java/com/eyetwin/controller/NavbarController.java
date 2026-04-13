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

import com.eyetwin.entities.Community.AppNotification;
import com.eyetwin.services.Community.NotificationServiceImpl;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;


public class NavbarController {
    private final NotificationServiceImpl notificationService = new NotificationServiceImpl();
    private static final SimpleDateFormat NOTIF_DATE_FMT = new SimpleDateFormat("dd/MM HH:mm");

    private static final String NAVBAR_POPUP_CSS = "/com/eyetwin/assets/css/navbar-popups.css";
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

        Platform.runLater(() -> {
            attachNavbarCssToScene();
            styleStaticMenuItems();
        });
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

        loadNotifications(user);
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
        if (action == null || action.isBlank()) return;

        String v = action.toLowerCase();

        if (v.contains("team")) {
            goToTeams();
        } else if (v.contains("profile")) {
            goToProfile();
        } else if (v.contains("tournoi")) {
            goToTournois();
        } else if (v.contains("support")) {
            goToSupport();
        } else if (v.contains("channel") || v.contains("community")) {
            goToCommunity();
        }
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
            Scene currentScene = stage.getScene();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());

            if (currentScene != null && !currentScene.getStylesheets().isEmpty()) {
                newScene.getStylesheets().addAll(currentScene.getStylesheets());
            }

            stage.setScene(newScene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadNotifications(User user) {
        if (user == null || navNotifMenu == null) return;

        try {
            // keep only fixed items first
            navNotifMenu.getItems().clear();

            if (notifHeaderItem == null) {
                notifHeaderItem = new MenuItem("Notifications");
                notifHeaderItem.setDisable(true);
            }
            if (notifEmptyItem == null) {
                notifEmptyItem = new MenuItem("No notifications");
                notifEmptyItem.setDisable(true);
            }

            navNotifMenu.getItems().add(notifHeaderItem);

            List<AppNotification> notifications = notificationService.findByUser(user.getId());
            int unreadCount = notificationService.countUnreadByUser(user.getId());

            if (notifications.isEmpty()) {
                navNotifMenu.getItems().add(notifEmptyItem);
            } else {
                CustomMenuItem deleteAllItem = buildDeleteAllMenuItem();
                navNotifMenu.getItems().add(deleteAllItem);
                navNotifMenu.getItems().add(new SeparatorMenuItem());

                for (AppNotification notif : notifications) {
                    navNotifMenu.getItems().add(buildNotificationMenuItem(notif));
                }
            }

//           navNotifMenu.getItems().add(new SeparatorMenuItem());
//
//            MenuItem markInfo = new MenuItem("Recent notifications");
//            markInfo.setDisable(true);
//            navNotifMenu.getItems().add(markInfo);

            updateNotifBadge(unreadCount);

        } catch (SQLException e) {
            System.err.println("[NavbarController] Failed to load notifications: " + e.getMessage());
            updateNotifBadge(0);
        }
    }

    private String formatNotifText(AppNotification notif) {
        String date = notif.getCreatedAt() == null ? "" : "  •  " + NOTIF_DATE_FMT.format(notif.getCreatedAt());
        String prefix = notif.isRead() ? "" : "• ";
        return prefix + notif.getMessage() + date;
    }

    private void handleNotificationClick(AppNotification notif) {
        User user = SessionManager.getCurrentUser();
        if (user == null || notif == null) return;

        try {
            if (!notif.isRead()) {
                notificationService.markAsRead(notif.getId(), user.getId());
            }
        } catch (SQLException e) {
            System.err.println("[NavbarController] Failed to mark notification as read: " + e.getMessage());
        }

        // reload badge/menu after click
        loadNotifications(user);

        // basic navigation
        if (notif.getType() != null) {
            switch (notif.getType()) {
                case AppNotification.CHANNEL_APPROVED, AppNotification.CHANNEL_REJECTED -> goToCommunity();
                default -> handleNotifAction(notif.getLink());
            }
        } else {
            handleNotifAction(notif.getLink());
        }
    }

    private void attachNavbarCssToScene() {
        Stage stage = resolveStage();
        if (stage == null || stage.getScene() == null) return;

        String css = getClass().getResource(NAVBAR_POPUP_CSS).toExternalForm();
        if (!stage.getScene().getStylesheets().contains(css)) {
            stage.getScene().getStylesheets().add(css);
        }
    }

    private void styleStaticMenuItems() {
        if (notifHeaderItem != null) {
            notifHeaderItem.getStyleClass().add("notif-header-item");
        }

        if (profileHeaderItem != null) {
            profileHeaderItem.getStyleClass().add("profile-header-item");
        }

        if (profileStatsItem != null) {
            profileStatsItem.getStyleClass().add("profile-stats-item");
        }

        if (profileAdminItem != null) {
            profileAdminItem.getStyleClass().add("admin-item");
        }

        // logout item is not fx:id in your current file, so we leave it unless you want to add one later
    }

    private CustomMenuItem buildNotificationMenuItem(AppNotification notif) {
        HBox root = new HBox(8);
        root.setPrefWidth(250);
        root.setMinWidth(250);
        root.setMaxWidth(250);
        root.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 6 8 6 8;");

        Label dot = new Label(notif.isRead() ? " " : "•");
        dot.setStyle(
                notif.isRead()
                        ? "-fx-text-fill: transparent; -fx-font-size: 12px;"
                        : "-fx-text-fill: #ff5b57; -fx-font-size: 12px; -fx-font-weight: bold;"
        );

        VBox textBox = new VBox(4);
        textBox.setPrefWidth(185);
        textBox.setMinWidth(185);
        textBox.setMaxWidth(185);

        Label message = new Label(shortenNotif(notif.getMessage(), 95));
        message.setWrapText(true);
        message.setPrefWidth(185);
        message.setMinWidth(185);
        message.setMaxWidth(185);
        message.setStyle(
                "-fx-text-fill: " + (notif.isRead() ? "rgba(255,255,255,0.78)" : "white") + ";" +
                        "-fx-font-size: 12px;" +
                        (notif.isRead() ? "" : "-fx-font-weight: bold;")
        );

        Label date = new Label(
                notif.getCreatedAt() == null ? "" : NOTIF_DATE_FMT.format(notif.getCreatedAt())
        );
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 10px;");

        textBox.getChildren().addAll(message, date);

        Button btnDelete = new Button("🗑");
        btnDelete.setFocusTraversable(false);
        btnDelete.setMinSize(24, 24);
        btnDelete.setPrefSize(24, 24);
        btnDelete.setMaxSize(24, 24);
        btnDelete.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                        "-fx-border-color: rgba(251,113,133,0.40);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #fb7185;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Arial';" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;" +
                        "-fx-alignment: center;"
        );

        btnDelete.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            e.consume();
            deleteSingleNotification(notif);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(dot, textBox, spacer, btnDelete);

        root.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            Object target = e.getTarget();

            if (target instanceof Button) {
                return;
            }

            handleNotificationClick(notif);
            e.consume();
        });

        CustomMenuItem item = new CustomMenuItem(root, false);
        item.setHideOnClick(true);
        //item.setOnAction(e -> handleNotificationClick(notif));
        return item;
    }
    private void deleteSingleNotification(AppNotification notif) {
        User user = SessionManager.getCurrentUser();
        if (user == null || notif == null) return;

        try {
            notificationService.deleteNotification(notif.getId(), user.getId());
            loadNotifications(user);
        } catch (SQLException e) {
            System.err.println("[NavbarController] Failed to delete notification: " + e.getMessage());
        }
    }

    private CustomMenuItem buildDeleteAllMenuItem() {
        HBox box = new HBox();
        box.setPrefWidth(250);
        box.setMinWidth(250);
        box.setMaxWidth(250);
        box.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        box.setStyle("-fx-padding: 2 8 6 8;");

        Button btnDeleteAll = new Button("Delete all");
        btnDeleteAll.setFocusTraversable(false);
        btnDeleteAll.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                        "-fx-border-color: rgba(251,113,133,0.35);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #fb7185;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 5 10 5 10;"
        );

        btnDeleteAll.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            e.consume();
            deleteAllNotifications();
        });

        box.getChildren().add(btnDeleteAll);

        CustomMenuItem item = new CustomMenuItem(box, false);
        item.setHideOnClick(false);
        return item;
    }

    private void deleteAllNotifications() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        try {
            notificationService.deleteAllNotifications(user.getId());
            loadNotifications(user);
        } catch (SQLException e) {
            System.err.println("[NavbarController] Failed to delete all notifications: " + e.getMessage());
        }
    }

    private String shortenNotif(String text, int max) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= max) return clean;
        return clean.substring(0, max - 1) + "…";
    }
}