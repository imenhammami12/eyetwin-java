package com.eyetwin.interfaces;

import com.eyetwin.entities.Team;
import com.eyetwin.entities.TeamMembership;
import com.eyetwin.entities.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * ITeamService — contrat de la couche équipes.
 * Fusionne TeamDAO + TeamService Symfony.
 */
public interface ITeamService {

    // ── Index / Listing ────────────────────────────────────────
    List<Team> getOwnedTeams(int userId) throws SQLException;
    List<Team> getMemberTeams(int userId) throws SQLException;
    List<Team> getAllActiveTeams() throws SQLException;

    // ── Invitations / Demandes ─────────────────────────────────
    List<TeamMembership> getPendingInvitations(int userId) throws SQLException;
    List<TeamMembership> getUserPendingRequests(int userId) throws SQLException;

    // ── CRUD Team ──────────────────────────────────────────────
    Team createTeam(Team team, int ownerId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException;
    Team createTeam(Team team, int ownerId) throws SQLException, IOException;

    Team getTeamWithDetails(int teamId) throws SQLException;

    void updateTeam(Team team, int currentUserId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException;
    void updateTeam(Team team, int currentUserId) throws SQLException, IOException;

    void deleteTeam(int teamId, int currentUserId) throws SQLException, IOException;

    void toggleActive(int teamId, boolean isActive, int currentUserId)
            throws SQLException, IOException;

    // ── Membres ────────────────────────────────────────────────
    List<TeamMembership> getActiveMembers(int teamId) throws SQLException;
    List<TeamMembership> getPendingRequests(int teamId) throws SQLException;
    int countActiveMembers(int teamId) throws SQLException;
    int countPendingRequests(int teamId) throws SQLException;
    boolean hasPendingRequest(int teamId, int userId) throws SQLException;

    // ── Invitations ────────────────────────────────────────────
    void inviteUser(int teamId, int targetUserId, int currentUserId) throws SQLException;
    void acceptInvitation(int membershipId, int currentUserId) throws SQLException;
    void declineInvitation(int membershipId, int currentUserId) throws SQLException;

    // ── Demandes rejoindre ─────────────────────────────────────
    void requestJoin(int teamId, int userId) throws SQLException;
    void cancelRequest(int membershipId, int currentUserId) throws SQLException;

    // ── Gestion owner ──────────────────────────────────────────
    void acceptRequest(int membershipId, int currentUserId) throws SQLException;
    void rejectRequest(int membershipId, int currentUserId) throws SQLException;
    void removeMember(int teamId, int membershipId, int currentUserId) throws SQLException;

    // ── Quitter ────────────────────────────────────────────────
    void leaveTeam(int teamId, int userId) throws SQLException;

    // ── Recherche utilisateurs ─────────────────────────────────
    List<User> searchUsers(String query) throws SQLException;

    // ── Compteurs (pour UserProfile) ───────────────────────────
    int countOwnedTeams(int userId) throws SQLException;
    int countMemberTeams(int userId) throws SQLException;
    int countUnreadNotifications(int userId);
}