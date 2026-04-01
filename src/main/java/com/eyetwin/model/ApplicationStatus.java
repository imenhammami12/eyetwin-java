package com.eyetwin.model;

/**
 * ApplicationStatus — miroir de l'enum Symfony ApplicationStatus
 * Valeurs stockées en DB : PENDING | APPROVED | REJECTED
 */
public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
