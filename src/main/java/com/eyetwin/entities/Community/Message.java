package com.eyetwin.entities.Community;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Message {
    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_HAHA = "HAHA";
    public static final String REACTION_LOVE = "LOVE";
    public static final String REACTION_ANGRY = "ANGRY";

    private int id;
    private String content;
    private String sender_name;
    private String sender_email;
    private Timestamp sentAt;
    private Timestamp editedAt;
    private boolean is_deleted;
    private int channel_id;
    private String channelName;

    private List<MessageAttachment> attachments = new ArrayList<>();

    private int likeCount;
    private int hahaCount;
    private int loveCount;
    private int angryCount;
    private String userReaction;

    public Message() {
    }

    public String getDisplayContent() {
        return is_deleted ? "this message is deleted." : content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getSender_email() {
        return sender_email;
    }

    public void setSender_email(String sender_email) {
        this.sender_email = sender_email;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public Timestamp getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Timestamp editedAt) {
        this.editedAt = editedAt;
    }

    public boolean isIs_deleted() {
        return is_deleted;
    }

    public boolean getDeleted() {
        return is_deleted;
    }

    public void setIs_deleted(boolean is_deleted) {
        this.is_deleted = is_deleted;
    }

    public int getChannel_id() {
        return channel_id;
    }

    public void setChannel_id(int channel_id) {
        this.channel_id = channel_id;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public List<MessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MessageAttachment> attachments) {
        this.attachments = attachments;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getHahaCount() {
        return hahaCount;
    }

    public void setHahaCount(int hahaCount) {
        this.hahaCount = hahaCount;
    }

    public int getLoveCount() {
        return loveCount;
    }

    public void setLoveCount(int loveCount) {
        this.loveCount = loveCount;
    }

    public int getAngryCount() {
        return angryCount;
    }

    public void setAngryCount(int angryCount) {
        this.angryCount = angryCount;
    }

    public String getUserReaction() {
        return userReaction;
    }

    public void setUserReaction(String userReaction) {
        this.userReaction = userReaction;
    }

    public int getReactionCount(String reactionType) {
        if (REACTION_LIKE.equalsIgnoreCase(reactionType)) return likeCount;
        if (REACTION_HAHA.equalsIgnoreCase(reactionType)) return hahaCount;
        if (REACTION_LOVE.equalsIgnoreCase(reactionType)) return loveCount;
        if (REACTION_ANGRY.equalsIgnoreCase(reactionType)) return angryCount;
        return 0;
    }

    public boolean isUserReaction(String reactionType) {
        return userReaction != null && userReaction.equalsIgnoreCase(reactionType);
    }

    public boolean hasAnyReaction() {
        return likeCount > 0 || hahaCount > 0 || loveCount > 0 || angryCount > 0;
    }
}