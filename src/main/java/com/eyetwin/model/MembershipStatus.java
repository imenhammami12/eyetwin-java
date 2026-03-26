package com.eyetwin.model;

public enum MembershipStatus {
    ACTIVE,
    INVITED,
    PENDING,
    INACTIVE,
    LEFT;

    public static MembershipStatus fromValue(String value) {
        for (MembershipStatus s : values()) {
            if (s.name().equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown MembershipStatus: " + value);
    }
}