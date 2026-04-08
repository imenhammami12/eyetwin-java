package com.eyetwin.interfaces;

import com.eyetwin.entities.CoachApplication;
import com.eyetwin.entities.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * ICoachApplicationService — contrat demande de coaching.
 * Fusionne CoachApplicationDAO Symfony.
 */
public interface ICoachApplicationService {

    // ── CRUD / Listing ────────────────────────────────────────────
    List<CoachApplication> getAllApplications();
    CoachApplication       findById(int id);
    List<User>             getAllCoaches();

    /** Retourne la dernière demande de l'utilisateur (desc date) */
    CoachApplication findLatestByUserId(int userId) throws SQLException;

    // ── Actions ───────────────────────────────────────────────────
    void approve(int applicationId, String comment, int adminUserId);
    void reject (int applicationId, String comment, int adminUserId);

    /** Enregistre une nouvelle demande (status = PENDING) */
    void save(CoachApplication app) throws SQLException;

    // ── Vérifications ─────────────────────────────────────────────
    /** Vérifie si l'utilisateur a déjà une demande PENDING */
    boolean hasPendingApplication(int userId) throws SQLException;

    // ── Stats ─────────────────────────────────────────────────────
    Map<String, Integer> getGlobalStats();
}