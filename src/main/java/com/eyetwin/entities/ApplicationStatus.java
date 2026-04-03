package com.eyetwin.entities;

/**
 * ApplicationStatus — miroir de l'enum Symfony ApplicationStatus
 * Valeurs stockées en DB : PENDING | APPROVED | REJECTED
 */
public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
