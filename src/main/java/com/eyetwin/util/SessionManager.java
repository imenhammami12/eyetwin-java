package com.eyetwin.util;

import com.eyetwin.model.User;
import com.eyetwin.dao.UserDAO;

import java.time.LocalDateTime;
import java.util.prefs.Preferences;

/**
 * SessionManager — Gestion de session avec support 2FA + Trusted Device.
 *
 * Flux 2FA :
 *   1. LoginController → password OK + 2FA activée
 *      → isTrustedDevice(userId) ? login direct : setPending2FAUser(user)
 *   2. TwoFactorVerifyController → code OK
 *      → completeTwoFactorLogin(user, trustDevice) → home.fxml
 */
public class SessionManager {

    // ─────────────────────────────────────────────────────────
    //  État de session
    // ─────────────────────────────────────────────────────────
    private static User    currentUser        = null;
    private static User    pending2FAUser     = null;
    private static boolean twoFactorCompleted = false;

    // Nœud Preferences pour les appareils de confiance
    private static final String PREFS_NODE = "eyetwin/trusted";

    // ─────────────────────────────────────────────────────────
    //  Session de base
    // ─────────────────────────────────────────────────────────
    public static void    setCurrentUser(User user) { currentUser = user; }
    public static User    getCurrentUser()          { return currentUser; }
    public static boolean isLoggedIn()              { return currentUser != null; }

    public static boolean isSuperAdmin() {
        return currentUser != null
                && currentUser.getRolesJson() != null
                && currentUser.getRolesJson().contains("ROLE_SUPER_ADMIN");
    }

    public static boolean isAdmin() {
        return currentUser != null
                && currentUser.getRolesJson() != null
                && (currentUser.getRolesJson().contains("ROLE_ADMIN")
                ||  currentUser.getRolesJson().contains("ROLE_SUPER_ADMIN"));
    }

    public static boolean isCoach() {
        return currentUser != null
                && currentUser.getRolesJson() != null
                && currentUser.getRolesJson().contains("ROLE_COACH");
    }

    public static boolean isUser()  { return currentUser != null; }
    public static boolean isGuest() { return currentUser == null; }

    public static String getHighestRole() {
        if (isSuperAdmin()) return "ROLE_SUPER_ADMIN";
        if (isAdmin())      return "ROLE_ADMIN";
        if (isCoach())      return "ROLE_COACH";
        if (isUser())       return "ROLE_USER";
        return "GUEST";
    }

    // ─────────────────────────────────────────────────────────
    //  2FA — Étape 1 : Mettre l'utilisateur en attente
    // ─────────────────────────────────────────────────────────
    public static void setPending2FAUser(User user) {
        pending2FAUser     = user;
        twoFactorCompleted = false;
        System.out.println("[SessionManager] 2FA requise pour : " + user.getEmail());
    }

    public static User getPending2FAUser() {
        return pending2FAUser;
    }

    // ─────────────────────────────────────────────────────────
    //  2FA — Étape 2 : Compléter la connexion
    // ─────────────────────────────────────────────────────────
    public static void completeTwoFactorLogin(User user, boolean trustDevice) {
        pending2FAUser     = null;
        twoFactorCompleted = true;
        setCurrentUser(user);

        if (trustDevice) {
            saveTrustedDevice(user.getId());
            System.out.println("[SessionManager] ✅ Appareil de confiance enregistré (30 jours) : "
                    + user.getEmail());
        }

        System.out.println("[SessionManager] ✅ 2FA complétée — connecté : " + user.getEmail());
    }

    public static boolean isTwoFactorCompleted() {
        return twoFactorCompleted;
    }

    // ─────────────────────────────────────────────────────────
    //  TRUSTED DEVICE — Persistance via java.util.prefs
    // ─────────────────────────────────────────────────────────

    /**
     * Vérifie si l'appareil est de confiance pour cet utilisateur.
     * Appelé dans LoginController AVANT d'afficher la page 2FA.
     *
     * @param userId ID de l'utilisateur
     * @return true si l'appareil est enregistré et la date non expirée
     */
    public static boolean isTrustedDevice(int userId) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE + "/" + userId);
            String storedDate = prefs.get("trusted_until", null);
            if (storedDate == null || storedDate.isEmpty()) {
                System.out.println("[SessionManager] Aucun appareil de confiance pour userId=" + userId);
                return false;
            }
            LocalDateTime until = LocalDateTime.parse(storedDate);
            boolean trusted = LocalDateTime.now().isBefore(until);
            System.out.println("[SessionManager] Trusted device check — userId=" + userId
                    + " | until=" + storedDate + " | valid=" + trusted);
            return trusted;
        } catch (Exception e) {
            System.err.println("[SessionManager] Erreur isTrustedDevice : " + e.getMessage());
            return false;
        }
    }

    /**
     * Enregistre l'appareil comme de confiance pour 30 jours.
     *
     * @param userId ID de l'utilisateur
     */
    private static void saveTrustedDevice(int userId) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE + "/" + userId);
            String expiry = LocalDateTime.now().plusDays(30).toString();
            prefs.put("trusted_until", expiry);
            prefs.flush();
            System.out.println("[SessionManager] Trusted device saved — userId=" + userId
                    + " | expires=" + expiry);
        } catch (Exception e) {
            System.err.println("[SessionManager] Erreur saveTrustedDevice : " + e.getMessage());
        }
    }

    /**
     * Révoque la confiance de l'appareil pour un utilisateur.
     * Appelé lors du disable 2FA ou logout sécurisé.
     *
     * @param userId ID de l'utilisateur
     */
    public static void revokeTrustedDevice(int userId) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE + "/" + userId);
            prefs.remove("trusted_until");
            prefs.flush();
            System.out.println("[SessionManager] Trusted device révoqué — userId=" + userId);
        } catch (Exception e) {
            System.err.println("[SessionManager] Erreur revokeTrustedDevice : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Refresh — recharger depuis la DB
    // ─────────────────────────────────────────────────────────
    public static void refresh() {
        User current = getCurrentUser();
        if (current != null) {
            try {
                User refreshed = new UserDAO().findById(current.getId());
                if (refreshed != null) {
                    setCurrentUser(refreshed);
                    System.out.println("[SessionManager] 🔄 Utilisateur rechargé : " + refreshed.getEmail());
                }
            } catch (Exception e) {
                System.err.println("[SessionManager] Erreur refresh : " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Déconnexion
    // ─────────────────────────────────────────────────────────
    public static void logout() {
        System.out.println("👋 Déconnexion : "
                + (currentUser != null ? currentUser.getEmail() : "?"));
        currentUser        = null;
        pending2FAUser     = null;
        twoFactorCompleted = false;
        // NE PAS supprimer le trusted device ici —
        // il doit persister entre les sessions (comme un cookie "remember_me")
    }
}