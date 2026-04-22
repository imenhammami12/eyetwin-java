package com.eyetwin.entities;

import java.time.LocalDateTime;

public class TournoiInscription {
    private int id;
    private int userId;
    private int tournoiId;
    private String stripeSessionId;
    private String status; // PENDING, PAID, CANCELLED
    private LocalDateTime createdAt;

    // Optional: for convenience
    private User user;
    private Tournoi tournoi;

    public TournoiInscription() {
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public TournoiInscription(int userId, int tournoiId, String stripeSessionId) {
        this();
        this.userId = userId;
        this.tournoiId = tournoiId;
        this.stripeSessionId = stripeSessionId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTournoiId() { return tournoiId; }
    public void setTournoiId(int tournoiId) { this.tournoiId = tournoiId; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Tournoi getTournoi() { return tournoi; }
    public void setTournoi(Tournoi tournoi) { this.tournoi = tournoi; }
}
