package com.eyetwin.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Miroir Java de l'entité Symfony LiveStream.
 */
public class LiveStream {

    private int id;
    private User coach;
    private String title = "";
    private String description;
    private int coinPrice = 0;
    private String status = "scheduled";
    private String streamKey = UUID.randomUUID().toString().replace("-", "");
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int accessCount = 0;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getCoach() { return coach; }
    public void setCoach(User coach) { this.coach = coach; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title : ""; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCoinPrice() { return coinPrice; }
    public void setCoinPrice(int coinPrice) { this.coinPrice = Math.max(0, coinPrice); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status : "scheduled"; }

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) {
        this.streamKey = (streamKey == null || streamKey.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : streamKey;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public int getAccessCount() { return accessCount; }
    public void setAccessCount(int accessCount) { this.accessCount = Math.max(0, accessCount); }

    public boolean isLive() { return "live".equalsIgnoreCase(status); }
    public boolean isEnded() { return "ended".equalsIgnoreCase(status); }
    public boolean isScheduled() { return "scheduled".equalsIgnoreCase(status); }

    public int getRevenueCoins() { return accessCount * coinPrice; }
}
