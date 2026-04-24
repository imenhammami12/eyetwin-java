package com.eyetwin.services.Community;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.Community.ChannelInvite;
import com.eyetwin.entities.Community.ChannelJoinRequest;
import com.eyetwin.entities.User;
import com.eyetwin.tools.DatabaseConfig;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class ChannelAccessService {

    private final NotificationServiceImpl notificationService = new NotificationServiceImpl();
    private final ChannelServiceImpl channelService = new ChannelServiceImpl();

    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    public boolean canOpenChannel(User user, Channel channel) throws SQLException {
        if (channel == null) return false;
        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) return false;

        if (Channel.TYPE_PUBLIC.equalsIgnoreCase(channel.getType())) {
            return user != null;
        }

        if (user == null) return false;
        if (isOwner(user, channel)) return true;
        return isMember(channel.getId(), user.getId());
    }

    public boolean isOwner(User user, Channel channel) {
        return user != null
                && channel != null
                && channel.getCreatedBy() != null
                && channel.getCreatedBy().equalsIgnoreCase(user.getEmail());
    }

    public boolean isMember(int channelId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM channel_member WHERE channel_id = ? AND user_id = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);
        ps.setInt(2, userId);

        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean hasPendingRequest(int channelId, int userId) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM channel_join_request
            WHERE channel_id = ? AND requester_id = ? AND status = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);
        ps.setInt(2, userId);
        ps.setString(3, ChannelJoinRequest.STATUS_PENDING);

        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public void requestAccess(Channel channel, User requester) throws SQLException {
        if (channel == null) throw new IllegalArgumentException("Channel not found.");
        if (requester == null) throw new IllegalArgumentException("You must be logged in.");
        if (!Channel.TYPE_PRIVATE.equalsIgnoreCase(channel.getType())) {
            throw new IllegalStateException("Join requests are only for private channels.");
        }
        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) {
            throw new IllegalStateException("Channel is not available.");
        }
        if (isOwner(requester, channel)) {
            throw new IllegalStateException("Owner already has access.");
        }
        if (isMember(channel.getId(), requester.getId())) {
            throw new IllegalStateException("You are already a member of this channel.");
        }
        if (hasPendingRequest(channel.getId(), requester.getId())) {
            throw new IllegalStateException("You already have a pending request for this channel.");
        }

        String sql = """
            INSERT INTO channel_join_request
            (status, requested_at, decided_at, decided_by_email, reason, channel_id, requester_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, ChannelJoinRequest.STATUS_PENDING);
        ps.setTimestamp(2, now);
        ps.setTimestamp(3, null);
        ps.setString(4, null);
        ps.setString(5, null);
        ps.setInt(6, channel.getId());
        ps.setInt(7, requester.getId());
        ps.executeUpdate();

        Integer ownerUserId = findUserIdByEmail(channel.getCreatedBy());
        if (ownerUserId != null) {
            notificationService.createChannelJoinRequestedNotification(channel, ownerUserId, buildRequesterDisplay(requester));
        }
    }

//    public List<ChannelJoinRequest> findPendingRequestsForOwner(int channelId, User owner) throws SQLException {
//        Channel channel = channelService.findById(channelId);
//        if (channel == null) throw new IllegalArgumentException("Channel not found.");
//        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can manage access.");
//
//        String sql = """
//            SELECT r.*,
//                   u.email AS requester_email,
//                   u.username AS requester_username,
//                   c.name AS channel_name
//            FROM channel_join_request r
//            INNER JOIN channel c ON c.id = r.channel_id
//            LEFT JOIN user u ON u.id = r.requester_id
//            WHERE r.channel_id = ? AND r.status = ?
//            ORDER BY r.requested_at DESC, r.id DESC
//            """;
//
//        List<ChannelJoinRequest> requests = new ArrayList<>();
//
//        Connection c = getConnection();
//        PreparedStatement ps = c.prepareStatement(sql);
//        ps.setInt(1, channelId);
//        ps.setString(2, ChannelJoinRequest.STATUS_PENDING);
//
//        ResultSet rs = ps.executeQuery();
//        while (rs.next()) {
//            requests.add(mapJoinRequest(rs));
//        }
//
//        return requests;
//    }

    public List<ChannelJoinRequest> findPendingRequestsForOwner(int channelId, User owner) throws SQLException {
        Channel channel = channelService.findById(channelId);
        if (channel == null) throw new IllegalArgumentException("Channel not found.");
        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can manage access.");

        String sql = """
        SELECT r.*,
               u.email AS requester_email,
               u.username AS requester_username,
               c.name AS channel_name
        FROM channel_join_request r
        INNER JOIN channel c ON c.id = r.channel_id
        LEFT JOIN user u ON u.id = r.requester_id
        LEFT JOIN channel_member m
               ON m.channel_id = r.channel_id
              AND m.user_id = r.requester_id
        WHERE r.channel_id = ?
          AND r.status = ?
          AND m.user_id IS NULL
        ORDER BY r.requested_at DESC, r.id DESC
        """;

        List<ChannelJoinRequest> requests = new ArrayList<>();

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, channelId);
        ps.setString(2, ChannelJoinRequest.STATUS_PENDING);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            requests.add(mapJoinRequest(rs));
        }

        return requests;
    }

