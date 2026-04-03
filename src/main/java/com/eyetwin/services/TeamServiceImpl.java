package com.eyetwin.services;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.ITeamService;
import com.eyetwin.tools.DatabaseConfig;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TeamServiceImpl — implémentation de ITeamService.
 *
 * Fusionne l'ancien TeamDAO (accès SQL) + TeamService (logique métier).
 * Gère aussi l'upload de logo (uploads/teams/).
 */
public class TeamServiceImpl implements ITeamService {

    private static final String UPLOAD_DIR = "uploads/teams/";

    // ════════════════════════════════════════════════════════════
    //  INDEX / LISTING
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Team> getOwnedTeams(int userId) throws SQLException {
        return findByOwner(userId);
    }

    @Override
    public List<Team> getMemberTeams(int userId) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t
            INNER JOIN team_membership tm ON tm.team_id = t.id
            INNER JOIN user u ON u.id = t.owner_id
            WHERE tm.user_id = ? AND tm.status = 'ACTIVE' AND t.owner_id != ?
            ORDER BY t.created_at DESC
            """;
        List<Team> teams = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team t = mapTeam(rs);
                    User owner = new User();
                    owner.setId(t.getOwnerId());
                    owner.setUsername(rs.getString("owner_username"));
                    t.setOwner(owner);
                    t.setTeamMemberships(findMembershipsByTeamId(t.getId()));
                    teams.add(t);
                }
            }
        }
        return teams;
    }

    @Override
    public List<Team> getAllActiveTeams() throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t INNER JOIN user u ON u.id = t.owner_id
            WHERE t.is_active = 1 ORDER BY t.created_at DESC
            """;
        List<Team> teams = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Team t = mapTeam(rs);
                User owner = new User();
                owner.setId(t.getOwnerId());
                owner.setUsername(rs.getString("owner_username"));
                t.setOwner(owner);
                t.setTeamMemberships(findMembershipsByTeamId(t.getId()));
                teams.add(t);
            }
        }
        return teams;
    }

    @Override
    public List<TeamMembership> getPendingInvitations(int userId) throws SQLException {
        String sql = """
            SELECT tm.*, t.name AS team_name, u.username AS owner_username
            FROM team_membership tm
            INNER JOIN team t ON t.id = tm.team_id
            INNER JOIN user u ON u.id = t.owner_id
            WHERE tm.user_id = ? AND tm.status = 'INVITED'
            ORDER BY tm.invited_at DESC
            """;
        List<TeamMembership> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamMembership m = mapMembership(rs);
                    Team t = new Team();
                    t.setId(m.getTeamId());
                    t.setName(rs.getString("team_name"));
                    User owner = new User();
                    owner.setUsername(rs.getString("owner_username"));
                    t.setOwner(owner);
                    m.setTeam(t);
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public List<TeamMembership> getUserPendingRequests(int userId) throws SQLException {
        String sql = """
            SELECT tm.*, t.name AS team_name, u.username AS owner_username
            FROM team_membership tm
            INNER JOIN team t ON t.id = tm.team_id
            INNER JOIN user u ON u.id = t.owner_id
            WHERE tm.user_id = ? AND tm.status = 'PENDING'
            ORDER BY tm.invited_at DESC
            """;
        List<TeamMembership> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamMembership m = mapMembership(rs);
                    Team t = new Team();
                    t.setId(m.getTeamId());
                    t.setName(rs.getString("team_name"));
                    User owner = new User();
                    owner.setUsername(rs.getString("owner_username"));
                    t.setOwner(owner);
                    m.setTeam(t);
                    list.add(m);
                }
            }
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  CRUD TEAM
    // ════════════════════════════════════════════════════════════

    @Override
    public Team createTeam(Team team, int ownerId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException {
        if (logoBytes != null && logoBytes.length > 0 && logoExt != null) {
            team.setLogo(saveLogo(logoBytes, logoExt));
        }
        team.setOwnerId(ownerId);
        team.setActive(true);
        insertTeam(team);

        TeamMembership ownerMembership = new TeamMembership();
        ownerMembership.setTeamId(team.getId());
        ownerMembership.setUserId(ownerId);
        ownerMembership.setRole(MemberRole.OWNER);
        ownerMembership.setStatus(MembershipStatus.ACTIVE);
        ownerMembership.accept();
        insertMembership(ownerMembership);

        return team;
    }

    @Override
    public Team createTeam(Team team, int ownerId) throws SQLException, IOException {
        return createTeam(team, ownerId, null, null);
    }

    @Override
    public Team getTeamWithDetails(int teamId) throws SQLException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found: " + teamId);
        team.setTeamMemberships(findMembershipsByTeamId(teamId));
        return team;
    }

    @Override
    public void updateTeam(Team team, int currentUserId, byte[] logoBytes, String logoExt)
            throws SQLException, IOException {
        Team existing = findById(team.getId());
        if (existing == null) throw new IllegalArgumentException("Team not found");
        if (existing.getOwnerId() != currentUserId)
            throw new SecurityException("You are not authorized to edit this team");

        if (logoBytes != null && logoBytes.length > 0 && logoExt != null) {
            if (existing.getLogo() != null) deleteLogo(existing.getLogo());
            team.setLogo(saveLogo(logoBytes, logoExt));
        } else {
            team.setLogo(existing.getLogo());
        }
        updateTeamInDb(team);
    }

    @Override
    public void updateTeam(Team team, int currentUserId) throws SQLException, IOException {
        updateTeam(team, currentUserId, null, null);
    }

    @Override
    public void deleteTeam(int teamId, int currentUserId) throws SQLException, IOException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can delete this team");
        if (team.getLogo() != null) deleteLogo(team.getLogo());
        deleteTeamCascade(teamId);
    }

    @Override
    public void toggleActive(int teamId, boolean isActive, int currentUserId)
            throws SQLException, IOException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can change team status");
        team.setActive(isActive);
        updateTeamInDb(team);
    }

    // ════════════════════════════════════════════════════════════
    //  MEMBRES
    // ════════════════════════════════════════════════════════════

    @Override
    public List<TeamMembership> getActiveMembers(int teamId) throws SQLException {
        String sql = """
            SELECT tm.*, u.username, u.email, u.profile_picture
            FROM team_membership tm INNER JOIN user u ON u.id = tm.user_id
            WHERE tm.team_id = ? AND tm.status = 'ACTIVE'
            ORDER BY tm.role = 'OWNER' DESC, tm.joined_at ASC
            """;
        List<TeamMembership> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamMembership m = mapMembership(rs);
                    User u = new User();
                    u.setId(m.getUserId());
                    u.setUsername(rs.getString("username"));
                    u.setEmail(rs.getString("email"));
                    m.setUser(u);
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public List<TeamMembership> getPendingRequests(int teamId) throws SQLException {
        String sql = """
            SELECT tm.*, u.username, u.email
            FROM team_membership tm INNER JOIN user u ON u.id = tm.user_id
            WHERE tm.team_id = ? AND tm.status = 'PENDING'
            ORDER BY tm.invited_at ASC
            """;
        List<TeamMembership> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamMembership m = mapMembership(rs);
                    User u = new User();
                    u.setId(m.getUserId());
                    u.setUsername(rs.getString("username"));
                    u.setEmail(rs.getString("email"));
                    m.setUser(u);
                    list.add(m);
                }
            }
        }
        return list;
    }

    @Override
    public int countActiveMembers(int teamId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM team_membership WHERE team_id = ? AND status = 'ACTIVE'";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int countPendingRequests(int teamId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM team_membership WHERE team_id = ? AND status = 'PENDING'";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean hasPendingRequest(int teamId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM team_membership WHERE team_id=? AND user_id=? AND status='PENDING'";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  INVITATIONS
    // ════════════════════════════════════════════════════════════

    @Override
    public void inviteUser(int teamId, int targetUserId, int currentUserId) throws SQLException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can invite members");
        if (isMemberOrInvited(teamId, targetUserId))
            throw new IllegalStateException("This user is already a member or has been invited");
        if (countActiveMembers(teamId) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached the maximum number of members");

        TeamMembership m = new TeamMembership();
        m.setTeamId(teamId);
        m.setUserId(targetUserId);
        m.setRole(MemberRole.MEMBER);
        m.setStatus(MembershipStatus.INVITED);
        insertMembership(m);
    }

    @Override
    public void acceptInvitation(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId) throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.INVITED)
            throw new IllegalStateException("This invitation is no longer valid");
        m.accept();
        updateMembership(m);
    }

    @Override
    public void declineInvitation(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId) throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.INVITED)
            throw new IllegalStateException("This invitation is no longer valid");
        m.decline();
        updateMembership(m);
    }

    // ════════════════════════════════════════════════════════════
    //  DEMANDES REJOINDRE
    // ════════════════════════════════════════════════════════════

    @Override
    public void requestJoin(int teamId, int userId) throws SQLException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (!team.isActive()) throw new IllegalStateException("This team is not active");
        if (isMemberOrInvited(teamId, userId))
            throw new IllegalStateException("You already have a pending request or are already a member");
        if (countActiveMembers(teamId) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached the maximum number of members");

        TeamMembership m = new TeamMembership();
        m.setTeamId(teamId);
        m.setUserId(userId);
        m.setRole(MemberRole.MEMBER);
        m.setStatus(MembershipStatus.PENDING);
        insertMembership(m);
    }

    @Override
    public void cancelRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        if (m.getUserId() != currentUserId) throw new SecurityException("Access denied");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        deleteMembership(membershipId);
    }

    // ════════════════════════════════════════════════════════════
    //  GESTION OWNER
    // ════════════════════════════════════════════════════════════

    @Override
    public void acceptRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        Team team = findById(m.getTeamId());
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can accept requests");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        if (countActiveMembers(m.getTeamId()) >= team.getMaxMembers())
            throw new IllegalStateException("The team has reached maximum capacity");
        m.accept();
        updateMembership(m);
    }

    @Override
    public void rejectRequest(int membershipId, int currentUserId) throws SQLException {
        TeamMembership m = findMembershipById(membershipId);
        if (m == null) throw new IllegalArgumentException("Membership not found");
        Team team = findById(m.getTeamId());
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can reject requests");
        if (m.getStatus() != MembershipStatus.PENDING)
            throw new IllegalStateException("This request is no longer pending");
        deleteMembership(membershipId);
    }

    @Override
    public void removeMember(int teamId, int membershipId, int currentUserId) throws SQLException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() != currentUserId)
            throw new SecurityException("Only the owner can remove members");
        TeamMembership m = findMembershipById(membershipId);
        if (m == null || m.getTeamId() != teamId)
            throw new IllegalArgumentException("Membership not found");
        if (m.getRole() == MemberRole.OWNER)
            throw new IllegalStateException("Cannot remove the owner");
        deleteMembership(membershipId);
    }

    @Override
    public void leaveTeam(int teamId, int userId) throws SQLException {
        Team team = findById(teamId);
        if (team == null) throw new IllegalArgumentException("Team not found");
        if (team.getOwnerId() == userId)
            throw new IllegalStateException("Owner cannot leave. Transfer ownership first.");
        for (TeamMembership m : findMembershipsByTeamId(teamId)) {
            if (m.getUserId() == userId && m.getStatus() == MembershipStatus.ACTIVE) {
                m.setStatus(MembershipStatus.LEFT);
                updateMembership(m);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  RECHERCHE UTILISATEURS
    // ════════════════════════════════════════════════════════════

    @Override
    public List<User> searchUsers(String query) throws SQLException {
        if (query == null || query.length() < 2)
            throw new IllegalArgumentException("Query too short");
        String sql = "SELECT id, username, email FROM user WHERE (username LIKE ? OR email LIKE ?) LIMIT 10";
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setEmail(rs.getString("email"));
                    users.add(u);
                }
            }
        }
        return users;
    }

    // ════════════════════════════════════════════════════════════
    //  COMPTEURS (pour UserProfile)
    // ════════════════════════════════════════════════════════════

    @Override
    public int countOwnedTeams(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM team WHERE owner_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int countMemberTeams(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM team_membership WHERE user_id = ? AND status = 'ACTIVE'";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public int countUnreadNotifications(int userId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("[TeamService] countUnreadNotifications: " + e.getMessage());
            return 0;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MÉTHODES SQL PRIVÉES (ex-DAO)
    // ════════════════════════════════════════════════════════════

    private int insertTeam(Team team) throws SQLException {
        String sql = """
            INSERT INTO team (name, description, logo, created_at, max_members, is_active, owner_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, team.getName());
            ps.setString(2, team.getDescription());
            ps.setString(3, team.getLogo());
            ps.setTimestamp(4, Timestamp.valueOf(
                    team.getCreatedAt() != null ? team.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(5, team.getMaxMembers());
            ps.setBoolean(6, team.isActive());
            ps.setInt(7, team.getOwnerId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { int id = rs.getInt(1); team.setId(id); return id; }
            }
        }
        return -1;
    }

    private void updateTeamInDb(Team team) throws SQLException {
        String sql = "UPDATE team SET name=?, description=?, logo=?, max_members=?, is_active=? WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, team.getName());
            ps.setString(2, team.getDescription());
            ps.setString(3, team.getLogo());
            ps.setInt(4, team.getMaxMembers());
            ps.setBoolean(5, team.isActive());
            ps.setInt(6, team.getId());
            ps.executeUpdate();
        }
    }

    private void deleteTeamCascade(int teamId) throws SQLException {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM team_membership WHERE team_id = ?")) {
                    ps.setInt(1, teamId); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM team WHERE id = ?")) {
                    ps.setInt(1, teamId); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private Team findById(int id) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username, u.email AS owner_email
            FROM team t INNER JOIN user u ON u.id = t.owner_id WHERE t.id = ?
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Team team = mapTeam(rs);
                    User owner = new User();
                    owner.setId(team.getOwnerId());
                    owner.setUsername(rs.getString("owner_username"));
                    owner.setEmail(rs.getString("owner_email"));
                    team.setOwner(owner);
                    return team;
                }
            }
        }
        return null;
    }

    private List<Team> findByOwner(int ownerId) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t INNER JOIN user u ON u.id = t.owner_id
            WHERE t.owner_id = ? ORDER BY t.created_at DESC
            """;
        List<Team> teams = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team t = mapTeam(rs);
                    User owner = new User();
                    owner.setId(t.getOwnerId());
                    owner.setUsername(rs.getString("owner_username"));
                    t.setOwner(owner);
                    t.setTeamMemberships(findMembershipsByTeamId(t.getId()));
                    teams.add(t);
                }
            }
        }
        return teams;
    }

    private int insertMembership(TeamMembership m) throws SQLException {
        String sql = """
            INSERT INTO team_membership (team_id, user_id, role, status, invited_at, joined_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getTeamId());
            ps.setInt(2, m.getUserId());
            ps.setString(3, m.getRole().name());
            ps.setString(4, m.getStatus().name());
            ps.setTimestamp(5, m.getInvitedAt() != null ? Timestamp.valueOf(m.getInvitedAt()) : null);
            ps.setTimestamp(6, m.getJoinedAt()  != null ? Timestamp.valueOf(m.getJoinedAt())  : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { int id = rs.getInt(1); m.setId(id); return id; }
            }
        }
        return -1;
    }

    private void updateMembership(TeamMembership m) throws SQLException {
        String sql = "UPDATE team_membership SET status=?, joined_at=? WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getStatus().name());
            ps.setTimestamp(2, m.getJoinedAt() != null ? Timestamp.valueOf(m.getJoinedAt()) : null);
            ps.setInt(3, m.getId());
            ps.executeUpdate();
        }
    }

    private void deleteMembership(int membershipId) throws SQLException {
        String sql = "DELETE FROM team_membership WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, membershipId);
            ps.executeUpdate();
        }
    }

    private TeamMembership findMembershipById(int id) throws SQLException {
        String sql = "SELECT * FROM team_membership WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMembership(rs);
            }
        }
        return null;
    }

    private List<TeamMembership> findMembershipsByTeamId(int teamId) throws SQLException {
        String sql = "SELECT * FROM team_membership WHERE team_id = ?";
        List<TeamMembership> list = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMembership(rs));
            }
        }
        return list;
    }

    private boolean isMemberOrInvited(int teamId, int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM team_membership
            WHERE team_id = ? AND user_id = ? AND status IN ('ACTIVE','INVITED','PENDING')
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LOGO UTILS
    // ════════════════════════════════════════════════════════════

    private String saveLogo(byte[] bytes, String ext) throws IOException {
        Path dir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String filename = "logo_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Files.write(dir.resolve(filename), bytes);
        return filename;
    }

    private void deleteLogo(String filename) {
        try { Files.deleteIfExists(Paths.get(UPLOAD_DIR, filename)); }
        catch (IOException e) { System.err.println("⚠ Cannot delete logo: " + filename); }
    }

    // ════════════════════════════════════════════════════════════
    //  MAPPING ResultSet → Entités
    // ════════════════════════════════════════════════════════════

    private Team mapTeam(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Team(
                rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                rs.getString("logo"),
                createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now(),
                rs.getInt("max_members"), rs.getBoolean("is_active"), rs.getInt("owner_id"));
    }

    private TeamMembership mapMembership(ResultSet rs) throws SQLException {
        Timestamp invitedAt = rs.getTimestamp("invited_at");
        Timestamp joinedAt  = rs.getTimestamp("joined_at");
        return new TeamMembership(
                rs.getInt("id"), rs.getInt("team_id"), rs.getInt("user_id"),
                MemberRole.fromValue(rs.getString("role")),
                MembershipStatus.fromValue(rs.getString("status")),
                invitedAt != null ? invitedAt.toLocalDateTime() : null,
                joinedAt  != null ? joinedAt.toLocalDateTime()  : null);
    }
}
