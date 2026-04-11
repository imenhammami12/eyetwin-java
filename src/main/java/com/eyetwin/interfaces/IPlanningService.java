package com.eyetwin.interfaces;

import com.eyetwin.entities.Planning;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * IPlanningService — contrat de la couche planning.
 */
public interface IPlanningService {

    // ── CRUD Planning ──────────────────────────────────────────
    Planning createPlanning(Planning planning, byte[] imageBytes, String imageExt)
            throws SQLException, IOException;
    Planning createPlanning(Planning planning) throws SQLException, IOException;

    Planning getPlanningById(int idPlanning) throws SQLException;
    Planning getPlanningWithDetails(int idPlanning) throws SQLException;

    void updatePlanning(Planning planning, byte[] imageBytes, String imageExt)
            throws SQLException, IOException;
    void updatePlanning(Planning planning) throws SQLException, IOException;

    void deletePlanning(int idPlanning) throws SQLException, IOException;

    // ── Listing ────────────────────────────────────────────────
    List<Planning> getAllPlannings() throws SQLException;
    List<Planning> getPlanningsByType(String type) throws SQLException;
    List<Planning> getPlanningsByLevel(String level) throws SQLException;
    List<Planning> getUpcomingPlannings() throws SQLException;
    List<Planning> searchPlannings(String keyword) throws SQLException;

    // ── Statistiques ───────────────────────────────────────────
    int countParticipants(int idPlanning) throws SQLException;
    double getAverageRating(int idPlanning) throws SQLException;
}
