package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.ChannelReadState;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ChannelReadStateService {

    private Connection getConnection() {
        return DatabaseConfig.getConnection();
    }

    public ChannelReadState findByUserAndChannel(int userId, int channelId) throws SQLException {
        String sql = """
            SELECT *
            FROM channel_read_state
            WHERE user_id = ? AND channel_id = ?
            LIMIT 1
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.setInt(2, channelId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            ChannelReadState state = new ChannelReadState();
            state.setId(rs.getInt("id"));
            state.setUserId(rs.getInt("user_id"));
            state.setChannelId(rs.getInt("channel_id"));

            int lastSeenId = rs.getInt("last_seen_message_id");
            state.setLastSeenMessageId(rs.wasNull() ? null : lastSeenId);

            state.setLastSeenAt(rs.getTimestamp("last_seen_at"));
            return state;
        }

        return null;
    }

    public void upsertLastSeen(int userId, int channelId, Integer lastSeenMessageId) throws SQLException {
        String sql = """
            INSERT INTO channel_read_state (user_id, channel_id, last_seen_message_id, last_seen_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_seen_message_id = VALUES(last_seen_message_id),
                last_seen_at = VALUES(last_seen_at)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);

        ps.setInt(1, userId);
        ps.setInt(2, channelId);

        if (lastSeenMessageId == null) {
            ps.setNull(3, java.sql.Types.INTEGER);
        } else {
            ps.setInt(3, lastSeenMessageId);
        }

        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }
}