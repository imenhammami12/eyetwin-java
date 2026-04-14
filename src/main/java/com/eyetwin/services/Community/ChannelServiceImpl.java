package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.Community.IChannelService;
import com.eyetwin.tools.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChannelServiceImpl implements IChannelService {

    private final NotificationServiceImpl notificationService = new NotificationServiceImpl();

    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    @Override
    public List<Channel> findVisibleChannels(User currentUser) throws SQLException {
        List<Channel> channels = new ArrayList<>();
        String sql;

        if (currentUser == null) {
            sql = """
            SELECT * FROM channel
            WHERE status = ? AND is_active = 1 AND type = ?
            ORDER BY created_at DESC
            """;
        } else {
            sql = """
            SELECT * FROM channel
            WHERE (status = ? AND is_active = 1)
               OR created_by = ?
            ORDER BY created_at DESC
            """;
        }

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);

        if (currentUser == null) {
            ps.setString(1, Channel.STATUS_APPROVED);
            ps.setString(2, Channel.TYPE_PUBLIC);
        } else {
            ps.setString(1, Channel.STATUS_APPROVED);
            ps.setString(2, currentUser.getEmail());
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            channels.add(mapChannel(rs));
        }
        return channels;
    }
    @Override
    public List<Channel> findAdminChannels(String search, String status, String type) throws SQLException {
        List<Channel> channels = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM channel WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR game LIKE ? OR created_by LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }

        if (type != null && !type.trim().isEmpty() && !"all".equalsIgnoreCase(type.trim())) {
            sql.append(" AND type = ?");
            params.add(type.trim());
        }

        sql.append(" ORDER BY created_at DESC");

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql.toString());

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            channels.add(mapChannel(rs));
        }
        return channels;
    }

    @Override
    public List<Channel> findPendingChannels() throws SQLException {
        List<Channel> channels = new ArrayList<>();
        String sql = """
            SELECT * FROM channel
            WHERE status = ?
            ORDER BY created_at DESC
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, Channel.STATUS_PENDING);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            channels.add(mapChannel(rs));
        }
        return channels;
    }

    @Override
    public Channel findById(int id) throws SQLException {
        String sql = "SELECT * FROM channel WHERE id = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapChannel(rs);
        }
        return null;
    }

    @Override
    public void createByAdmin(Channel channel, User admin) throws SQLException {
        String sql = """
            INSERT INTO channel (name, description, game, type, status, is_active, image_url,
                                 created_at, created_by, approved_by, approved_at, rejection_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, channel.getName());
        ps.setString(2, channel.getDescription());
        ps.setString(3, channel.getGame());
        ps.setString(4, channel.getType());
        ps.setString(5, Channel.STATUS_APPROVED);
        ps.setBoolean(6, true);
        ps.setString(7, channel.getImageUrl());
        ps.setTimestamp(8, now);
        ps.setString(9, admin.getEmail());
        ps.setString(10, admin.getEmail());
        ps.setTimestamp(11, now);
        ps.setString(12, null);

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            channel.setId(rs.getInt(1));
        }

        channel.setStatus(Channel.STATUS_APPROVED);
        channel.setActive(true);
        channel.setCreatedAt(now);
        channel.setCreatedBy(admin.getEmail());
        channel.setApprovedBy(admin.getEmail());
        channel.setApprovedAt(now);
    }

    @Override
    public void createByPlayer(Channel channel, User player) throws SQLException {
        String sql = """
            INSERT INTO channel (name, description, game, type, status, is_active, image_url,
                                 created_at, created_by, approved_by, approved_at, rejection_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, channel.getName());
        ps.setString(2, channel.getDescription());
        ps.setString(3, channel.getGame());
        ps.setString(4, channel.getType());
        ps.setString(5, Channel.STATUS_PENDING);
        ps.setBoolean(6, false);
        ps.setString(7, channel.getImageUrl());
        ps.setTimestamp(8, now);
        ps.setString(9, player.getEmail());
        ps.setString(10, null);
        ps.setTimestamp(11, null);
        ps.setString(12, null);

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            channel.setId(rs.getInt(1));
        }

        channel.setStatus(Channel.STATUS_PENDING);
        channel.setActive(false);
        channel.setCreatedAt(now);
        channel.setCreatedBy(player.getEmail());
    }

    @Override
    public void updateByAdmin(Channel channel, User admin) throws SQLException {
        Channel existing = findById(channel.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        String sql = """
            UPDATE channel
            SET name = ?, description = ?, game = ?, type = ?, is_active = ?, image_url = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, channel.getName());
        ps.setString(2, channel.getDescription());
        ps.setString(3, channel.getGame());
        ps.setString(4, channel.getType());
        ps.setBoolean(5, channel.isActive());
        ps.setString(6, channel.getImageUrl());
        ps.setInt(7, channel.getId());
        ps.executeUpdate();
    }

    @Override
    public void updateByOwner(Channel channel, User player) throws SQLException {
        Channel existing = findById(channel.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        if (!player.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
            throw new SecurityException("You can only edit your own channel.");
        }

        String sql = """
            UPDATE channel
            SET name = ?, description = ?, game = ?, type = ?, image_url = ?,
                status = ?, is_active = ?, approved_by = ?, approved_at = ?, rejection_reason = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, channel.getName());
        ps.setString(2, channel.getDescription());
        ps.setString(3, channel.getGame());
        ps.setString(4, channel.getType());
        ps.setString(5, channel.getImageUrl());
        ps.setString(6, Channel.STATUS_PENDING);
        ps.setBoolean(7, false);
        ps.setString(8, null);
        ps.setTimestamp(9, null);
        ps.setString(10, null);
        ps.setInt(11, channel.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteByAdmin(int channelId) throws SQLException {
        deleteChannelCascade(channelId);
    }

    @Override
    public void deleteByOwner(int channelId, User player) throws SQLException {
        Channel existing = findById(channelId);
        if (existing == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        if (!player.getEmail().equalsIgnoreCase(existing.getCreatedBy())) {
            throw new SecurityException("You can only delete your own channel.");
        }

        deleteChannelCascade(channelId);
    }

    @Override
    public void approve(int channelId, User admin) throws SQLException {
        Channel channel = findById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        String sql = """
            UPDATE channel
            SET status = ?, is_active = ?, approved_by = ?, approved_at = ?, rejection_reason = ?
            WHERE id = ?
            """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, Channel.STATUS_APPROVED);
        ps.setBoolean(2, true);
        ps.setString(3, admin.getEmail());
        ps.setTimestamp(4, now);
        ps.setString(5, null);
        ps.setInt(6, channelId);
        ps.executeUpdate();

        channel.setStatus(Channel.STATUS_APPROVED);
        channel.setActive(true);
        channel.setApprovedBy(admin.getEmail());
        channel.setApprovedAt(now);
        channel.setRejectionReason(null);

        Integer targetUserId = findUserIdByEmail(channel.getCreatedBy());
        if (targetUserId != null) {
            notificationService.createChannelApprovedNotification(channel, targetUserId);
        }
    }

    @Override
    public void reject(int channelId, String reason, User admin) throws SQLException {
        Channel channel = findById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();

        String sql = """
            UPDATE channel
            SET status = ?, is_active = ?, approved_by = ?, approved_at = ?, rejection_reason = ?
            WHERE id = ?
            """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, Channel.STATUS_REJECTED);
        ps.setBoolean(2, false);
        ps.setString(3, admin.getEmail());
        ps.setTimestamp(4, now);
        ps.setString(5, finalReason);
        ps.setInt(6, channelId);
        ps.executeUpdate();

        channel.setStatus(Channel.STATUS_REJECTED);
        channel.setActive(false);
        channel.setApprovedBy(admin.getEmail());
        channel.setApprovedAt(now);
        channel.setRejectionReason(finalReason);

        Integer targetUserId = findUserIdByEmail(channel.getCreatedBy());
        if (targetUserId != null) {
            notificationService.createChannelRejectedNotification(channel, targetUserId, finalReason);
        }
    }

    @Override
    public void toggleActive(int channelId, User admin) throws SQLException {
        Channel channel = findById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        String sql = "UPDATE channel SET is_active = ? WHERE id = ?";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setBoolean(1, !channel.isActive());
        ps.setInt(2, channelId);
        ps.executeUpdate();
    }

    private void deleteChannelCascade(int channelId) throws SQLException {
        Connection c = getConnection();

        PreparedStatement ps1 = c.prepareStatement("DELETE FROM message_attachment WHERE message_id IN (SELECT id FROM message WHERE channel_id = ?)");
        ps1.setInt(1, channelId);
        ps1.executeUpdate();

        PreparedStatement ps2 = c.prepareStatement("DELETE FROM message WHERE channel_id = ?");
        ps2.setInt(1, channelId);
        ps2.executeUpdate();

        PreparedStatement ps3 = c.prepareStatement("DELETE FROM channel_member WHERE channel_id = ?");
        ps3.setInt(1, channelId);
        ps3.executeUpdate();

        PreparedStatement ps4 = c.prepareStatement("DELETE FROM channel_join_request WHERE channel_id = ?");
        ps4.setInt(1, channelId);
        ps4.executeUpdate();

        PreparedStatement ps5 = c.prepareStatement("DELETE FROM channel_invite WHERE channel_id = ?");
        ps5.setInt(1, channelId);
        ps5.executeUpdate();

        PreparedStatement ps6 = c.prepareStatement("DELETE FROM channel WHERE id = ?");
        ps6.setInt(1, channelId);
        ps6.executeUpdate();
    }

    private Integer findUserIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM user WHERE email = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        return null;
    }

    private Channel mapChannel(ResultSet rs) throws SQLException {
        Channel channel = new Channel();
        channel.setId(rs.getInt("id"));
        channel.setName(rs.getString("name"));
        channel.setDescription(rs.getString("description"));
        channel.setGame(rs.getString("game"));
        channel.setType(rs.getString("type"));
        channel.setStatus(rs.getString("status"));
        channel.setActive(rs.getBoolean("is_active"));
        channel.setImageUrl(rs.getString("image_url"));
        channel.setCreatedAt(rs.getTimestamp("created_at"));
        channel.setCreatedBy(rs.getString("created_by"));
        channel.setApprovedBy(rs.getString("approved_by"));
        channel.setApprovedAt(rs.getTimestamp("approved_at"));
        channel.setRejectionReason(rs.getString("rejection_reason"));
        return channel;
    }

    public List<Channel> findApprovedCommunityChannels(User currentUser) throws SQLException {
        List<Channel> channels = new ArrayList<>();
        String sql;

        if (currentUser == null) {
            sql = """
                SELECT * FROM channel
                WHERE status = ? AND is_active = 1 AND type = ?
                ORDER BY created_at DESC
                """;
        } else {
            sql = """
                SELECT * FROM channel
                WHERE status = ? AND is_active = 1
                ORDER BY created_at DESC
                """;
        }

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);

        if (currentUser == null) {
            ps.setString(1, Channel.STATUS_APPROVED);
            ps.setString(2, Channel.TYPE_PUBLIC);
        } else {
            ps.setString(1, Channel.STATUS_APPROVED);
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            channels.add(mapChannel(rs));
        }
        return channels;
    }

    public List<Channel> findOwnPendingChannels(User currentUser) throws SQLException {
        List<Channel> channels = new ArrayList<>();

        if (currentUser == null || currentUser.getEmail() == null || currentUser.getEmail().isBlank()) {
            return channels;
        }

        String sql = """
            SELECT * FROM channel
            WHERE created_by = ? AND status = ?
            ORDER BY created_at DESC
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, currentUser.getEmail());
        ps.setString(2, Channel.STATUS_PENDING);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            channels.add(mapChannel(rs));
        }
        return channels;
    }

    public int countApprovedVisibleChannels(User currentUser) throws SQLException {
        return findApprovedCommunityChannels(currentUser).size();
    }

    public int countApprovedPublicChannels(User currentUser) throws SQLException {
        int count = 0;
        for (Channel channel : findApprovedCommunityChannels(currentUser)) {
            if (Channel.TYPE_PUBLIC.equalsIgnoreCase(channel.getType())) {
                count++;
            }
        }
        return count;
    }

    public int countApprovedPrivateChannels(User currentUser) throws SQLException {
        int count = 0;
        for (Channel channel : findApprovedCommunityChannels(currentUser)) {
            if (Channel.TYPE_PRIVATE.equalsIgnoreCase(channel.getType())) {
                count++;
            }
        }
        return count;
    }

    public int countOwnPendingChannels(User currentUser) throws SQLException {
        return findOwnPendingChannels(currentUser).size();
    }
}