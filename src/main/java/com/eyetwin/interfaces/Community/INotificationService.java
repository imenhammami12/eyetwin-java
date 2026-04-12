package com.eyetwin.interfaces.Community;
import com.eyetwin.entities.Community.Channel;

import java.sql.SQLException;


public interface INotificationService {
    void createChannelApprovedNotification(Channel channel, int targetUserId) throws SQLException;
    void createChannelRejectedNotification(Channel channel, int targetUserId, String reason) throws SQLException;
}
