package com.eyetwin.services;

import com.eyetwin.entities.Tournoi;
import com.eyetwin.entities.TypeTournoi;
import com.eyetwin.interfaces.ITournoiService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournoiServiceImpl implements ITournoiService {

    public TournoiServiceImpl() {
        ensureSchema();
    }

    private void ensureSchema() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (conn == null) return;
            
            // 1. Check if column 'prix' exists in 'tournoi'
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, "tournoi", "prix")) {
                if (!rs.next()) {
                    System.out.println("[TournoiService] Colonne 'prix' manquante. Ajout en cours...");
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE tournoi ADD COLUMN prix DOUBLE NOT NULL DEFAULT 0.0");
                        System.out.println("[TournoiService] ✅ Colonne 'prix' ajoutée avec succès.");
                    }
                }
            }

            // 2. Create 'inscription_tournoi' table
            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS inscription_tournoi (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id INT NOT NULL, " +
                        "tournoi_id INT NOT NULL, " +
                        "stripe_session_id VARCHAR(255), " +
                        "status VARCHAR(50) DEFAULT 'PENDING', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB";
                stmt.execute(sql);
                System.out.println("[TournoiService] ✅ Table 'inscription_tournoi' vérifiée/créée.");
            }
        } catch (SQLException e) {
            System.err.println("[TournoiService] ❌ Erreur lors de la vérification du schéma : " + e.getMessage());
        }
    }

    @Override
    public void add(Tournoi tournoi) {
        String query = "INSERT INTO tournoi (nom, date_debut, date_fin, description, image, type_tournoi, prix) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tournoi.getNom());
            stmt.setDate(2, Date.valueOf(tournoi.getDateDebut()));
            stmt.setDate(3, Date.valueOf(tournoi.getDateFin()));
            stmt.setString(4, tournoi.getDescription());
            stmt.setString(5, tournoi.getImage());
            stmt.setString(6, tournoi.getTypeTournoi() != null ? tournoi.getTypeTournoi().name() : null);
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
            stmt.setDate(2, Date.valueOf(tournoi.getDateDebut()));
            stmt.setDate(3, Date.valueOf(tournoi.getDateFin()));
            stmt.setString(4, tournoi.getDescription());
            stmt.setString(5, tournoi.getImage());
            stmt.setString(6, tournoi.getTypeTournoi() != null ? tournoi.getTypeTournoi().name() : null);
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
            if (e.getMessage().contains("a foreign key constraint fails")) {
                throw new RuntimeException("Vous ne pouvez pas supprimer ce tournoi car il contient encore des matchs associés. Veuillez d'abord supprimer ses matchs.");
            }
            e.printStackTrace();
        }
    }

    @Override
    public Tournoi getById(int id) {
        String query = "SELECT * FROM tournoi WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
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
                            t.setTypeTournoi(TypeTournoi.valueOf(typeStr.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            t.setTypeTournoi(TypeTournoi.AUTRE);
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

    @Override
    public List<Tournoi> getAll() {
        List<Tournoi> list = new ArrayList<>();
        String query = "SELECT * FROM tournoi";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
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
                        t.setTypeTournoi(TypeTournoi.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        t.setTypeTournoi(TypeTournoi.AUTRE);
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
}
