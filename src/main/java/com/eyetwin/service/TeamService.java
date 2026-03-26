package com.eyetwin.service;

import com.eyetwin.dao.TeamDAO;
import com.eyetwin.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * TeamService — logique métier des teams.
 * Gère aussi l'upload de logo (stocké dans uploads/teams/).
 */
public class TeamService {

    private final TeamDAO dao = new TeamDAO();

    // Dossier où sont stockés les logos (même chemin que Symfony)
    // Adapter ce chemin selon ton environnement
    private static final String UPLOAD_DIR = "uploads/teams/";

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
     * @param team      objet Team avec name/desc/max/active déjà set
     * @param ownerId   id du user créateur
     * @param logoBytes bytes du fichier logo (null = pas de logo)
     * @param logoExt   extension du fichier (ex: "png", "jpg") — ignoré si logoBytes null
     */
    public Team createTeam(Team team, int ownerId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException {

        // Sauvegarder le logo si fourni
        if (logoBytes != null && logoBytes.length > 0 && logoExt != null) {
            String filename = saveLogo(logoBytes, logoExt);
            team.setLogo(filename);
        }

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

    /** Surcharge sans logo pour compatibilité */
    public Team createTeam(Team team, int ownerId) throws SQLException, IOException {
        return createTeam(team, ownerId, null, null);
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

    /**
     * Met à jour une team. Si logoBytes != null, remplace l'ancien logo.
     */
    public void updateTeam(Team team, int currentUserId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException {
        Team existing = dao.findById(team.getId());
        if (existing == null) throw new IllegalArgumentException("Team not found");
        if (existing.getOwnerId() != currentUserId)
            throw new SecurityException("You are not authorized to edit this team");

        // Remplacer le logo si un nouveau est fourni
        if (logoBytes != null && logoBytes.length > 0 && logoExt != null) {
            // Supprimer l'ancien logo si existant
            if (existing.getLogo() != null) deleteLogo(existing.getLogo());
            String filename = saveLogo(logoBytes, logoExt);
            team.setLogo(filename);
        } else {
            // Garder l'ancien logo
            team.setLogo(existing.getLogo());
        }

        dao.updateTeam(team);
    }

    /** Surcharge sans logo pour compatibilité */
    public void updateTeam(Team team, int currentUserId) throws SQLException, IOException {
        updateTeam(team, currentUserId, null, null);
    }

    // ─── DELETE ──────────────────────────────────────────────────

    /**
     * Supprime une team et tous ses memberships.
     * Seul l'owner peut supprimer sa team.
     * Supprime aussi le logo du disque si présent.
     */
    public void deleteTeam(int teamId, int currentUserId) throws SQLException, IOException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can delete this team");

        // Supprimer le logo du disque
        if (team.getLogo() != null) deleteLogo(team.getLogo());

        dao.deleteTeam(teamId);
    }

    // ─── TOGGLE ACTIVE ───────────────────────────────────────────

    /**
     * Active ou désactive une team.
     * Seul l'owner peut le faire.
     */
    public void toggleActive(int teamId, boolean isActive, int currentUserId)
            throws SQLException, IOException {
        Team team = dao.findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can change team status");
        team.setActive(isActive);
        dao.updateTeam(team);
    }

    // ─── INVITE ──────────────────────────────────────────────────

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

    // ─── LOGO UTILS ──────────────────────────────────────────────

    /**
     * Sauvegarde un logo sur le disque.
     * @return le nom du fichier sauvegardé (ex: "logo_UUID.png")
     */
    private String saveLogo(byte[] bytes, String ext) throws IOException {
        // Créer le dossier si besoin
        Path dir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        // Nom unique
        String filename = "logo_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dest = dir.resolve(filename);
        Files.write(dest, bytes);
        System.out.println("✅ Logo sauvegardé : " + dest.toAbsolutePath());
        return filename;
    }

    /** Supprime un logo du disque (silencieux si absent) */
    private void deleteLogo(String filename) {
        try {
            Path path = Paths.get(UPLOAD_DIR, filename);
            Files.deleteIfExists(path);
            System.out.println("🗑 Logo supprimé : " + filename);
        } catch (IOException e) {
            System.err.println("⚠ Impossible de supprimer le logo : " + filename);
        }
    }
}