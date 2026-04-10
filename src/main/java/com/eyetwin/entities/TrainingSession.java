package com.eyetwin.entities;

import java.time.LocalDateTime;

/**
 * TrainingSession.java — Miroir exact de la table training_session
 *
 * Colonnes DB :
 *   idtraining, status, joined_at,
 *   ID_planning, IDcurrent_user
 */
public class TrainingSession {

    private int           idTraining;
    private String        status;
    private LocalDateTime joinedAt;
    private int           idPlanning;
    private int           idCurrentUser;

    // Relations chargées à la demande
    private Planning planning;
    private User     user;

    // ─── Constructeurs ───────────────────────────────────────────
    public TrainingSession() {
        this.joinedAt = LocalDateTime.now();
        this.status   = "en attente";
    }

    public TrainingSession(int idTraining, String status, LocalDateTime joinedAt,
                           int idPlanning, int idCurrentUser) {
        this.idTraining     = idTraining;
        this.status         = status;
        this.joinedAt       = joinedAt;
        this.idPlanning     = idPlanning;
        this.idCurrentUser  = idCurrentUser;
    }

    // ─── Getters / Setters ───────────────────────────────────────
    public int getIdTraining() {
        return idTraining;
    }

    public void setIdTraining(int idTraining) {
        this.idTraining = idTraining;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public int getIdCurrentUser() {
        return idCurrentUser;
    }

    public void setIdCurrentUser(int idCurrentUser) {
        this.idCurrentUser = idCurrentUser;
    }

    public Planning getPlanning() {
        return planning;
    }

    public void setPlanning(Planning planning) {
        this.planning = planning;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // ─── Helpers ─────────────────────────────────────────────────
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void complete() {
        this.status = "COMPLETED";
    }

    @Override
    public String toString() {
        return "TrainingSession{" +
                "idTraining=" + idTraining +
                ", status='" + status + '\'' +
                ", idPlanning=" + idPlanning +
                ", idCurrentUser=" + idCurrentUser +
                '}';
    }
}
