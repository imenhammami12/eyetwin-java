package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.AppNotification;
import com.eyetwin.entities.Community.Channel;
import com.eyetwin.interfaces.Community.INotificationService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NotificationServiceImpl implements INotificationService {

    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    @Override
    public void createChannelApprovedNotification(Channel channel, int targetUserId) throws SQLException {
        String sql = """
            INSERT INTO notification (type, message, is_read, created_at, link, `read`, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, AppNotification.CHANNEL_APPROVED);
        ps.setString(2, "Your channel \"" + channel.getName() + "\" has been approved by an admin. It is now visible in the community.");
        ps.setBoolean(3, false);
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.setString(5, "/channels/" + channel.getId());
        ps.setBoolean(6, false);
        ps.setInt(7, targetUserId);
        ps.executeUpdate();
    }

    @Override
    public void createChannelRejectedNotification(Channel channel, int targetUserId, String reason) throws SQLException {
        String sql = """
            INSERT INTO notification (type, message, is_read, created_at, link, `read`, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, AppNotification.CHANNEL_REJECTED);
        ps.setString(2, "Your channel \"" + channel.getName() + "\" has been rejected by an admin. Reason: " + finalReason);
        ps.setBoolean(3, false);
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.setString(5, "/channels");
        ps.setBoolean(6, false);
        ps.setInt(7, targetUserId);
        ps.executeUpdate();
    }
}