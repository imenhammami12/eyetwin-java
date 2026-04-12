package com.eyetwin.entities;

/**
 * Complaint statistics DTO
 */
public class ComplaintStats {

    private int total;
    private int pending;
    private int inProgress;
    private int resolved;
    private int unassigned;
    private int rejected;

    // ✅ NOUVEAUX CHAMPS
    private int critical;
    private int negative;
    private int neutral;
    private int positive;

    private double avgResolutionHours;

    public ComplaintStats() {}

    // ─── Helpers ─────────────────────────────────────────

    public int getResolutionRate() {
        return total > 0 ? (int) Math.round((resolved * 100.0) / total) : 0;
    }

    public int getPendingPct() {
        return total > 0 ? (int) Math.round((pending * 100.0) / total) : 0;
    }

    public int getInProgressPct() {
        return total > 0 ? (int) Math.round((inProgress * 100.0) / total) : 0;
    }

    public String getAvgResolutionLabel() {
        if (avgResolutionHours <= 0) return "N/A";
        return String.format("%.1fh", avgResolutionHours);
    }

    // ─── Getters / Setters ───────────────────────────────

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }

    public int getInProgress() { return inProgress; }
    public void setInProgress(int inProgress) { this.inProgress = inProgress; }

    public int getResolved() { return resolved; }
    public void setResolved(int resolved) { this.resolved = resolved; }

    public int getUnassigned() { return unassigned; }
    public void setUnassigned(int unassigned) { this.unassigned = unassigned; }

    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }

    // ✅ NOUVEAUX

    public int getCritical() { return critical; }
    public void setCritical(int critical) { this.critical = critical; }

    public int getNegative() { return negative; }
    public void setNegative(int negative) { this.negative = negative; }

    public int getNeutral() { return neutral; }
    public void setNeutral(int neutral) { this.neutral = neutral; }

    public int getPositive() { return positive; }
    public void setPositive(int positive) { this.positive = positive; }

    public double getAvgResolutionHours() { return avgResolutionHours; }
    public void setAvgResolutionHours(double h) { this.avgResolutionHours = h; }
}