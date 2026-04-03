package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IStatsService;
import com.eyetwin.services.StatsServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminDashboardController {

    @FXML private Button navDashboard;
    @FXML private Button navAuditLogs;
    @FXML private Label  pageTitle;
    @FXML private Label  usernameLabel;
    @FXML private Label  userAvatarInitial;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label suspendedUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label totalTeamsLabel;
    @FXML private Label activeTeamsLabel;
    @FXML private Label totalApplicationsLabel;
    @FXML private Label pendingApplicationsLabel;
    @FXML private Label approvedApplicationsLabel;
    @FXML private Label rejectedApplicationsLabel;
    @FXML private Label approvalRateLabel;
    @FXML private Label userGrowthRateLabel;
    @FXML private Label pendingApplicationsBadge;
    @FXML private Label pendingComplaintsBadge;
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;


    @FXML
    public void initialize() {
        adminSidebarController.setActivePage("dashboard");
        adminTopbarController.setTitle("Dashboard");

        if (!SessionManager.isAdmin()) {
            navigateTo("AdminLogin.fxml");
            return;
        }
        User user = SessionManager.getCurrentUser();
        setupTopBar(user);
        setupSidebar();
        loadDashboardStats();

        String[] flash = SessionManager.consumeFlash();
        if (flash != null)
            System.out.println("[AdminDashboard] Flash: [" + flash[0] + "] " + flash[1]);
    }

    private void setupTopBar(User user) {
        if (pageTitle != null) pageTitle.setText("Dashboard");
        if (user != null) {
            String username = user.getUsername() != null ? user.getUsername() : "Admin";
            if (usernameLabel     != null) usernameLabel.setText(username);
            if (userAvatarInitial != null)
                userAvatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }
    }

    private void setupSidebar() {
        if (navAuditLogs != null) {
            navAuditLogs.setVisible(SessionManager.isSuperAdmin());
            navAuditLogs.setManaged(SessionManager.isSuperAdmin());
        }
    }

    private void loadDashboardStats() {
        new Thread(() -> Platform.runLater(() -> {
            setLabel(totalUsersLabel,          "—");
            setLabel(activeUsersLabel,         "—");
            setLabel(suspendedUsersLabel,      "—");
            setLabel(bannedUsersLabel,         "—");
            setLabel(totalTeamsLabel,          "—");
            setLabel(activeTeamsLabel,         "—");
            setLabel(totalApplicationsLabel,   "—");
            setLabel(pendingApplicationsLabel, "—");
            setLabel(approvedApplicationsLabel,"—");
            setLabel(rejectedApplicationsLabel,"—");
            setLabel(approvalRateLabel,        "— %");
            setLabel(userGrowthRateLabel,      "— %");
        })).start();
    }

    private void setLabel(Label label, String value) {
        if (label != null) label.setText(value);
    }

    // ── Navigation sidebar ──────────────────────────────────────

    @FXML public void goToDashboard()         { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToPlanning()          { navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { navigateTo("AdminTournois.fxml"); }
    @FXML public void goToVideos()            { navigateTo("AdminVideos.fxml"); }
    @FXML public void goToCoachApplications() { navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { navigateTo("AdminMessages.fxml"); }
    @FXML public void goToTeams()             { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToSite()              { navigateTo("home.fxml"); }

    // ✅ FIX : My Profile → AdminProfile (pas UserProfile)
    @FXML public void goToProfile()           { navigateTo("AdminProfile.fxml"); }

    // ✅ FIX : Face Recognition → AdminProfile onglet Security (pas FaceRegister)
    @FXML public void goToFaceRegister()      { navigateTo("AdminProfile.fxml"); }

    @FXML
    public void goToAuditLogs() {
        if (!SessionManager.isSuperAdmin()) return;
        navigateTo("AdminAuditLogs.fxml");
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        navigateTo("AdminLogin.fxml");
    }

    // ── Navigation interne ──────────────────────────────────────

    private void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        URL url = null;
        for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
        if (url == null) {
            System.err.println("[AdminDashboardController] FXML introuvable : " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminDashboardController] Erreur : " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{ navDashboard, pageTitle, usernameLabel }) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}