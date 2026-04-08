package com.eyetwin.services;

import com.eyetwin.entities.ApplicationStatus;
import com.eyetwin.entities.CoachApplication;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoachApplicationService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CoachApplicationServiceImpl — implémentation de ICoachApplicationService.
 * Fusionne l'ancien CoachApplicationDAO (accès SQL) + logique métier.
 */
public class CoachApplicationServiceImpl implements ICoachApplicationService {

    // ═══════════════════════════════════════════════════════════
    //  LISTING
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<CoachApplication> getAllApplications() {
        String sql = """
            SELECT ca.id, ca.user_id, ca.status, ca.certifications, ca.experience,
                   ca.cv_file, ca.documents, ca.submitted_at, ca.reviewed_at, ca.review_comment,
                   u.username, u.email, u.full_name, u.profile_picture,
                   u.roles_json, u.account_status, u.created_at AS u_created_at, u.bio
            FROM coach_application ca
            INNER JOIN user u ON u.id = ca.user_id
            ORDER BY ca.submitted_at DESC
            """;
        List<CoachApplication> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapFull(rs));
        } catch (Exception e) {
            System.err.println("[CoachApplicationService] getAllApplications: " + e.getMessage());
        }
        return list;
    }

    @Override
    public CoachApplication findById(int id) {
        String sql = """
            SELECT ca.id, ca.user_id, ca.status, ca.certifications, ca.experience,
                   ca.cv_file, ca.documents, ca.submitted_at, ca.reviewed_at, ca.review_comment,
                   u.username, u.email, u.full_name, u.profile_picture,
                   u.roles_json, u.account_status, u.created_at AS u_created_at, u.bio
            FROM coach_application ca
            INNER JOIN user u ON u.id = ca.user_id
            WHERE ca.id = ?
            """;
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapFull(rs);
            }
        } catch (Exception e) {
            System.err.println("[CoachApplicationService] findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> getAllCoaches() {
        String sql = """
            SELECT id, username, email, full_name, profile_picture,
                   roles_json, account_status, created_at, bio, coin_balance
            FROM user
            WHERE roles_json LIKE '%ROLE_COACH%'
            ORDER BY created_at DESC
            """;
        List<User> coaches = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) coaches.add(mapUser(rs));
        } catch (Exception e) {
            System.err.println("[CoachApplicationService] getAllCoaches: " + e.getMessage());
        }
        return coaches;
    }

    // ═══════════════════════════════════════════════════════════
    //  HAS PENDING APPLICATION
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean hasPendingApplication(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coach_application WHERE user_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SAVE
    // ═══════════════════════════════════════════════════════════

    @Override
    public void save(CoachApplication app) throws SQLException {
        String sql = "INSERT INTO coach_application " +
                "(user_id, status, certifications, experience, cv_file, submitted_at) " +
                "VALUES (?, 'PENDING', ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, app.getUserId());
            ps.setString(2, app.getCertifications());
            ps.setString(3, app.getExperience());
            ps.setString(4, app.getCvFile());
            ps.setTimestamp(5, Timestamp.valueOf(
                    app.getSubmittedAt() != null ? app.getSubmittedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) app.setId(keys.getInt(1));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FIND LATEST BY USER ID
    // ═══════════════════════════════════════════════════════════

    @Override
    public CoachApplication findLatestByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM coach_application WHERE user_id = ? " +
                "ORDER BY submitted_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    //  APPROVE
    // ═══════════════════════════════════════════════════════════

    @Override
    public void approve(int applicationId, String comment, int adminUserId) {
        CoachApplication app = findById(applicationId);
        if (app == null) throw new IllegalArgumentException("Application not found: " + applicationId);
        if (!app.canBeReviewed()) throw new IllegalStateException("Application already reviewed");

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE coach_application SET status=?, reviewed_at=?, review_comment=? WHERE id=?")) {
                    ps.setString(1, ApplicationStatus.APPROVED.name());
                    ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(3, comment);
                    ps.setInt(4, applicationId);
                    ps.executeUpdate();
                }

                String currentRoles = app.getUser().getRolesJson();
                if (currentRoles == null || !currentRoles.contains("ROLE_COACH")) {
                    String newRoles = currentRoles != null
                            ? currentRoles.replace("]", ",\"ROLE_COACH\"]")
                            : "[\"ROLE_USER\",\"ROLE_COACH\"]";
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE user SET roles_json=? WHERE id=?")) {
                        ps.setString(1, newRoles);
                        ps.setInt(2, app.getUserId());
                        ps.executeUpdate();
                    }
                }

                con.commit();
                System.out.println("[CoachApplicationService] ✅ Approved application #" + applicationId);
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("approve failed: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  REJECT
    // ═══════════════════════════════════════════════════════════

    @Override
    public void reject(int applicationId, String comment, int adminUserId) {
        if (comment == null || comment.isBlank())
            throw new IllegalArgumentException("A comment is required to reject an application");

        CoachApplication app = findById(applicationId);
        if (app == null) throw new IllegalArgumentException("Application not found: " + applicationId);
        if (!app.canBeReviewed()) throw new IllegalStateException("Application already reviewed");

        String sql = "UPDATE coach_application SET status=?, reviewed_at=?, review_comment=? WHERE id=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ApplicationStatus.REJECTED.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, comment);
            ps.setInt(4, applicationId);
            ps.executeUpdate();
            System.out.println("[CoachApplicationService] ✅ Rejected application #" + applicationId);
        } catch (Exception e) {
            throw new RuntimeException("reject failed: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════════

    @Override
    public Map<String, Integer> getGlobalStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total",        0);
        stats.put("pending",      0);
        stats.put("under_review", 0);
        stats.put("approved",     0);
        stats.put("rejected",     0);

        String sql = "SELECT status, COUNT(*) AS cnt FROM coach_application GROUP BY status";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int total = 0;
            while (rs.next()) {
                String status = rs.getString("status").toLowerCase();
                int cnt       = rs.getInt("cnt");
                total        += cnt;
                switch (status) {
                    case "pending"      -> stats.put("pending",      cnt);
                    case "under_review" -> stats.put("under_review", cnt);
                    case "approved"     -> stats.put("approved",     cnt);
                    case "rejected"     -> stats.put("rejected",     cnt);
                }
            }
            stats.put("total", total);
        } catch (Exception e) {
            System.err.println("[CoachApplicationService] getGlobalStats: " + e.getMessage());
        }
        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPING
    // ═══════════════════════════════════════════════════════════

    /**
     * Mapping complet avec JOIN user (utilisé par findById, getAllApplications).
     */
    private CoachApplication mapFull(ResultSet rs) throws SQLException {
        CoachApplication app = new CoachApplication();
        app.setId(rs.getInt("id"));
        app.setUserId(rs.getInt("user_id"));
        app.setStatus(ApplicationStatus.fromValue(rs.getString("status")));
        app.setCertifications(rs.getString("certifications"));
        app.setExperience(rs.getString("experience"));
        app.setCvFile(rs.getString("cv_file"));
        app.setDocuments(rs.getString("documents"));

        Timestamp sub = rs.getTimestamp("submitted_at");
        Timestamp rev = rs.getTimestamp("reviewed_at");
        if (sub != null) app.setSubmittedAt(sub.toLocalDateTime());
        if (rev != null) app.setReviewedAt(rev.toLocalDateTime());
        app.setReviewComment(rs.getString("review_comment"));

        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setProfilePicture(rs.getString("profile_picture"));
        u.setRolesJson(rs.getString("roles_json"));
        u.setAccountStatus(rs.getString("account_status"));
        u.setBio(rs.getString("bio"));
        Timestamp uCreated = rs.getTimestamp("u_created_at");
        if (uCreated != null) u.setCreatedAt(uCreated.toLocalDateTime());
        app.setUser(u);

        return app;
    }

    /**
     * Mapping simple sans JOIN (utilisé par findLatestByUserId, hasPendingApplication).
     */
    private CoachApplication mapRow(ResultSet rs) throws SQLException {
        CoachApplication app = new CoachApplication();
        app.setId(rs.getInt("id"));
        app.setUserId(rs.getInt("user_id"));
        app.setCertifications(rs.getString("certifications"));
        app.setExperience(rs.getString("experience"));
        app.setCvFile(rs.getString("cv_file"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try   { app.setStatus(ApplicationStatus.valueOf(statusStr)); }
            catch (IllegalArgumentException e) { app.setStatus(ApplicationStatus.PENDING); }
        }

        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) app.setSubmittedAt(submitted.toLocalDateTime());

        Timestamp reviewed = rs.getTimestamp("reviewed_at");
        if (reviewed != null) app.setReviewedAt(reviewed.toLocalDateTime());

        app.setReviewComment(rs.getString("review_comment"));
        return app;
    }

    /**
     * Mapping User seul (utilisé par getAllCoaches).
     */
    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setProfilePicture(rs.getString("profile_picture"));
        u.setRolesJson(rs.getString("roles_json"));
        u.setAccountStatus(rs.getString("account_status"));
        u.setBio(rs.getString("bio"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());
        return u;
    }
}