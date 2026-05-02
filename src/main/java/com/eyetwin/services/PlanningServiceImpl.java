package com.eyetwin.services;

import com.eyetwin.entities.Planning;
import com.eyetwin.interfaces.IPlanningService;
import com.eyetwin.tools.DatabaseConfig;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlanningServiceImpl implements IPlanningService {

    private static final String UPLOAD_DIR = "uploads/plannings/";

    // ── Helper centralisé ────────────────────────────────────────────────────
    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════════════════

    @Override
    public Planning createPlanning(Planning planning, byte[] imageBytes, String imageExt)
            throws SQLException, IOException {
        if (imageBytes != null && imageBytes.length > 0)
            planning.setImage(saveImage(imageBytes, imageExt));
        return createPlanning(planning);
    }

    @Override
    public Planning createPlanning(Planning planning) throws SQLException, IOException {
        String sql = """
            INSERT INTO planning (image, date, time, localisation, description,
                                  need_partner, level, type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
        if (rs.next()) planning.setIdPlanning(rs.getInt(1));
        return planning;
    }

    // ════════════════════════════════════════════════════════════
    //  READ
    // ════════════════════════════════════════════════════════════

    @Override
    public Planning getPlanningById(int idPlanning) throws SQLException {
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM planning WHERE IDplanning = ?");
        ps.setInt(1, idPlanning);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapPlanning(rs);
        return null;
    }

    @Override
    public Planning getPlanningWithDetails(int idPlanning) throws SQLException {
        Planning planning = getPlanningById(idPlanning);
        if (planning != null) {
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
        if (imageBytes != null && imageBytes.length > 0) {
            if (planning.getImage() != null) deleteImage(planning.getImage());
            planning.setImage(saveImage(imageBytes, imageExt));
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
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
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

    // ════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════

    @Override
    public void deletePlanning(int idPlanning) throws SQLException, IOException {
        Planning planning = getPlanningById(idPlanning);
        if (planning == null) return;
        if (planning.getImage() != null) deleteImage(planning.getImage());
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "DELETE FROM planning WHERE IDplanning = ?");
        ps.setInt(1, idPlanning);
        ps.executeUpdate();
    }

    // ════════════════════════════════════════════════════════════
    //  LISTING
    // ════════════════════════════════════════════════════════════

    @Override
    public List<Planning> getAllPlannings() throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM planning ORDER BY date DESC, time DESC");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) plannings.add(mapPlanning(rs));
        return plannings;
    }

    @Override
    public List<Planning> getPlanningsByType(String type) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM planning WHERE type = ? ORDER BY date DESC, time DESC");
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) plannings.add(mapPlanning(rs));
        return plannings;
    }

    @Override
    public List<Planning> getPlanningsByLevel(String level) throws SQLException {
        List<Planning> plannings = new ArrayList<>();
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM planning WHERE level = ? ORDER BY date DESC, time DESC");
        ps.setString(1, level);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) plannings.add(mapPlanning(rs));
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
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) plannings.add(mapPlanning(rs));
        return plannings;
    }

    @Override
    public List<Planning> searchPlannings(String keyword) throws SQLException {
        String sql = """
            SELECT * FROM planning
            WHERE description LIKE ? OR localisation LIKE ? OR type LIKE ?
            ORDER BY date DESC, time DESC
            """;
        String pattern = "%" + keyword + "%";
        List<Planning> plannings = new ArrayList<>();
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ps.setString(3, pattern);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) plannings.add(mapPlanning(rs));
        return plannings;
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════════

    @Override
    public int countParticipants(int idPlanning) throws SQLException {
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement("""
            SELECT COUNT(*) FROM training_session
            WHERE ID_planning = ? AND status <> 'CANCELLED'
            """);
        ps.setInt(1, idPlanning);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    @Override
    public double getAverageRating(int idPlanning) throws SQLException {
        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(
                "SELECT AVG(rating) FROM review WHERE ID_planning = ?");
        ps.setInt(1, idPlanning);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble(1) : 0.0;
    }

    // ════════════════════════════════════════════════════════════
    //  MAPPING
    // ════════════════════════════════════════════════════════════

    private Planning mapPlanning(ResultSet rs) throws SQLException {
        Planning p = new Planning();
        p.setIdPlanning(rs.getInt("IDplanning"));
        p.setImage(rs.getString("image"));
        
        Date d = rs.getDate("date");
        if (d != null) p.setDate(d.toLocalDate());
        
        Time t = rs.getTime("time");
        if (t != null) p.setTime(t.toLocalTime());
        
        p.setLocalisation(rs.getString("localisation"));
        p.setDescription(rs.getString("description"));
        p.setNeedPartner(rs.getBoolean("need_partner"));
        p.setLevel(rs.getString("level"));
        p.setType(rs.getString("type"));
        return p;
    }

    private String saveImage(byte[] imageBytes, String ext) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String filename = "planning-" + UUID.randomUUID() + "." + ext;
        Files.write(uploadPath.resolve(filename), imageBytes);
        return filename;
    }

    private void deleteImage(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) return;
        Files.deleteIfExists(Paths.get(UPLOAD_DIR, filename));
    }
}