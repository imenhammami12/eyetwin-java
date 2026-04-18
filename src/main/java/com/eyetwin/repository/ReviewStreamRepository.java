package com.eyetwin.repository;

import com.eyetwin.entities.ReviewStream;
import com.eyetwin.interfaces.IReviewStreamRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewStreamRepository implements IReviewStreamRepository {

    private final Connection connection;

    public ReviewStreamRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(ReviewStream review) {
        String sql = """
            INSERT INTO review_stream (author_id, live_stream_id, rating, comment, verified, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getAuthor().getId());
            ps.setInt(2, review.getLiveStream().getId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.setBoolean(5, review.isVerified());
            ps.setTimestamp(6, Timestamp.valueOf(review.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) review.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save ReviewStream", e);
        }
    }

    @Override
    public double getAverageRating(int liveStreamId) {
        String sql = "SELECT AVG(rating) FROM review_stream WHERE live_stream_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, liveStreamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAverageRating", e);
        }
        return 0.0;
    }

    @Override
    public ReviewStream findById(int id) { return null; }

    @Override
    public List<ReviewStream> findByLiveStreamId(int liveStreamId) { return new ArrayList<>(); }
}