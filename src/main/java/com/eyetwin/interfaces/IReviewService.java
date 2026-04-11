package com.eyetwin.interfaces;

import com.eyetwin.entities.Review;

import java.sql.SQLException;
import java.util.List;

/**
 * IReviewService — contrat de la couche review.
 */
public interface IReviewService {

    // ── CRUD Review ────────────────────────────────────────────
    Review createReview(Review review) throws SQLException;
    Review getReviewById(int id) throws SQLException;
    void updateReview(Review review) throws SQLException;
    void deleteReview(int id, int userId) throws SQLException;

    // ── Listing ────────────────────────────────────────────────
    List<Review> getAllReviews() throws SQLException;
    List<Review> getReviewsByPlanning(int idPlanning) throws SQLException;
    List<Review> getReviewsByUser(int userId) throws SQLException;
    List<Review> getReviewsBySentiment(String sentiment) throws SQLException;

    // ── Validation ─────────────────────────────────────────────
    boolean hasUserReviewedPlanning(int userId, int idPlanning) throws SQLException;

    // ── Statistiques ───────────────────────────────────────────
    double getAverageRating(int idPlanning) throws SQLException;
    int countReviews(int idPlanning) throws SQLException;
}
