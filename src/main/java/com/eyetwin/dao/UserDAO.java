package com.eyetwin.dao;

import com.eyetwin.config.DatabaseConfig;
import com.eyetwin.model.User;

import java.sql.*;
import java.time.LocalDateTime;

public class UserDAO {

    // ═══════════════════════════════════════════════════════════
    //  FIND BY EMAIL
    // ═══════════════════════════════════════════════════════════

    public User findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapUser(rs);

        } catch (SQLException e) {
            System.err.println("❌ Erreur findByEmail: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    //  FIND BY ID
    // ═══════════════════════════════════════════════════════════

    public User findById(int id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapUser(rs);

        } catch (SQLException e) {
            System.err.println("❌ Erreur findById: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    //  EMAIL EXISTS
    // ═══════════════════════════════════════════════════════════

    public boolean emailExists(String email) {
        String sql = "SELECT id FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            return stmt.executeQuery().next();

        } catch (SQLException e) {
            System.err.println("❌ Erreur emailExists: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SAVE — inscription nouvel utilisateur
    // ═══════════════════════════════════════════════════════════

    public boolean save(String fullName, String email, String hashedPassword) {
        String sql = "INSERT INTO `user` " +
                "(email, username, roles_json, password, account_status, " +
                " full_name, created_at, last_login, coin_balance, is_totp_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0, 0)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Username unique généré depuis l'email
            String username = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + (int)(Math.random() * 9000 + 1000);

            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, "[\"ROLE_USER\"]");   // format JSON Symfony exact
            stmt.setString(4, hashedPassword);
            stmt.setString(5, "ACTIVE");
            stmt.setString(6, fullName);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Erreur save: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UPDATE — mise à jour complète (inclut les champs 2FA)
    //  Appelé par TwoFactorAuthService après chaque modification
    // ═══════════════════════════════════════════════════════════

    public void update(User user) {
        String sql = "UPDATE `user` SET " +
                "email             = ?, " +
                "username          = ?, " +
                "full_name         = ?, " +
                "bio               = ?, " +
                "profile_picture   = ?, " +
                "account_status    = ?, " +
                "coin_balance      = ?, " +
                "last_login        = ?, " +
                // ── 2FA ──
                "totp_secret       = ?, " +
                "is_totp_enabled   = ?, " +
                "backup_codes_json = ?, " +
                "totp_enabled_at   = ?, " +
                // ── Contact / Face ──
                "phone             = ?, " +
                "telegram_chat_id  = ?, " +
                "face_descriptor   = ?, " +
                "face_image        = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;

            // ── Champs de base ──
            stmt.setString(i++, user.getEmail());
            stmt.setString(i++, user.getUsername());
            stmt.setString(i++, user.getFullName());
            stmt.setString(i++, user.getBio());
            stmt.setString(i++, user.getProfilePicture());
            stmt.setString(i++, user.getAccountStatus());
            stmt.setInt(i++,    user.getCoinBalance());
            stmt.setTimestamp(i++, user.getLastLogin() != null
                    ? Timestamp.valueOf(user.getLastLogin()) : null);

            // ── 2FA — on passe le JSON brut, sans sérialisation supplémentaire ──
            stmt.setString(i++,  user.getTotpSecret());
            stmt.setBoolean(i++, user.isTotpEnabled());
            stmt.setString(i++,  user.getBackupCodesJson());   // TEXT JSON brut
            stmt.setTimestamp(i++, user.getTotpEnabledAt() != null
                    ? Timestamp.valueOf(user.getTotpEnabledAt()) : null);

            // ── Contact / Face ──
            stmt.setString(i++, user.getPhone());
            stmt.setString(i++, user.getTelegramChatId());
            stmt.setString(i++, user.getFaceDescriptor());
            stmt.setString(i++, user.getFaceImage());

            // ── WHERE ──
            stmt.setInt(i, user.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  VERIFY PASSWORD
    //  Utilisé par TwoFactorSettingsController (désactivation 2FA)
    // ═══════════════════════════════════════════════════════════

    /**
     * Vérifie le mot de passe d'un utilisateur.
     *
     * Symfony utilise BCrypt — si votre appli Java ne hashe pas encore,
     * remplacez la comparaison directe par BCrypt.checkpw() :
     *
     *   Dépendance pom.xml :
     *   <dependency>
     *     <groupId>de.svenkubiak</groupId>
     *     <artifactId>jBCrypt</artifactId>
     *     <version>0.4.3</version>
     *   </dependency>
     *
     *   Utilisation : return BCrypt.checkpw(plainPassword, hashedPassword);
     */
    public boolean verifyPassword(String email, String plainPassword) {
        String sql = "SELECT password FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String stored = rs.getString("password");
                // BCrypt check — works with Symfony's hashed passwords
                return at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
                        .verify(plainPassword.toCharArray(), stored)
                        .verified;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur verifyPassword: " + e.getMessage());
        }
        return false;
    }
    // ═══════════════════════════════════════════════════════════
    //  MAP USER — mapping exact des colonnes DB → User Java
    // ═══════════════════════════════════════════════════════════

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        // ── Champs de base ──
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRolesJson(rs.getString("roles_json"));
        user.setAccountStatus(rs.getString("account_status"));
        user.setFullName(rs.getString("full_name"));        // colonne full_name ✓
        user.setBio(rs.getString("bio"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setCoinBalance(rs.getInt("coin_balance"));     // colonne coin_balance ✓

        // ── Dates ──
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) user.setLastLogin(lastLogin.toLocalDateTime());

        // ── 2FA ──
        user.setTotpSecret(rs.getString("totp_secret"));

        // is_totp_enabled : TINYINT(4) — getBoolean() fonctionne (0=false, 1=true)
        user.setIsTotpEnabled(rs.getBoolean("is_totp_enabled"));

        // backup_codes_json : on stocke le JSON brut tel quel
        // getBackupCodes() dans User.java parse automatiquement
        user.setBackupCodesJson(rs.getString("backup_codes_json"));

        Timestamp totpEnabledAt = rs.getTimestamp("totp_enabled_at");
        if (totpEnabledAt != null) user.setTotpEnabledAt(totpEnabledAt.toLocalDateTime());

        // ── Contact / Face ──
        user.setPhone(rs.getString("phone"));
        user.setTelegramChatId(rs.getString("telegram_chat_id"));
        user.setFaceDescriptor(rs.getString("face_descriptor"));
        user.setFaceImage(rs.getString("face_image"));

        return user;
    }


    // ═══════════════════════════════════════════════════════════
//  SAVE PROFILE PICTURE — stores image bytes to disk and
//  updates the profile_picture column in DB
// ═══════════════════════════════════════════════════════════

    public void saveProfilePicture(int userId, byte[] imageBytes, String filename) throws Exception {
        // 1. Write file to uploads/profiles/
        java.nio.file.Path uploadDir = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "uploads", "profiles");
        java.nio.file.Files.createDirectories(uploadDir);
        java.nio.file.Files.write(uploadDir.resolve(filename), imageBytes);

        // 2. Update the DB column
        String sql = "UPDATE `user` SET profile_picture = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, filename);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Erreur saveProfilePicture: " + e.getMessage());
            throw e;
        }
    }
}