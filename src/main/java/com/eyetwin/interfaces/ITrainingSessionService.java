package com.eyetwin.interfaces;

import com.eyetwin.entities.TrainingSession;

import java.sql.SQLException;
import java.util.List;

/**
 * ITrainingSessionService — contrat de la couche training session.
 */
public interface ITrainingSessionService {

    // ── CRUD Training Session ──────────────────────────────────
    TrainingSession joinSession(int idPlanning, int userId) throws SQLException;
    TrainingSession getSessionById(int idTraining) throws SQLException;
    void updateSessionStatus(int idTraining, String status) throws SQLException;
    void cancelSession(int idTraining, int userId) throws SQLException;

    // ── Listing ────────────────────────────────────────────────
    List<TrainingSession> getSessionsByPlanning(int idPlanning) throws SQLException;
    List<TrainingSession> getSessionsByUser(int userId) throws SQLException;
    List<TrainingSession> getActiveSessionsByUser(int userId) throws SQLException;

    // ── Validation ─────────────────────────────────────────────
    boolean hasUserJoinedPlanning(int userId, int idPlanning) throws SQLException;
    TrainingSession getUserSessionForPlanning(int userId, int idPlanning) throws SQLException;

    // ── Statistiques ───────────────────────────────────────────
    int countActiveParticipants(int idPlanning) throws SQLException;
}
