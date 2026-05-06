package com.eyetwin.repository;

import com.eyetwin.entities.Agent;
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

public class AgentRepository {

    public List<Agent> findAll() {
        String sql = """
            SELECT a.id, a.game_id, a.name, a.slug, a.image, a.description, a.created_at,
                   g.name AS game_name, g.slug AS game_slug, g.icon AS game_icon,
                   g.color AS game_color, g.description AS game_description, g.created_at AS game_created_at
            FROM agent a
            LEFT JOIN game g ON g.id = a.game_id
            ORDER BY a.name ASC
            """;

        List<Agent> agents = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                agents.add(mapAgent(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AgentRepository] findAll error: " + e.getMessage());
        }

        return agents;
    }

    public List<Agent> findByGame(Game game) {
        if (game == null) return List.of();

        String sql = """
            SELECT a.id, a.game_id, a.name, a.slug, a.image, a.description, a.created_at,
                   g.name AS game_name, g.slug AS game_slug, g.icon AS game_icon,
                   g.color AS game_color, g.description AS game_description, g.created_at AS game_created_at
            FROM agent a
            LEFT JOIN game g ON g.id = a.game_id
            WHERE a.game_id = ?
            ORDER BY a.name ASC
            """;

        List<Agent> agents = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, game.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    agents.add(mapAgent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AgentRepository] findByGame error: " + e.getMessage());
        }

        return agents;
    }

    public Agent findById(int id) {
        String sql = """
            SELECT a.id, a.game_id, a.name, a.slug, a.image, a.description, a.created_at,
                   g.name AS game_name, g.slug AS game_slug, g.icon AS game_icon,
                   g.color AS game_color, g.description AS game_description, g.created_at AS game_created_at
            FROM agent a
            LEFT JOIN game g ON g.id = a.game_id
            WHERE a.id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAgent(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AgentRepository] findById error: " + e.getMessage());
        }

        return null;
    }

    public Agent save(Agent agent) {
        String sql = """
            INSERT INTO agent (id, game_id, name, slug, image, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (agent.getId() == 0) {
                agent.setId(nextId(conn));
            }
            bindAgentForInsert(ps, agent);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AgentRepository] save error: " + e.getMessage());
        }

        return agent;
    }

    public void update(Agent agent) {
        if (agent == null || agent.getId() == 0) return;

        String sql = """
            UPDATE agent
            SET game_id = ?, name = ?, slug = ?, image = ?, description = ?, created_at = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindAgentForUpdate(ps, agent);
            ps.setInt(7, agent.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AgentRepository] update error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM agent WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AgentRepository] delete error: " + e.getMessage());
        }
    }

    private Agent mapAgent(ResultSet rs) throws SQLException {
        Agent agent = new Agent();
        agent.setId(rs.getInt("id"));
        agent.setName(rs.getString("name"));
        agent.setSlug(rs.getString("slug"));
        agent.setImage(rs.getString("image"));
        agent.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            agent.setCreatedAt(createdAt.toLocalDateTime());
        }

        int gameId = rs.getInt("game_id");
        if (!rs.wasNull()) {
            Game game = new Game();
            game.setId(gameId);
            game.setName(rs.getString("game_name"));
            game.setSlug(rs.getString("game_slug"));
            game.setIcon(rs.getString("game_icon"));
            game.setColor(rs.getString("game_color"));
            game.setDescription(rs.getString("game_description"));

            Timestamp gameCreatedAt = rs.getTimestamp("game_created_at");
            if (gameCreatedAt != null) {
                game.setCreatedAt(gameCreatedAt.toLocalDateTime());
            }

            agent.setGame(game);
        }

        return agent;
    }

    private void bindAgentForInsert(PreparedStatement ps, Agent agent) throws SQLException {
        ps.setInt(1, agent.getId());

        if (agent.getGame() != null) {
            ps.setInt(2, agent.getGame().getId());
        } else {
            ps.setNull(2, java.sql.Types.INTEGER);
        }

        ps.setString(3, agent.getName());
        ps.setString(4, agent.getSlug());
        ps.setString(5, agent.getImage());
        ps.setString(6, agent.getDescription());

        LocalDateTime createdAt = agent.getCreatedAt() != null ? agent.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(7, Timestamp.valueOf(createdAt));
    }

    private void bindAgentForUpdate(PreparedStatement ps, Agent agent) throws SQLException {
        if (agent.getGame() != null) {
            ps.setInt(1, agent.getGame().getId());
        } else {
            ps.setNull(1, java.sql.Types.INTEGER);
        }

        ps.setString(2, agent.getName());
        ps.setString(3, agent.getSlug());
        ps.setString(4, agent.getImage());
        ps.setString(5, agent.getDescription());

        LocalDateTime createdAt = agent.getCreatedAt() != null ? agent.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(6, Timestamp.valueOf(createdAt));
    }

    private int nextId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM agent";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        }
        return 1;
    }
}
