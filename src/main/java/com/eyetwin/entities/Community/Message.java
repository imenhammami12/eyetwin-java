package com.eyetwin.entities.Community;

import java.sql.Timestamp;

public class Message {
    private int id;
    private String content;
    private String sender_name;
    private String sender_email;
    private Timestamp sentAt;
    private Timestamp editedAt;
    private boolean is_deleted;
    private int channel_id;
    private String channelName;

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

    public String getChannelName() {return channelName;}

    public void setChannelName(String channelName) {this.channelName = channelName;}
}