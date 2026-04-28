package com.eyetwin.interfaces.Community;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.User;
import  com.eyetwin.entities.Community.AdminChannelMessageStat;

import java.sql.SQLException;
import java.util.List;

import com.eyetwin.entities.Community.MessageAttachment;

public interface IMessageService {
    List<Message> findByChannel(int channelId) throws SQLException;
    List<Message> findAdminMessages(String search, String status) throws SQLException;
    List<AdminChannelMessageStat> findAdminChannelStats(String search) throws SQLException;
    List<Message> findAdminMessagesByChannel(int channelId, String search, String status, String sort) throws SQLException;
    Message findById(int id) throws SQLException;

    void sendMessage(int channelId, String content, User player) throws SQLException;
    void sendMessage(int channelId, String content, User player, List<MessageAttachment> attachments) throws SQLException;
    void updateOwnMessage(int messageId, String newContent, User player) throws SQLException;
    void softDeleteOwnMessage(int messageId, User player) throws SQLException;

    void adminDeleteMessage(int messageId, User admin) throws SQLException;
    void adminRestoreMessage(int messageId, User admin) throws SQLException;

    List<Message> findByChannelForUser(int channelId, User viewer) throws SQLException;
    void toggleReaction(int messageId, String reactionType, User user) throws SQLException;
    Message findByIdForUser(int id, User viewer) throws SQLException;

    /// SUMMARY
    List<Message> findMessagesAfter(int channelId, int messageId) throws SQLException;
    Message findLatestMessageInChannel(int channelId) throws SQLException;
    int countMessagesAfter(int channelId, int messageId) throws SQLException;
}
