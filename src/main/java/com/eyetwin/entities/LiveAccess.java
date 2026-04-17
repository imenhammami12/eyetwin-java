package com.eyetwin.entities;

import java.time.LocalDateTime;

/**
 * Miroir Java de l'entité Symfony LiveAccess.
 */
public class LiveAccess {

    private int id;
    private User user;
    private LiveStream liveStream;
    private int coinsSpent = 0;
    private LocalDateTime purchasedAt = LocalDateTime.now();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LiveStream getLiveStream() { return liveStream; }
    public void setLiveStream(LiveStream liveStream) { this.liveStream = liveStream; }

    public int getCoinsSpent() { return coinsSpent; }
    public void setCoinsSpent(int coinsSpent) { this.coinsSpent = Math.max(0, coinsSpent); }

    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
}