//    public void approveJoinRequest(int requestId, User owner) throws SQLException {
//        ChannelJoinRequest request = findRequestById(requestId);
//        if (request == null) throw new IllegalArgumentException("Join request not found.");
//
//        Channel channel = channelService.findById(request.getChannelId());
//        if (channel == null) throw new IllegalArgumentException("Channel not found.");
//        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can approve this request.");
//        if (!ChannelJoinRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
//            throw new IllegalStateException("Request is no longer pending.");
//        }
//
//        Connection c = getConnection();
//        boolean oldAutoCommit = c.getAutoCommit();
//        c.setAutoCommit(false);
//
//        try {
//            Timestamp now = new Timestamp(System.currentTimeMillis());
//
//            PreparedStatement ps1 = c.prepareStatement("""
//                UPDATE channel_join_request
//                SET status = ?, decided_at = ?, decided_by_email = ?, reason = ?
//                WHERE id = ?
//                """);
//            ps1.setString(1, ChannelJoinRequest.STATUS_APPROVED);
//            ps1.setTimestamp(2, now);
//            ps1.setString(3, owner.getEmail());
//            ps1.setString(4, null);
//            ps1.setInt(5, requestId);
//            ps1.executeUpdate();
//
//            if (!isMember(channel.getId(), request.getRequesterId())) {
//                PreparedStatement ps2 = c.prepareStatement("""
//                    INSERT INTO channel_member (joined_at, channel_id, user_id)
//                    VALUES (?, ?, ?)
//                    """);
//                ps2.setTimestamp(1, now);
//                ps2.setInt(2, channel.getId());
//                ps2.setInt(3, request.getRequesterId());
//                ps2.executeUpdate();
//            }
//
//            c.commit();
//
//            if (request.getRequesterId() != null) {
//                notificationService.createChannelJoinApprovedNotification(channel, request.getRequesterId());
//            }
//
//        } catch (Exception e) {
//            c.rollback();
//            throw e;
//        } finally {
//            c.setAutoCommit(oldAutoCommit);
//        }
//    }

    public void approveJoinRequest(int requestId, User owner) throws SQLException {
        ChannelJoinRequest request = findRequestById(requestId);
        if (request == null) throw new IllegalArgumentException("Join request not found.");

        Channel channel = channelService.findById(request.getChannelId());
        if (channel == null) throw new IllegalArgumentException("Channel not found.");
        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can approve this request.");
        if (!ChannelJoinRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Request is no longer pending.");
        }

        Connection c = getConnection();
        boolean oldAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);

        try {
            if (isAlreadyMember(c, channel.getId(), request.getRequesterId())) {
                clearPendingRequests(c, channel.getId(), request.getRequesterId());
                c.commit();
                return;
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());

            PreparedStatement ps1 = c.prepareStatement("""
            UPDATE channel_join_request
            SET status = ?, decided_at = ?, decided_by_email = ?, reason = ?
            WHERE id = ?
            """);
            ps1.setString(1, ChannelJoinRequest.STATUS_APPROVED);
            ps1.setTimestamp(2, now);
            ps1.setString(3, owner.getEmail());
            ps1.setString(4, null);
            ps1.setInt(5, requestId);
            ps1.executeUpdate();

            PreparedStatement ps2 = c.prepareStatement("""
            INSERT INTO channel_member (joined_at, channel_id, user_id)
            VALUES (?, ?, ?)
            """);
            ps2.setTimestamp(1, now);
            ps2.setInt(2, channel.getId());
            ps2.setInt(3, request.getRequesterId());
            ps2.executeUpdate();

            clearPendingRequests(c, channel.getId(), request.getRequesterId());

            c.commit();

            if (request.getRequesterId() != null) {
                notificationService.createChannelJoinApprovedNotification(channel, request.getRequesterId());
            }

        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(oldAutoCommit);
        }
    }

