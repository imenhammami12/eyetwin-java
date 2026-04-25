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
import java.sql.Statement;

import com.eyetwin.entities.Community.MessageModerationResult;

public class MessageServiceImpl implements IMessageService {

    private final MessageModerationService moderationService = new MessageModerationService();

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
            Message message = mapMessage(rs);
            message.setAttachments(loadAttachmentsForMessage(message.getId(), c));
            messages.add(message);
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
            Message message = mapMessage(rs);
            message.setAttachments(loadAttachmentsForMessage(message.getId(), c));
            return message;
        }
        return null;
    }

    @Override
    public void sendMessage(int channelId, String content, User player) throws SQLException {
        sendMessage(channelId, content, player, new ArrayList<>());
    }

    @Override
    public void sendMessage(int channelId, String content, User player, List<MessageAttachment> attachments) throws SQLException {
        Channel channel = findChannelById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) {
            throw new IllegalStateException("You cannot send a message to an unavailable channel.");
        }

        String rawContent = (content == null) ? "" : content.trim();
        MessageModerationResult moderation = moderationService.moderate(rawContent);
        String cleanContent = moderation.getMaskedContent();

        boolean hasAttachments = attachments != null && !attachments.isEmpty();

        if (cleanContent.isEmpty() && !hasAttachments) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        Connection c = getConnection();
        boolean oldAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);

        try {
            String messageSql = """
            INSERT INTO message (content, sent_at, edited_at, is_deleted, sender_name, sender_email, channel_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            Timestamp now = new Timestamp(System.currentTimeMillis());

            PreparedStatement ps = c.prepareStatement(messageSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, cleanContent);
            ps.setTimestamp(2, now);
            ps.setTimestamp(3, now);
            ps.setBoolean(4, false);
            ps.setString(5, player.getUsername());
            ps.setString(6, player.getEmail());
            ps.setInt(7, channelId);
            ps.executeUpdate();

            int messageId;
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                messageId = keys.getInt(1);
            } else {
                throw new SQLException("Failed to retrieve inserted message ID.");
            }

            if (hasAttachments) {
                String attachmentSql = """
                INSERT INTO message_attachment
                (original_name, stored_name, mime_type, size, message_id, url, public_id, cloud_resource_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

                PreparedStatement aps = c.prepareStatement(attachmentSql);

                for (MessageAttachment attachment : attachments) {
                    aps.setString(1, attachment.getOriginalName());
                    aps.setString(2, attachment.getStoredName());
                    aps.setString(3, attachment.getMimeType());
                    aps.setInt(4, attachment.getSize());
                    aps.setInt(5, messageId);
                    aps.setString(6, attachment.getUrl());
                    aps.setString(7, attachment.getPublicId());
                    aps.setString(8, attachment.getCloudResourceType());
                    aps.addBatch();
                }

                aps.executeBatch();
            }

            c.commit();

            if (moderation.wasModified()) {
                System.out.println("[Moderation] Masked terms in new message: " + moderation.getMatchedTerms());
            }

        } catch (Exception e) {
            c.rollback();
            throw new SQLException("Failed to send message with attachments: " + e.getMessage(), e);
        } finally {
            c.setAutoCommit(oldAutoCommit);
        }
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

        String rawContent = (newContent == null) ? "" : newContent.trim();
        MessageModerationResult moderation = moderationService.moderate(rawContent);
        String cleanContent = moderation.getMaskedContent();

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
        if (moderation.wasModified()) {
            System.out.println("[Moderation] Masked terms in edited message: " + moderation.getMatchedTerms());
        }
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

        try {
            message.setChannelName(rs.getString("channel_name"));
        } catch (SQLException ignored) {
            message.setChannelName(null);
        }

        return message;
    }

    private List<MessageAttachment> loadAttachmentsForMessage(int messageId, Connection c) throws SQLException {
        List<MessageAttachment> attachments = new ArrayList<>();

        String sql = """
        SELECT *
        FROM message_attachment
        WHERE message_id = ?
        ORDER BY id ASC
        """;

        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, messageId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            MessageAttachment attachment = new MessageAttachment();
            attachment.setId(rs.getInt("id"));
            attachment.setOriginalName(rs.getString("original_name"));
            attachment.setStoredName(rs.getString("stored_name"));
            attachment.setMimeType(rs.getString("mime_type"));
            attachment.setSize(rs.getInt("size"));
            attachment.setMessageId(rs.getInt("message_id"));
            attachment.setUrl(rs.getString("url"));
            attachment.setPublicId(rs.getString("public_id"));
            attachment.setCloudResourceType(rs.getString("cloud_resource_type"));
            attachments.add(attachment);
        }

        return attachments;
    }


    /// SUMMARY
    @Override
    public List<Message> findMessagesAfter(int channelId, int messageId) throws SQLException {
        List<Message> messages = new ArrayList<>();

        if (messageId <= 0) {
            return findByChannel(channelId);
        }

        String sql = """
        SELECT * FROM message
        WHERE channel_id = ? AND id > ?
        ORDER BY sent_at ASC, id ASC
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);
        ps.setInt(2, messageId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Message message = mapMessage(rs);
            message.setAttachments(loadAttachmentsForMessage(message.getId(), c));
            messages.add(message);
        }

        return messages;
    }

    @Override
    public Message findLatestMessageInChannel(int channelId) throws SQLException {
        String sql = """
        SELECT * FROM message
        WHERE channel_id = ?
        ORDER BY id DESC
        LIMIT 1
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Message message = mapMessage(rs);
            message.setAttachments(loadAttachmentsForMessage(message.getId(), c));
            return message;
        }

        return null;
    }

    @Override
    public int countMessagesAfter(int channelId, int messageId) throws SQLException {
        String sql = """
        SELECT COUNT(*)
        FROM message
        WHERE channel_id = ? AND id > ?
        """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);
        ps.setInt(2, Math.max(0, messageId));

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }

        return 0;
    }


}