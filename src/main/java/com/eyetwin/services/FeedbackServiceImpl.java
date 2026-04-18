package com.eyetwin.services;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IFeedbackService;
import com.eyetwin.interfaces.IReviewStreamService;
import com.eyetwin.interfaces.IComplaintService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackServiceImpl implements IFeedbackService {

    private final Connection         connection;
    private final IReviewStreamService reviewService;
    private final IComplaintService    complaintService;

    public FeedbackServiceImpl() {
        this.connection      = DatabaseConfig.getConnection();
        this.reviewService   = new ReviewStreamServiceImpl();
        this.complaintService = new ComplaintServiceImpl();
    }

    // ════════════════════════════════════════════════════════
    //  SAVE
    // ════════════════════════════════════════════════════════

    @Override
    public void save(StreamFeedback fb) throws SQLException {
        String sql = """
            INSERT INTO stream_feedback
                (live_stream_id, spectator_id, rating, comment,
                 feedback_type, processed, submitted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,       fb.getLiveStream().getId());
            ps.setInt(2,       fb.getSpectator().getId());
            ps.setInt(3,       fb.getRating());
            ps.setString(4,    fb.getComment());
            ps.setString(5,    fb.getFeedbackType().name());
            ps.setBoolean(6,   fb.isProcessed());
            ps.setTimestamp(7, Timestamp.valueOf(fb.getSubmittedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) fb.setId(rs.getInt(1));
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  PROCESS FEEDBACK — logique principale
    // ════════════════════════════════════════════════════════

    @Override
    public void processFeedback(StreamFeedback feedback) throws SQLException {
        if (feedback.isProcessed())
            throw new IllegalStateException("Feedback déjà traité : " + feedback.getId());

        feedback.inferFeedbackType();
        save(feedback);

        if (feedback.shouldGenerateReview()) {
            ReviewStream review = ReviewStream.fromFeedback(feedback);
            reviewService.save(review);
            feedback.setGeneratedReview(review);
            updateGeneratedReview(feedback.getId(), review.getId());
        }

        if (feedback.shouldGenerateComplaint()) {
            Complaint complaint = buildComplaint(feedback);
            complaintService.create(complaint);
            feedback.setGeneratedComplaint(complaint);
            updateGeneratedComplaint(feedback.getId(), complaint.getId());
        }

        markAsProcessed(feedback.getId());

        // ── Email de confirmation ──────────────────────────────
        User spectator = feedback.getSpectator();
        if (spectator != null && spectator.getEmail() != null) {
            EmailService.getInstance().sendFeedbackConfirmationEmail(
                    spectator.getEmail(),
                    spectator.getFullName() != null ? spectator.getFullName() : spectator.getUsername(),
                    feedback.getLiveStream().getTitle(),
                    feedback.getRating(),
                    feedback.getComment(),
                    feedback.getFeedbackType()
            );
        }
    }
    // ════════════════════════════════════════════════════════
    //  QUERIES
    // ════════════════════════════════════════════════════════

    @Override
    public StreamFeedback findById(int id) throws SQLException {
        String sql = "SELECT * FROM stream_feedback WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public List<StreamFeedback> findByLiveStreamId(int streamId) throws SQLException {
        String sql = "SELECT * FROM stream_feedback WHERE live_stream_id = ? ORDER BY submitted_at DESC";
        List<StreamFeedback> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, streamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<StreamFeedback> findUnprocessed() throws SQLException {
        String sql = "SELECT * FROM stream_feedback WHERE processed = false";
        List<StreamFeedback> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void markAsProcessed(int feedbackId) throws SQLException {
        String sql = "UPDATE stream_feedback SET processed = true WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, feedbackId);
            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

    private void updateGeneratedReview(int feedbackId, int reviewId) throws SQLException {
        String sql = "UPDATE stream_feedback SET generated_review_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, feedbackId);
            ps.executeUpdate();
        }
    }

    private void updateGeneratedComplaint(int feedbackId, int complaintId) throws SQLException {
        String sql = "UPDATE stream_feedback SET generated_complaint_id = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, feedbackId);
            ps.executeUpdate();
        }
    }

    private Complaint buildComplaint(StreamFeedback fb) {
        Complaint c = new Complaint();
        c.setSubmittedBy(fb.getSpectator());
        c.setSubject("Feedback négatif – Stream : " + fb.getLiveStream().getTitle());
        c.setDescription(fb.getComment());
        c.setCategory(ComplaintCategory.OTHER);
        c.setPriority(switch (fb.getRating()) {
            case 1  -> ComplaintPriority.HIGH;
            case 2  -> ComplaintPriority.MEDIUM;
            default -> ComplaintPriority.LOW;
        });
        return c;
    }

    private StreamFeedback mapRow(ResultSet rs) throws SQLException {
        StreamFeedback fb = new StreamFeedback();
        fb.setId(rs.getInt("id"));
        fb.setRating(rs.getInt("rating"));
        fb.setComment(rs.getString("comment"));
        fb.setFeedbackType(StreamFeedback.FeedbackType.valueOf(rs.getString("feedback_type")));
        fb.setProcessed(rs.getBoolean("processed"));
        Timestamp ts = rs.getTimestamp("submitted_at");
        if (ts != null) fb.setSubmittedAt(ts.toLocalDateTime());

        // Hydrate FK (id only)
        LiveStream ls = new LiveStream(); ls.setId(rs.getInt("live_stream_id"));
        fb.setLiveStream(ls);
        User u = new User(); u.setId(rs.getInt("spectator_id"));
        fb.setSpectator(u);

        return fb;
    }
}