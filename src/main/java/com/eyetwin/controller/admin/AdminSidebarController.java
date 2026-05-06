package com.eyetwin.controller.admin;

import com.eyetwin.tools.SessionManager;
import com.eyetwin.entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminSidebarController {

    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navPlanning;
    @FXML private Button navTournaments;
    @FXML private Button navVideos;
    @FXML private Button navGuides;
    @FXML private Button navCoachApps;
    @FXML private Button navChannels;
    @FXML private Button navComplaints;
    @FXML private Button navMessages;
    @FXML private Button navTeams;
    @FXML private Button navAuditLogs;

    @FXML private Label pendingApplicationsBadge;
    @FXML private Label pendingComplaintsBadge;

    private static final String STYLE_ACTIVE =
            "-fx-background-color: rgba(255,60,100,0.12);" +
                    "-fx-border-color: rgba(255,60,100,0.35);" +
                    "-fx-border-radius: 10; -fx-background-radius: 10;" +
                    "-fx-border-width: 1; -fx-text-fill: white;" +
                    "-fx-font-size: 12; -fx-font-weight: bold;" +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 14 11 14; -fx-cursor: hand;";

    private static final String STYLE_INACTIVE =
            "-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12;" +
                    "-fx-alignment: CENTER_LEFT; -fx-padding: 11 14 11 14;" +
                    "-fx-cursor: hand; -fx-background-radius: 10;";

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null && SessionManager.isSuperAdmin()) {
            if (navAuditLogs != null) {
                navAuditLogs.setVisible(true);
                navAuditLogs.setManaged(true);
            }
        }
    }

    public void setActivePage(String page) {
        Button[] all = { navDashboard, navUsers, navPlanning, navTournaments,
                navVideos, navGuides, navCoachApps, navChannels, navComplaints,
                navMessages, navTeams, navAuditLogs };
        for (Button b : all)
            if (b != null) b.setStyle(STYLE_INACTIVE);

        Button active = switch (page) {
            case "dashboard"   -> navDashboard;
            case "users"       -> navUsers;
            case "planning"    -> navPlanning;
            case "tournaments" -> navTournaments;
            case "videos"      -> navVideos;
            case "guides"      -> navGuides;
            case "coachapps"   -> navCoachApps;
            case "channels"    -> navChannels;
            case "complaints"  -> navComplaints;
            case "messages"    -> navMessages;
            case "teams"       -> navTeams;
            case "auditlogs"   -> navAuditLogs;
            default            -> null;
        };
        if (active != null) active.setStyle(STYLE_ACTIVE);
    }

    public void setBadge(String badge, int count) {
        switch (badge) {
            case "applications" -> {
                if (pendingApplicationsBadge != null) {
                    pendingApplicationsBadge.setText(String.valueOf(count));
                    pendingApplicationsBadge.setVisible(count > 0);
                    pendingApplicationsBadge.setManaged(count > 0);
                }
            }
            case "complaints" -> {
                if (pendingComplaintsBadge != null) {
                    pendingComplaintsBadge.setText(String.valueOf(count));
                    pendingComplaintsBadge.setVisible(count > 0);
                    pendingComplaintsBadge.setManaged(count > 0);
                }
            }
        }
    }

    @FXML public void goToDashboard()         { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToPlanning()          { navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { navigateTo("AdminTournaments.fxml"); }
    @FXML public void goToMatches()           { navigateTo("Matches.fxml"); }
    @FXML public void goToVideos()            { navigateTo("AdminVideos.fxml"); }
    @FXML public void goToGuides()            { navigateTo("AdminGuides.fxml"); }
    @FXML public void goToCoachApplications() { navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { navigateTo("AdminMessages.fxml"); }
    @FXML public void goToTeams()             { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToAuditLogs()         { navigateTo("AdminAuditLogs.fxml"); }
    @FXML public void goToSite()              { navigateTo("home.fxml"); }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
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
            System.err.println("❌ FXML not found: " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);   // ✅ méthode statique — résout les chemins relatifs
            Stage  stage = resolveStage();
            if (stage == null) return;
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage resolveStage() {
        for (Button b : new Button[]{ navDashboard, navUsers, navTeams }) {
            if (b != null && b.getScene() != null)
                return (Stage) b.getScene().getWindow();
        }
        return null;
    }
}
