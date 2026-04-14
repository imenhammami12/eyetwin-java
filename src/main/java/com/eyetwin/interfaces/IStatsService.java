package com.eyetwin.interfaces;

import java.util.List;

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

    // ── Channel Stats ──
    int getTotalChannels();
    int getApprovedChannels();
    int getPendingChannels();
    int getRejectedChannels();
    int getPublicChannels();
    int getPrivateChannels();
    int getChannelsToday();
    int getChannelsLast7Days();
    int getChannelsThisMonth();
    int getChannelsLastMonth();
    double getChannelGrowthRate();
    List<Integer> getChannelsLast7DaysChart();

    // Dans IStatsService — ajoute ces méthodes
    List<Integer> getUsersLast7DaysChart();   // 1 valeur par jour
    List<Integer> getTeamsLast7DaysChart();
    List<Integer> getAppsLast7DaysChart();
    List<String>  getLast7DaysLabels();

}