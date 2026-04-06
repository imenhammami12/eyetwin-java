package com.eyetwin.entities;

public enum ComplaintCategory {

    TECHNICAL("Technical",  "bi-tools"),
    ACCOUNT("Account",      "bi-person-circle"),
    TOURNAMENT("Tournament","bi-trophy"),
    TEAM("Team",            "bi-people"),
    PAYMENT("Payment",      "bi-credit-card"),
    CONTENT("Content",      "bi-file-earmark-text"),
    HARASSMENT("Harassment","bi-shield-exclamation"),
    BUG("Bug",              "bi-bug"),
    OTHER("Other",          "bi-three-dots");

    private final String label;
    private final String icon;

    ComplaintCategory(String label, String icon) {
        this.label = label;
        this.icon  = icon;
    }

    public String getLabel() { return label; }
    public String getIcon()  { return icon; }

    /** Mirrors PHP getDescription() */
    public String getDescription() {
        return switch (this) {
            case TECHNICAL  -> "Technical problems with the platform";
            case ACCOUNT    -> "Issues related to your account";
            case TOURNAMENT -> "Problems with tournaments";
            case TEAM       -> "Team-related issues";
            case PAYMENT    -> "Payment and billing issues";
            case CONTENT    -> "Inappropriate content";
            case HARASSMENT -> "Report harassment or abuse";
            case BUG        -> "Report a bug or error";
            case OTHER      -> "Other issues not listed above";
        };
    }

    /** Mirrors PHP getDefaultPriority() */
    public ComplaintPriority getDefaultPriority() {
        return switch (this) {
            case HARASSMENT -> ComplaintPriority.URGENT;
            case PAYMENT, BUG -> ComplaintPriority.HIGH;
            case TECHNICAL    -> ComplaintPriority.MEDIUM;
            default           -> ComplaintPriority.MEDIUM;
        };
    }

    public static ComplaintCategory fromValue(String value) {
        if (value == null) return OTHER;
        for (ComplaintCategory c : values())
            if (c.name().equalsIgnoreCase(value)) return c;
        return OTHER;
    }
}
