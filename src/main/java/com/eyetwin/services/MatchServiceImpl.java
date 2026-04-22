package com.eyetwin.services;

import com.eyetwin.entities.Match;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchServiceImpl implements IMatchService {

    public MatchServiceImpl() {
        ensureSchema();
    }

    private void ensureSchema() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (conn == null) return;
            DatabaseMetaData metaData = conn.getMetaData();
            
            checkAndAddColumn(conn, metaData, "play_mode", "VARCHAR(255) DEFAULT 'Online'");
            checkAndAddColumn(conn, metaData, "localisation", "VARCHAR(255) DEFAULT ''");
            checkAndAddColumn(conn, metaData, "prix", "VARCHAR(255) DEFAULT 'Free'");
            
        } catch (SQLException e) {
            System.err.println("[MatchService] ❌ Erreur lors de la vérification du schéma : " + e.getMessage());
        }
    }

    private void checkAndAddColumn(Connection conn, DatabaseMetaData metaData, String columnName, String type) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, "matches", columnName)) {
            if (!rs.next()) {
                System.out.println("[MatchService] Colonne '" + columnName + "' manquante. Ajout en cours...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE matches ADD COLUMN " + columnName + " " + type);
                    System.out.println("[MatchService] ✅ Colonne '" + columnName + "' ajoutée avec succès.");
                }
            }
        }
    }

    @Override
    public void add(Match match) {
        String query = "INSERT INTO matches (equipe1, equipe2, score, date_match, prix, tournoi_id, play_mode, localisation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, match.getEquipe1());
            stmt.setString(2, match.getEquipe2());
            stmt.setInt(3, match.getScore());
            stmt.setDate(4, Date.valueOf(match.getDateMatch()));
            stmt.setString(5, match.getPrix());
            stmt.setInt(6, match.getTournoiId());
            stmt.setString(7, match.getPlayMode());
            stmt.setString(8, match.getLocalisation());
            stmt.executeUpdate();
            System.out.println("Match ajouté avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Match match) {
        String query = "UPDATE matches SET equipe1=?, equipe2=?, score=?, date_match=?, prix=?, tournoi_id=?, play_mode=?, localisation=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, match.getEquipe1());
            stmt.setString(2, match.getEquipe2());
            stmt.setInt(3, match.getScore());
            stmt.setDate(4, Date.valueOf(match.getDateMatch()));
            stmt.setString(5, match.getPrix());
            stmt.setInt(6, match.getTournoiId());
            stmt.setString(7, match.getPlayMode());
            stmt.setString(8, match.getLocalisation());
            stmt.setInt(9, match.getId());
            stmt.executeUpdate();
            System.out.println("Match modifié avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM matches WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("Match supprimé avec succès.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Match getById(int id) {
        String query = "SELECT * FROM matches WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Match m = new Match();
                    m.setId(rs.getInt("id"));
                    m.setEquipe1(rs.getString("equipe1"));
                    m.setEquipe2(rs.getString("equipe2"));
                    m.setScore(rs.getInt("score"));
                    m.setDateMatch(rs.getDate("date_match").toLocalDate());
                    m.setPrix(rs.getString("prix"));
                    m.setTournoiId(rs.getInt("tournoi_id"));
                    m.setPlayMode(rs.getString("play_mode"));
                    m.setLocalisation(rs.getString("localisation"));
                    return m;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Match> getAll() {
        List<Match> list = new ArrayList<>();
        String query = "SELECT * FROM matches";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Match m = new Match();
                m.setId(rs.getInt("id"));
                m.setEquipe1(rs.getString("equipe1"));
                m.setEquipe2(rs.getString("equipe2"));
                m.setScore(rs.getInt("score"));
                m.setDateMatch(rs.getDate("date_match").toLocalDate());
                m.setPrix(rs.getString("prix"));
                m.setTournoiId(rs.getInt("tournoi_id"));
                m.setPlayMode(rs.getString("play_mode"));
                m.setLocalisation(rs.getString("localisation"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Match> getByTournoi(int tournoiId) {
        List<Match> list = new ArrayList<>();
        String query = "SELECT * FROM matches WHERE tournoi_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, tournoiId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Match m = new Match();
                    m.setId(rs.getInt("id"));
                    m.setEquipe1(rs.getString("equipe1"));
                    m.setEquipe2(rs.getString("equipe2"));
                    m.setScore(rs.getInt("score"));
                    m.setDateMatch(rs.getDate("date_match").toLocalDate());
                    m.setPrix(rs.getString("prix"));
                    m.setTournoiId(rs.getInt("tournoi_id"));
                    m.setPlayMode(rs.getString("play_mode"));
                    m.setLocalisation(rs.getString("localisation"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
