package com.eyetwin.dao;

import com.eyetwin.config.DatabaseConfig;
import java.sql.*;

public class StatsDAO {

    public int countPlayers() {
        return count("SELECT COUNT(*) FROM `user` " +
                "WHERE roles_json NOT LIKE '%ROLE_ADMIN%'");
    }

    public int countCoaches() {
        return count("SELECT COUNT(*) FROM `user` " +
                "WHERE roles_json LIKE '%ROLE_COACH%'");
    }

    public int countTeams() {
        // Adaptez le nom de la table si différent
        return count("SELECT COUNT(*) FROM `team`");
    }

    public int countTournaments() {
        return count("SELECT COUNT(*) FROM `tournoi`");
    }

    private int count(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ StatsDAO: " + e.getMessage());
        }
        return 0;
    }
}