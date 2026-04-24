package com.eyetwin.entities.Community;

import java.sql.Timestamp;

public class ChannelInvite {

    public static final String MODE_AUTO_JOIN = "auto_join";
    public static final String MODE_REQUIRES_APPROVAL = "requires_approval";

    private int id;
    private String token;
    private String createdByEmail;
    private Timestamp expiresAt;
    private String mode;
    private Integer maxUses;
    private Integer uses;
    private boolean active;
    private int channelId;

    private String channelName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getUses() {
        return uses;
    }

    public void setUses(Integer uses) {
        this.uses = uses;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }

    public boolean isUsageExhausted() {
        return maxUses != null && uses != null && uses >= maxUses;
    }
}