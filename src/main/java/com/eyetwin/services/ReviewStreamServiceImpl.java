package com.eyetwin.services;

import com.eyetwin.entities.ReviewStream;
import com.eyetwin.interfaces.IReviewStreamService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewStreamServiceImpl implements IReviewStreamService {

    private final Connection connection;

    public ReviewStreamServiceImpl() {
        this.connection = DatabaseConfig.getConnection();
    }

    @Override
    public void save(ReviewStream review) throws SQLException {
        String sql = """
            INSERT INTO review_stream
                (author_id, live_stream_id, rating, comment, verified, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,       review.getAuthor().getId());
            ps.setInt(2,       review.getLiveStream().getId());
            ps.setInt(3,       review.getRating());
            ps.setString(4,    review.getComment());
            ps.setBoolean(5,   review.isVerified());
            ps.setTimestamp(6, Timestamp.valueOf(review.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) review.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public ReviewStream findById(int id) throws SQLException {
        String sql = "SELECT * FROM review_stream WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public List<ReviewStream> findByLiveStreamId(int streamId) throws SQLException {
        String sql = "SELECT * FROM review_stream WHERE live_stream_id = ? ORDER BY created_at DESC";
        List<ReviewStream> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, streamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public double getAverageRating(int streamId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM review_stream WHERE live_stream_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, streamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    private ReviewStream mapRow(ResultSet rs) throws SQLException {
        ReviewStream r = new ReviewStream();
        r.setId(rs.getInt("id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setVerified(rs.getBoolean("verified"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());

        // Hydrate author (id only — full load si besoin)
        com.eyetwin.entities.User author = new com.eyetwin.entities.User();
        author.setId(rs.getInt("author_id"));
        r.setAuthor(author);

        // Hydrate stream (id only)
        com.eyetwin.entities.LiveStream ls = new com.eyetwin.entities.LiveStream();
        ls.setId(rs.getInt("live_stream_id"));
        r.setLiveStream(ls);

        return r;
    }
}