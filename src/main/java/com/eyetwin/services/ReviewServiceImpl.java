package com.eyetwin.services;

import com.eyetwin.entities.Review;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IReviewService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewServiceImpl — implémentation de IReviewService.
 */
public class ReviewServiceImpl implements IReviewService {

    // ════════════════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════════════════

    private final SentimentAnalysisService sentimentService = new SentimentAnalysisService();

    @Override
    public Review createReview(Review review) throws SQLException {
        // Enregistrer le sentiment basé sur l'analyse de texte (HuggingFace)
        if (review.getSentiment() == null) {
            try {
                String sentiment = sentimentService.analyze(review.getContent(), review.getRating()).join();
                review.setSentiment(sentiment);
            } catch (Exception e) {
                // Fallback direct sur le rating
                review.setSentiment(calculateSentiment(review.getRating()));
            }
        }

        String sql = """
            INSERT INTO review (content, rating, created_at, sentiment, user_id, ID_planning)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, review.getContent());
            ps.setInt(2, review.getRating());
            ps.setTimestamp(3, Timestamp.valueOf(review.getCreatedAt()));
            ps.setString(4, review.getSentiment());
            ps.setInt(5, review.getUserId());
            ps.setInt(6, review.getIdPlanning());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                review.setId(id);
            }
        }
        return review;
    }

    // ════════════════════════════════════════════════════════════
    //  READ
    // ════════════════════════════════════════════════════════════

    @Override
    public Review getReviewById(int id) throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.profile_picture
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            WHERE r.id = ?
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapReviewWithUser(rs);
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE
    // ════════════════════════════════════════════════════════════

    @Override
    public void updateReview(Review review) throws SQLException {
        // Recalculer le sentiment si le rating change
        if (review.getSentiment() == null) {
            review.setSentiment(calculateSentiment(review.getRating()));
        }

        String sql = """
            UPDATE review 
            SET content = ?, rating = ?, sentiment = ?
            WHERE id = ?
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, review.getContent());
            ps.setInt(2, review.getRating());
            ps.setString(3, review.getSentiment());
            ps.setInt(4, review.getId());

            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════

    @Override
    public void deleteReview(int id, int userId) throws SQLException {
        // Vérifier que l'utilisateur est le propriétaire
        String sql = "DELETE FROM review WHERE id = ? AND user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LISTING
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Review> getAllReviews() throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.profile_picture
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            ORDER BY r.created_at DESC
            """;
        List<Review> reviews = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReviewWithUser(rs));
                }
            }
        }
        return reviews;
    }

    /**
     * Admin listing: include planning info for "Planning / Session" column.
     */
    public List<Review> getAllReviewsWithPlanning() throws SQLException {
        String sql = """
            SELECT r.*, 
                   u.username, u.profile_picture,
                   p.IDplanning AS p_id, p.type AS p_type, p.level AS p_level, p.date AS p_date, p.time AS p_time
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            LEFT JOIN planning p ON p.IDplanning = r.ID_planning
            ORDER BY r.created_at DESC
            """;
        List<Review> reviews = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = mapReviewWithUser(rs);
                    Planning p = mapPlanningStub(rs);
                    if (p != null) r.setPlanning(p);
                    reviews.add(r);
                }
            }
        }
        return reviews;
    }

    /**
     * Admin delete without user ownership restriction.
     */
    public void deleteReviewAdmin(int id) throws SQLException {
        String sql = "DELETE FROM review WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Update sentiment only (used by AI analyze buttons).
     */
    public void updateSentiment(int reviewId, String sentiment) throws SQLException {
        String sql = "UPDATE review SET sentiment = ? WHERE id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sentiment);
            ps.setInt(2, reviewId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Review> getReviewsByPlanning(int idPlanning) throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.profile_picture
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            WHERE r.ID_planning = ?
            ORDER BY r.created_at DESC
            """;
        List<Review> reviews = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReviewWithUser(rs));
                }
            }
        }
        return reviews;
    }

    @Override
    public List<Review> getReviewsByUser(int userId) throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.profile_picture
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            WHERE r.user_id = ?
            ORDER BY r.created_at DESC
            """;
        List<Review> reviews = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReviewWithUser(rs));
                }
            }
        }
        return reviews;
    }

    @Override
    public List<Review> getReviewsBySentiment(String sentiment) throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.profile_picture
            FROM review r
            INNER JOIN user u ON u.id = r.user_id
            WHERE r.sentiment = ?
            ORDER BY r.created_at DESC
            """;
        List<Review> reviews = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sentiment);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReviewWithUser(rs));
                }
            }
        }
        return reviews;
    }

    // ════════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean hasUserReviewedPlanning(int userId, int idPlanning) throws SQLException {
        String sql = "SELECT id FROM review WHERE user_id = ? AND ID_planning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, idPlanning);
            return ps.executeQuery().next();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════════

    @Override
    public double getAverageRating(int idPlanning) throws SQLException {
        String sql = "SELECT AVG(rating) FROM review WHERE ID_planning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    @Override
    public int countReviews(int idPlanning) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review WHERE ID_planning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════

    private Review mapReview(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setContent(rs.getString("content"));
        r.setRating(rs.getInt("rating"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setSentiment(rs.getString("sentiment"));
        r.setUserId(rs.getInt("user_id"));
        r.setIdPlanning(rs.getInt("ID_planning"));
        return r;
    }

    private Review mapReviewWithUser(ResultSet rs) throws SQLException {
        Review r = mapReview(rs);
        
        // Créer l'objet User
        User user = new User();
        user.setId(r.getUserId());
        user.setUsername(rs.getString("username"));
        user.setProfilePicture(rs.getString("profile_picture"));
        r.setUser(user);
        
        return r;
    }

    private Planning mapPlanningStub(ResultSet rs) throws SQLException {
        try {
            int pid = rs.getInt("p_id");
            if (rs.wasNull()) return null;
            Planning p = new Planning();
            p.setIdPlanning(pid);
            p.setType(rs.getString("p_type"));
            p.setLevel(rs.getString("p_level"));
            Date d = rs.getDate("p_date");
            Time t = rs.getTime("p_time");
            if (d != null) p.setDate(d.toLocalDate());
            if (t != null) p.setTime(t.toLocalTime());
            return p;
        } catch (SQLException e) {
            return null;
        }
    }

    private String calculateSentiment(int rating) {
        if (rating >= 4) return "positive";
        if (rating <= 2) return "negative";
        return "neutral";
    }
}