//    public void denyJoinRequest(int requestId, User owner, String reason) throws SQLException {
//        ChannelJoinRequest request = findRequestById(requestId);
//        if (request == null) throw new IllegalArgumentException("Join request not found.");
//
//        Channel channel = channelService.findById(request.getChannelId());
//        if (channel == null) throw new IllegalArgumentException("Channel not found.");
//        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can deny this request.");
//        if (!ChannelJoinRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
//            throw new IllegalStateException("Request is no longer pending.");
//        }
//
//        String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();
//
//        String sql = """
//            UPDATE channel_join_request
//            SET status = ?, decided_at = ?, decided_by_email = ?, reason = ?
//            WHERE id = ?
//            """;
//
//        Connection c = getConnection();
//        PreparedStatement ps = c.prepareStatement(sql);
//        ps.setString(1, ChannelJoinRequest.STATUS_DENIED);
//        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
//        ps.setString(3, owner.getEmail());
//        ps.setString(4, finalReason);
//        ps.setInt(5, requestId);
//        ps.executeUpdate();
//
//        if (request.getRequesterId() != null) {
//            notificationService.createChannelJoinDeniedNotification(channel, request.getRequesterId(), finalReason);
//        }
//    }

    public void denyJoinRequest(int requestId, User owner, String reason) throws SQLException {
        ChannelJoinRequest request = findRequestById(requestId);
        if (request == null) throw new IllegalArgumentException("Join request not found.");

        Channel channel = channelService.findById(request.getChannelId());
        if (channel == null) throw new IllegalArgumentException("Channel not found.");
        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can deny this request.");
        if (!ChannelJoinRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Request is no longer pending.");
        }

        Connection c = getConnection();
        boolean oldAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);

        try {
            if (isAlreadyMember(c, channel.getId(), request.getRequesterId())) {
                clearPendingRequests(c, channel.getId(), request.getRequesterId());
                c.commit();
                return;
            }

            String finalReason = (reason == null || reason.trim().isEmpty()) ? "No reason provided." : reason.trim();

            String sql = """
            UPDATE channel_join_request
            SET status = ?, decided_at = ?, decided_by_email = ?, reason = ?
            WHERE id = ?
            """;

            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, ChannelJoinRequest.STATUS_DENIED);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setString(3, owner.getEmail());
            ps.setString(4, finalReason);
            ps.setInt(5, requestId);
            ps.executeUpdate();

            c.commit();

            if (request.getRequesterId() != null) {
                notificationService.createChannelJoinDeniedNotification(channel, request.getRequesterId(), finalReason);
            }

        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(oldAutoCommit);
        }
    }

    public ChannelInvite createInvite(Channel channel, User owner, String mode, Timestamp expiresAt, Integer maxUses) throws SQLException {
        if (channel == null) throw new IllegalArgumentException("Channel not found.");
        if (owner == null) throw new IllegalArgumentException("Owner is required.");
        if (!isOwner(owner, channel)) throw new SecurityException("Only the owner can create invites.");
        if (!Channel.TYPE_PRIVATE.equalsIgnoreCase(channel.getType())) {
            throw new IllegalStateException("Only private channels can use access invites.");
        }
        if (!Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus()) || !channel.isActive()) {
            throw new IllegalStateException("Channel must be approved and active.");
        }

        String finalMode = normalizeInviteMode(mode);
        String token = generateSecureToken();

        String sql = """
            INSERT INTO channel_invite
            (token, created_by_email, expires_at, mode, max_uses, uses, is_active, channel_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, token);
        ps.setString(2, owner.getEmail());
        ps.setTimestamp(3, expiresAt);
        ps.setString(4, finalMode);
        if (maxUses == null || maxUses <= 0) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, maxUses);
        }
        ps.setInt(6, 0);
        ps.setBoolean(7, true);
        ps.setInt(8, channel.getId());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (!rs.next()) {
            throw new SQLException("Failed to create invite.");
        }

        return findInviteById(rs.getInt(1));
    }

    public ChannelInvite resolveActiveInvite(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invite token is required.");
        }

        String sql = """
            SELECT i.*, c.name AS channel_name
            FROM channel_invite i
            INNER JOIN channel c ON c.id = i.channel_id
            WHERE i.token = ?
            LIMIT 1
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, token.trim());

        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("Invite not found.");
        }

        ChannelInvite invite = mapInvite(rs);

        if (!invite.isActive()) {
            throw new IllegalStateException("Invite is no longer active.");
        }

        if (invite.isExpired()) {
            deactivateInvite(invite.getId());
            throw new IllegalStateException("Invite has expired.");
        }

        if (invite.isUsageExhausted()) {
            deactivateInvite(invite.getId());
            throw new IllegalStateException("Invite has reached its maximum uses.");
        }

        return invite;
    }

