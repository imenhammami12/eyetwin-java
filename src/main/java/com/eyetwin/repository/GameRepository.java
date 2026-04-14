package com.eyetwin.repository;

import com.eyetwin.entities.Game;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameRepository {

    public List<Game> findAllOrderedByName() {
        String sql = """
            SELECT id, name, description, icon
            FROM game
            ORDER BY name ASC
            """;

        List<Game> games = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Game game = new Game();
                game.setId(rs.getInt("id"));
                game.setName(rs.getString("name"));
                game.setDescription(rs.getString("description"));
                game.setIcon(rs.getString("icon"));
                games.add(game);
            }
        } catch (SQLException e) {
            System.err.println("[GameRepository] findAllOrderedByName error: " + e.getMessage());
        }

        return games;
    }
}
