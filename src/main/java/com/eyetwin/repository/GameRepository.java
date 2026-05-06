package com.eyetwin.repository;

import com.eyetwin.entities.Game;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameRepository {

    public List<Game> findAllOrderedByName() {
        String sql = """
            SELECT id, name, slug, icon, color, description, created_at
            FROM game
            ORDER BY name ASC
            """;

        List<Game> games = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                games.add(mapGame(rs));
            }
        } catch (SQLException e) {
            System.err.println("[GameRepository] findAllOrderedByName error: " + e.getMessage());
        }

        return games;
    }

    public Game findById(int id) {
        String sql = """
            SELECT id, name, slug, icon, color, description, created_at
            FROM game
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGame(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GameRepository] findById error: " + e.getMessage());
        }

        return null;
    }

    public Game save(Game game) {
        String sql = """
            INSERT INTO game (id, name, slug, icon, color, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (game.getId() == 0) {
                game.setId(nextId(conn));
            }
            bindGameForInsert(ps, game);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GameRepository] save error: " + e.getMessage());
        }

        return game;
    }

    public void update(Game game) {
        if (game == null || game.getId() == 0) return;

        String sql = """
            UPDATE game
            SET name = ?, slug = ?, icon = ?, color = ?, description = ?, created_at = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindGameForUpdate(ps, game);
            ps.setInt(7, game.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GameRepository] update error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM game WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GameRepository] delete error: " + e.getMessage());
        }
    }

    private Game mapGame(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setId(rs.getInt("id"));
        game.setName(rs.getString("name"));
        game.setSlug(rs.getString("slug"));
        game.setIcon(rs.getString("icon"));
        game.setColor(rs.getString("color"));
        game.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            game.setCreatedAt(createdAt.toLocalDateTime());
        }

        return game;
    }

    private void bindGameForInsert(PreparedStatement ps, Game game) throws SQLException {
        ps.setInt(1, game.getId());
        ps.setString(2, game.getName());
        ps.setString(3, game.getSlug());
        ps.setString(4, game.getIcon());
        ps.setString(5, normalizeHexColor(game.getColor()));
        ps.setString(6, game.getDescription());

        LocalDateTime createdAt = game.getCreatedAt() != null ? game.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(7, Timestamp.valueOf(createdAt));
    }

    private void bindGameForUpdate(PreparedStatement ps, Game game) throws SQLException {
        ps.setString(1, game.getName());
        ps.setString(2, game.getSlug());
        ps.setString(3, game.getIcon());
        ps.setString(4, normalizeHexColor(game.getColor()));
        ps.setString(5, game.getDescription());

        LocalDateTime createdAt = game.getCreatedAt() != null ? game.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(6, Timestamp.valueOf(createdAt));
    }

    private String normalizeHexColor(String color) {
        if (color == null || color.isBlank()) return "#0a8cc9";
        String v = color.trim();

        if (v.matches("^#[0-9a-fA-F]{6}$")) {
            return v;
        }
        if (v.matches("^#[0-9a-fA-F]{8}$")) {
            return v.substring(0, 7);
        }

        // Handles JavaFX format like 0x0a8cc9ff
        if (v.matches("^0x[0-9a-fA-F]{8}$")) {
            return "#" + v.substring(2, 8);
        }

        return "#0a8cc9";
    }

    private int nextId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM game";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        }
        return 1;
    }
}
