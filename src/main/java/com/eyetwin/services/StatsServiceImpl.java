package com.eyetwin.services;

import com.eyetwin.interfaces.IStatsService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * StatsServiceImpl — implémentation de IStatsService.
 * Fusionne l'ancien StatsDAO (accès SQL) + logique métier.
 */public class StatsServiceImpl implements IStatsService {

    // ── Existants ──
    @Override public int countPlayers() {
        return count("SELECT COUNT(*) FROM `user` WHERE roles_json NOT LIKE '%ROLE_ADMIN%'");
    }
    @Override public int countCoaches() {
        return count("SELECT COUNT(*) FROM `user` WHERE roles_json LIKE '%ROLE_COACH%'");
    }
    @Override public int countTeams() {
        return count("SELECT COUNT(*) FROM `team`");
    }
    @Override public int countTournaments() {
        return count("SELECT COUNT(*) FROM `tournoi`");
    }

    // ── User Stats ──
    @Override public int getTotalUsers() {
        return count("SELECT COUNT(*) FROM `user`");
    }
    @Override public int getActiveUsers() {
        return count("SELECT COUNT(*) FROM `user` WHERE account_status = 'active'");
    }
    @Override public int getSuspendedUsers() {
        return count("SELECT COUNT(*) FROM `user` WHERE account_status = 'suspended'");
    }
    @Override public int getBannedUsers() {
        return count("SELECT COUNT(*) FROM `user` WHERE account_status = 'banned'");
    }
    @Override public int getTotalAdmins() {
        return count("SELECT COUNT(*) FROM `user` WHERE roles_json LIKE '%ROLE_ADMIN%'");
    }
    @Override public int getRegularUsers() {
        int total   = getTotalUsers();
        int coaches = countCoaches();
        int admins  = getTotalAdmins();
        return total - coaches - admins;
    }
    @Override public int getUsersToday() {
        return count("SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = CURDATE()");
    }
    @Override public int getUsersYesterday() {
        return count("SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = CURDATE() - INTERVAL 1 DAY");
    }
    @Override public int getUsersLast7Days() {
        return count("SELECT COUNT(*) FROM `user` WHERE created_at >= NOW() - INTERVAL 7 DAY");
    }
    @Override public int getUsersLast30Days() {
        return count("SELECT COUNT(*) FROM `user` WHERE created_at >= NOW() - INTERVAL 30 DAY");
    }
    @Override public int getUsersThisMonth() {
        return count("SELECT COUNT(*) FROM `user` WHERE MONTH(created_at)=MONTH(NOW()) AND YEAR(created_at)=YEAR(NOW())");
    }
    @Override public int getUsersLastMonth() {
        return count("SELECT COUNT(*) FROM `user` WHERE MONTH(created_at)=MONTH(NOW()-INTERVAL 1 MONTH) AND YEAR(created_at)=YEAR(NOW()-INTERVAL 1 MONTH)");
    }
    @Override public double getUserGrowthRate() {
        int thisMonth = getUsersThisMonth();
        int lastMonth = getUsersLastMonth();
        if (lastMonth == 0) return thisMonth > 0 ? 100.0 : 0.0;
        return Math.round(((thisMonth - lastMonth) * 100.0 / lastMonth) * 10.0) / 10.0;
    }
    @Override public double getAvgUsersPerDay() {
        int last30 = getUsersLast30Days();
        return Math.round((last30 / 30.0) * 10.0) / 10.0;
    }
    @Override public double getActiveUsersPercentage() {
        int total = getTotalUsers();
        if (total == 0) return 0.0;
        return Math.round((getActiveUsers() * 100.0 / total) * 10.0) / 10.0;
    }

    // ── Team Stats ──
    @Override public int getTotalTeams() {
        return count("SELECT COUNT(*) FROM `team`");
    }
    @Override public int getActiveTeams() {
        return count("SELECT COUNT(*) FROM `team` WHERE is_active = 1");
    }
    @Override public int getInactiveTeams() {
        return count("SELECT COUNT(*) FROM `team` WHERE is_active = 0");
    }
    @Override public int getTeamsToday() {
        return count("SELECT COUNT(*) FROM `team` WHERE DATE(created_at) = CURDATE()");
    }
    @Override public int getTeamsLast7Days() {
        return count("SELECT COUNT(*) FROM `team` WHERE created_at >= NOW() - INTERVAL 7 DAY");
    }
    @Override public int getTeamsThisMonth() {
        return count("SELECT COUNT(*) FROM `team` WHERE MONTH(created_at)=MONTH(NOW()) AND YEAR(created_at)=YEAR(NOW())");
    }
    @Override public int getTeamsLastMonth() {
        return count("SELECT COUNT(*) FROM `team` WHERE MONTH(created_at)=MONTH(NOW()-INTERVAL 1 MONTH) AND YEAR(created_at)=YEAR(NOW()-INTERVAL 1 MONTH)");
    }
    @Override public double getTeamGrowthRate() {
        int thisMonth = getTeamsThisMonth();
        int lastMonth = getTeamsLastMonth();
        if (lastMonth == 0) return thisMonth > 0 ? 100.0 : 0.0;
        return Math.round(((thisMonth - lastMonth) * 100.0 / lastMonth) * 10.0) / 10.0;
    }
    @Override public int getTotalMembers() {
        return count("SELECT COUNT(*) FROM `team_membership`");
    }
    @Override public int getActiveMembers() {
        return count("SELECT COUNT(*) FROM `team_membership` WHERE status = 'active'");
    }
    @Override public double getAvgMembersPerTeam() {
        int teams = getTotalTeams();
        if (teams == 0) return 0.0;
        return Math.round((getTotalMembers() * 10.0 / teams)) / 10.0;
    }
    @Override public double getActiveTeamsPercentage() {
        int total = getTotalTeams();
        if (total == 0) return 0.0;
        return Math.round((getActiveTeams() * 100.0 / total) * 10.0) / 10.0;
    }

    // ── Application Stats ──
    @Override public int getTotalApplications() {
        return count("SELECT COUNT(*) FROM `coach_application`");
    }
    @Override public int getPendingApplications() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE status = 'pending'");
    }
    @Override public int getApprovedApplications() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE status = 'approved'");
    }
    @Override public int getRejectedApplications() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE status = 'rejected'");
    }
    @Override public int getApplicationsToday() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE DATE(submitted_at) = CURDATE()");
    }
    @Override public int getApplicationsLast7Days() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE submitted_at >= NOW() - INTERVAL 7 DAY");
    }
    @Override public int getApplicationsLast30Days() {
        return count("SELECT COUNT(*) FROM `coach_application` WHERE submitted_at >= NOW() - INTERVAL 30 DAY");
    }
    @Override public double getApprovalRate() {
        int total = getTotalApplications();
        if (total == 0) return 0.0;
        return Math.round((getApprovedApplications() * 100.0 / total) * 10.0) / 10.0;
    }

    // ── Helper ──
    private int count(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ StatsServiceImpl: " + e.getMessage());
        }
        return 0;
    }


    // Dans StatsServiceImpl — implémentation
    @Override
    public List<Integer> getUsersLast7DaysChart() {
        return getLast7DaysData("SELECT COUNT(*) FROM `user` WHERE DATE(created_at) = ?");
    }

    @Override
    public List<Integer> getTeamsLast7DaysChart() {
        return getLast7DaysData("SELECT COUNT(*) FROM `team` WHERE DATE(created_at) = ?");
    }

    @Override
    public List<Integer> getAppsLast7DaysChart() {
        return getLast7DaysData("SELECT COUNT(*) FROM `coach_application` WHERE DATE(submitted_at) = ?");
    }

    @Override
    public List<String> getLast7DaysLabels() {
        List<String> labels = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            labels.add(d.format(DateTimeFormatter.ofPattern("dd/MM")));
        }
        return labels;
    }

    private List<Integer> getLast7DaysData(String sql) {
        List<Integer> data = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 6; i >= 0; i--) {
                stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now().minusDays(i)));
                try (ResultSet rs = stmt.executeQuery()) {
                    data.add(rs.next() ? rs.getInt(1) : 0);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ getLast7DaysData: " + e.getMessage());
            for (int i = 0; i < 7; i++) data.add(0);
        }
        return data;
    }
}