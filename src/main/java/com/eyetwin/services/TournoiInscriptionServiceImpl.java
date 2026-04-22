package com.eyetwin.services;

import com.eyetwin.entities.TournoiInscription;
import com.eyetwin.interfaces.ITournoiInscriptionService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournoiInscriptionServiceImpl implements ITournoiInscriptionService {

    @Override
    public void add(TournoiInscription inscription) {
        String query = "INSERT INTO inscription_tournoi (user_id, tournoi_id, stripe_session_id, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, inscription.getUserId());
            stmt.setInt(2, inscription.getTournoiId());
            stmt.setString(3, inscription.getStripeSessionId());
            stmt.setString(4, inscription.getStatus());
            stmt.setTimestamp(5, Timestamp.valueOf(inscription.getCreatedAt()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) inscription.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateStatus(int id, String status) {
        String query = "UPDATE inscription_tournoi SET status=? WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateStatusBySession(String sessionId, String status) {
        String query = "UPDATE inscription_tournoi SET status=? WHERE stripe_session_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public TournoiInscription getById(int id) {
        String query = "SELECT * FROM inscription_tournoi WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToInscription(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public TournoiInscription getBySession(String sessionId) {
        String query = "SELECT * FROM inscription_tournoi WHERE stripe_session_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToInscription(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TournoiInscription> getByUser(int userId) {
        List<TournoiInscription> list = new ArrayList<>();
        String query = "SELECT * FROM inscription_tournoi WHERE user_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToInscription(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<TournoiInscription> getByTournoi(int tournoiId) {
        List<TournoiInscription> list = new ArrayList<>();
        String query = "SELECT * FROM inscription_tournoi WHERE tournoi_id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, tournoiId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToInscription(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean isUserRegistered(int userId, int tournoiId) {
        String query = "SELECT COUNT(*) FROM inscription_tournoi WHERE user_id=? AND tournoi_id=? AND status='PAID'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, tournoiId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private TournoiInscription mapResultSetToInscription(ResultSet rs) throws SQLException {
        TournoiInscription ins = new TournoiInscription();
        ins.setId(rs.getInt("id"));
        ins.setUserId(rs.getInt("user_id"));
        ins.setTournoiId(rs.getInt("tournoi_id"));
        ins.setStripeSessionId(rs.getString("stripe_session_id"));
        ins.setStatus(rs.getString("status"));
        ins.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return ins;
    }
}
