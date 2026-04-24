package com.eyetwin.entities.Community;

import java.sql.Timestamp;

public class ChannelReadState {
    private int id;
    private int userId;
    private int channelId;
    private Integer lastSeenMessageId;
    private Timestamp lastSeenAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public Integer getLastSeenMessageId() {
        return lastSeenMessageId;
    }

    public void setLastSeenMessageId(Integer lastSeenMessageId) {
        this.lastSeenMessageId = lastSeenMessageId;
    }

    public Timestamp getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Timestamp lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}