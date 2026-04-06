package com.eyetwin.entities;

/**
 * Mirrors the $stats array returned by ComplaintRepository::getStatistics()
 * in the Symfony controller.
 */
public class ComplaintStats {

    private int    total;
    private int    pending;
    private int    inProgress;
    private int    resolved;
    private int    unassigned;
    private int    rejected;
    private double avgResolutionHours;  // 0 = N/A

    public ComplaintStats() {}

    // ─── Computed helpers ─────────────────────────────────────────
    public int    getResolutionRate()  {
        return total > 0 ? (int) Math.round((resolved * 100.0) / total) : 0;
    }
    public int    getPendingPct()      {
        return total > 0 ? (int) Math.round((pending * 100.0) / total) : 0;
    }
    public int    getInProgressPct()   {
        return total > 0 ? (int) Math.round((inProgress * 100.0) / total) : 0;
    }
    public String getAvgResolutionLabel() {
        if (avgResolutionHours <= 0) return "N/A";
        return String.format("%.1fh", avgResolutionHours);
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public int    getTotal()                              { return total; }
    public void   setTotal(int total)                     { this.total = total; }

    public int    getPending()                            { return pending; }
    public void   setPending(int pending)                 { this.pending = pending; }

    public int    getInProgress()                         { return inProgress; }
    public void   setInProgress(int inProgress)           { this.inProgress = inProgress; }

    public int    getResolved()                           { return resolved; }
    public void   setResolved(int resolved)               { this.resolved = resolved; }

    public int    getUnassigned()                         { return unassigned; }
    public void   setUnassigned(int unassigned)           { this.unassigned = unassigned; }

    public int    getRejected()                           { return rejected; }
    public void   setRejected(int rejected)               { this.rejected = rejected; }

    public double getAvgResolutionHours()                 { return avgResolutionHours; }
    public void   setAvgResolutionHours(double h)         { this.avgResolutionHours = h; }
}
