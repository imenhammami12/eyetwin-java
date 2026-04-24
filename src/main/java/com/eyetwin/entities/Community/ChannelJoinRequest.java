package com.eyetwin.entities.Community;

import java.sql.Timestamp;

public class ChannelJoinRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_DENIED = "denied";

    private int id;
    private String status;
    private Timestamp requestedAt;
    private Timestamp decidedAt;
    private String decidedByEmail;
    private String reason;
    private int channelId;
    private Integer requesterId;

    private String requesterEmail;
    private String requesterUsername;
    private String channelName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Timestamp getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Timestamp decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecidedByEmail() {
        return decidedByEmail;
    }

    public void setDecidedByEmail(String decidedByEmail) {
        this.decidedByEmail = decidedByEmail;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public Integer getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Integer requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}