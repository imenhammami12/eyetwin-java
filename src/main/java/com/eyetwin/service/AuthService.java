package com.eyetwin.service;

import com.eyetwin.dao.UserDAO;
import com.eyetwin.model.User;
import com.eyetwin.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ===== LOGIN =====
    public User login(String email, String password) {

        // --- Validation serveur ---
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        User user = userDAO.findByEmail(email.toLowerCase().trim());

        if (user == null) {
            System.out.println("❌ User not found: " + email);
            return null;
        }

        // Fix compatibilité Symfony : $2y$ → $2a$ (identiques algorithmiquement)
        String hash = user.getPassword();
        if (hash != null && hash.startsWith("$2y$")) {
            hash = "$2a$" + hash.substring(4);
        }

        if (!BCrypt.checkpw(password, hash)) {
            System.out.println("❌ Wrong password");
            return null;
        }

        // --- Account status check (RBAC) ---
        String status = user.getAccountStatus();
        if (status == null || status.isBlank()) {
            System.out.println("❌ No account status");
            return null;
        }
        switch (status.toUpperCase()) {
            case "ACTIVE"    -> {} // OK
            case "BANNED"    -> throw new IllegalStateException(
                    "Your account has been banned. Contact support.");
            case "SUSPENDED" -> throw new IllegalStateException(
                    "Your account is suspended. Contact support.");
            default          -> throw new IllegalStateException(
                    "Account not active: " + status);
        }

        SessionManager.setCurrentUser(user);
        System.out.println("✅ Logged in: " + user.getEmail()
                + " | Roles: " + user.getRolesJson());
        return user;
    }

    // ===== REGISTER =====
    public boolean register(String fullName, String email, String password) {

        // --- Validation serveur ---
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (fullName.trim().length() < 2) {
            throw new IllegalArgumentException(
                    "Full name must be at least 2 characters.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must be at least 6 characters.");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException(
                    "Password must contain uppercase, lowercase and a number.");
        }

        String normalizedEmail = email.toLowerCase().trim();

        if (userDAO.emailExists(normalizedEmail)) {
            System.out.println("❌ Email already registered: " + normalizedEmail);
            return false;
        }

        String hashed  = BCrypt.hashpw(password, BCrypt.gensalt());
        boolean success = userDAO.save(fullName.trim(), normalizedEmail, hashed);

        if (success) System.out.println("✅ Account created: " + normalizedEmail);
        return success;
    }

    // ===== RBAC HELPERS =====
    public boolean hasRole(String role) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return false;
        return user.getRolesJson() != null
                && user.getRolesJson().contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ROLE_SUPER_ADMIN");
    }

    public boolean isCoach() {
        return hasRole("ROLE_COACH");
    }

    public boolean isUser() {
        return SessionManager.getCurrentUser() != null;
    }

    public void logout() {
        SessionManager.logout();
    }
}