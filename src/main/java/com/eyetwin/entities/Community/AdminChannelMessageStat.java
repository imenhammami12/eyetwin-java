package com.eyetwin.entities.Community;

public class AdminChannelMessageStat {
    private int channelId;
    private String channelName;
    private String game;
    private String type;
    private int totalMessages;
    private int deletedMessages;

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

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public int getDeletedMessages() {
        return deletedMessages;
    }

    public void setDeletedMessages(int deletedMessages) {
        this.deletedMessages = deletedMessages;
    }
}