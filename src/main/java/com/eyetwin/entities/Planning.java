package com.eyetwin.entities;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Planning.java — Miroir exact de la table planning
 *
 * Colonnes DB :
 *   IDplanning, image, date, time, localisation, description,
 *   need_partner, level, type
 */
public class Planning {

    private int       idPlanning;
    private String    image;
    private LocalDate date;
    private LocalTime time;
    private String    localisation;
    private String    description;
    private boolean   needPartner;
    private String    level;
    private String    type;

    // Relations chargées à la demande
    private List<Review> reviews = new ArrayList<>();
    private List<TrainingSession> trainingSessions = new ArrayList<>();

    // ─── Constructeurs ───────────────────────────────────────────
    public Planning() {
    }

    public Planning(int idPlanning, String image, LocalDate date, LocalTime time,
                    String localisation, String description, boolean needPartner,
                    String level, String type) {
        this.idPlanning   = idPlanning;
        this.image        = image;
        this.date         = date;
        this.time         = time;
        this.localisation = localisation;
        this.description  = description;
        this.needPartner  = needPartner;
        this.level        = level;
        this.type         = type;
    }

    // ─── Getters / Setters ───────────────────────────────────────
    public int getIdPlanning() {
        return idPlanning;
    }

    public void setIdPlanning(int idPlanning) {
        this.idPlanning = idPlanning;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isNeedPartner() {
        return needPartner;
    }

    public void setNeedPartner(boolean needPartner) {
        this.needPartner = needPartner;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Optional<PlanningLevel> getLevelEnum() {
        return PlanningLevel.fromDbValue(level);
    }

    public void setLevelEnum(PlanningLevel level) {
        this.level = level == null ? null : level.getDbValue();
    }

    public Optional<PlanningType> getTypeEnum() {
        return PlanningType.fromDbValue(type);
    }

    public void setTypeEnum(PlanningType type) {
        this.type = type == null ? null : type.getDbValue();
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<TrainingSession> getTrainingSessions() {
        return trainingSessions;
    }

    public void setTrainingSessions(List<TrainingSession> trainingSessions) {
        this.trainingSessions = trainingSessions;
    }

    // ─── Helpers ─────────────────────────────────────────────────
    public int getParticipantCount() {
        return (int) trainingSessions.stream()
                .filter(ts -> ts.getStatus() != null && !"CANCELLED".equalsIgnoreCase(ts.getStatus()))
                .count();
    }

    public boolean hasUserJoined(int userId) {
        return trainingSessions.stream()
                .anyMatch(ts -> ts.getIdCurrentUser() == userId
                        && ts.getStatus() != null
                        && !"CANCELLED".equalsIgnoreCase(ts.getStatus()));
    }

    @Override
    public String toString() {
        return "Planning{" +
                "idPlanning=" + idPlanning +
                ", date=" + date +
                ", time=" + time +
                ", type='" + type + '\'' +
                ", level='" + level + '\'' +
                '}';
    }
}
