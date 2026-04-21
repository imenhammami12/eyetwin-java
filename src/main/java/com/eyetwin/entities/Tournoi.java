package com.eyetwin.entities;

import java.sql.Date;

public class Tournoi {
    private int id;
    private String nom;
    private Date dateDebut;
    private Date dateFin;
    private String description;
    private String image;
    private String typeTournoi;
    private double prix;

    public Tournoi() {}

    public Tournoi(int id, String nom, Date dateDebut, Date dateFin, String description, String image, String typeTournoi, double prix) {
        this.id = id;
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.description = description;
        this.image = image;
        this.typeTournoi = typeTournoi;
        this.prix = prix;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getTypeTournoi() { return typeTournoi; }
    public void setTypeTournoi(String typeTournoi) { this.typeTournoi = typeTournoi; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    @Override
    public String toString() {
        return "Tournoi{id=" + id + ", nom='" + nom + "', dateDebut=" + dateDebut + ", prix=" + prix + "}";
    }
}
