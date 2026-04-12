package com.eyetwin.interfaces.Community;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.User;

import java.sql.SQLException;
import java.util.List;

public interface IMessageService {
    List<Message> findByChannel(int channelId) throws SQLException;
    List<Message> findAdminMessages(String search, String status) throws SQLException;
    Message findById(int id) throws SQLException;

    void sendMessage(int channelId, String content, User player) throws SQLException;
    void updateOwnMessage(int messageId, String newContent, User player) throws SQLException;
    void softDeleteOwnMessage(int messageId, User player) throws SQLException;

    void adminDeleteMessage(int messageId, User admin) throws SQLException;
    void adminRestoreMessage(int messageId, User admin) throws SQLException;
}
