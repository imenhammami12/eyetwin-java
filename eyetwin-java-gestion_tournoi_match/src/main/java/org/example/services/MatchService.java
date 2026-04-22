package org.example.services;

import org.example.entities.Match;
import org.example.interfaces.IService;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchService implements IService<Match> {
    private Connection connection;

    public MatchService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Match match) {
        String query = "INSERT INTO matches (equipe1, equipe2, score, date_match, prix, tournoi_id, play_mode, localisation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, match.getEquipe1());
            pst.setString(2, match.getEquipe2());
            pst.setInt(3, match.getScore());
            pst.setDate(4, Date.valueOf(match.getDateMatch()));
            pst.setString(5, match.getPrix());
            pst.setInt(6, match.getTournoiId());
            pst.setString(7, match.getPlayMode());
            pst.setString(8, match.getLocalisation());
            pst.executeUpdate();
            System.out.println("Match added successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Match match) {
        String query = "UPDATE matches SET equipe1=?, equipe2=?, score=?, date_match=?, prix=?, tournoi_id=?, play_mode=?, localisation=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, match.getEquipe1());
            pst.setString(2, match.getEquipe2());
            pst.setInt(3, match.getScore());
            pst.setDate(4, Date.valueOf(match.getDateMatch()));
            pst.setString(5, match.getPrix());
            pst.setInt(6, match.getTournoiId());
            pst.setString(7, match.getPlayMode());
            pst.setString(8, match.getLocalisation());
            pst.setInt(9, match.getId());
            pst.executeUpdate();
            System.out.println("Match updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM matches WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Match deleted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Match> getAll() {
        List<Match> list = new ArrayList<>();
        String query = "SELECT * FROM matches";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
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
    public Match getOne(int id) {
        String query = "SELECT * FROM matches WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
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
}
