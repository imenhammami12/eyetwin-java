package com.eyetwin.interfaces;

import com.eyetwin.entities.AuditLog;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuditLogService {

    // ── Listing / Filtering ────────────────────────────────────────
    List<AuditLog> findAll() throws SQLException;

    List<AuditLog> findFiltered(String action, String entityType,
                                Integer userId,
                                LocalDateTime dateFrom, LocalDateTime dateTo,
                                String sortBy, String sortOrder) throws SQLException;

    // ── Single record ──────────────────────────────────────────────
    AuditLog findById(int id) throws SQLException;

    // ── Stats ──────────────────────────────────────────────────────
    int countAll()      throws SQLException;
    int countSince(LocalDateTime since) throws SQLException;

    // ── Distinct filter values ─────────────────────────────────────
    List<String> getDistinctActions()     throws SQLException;
    List<String> getDistinctEntityTypes() throws SQLException;

    // ── Write ──────────────────────────────────────────────────────
    void log(AuditLog entry) throws SQLException;
}
