package com.eyetwin.services;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IComplaintService;
import com.eyetwin.repository.ComplaintRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ComplaintServiceImpl — business logic layer.
 *
 * Each method mirrors one Symfony controller action:
 *
 *   submit()          ← user submits a complaint
 *   assign()          ← POST /admin/complaints/{id}/assign
 *   unassign()        ← POST /admin/complaints/{id}/assign  (admin_id = "unassign")
 *   updateStatus()    ← POST /admin/complaints/{id}/update-status
 *   updatePriority()  ← POST /admin/complaints/{id}/update-priority
 *   addAdminResponse()← POST /admin/complaints/{id}/respond
 *   resolve()         ← POST /admin/complaints/{id}/resolve
 *   delete()          ← POST /admin/complaints/{id}/delete
 *   getStatistics()   ← ComplaintRepository::getStatistics()
 */
public class ComplaintServiceImpl implements IComplaintService {

    private final ComplaintRepository repo;

    public ComplaintServiceImpl() {
        this.repo = new ComplaintRepository();
    }

    // ════════════════════════════════════════════════════════════
    //  LISTING / SEARCH
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Complaint> getAll() throws SQLException {
        return repo.findAll();
    }

    @Override
    public List<Complaint> search(String keyword,
                                  ComplaintStatus   status,
                                  ComplaintPriority priority,
                                  ComplaintCategory category,
                                  String            sentimentFilter) throws SQLException {
        return repo.search(keyword, status, priority, category, sentimentFilter);
    }

    @Override
    public Complaint getById(int id) throws SQLException {
        Complaint c = repo.findById(id);
        if (c == null) throw new IllegalArgumentException("Complaint not found: " + id);
        return c;
    }

    @Override
    public List<Complaint> getByUser(int userId) throws SQLException {
        return repo.findByUser(userId);
    }

    @Override
    public List<Complaint> getUnassigned() throws SQLException {
        return repo.findUnassigned();
    }

    // ════════════════════════════════════════════════════════════
    //  SUBMIT  (user-facing)
    // ════════════════════════════════════════════════════════════

    @Override
    public Complaint submit(Complaint complaint, int submittedByUserId) throws SQLException {
        // Validation mirrors Symfony Assert constraints
        if (complaint.getSubject() == null || complaint.getSubject().length() < 5)
            throw new IllegalArgumentException("Subject must be at least 5 characters");
        if (complaint.getDescription() == null || complaint.getDescription().length() < 10)
            throw new IllegalArgumentException("Description must be at least 10 characters");
        if (complaint.getCategory() == null)
            throw new IllegalArgumentException("Category is required");

        // Apply category default priority if not set explicitly
        if (complaint.getPriority() == null)
            complaint.setPriority(complaint.getCategory().getDefaultPriority());

        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());

        User submitter = new User();
        submitter.setId(submittedByUserId);
        complaint.setSubmittedBy(submitter);

        repo.insert(complaint);
        return complaint;
    }

    // ════════════════════════════════════════════════════════════
    //  ADMIN ACTIONS
    // ════════════════════════════════════════════════════════════

    /**
     * Mirrors assign action.
     * If complaint is PENDING and gets assigned → auto-advance to IN_PROGRESS.
     */
    @Override
    public void assign(int complaintId, int adminUserId) throws SQLException {
        Complaint c = mustFind(complaintId);

        User admin = new User();
        admin.setId(adminUserId);
        c.setAssignedTo(admin);
        c.setUpdatedAt(LocalDateTime.now());

        // Auto-advance status (mirrors Symfony controller logic)
        if (c.getStatus() == ComplaintStatus.PENDING)
            c.setStatus(ComplaintStatus.IN_PROGRESS);

        repo.update(c);
    }

    @Override
    public void unassign(int complaintId) throws SQLException {
        Complaint c = mustFind(complaintId);
        c.setAssignedTo(null);
        c.setUpdatedAt(LocalDateTime.now());
        repo.update(c);
    }

    /**
     * Mirrors update-status action.
     * Validates transition via ComplaintStatus.allowedTransitions().
     */
    @Override
    public void updateStatus(int complaintId, ComplaintStatus newStatus) throws SQLException {
        Complaint c = mustFind(complaintId);

        if (!c.getStatus().canTransitionTo(newStatus))
            throw new IllegalStateException(
                "Cannot transition from " + c.getStatus() + " to " + newStatus);

        c.setStatus(newStatus);
        c.setUpdatedAt(LocalDateTime.now());

        // Set resolvedAt when finalised (mirrors Symfony setStatus())
        if (newStatus.isFinal() && c.getResolvedAt() == null)
            c.setResolvedAt(LocalDateTime.now());

        repo.update(c);
    }

    @Override
    public void updatePriority(int complaintId, ComplaintPriority newPriority) throws SQLException {
        Complaint c = mustFind(complaintId);
        c.setPriority(newPriority);
        c.setUpdatedAt(LocalDateTime.now());
        repo.update(c);
    }

    /**
     * Mirrors respond action.
     * Auto-assigns to responding admin if unassigned.
     * Auto-advances PENDING → IN_PROGRESS.
     */
    @Override
    public void addAdminResponse(int complaintId, String response,
                                 int respondingAdminId) throws SQLException {
        if (response == null || response.isBlank())
            throw new IllegalArgumentException("Response cannot be empty");

        Complaint c = mustFind(complaintId);

        if (c.isResolved())
            throw new IllegalStateException("Cannot respond to a resolved complaint");

        c.setAdminResponse(response);
        c.setUpdatedAt(LocalDateTime.now());

        // Auto-assign if unassigned (mirrors Symfony controller)
        if (c.getAssignedTo() == null) {
            User admin = new User();
            admin.setId(respondingAdminId);
            c.setAssignedTo(admin);
        }

        // Auto-advance status
        if (c.getStatus() == ComplaintStatus.PENDING)
            c.setStatus(ComplaintStatus.IN_PROGRESS);

        repo.update(c);
    }

    /**
     * Mirrors resolve action.
     * Forces status to RESOLVED + stamps resolvedAt.
     */
    @Override
    public void resolve(int complaintId, String resolutionNotes) throws SQLException {
        if (resolutionNotes == null || resolutionNotes.isBlank())
            throw new IllegalArgumentException("Resolution notes are required");

        Complaint c = mustFind(complaintId);
        c.setResolutionNotes(resolutionNotes);
        c.setStatus(ComplaintStatus.RESOLVED);
        c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        repo.update(c);
    }

    @Override
    public void delete(int complaintId) throws SQLException {
        mustFind(complaintId); // existence check
        repo.delete(complaintId);
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTICS
    // ════════════════════════════════════════════════════════════

    @Override
    public ComplaintStats getStatistics() throws SQLException {
        return repo.getStatistics();
    }


    /**
     * Mirrors the front-office submit — user already set on the complaint object.
     * Called by ComplaintController (JavaFX front-office).
     */
    @Override
    public Complaint create(Complaint complaint) throws SQLException {
        if (complaint.getSubmittedBy() == null)
            throw new IllegalArgumentException("submittedBy is required");

        return submit(complaint, complaint.getSubmittedBy().getId());
    }

    // ════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    private Complaint mustFind(int id) throws SQLException {
        Complaint c = repo.findById(id);
        if (c == null) throw new IllegalArgumentException("Complaint not found: " + id);
        return c;
    }
}
