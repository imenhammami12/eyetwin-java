package com.eyetwin.repository;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.entities.GuideVideo;
import com.eyetwin.entities.User;
import com.eyetwin.tools.DatabaseConfig;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for GuideVideo.
 * Provides the methods used by guide-related controllers.
 */
public class GuideVideoRepository {

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public List<GuideVideo> findByUploader(User user) {
        if (user == null) return List.of();

        String sql = """
            SELECT g.id, g.title, g.description, g.video_url, g.thumbnail, g.map,
                   g.likes, g.views, g.status, g.created_at, g.approved_at,
                   g.uploaded_by_id, g.game_id, g.agent_id,
                   u.username AS uploader_username,
                   gm.name AS game_name,
                   a.name AS agent_name,
                   a.image AS agent_image
            FROM guide_video g
            LEFT JOIN user u ON u.id = g.uploaded_by_id
            LEFT JOIN game gm ON gm.id = g.game_id
            LEFT JOIN agent a ON a.id = g.agent_id
            WHERE g.uploaded_by_id = ?
            ORDER BY g.created_at DESC
            """;

        return queryGuides(sql, ps -> ps.setInt(1, user.getId()));
    }

    public List<GuideVideo> findApprovedByGameAndAgent(Game game, Agent agent) {
        if (game == null || agent == null) return List.of();

        String sql = """
            SELECT g.id, g.title, g.description, g.video_url, g.thumbnail, g.map,
                   g.likes, g.views, g.status, g.created_at, g.approved_at,
                   g.uploaded_by_id, g.game_id, g.agent_id,
                   u.username AS uploader_username,
                   gm.name AS game_name,
                   a.name AS agent_name,
                   a.image AS agent_image
            FROM guide_video g
            LEFT JOIN user u ON u.id = g.uploaded_by_id
            LEFT JOIN game gm ON gm.id = g.game_id
            LEFT JOIN agent a ON a.id = g.agent_id
            WHERE g.status = 'approved' AND g.game_id = ? AND g.agent_id = ?
            ORDER BY g.created_at DESC
            """;

        return queryGuides(sql, ps -> {
            ps.setInt(1, game.getId());
            ps.setInt(2, agent.getId());
        });
    }

