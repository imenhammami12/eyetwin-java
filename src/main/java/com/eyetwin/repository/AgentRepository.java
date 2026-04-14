package com.eyetwin.repository;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AgentRepository {

    public List<Agent> findAll() {
        String sql = """
            SELECT id, name, image
            FROM agent
            ORDER BY name ASC
            """;

        List<Agent> agents = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Agent agent = new Agent();
                agent.setId(rs.getInt("id"));
                agent.setName(rs.getString("name"));
                agent.setImage(rs.getString("image"));
                agents.add(agent);
            }
        } catch (SQLException e) {
            System.err.println("[AgentRepository] findAll error: " + e.getMessage());
        }

        return agents;
    }

    public List<Agent> findByGame(Game game) {
        if (game == null) return List.of();

        String sql = """
            SELECT id, name, image
            FROM agent
            WHERE game_id = ?
            ORDER BY name ASC
            """;

        List<Agent> agents = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, game.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Agent agent = new Agent();
                    agent.setId(rs.getInt("id"));
                    agent.setName(rs.getString("name"));
                    agent.setImage(rs.getString("image"));
                    agents.add(agent);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AgentRepository] findByGame error: " + e.getMessage());
        }

        return agents;
    }
}
