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

// ✅ Charts
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

// ✅ Java collections
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class AdminDashboardController {

    @FXML private Button navDashboard;
    @FXML private Button navAuditLogs;
    @FXML private Label  pageTitle;
    @FXML private Label  usernameLabel;
    @FXML private Label  userAvatarInitial;

    // Nouveaux @FXML pour les charts
    @FXML private LineChart<String, Number> activityLineChart;
    @FXML private CategoryAxis             lineChartXAxis;
    @FXML private NumberAxis               lineChartYAxis;
    @FXML private PieChart                 userPieChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private Label                    adminsCountLabel;
    @FXML private Label                    regularUsersLabel2;
    @FXML private Label                    coachesCountLabel2;

    // KPI badges
    @FXML private Label userGrowthBadge;
    @FXML private Label teamGrowthBadge;
    @FXML private Label appsTodayBadge;
    @FXML private Label approvalRateSmall;
    @FXML private Label usersLast7Badge;
    @FXML private Label avgUsersPerDayLabel;

    // Channel KPI
    @FXML private Label totalChannelsLabel;
    @FXML private Label approvedChannelsLabel;
    @FXML private Label pendingChannelsDashboardLabel;
    @FXML private Label channelsLast7DaysLabel;
    @FXML private Label channelGrowthBadge;
    @FXML private Label approvedChannelsSmallLabel;
    @FXML private Label pendingChannelsSmallLabel;
    @FXML private Label channelsThisMonthBadge;

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
        if (activityLineChart != null) {
            URL cssUrl = getClass().getResource("/com/eyetwin/css/chart.css");
            if (cssUrl != null) {
                if (activityLineChart.getScene() != null) {
                    activityLineChart.getScene().getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    activityLineChart.getStylesheets().add(cssUrl.toExternalForm());
                }
            }
        }
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

                // User stats
                int    totalUsers     = s.getTotalUsers();
                int    activeUsers    = s.getActiveUsers();
                int    suspendedUsers = s.getSuspendedUsers();
                int    bannedUsers    = s.getBannedUsers();
                int    usersToday     = s.getUsersToday();
                int    usersYesterday = s.getUsersYesterday();
                int    usersLast7     = s.getUsersLast7Days();
                int    regularUsers   = s.getRegularUsers();
                int    coaches        = s.countCoaches();
                int    admins         = s.getTotalAdmins();
                double growthRate     = s.getUserGrowthRate();
                double avgPerDay      = s.getAvgUsersPerDay();
                double activePct      = s.getActiveUsersPercentage();

                // Team stats
                int    totalTeams     = s.getTotalTeams();
                int    activeTeams    = s.getActiveTeams();
                int    inactiveTeams  = s.getInactiveTeams();
                int    teamsLast7     = s.getTeamsLast7Days();
                int    teamsThisMonth = s.getTeamsThisMonth();
                int    totalMembers   = s.getTotalMembers();
                double teamGrowth     = s.getTeamGrowthRate();
                double avgMembers     = s.getAvgMembersPerTeam();
                double activeTeamPct  = s.getActiveTeamsPercentage();

                // App stats
                int    totalApps    = s.getTotalApplications();
                int    pendingApps  = s.getPendingApplications();
                int    approvedApps = s.getApprovedApplications();
                int    rejectedApps = s.getRejectedApplications();
                int    appsToday    = s.getApplicationsToday();
                int    appsLast7    = s.getApplicationsLast7Days();
                int    appsLast30   = s.getApplicationsLast30Days();
                double approvalRate = s.getApprovalRate();

                // Channel stats
                int    totalChannels     = s.getTotalChannels();
                int    approvedChannels  = s.getApprovedChannels();
                int    pendingChannels   = s.getPendingChannels();
                int    channelsLast7     = s.getChannelsLast7Days();
                int    channelsThisMonth = s.getChannelsThisMonth();
                double channelGrowth     = s.getChannelGrowthRate();

                // Chart data
                List<String>  labels     = s.getLast7DaysLabels();
                List<Integer> usersChart = s.getUsersLast7DaysChart();
                List<Integer> teamsChart = s.getTeamsLast7DaysChart();
                List<Integer> appsChart  = s.getAppsLast7DaysChart();

                // Monthly comparison
                int usersThisMonth = s.getUsersThisMonth();
                int usersLastMonth = s.getUsersLastMonth();
                int teamsLastMonth = s.getTeamsLastMonth();
                int appsThisMonth  = s.getApplicationsLast30Days();
                int appsLastMonth  = rejectedApps + approvedApps;

                Platform.runLater(() -> {
                    // KPI
                    setLabel(totalUsersLabel,          String.valueOf(totalUsers));
                    setLabel(totalTeamsLabel,          String.valueOf(totalTeams));
                    setLabel(pendingApplicationsLabel, String.valueOf(pendingApps));
                    setLabel(avgUsersPerDayLabel,      String.format("%.1f", avgPerDay));
                    setLabel(userGrowthBadge,          formatRate(growthRate));
                    setLabel(teamGrowthBadge,          formatRate(teamGrowth));
                    setLabel(appsTodayBadge,           appsToday + " today");
                    setLabel(approvalRateSmall,        String.format("%.1f%% approved", approvalRate));
                    setLabel(usersLast7Badge,          usersLast7 + " this week");

                    // Channel KPI
                    setLabel(totalChannelsLabel,              String.valueOf(totalChannels));
                    setLabel(approvedChannelsLabel,           String.valueOf(approvedChannels));
                    setLabel(pendingChannelsDashboardLabel,   String.valueOf(pendingChannels));
                    setLabel(channelsLast7DaysLabel,          String.valueOf(channelsLast7));
                    setLabel(channelGrowthBadge,              formatRate(channelGrowth));
                    setLabel(approvedChannelsSmallLabel,      approvedChannels + " approved");
                    setLabel(pendingChannelsSmallLabel,       pendingChannels + " pending");
                    setLabel(channelsThisMonthBadge,          channelsThisMonth + " this month");

                    // User stats
                    setLabel(totalUsersLabel2,         String.valueOf(totalUsers));
                    setLabel(activeUsersLabel,         String.valueOf(activeUsers));
                    setLabel(suspendedUsersLabel,      String.valueOf(suspendedUsers));
                    setLabel(bannedUsersLabel,         String.valueOf(bannedUsers));
                    setLabel(userGrowthRateLabel,      formatRate(growthRate));

                    // Advanced metrics
                    setLabel(activeUsersPercentageLabel, String.format("%.1f%%", activePct));
                    setLabel(activeUsersDetailLabel,     activeUsers + " of " + totalUsers + " users");
                    setLabel(avgMembersPerTeamLabel,     String.format("%.1f", avgMembers));
                    setLabel(totalMembersLabel,          totalMembers + " active members");
                    setLabel(approvalRateLabel,          String.format("%.1f%%", approvalRate));
                    setLabel(approvalDetailLabel,        approvedApps + " / " + totalApps + " applications");
                    setLabel(activeTeamsPercentageLabel, String.format("%.1f%%", activeTeamPct));
                    setLabel(activeTeamsDetailLabel,     activeTeams + " of " + totalTeams + " teams");

                    // Progress bars
                    if (activeUsersProgress != null) activeUsersProgress.setProgress(activePct / 100.0);
                    if (approvalProgress    != null) approvalProgress.setProgress(approvalRate / 100.0);
                    if (activeTeamsProgress != null) activeTeamsProgress.setProgress(activeTeamPct / 100.0);
                    if (membersProgress     != null) membersProgress.setProgress(Math.min(avgMembers / 10.0, 1.0));

                    // Teams
                    setLabel(totalTeamsLabel2,    String.valueOf(totalTeams));
                    setLabel(activeTeamsLabel,    String.valueOf(activeTeams));
                    setLabel(inactiveTeamsLabel,  String.valueOf(inactiveTeams));
                    setLabel(teamsLast7Label,     String.valueOf(teamsLast7));
                    setLabel(teamGrowthRateLabel, formatRate(teamGrowth));

                    // Applications
                    setLabel(totalApplicationsLabel,    String.valueOf(totalApps));
                    setLabel(pendingApplicationsLabel2, String.valueOf(pendingApps));
                    setLabel(approvedApplicationsLabel, String.valueOf(approvedApps));
                    setLabel(rejectedApplicationsLabel, String.valueOf(rejectedApps));
                    setLabel(appsLast7Label,            String.valueOf(appsLast7));
                    setLabel(approvalRateLabel2,        String.format("%.1f%%", approvalRate));

                    // Summary
                    setLabel(usersTodayLabel,     String.valueOf(usersToday));
                    setLabel(usersYesterdayLabel, usersYesterday + " yesterday");
                    setLabel(teamsLast7Label2,    String.valueOf(teamsLast7));
                    setLabel(teamsThisMonthLabel, teamsThisMonth + " this month");
                    setLabel(appsLast7Label2,     String.valueOf(appsLast7));
                    setLabel(appsLast30Label,     appsLast30 + " (30 days)");
                    setLabel(regularUsersLabel,   String.valueOf(regularUsers));
                    setLabel(coachesCountLabel,   coaches + " coaches");
                    setLabel(regularUsersLabel2,  String.valueOf(regularUsers));
                    setLabel(coachesCountLabel2,  coaches + " coaches");
                    setLabel(adminsCountLabel,    String.valueOf(admins));

                    // ══ LINE CHART ══
                    setupLineChart(labels, usersChart, teamsChart, appsChart);

                    // ══ PIE CHART ══
                    setupPieChart(regularUsers, coaches, admins);

                    // ══ BAR CHART ══
                    setupBarChart(usersThisMonth, usersLastMonth,
                            teamsThisMonth, teamsLastMonth,
                            appsLast7, appsLastMonth);
                });

            } catch (Exception e) {
                System.err.println("[AdminDashboard] Erreur: " + e.getMessage());
                e.printStackTrace();
            }
        }, "DashboardStats").start();
    }

    private void setupLineChart(List<String> labels,
                                List<Integer> users,
                                List<Integer> teams,
                                List<Integer> apps) {
        if (activityLineChart == null) return;

        activityLineChart.setAnimated(true);
        activityLineChart.setCreateSymbols(true);
        activityLineChart.setLegendVisible(true);

        URL cssUrl = getClass().getResource("/com/eyetwin/css/chart.css");
        if (cssUrl != null)
            activityLineChart.getStylesheets().add(cssUrl.toExternalForm());

        XYChart.Series<String, Number> userSeries = new XYChart.Series<>();
        userSeries.setName("Users");
        XYChart.Series<String, Number> teamSeries = new XYChart.Series<>();
        teamSeries.setName("Teams");
        XYChart.Series<String, Number> appSeries  = new XYChart.Series<>();
        appSeries.setName("Applications");

        for (int i = 0; i < labels.size(); i++) {
            userSeries.getData().add(new XYChart.Data<>(labels.get(i), users.get(i)));
            teamSeries.getData().add(new XYChart.Data<>(labels.get(i), teams.get(i)));
            appSeries.getData().add(new XYChart.Data<>(labels.get(i),  apps.get(i)));
        }

        activityLineChart.getData().clear();
        activityLineChart.getData().addAll(userSeries, teamSeries, appSeries);

        activityLineChart.lookupAll(".chart-series-line").forEach(n ->
                n.setStyle("-fx-stroke-width: 2.5;"));
    }

    private void setupPieChart(int regular, int coaches, int admins) {
        if (userPieChart == null) return;
        userPieChart.setAnimated(true);
        userPieChart.setLegendVisible(false);
        userPieChart.getData().clear();
        if (regular + coaches + admins == 0) return;

        PieChart.Data d1 = new PieChart.Data("Users",   regular);
        PieChart.Data d2 = new PieChart.Data("Coaches", coaches);
        PieChart.Data d3 = new PieChart.Data("Admins",  admins);
        userPieChart.getData().addAll(d1, d2, d3);

        Platform.runLater(() -> {
            if (d1.getNode() != null) d1.getNode().setStyle("-fx-pie-color: #667eea;");
            if (d2.getNode() != null) d2.getNode().setStyle("-fx-pie-color: #f093fb;");
            if (d3.getNode() != null) d3.getNode().setStyle("-fx-pie-color: #4facfe;");
        });
    }

    private void setupBarChart(int usersThis, int usersLast,
                               int teamsThis, int teamsLast,
                               int appsThis,  int appsLast) {
        if (monthlyBarChart == null) return;
        monthlyBarChart.setAnimated(true);
        monthlyBarChart.setLegendVisible(true);
        monthlyBarChart.getData().clear();

        XYChart.Series<String, Number> thisMonth = new XYChart.Series<>();
        thisMonth.setName("This Month");
        XYChart.Series<String, Number> lastMonth = new XYChart.Series<>();
        lastMonth.setName("Last Month");

        thisMonth.getData().add(new XYChart.Data<>("Users", usersThis));
        thisMonth.getData().add(new XYChart.Data<>("Teams", teamsThis));
        thisMonth.getData().add(new XYChart.Data<>("Apps",  appsThis));

        lastMonth.getData().add(new XYChart.Data<>("Users", usersLast));
        lastMonth.getData().add(new XYChart.Data<>("Teams", teamsLast));
        lastMonth.getData().add(new XYChart.Data<>("Apps",  appsLast));

        monthlyBarChart.getData().addAll(thisMonth, lastMonth);
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