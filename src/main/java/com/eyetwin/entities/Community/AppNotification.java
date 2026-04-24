package com.eyetwin.entities.Community;

import java.sql.Timestamp;

public class AppNotification {
    private int id;
    private String type;
    private String message;
    private boolean read;
    private Timestamp createdAt;
    private String link;
    private int userId;
    private boolean isReadDb;

    public static final String CHANNEL_APPROVED = "CHANNEL_APPROVED";
    public static final String CHANNEL_REJECTED = "CHANNEL_REJECTED";

    public static final String CHANNEL_JOIN_REQUESTED = "CHANNEL_JOIN_REQUESTED";
    public static final String CHANNEL_JOIN_APPROVED = "CHANNEL_JOIN_APPROVED";
    public static final String CHANNEL_JOIN_DENIED = "CHANNEL_JOIN_DENIED";

    public AppNotification() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public boolean getRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}