package com.eyetwin.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Miroir de l'entité Symfony CoinPurchase
 * Table : coin_purchase
 */
public class CoinPurchase {

    private Integer id;
    private User user;
    private int coinsAmount = 0;
    private BigDecimal pricePaid = BigDecimal.ZERO;
    private String stripeSessionId;
    private String status = "pending"; // pending | completed | failed
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public CoinPurchase() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getCoinsAmount() { return coinsAmount; }
    public void setCoinsAmount(int coinsAmount) { this.coinsAmount = coinsAmount; }

    public BigDecimal getPricePaid() { return pricePaid; }
    public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    @Override
    public String toString() {
        return "CoinPurchase{id=" + id + ", coins=" + coinsAmount
                + ", status='" + status + "', user=" + (user != null ? user.getId() : "null") + "}";
    }
}