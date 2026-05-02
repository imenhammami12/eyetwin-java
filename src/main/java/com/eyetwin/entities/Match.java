package com.eyetwin.entities;

import java.time.LocalDate;

public class Match {
    private int id;
    private String equipe1;
    private String equipe2;
    private int score;
    private LocalDate dateMatch;
    private String prix;
    private int tournoiId;
    private String playMode;
    private String localisation;

    public Match() {
        this.playMode = "En Ligne"; // Default
    }

    public Match(String equipe1, String equipe2, int score, LocalDate dateMatch, String prix, int tournoiId, String playMode, String localisation) {
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.score = score;
        this.dateMatch = dateMatch;
        this.prix = prix;
        this.tournoiId = tournoiId;
        this.playMode = playMode;
        this.localisation = localisation;
    }

    public Match(int id, String equipe1, String equipe2, int score, LocalDate dateMatch, String prix, int tournoiId, String playMode, String localisation) {
        this.id = id;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
        this.score = score;
        this.dateMatch = dateMatch;
        this.prix = prix;
        this.tournoiId = tournoiId;
        this.playMode = playMode;
        this.localisation = localisation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getEquipe1() { return equipe1; }
    public void setEquipe1(String equipe1) { this.equipe1 = equipe1; }
    public String getEquipe2() { return equipe2; }
    public void setEquipe2(String equipe2) { this.equipe2 = equipe2; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public LocalDate getDateMatch() { return dateMatch; }
    public void setDateMatch(LocalDate dateMatch) { this.dateMatch = dateMatch; }
    public String getPrix() { return prix; }
    public void setPrix(String prix) { this.prix = prix; }
    public int getTournoiId() { return tournoiId; }
    public void setTournoiId(int tournoiId) { this.tournoiId = tournoiId; }
    public String getPlayMode() { return playMode; }
    public void setPlayMode(String playMode) { this.playMode = playMode; }
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", equipe1='" + equipe1 + '\'' +
                ", equipe2='" + equipe2 + '\'' +
                ", score=" + score +
                ", dateMatch=" + dateMatch +
                '}';
    }
}
