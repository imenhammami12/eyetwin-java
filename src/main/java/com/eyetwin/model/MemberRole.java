package com.eyetwin.model;

public enum MemberRole {
    OWNER("Owner"),
    CO_CAPTAIN("Co-Captain"),
    MEMBER("Member");

    private final String label;

    MemberRole(String label) { this.label = label; }

    public String getLabel() { return label; }

    /** Correspond au value Symfony stocké en BDD (ex: "OWNER") */
    public static MemberRole fromValue(String value) {
        for (MemberRole r : values()) {
            if (r.name().equalsIgnoreCase(value)) return r;
        }
        throw new IllegalArgumentException("Unknown MemberRole: " + value);
    }
}