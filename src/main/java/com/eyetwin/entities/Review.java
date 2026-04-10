package com.eyetwin.entities;

import java.time.LocalDateTime;

/**
 * Review.java — Miroir exact de la table review
 *
 * Colonnes DB :
 *   id, content, rating, created_at, sentiment,
 *   user_id, ID_planning
 */
public class Review {

    private int           id;
    private String        content;
    private int           rating;
    private LocalDateTime createdAt;
    private String        sentiment;
    private int           userId;
    private int           idPlanning;

    // Relations chargées à la demande
    private User     user;
    private Planning planning;

    // ─── Constructeurs ───────────────────────────────────────────
    public Review() {
        this.createdAt = LocalDateTime.now();
    }

    public Review(int id, String content, int rating, LocalDateTime createdAt,
                  String sentiment, int userId, int idPlanning) {
        this.id         = id;
        this.content    = content;
        this.rating     = rating;
        this.createdAt  = createdAt;
        this.sentiment  = sentiment;
        this.userId     = userId;
        this.idPlanning = idPlanning;
    }

    // ─── Getters / Setters ───────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Planning getPlanning() {
        return planning;
    }

    public void setPlanning(Planning planning) {
        this.planning = planning;
    }

    // ─── Helpers ─────────────────────────────────────────────────
    public String getSentimentBadgeClass() {
        if (sentiment == null) return "neutral";
        return switch (sentiment.toLowerCase()) {
            case "positive" -> "positive";
            case "negative" -> "negative";
            default -> "neutral";
        };
    }

    public boolean isPositive() {
        return "positive".equalsIgnoreCase(sentiment);
    }

    public boolean isNegative() {
        return "negative".equalsIgnoreCase(sentiment);
    }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", rating=" + rating +
                ", sentiment='" + sentiment + '\'' +
                ", userId=" + userId +
                ", idPlanning=" + idPlanning +
                '}';
    }
}
