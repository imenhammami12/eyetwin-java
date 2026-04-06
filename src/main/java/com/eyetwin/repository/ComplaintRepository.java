package com.eyetwin.repository;

import com.eyetwin.entities.*;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ComplaintRepository — raw SQL layer.
 * Mirrors Symfony's ComplaintRepository (Doctrine) + getStatistics().
 *
 * All public methods are package-private or used only by ComplaintServiceImpl.
 * Controllers must go through the service, never this class directly.
 */
public class ComplaintRepository {

    // ════════════════════════════════════════════════════════════
    //  FIND / LISTING
    // ════════════════════════════════════════════════════════════

    public List<Complaint> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY c.priority DESC, c.created_at DESC";
        return executeQuery(sql);
    }

    /**
     * Mirrors the QueryBuilder in AdminComplaintController::index()
     * search across subject, description, username + optional filters.
     */
    public List<Complaint> search(String keyword,
                                  ComplaintStatus   status,
                                  ComplaintPriority priority,
                                  ComplaintCategory category,
                                  String            sentimentFilter) throws SQLException {

        StringBuilder sql    = new StringBuilder(baseSelect());
        List<Object>  params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                 AND (c.subject LIKE ? OR c.description LIKE ?
                      OR u.username LIKE ?)
                """);
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (status != null) {
            sql.append(" AND c.status = ?");
            params.add(status.name());
        }
        if (priority != null) {
            sql.append(" AND c.priority = ?");
            params.add(priority.name());
        }
        if (category != null) {
            sql.append(" AND c.category = ?");
            params.add(category.name());
        }
        if (sentimentFilter != null && !sentimentFilter.isBlank()) {
            sql.append(" AND c.sentiment_label = ?");
            params.add(sentimentFilter.toUpperCase());
        }

        sql.append(" ORDER BY c.priority DESC, c.created_at DESC");
        return executeQuery(sql.toString(), params);
    }

    public Complaint findById(int id) throws SQLException {
        String sql = baseSelect() + " AND c.id = ?";
        List<Complaint> result = executeQuery(sql, List.of(id));
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Complaint> findByUser(int userId) throws SQLException {
        String sql = baseSelect() + " AND c.submitted_by_id = ? ORDER BY c.created_at DESC";
        return executeQuery(sql, List.of(userId));
    }

    public List<Complaint> findUnassigned() throws SQLException {
        String sql = baseSelect() + " AND c.assigned_to_id IS NULL ORDER BY c.priority DESC, c.created_at DESC";
        return executeQuery(sql);
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTICS  (mirrors ComplaintRepository::getStatistics())
    // ════════════════════════════════════════════════════════════

    public ComplaintStats getStatistics() throws SQLException {
        ComplaintStats stats = new ComplaintStats();
        String sql = """
            SELECT
                COUNT(*)                                               AS total,
                SUM(c.status = 'PENDING')                             AS pending,
                SUM(c.status = 'IN_PROGRESS')                         AS in_progress,
                SUM(c.status = 'RESOLVED')                            AS resolved,
                SUM(c.status = 'REJECTED')                            AS rejected,
                SUM(c.assigned_to_id IS NULL)                         AS unassigned,
                AVG(CASE WHEN c.resolved_at IS NOT NULL
                    THEN TIMESTAMPDIFF(HOUR, c.created_at, c.resolved_at)
                    ELSE NULL END)                                     AS avg_hours
            FROM complaint c
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setTotal(rs.getInt("total"));
                stats.setPending(rs.getInt("pending"));
                stats.setInProgress(rs.getInt("in_progress"));
                stats.setResolved(rs.getInt("resolved"));
                stats.setRejected(rs.getInt("rejected"));
                stats.setUnassigned(rs.getInt("unassigned"));
                stats.setAvgResolutionHours(rs.getDouble("avg_hours"));
            }
        }
        return stats;
    }

    // ════════════════════════════════════════════════════════════
    //  INSERT / UPDATE / DELETE
    // ════════════════════════════════════════════════════════════

    public int insert(Complaint c) throws SQLException {
        String sql = """
            INSERT INTO complaint
                (subject, description, category, status, priority,
                 submitted_by_id, assigned_to_id, created_at, updated_at,
                 resolved_at, admin_response, resolution_notes,
                 attachment_path, sentiment_label, sentiment_score,
                 sentiment_source, sentiment_priority_suggestion)
            VALUES (?,?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?,?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setWriteParams(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { c.setId(keys.getInt(1)); return c.getId(); }
            }
        }
        return -1;
    }

    public void update(Complaint c) throws SQLException {
        String sql = """
            UPDATE complaint SET
                subject = ?, description = ?, category = ?, status = ?, priority = ?,
                submitted_by_id = ?, assigned_to_id = ?, created_at = ?,
                updated_at = ?, resolved_at = ?, admin_response = ?,
                resolution_notes = ?, attachment_path = ?,
                sentiment_label = ?, sentiment_score = ?,
                sentiment_source = ?, sentiment_priority_suggestion = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setWriteParams(ps, c);
            ps.setInt(18, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM complaint WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /** Base SELECT with JOINs for submittedBy (u) and assignedTo (a). */
    private String baseSelect() {
        return """
            SELECT
                c.id, c.subject, c.description, c.category, c.status, c.priority,
                c.submitted_by_id, c.assigned_to_id,
                c.created_at, c.updated_at, c.resolved_at,
                c.admin_response, c.resolution_notes, c.attachment_path,
                c.sentiment_label, c.sentiment_score,
                c.sentiment_source, c.sentiment_priority_suggestion,
                u.id AS u_id, u.username AS u_username, u.email AS u_email,
                a.id AS a_id, a.username AS a_username, a.email AS a_email
            FROM complaint c
            INNER JOIN user u ON u.id = c.submitted_by_id
            LEFT  JOIN user a ON a.id = c.assigned_to_id
            WHERE 1=1
            """;
    }

    private List<Complaint> executeQuery(String sql) throws SQLException {
        return executeQuery(sql, List.of());
    }

    private List<Complaint> executeQuery(String sql, List<Object> params) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Complaint mapRow(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getInt("id"));
        c.setSubject(rs.getString("subject"));
        c.setDescription(rs.getString("description"));
        c.setCategory(ComplaintCategory.fromValue(rs.getString("category")));
        c.setStatus(ComplaintStatus.fromValue(rs.getString("status")));
        c.setPriority(ComplaintPriority.fromValue(rs.getString("priority")));
        c.setAdminResponse(rs.getString("admin_response"));
        c.setResolutionNotes(rs.getString("resolution_notes"));
        c.setAttachmentPath(rs.getString("attachment_path"));

        // Sentiment
        c.setSentimentLabel(rs.getString("sentiment_label"));
        c.setSentimentScore(rs.getDouble("sentiment_score"));
        c.setSentimentSource(rs.getString("sentiment_source"));
        c.setSentimentPrioritySuggestion(rs.getString("sentiment_priority_suggestion"));

        // Timestamps
        c.setCreatedAt(toLocal(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocal(rs.getTimestamp("updated_at")));
        c.setResolvedAt(toLocal(rs.getTimestamp("resolved_at")));

        // submittedBy
        User submitter = new User();
        submitter.setId(rs.getInt("u_id"));
        submitter.setUsername(rs.getString("u_username"));
        submitter.setEmail(rs.getString("u_email"));
        c.setSubmittedBy(submitter);

        // assignedTo (nullable)
        int assignedId = rs.getInt("a_id");
        if (!rs.wasNull()) {
            User admin = new User();
            admin.setId(assignedId);
            admin.setUsername(rs.getString("a_username"));
            admin.setEmail(rs.getString("a_email"));
            c.setAssignedTo(admin);
        }

        return c;
    }

    /** Maps all writable fields to PreparedStatement (positions 1-17). */
    private void setWriteParams(PreparedStatement ps, Complaint c) throws SQLException {
        ps.setString(1,  c.getSubject());
        ps.setString(2,  c.getDescription());
        ps.setString(3,  c.getCategory() != null ? c.getCategory().name() : null);
        ps.setString(4,  c.getStatus()   != null ? c.getStatus().name()   : ComplaintStatus.PENDING.name());
        ps.setString(5,  c.getPriority() != null ? c.getPriority().name() : ComplaintPriority.MEDIUM.name());

        if (c.getSubmittedBy() != null) ps.setInt(6, c.getSubmittedBy().getId());
        else ps.setNull(6, Types.INTEGER);

        if (c.getAssignedTo() != null) ps.setInt(7, c.getAssignedTo().getId());
        else ps.setNull(7, Types.INTEGER);

        ps.setTimestamp(8,  toTimestamp(c.getCreatedAt()));
        ps.setTimestamp(9,  toTimestamp(c.getUpdatedAt()));
        ps.setTimestamp(10, toTimestamp(c.getResolvedAt()));

        ps.setString(11, c.getAdminResponse());
        ps.setString(12, c.getResolutionNotes());
        ps.setString(13, c.getAttachmentPath());

        ps.setString(14, c.getSentimentLabel());
        if (c.getSentimentScore() > 0)
            ps.setDouble(15, c.getSentimentScore());
        else
            ps.setNull(15, Types.DOUBLE);
        ps.setString(16, c.getSentimentSource());
        ps.setString(17, c.getSentimentPrioritySuggestion());
    }

    private LocalDateTime toLocal(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return ldt != null ? Timestamp.valueOf(ldt) : null;
    }
}
