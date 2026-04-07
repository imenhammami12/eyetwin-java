package com.eyetwin.interfaces;

import com.eyetwin.entities.*;

import java.sql.SQLException;
import java.util.List;

/**
 * IComplaintService — mirrors AdminComplaintController actions in Symfony.
 *
 * Each method maps to a route:
 *   index          → GET  /admin/complaints/
 *   getById        → GET  /admin/complaints/{id}
 *   submit         → user-facing submission
 *   assign         → POST /admin/complaints/{id}/assign
 *   updateStatus   → POST /admin/complaints/{id}/update-status
 *   updatePriority → POST /admin/complaints/{id}/update-priority
 *   respond        → POST /admin/complaints/{id}/respond
 *   resolve        → POST /admin/complaints/{id}/resolve
 *   delete         → POST /admin/complaints/{id}/delete
 */
public interface IComplaintService {

    // ── Listing / search ──────────────────────────────────────────
    List<Complaint> getAll()                                          throws SQLException;
    List<Complaint> search(String keyword, ComplaintStatus status,
                           ComplaintPriority priority,
                           ComplaintCategory category,
                           String sentimentFilter)                    throws SQLException;

    // ── CRUD ──────────────────────────────────────────────────────
    Complaint getById(int id)                                         throws SQLException;
    Complaint submit(Complaint complaint, int submittedByUserId)
            throws SQLException;
    Complaint create(Complaint complaint) throws SQLException;

    void      delete(int complaintId)                                 throws SQLException;

    // ── Admin actions ─────────────────────────────────────────────
    void assign(int complaintId, int adminUserId)                     throws SQLException;
    void unassign(int complaintId)                                    throws SQLException;
    void updateStatus(int complaintId, ComplaintStatus newStatus)     throws SQLException;
    void updatePriority(int complaintId, ComplaintPriority newPriority) throws SQLException;
    void addAdminResponse(int complaintId, String response,
                          int respondingAdminId)                      throws SQLException;
    void resolve(int complaintId, String resolutionNotes)            throws SQLException;

    // ── Statistics (mirrors ComplaintRepository::getStatistics) ───
    ComplaintStats getStatistics()                                    throws SQLException;

    // ── Helpers ───────────────────────────────────────────────────
    List<Complaint> getByUser(int userId)                             throws SQLException;
    List<Complaint> getUnassigned()                                   throws SQLException;
}
