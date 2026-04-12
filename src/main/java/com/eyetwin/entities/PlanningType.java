package com.eyetwin.entities;

import java.util.Arrays;
import java.util.Optional;

public enum PlanningType {
    FPS("FPS", "First Person Shooter"),
    MOBA("MOBA", "Multiplayer Online Battle Arena"),
    BATTLE_ROYALE("Battle Royale", "Battle Royale (BR)"),
    SPORT("Sport", "Sports Games"),
    COMBAT("Combat", "Fighting Games"),
    RPG_MMORPG("RPG/MMORPG", "Role-Playing Game / Massively Multiplayer Online RPG"),
    STRATEGY("Stratégie", "RTS / TBS");

    private final String dbValue;
    private final String label;

    PlanningType(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<PlanningType> fromDbValue(String value) {
        if (value == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(v -> v.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
