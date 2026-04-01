package com.eyetwin.dao;

import com.eyetwin.config.DatabaseConfig;
import com.eyetwin.model.ApplicationStatus;
import com.eyetwin.model.CoachApplication;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * CoachApplicationDAO
 *
 * Mirrors Symfony CoachApplicationRepository queries used in UserController#applyCoach().
 * Uses DatabaseConfig (same as UserDAO) instead of DatabaseConnection.
 */
public class CoachApplicationDAO {

    /**
     * Mirrors:
     *   $em->getRepository(CoachApplication::class)
     *      ->findOneBy(['user' => $user, 'status' => ApplicationStatus::PENDING])
     *
     * Returns true if the user already has a PENDING application.
     */
    public boolean hasPendingApplication(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coach_application WHERE user_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * Mirrors:  $em->persist($application); $em->flush();
     * Inserts a new CoachApplication row with status = PENDING.
     */
    public void save(CoachApplication app) throws SQLException {
        String sql = "INSERT INTO coach_application "
                + "(user_id, status, certifications, experience, cv_file, submitted_at) "
                + "VALUES (?, 'PENDING', ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, app.getUserId());
            ps.setString(2, app.getCertifications());
            ps.setString(3, app.getExperience());
            ps.setString(4, app.getCvFile());   // nullable — OK with setString(null)
            ps.setTimestamp(5, Timestamp.valueOf(
                    app.getSubmittedAt() != null ? app.getSubmittedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) app.setId(keys.getInt(1));
        }
    }

    /**
     * Mirrors:
     *   $em->getRepository(CoachApplication::class)
     *      ->findBy(['user' => $user], ['submittedAt' => 'DESC'])  — first result
     *
     * Used by UserProfileController to show latest application status.
     */
    public CoachApplication findLatestByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM coach_application "
                + "WHERE user_id = ? ORDER BY submitted_at DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── Row mapper ──
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
}