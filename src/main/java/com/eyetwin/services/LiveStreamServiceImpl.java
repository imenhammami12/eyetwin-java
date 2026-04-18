package com.eyetwin.services;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ILiveStreamService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LiveStreamServiceImpl implements ILiveStreamService {

    public LiveStreamServiceImpl() {
        ensureSchema();
    }

    @Override
    public List<LiveStream> getAvailableStreams() throws SQLException {
        String sql = """
                SELECT ls.*, u.id AS coach_id, u.username AS coach_username, u.full_name AS coach_full_name,
                       COALESCE((SELECT COUNT(*) FROM live_access la WHERE la.live_stream_id = ls.id), 0) AS access_count
                FROM live_stream ls
                JOIN user u ON u.id = ls.coach_id
                WHERE ls.status IN ('scheduled', 'live')
                ORDER BY CASE WHEN ls.status = 'live' THEN 0 ELSE 1 END, ls.created_at DESC
                """;
        List<LiveStream> lives = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lives.add(mapLiveStream(rs));
        }
        return lives;
    }

    @Override
    public List<LiveStream> getStreamsByCoach(User coach) throws SQLException {
        String sql = """
                SELECT ls.*, u.id AS coach_id, u.username AS coach_username, u.full_name AS coach_full_name,
                       COALESCE((SELECT COUNT(*) FROM live_access la WHERE la.live_stream_id = ls.id), 0) AS access_count
                FROM live_stream ls
                JOIN user u ON u.id = ls.coach_id
                WHERE ls.coach_id = ?
                ORDER BY ls.created_at DESC
                """;
        List<LiveStream> streams = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coach.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) streams.add(mapLiveStream(rs));
            }
        }
        return streams;
    }

    @Override
    public LiveStream getById(int id) throws SQLException {
        String sql = """
                SELECT ls.*, u.id AS coach_id, u.username AS coach_username, u.full_name AS coach_full_name,
                       COALESCE((SELECT COUNT(*) FROM live_access la WHERE la.live_stream_id = ls.id), 0) AS access_count
                FROM live_stream ls
                JOIN user u ON u.id = ls.coach_id
                WHERE ls.id = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapLiveStream(rs) : null;
            }
        }
    }

    @Override
    public LiveStream createStream(User coach, String title, String description, int coinPrice) throws SQLException {
        String sql = """
                INSERT INTO live_stream (coach_id, title, description, coin_price, status, stream_key, created_at)
                VALUES (?, ?, ?, ?, 'scheduled', ?, ?)
                """;
        LiveStream live = new LiveStream();
        live.setCoach(coach);
        live.setTitle(title);
        live.setDescription(description);
        live.setCoinPrice(coinPrice);
        live.setStreamKey(UUID.randomUUID().toString().replace("-", ""));
        live.setCreatedAt(LocalDateTime.now());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, coach.getId());
            ps.setString(2, live.getTitle());
            ps.setString(3, normalizeNullable(description));
            ps.setInt(4, live.getCoinPrice());
            ps.setString(5, live.getStreamKey());
            ps.setTimestamp(6, Timestamp.valueOf(live.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) live.setId(keys.getInt(1));
            }
        }
        return getById(live.getId());
    }

    @Override
    public boolean startStream(int liveId, User coach) throws SQLException {
        String sql = """
                UPDATE live_stream
                SET status = 'live', started_at = ?, ended_at = NULL
                WHERE id = ? AND coach_id = ?
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, liveId);
            ps.setInt(3, coach.getId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean endStream(int liveId, User coach) throws SQLException {
        String sql = """
            UPDATE live_stream
            SET status = 'ended', ended_at = ?
            WHERE id = ? AND coach_id = ?
            """;
        boolean updated;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, liveId);
            ps.setInt(3, coach.getId());
            updated = ps.executeUpdate() > 0;
        }

        // ── Notifie n8n pour envoyer emails feedback aux spectateurs ──
        if (updated) {
            try {
                LiveStream stream = getById(liveId);
                List<User> spectators = getSpectatorsByStream(liveId);
                notifyN8nStreamEnded(stream, spectators);
            } catch (Exception e) {
                System.err.println("[n8n] Warning: " + e.getMessage());
            }
        }
        return updated;
    }

    // ── Récupère les spectateurs du stream ────────────────────────
    private List<User> getSpectatorsByStream(int liveId) throws SQLException {
        String sql = """
            SELECT u.id, u.email, u.full_name, u.username
            FROM live_access la
            JOIN user u ON u.id = la.user_id
            WHERE la.live_stream_id = ?
            """;
        List<User> spectators = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, liveId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setEmail(rs.getString("email"));
                    u.setFullName(rs.getString("full_name"));
                    u.setUsername(rs.getString("username"));
                    spectators.add(u);
                }
            }
        }
        return spectators;
    }

    // ── Appelle le webhook n8n ─────────────────────────────────────
    private void notifyN8nStreamEnded(LiveStream stream, List<User> spectators) {
        new Thread(() -> {
            try {
                StringBuilder specs = new StringBuilder("[");
                for (int i = 0; i < spectators.size(); i++) {
                    User u = spectators.get(i);
                    specs.append("{")
                            .append("\"id\":").append(u.getId()).append(",")
                            .append("\"email\":\"").append(u.getEmail()).append("\",")
                            .append("\"fullName\":\"")
                            .append(u.getFullName() != null
                                    ? u.getFullName() : u.getUsername())
                            .append("\"}");;
                    if (i < spectators.size() - 1) specs.append(",");
                }
                specs.append("]");

                String body = "{"
                        + "\"streamId\":"    + stream.getId()    + ","
                        + "\"streamTitle\":\"" + stream.getTitle() + "\","
                        + "\"coachName\":\""
                        + (stream.getCoach().getFullName() != null
                        ? stream.getCoach().getFullName()
                        : stream.getCoach().getUsername()) + "\","
                        + "\"spectators\":"  + specs
                        + "}";

                java.net.http.HttpClient client =
                        java.net.http.HttpClient.newHttpClient();

                java.net.http.HttpRequest request =
                        java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(
                                        "https://imenhammami.app.n8n.cloud/webhook/eyetwin-stream-ended"))
                                .header("Content-Type", "application/json")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                                .build();

                java.net.http.HttpResponse<String> response =
                        client.send(request,
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                System.out.println("[n8n] ✅ Status: " + response.statusCode());
                System.out.println("[n8n] ✅ Response: " + response.body());

            } catch (Exception e) {
                System.err.println("[n8n] ❌ Failed: " + e.getMessage());
            }
        }, "N8nWebhook").start();
    }

    @Override
    public boolean grantFreeAccess(User user, LiveStream live) throws SQLException {
        if (user == null || live == null) return false;
        if (userHasAccess(user, live)) return true;

        String sql = """
            INSERT IGNORE INTO live_access
                (user_id, live_stream_id, coins_spent, purchased_at)
            VALUES (?, ?, 0, ?)
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.setInt(2, live.getId());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("[LiveAccess] Free access granted — user:"
                    + user.getId() + " stream:" + live.getId());
        }
        return true;
    }
    @Override
    public boolean userHasAccess(User user, LiveStream live) throws SQLException {
        if (user == null || live == null) return false;
        if (live.getCoach() != null && live.getCoach().getId() == user.getId()) return true;

        String sql = "SELECT 1 FROM live_access WHERE user_id = ? AND live_stream_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.setInt(2, live.getId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean grantPaidAccess(User user, LiveStream live) throws SQLException {
        if (user == null || live == null) return false;
        if (live.isEnded()) return false;
        if (userHasAccess(user, live)) return true;
        if (user.getCoinBalance() < live.getCoinPrice()) return false;

        String debitSql = "UPDATE user SET coin_balance = coin_balance - ? WHERE id = ? AND coin_balance >= ?";
        String accessSql = """
                INSERT INTO live_access (user_id, live_stream_id, coins_spent, purchased_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement debitPs = conn.prepareStatement(debitSql);
                 PreparedStatement accessPs = conn.prepareStatement(accessSql)) {
                debitPs.setInt(1, live.getCoinPrice());
                debitPs.setInt(2, user.getId());
                debitPs.setInt(3, live.getCoinPrice());
                int updated = debitPs.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    conn.setAutoCommit(previousAutoCommit);
                    return false;
                }

                accessPs.setInt(1, user.getId());
                accessPs.setInt(2, live.getId());
                accessPs.setInt(3, live.getCoinPrice());
                accessPs.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                accessPs.executeUpdate();

                conn.commit();
                conn.setAutoCommit(previousAutoCommit);
                user.setCoinBalance(user.getCoinBalance() - live.getCoinPrice());
                return true;
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(previousAutoCommit);
                throw e;
            }
        }
    }

    private LiveStream mapLiveStream(ResultSet rs) throws SQLException {
        LiveStream live = new LiveStream();
        live.setId(rs.getInt("id"));
        live.setTitle(rs.getString("title"));
        live.setDescription(rs.getString("description"));
        live.setCoinPrice(rs.getInt("coin_price"));
        live.setStatus(rs.getString("status"));
        live.setStreamKey(rs.getString("stream_key"));
        live.setAccessCount(rs.getInt("access_count"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) live.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) live.setStartedAt(startedAt.toLocalDateTime());
        Timestamp endedAt = rs.getTimestamp("ended_at");
        if (endedAt != null) live.setEndedAt(endedAt.toLocalDateTime());

        User coach = new User();
        coach.setId(rs.getInt("coach_id"));
        coach.setUsername(rs.getString("coach_username"));
        coach.setFullName(rs.getString("coach_full_name"));
        live.setCoach(coach);
        return live;
    }

    private String normalizeNullable(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void ensureSchema() {
        String createLiveStream = """
                CREATE TABLE IF NOT EXISTS live_stream (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    coach_id INT NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    description TEXT NULL,
                    coin_price INT NOT NULL DEFAULT 0,
                    status VARCHAR(50) NOT NULL DEFAULT 'scheduled',
                    stream_key VARCHAR(191) NOT NULL UNIQUE,
                    created_at DATETIME NOT NULL,
                    started_at DATETIME NULL,
                    ended_at DATETIME NULL,
                    CONSTRAINT fk_live_stream_user FOREIGN KEY (coach_id) REFERENCES user(id)
                )
                """;
        String createLiveAccess = """
                CREATE TABLE IF NOT EXISTS live_access (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    live_stream_id INT NOT NULL,
                    coins_spent INT NOT NULL DEFAULT 0,
                    purchased_at DATETIME NOT NULL,
                    UNIQUE KEY unique_user_live (user_id, live_stream_id),
                    CONSTRAINT fk_live_access_user FOREIGN KEY (user_id) REFERENCES user(id),
                    CONSTRAINT fk_live_access_stream FOREIGN KEY (live_stream_id) REFERENCES live_stream(id)
                )
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createLiveStream);
            stmt.execute(createLiveAccess);
        } catch (SQLException e) {
            System.err.println("[LiveStreamService] Schema init error: " + e.getMessage());
        }
    }
}
