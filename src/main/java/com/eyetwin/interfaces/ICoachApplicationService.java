package com.eyetwin.interfaces;

import com.eyetwin.entities.CoachApplication;

import java.sql.SQLException;

/**
 * ICoachApplicationService — contrat demande de coaching.
 * Fusionne CoachApplicationDAO Symfony.
 */
public interface ICoachApplicationService {

    /** Vérifie si l'utilisateur a déjà une demande PENDING */
    boolean hasPendingApplication(int userId) throws SQLException;

    /** Enregistre une nouvelle demande (status = PENDING) */
    void save(CoachApplication app) throws SQLException;

    /** Retourne la dernière demande de l'utilisateur (desc date) */
    CoachApplication findLatestByUserId(int userId) throws SQLException;
}