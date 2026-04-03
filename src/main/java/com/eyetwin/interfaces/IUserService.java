package com.eyetwin.interfaces;

import com.eyetwin.entities.User;

/**
 * IUserService — contrat de la couche utilisateur.
 * Regroupe toutes les opérations de UserDAO + AuthService.
 */
public interface IUserService {

    // ── Recherche ──────────────────────────────────────────────
    User findByEmail(String email);
    User findById(int id);
    boolean emailExists(String email);

    // ── Persistence ────────────────────────────────────────────
    boolean save(String fullName, String email, String hashedPassword);
    void update(User user);
    void saveProfilePicture(int userId, byte[] imageBytes, String filename) throws Exception;

    // ── Auth ───────────────────────────────────────────────────
    /** Retourne l'utilisateur connecté ou null si identifiants invalides */
    User login(String email, String password);

    /** Crée un compte, retourne false si l'email existe déjà */
    boolean register(String fullName, String email, String password);

    boolean verifyPassword(String email, String plainPassword);

    void logout();

    // ── RBAC helpers ───────────────────────────────────────────
    boolean hasRole(String role);
    boolean isAdmin();
    boolean isCoach();
    boolean isLoggedIn();
}