package com.eyetwin.dao;

import com.eyetwin.config.DatabaseConfig;
import com.eyetwin.model.User;
import java.sql.*;

public class UserDAO {

    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEmail: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById: " + e.getMessage());
        }
        return null;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT id FROM user WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Erreur emailExists: " + e.getMessage());
            return false;
        }
    }

    public boolean save(String fullName, String email, String hashedPassword) {
        String sql = "INSERT INTO `user` " +
                "(email, username, roles_json, password, account_status, " +
                "full_name, created_at, last_login, coin_balance, is_totp_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0, 0)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Génère un username unique depuis l'email (ex: john.doe@gmail.com → john_doe_1234)
            String username = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + (int)(Math.random() * 9000 + 1000);

            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, "[\"ROLE_USER\"]");
            stmt.setString(4, hashedPassword);
            stmt.setString(5, "ACTIVE");
            stmt.setString(6, fullName);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Erreur save: " + e.getMessage());
            return false;
        }
    }
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setUsername(rs.getString("username"));
        user.setFirstName(rs.getString("full_name"));   // full_name → firstName
        user.setLastName("");                            // pas de last_name dans la DB
        user.setRolesJson(rs.getString("roles_json"));  // ← corrigé
        user.setAccountStatus(rs.getString("account_status"));
        user.setCoins(rs.getInt("coin_balance"));        // ← corrigé
        return user;
    }
}