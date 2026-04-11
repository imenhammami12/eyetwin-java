package com.eyetwin.services;

import com.eyetwin.entities.Planning;
import com.eyetwin.interfaces.IPlanningService;
import com.eyetwin.tools.DatabaseConfig;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PlanningServiceImpl — implémentation de IPlanningService.
 * Gère aussi l'upload d'images (uploads/plannings/).
 */
public class PlanningServiceImpl implements IPlanningService {

    private static final String UPLOAD_DIR = "uploads/plannings/";

    // ════════════════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════════════════

    @Override
    public Planning createPlanning(Planning planning, byte[] imageBytes, String imageExt)
            throws SQLException, IOException {
        // Upload image
        if (imageBytes != null && imageBytes.length > 0) {
            String filename = saveImage(imageBytes, imageExt);
            planning.setImage(filename);
        }
        return createPlanning(planning);
    }

    @Override
    public Planning createPlanning(Planning planning) throws SQLException, IOException {
        String sql = """
            INSERT INTO planning (image, date, time, localisation, description, 
                                  need_partner, level, type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, planning.getImage());
            ps.setDate(2, Date.valueOf(planning.getDate()));
            ps.setTime(3, Time.valueOf(planning.getTime()));
            ps.setString(4, planning.getLocalisation());
            ps.setString(5, planning.getDescription());
            ps.setBoolean(6, planning.isNeedPartner());
            ps.setString(7, planning.getLevel());
            ps.setString(8, planning.getType());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                planning.setIdPlanning(id);
            }
        }
        return planning;
    }

    // ════════════════════════════════════════════════════════════
    //  READ
    // ════════════════════════════════════════════════════════════

    @Override
    public Planning getPlanningById(int idPlanning) throws SQLException {
        String sql = "SELECT * FROM planning WHERE IDplanning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapPlanning(rs);
            }
        }
        return null;
    }

    @Override
    public Planning getPlanningWithDetails(int idPlanning) throws SQLException {
        Planning planning = getPlanningById(idPlanning);
        if (planning != null) {
            // Charger les reviews et sessions via leurs services
            ReviewServiceImpl reviewService = new ReviewServiceImpl();
            TrainingSessionServiceImpl sessionService = new TrainingSessionServiceImpl();
            
            planning.setReviews(reviewService.getReviewsByPlanning(idPlanning));
            planning.setTrainingSessions(sessionService.getSessionsByPlanning(idPlanning));
        }
        return planning;
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE
    // ════════════════════════════════════════════════════════════

    @Override
    public void updatePlanning(Planning planning, byte[] imageBytes, String imageExt)
            throws SQLException, IOException {
        // Upload nouvelle image si fournie
        if (imageBytes != null && imageBytes.length > 0) {
            // Supprimer ancienne image
            if (planning.getImage() != null) {
                deleteImage(planning.getImage());
            }
            String filename = saveImage(imageBytes, imageExt);
            planning.setImage(filename);
        }
        updatePlanning(planning);
    }

    @Override
    public void updatePlanning(Planning planning) throws SQLException, IOException {
        String sql = """
            UPDATE planning 
            SET image = ?, date = ?, time = ?, localisation = ?, 
                description = ?, need_partner = ?, level = ?, type = ?
            WHERE IDplanning = ?
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, planning.getImage());
            ps.setDate(2, Date.valueOf(planning.getDate()));
            ps.setTime(3, Time.valueOf(planning.getTime()));
            ps.setString(4, planning.getLocalisation());
            ps.setString(5, planning.getDescription());
            ps.setBoolean(6, planning.isNeedPartner());
            ps.setString(7, planning.getLevel());
            ps.setString(8, planning.getType());
            ps.setInt(9, planning.getIdPlanning());

            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════

    @Override
    public void deletePlanning(int idPlanning) throws SQLException, IOException {
        Planning planning = getPlanningById(idPlanning);
        if (planning == null) return;

        // Supprimer l'image
        if (planning.getImage() != null) {
            deleteImage(planning.getImage());
        }

        // Supprimer les reviews et sessions (cascade)
        String sql = "DELETE FROM planning WHERE IDplanning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LISTING
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Planning> getAllPlannings() throws SQLException {
        String sql = "SELECT * FROM planning ORDER BY date DESC, time DESC";
        List<Planning> plannings = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plannings.add(mapPlanning(rs));
            }
        }
        return plannings;
    }

    @Override
    public List<Planning> getPlanningsByType(String type) throws SQLException {
        String sql = "SELECT * FROM planning WHERE type = ? ORDER BY date DESC, time DESC";
        List<Planning> plannings = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plannings.add(mapPlanning(rs));
                }
            }
        }
        return plannings;
    }

    @Override
    public List<Planning> getPlanningsByLevel(String level) throws SQLException {
        String sql = "SELECT * FROM planning WHERE level = ? ORDER BY date DESC, time DESC";
        List<Planning> plannings = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, level);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plannings.add(mapPlanning(rs));
                }
            }
        }
        return plannings;
    }

    @Override
    public List<Planning> getUpcomingPlannings() throws SQLException {
        String sql = """
            SELECT * FROM planning 
            WHERE date >= CURDATE() 
            ORDER BY date ASC, time ASC
            """;
        List<Planning> plannings = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plannings.add(mapPlanning(rs));
            }
        }
        return plannings;
    }

    @Override
    public List<Planning> searchPlannings(String keyword) throws SQLException {
        String sql = """
            SELECT * FROM planning 
            WHERE description LIKE ? OR localisation LIKE ? OR type LIKE ?
            ORDER BY date DESC, time DESC
            """;
        List<Planning> plannings = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    plannings.add(mapPlanning(rs));
                }
            }
        }
        return plannings;
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════════

    @Override
    public int countParticipants(int idPlanning) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM training_session 
            WHERE ID_planning = ? AND status <> 'CANCELLED'
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public double getAverageRating(int idPlanning) throws SQLException {
        String sql = "SELECT AVG(rating) FROM review WHERE ID_planning = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPlanning);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════

    private Planning mapPlanning(ResultSet rs) throws SQLException {
        Planning p = new Planning();
        p.setIdPlanning(rs.getInt("IDplanning"));
        p.setImage(rs.getString("image"));
        p.setDate(rs.getDate("date").toLocalDate());
        p.setTime(rs.getTime("time").toLocalTime());
        p.setLocalisation(rs.getString("localisation"));
        p.setDescription(rs.getString("description"));
        p.setNeedPartner(rs.getBoolean("need_partner"));
        p.setLevel(rs.getString("level"));
        p.setType(rs.getString("type"));
        return p;
    }

    private String saveImage(byte[] imageBytes, String ext) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = "planning-" + UUID.randomUUID() + "." + ext;
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, imageBytes);
        return filename;
    }

    private void deleteImage(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) return;
        Path filePath = Paths.get(UPLOAD_DIR, filename);
        Files.deleteIfExists(filePath);
    }
}
