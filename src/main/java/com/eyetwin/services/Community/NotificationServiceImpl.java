package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.AppNotification;
import com.eyetwin.entities.Community.Channel;
import com.eyetwin.interfaces.Community.INotificationService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NotificationServiceImpl implements INotificationService {

    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    private void createNotification(String type, String message, String link, int targetUserId) throws SQLException {
        String sql = """
            INSERT INTO notification (type, message, is_read, created_at, link, `read`, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, type);
        ps.setString(2, message);
        ps.setBoolean(3, false);
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.setString(5, link);
        ps.setBoolean(6, false);
        ps.setInt(7, targetUserId);
        ps.executeUpdate();
    }

    @Override
    public void createChannelApprovedNotification(Channel channel, int targetUserId) throws SQLException {
        createNotification(
                AppNotification.CHANNEL_APPROVED,
                "Your channel \"" + channel.getName() + "\" has been approved by an admin. It is now visible in the community.",
                "/channels/" + channel.getId(),
                targetUserId
        );
    }

    @Override
    public void createChannelRejectedNotification(Channel channel, int targetUserId, String reason) throws SQLException {
        String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();

        createNotification(
                AppNotification.CHANNEL_REJECTED,
                "Your channel \"" + channel.getName() + "\" has been rejected by an admin. Reason: " + finalReason,
                "/channels",
                targetUserId
        );
    }

    @Override
    public void createChannelJoinRequestedNotification(Channel channel, int ownerUserId, String requesterDisplay) throws SQLException {
        createNotification(
                AppNotification.CHANNEL_JOIN_REQUESTED,
                requesterDisplay + " requested to join your private channel \"" + channel.getName() + "\".",
                "/community/access/" + channel.getId(),
                ownerUserId
        );
    }

    @Override
    public void createChannelJoinApprovedNotification(Channel channel, int targetUserId) throws SQLException {
        createNotification(
                AppNotification.CHANNEL_JOIN_APPROVED,
                "Your request to join \"" + channel.getName() + "\" was approved. You can open the channel now.",
                "/channels/" + channel.getId(),
                targetUserId
        );
    }

    @Override
    public void createChannelJoinDeniedNotification(Channel channel, int targetUserId, String reason) throws SQLException {
        String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();

        createNotification(
                AppNotification.CHANNEL_JOIN_DENIED,
                "Your request to join \"" + channel.getName() + "\" was denied. Reason: " + finalReason,
                "/channels",
                targetUserId
        );
    }

    @Override
    public List<AppNotification> findByUser(int userId) throws SQLException {
        List<AppNotification> items = new ArrayList<>();

        String sql = """
        SELECT id, type, message, is_read, created_at, link, user_id
        FROM notification
        WHERE user_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT 10
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            items.add(mapNotification(rs));
        }

        return items;
    }

    @Override
    public List<AppNotification> findUnreadByUser(int userId) throws SQLException {
        List<AppNotification> items = new ArrayList<>();

        String sql = """
        SELECT id, type, message, is_read, created_at, link, user_id
        FROM notification
        WHERE user_id = ? AND is_read = 0
        ORDER BY created_at DESC, id DESC
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            items.add(mapNotification(rs));
        }

        return items;
    }

    @Override
    public int countUnreadByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM notification WHERE user_id = ? AND is_read = 0";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    @Override
    public void markAsRead(int notificationId, int userId) throws SQLException {
        String sql = """
        UPDATE notification
        SET is_read = 1, `read` = 1
        WHERE id = ? AND user_id = ?
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, notificationId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    private AppNotification mapNotification(ResultSet rs) throws SQLException {
        AppNotification n = new AppNotification();
        n.setId(rs.getInt("id"));
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getTimestamp("created_at"));
        n.setLink(rs.getString("link"));
        n.setUserId(rs.getInt("user_id"));
        return n;
    }

    @Override
    public void deleteNotification(int notificationId, int userId) throws SQLException {
        String sql = "DELETE FROM notification WHERE id = ? AND user_id = ?";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, notificationId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    @Override
    public void deleteAllNotifications(int userId ) throws SQLException {
        String sql = "DELETE FROM notification WHERE user_id = ?";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }
}