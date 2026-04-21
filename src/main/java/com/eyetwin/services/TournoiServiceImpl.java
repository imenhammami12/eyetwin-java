package com.eyetwin.services;

import com.eyetwin.entities.Tournoi;
import com.eyetwin.interfaces.ITournoiService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournoiServiceImpl implements ITournoiService {

    @Override
    public void add(Tournoi tournoi) {
        String query = "INSERT INTO tournoi (nom, date_debut, date_fin, description, image, type_tournoi, prix) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tournoi.getNom());
            stmt.setDate(2, tournoi.getDateDebut());
            stmt.setDate(3, tournoi.getDateFin());
            stmt.setString(4, tournoi.getDescription());
            stmt.setString(5, tournoi.getImage());
            stmt.setString(6, tournoi.getTypeTournoi());
            stmt.setDouble(7, tournoi.getPrix());
            stmt.executeUpdate();
            System.out.println("Tournoi ajouté avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Tournoi tournoi) {
        String query = "UPDATE tournoi SET nom=?, date_debut=?, date_fin=?, description=?, image=?, type_tournoi=?, prix=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tournoi.getNom());
            stmt.setDate(2, tournoi.getDateDebut());
            stmt.setDate(3, tournoi.getDateFin());
            stmt.setString(4, tournoi.getDescription());
            stmt.setString(5, tournoi.getImage());
            stmt.setString(6, tournoi.getTypeTournoi());
            stmt.setDouble(7, tournoi.getPrix());
            stmt.setInt(8, tournoi.getId());
            stmt.executeUpdate();
            System.out.println("Tournoi modifié avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM tournoi WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Tournoi supprimé avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Tournoi getById(int id) {
        String query = "SELECT * FROM tournoi WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Tournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("description"),
                        rs.getString("image"),
                        rs.getString("type_tournoi"),
                        rs.getDouble("prix")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Tournoi> getAll() {
        List<Tournoi> list = new ArrayList<>();
        String query = "SELECT * FROM tournoi";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Tournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("description"),
                        rs.getString("image"),
                        rs.getString("type_tournoi"),
                        rs.getDouble("prix")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
