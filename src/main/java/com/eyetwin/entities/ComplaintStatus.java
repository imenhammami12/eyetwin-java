package com.eyetwin.entities;

// ═══════════════════════════════════════════════════════════
//  ComplaintStatus  (mirrors Symfony ComplaintStatus enum)
// ═══════════════════════════════════════════════════════════
public enum ComplaintStatus {

    PENDING("Pending",     "bi-hourglass-split",   "warning"),
    IN_PROGRESS("In Progress","bi-lightning-charge-fill","info"),
    RESOLVED("Resolved",   "bi-shield-check",      "success"),
    CLOSED("Closed",       "bi-lock-fill",         "secondary"),
    REJECTED("Rejected",   "bi-x-octagon-fill",    "danger");

    private final String label;
    private final String icon;
    private final String badgeClass;

    ComplaintStatus(String label, String icon, String badgeClass) {
        this.label      = label;
        this.icon       = icon;
        this.badgeClass = badgeClass;
    }

    public String getLabel()      { return label; }
    public String getIcon()       { return icon; }
    public String getBadgeClass() { return badgeClass; }

    /** JavaFX color for badges */
    public String getColor() {
        return switch (this) {
            case PENDING     -> "#ffd54f";
            case IN_PROGRESS -> "#4facfe";
            case RESOLVED    -> "#43e97b";
            case CLOSED      -> "rgba(255,255,255,0.5)";
            case REJECTED    -> "#ff6b6b";
        };
    }

    public String getBgColor() {
        return switch (this) {
            case PENDING     -> "rgba(255,213,79,0.15)";
            case IN_PROGRESS -> "rgba(79,172,254,0.15)";
            case RESOLVED    -> "rgba(67,233,123,0.15)";
            case CLOSED      -> "rgba(255,255,255,0.07)";
            case REJECTED    -> "rgba(255,107,107,0.15)";
        };
    }

    /** Mirrors PHP isFinal() */
    public boolean isFinal() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }

    /** Mirrors PHP allowedTransitions() — domain-level guard */
    public ComplaintStatus[] allowedTransitions() {
        return switch (this) {
            case PENDING     -> new ComplaintStatus[]{IN_PROGRESS, REJECTED};
            case IN_PROGRESS -> new ComplaintStatus[]{RESOLVED, REJECTED};
            case RESOLVED    -> new ComplaintStatus[]{CLOSED};
            case CLOSED, REJECTED -> new ComplaintStatus[]{};
        };
    }

    public boolean canTransitionTo(ComplaintStatus target) {
        for (ComplaintStatus s : allowedTransitions())
            if (s == target) return true;
        return false;
    }

    public static ComplaintStatus fromValue(String value) {
        for (ComplaintStatus s : values())
            if (s.name().equalsIgnoreCase(value)) return s;
        return PENDING;
    }

    /** Mirrors PHP tryFromLabel() */
    public static ComplaintStatus tryFromLabel(String label) {
        for (ComplaintStatus s : values())
            if (s.label.equalsIgnoreCase(label)) return s;
        return null;
    }
}
