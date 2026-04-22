package com.eyetwin.interfaces.Community;
import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;

import java.sql.SQLException;
import java.util.List;

public interface IChannelService {
    List<Channel> findVisibleChannels(User currentUser) throws SQLException;
    List<Channel> findAdminChannels(String search, String status, String type) throws SQLException;
    List<Channel> findPendingChannels() throws SQLException;
    Channel findById(int id) throws SQLException;

    void createByAdmin(Channel channel, User admin) throws SQLException;
    void createByPlayer(Channel channel, User player) throws SQLException;

    void updateByAdmin(Channel channel, User admin) throws SQLException;
    void updateByOwner(Channel channel, User player) throws SQLException;

    void deleteByAdmin(int channelId) throws SQLException;
    void deleteByOwner(int channelId, User player) throws SQLException;

    void approve(int channelId, User admin) throws SQLException;
    void reject(int channelId, String reason, User admin) throws SQLException;
    void toggleActive(int channelId, User admin) throws SQLException;

}
