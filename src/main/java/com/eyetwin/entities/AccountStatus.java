package com.eyetwin.entities;

public enum AccountStatus {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    BANNED("BANNED"),
    PENDING("PENDING");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return switch (this) {
            case ACTIVE    -> "Active";
            case SUSPENDED -> "Suspended";
            case BANNED    -> "Banned";
            case PENDING   -> "Pending";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case ACTIVE    -> "success";
            case SUSPENDED -> "warning";
            case BANNED    -> "danger";
            case PENDING   -> "info";
        };
    }

    public String getDescription() {
        return switch (this) {
            case ACTIVE    -> "Account is active and can access the application";
            case SUSPENDED -> "Account is temporarily suspended. Access is restricted until reactivated.";
            case BANNED    -> "Account is permanently banned. User cannot access the application.";
            case PENDING   -> "Account is pending approval from an administrator";
        };
    }
}