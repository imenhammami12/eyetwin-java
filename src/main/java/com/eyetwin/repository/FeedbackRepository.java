package com.eyetwin.repository;

import com.eyetwin.entities.StreamFeedback;
import com.eyetwin.interfaces.IFeedbackRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedbackRepository implements IFeedbackRepository {

    private final Connection connection;

    public FeedbackRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(StreamFeedback fb) {
        String sql = """
            INSERT INTO stream_feedback
                (live_stream_id, spectator_id, rating, comment, feedback_type, processed, submitted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fb.getLiveStream().getId());
            ps.setInt(2, fb.getSpectator().getId());
            ps.setInt(3, fb.getRating());
            ps.setString(4, fb.getComment());
            ps.setString(5, fb.getFeedbackType().name());
            ps.setBoolean(6, fb.isProcessed());
            ps.setTimestamp(7, Timestamp.valueOf(fb.getSubmittedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) fb.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save StreamFeedback", e);
        }
    }

    @Override
    public List<StreamFeedback> findUnprocessed() {
        String sql = "SELECT * FROM stream_feedback WHERE processed = false";
        List<StreamFeedback> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findUnprocessed", e);
        }
        return list;
    }

    @Override
    public void markAsProcessed(int feedbackId) {
        String sql = "UPDATE stream_feedback SET processed = true WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, feedbackId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur markAsProcessed", e);
        }
    }

    @Override
    public StreamFeedback findById(int id) {
        String sql = "SELECT * FROM stream_feedback WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findById StreamFeedback", e);
        }
        return null;
    }

    @Override
    public List<StreamFeedback> findByLiveStreamId(int liveStreamId) {
        String sql = "SELECT * FROM stream_feedback WHERE live_stream_id = ?";
        List<StreamFeedback> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, liveStreamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByLiveStreamId", e);
        }
        return list;
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
        // Note: liveStream & spectator à hydrater via leurs repos si besoin
        return fb;
    }
}