    public GuideVideo save(GuideVideo guide) {
        String sql = """
            INSERT INTO guide_video
                (title, description, video_url, thumbnail, map, likes, views, status,
                 created_at, approved_at, uploaded_by_id, game_id, agent_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindGuideWrite(ps, guide, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    setIntegerProperty(guide, "setId", keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("[GuideVideoRepository] save error: " + e.getMessage());
        }

        return guide;
    }

    public void update(GuideVideo guide) {
        if (guide == null || guide.getId() == null) return;

        String sql = """
            UPDATE guide_video SET
                title = ?, description = ?, video_url = ?, thumbnail = ?, map = ?,
                likes = ?, views = ?, status = ?, created_at = ?, approved_at = ?,
                uploaded_by_id = ?, game_id = ?, agent_id = ?
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindGuideWrite(ps, guide, true);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[GuideVideoRepository] update error: " + e.getMessage());
        }
    }

    public void delete(GuideVideo guide) {
        if (guide == null || guide.getId() == null) return;

        String sql = "DELETE FROM guide_video WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, guide.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[GuideVideoRepository] delete error: " + e.getMessage());
        }
    }

    public void saveLike(GuideVideo guide) {
        if (guide == null || guide.getId() == null) return;

        String updateLikesSql = "UPDATE guide_video SET likes = ? WHERE id = ?";
        String deleteLikesSql = "DELETE FROM guide_video_like WHERE guide_video_id = ?";
        String insertLikeSql = "INSERT INTO guide_video_like (guide_video_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement ps = conn.prepareStatement(updateLikesSql)) {
                    ps.setInt(1, guide.getLikes());
                    ps.setInt(2, guide.getId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(deleteLikesSql)) {
                    ps.setInt(1, guide.getId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(insertLikeSql)) {
                    for (User user : guide.getLikedBy()) {
                        if (user == null) continue;
                        ps.setInt(1, guide.getId());
                        ps.setInt(2, user.getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[GuideVideoRepository] saveLike error: " + e.getMessage());
        }
    }

    private List<GuideVideo> queryGuides(String sql, SqlBinder binder) {
        List<GuideVideo> guides = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            binder.bind(ps);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GuideVideo guide = mapGuide(rs);
                    guide.getLikedBy().addAll(loadLikedUsers(conn, guide.getId()));
                    guide.setLikes(guide.getLikedBy().size() > 0 ? guide.getLikedBy().size() : guide.getLikes());
                    guides.add(guide);
                }
            }

        } catch (SQLException e) {
            System.err.println("[GuideVideoRepository] query error: " + e.getMessage());
        }

        return guides;
    }

    private GuideVideo mapGuide(ResultSet rs) throws SQLException {
        GuideVideo guide = new GuideVideo();

        setIntegerProperty(guide, "setId", rs.getInt("id"));
        guide.setTitle(rs.getString("title"));
        guide.setDescription(rs.getString("description"));
        guide.setVideoUrl(rs.getString("video_url"));
        guide.setThumbnail(rs.getString("thumbnail"));
        guide.setMap(rs.getString("map"));
        guide.setLikes(rs.getInt("likes"));
        guide.setViews(rs.getInt("views"));
        guide.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) guide.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) guide.setApprovedAt(approvedAt.toLocalDateTime());

        int uploaderId = rs.getInt("uploaded_by_id");
        if (!rs.wasNull()) {
            User uploader = new User();
            uploader.setId(uploaderId);
            uploader.setUsername(rs.getString("uploader_username"));
            guide.setUploadedBy(uploader);
        }

        int gameId = rs.getInt("game_id");
        if (!rs.wasNull()) {
            Game game = new Game();
            setIntProperty(game, "setId", gameId);
            setStringProperty(game, "setName", rs.getString("game_name"));
            guide.setGame(game);
        }

        int agentId = rs.getInt("agent_id");
        if (!rs.wasNull()) {
            Agent agent = new Agent();
            setIntProperty(agent, "setId", agentId);
            setStringProperty(agent, "setName", rs.getString("agent_name"));
            setStringProperty(agent, "setImage", rs.getString("agent_image"));
            guide.setAgent(agent);
        }

        return guide;
    }

    private List<User> loadLikedUsers(Connection conn, Integer guideId) {
        if (guideId == null) return List.of();

        List<User> likedBy = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username
            FROM guide_video_like l
            INNER JOIN user u ON u.id = l.user_id
            WHERE l.guide_video_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    likedBy.add(user);
                }
            }
        } catch (SQLException e) {
            // Keep guide loading resilient if likes table is absent/incompatible.
        }

        return likedBy;
    }

    private void bindGuideWrite(PreparedStatement ps, GuideVideo guide, boolean includeWhereId) throws SQLException {
        ps.setString(1, guide.getTitle());
        ps.setString(2, guide.getDescription());
        ps.setString(3, guide.getVideoUrl());
        ps.setString(4, guide.getThumbnail());
        ps.setString(5, guide.getMap());
        ps.setInt(6, guide.getLikes());
        ps.setInt(7, guide.getViews());
        ps.setString(8, guide.getStatus());

        LocalDateTime createdAt = guide.getCreatedAt() != null ? guide.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(9, Timestamp.valueOf(createdAt));

        if (guide.getApprovedAt() != null) {
            ps.setTimestamp(10, Timestamp.valueOf(guide.getApprovedAt()));
        } else {
            ps.setNull(10, Types.TIMESTAMP);
        }

        if (guide.getUploadedBy() != null) {
            ps.setInt(11, guide.getUploadedBy().getId());
        } else {
            ps.setNull(11, Types.INTEGER);
        }

        if (guide.getGame() != null) {
            ps.setInt(12, guide.getGame().getId());
        } else {
            ps.setNull(12, Types.INTEGER);
        }

        if (guide.getAgent() != null) {
            ps.setInt(13, guide.getAgent().getId());
        } else {
            ps.setNull(13, Types.INTEGER);
        }

        if (includeWhereId) {
            ps.setInt(14, guide.getId());
        }
    }

    private void setIntegerProperty(Object target, String methodName, Integer value) {
        if (target == null || value == null) return;

        try {
            Method m = target.getClass().getMethod(methodName, Integer.class);
            m.invoke(target, value);
            return;
        } catch (Exception ignored) {
        }

        setIntProperty(target, methodName, value);
    }

    private void setIntProperty(Object target, String methodName, int value) {
        if (target == null) return;

        try {
            Method m = target.getClass().getMethod(methodName, int.class);
            m.invoke(target, value);
        } catch (Exception ignored) {
        }
    }

    private void setStringProperty(Object target, String methodName, String value) {
        if (target == null) return;

        try {
            Method m = target.getClass().getMethod(methodName, String.class);
            m.invoke(target, value);
        } catch (Exception ignored) {
        }
    }
}
