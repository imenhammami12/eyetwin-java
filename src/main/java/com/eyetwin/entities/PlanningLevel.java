package com.eyetwin.entities;

import java.util.Arrays;
import java.util.Optional;

public enum PlanningLevel {
    BEGINNER("Beginner", "Beginner (Entry Level)"),
    INTERMEDIATE("Intermediate", "Intermediate (Mid Level)"),
    ADVANCED("Advanced", "Advanced (High Level)"),
    PROFESSIONAL("Professional", "Professional (Pro Level)");

    private final String dbValue;
    private final String label;

    PlanningLevel(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<PlanningLevel> fromDbValue(String value) {
        if (value == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(v -> v.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
