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

    // KPI badges
    @FXML private Label userGrowthBadge;
    @FXML private Label teamGrowthBadge;
    @FXML private Label appsTodayBadge;
    @FXML private Label approvalRateSmall;
    @FXML private Label usersLast7Badge;
    @FXML private Label avgUsersPerDayLabel;

    // User stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalUsersLabel2;
    @FXML private Label activeUsersLabel;
    @FXML private Label suspendedUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label userGrowthRateLabel;

    // Advanced metrics
    @FXML private Label       activeUsersPercentageLabel;
    @FXML private Label       activeUsersDetailLabel;
    @FXML private Label       avgMembersPerTeamLabel;
    @FXML private Label       totalMembersLabel;
    @FXML private Label       approvalRateLabel;
    @FXML private Label       approvalDetailLabel;
    @FXML private Label       activeTeamsPercentageLabel;
    @FXML private Label       activeTeamsDetailLabel;
    @FXML private ProgressBar activeUsersProgress;
    @FXML private ProgressBar membersProgress;
    @FXML private ProgressBar approvalProgress;
    @FXML private ProgressBar activeTeamsProgress;

    // Team stats
    @FXML private Label totalTeamsLabel;
    @FXML private Label totalTeamsLabel2;
    @FXML private Label activeTeamsLabel;
    @FXML private Label inactiveTeamsLabel;
    @FXML private Label teamsLast7Label;
    @FXML private Label teamsLast7Label2;
    @FXML private Label teamsThisMonthLabel;
    @FXML private Label teamGrowthRateLabel;
    @FXML private Label teamGrowthBadgeLabel;

    // Applications
    @FXML private Label totalApplicationsLabel;
    @FXML private Label pendingApplicationsLabel;
    @FXML private Label pendingApplicationsLabel2;
    @FXML private Label approvedApplicationsLabel;
    @FXML private Label rejectedApplicationsLabel;
    @FXML private Label appsLast7Label;
    @FXML private Label appsLast7Label2;
    @FXML private Label appsLast30Label;
    @FXML private Label approvalRateLabel2;

    // Summary
    @FXML private Label usersTodayLabel;
    @FXML private Label usersYesterdayLabel;
    @FXML private Label regularUsersLabel;
    @FXML private Label coachesCountLabel;

    // Badges sidebar
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
        setupTopBar(SessionManager.getCurrentUser());
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
        new Thread(() -> {
            try {
                IStatsService s = new StatsServiceImpl();

                // ── User stats ──
                int    totalUsers     = s.getTotalUsers();
                int    activeUsers    = s.getActiveUsers();
                int    suspendedUsers = s.getSuspendedUsers();
                int    bannedUsers    = s.getBannedUsers();
                int    usersToday     = s.getUsersToday();
                int    usersYesterday = s.getUsersYesterday();
                int    usersLast7     = s.getUsersLast7Days();
                int    regularUsers   = s.getRegularUsers();
                int    coaches        = s.countCoaches();
                double growthRate     = s.getUserGrowthRate();
                double avgPerDay      = s.getAvgUsersPerDay();
                double activePct      = s.getActiveUsersPercentage();

                // ── Team stats ──
                int    totalTeams    = s.getTotalTeams();
                int    activeTeams   = s.getActiveTeams();
                int    inactiveTeams = s.getInactiveTeams();
                int    teamsLast7    = s.getTeamsLast7Days();
                int    teamsThisMonth= s.getTeamsThisMonth();
                int    totalMembers  = s.getTotalMembers();
                double teamGrowth    = s.getTeamGrowthRate();
                double avgMembers    = s.getAvgMembersPerTeam();
                double activeTeamPct = s.getActiveTeamsPercentage();

                // ── Application stats ──
                int    totalApps    = s.getTotalApplications();
                int    pendingApps  = s.getPendingApplications();
                int    approvedApps = s.getApprovedApplications();
                int    rejectedApps = s.getRejectedApplications();
                int    appsToday    = s.getApplicationsToday();
                int    appsLast7    = s.getApplicationsLast7Days();
                int    appsLast30   = s.getApplicationsLast30Days();
                double approvalRate = s.getApprovalRate();

                Platform.runLater(() -> {

                    // ── KPI row ──
                    setLabel(totalUsersLabel,          String.valueOf(totalUsers));
                    setLabel(totalTeamsLabel,          String.valueOf(totalTeams));
                    setLabel(pendingApplicationsLabel, String.valueOf(pendingApps));
                    setLabel(avgUsersPerDayLabel,      String.format("%.1f", avgPerDay));
                    setLabel(userGrowthBadge,          formatRate(growthRate));
                    setLabel(teamGrowthBadge,          formatRate(teamGrowth));
                    setLabel(appsTodayBadge,           appsToday + " today");
                    setLabel(approvalRateSmall,        String.format("%.1f%% approved", approvalRate));
                    setLabel(usersLast7Badge,          usersLast7 + " this week");

                    // ── User stats row ──
                    setLabel(totalUsersLabel2,         String.valueOf(totalUsers));
                    setLabel(activeUsersLabel,         String.valueOf(activeUsers));
                    setLabel(suspendedUsersLabel,      String.valueOf(suspendedUsers));
                    setLabel(bannedUsersLabel,         String.valueOf(bannedUsers));
                    setLabel(userGrowthRateLabel,      formatRate(growthRate));

                    // ── Advanced metrics ──
                    setLabel(activeUsersPercentageLabel, String.format("%.1f%%", activePct));
                    setLabel(activeUsersDetailLabel,     activeUsers + " of " + totalUsers + " users");
                    setLabel(avgMembersPerTeamLabel,     String.format("%.1f", avgMembers));
                    setLabel(totalMembersLabel,          totalMembers + " active members");
                    setLabel(approvalRateLabel,          String.format("%.1f%%", approvalRate));
                    setLabel(approvalDetailLabel,        approvedApps + " / " + totalApps + " applications");
                    setLabel(activeTeamsPercentageLabel, String.format("%.1f%%", activeTeamPct));
                    setLabel(activeTeamsDetailLabel,     activeTeams + " of " + totalTeams + " teams");

                    // ── Progress bars ──
                    if (activeUsersProgress != null) activeUsersProgress.setProgress(activePct / 100.0);
                    if (approvalProgress    != null) approvalProgress.setProgress(approvalRate / 100.0);
                    if (activeTeamsProgress != null) activeTeamsProgress.setProgress(activeTeamPct / 100.0);
                    if (membersProgress     != null) membersProgress.setProgress(Math.min(avgMembers / 10.0, 1.0));

                    // ── Team stats row ──
                    setLabel(totalTeamsLabel2,    String.valueOf(totalTeams));
                    setLabel(activeTeamsLabel,    String.valueOf(activeTeams));
                    setLabel(inactiveTeamsLabel,  String.valueOf(inactiveTeams));
                    setLabel(teamsLast7Label,     String.valueOf(teamsLast7));
                    setLabel(teamGrowthRateLabel, formatRate(teamGrowth));

                    // ── Applications row ──
                    setLabel(totalApplicationsLabel,    String.valueOf(totalApps));
                    setLabel(pendingApplicationsLabel2, String.valueOf(pendingApps));
                    setLabel(approvedApplicationsLabel, String.valueOf(approvedApps));
                    setLabel(rejectedApplicationsLabel, String.valueOf(rejectedApps));
                    setLabel(appsLast7Label,            String.valueOf(appsLast7));
                    setLabel(approvalRateLabel2,        String.format("%.1f%%", approvalRate));

                    // ── Summary row ──
                    setLabel(usersTodayLabel,    String.valueOf(usersToday));
                    setLabel(usersYesterdayLabel, usersYesterday + " yesterday");
                    setLabel(teamsLast7Label2,   String.valueOf(teamsLast7));
                    setLabel(teamsThisMonthLabel, teamsThisMonth + " this month");
                    setLabel(appsLast7Label2,    String.valueOf(appsLast7));
                    setLabel(appsLast30Label,    appsLast30 + " (30 days)");
                    setLabel(regularUsersLabel,  String.valueOf(regularUsers));
                    setLabel(coachesCountLabel,  coaches + " coaches");
                });

            } catch (Exception e) {
                System.err.println("[AdminDashboard] Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }, "DashboardStats").start();
    }

    private String formatRate(double rate) {
        return (rate >= 0 ? "+" : "") + String.format("%.1f%%", rate);
    }

    private void setLabel(Label label, String value) {
        if (label != null) label.setText(value);
    }

    // ── Navigation ──
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
    @FXML public void goToProfile()           { navigateTo("AdminProfile.fxml"); }
    @FXML public void goToFaceRegister()      { navigateTo("AdminProfile.fxml"); }

    @FXML public void goToAuditLogs() {
        if (!SessionManager.isSuperAdmin()) return;
        navigateTo("AdminAuditLogs.fxml");
    }

    @FXML public void handleLogout() {
        SessionManager.logout();
        navigateTo("AdminLogin.fxml");
    }

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