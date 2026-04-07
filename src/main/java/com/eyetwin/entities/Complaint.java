package com.eyetwin.entities;

import java.time.LocalDateTime;

public class Complaint {

    private int    id;
    private String subject;
    private String description;
    private String attachmentPath;
    private String adminResponse;
    private String resolutionNotes;

    private ComplaintStatus   status;
    private ComplaintPriority priority;
    private ComplaintCategory category;

    // Sentiment analysis
    private String  sentimentLabel;
    private Double  sentimentScore;          // ← Double (nullable), was double
    private String  sentimentSource;
    private String  sentimentPrioritySuggestion;

    private User submittedBy;
    private User assignedTo;

    private LocalDateTime createdAt;         // ← LocalDateTime throughout, no Timestamp
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    // ─── Constructors ────────────────────────────────────────────
    public Complaint() {
        this.createdAt = LocalDateTime.now();
        this.status    = ComplaintStatus.PENDING;
        this.priority  = ComplaintPriority.MEDIUM;
        this.category  = ComplaintCategory.OTHER;
    }

    public Complaint(int id, String subject, String description,
                     ComplaintStatus status, ComplaintPriority priority,
                     ComplaintCategory category, LocalDateTime createdAt) {
        this.id          = id;
        this.subject     = subject;
        this.description = description;
        this.status      = status;
        this.priority    = priority;
        this.category    = category;
        this.createdAt   = createdAt;
    }

    // ─── Sentiment helpers ───────────────────────────────────────
    public boolean hasSentiment() {
        return sentimentLabel != null && !sentimentLabel.isBlank();
    }

    public String getSentimentEmoji() {
        if (sentimentLabel == null) return "😐";
        return switch (sentimentLabel) {
            case "POSITIVE" -> "😊";
            case "NEGATIVE" -> "😡";
            default         -> "😐";
        };
    }

    public String getSentimentTextLabel() {
        if (sentimentLabel == null) return "Unknown";
        return switch (sentimentLabel) {
            case "POSITIVE" -> "Positive";
            case "NEGATIVE" -> "Negative";
            default         -> "Neutral";
        };
    }

    public String getSentimentBadgeClass() {
        if (sentimentLabel == null) return "secondary";
        return switch (sentimentLabel) {
            case "POSITIVE" -> "success";
            case "NEGATIVE" -> "danger";
            default         -> "warning";
        };
    }

    public String getSentimentColor() {
        if (sentimentLabel == null) return "rgba(255,255,255,0.5)";
        return switch (sentimentLabel) {
            case "POSITIVE" -> "#43e97b";
            case "NEGATIVE" -> "#ff6b6b";
            default         -> "#ffd54f";
        };
    }

    public boolean isResolved() {
        return status == ComplaintStatus.RESOLVED || status == ComplaintStatus.CLOSED;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public int     getId()                                   { return id; }
    public void    setId(int id)                             { this.id = id; }

    public String  getSubject()                              { return subject; }
    public void    setSubject(String subject)                { this.subject = subject; }

    public String  getDescription()                          { return description; }
    public void    setDescription(String description)        { this.description = description; }

    public String  getAttachmentPath()                       { return attachmentPath; }
    public void    setAttachmentPath(String attachmentPath)  { this.attachmentPath = attachmentPath; }

    public String  getAdminResponse()                        { return adminResponse; }
    public void    setAdminResponse(String adminResponse)    { this.adminResponse = adminResponse; }

    public String  getResolutionNotes()                      { return resolutionNotes; }
    public void    setResolutionNotes(String resolutionNotes){ this.resolutionNotes = resolutionNotes; }

    public ComplaintStatus   getStatus()                     { return status; }
    public void              setStatus(ComplaintStatus s)    { this.status = s; }

    public ComplaintPriority getPriority()                   { return priority; }
    public void              setPriority(ComplaintPriority p){ this.priority = p; }

    public ComplaintCategory getCategory()                   { return category; }
    public void              setCategory(ComplaintCategory c){ this.category = c; }

    public String  getSentimentLabel()                       { return sentimentLabel; }
    public void    setSentimentLabel(String l)               { this.sentimentLabel = l; }

    /** Returns Double (nullable) — use != null before unboxing */
    public Double  getSentimentScore()                       { return sentimentScore; }
    public void    setSentimentScore(Double s)               { this.sentimentScore = s; }

    public String  getSentimentSource()                      { return sentimentSource; }
    public void    setSentimentSource(String s)              { this.sentimentSource = s; }

    public String  getSentimentPrioritySuggestion()                  { return sentimentPrioritySuggestion; }
    public void    setSentimentPrioritySuggestion(String suggestion) { this.sentimentPrioritySuggestion = suggestion; }

    public User    getSubmittedBy()                          { return submittedBy; }
    public void    setSubmittedBy(User u)                    { this.submittedBy = u; }

    public User    getAssignedTo()                           { return assignedTo; }
    public void    setAssignedTo(User u)                     { this.assignedTo = u; }

    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void          setCreatedAt(LocalDateTime dt)      { this.createdAt = dt; }

    public LocalDateTime getUpdatedAt()                      { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime dt)      { this.updatedAt = dt; }

    public LocalDateTime getResolvedAt()                     { return resolvedAt; }
    public void          setResolvedAt(LocalDateTime dt)     { this.resolvedAt = dt; }

    @Override
    public String toString() {
        return "Complaint{id=" + id + ", subject='" + subject + "', status=" + status + "}";
    }
}