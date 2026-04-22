package com.eyetwin.entities;

/**
 * ApplicationStatus — miroir du enum Symfony ApplicationStatus
 */
public enum ApplicationStatus {
    PENDING("Pending",     "warning", "⏳"),
    UNDER_REVIEW("Under Review", "info",    "👁"),
    APPROVED("Approved",   "success", "✓"),
    REJECTED("Rejected",   "danger",  "✕");

    private final String label;
    private final String badgeVariant; // success / warning / danger / info
    private final String icon;

    ApplicationStatus(String label, String badgeVariant, String icon) {
        this.label        = label;
        this.badgeVariant = badgeVariant;
        this.icon         = icon;
    }

    public String getLabel()        { return label; }
    public String getBadgeVariant() { return badgeVariant; }
    public String getIcon()         { return icon; }

    public static ApplicationStatus fromValue(String value) {
        if (value == null) return PENDING;
        for (ApplicationStatus s : values()) {
            if (s.name().equalsIgnoreCase(value)) return s;
        }
        return PENDING;
    }
}
