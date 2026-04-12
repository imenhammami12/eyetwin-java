package com.eyetwin.entities;

import java.time.LocalDateTime;

public class AuditLog {

    private int id;
    private User user;
    private String action;
    private String entityType;
    private Integer entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {
        this.createdAt = LocalDateTime.now();
    }

    public AuditLog(int id, User user, String action, String entityType,
                    Integer entityId, String details, String ipAddress,
                    LocalDateTime createdAt) {
        this.id         = id;
        this.user       = user;
        this.action     = action;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.details    = details;
        this.ipAddress  = ipAddress;
        this.createdAt  = createdAt != null ? createdAt : LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int              getId()           { return id; }
    public void             setId(int id)     { this.id = id; }

    public User             getUser()                   { return user; }
    public void             setUser(User user)           { this.user = user; }

    public String           getAction()                       { return action; }
    public void             setAction(String action)           { this.action = action; }

    public String           getEntityType()                         { return entityType; }
    public void             setEntityType(String entityType)         { this.entityType = entityType; }

    public Integer          getEntityId()                      { return entityId; }
    public void             setEntityId(Integer entityId)       { this.entityId = entityId; }

    public String           getDetails()                     { return details; }
    public void             setDetails(String details)        { this.details = details; }

    public String           getIpAddress()                        { return ipAddress; }
    public void             setIpAddress(String ipAddress)         { this.ipAddress = ipAddress; }

    public LocalDateTime    getCreatedAt()                          { return createdAt; }
    public void             setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    /** Helper: action badge colour category */
    public String getActionCategory() {
        if (action == null) return "info";
        String a = action.toUpperCase();
        if (a.startsWith("DELETE") || a.startsWith("BAN") || a.contains("REJECT")) return "danger";
        if (a.startsWith("CREATE") || a.startsWith("APPROVE") || a.contains("ACTIVATED")) return "success";
        if (a.startsWith("UPDATE") || a.startsWith("EDIT") || a.contains("SUSPEND")) return "warning";
        return "info";
    }
}
