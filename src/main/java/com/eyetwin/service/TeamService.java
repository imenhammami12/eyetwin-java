package com.eyetwin.service;

import com.eyetwin.dao.TeamDAO;
import com.eyetwin.model.*;

import java.sql.SQLException;
import java.util.List;

/**
 * TeamService — contient toute la logique métier des teams.
 * Traduit exactement les actions du TeamController Symfony.
 */
public class TeamService {

    private final TeamDAO dao = new TeamDAO();

    // ─── INDEX ───────────────────────────────────────────────────

    public List<Team> getOwnedTeams(int userId) throws SQLException {
        return dao.findByOwner(userId);
    }

    public List<Team> getMemberTeams(int userId) throws SQLException {
        return dao.findTeamsByMember(userId);
    }

    public List<TeamMembership> getPendingInvitations(int userId) throws SQLException {
        return dao.findPendingInvitations(userId);
    }

    public List<TeamMembership> getUserPendingRequests(int userId) throws SQLException {
        return dao.findUserPendingRequests(userId);
    }

    public List<Team> getAllActiveTeams() throws SQLException {
        return dao.findAllActiveWithMembers();
    }

    // ─── CREATE ──────────────────────────────────────────────────

    /**
     * Crée une équipe + ajoute le créateur comme OWNER ACTIVE.
     * Identique au TeamController::create() Symfony.
     */
    public Team createTeam(Team team, int ownerId) throws SQLException {
        team.setOwnerId(ownerId);
        team.setActive(true);
        dao.createTeam(team);

        // Créer le membership OWNER
        TeamMembership ownerMembership = new TeamMembership();
        ownerMembership.setTeamId(team.getId());
        ownerMembership.setUserId(ownerId);
        ownerMembership.setRole(MemberRole.OWNER);
        ownerMembership.setStatus(MembershipStatus.ACTIVE);
        ownerMembership.accept(); // fixe joinedAt = now
        dao.createMembership(ownerMembership);

        return team;
    }

    // ─── SHOW ────────────────────────────────────────────────────

    public Team getTeamWithDetails(int teamId) throws SQLException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found: " + teamId);
        team.setTeamMemberships(dao.findMembershipsByTeamId(teamId));
        return team;
    }

    public List<TeamMembership> getActiveMembers(int teamId) throws SQLException {
        return dao.findActiveMembers(teamId);
    }

    public List<TeamMembership> getPendingRequests(int teamId) throws SQLException {
        return dao.findPendingRequests(teamId);
    }

    public int countActiveMembers(int teamId) throws SQLException {
        return dao.countActiveMembers(teamId);
    }

    public int countPendingRequests(int teamId) throws SQLException {
        return dao.countPendingRequests(teamId);
    }

    public boolean hasPendingRequest(int teamId, int userId) throws SQLException {
        return dao.hasPendingRequest(teamId, userId);
    }

    // ─── EDIT ────────────────────────────────────────────────────

    public void updateTeam(Team team, int currentUserId) throws SQLException {
        Team existing = dao.findById(team.getId());
        if (existing == null) throw new IllegalArgumentException("Team not found");
        if (existing.getOwnerId() != currentUserId)
            throw new SecurityException("You are not authorized to edit this team");
        dao.updateTeam(team);
    }

    // ─── INVITE ──────────────────────────────────────────────────

    /**
     * Envoyer une invitation à un user.
     * Vérifie : owner, pas déjà membre, place disponible.
     */
    public void inviteUser(int teamId, int targetUserId, int currentUserId) throws SQLException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can invite members");
        if (dao.isMemberOrInvited(teamId, targetUserId))
            throw new IllegalStateException("This user is already a member or has been invited");
        if (dao.countActiveMembers(teamId) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached the maximum number of members");

        TeamMembership m = new TeamMembership();
        m.setTeamId(teamId);
        m.setUserId(targetUserId);
        m.setRole(MemberRole.MEMBER);
        m.setStatus(MembershipStatus.INVITED);
        dao.createMembership(m);
    }

    // ─── ACCEPT / DECLINE INVITATION ─────────────────────────────

    public void acceptInvitation(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId)
            throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.INVITED)
            throw new IllegalStateException("This invitation is no longer valid");
        m.accept();
        dao.updateMembership(m);
    }

    public void declineInvitation(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId)
            throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.INVITED)
            throw new IllegalStateException("This invitation is no longer valid");
        m.decline();
        dao.updateMembership(m);
    }

    // ─── REQUEST JOIN ─────────────────────────────────────────────

    public void requestJoin(int teamId, int userId) throws SQLException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (!team.isActive())
            throw new IllegalStateException("This team is not active");
        if (dao.isMemberOrInvited(teamId, userId))
            throw new IllegalStateException("You already have a pending request or are already a member");
        if (dao.countActiveMembers(teamId) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached the maximum number of members");

        TeamMembership m = new TeamMembership();
        m.setTeamId(teamId);
        m.setUserId(userId);
        m.setRole(MemberRole.MEMBER);
        m.setStatus(MembershipStatus.PENDING);
        dao.createMembership(m);
    }

    public void cancelRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId)
            throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        dao.deleteMembership(membershipId);
    }

    // ─── ACCEPT / REJECT REQUEST (owner) ─────────────────────────

    public void acceptRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        Team team = dao.findById(m.getTeamId());
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can accept requests");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        if (dao.countActiveMembers(m.getTeamId()) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached maximum capacity");
        m.accept();
        dao.updateMembership(m);
    }

    public void rejectRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        Team team = dao.findById(m.getTeamId());
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can reject requests");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        dao.deleteMembership(membershipId);
    }

    // ─── LEAVE ───────────────────────────────────────────────────

    public void leaveTeam(int teamId, int userId) throws SQLException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() == userId)
            throw new IllegalStateException("Owner cannot leave the team. Transfer ownership first.");

        List<TeamMembership> memberships = dao.findMembershipsByTeamId(teamId);
        for (TeamMembership m : memberships) {
            if (m.getUserId() == userId && m.getStatus() == MembershipStatus.ACTIVE) {
                m.setStatus(MembershipStatus.LEFT);
                dao.updateMembership(m);
                return;
            }
        }
    }

    // ─── REMOVE MEMBER (owner) ────────────────────────────────────

    public void removeMember(int teamId, int membershipId, int currentUserId) throws SQLException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can remove members");
        TeamMembership m = dao.findMembershipById(membershipId);
        if (m == null || m.getTeamId() != teamId)
            throw new IllegalArgumentException("Membership not found");
        if (m.getRole() == MemberRole.OWNER)
            throw new IllegalStateException("Cannot remove the owner");
        dao.deleteMembership(membershipId);
    }

    // ─── SEARCH USERS ─────────────────────────────────────────────

    public List<User> searchUsers(String query) throws SQLException {
        if (query == null || query.length() < 2)
            throw new IllegalArgumentException("Query too short");
        return dao.searchUsersForInvitation(query);
    }
}