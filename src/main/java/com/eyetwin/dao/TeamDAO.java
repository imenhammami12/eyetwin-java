package com.eyetwin.dao;

import com.eyetwin.config.DatabaseConfig;
import com.eyetwin.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO Teams — traduit exactement les méthodes du TeamRepository
 * et TeamMembershipRepository Symfony en JDBC pur.
 */
public class TeamDAO {

    // ════════════════════════════════════════════════════════════
    //  TEAM CRUD
    // ════════════════════════════════════════════════════════════

    /** Créer une équipe (INSERT) — retourne l'id généré */
    public int createTeam(Team team) throws SQLException {
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

    /** Mettre à jour une équipe (name, description, logo, max_members, is_active) */
    public void updateTeam(Team team) throws SQLException {
        String sql = """
            UPDATE team SET name=?, description=?, logo=?, max_members=?, is_active=?
            WHERE id=?
            """;
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

    /**
     * Supprimer une équipe et tous ses memberships (CASCADE).
     */
    public void deleteTeam(int teamId) throws SQLException {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM team_membership WHERE team_id = ?")) {
                    ps.setInt(1, teamId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM team WHERE id = ?")) {
                    ps.setInt(1, teamId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Trouver une équipe par id avec hydratation du owner */
    public Team findById(int id) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username, u.email AS owner_email
            FROM team t
            INNER JOIN user u ON u.id = t.owner_id
            WHERE t.id = ?
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

    /** Toutes les équipes actives avec leurs memberships */
    public List<Team> findAllActiveWithMembers() throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t
            INNER JOIN user u ON u.id = t.owner_id
            WHERE t.is_active = 1
            ORDER BY t.created_at DESC
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

    /** Équipes dont l'utilisateur est owner */
    public List<Team> findByOwner(int ownerId) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t
            INNER JOIN user u ON u.id = t.owner_id
            WHERE t.owner_id = ?
            ORDER BY t.created_at DESC
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

    /**
     * ✅ ALIAS de findByOwner — utilisé par UserProfileController.
     * Miroir de user.getOwnedTeams() Symfony.
     */
    public List<Team> getOwnedTeams(int userId) throws SQLException {
        return findByOwner(userId);
    }

    /** Équipes où l'utilisateur est membre ACTIVE (pas owner) */
    public List<Team> findTeamsByMember(int userId) throws SQLException {
        String sql = """
            SELECT t.*, u.username AS owner_username
            FROM team t
            INNER JOIN team_membership tm ON tm.team_id = t.id
            INNER JOIN user u ON u.id = t.owner_id
            WHERE tm.user_id = ? AND tm.status = 'ACTIVE'
              AND t.owner_id != ?
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

    // ════════════════════════════════════════════════════════════
    //  ✅ MÉTHODES COUNT — ajoutées pour UserProfileController
    //     Miroir de user.getTeamMemberships().count() Symfony
    // ════════════════════════════════════════════════════════════

    /**
     * Compte le nombre d'équipes dont l'user est owner.
     * Miroir : user.getOwnedTeams().count()
     */
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

    /**
     * Compte le nombre d'équipes dont l'user est membre ACTIVE.
     * Inclut les équipes dont il est owner (comme Symfony getTeamMemberships().count()).
     * Miroir : user.getTeamMemberships().count()
     */
    public int countMemberTeams(int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM team_membership
            WHERE user_id = ? AND status = 'ACTIVE'
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Compte les notifications non lues d'un user.
     * Miroir : user.getNotifications().filter(fn($n) => !$n->isRead())->count()
     * Retourne 0 si la table notification n'existe pas encore.
     */
    public int countUnreadNotifications(int userId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            // Table notification peut ne pas exister encore
            System.err.println("[TeamDAO] countUnreadNotifications: " + e.getMessage());
            return 0;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MEMBERSHIP CRUD
    // ════════════════════════════════════════════════════════════

    /** Créer un membership (invitation, demande, owner) */
    public int createMembership(TeamMembership m) throws SQLException {
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

    /** Mettre à jour le status + joinedAt d'un membership */
    public void updateMembership(TeamMembership m) throws SQLException {
        String sql = "UPDATE team_membership SET status=?, joined_at=? WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getStatus().name());
            ps.setTimestamp(2, m.getJoinedAt() != null ? Timestamp.valueOf(m.getJoinedAt()) : null);
            ps.setInt(3, m.getId());
            ps.executeUpdate();
        }
    }

    /** Supprimer un membership */
    public void deleteMembership(int membershipId) throws SQLException {
        String sql = "DELETE FROM team_membership WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, membershipId);
            ps.executeUpdate();
        }
    }

    /** Trouver un membership par id */
    public TeamMembership findMembershipById(int id) throws SQLException {
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

    /** Tous les memberships d'une team */
    public List<TeamMembership> findMembershipsByTeamId(int teamId) throws SQLException {
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

    /** Membres ACTIVE d'une team */
    public List<TeamMembership> findActiveMembers(int teamId) throws SQLException {
        String sql = """
            SELECT tm.*, u.username, u.email, u.profile_picture
            FROM team_membership tm
            INNER JOIN user u ON u.id = tm.user_id
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

    /** Demandes PENDING d'une team */
    public List<TeamMembership> findPendingRequests(int teamId) throws SQLException {
        String sql = """
            SELECT tm.*, u.username, u.email, u.profile_picture
            FROM team_membership tm
            INNER JOIN user u ON u.id = tm.user_id
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

    /** Invitations PENDING pour un user */
    public List<TeamMembership> findPendingInvitations(int userId) throws SQLException {
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

    /** Demandes PENDING envoyées par un user */
    public List<TeamMembership> findUserPendingRequests(int userId) throws SQLException {
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

    /** Vérifier si un user est déjà membre ou invité */
    public boolean isMemberOrInvited(int teamId, int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM team_membership
            WHERE team_id = ? AND user_id = ?
              AND status IN ('ACTIVE', 'INVITED', 'PENDING')
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Vérifier si un user a une demande PENDING */
    public boolean hasPendingRequest(int teamId, int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM team_membership
            WHERE team_id = ? AND user_id = ? AND status = 'PENDING'
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, teamId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Compter les membres ACTIVE */
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

    /** Compter les demandes PENDING */
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

    /** Rechercher des users pour invitation */
    public List<User> searchUsersForInvitation(String query) throws SQLException {
        String sql = """
            SELECT id, username, email, roles_json
            FROM user
            WHERE (username LIKE ? OR email LIKE ?)
            LIMIT 10
            """;
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
    //  MAPPING ResultSet → Objets
    // ════════════════════════════════════════════════════════════

    private Team mapTeam(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Team(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("logo"),
                createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now(),
                rs.getInt("max_members"),
                rs.getBoolean("is_active"),
                rs.getInt("owner_id")
        );
    }

    private TeamMembership mapMembership(ResultSet rs) throws SQLException {
        Timestamp invitedAt = rs.getTimestamp("invited_at");
        Timestamp joinedAt  = rs.getTimestamp("joined_at");
        return new TeamMembership(
                rs.getInt("id"),
                rs.getInt("team_id"),
                rs.getInt("user_id"),
                MemberRole.fromValue(rs.getString("role")),
                MembershipStatus.fromValue(rs.getString("status")),
                invitedAt != null ? invitedAt.toLocalDateTime() : null,
                joinedAt  != null ? joinedAt.toLocalDateTime()  : null
        );
    }
}