package com.eyetwin.services;

import com.eyetwin.entities.AuditLog;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IAuditLogService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogServiceImpl implements IAuditLogService {

    // ── Helper centralisé (singleton, comme PlanningServiceImpl) ────────────
    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════════
    //  FIND ALL
    // ════════════════════════════════════════════════════════════
    @Override
    public List<AuditLog> findAll() throws SQLException {
        String sql = """
            SELECT al.*, u.username, u.email
            FROM audit_log al
            LEFT JOIN user u ON u.id = al.user_id
            ORDER BY al.created_at DESC
            """;
        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  FIND FILTERED
    // ════════════════════════════════════════════════════════════
    @Override
    public List<AuditLog> findFiltered(String action, String entityType,
                                       Integer userId,
                                       LocalDateTime dateFrom, LocalDateTime dateTo,
                                       String sortBy, String sortOrder) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT al.*, u.username, u.email
            FROM audit_log al
            LEFT JOIN user u ON u.id = al.user_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (action != null && !action.isBlank()) {
            sql.append(" AND al.action = ?");
            params.add(action);
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND al.entity_type = ?");
            params.add(entityType);
        }
        if (userId != null) {
            sql.append(" AND al.user_id = ?");
            params.add(userId);
        }
        if (dateFrom != null) {
            sql.append(" AND al.created_at >= ?");
            params.add(Timestamp.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append(" AND al.created_at <= ?");
            params.add(Timestamp.valueOf(dateTo));
        }

        // Sort
        List<String> validFields = List.of("created_at", "action", "entity_type");
        String field = validFields.contains(sortBy) ? sortBy : "created_at";
        String order = "ASC".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        sql.append(" ORDER BY al.").append(field).append(" ").append(order);

        List<AuditLog> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  FIND BY ID
    // ════════════════════════════════════════════════════════════
    @Override
    public AuditLog findById(int id) throws SQLException {
        String sql = """
            SELECT al.*, u.username, u.email
            FROM audit_log al
            LEFT JOIN user u ON u.id = al.user_id
            WHERE al.id = ?
            """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════════════════════
    @Override
    public int countAll() throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT COUNT(*) FROM audit_log");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Override
    public int countSince(LocalDateTime since) throws SQLException {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE created_at >= ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DISTINCT VALUES (for filter dropdowns)
    // ════════════════════════════════════════════════════════════
    @Override
    public List<String> getDistinctActions() throws SQLException {
        return queryDistinct("SELECT DISTINCT action FROM audit_log ORDER BY action ASC");
    }

    @Override
    public List<String> getDistinctEntityTypes() throws SQLException {
        return queryDistinct("SELECT DISTINCT entity_type FROM audit_log ORDER BY entity_type ASC");
    }

    private List<String> queryDistinct(String sql) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null) list.add(v);
            }
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    //  WRITE
    // ════════════════════════════════════════════════════════════
    @Override
    public void log(AuditLog entry) throws SQLException {
        String sql = """
            INSERT INTO audit_log (user_id, action, entity_type, entity_id, details, ip_address, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (entry.getUser() != null) ps.setInt(1, entry.getUser().getId());
            else                         ps.setNull(1, Types.INTEGER);
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getEntityType());
            if (entry.getEntityId() != null) ps.setInt(4, entry.getEntityId());
            else                              ps.setNull(4, Types.INTEGER);
            ps.setString(5, entry.getDetails());
            ps.setString(6, entry.getIpAddress());
            ps.setTimestamp(7, Timestamp.valueOf(
                    entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) entry.setId(rs.getInt(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MAPPING
    // ════════════════════════════════════════════════════════════
    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));

        int entityId = rs.getInt("entity_id");
        log.setEntityId(rs.wasNull() ? null : entityId);

        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));

        Timestamp ts = rs.getTimestamp("created_at");
        log.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());

        // User (LEFT JOIN — may be null)
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            User u = new User();
            u.setId(userId);
            try { u.setUsername(rs.getString("username")); } catch (SQLException ignored) {}
            try { u.setEmail(rs.getString("email"));       } catch (SQLException ignored) {}
            log.setUser(u);
        }

        return log;
    }
}