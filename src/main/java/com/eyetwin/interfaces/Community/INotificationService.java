package com.eyetwin.interfaces.Community;

import com.eyetwin.entities.Community.AppNotification;
import com.eyetwin.entities.Community.Channel;

import java.sql.SQLException;
import java.util.List;

public interface INotificationService {

    void createChannelApprovedNotification(Channel channel, int targetUserId) throws SQLException;
    void createChannelRejectedNotification(Channel channel, int targetUserId, String reason) throws SQLException;

    void createChannelJoinRequestedNotification(Channel channel, int ownerUserId, String requesterDisplay) throws SQLException;
    void createChannelJoinApprovedNotification(Channel channel, int targetUserId) throws SQLException;
    void createChannelJoinDeniedNotification(Channel channel, int targetUserId, String reason) throws SQLException;

    List<AppNotification> findByUser(int userId) throws SQLException;
    List<AppNotification> findUnreadByUser(int userId) throws SQLException;
    int countUnreadByUser(int userId) throws SQLException;
    void markAsRead(int notificationId, int userId) throws SQLException;

    void deleteNotification(int notificationId, int userId) throws SQLException;
    void deleteAllNotifications(int userId) throws SQLException;
}