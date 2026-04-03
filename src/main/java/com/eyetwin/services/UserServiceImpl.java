package com.eyetwin.services;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.tools.DatabaseConfig;
import com.eyetwin.tools.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * UserServiceImpl — implémentation de IUserService.
 *
 * Fusionne l'ancien UserDAO (accès SQL) + AuthService (logique métier).
 * Le prof interdit les DAOs : toute la logique SQL est ici directement.
 */
public class UserServiceImpl implements IUserService {

    // ════════════════════════════════════════════════════════════
    //  FIND BY EMAIL
    // ════════════════════════════════════════════════════════════

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ findByEmail: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  FIND BY ID
    // ════════════════════════════════════════════════════════════

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ findById: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  EMAIL EXISTS
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean emailExists(String email) {
        String sql = "SELECT id FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("❌ emailExists: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  SAVE — inscription
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean save(String fullName, String email, String hashedPassword) {
        String sql = "INSERT INTO `user` " +
                "(email, username, roles_json, password, account_status, " +
                " full_name, created_at, last_login, coin_balance, is_totp_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0, 0)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String username = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + (int)(Math.random() * 9000 + 1000);
            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, "[\"ROLE_USER\"]");
            stmt.setString(4, hashedPassword);
            stmt.setString(5, "ACTIVE");
            stmt.setString(6, fullName);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ save: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  UPDATE — mise à jour complète
    // ════════════════════════════════════════════════════════════

    @Override
    public void update(User user) {
        String sql = "UPDATE `user` SET " +
                "email = ?, username = ?, full_name = ?, bio = ?, " +
                "profile_picture = ?, account_status = ?, coin_balance = ?, last_login = ?, " +
                "totp_secret = ?, is_totp_enabled = ?, backup_codes_json = ?, totp_enabled_at = ?, " +
                "phone = ?, telegram_chat_id = ?, face_descriptor = ?, face_image = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setString(i++, user.getEmail());
            stmt.setString(i++, user.getUsername());
            stmt.setString(i++, user.getFullName());
            stmt.setString(i++, user.getBio());
            stmt.setString(i++, user.getProfilePicture());
            stmt.setString(i++, user.getAccountStatus());
            stmt.setInt(i++, user.getCoinBalance());
            stmt.setTimestamp(i++, user.getLastLogin() != null
                    ? Timestamp.valueOf(user.getLastLogin()) : null);
            stmt.setString(i++, user.getTotpSecret());
            stmt.setBoolean(i++, user.isTotpEnabled());
            stmt.setString(i++, user.getBackupCodesJson());
            stmt.setTimestamp(i++, user.getTotpEnabledAt() != null
                    ? Timestamp.valueOf(user.getTotpEnabledAt()) : null);
            stmt.setString(i++, user.getPhone());
            stmt.setString(i++, user.getTelegramChatId());
            stmt.setString(i++, user.getFaceDescriptor());
            stmt.setString(i++, user.getFaceImage());
            stmt.setInt(i, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  SAVE PROFILE PICTURE
    // ════════════════════════════════════════════════════════════

    @Override
    public void saveProfilePicture(int userId, byte[] imageBytes, String filename) throws Exception {
        java.nio.file.Path uploadDir = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "uploads", "profiles");
        java.nio.file.Files.createDirectories(uploadDir);
        java.nio.file.Files.write(uploadDir.resolve(filename), imageBytes);

        String sql = "UPDATE `user` SET profile_picture = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filename);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LOGIN — logique métier (ex-AuthService)
    // ════════════════════════════════════════════════════════════

    @Override
    public User login(String email, String password) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Invalid email format.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required.");

        User user = findByEmail(email.toLowerCase().trim());
        if (user == null) return null;

        // Compatibilité Symfony : $2y$ → $2a$
        String hash = user.getPassword();
        if (hash != null && hash.startsWith("$2y$")) {
            hash = "$2a$" + hash.substring(4);
        }
        if (!BCrypt.checkpw(password, hash)) return null;

        String status = user.getAccountStatus();
        if (status == null || status.isBlank()) return null;
        switch (status.toUpperCase()) {
            case "ACTIVE"    -> {}
            case "BANNED"    -> throw new IllegalStateException("Your account has been banned.");
            case "SUSPENDED" -> throw new IllegalStateException("Your account is suspended.");
            default          -> throw new IllegalStateException("Account not active: " + status);
        }

        SessionManager.setCurrentUser(user);
        System.out.println("✅ Logged in: " + user.getEmail());
        return user;
    }

    // ════════════════════════════════════════════════════════════
    //  REGISTER — logique métier (ex-AuthService)
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean register(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Full name is required.");
        if (fullName.trim().length() < 2)
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Invalid email format.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required.");
        if (password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"))
            throw new IllegalArgumentException(
                    "Password must contain uppercase, lowercase and a number.");

        String normalizedEmail = email.toLowerCase().trim();
        if (emailExists(normalizedEmail)) return false;

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        return save(fullName.trim(), normalizedEmail, hashed);
    }

    // ════════════════════════════════════════════════════════════
    //  VERIFY PASSWORD
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean verifyPassword(String email, String plainPassword) {
        String sql = "SELECT password FROM `user` WHERE email = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                return at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
                        .verify(plainPassword.toCharArray(), stored)
                        .verified;
            }
        } catch (SQLException e) {
            System.err.println("❌ verifyPassword: " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  LOGOUT
    // ════════════════════════════════════════════════════════════

    @Override
    public void logout() {
        SessionManager.logout();
    }

    // ════════════════════════════════════════════════════════════
    //  RBAC HELPERS
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean hasRole(String role) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return false;
        return user.getRolesJson() != null && user.getRolesJson().contains(role);
    }

    @Override
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ROLE_SUPER_ADMIN");
    }

    @Override
    public boolean isCoach() {
        return hasRole("ROLE_COACH");
    }

    @Override
    public boolean isLoggedIn() {
        return SessionManager.getCurrentUser() != null;
    }

    // ════════════════════════════════════════════════════════════
    //  MAPPING ResultSet → User
    // ════════════════════════════════════════════════════════════

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRolesJson(rs.getString("roles_json"));
        user.setAccountStatus(rs.getString("account_status"));
        user.setFullName(rs.getString("full_name"));
        user.setBio(rs.getString("bio"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setCoinBalance(rs.getInt("coin_balance"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) user.setLastLogin(lastLogin.toLocalDateTime());

        user.setTotpSecret(rs.getString("totp_secret"));
        user.setIsTotpEnabled(rs.getBoolean("is_totp_enabled"));
        user.setBackupCodesJson(rs.getString("backup_codes_json"));

        Timestamp totpEnabledAt = rs.getTimestamp("totp_enabled_at");
        if (totpEnabledAt != null) user.setTotpEnabledAt(totpEnabledAt.toLocalDateTime());

        user.setPhone(rs.getString("phone"));
        user.setTelegramChatId(rs.getString("telegram_chat_id"));
        user.setFaceDescriptor(rs.getString("face_descriptor"));
        user.setFaceImage(rs.getString("face_image"));
        return user;
    }
}