//    public String joinWithInvite(String token, User user) throws SQLException {
//        if (user == null) {
//            throw new IllegalArgumentException("You must be logged in.");
//        }
//
//        ChannelInvite invite = resolveActiveInvite(token);
//        Channel channel = channelService.findById(invite.getChannelId());
//
//        if (channel == null) {
//            throw new IllegalArgumentException("Channel not found.");
//        }
//
//        if (!Channel.TYPE_PRIVATE.equalsIgnoreCase(channel.getType())) {
//            throw new IllegalStateException("This invite does not belong to a private channel.");
//        }
//
//        if (isOwner(user, channel) || isMember(channel.getId(), user.getId())) {
//            return "ALREADY_MEMBER";
//        }
//
//        if (ChannelInvite.MODE_AUTO_JOIN.equalsIgnoreCase(invite.getMode())) {
//            addMember(channel.getId(), user.getId());
//            consumeInvite(invite);
//            return "AUTO_JOINED";
//        }
//
//        if (hasPendingRequest(channel.getId(), user.getId())) {
//            return "ALREADY_PENDING";
//        }
//
//        requestAccess(channel, user);
//        consumeInvite(invite);
//        return "REQUEST_CREATED";
//    }

    public String joinWithInvite(String token, User user) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("You must be logged in.");
        }

        ChannelInvite invite = resolveActiveInvite(token);
        Channel channel = channelService.findById(invite.getChannelId());

        if (channel == null) {
            throw new IllegalArgumentException("Channel not found.");
        }

        if (!Channel.TYPE_PRIVATE.equalsIgnoreCase(channel.getType())) {
            throw new IllegalStateException("This invite does not belong to a private channel.");
        }

        Connection c = getConnection();
        boolean oldAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);

        try {
            if (isOwner(user, channel) || isAlreadyMember(c, channel.getId(), user.getId())) {
                c.rollback();
                return "ALREADY_MEMBER";
            }

            if (ChannelInvite.MODE_AUTO_JOIN.equalsIgnoreCase(invite.getMode())) {
                PreparedStatement ps = c.prepareStatement("""
                INSERT INTO channel_member (joined_at, channel_id, user_id)
                VALUES (?, ?, ?)
                """);
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setInt(2, channel.getId());
                ps.setInt(3, user.getId());
                ps.executeUpdate();

                clearPendingRequests(c, channel.getId(), user.getId());
                consumeInviteInConnection(c, invite);

                c.commit();
                return "AUTO_JOINED";
            }

            if (hasPendingRequest(channel.getId(), user.getId())) {
                c.rollback();
                return "ALREADY_PENDING";
            }

            c.rollback();
            requestAccess(channel, user);
            consumeInvite(invite);
            return "REQUEST_CREATED";

        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(oldAutoCommit);
        }
    }

    public ChannelJoinRequest findRequestById(int requestId) throws SQLException {
        String sql = """
            SELECT r.*,
                   u.email AS requester_email,
                   u.username AS requester_username,
                   c.name AS channel_name
            FROM channel_join_request r
            INNER JOIN channel c ON c.id = r.channel_id
            LEFT JOIN user u ON u.id = r.requester_id
            WHERE r.id = ?
            LIMIT 1
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, requestId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapJoinRequest(rs);
        }
        return null;
    }

    public ChannelInvite findInviteById(int inviteId) throws SQLException {
        String sql = """
            SELECT i.*, c.name AS channel_name
            FROM channel_invite i
            INNER JOIN channel c ON c.id = i.channel_id
            WHERE i.id = ?
            LIMIT 1
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, inviteId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapInvite(rs);
        }
        return null;
    }

    private void addMember(int channelId, int userId) throws SQLException {
        if (isMember(channelId, userId)) {
            return;
        }

        String sql = """
            INSERT INTO channel_member (joined_at, channel_id, user_id)
            VALUES (?, ?, ?)
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
        ps.setInt(2, channelId);
        ps.setInt(3, userId);
        ps.executeUpdate();
    }

    private void consumeInvite(ChannelInvite invite) throws SQLException {
        int nextUses = (invite.getUses() == null ? 0 : invite.getUses()) + 1;
        boolean stillActive = invite.getMaxUses() == null || nextUses < invite.getMaxUses();

        String sql = """
            UPDATE channel_invite
            SET uses = ?, is_active = ?
            WHERE id = ?
            """;

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, nextUses);
        ps.setBoolean(2, stillActive);
        ps.setInt(3, invite.getId());
        ps.executeUpdate();
    }

    private void deactivateInvite(int inviteId) throws SQLException {
        String sql = "UPDATE channel_invite SET is_active = 0 WHERE id = ?";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, inviteId);
        ps.executeUpdate();
    }

    private String buildRequesterDisplay(User user) {
        if (user == null) return "Someone";
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();
        return user.getEmail() != null ? user.getEmail() : "Someone";
    }

    private Integer findUserIdByEmail(String email) throws SQLException {
        String sql = "SELECT id FROM user WHERE email = ?";
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return null;
    }

    private String normalizeInviteMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ChannelInvite.MODE_REQUIRES_APPROVAL;
        }

        String m = mode.trim().toLowerCase();
        if (Objects.equals(m, ChannelInvite.MODE_AUTO_JOIN)) return ChannelInvite.MODE_AUTO_JOIN;
        return ChannelInvite.MODE_REQUIRES_APPROVAL;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ChannelJoinRequest mapJoinRequest(ResultSet rs) throws SQLException {
        ChannelJoinRequest request = new ChannelJoinRequest();
        request.setId(rs.getInt("id"));
        request.setStatus(rs.getString("status"));
        request.setRequestedAt(rs.getTimestamp("requested_at"));
        request.setDecidedAt(rs.getTimestamp("decided_at"));
        request.setDecidedByEmail(rs.getString("decided_by_email"));
        request.setReason(rs.getString("reason"));
        request.setChannelId(rs.getInt("channel_id"));

        int requesterId = rs.getInt("requester_id");
        request.setRequesterId(rs.wasNull() ? null : requesterId);

        try {
            request.setRequesterEmail(rs.getString("requester_email"));
        } catch (SQLException ignored) {
        }

        try {
            request.setRequesterUsername(rs.getString("requester_username"));
        } catch (SQLException ignored) {
        }

        try {
            request.setChannelName(rs.getString("channel_name"));
        } catch (SQLException ignored) {
        }

        return request;
    }

    private ChannelInvite mapInvite(ResultSet rs) throws SQLException {
        ChannelInvite invite = new ChannelInvite();
        invite.setId(rs.getInt("id"));
        invite.setToken(rs.getString("token"));
        invite.setCreatedByEmail(rs.getString("created_by_email"));
        invite.setExpiresAt(rs.getTimestamp("expires_at"));
        invite.setMode(rs.getString("mode"));

        int maxUses = rs.getInt("max_uses");
        invite.setMaxUses(rs.wasNull() ? null : maxUses);

        int uses = rs.getInt("uses");
        invite.setUses(rs.wasNull() ? 0 : uses);

        invite.setActive(rs.getBoolean("is_active"));
        invite.setChannelId(rs.getInt("channel_id"));

        try {
            invite.setChannelName(rs.getString("channel_name"));
        } catch (SQLException ignored) {
        }

        return invite;
    }

    private boolean isAlreadyMember(Connection cn, int channelId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM channel_member WHERE channel_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, channelId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void clearPendingRequests(Connection cn, int channelId, int userId) throws SQLException {
        String sql = "DELETE FROM channel_join_request WHERE channel_id = ? AND requester_id = ? AND status = 'pending'";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, channelId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void consumeInviteInConnection(Connection c, ChannelInvite invite) throws SQLException {
        int nextUses = (invite.getUses() == null ? 0 : invite.getUses()) + 1;
        boolean stillActive = invite.getMaxUses() == null || nextUses < invite.getMaxUses();

        String sql = """
        UPDATE channel_invite
        SET uses = ?, is_active = ?
        WHERE id = ?
        """;

        PreparedStatement ps = c.prepareStatement(sql);
        ps.setInt(1, nextUses);
        ps.setBoolean(2, stillActive);
        ps.setInt(3, invite.getId());
        ps.executeUpdate();
    }


}