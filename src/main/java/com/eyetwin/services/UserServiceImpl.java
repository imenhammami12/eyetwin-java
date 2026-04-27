package com.eyetwin.services;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.tools.DatabaseConfig;
import com.eyetwin.tools.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl implements IUserService {

    // ── Helper centralisé ────────────────────────────────────────────────────
    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════════
    // FIND BY EMAIL
    // ════════════════════════════════════════════════════════════

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE email = ?";
        try {
            Connection conn = getConnection();
            if (conn == null) {
                System.err
                        .println("❌ findByEmail: Connection is NULL. Database might be down or credentials incorrect.");
                return null;
            }
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ findByEmail: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    // FIND BY ID
    // ════════════════════════════════════════════════════════════

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ findById: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    // FIND BY USERNAME
    // ════════════════════════════════════════════════════════════

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM `user` WHERE username = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("❌ findByUsername: " + e.getMessage());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════
    // EMAIL EXISTS
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean emailExists(String email) {
        String sql = "SELECT id FROM `user` WHERE email = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("❌ emailExists: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // GET ALL USERS
    // ════════════════════════════════════════════════════════════

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM `user` ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                users.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("❌ getAllUsers: " + e.getMessage());
        }
        return users;
    }

    // ════════════════════════════════════════════════════════════
    // SAVE — inscription
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean save(String fullName, String email, String hashedPassword) {
        String sql = "INSERT INTO `user` " +
                "(email, username, roles_json, password, account_status, " +
                " full_name, created_at, last_login, coin_balance, is_totp_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0, 0)";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            String username = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + (int) (Math.random() * 9000 + 1000);
            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, "[\"ROLE_USER\"]");
            stmt.setString(4, hashedPassword);
            stmt.setString(5, "active");
            stmt.setString(6, fullName);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ save: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // UPDATE — mise à jour complète
    // ════════════════════════════════════════════════════════════

    @Override
    public void update(User user) {
        String sql = "UPDATE `user` SET " +
                "email = ?, username = ?, full_name = ?, bio = ?, " +
                "profile_picture = ?, account_status = ?, coin_balance = ?, last_login = ?, " +
                "totp_secret = ?, is_totp_enabled = ?, backup_codes_json = ?, totp_enabled_at = ?, " +
                "phone = ?, telegram_chat_id = ?, face_descriptor = ?, face_image = ? " +
                "WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            int i = 1;
            stmt.setString(i++, user.getEmail());
            stmt.setString(i++, user.getUsername());
            stmt.setString(i++, user.getFullName());
            stmt.setString(i++, user.getBio());
            stmt.setString(i++, user.getProfilePicture());
            stmt.setString(i++, user.getAccountStatus());
            stmt.setInt(i++, user.getCoinBalance());
            stmt.setTimestamp(i++, user.getLastLogin() != null
                    ? Timestamp.valueOf(user.getLastLogin())
                    : null);
            stmt.setString(i++, user.getTotpSecret());
            stmt.setBoolean(i++, user.isTotpEnabled());
            stmt.setString(i++, user.getBackupCodesJson());
            stmt.setTimestamp(i++, user.getTotpEnabledAt() != null
                    ? Timestamp.valueOf(user.getTotpEnabledAt())
                    : null);
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
    // SAVE PROFILE PICTURE
    // ════════════════════════════════════════════════════════════

    @Override
    public void saveProfilePicture(int userId, byte[] imageBytes, String filename) throws Exception {
        java.nio.file.Path uploadDir = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "uploads", "profiles");
        java.nio.file.Files.createDirectories(uploadDir);
        java.nio.file.Files.write(uploadDir.resolve(filename), imageBytes);

        String sql = "UPDATE `user` SET profile_picture = ? WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, filename);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ saveProfilePicture: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // LOGIN
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
        if (user == null)
            return null;

        String hash = user.getPassword();
        if (hash != null && hash.startsWith("$2y$"))
            hash = "$2a$" + hash.substring(4);
        if (!BCrypt.checkpw(password, hash))
            return null;

        String status = user.getAccountStatus();
        if (status == null || status.isBlank())
            return null;
        switch (status.toLowerCase()) {
            case "active" -> {
            }
            case "banned" -> throw new IllegalStateException("Your account has been banned.");
            case "suspended" -> throw new IllegalStateException("Your account is suspended.");
            default -> throw new IllegalStateException("Account not active: " + status);
        }

        SessionManager.setCurrentUser(user);
        System.out.println("✅ Logged in: " + user.getEmail());
        return user;
    }

    // ════════════════════════════════════════════════════════════
    // REGISTER
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
        if (emailExists(normalizedEmail))
            return false;

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        return save(fullName.trim(), normalizedEmail, hashed);
    }

    // ════════════════════════════════════════════════════════════
    // VERIFY PASSWORD
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean verifyPassword(String email, String plainPassword) {
        String sql = "SELECT password FROM `user` WHERE email = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                return at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
                        .verify(plainPassword.toCharArray(), stored).verified;
            }
        } catch (SQLException e) {
            System.err.println("❌ verifyPassword: " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    // LOGOUT
    // ════════════════════════════════════════════════════════════

    @Override
    public void logout() {
        SessionManager.logout();
    }

    // ════════════════════════════════════════════════════════════
    // RBAC HELPERS
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean hasRole(String role) {
        User user = SessionManager.getCurrentUser();
        if (user == null)
            return false;
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
    // ADMIN — CREATE USER
    // ════════════════════════════════════════════════════════════

    @Override
    public void adminCreateUser(String fullName, String username,
            String email, String plainPassword,
            String role) throws Exception {
        if (findByUsername(username) != null)
            throw new Exception("Username \"" + username + "\" est déjà pris.");
        if (emailExists(email))
            throw new Exception("Email \"" + email + "\" est déjà enregistré.");

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String rolesJson = role.equals("ROLE_USER")
                ? "[\"ROLE_USER\"]"
                : "[\"" + role + "\",\"ROLE_USER\"]";

        String sql = "INSERT INTO `user` " +
                "(email, username, roles_json, password, account_status, " +
                " full_name, created_at, last_login, coin_balance, is_totp_enabled) " +
                "VALUES (?, ?, ?, ?, 'active', ?, NOW(), NOW(), 0, 0)";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, rolesJson);
            stmt.setString(4, hashed);
            stmt.setString(5, fullName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erreur SQL adminCreateUser : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // ADMIN — UPDATE ROLE
    // ════════════════════════════════════════════════════════════

    @Override
    public void updateUserRole(int userId, String newRole) throws Exception {
        String rolesJson = newRole.equals("ROLE_USER")
                ? "[\"ROLE_USER\"]"
                : "[\"" + newRole + "\",\"ROLE_USER\"]";

        String sql = "UPDATE `user` SET roles_json = ? WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rolesJson);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erreur SQL updateUserRole : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // ADMIN — SUSPEND / BAN / ACTIVATE / DELETE
    // ════════════════════════════════════════════════════════════

    @Override
    public void suspendUser(int userId) throws Exception {
        updateStatus(userId, "suspended");
    }

    @Override
    public void banUser(int userId) throws Exception {
        updateStatus(userId, "banned");
    }

    @Override
    public void activateUser(int userId) throws Exception {
        updateStatus(userId, "active");
    }

    private void updateStatus(int userId, String status) throws Exception {
        String sql = "UPDATE `user` SET account_status = ? WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erreur SQL updateStatus : " + e.getMessage());
        }
    }

    @Override
    public void deleteUser(int userId) throws Exception {
        String sql = "DELETE FROM `user` WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Erreur SQL deleteUser : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    // GET TEAM MEMBERSHIPS
    // ════════════════════════════════════════════════════════════

    @Override
    public List<TeamMembership> getTeamMemberships(int userId) {
        List<TeamMembership> list = new ArrayList<>();
        String sql = """
                SELECT tm.id, tm.team_id, tm.user_id, tm.role, tm.status,
                       tm.invited_at, tm.joined_at,
                       t.id AS t_id, t.name AS t_name, t.description AS t_desc,
                       t.logo AS t_logo, t.is_active AS t_active,
                       t.max_members AS t_max, t.owner_id AS t_owner_id
                FROM team_membership tm
                INNER JOIN team t ON t.id = tm.team_id
                WHERE tm.user_id = ?
                ORDER BY tm.joined_at DESC
                """;
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TeamMembership m = new TeamMembership();
                m.setId(rs.getInt("id"));
                m.setTeamId(rs.getInt("team_id"));
                m.setUserId(rs.getInt("user_id"));

                try {
                    m.setRole(MemberRole.fromValue(rs.getString("role")));
                } catch (Exception e) {
                    m.setRole(MemberRole.MEMBER);
                }

                try {
                    m.setStatus(MembershipStatus.fromValue(rs.getString("status")));
                } catch (Exception e) {
                    m.setStatus(MembershipStatus.INACTIVE);
                }

                Timestamp invitedAt = rs.getTimestamp("invited_at");
                Timestamp joinedAt = rs.getTimestamp("joined_at");
                if (invitedAt != null)
                    m.setInvitedAt(invitedAt.toLocalDateTime());
                if (joinedAt != null)
                    m.setJoinedAt(joinedAt.toLocalDateTime());

                Team team = new Team();
                team.setId(rs.getInt("t_id"));
                team.setName(rs.getString("t_name"));
                team.setDescription(rs.getString("t_desc"));
                team.setLogo(rs.getString("t_logo"));
                team.setActive(rs.getBoolean("t_active"));
                team.setMaxMembers(rs.getInt("t_max"));
                team.setOwnerId(rs.getInt("t_owner_id"));
                m.setTeam(team);

                list.add(m);
            }
        } catch (Exception e) {
            System.err.println("[UserServiceImpl] getTeamMemberships: " + e.getMessage());
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════
    // MAPPING ResultSet → User
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
        if (createdAt != null)
            user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null)
            user.setLastLogin(lastLogin.toLocalDateTime());

        user.setTotpSecret(rs.getString("totp_secret"));
        user.setIsTotpEnabled(rs.getBoolean("is_totp_enabled"));
        user.setBackupCodesJson(rs.getString("backup_codes_json"));

        Timestamp totpEnabledAt = rs.getTimestamp("totp_enabled_at");
        if (totpEnabledAt != null)
            user.setTotpEnabledAt(totpEnabledAt.toLocalDateTime());

        user.setPhone(rs.getString("phone"));
        user.setTelegramChatId(rs.getString("telegram_chat_id"));
        user.setFaceDescriptor(rs.getString("face_descriptor"));
        user.setFaceImage(rs.getString("face_image"));
        return user;
    }
}