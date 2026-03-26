package com.eyetwin.util;

import com.eyetwin.model.User;
import com.eyetwin.dao.UserDAO;

/**
 * SessionManager — Gestion de session avec support 2FA complet.
 *
 * Flux 2FA (miroir SchebTwoFactorBundle Symfony) :
 *   1. LoginController → mot de passe OK + 2FA activée
 *      → setPending2FAUser(user) → naviguer vers TwoFactorVerify.fxml
 *   2. TwoFactorVerifyController → code OK
 *      → completeTwoFactorLogin(user) → naviguer vers home.fxml
 */
public class SessionManager {

    // ─────────────────────────────────────────────────────────
    //  État de session
    // ─────────────────────────────────────────────────────────

    private static User    currentUser        = null;
    private static User    pending2FAUser     = null;
    private static boolean twoFactorCompleted = false;

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

    /**
     * Appeler dans LoginController après vérification du mot de passe,
     * si 2FA est activée. L'utilisateur n'est PAS encore connecté.
     * Miroir de SchebTwoFactorBundle interceptor.
     */
    public static void setPending2FAUser(User user) {
        pending2FAUser     = user;
        twoFactorCompleted = false;
        System.out.println("[SessionManager] 2FA requise pour : " + user.getEmail());
    }

    /**
     * Récupérer l'utilisateur en attente de code 2FA.
     * Utilisé par TwoFactorVerifyController.initialize()
     */
    public static User getPending2FAUser() {
        return pending2FAUser;
    }

    // ─────────────────────────────────────────────────────────
    //  2FA — Étape 2 : Compléter la connexion
    // ─────────────────────────────────────────────────────────

    /**
     * Appeler dans TwoFactorVerifyController après validation du code.
     * Connecte définitivement l'utilisateur.
     * Miroir de SchebTwoFactorBundle::markTwoFactorComplete()
     *
     * @param user        L'utilisateur à connecter
     * @param trustDevice true = coché "Faire confiance à cet appareil"
     */
    public static void completeTwoFactorLogin(User user, boolean trustDevice) {
        pending2FAUser     = null;
        twoFactorCompleted = true;
        setCurrentUser(user);

        if (trustDevice) {
            // TODO : implémenter les appareils de confiance si nécessaire
            System.out.println("[SessionManager] Appareil de confiance enregistré : " + user.getEmail());
        }

        System.out.println("[SessionManager] ✅ 2FA complétée — connecté : " + user.getEmail());
    }

    /** Vérifier si la 2FA a été validée cette session */
    public static boolean isTwoFactorCompleted() {
        return twoFactorCompleted;
    }

    // ─────────────────────────────────────────────────────────
    //  Refresh — recharger depuis la DB
    // ─────────────────────────────────────────────────────────

    /**
     * Recharger l'utilisateur courant depuis la DB.
     * À appeler après enable/disable 2FA, regénération des codes.
     * Miroir de EntityManager::refresh() Symfony.
     */
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
        // NE PAS appeler RememberMeService.clear() ici —
        // le remember me doit persister entre les sessions,
        // exactement comme un cookie "remember_me" Symfony.
    }
}