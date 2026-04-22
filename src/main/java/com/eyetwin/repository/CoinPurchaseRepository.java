package com.eyetwin.repository;

import com.eyetwin.entities.CoinPurchase;
import com.eyetwin.entities.User;
import com.eyetwin.tools.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository JDBC pour CoinPurchase.
 * Miroir de CoinPurchaseRepository.php (Symfony/Doctrine)
 *
 * Table SQL attendue :
 * ┌──────────────────────────────────────────────────────────────┐
 * │ CREATE TABLE coin_purchase (                                 │
 * │   id              INT AUTO_INCREMENT PRIMARY KEY,           │
 * │   user_id         INT NOT NULL,                              │
 * │   coins_amount    INT NOT NULL DEFAULT 0,                   │
 * │   price_paid      DECIMAL(10,2) NOT NULL DEFAULT 0.00,      │
 * │   stripe_session_id VARCHAR(255),                           │
 * │   status          VARCHAR(50) NOT NULL DEFAULT 'pending',   │
 * │   created_at      DATETIME NOT NULL,                        │
 * │   completed_at    DATETIME,                                  │
 * │   FOREIGN KEY (user_id) REFERENCES user(id)                 │
 * │ );                                                           │
 * └──────────────────────────────────────────────────────────────┘
 */
public class CoinPurchaseRepository {

    // ══════════════════════════════════════════════════════════
    //  SAVE
    // ══════════════════════════════════════════════════════════

    /**
     * Insère un nouvel achat en base.
     * Retourne l'objet avec son id généré.
     */
    public CoinPurchase save(CoinPurchase purchase) {
        String sql = """
                INSERT INTO coin_purchase
                  (user_id, coins_amount, price_paid, stripe_session_id, status, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, purchase.getUser().getId());
            ps.setInt   (2, purchase.getCoinsAmount());
            ps.setBigDecimal(3, purchase.getPricePaid());
            ps.setString(4, purchase.getStripeSessionId());
            ps.setString(5, purchase.getStatus());
            ps.setTimestamp(6, Timestamp.valueOf(
                    purchase.getCreatedAt() != null ? purchase.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(7, purchase.getCompletedAt() != null
                    ? Timestamp.valueOf(purchase.getCompletedAt()) : null);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) purchase.setId(keys.getInt(1));
            }

        } catch (SQLException e) {
            System.err.println("[CoinPurchaseRepository] ❌ save() : " + e.getMessage());
        }

        return purchase;
    }

    // ══════════════════════════════════════════════════════════
    //  FIND BY STRIPE SESSION ID  (idempotence)
    // ══════════════════════════════════════════════════════════

    public Optional<CoinPurchase> findByStripeSessionId(String sessionId) {
        String sql = "SELECT * FROM coin_purchase WHERE stripe_session_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[CoinPurchaseRepository] ❌ findByStripeSessionId() : " + e.getMessage());
        }

        return Optional.empty();
    }

    // ══════════════════════════════════════════════════════════
    //  FIND BY USER
    // ══════════════════════════════════════════════════════════

    public List<CoinPurchase> findByUser(User user) {
        String sql = "SELECT * FROM coin_purchase WHERE user_id = ? ORDER BY created_at DESC";
        List<CoinPurchase> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("[CoinPurchaseRepository] ❌ findByUser() : " + e.getMessage());
        }

        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  FIND ALL  (admin)
    // ══════════════════════════════════════════════════════════

    public List<CoinPurchase> findAll() {
        String sql = "SELECT * FROM coin_purchase ORDER BY created_at DESC";
        List<CoinPurchase> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            System.err.println("[CoinPurchaseRepository] ❌ findAll() : " + e.getMessage());
        }

        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  UPDATE STATUS
    // ══════════════════════════════════════════════════════════

    public void updateStatus(int purchaseId, String status, LocalDateTime completedAt) {
        String sql = "UPDATE coin_purchase SET status = ?, completed_at = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString   (1, status);
            ps.setTimestamp(2, completedAt != null ? Timestamp.valueOf(completedAt) : null);
            ps.setInt      (3, purchaseId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[CoinPurchaseRepository] ❌ updateStatus() : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  MAPPING
    // ══════════════════════════════════════════════════════════

    private CoinPurchase mapRow(ResultSet rs) throws SQLException {
        CoinPurchase p = new CoinPurchase();
        p.setId(rs.getInt("id"));
        p.setCoinsAmount(rs.getInt("coins_amount"));
        p.setPricePaid(rs.getBigDecimal("price_paid"));
        p.setStripeSessionId(rs.getString("stripe_session_id"));
        p.setStatus(rs.getString("status"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());

        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) p.setCompletedAt(completed.toLocalDateTime());

        // Stub user (id seulement — le service fait le join si nécessaire)
        User user = new User();
        user.setId(rs.getInt("user_id"));
        p.setUser(user);

        return p;
    }
}
