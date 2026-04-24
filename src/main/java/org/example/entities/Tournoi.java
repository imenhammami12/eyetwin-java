package org.example.entities;

import java.time.LocalDate;

public class Tournoi {
    private int id;
    private String nom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String description;
    private String image;
    private TypeTournoi typeTournoi;
    private double prix;

    public Tournoi() {
    }

    public Tournoi(String nom, LocalDate dateDebut, LocalDate dateFin, String description, String image, TypeTournoi typeTournoi, double prix) {
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.description = description;
        this.image = image;
        this.typeTournoi = typeTournoi;
        this.prix = prix;
    }

    public Tournoi(int id, String nom, LocalDate dateDebut, LocalDate dateFin, String description, String image, TypeTournoi typeTournoi, double prix) {
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

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public TypeTournoi getTypeTournoi() { return typeTournoi; }
    public void setTypeTournoi(TypeTournoi typeTournoi) { this.typeTournoi = typeTournoi; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    @Override
    public String toString() {
        return nom; // For easy display in Dropdowns
    }
}
