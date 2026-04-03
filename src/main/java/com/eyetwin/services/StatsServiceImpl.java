package com.eyetwin.services;

import com.eyetwin.interfaces.IStatsService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;

/**
 * StatsServiceImpl — implémentation de IStatsService.
 * Fusionne l'ancien StatsDAO (accès SQL) + logique métier.
 */
public class StatsServiceImpl implements IStatsService {

    @Override
    public int countPlayers() {
        return count("SELECT COUNT(*) FROM `user` WHERE roles_json NOT LIKE '%ROLE_ADMIN%'");
    }

    @Override
    public int countCoaches() {
        return count("SELECT COUNT(*) FROM `user` WHERE roles_json LIKE '%ROLE_COACH%'");
    }

    @Override
    public int countTeams() {
        return count("SELECT COUNT(*) FROM `team`");
    }

    @Override
    public int countTournaments() {
        return count("SELECT COUNT(*) FROM `tournoi`");
    }

    // ════════════════════════════════════════════════════════════
    //  HELPER PRIVÉ
    // ════════════════════════════════════════════════════════════

    private int count(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ StatsServiceImpl: " + e.getMessage());
        }
        return 0;
    }
}
