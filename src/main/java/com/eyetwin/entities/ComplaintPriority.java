package com.eyetwin.entities;

public enum ComplaintPriority {

    LOW("Low",       "bi-arrow-down",       "secondary", 1),
    MEDIUM("Medium", "bi-dash-circle",      "primary",   2),
    HIGH("High",     "bi-arrow-up",         "warning",   3),
    URGENT("Urgent", "bi-exclamation-circle-fill", "danger", 4);

    private final String label;
    private final String icon;
    private final String badgeClass;
    private final int    sortOrder;   // higher = more urgent (matches Symfony ORDER BY priority DESC)

    ComplaintPriority(String label, String icon, String badgeClass, int sortOrder) {
        this.label     = label;
        this.icon      = icon;
        this.badgeClass = badgeClass;
        this.sortOrder = sortOrder;
    }

    public String getLabel()      { return label; }
    public String getIcon()       { return icon; }
    public String getBadgeClass() { return badgeClass; }
    public int    getSortOrder()  { return sortOrder; }

    public String getColor() {
        return switch (this) {
            case LOW    -> "rgba(255,255,255,0.5)";
            case MEDIUM -> "#4facfe";
            case HIGH   -> "#ffd54f";
            case URGENT -> "#ff6b6b";
        };
    }

    /** Mirrors PHP getWeight() */
    public int getWeight() { return sortOrder; }

    /** Mirrors PHP orderedByUrgency() */
    public static ComplaintPriority[] orderedByUrgency() {
        return new ComplaintPriority[]{URGENT, HIGH, MEDIUM, LOW};
    }

    /** Mirrors PHP fromStringOrDefault() */
    public static ComplaintPriority fromStringOrDefault(String value) {
        if (value == null) return MEDIUM;
        for (ComplaintPriority p : values())
            if (p.name().equalsIgnoreCase(value)) return p;
        return MEDIUM;
    }

    public static ComplaintPriority fromValue(String value) {
        return fromStringOrDefault(value);
    }
}
