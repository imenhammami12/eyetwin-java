package com.eyetwin.interfaces;

import com.eyetwin.entities.TeamMembership;
import com.eyetwin.entities.User;
import java.util.List;

/**
 * IUserService — contrat de la couche utilisateur.
 * Regroupe toutes les opérations de UserDAO + AuthService + Admin.
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

    // ════════════════════════════════════════════════════════════
    //  ADMIN — méthodes de gestion des utilisateurs
    // ════════════════════════════════════════════════════════════

    /** Tous les utilisateurs triés par date de création DESC */
    List<User> getAllUsers();

    /** Cherche un utilisateur par son username exact (null si absent) */
    User findByUsername(String username);

    /**
     * Crée un utilisateur depuis le panneau admin.
     * Le mot de passe plain-text est hashé en interne.
     * @throws Exception si l'email ou le username existe déjà
     */
    void adminCreateUser(String fullName, String username,
                         String email, String plainPassword,
                         String role) throws Exception;

    /** Change le rôle d'un utilisateur (ex: "ROLE_COACH") */
    void updateUserRole(int userId, String newRole) throws Exception;

    /** Passe accountStatus → suspended */
    void suspendUser(int userId) throws Exception;

    /** Passe accountStatus → banned */
    void banUser(int userId) throws Exception;

    /** Passe accountStatus → active */
    void activateUser(int userId) throws Exception;

    /** Suppression définitive de l'utilisateur */
    void deleteUser(int userId) throws Exception;

    /**
     * Retourne la liste des TeamMembership d'un utilisateur.
     * Utilisé par AdminUserController pour la vue détail.
     */
    List<TeamMembership> getTeamMemberships(int userId);
}