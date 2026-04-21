package com.eyetwin.services;

import com.eyetwin.entities.Match;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchServiceImpl implements IMatchService {

    @Override
    public void add(Match match) {
        String query = "INSERT INTO matches (equipe1, equipe2, score, date_match, prix, tournoi_id, play_mode, localisation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, match.getEquipe1());
            stmt.setString(2, match.getEquipe2());
            stmt.setInt(3, match.getScore());
            stmt.setDate(4, match.getDateMatch());
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
            stmt.setDate(4, match.getDateMatch());
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
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Match(
                        rs.getInt("id"),
                        rs.getString("equipe1"),
                        rs.getString("equipe2"),
                        rs.getInt("score"),
                        rs.getDate("date_match"),
                        rs.getString("prix"),
                        rs.getInt("tournoi_id"),
                        rs.getString("play_mode"),
                        rs.getString("localisation")
                );
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
                list.add(new Match(
                        rs.getInt("id"),
                        rs.getString("equipe1"),
                        rs.getString("equipe2"),
                        rs.getInt("score"),
                        rs.getDate("date_match"),
                        rs.getString("prix"),
                        rs.getInt("tournoi_id"),
                        rs.getString("play_mode"),
                        rs.getString("localisation")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
