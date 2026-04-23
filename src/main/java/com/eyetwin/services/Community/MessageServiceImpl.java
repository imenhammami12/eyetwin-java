package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.Community.IMessageService;
import com.eyetwin.tools.DatabaseConfig;
import com.eyetwin.entities.Community.AdminChannelMessageStat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.eyetwin.entities.Community.MessageAttachment;

public class MessageServiceImpl implements IMessageService {

    private Connection getConnection() {
        Connection c = DatabaseConfig.getInstance().getCnx();
        try {
            System.out.println("DB URL = " + c.getMetaData().getURL());
            System.out.println("DB USER = " + c.getMetaData().getUserName());
            System.out.println("AUTO COMMIT = " + c.getAutoCommit());
            System.out.println("CATALOG = " + c.getCatalog());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    @Override
    public List<Message> findByChannel(int channelId) throws SQLException {
        List<Message> messages = new ArrayList<>();

        String sql = """
            SELECT * FROM message
            WHERE channel_id = ?
            ORDER BY sent_at ASC, id ASC
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            messages.add(mapMessage(rs));
        }
        return messages;
    }

    @Override
    public List<Message> findAdminMessages(String search, String status) throws SQLException {
        List<Message> messages = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT m.* , c.name AS channel_name
            FROM message m
            INNER JOIN channel c ON c.id = m.channel_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (m.content LIKE ? OR m.sender_name LIKE ? OR m.sender_email LIKE ? OR c.name LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            if ("deleted".equalsIgnoreCase(status.trim())) {
                sql.append(" AND m.is_deleted = 1");
            } else if ("active".equalsIgnoreCase(status.trim())) {
                sql.append(" AND m.is_deleted = 0");
            }
        }

        sql.append(" ORDER BY m.sent_at DESC, m.id DESC");

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql.toString());

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            messages.add(mapMessage(rs));
        }
        return messages;
    }

    @Override
    public List<AdminChannelMessageStat> findAdminChannelStats(String search) throws SQLException {
        List<AdminChannelMessageStat> items = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT
            c.id AS channel_id,
            c.name AS channel_name,
            c.game,
            c.type,
            COUNT(m.id) AS total_messages,
            SUM(CASE WHEN m.is_deleted = 1 THEN 1 ELSE 0 END) AS deleted_messages
        FROM channel c
        LEFT JOIN message m ON m.channel_id = c.id
        WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (c.name LIKE ? OR c.game LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("""
        GROUP BY c.id, c.name, c.game, c.type
        HAVING COUNT(m.id) > 0
        ORDER BY total_messages DESC, c.name ASC
        """);

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql.toString());

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            AdminChannelMessageStat item = new AdminChannelMessageStat();
            item.setChannelId(rs.getInt("channel_id"));
            item.setChannelName(rs.getString("channel_name"));
            item.setGame(rs.getString("game"));
            item.setType(rs.getString("type"));
            item.setTotalMessages(rs.getInt("total_messages"));
            item.setDeletedMessages(rs.getInt("deleted_messages"));
            items.add(item);
        }

        return items;
    }

    @Override
    public List<Message> findAdminMessagesByChannel(int channelId, String search, String status, String sort) throws SQLException {
        List<Message> messages = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        SELECT m.*, c.name AS channel_name
        FROM message m
        INNER JOIN channel c ON c.id = m.channel_id
        WHERE m.channel_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(channelId);

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (m.content LIKE ? OR m.sender_name LIKE ? OR m.sender_email LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            if ("deleted".equalsIgnoreCase(status.trim())) {
                sql.append(" AND m.is_deleted = 1");
            } else if ("active".equalsIgnoreCase(status.trim())) {
                sql.append(" AND m.is_deleted = 0");
            }
        }

        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY m.sent_at ASC, m.id ASC");
        } else {
            sql.append(" ORDER BY m.sent_at DESC, m.id DESC");
        }

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql.toString());

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            messages.add(mapMessage(rs));
        }

        return messages;
    }

    @Override
    public Message findById(int id) throws SQLException {
        String sql = "SELECT * FROM message WHERE id = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapMessage(rs);
        }
        return null;
    }

//    @Override
//    public void sendMessage(int channelId, String content, User player) throws SQLException {
//        Channel channel = findChannelById(channelId);
//        if (channel == null) {
//            throw new IllegalArgumentException("Channel not found.");
//        }
//
//        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) {
//            throw new IllegalStateException("You cannot send a message to an unavailable channel.");
//        }
//
//        String cleanContent = (content == null) ? "" : content.trim();
//        if (cleanContent.isEmpty()) {
//            throw new IllegalArgumentException("Message content cannot be empty.");
//        }
//
//        String sql = """
//            INSERT INTO message (content, sent_at, edited_at, is_deleted, sender_name, sender_email, channel_id)
//            VALUES (?, ?, ?, ?, ?, ?, ?)
//            """;
//
//        Timestamp now = new Timestamp(System.currentTimeMillis());
//
//        Connection c = getConnection();
//        PreparedStatement ps = c.prepareStatement(sql);
//        ps.setString(1, cleanContent);
//        ps.setTimestamp(2, now);
//        ps.setTimestamp(3, now);
//        ps.setBoolean(4, false);
//        ps.setString(5, player.getUsername());
//        ps.setString(6, player.getEmail());
//        ps.setInt(7, channelId);
//        ps.executeUpdate();
//    }


    @Override
    public void sendMessage(int channelId, String content, User player) throws SQLException {
        sendMessage(channelId, content, player, null);
    }

    @Override
    public void sendMessage(int channelId, String content, User player, MessageAttachment attachment) throws SQLException {
        Channel channel = findChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) {
            throw new IllegalStateException("You cannot send a message to an unavailable channel.");
        }

        String cleanContent = (content == null) ? "" : content.trim();
        boolean hasAttachment = attachment != null && attachment.isPresent();

        if (cleanContent.isEmpty() && !hasAttachment) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        if (cleanContent.length() > 1000) {
            throw new IllegalArgumentException("Message must not exceed 1000 characters.");
        }

        String sql = """
        INSERT INTO message (
            content, sent_at, edited_at, is_deleted, sender_name, sender_email, channel_id,
            attachment_public_id, attachment_url, attachment_resource_type, attachment_format,
            attachment_original_name, attachment_mime_type, attachment_bytes
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, cleanContent);
        ps.setTimestamp(2, now);
        ps.setTimestamp(3, now);
        ps.setBoolean(4, false);
        ps.setString(5, player.getUsername());
        ps.setString(6, player.getEmail());
        ps.setInt(7, channelId);

        if (hasAttachment) {
            ps.setString(8, attachment.getPublicId());
            ps.setString(9, attachment.getUrl());
            ps.setString(10, attachment.getResourceType());
            ps.setString(11, attachment.getFormat());
            ps.setString(12, attachment.getOriginalName());
            ps.setString(13, attachment.getMimeType());
            ps.setLong(14, attachment.getBytes());
        } else {
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.VARCHAR);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
            ps.setNull(13, Types.VARCHAR);
            ps.setNull(14, Types.BIGINT);
        }

        ps.executeUpdate();
    }

    @Override
    public void updateOwnMessage(int messageId, String newContent, User player) throws SQLException {
        Message existing = findById(messageId);
        if (existing == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        if (!player.getEmail().equalsIgnoreCase(existing.getSender_email())) {
            throw new SecurityException("You can only edit your own messages.");
        }

        if (existing.isIs_deleted()) {
            throw new IllegalStateException("Deleted messages cannot be edited.");
        }

        String cleanContent = (newContent == null) ? "" : newContent.trim();
        if (cleanContent.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        String sql = """
            UPDATE message
            SET content = ?, edited_at = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, cleanContent);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setInt(3, messageId);
        ps.executeUpdate();
    }

    @Override
    public void softDeleteOwnMessage(int messageId, User player) throws SQLException {
        Message existing = findById(messageId);
        if (existing == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        if (!player.getEmail().equalsIgnoreCase(existing.getSender_email())) {
            throw new SecurityException("You can only delete your own messages.");
        }

        String sql = """
            UPDATE message
            SET is_deleted = ?, edited_at = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setBoolean(1, true);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setInt(3, messageId);
        ps.executeUpdate();
    }

    @Override
    public void adminDeleteMessage(int messageId, User admin) throws SQLException {
        if (!isAdmin(admin)) {
            throw new SecurityException("Only admin can delete messages here.");
        }

        String sql = """
            UPDATE message
            SET is_deleted = ?, edited_at = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setBoolean(1, true);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setInt(3, messageId);
        ps.executeUpdate();
    }

    @Override
    public void adminRestoreMessage(int messageId, User admin) throws SQLException {
        if (!isAdmin(admin)) {
            throw new SecurityException("Only admin can restore messages.");
        }

        String sql = """
            UPDATE message
            SET is_deleted = ?, edited_at = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setBoolean(1, false);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setInt(3, messageId);
        ps.executeUpdate();
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRolesJson() == null) {
            return false;
        }

        String roles = user.getRolesJson();
        return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN");
    }

    private Channel findChannelById(int id) throws SQLException {
        String sql = "SELECT * FROM channel WHERE id = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
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
        return null;
    }

    private Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setContent(rs.getString("content"));
        message.setSentAt(rs.getTimestamp("sent_at"));
        message.setEditedAt(rs.getTimestamp("edited_at"));
        message.setIs_deleted(rs.getBoolean("is_deleted"));
        message.setSender_name(rs.getString("sender_name"));
        message.setSender_email(rs.getString("sender_email"));
        message.setChannel_id(rs.getInt("channel_id"));

        message.setAttachmentPublicId(rs.getString("attachment_public_id"));
        message.setAttachmentUrl(rs.getString("attachment_url"));
        message.setAttachmentResourceType(rs.getString("attachment_resource_type"));
        message.setAttachmentFormat(rs.getString("attachment_format"));
        message.setAttachmentOriginalName(rs.getString("attachment_original_name"));
        message.setAttachmentMimeType(rs.getString("attachment_mime_type"));
        message.setAttachmentBytes(rs.getLong("attachment_bytes"));

        try {
            message.setChannelName(rs.getString("channel_name"));
        } catch (SQLException ignored) {
            message.setChannelName(null);
        }

        return message;
    }


}