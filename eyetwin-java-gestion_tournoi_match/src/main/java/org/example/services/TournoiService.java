package org.example.services;

import org.example.entities.Tournoi;
import org.example.interfaces.IService;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournoiService implements IService<Tournoi> {
    private Connection connection;

    public TournoiService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Tournoi tournoi) {
        String query = "INSERT INTO tournoi (nom, date_debut, date_fin, description, image, type_tournoi, prix) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, tournoi.getNom());
            pst.setDate(2, Date.valueOf(tournoi.getDateDebut()));
            pst.setDate(3, Date.valueOf(tournoi.getDateFin()));
            pst.setString(4, tournoi.getDescription());
            pst.setString(5, tournoi.getImage());
            pst.setString(6, tournoi.getTypeTournoi() != null ? tournoi.getTypeTournoi().name() : null);
            pst.setDouble(7, tournoi.getPrix());
            pst.executeUpdate();
            System.out.println("Tournoi added successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Tournoi tournoi) {
        String query = "UPDATE tournoi SET nom=?, date_debut=?, date_fin=?, description=?, image=?, type_tournoi=?, prix=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, tournoi.getNom());
            pst.setDate(2, Date.valueOf(tournoi.getDateDebut()));
            pst.setDate(3, Date.valueOf(tournoi.getDateFin()));
            pst.setString(4, tournoi.getDescription());
            pst.setString(5, tournoi.getImage());
            pst.setString(6, tournoi.getTypeTournoi() != null ? tournoi.getTypeTournoi().name() : null);
            pst.setDouble(7, tournoi.getPrix());
            pst.setInt(8, tournoi.getId());
            pst.executeUpdate();
            System.out.println("Tournoi updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM tournoi WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Tournoi deleted successfully");
        } catch (SQLException e) {
            if (e.getMessage().contains("a foreign key constraint fails")) {
                throw new RuntimeException("Vous ne pouvez pas supprimer ce tournoi car il contient encore des matchs associés. Veuillez d'abord supprimer ses matchs.");
            }
            e.printStackTrace();
        }
    }

    @Override
    public List<Tournoi> getAll() {
        List<Tournoi> list = new ArrayList<>();
        String query = "SELECT * FROM tournoi";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Tournoi t = new Tournoi();
                t.setId(rs.getInt("id"));
                t.setNom(rs.getString("nom"));
                t.setDateDebut(rs.getDate("date_debut").toLocalDate());
                t.setDateFin(rs.getDate("date_fin").toLocalDate());
                t.setDescription(rs.getString("description"));
                t.setImage(rs.getString("image"));
                String typeStr = rs.getString("type_tournoi");
                if (typeStr != null) {
                    try {
                        t.setTypeTournoi(org.example.entities.TypeTournoi.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        t.setTypeTournoi(org.example.entities.TypeTournoi.AUTRE);
                    }
                }
                t.setPrix(rs.getDouble("prix"));
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Tournoi getOne(int id) {
        String query = "SELECT * FROM tournoi WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Tournoi t = new Tournoi();
                    t.setId(rs.getInt("id"));
                    t.setNom(rs.getString("nom"));
                    t.setDateDebut(rs.getDate("date_debut").toLocalDate());
                    t.setDateFin(rs.getDate("date_fin").toLocalDate());
                    t.setDescription(rs.getString("description"));
                    t.setImage(rs.getString("image"));
                    String typeStr = rs.getString("type_tournoi");
                    if (typeStr != null) {
                        try {
                            t.setTypeTournoi(org.example.entities.TypeTournoi.valueOf(typeStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            t.setTypeTournoi(org.example.entities.TypeTournoi.AUTRE);
                        }
                    }
                    t.setPrix(rs.getDouble("prix"));
                    return t;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
