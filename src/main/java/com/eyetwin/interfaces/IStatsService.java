package com.eyetwin.interfaces;

public interface IStatsService {
    // ── Existants ──
    int countPlayers();
    int countCoaches();
    int countTeams();
    int countTournaments();

    // ── User Stats ──
    int getTotalUsers();
    int getActiveUsers();
    int getSuspendedUsers();
    int getBannedUsers();
    int getTotalAdmins();
    int getRegularUsers();
    int getUsersToday();
    int getUsersYesterday();
    int getUsersLast7Days();
    int getUsersLast30Days();
    int getUsersThisMonth();
    int getUsersLastMonth();
    double getUserGrowthRate();
    double getAvgUsersPerDay();
    double getActiveUsersPercentage();

    // ── Team Stats ──
    int getTotalTeams();
    int getActiveTeams();
    int getInactiveTeams();
    int getTeamsToday();
    int getTeamsLast7Days();
    int getTeamsThisMonth();
    int getTeamsLastMonth();
    double getTeamGrowthRate();
    int getTotalMembers();
    int getActiveMembers();
    double getAvgMembersPerTeam();
    double getActiveTeamsPercentage();

    // ── Application Stats ──
    int getTotalApplications();
    int getPendingApplications();
    int getApprovedApplications();
    int getRejectedApplications();
    int getApplicationsToday();
    int getApplicationsLast7Days();
    int getApplicationsLast30Days();
    double getApprovalRate();
}