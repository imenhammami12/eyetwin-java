package com.eyetwin.tools;

import com.eyetwin.entities.Complaint;
import com.eyetwin.entities.Team;
import com.eyetwin.entities.User;

import java.time.LocalDateTime;
import java.util.prefs.Preferences;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;

/**
 * SessionManager — Gestion de session avec support 2FA + Trusted Device + Flash Messages + Face Login.
 *
 * Flux 2FA :
 *   1. LoginController → password OK + 2FA activée
 *      → isTrustedDevice(userId) ? login direct : setPending2FAUser(user)
 *   2. TwoFactorVerifyController → code OK
 *      → completeTwoFactorLogin(user, trustDevice) → home.fxml
 *
 * Flash Messages :
 *   - Avant navigateTo() : SessionManager.setPendingFlash("success", "Message");
 *   - Dans initialize() de la vue cible :
 *       String[] flash = SessionManager.consumeFlash();
 *       if (flash != null) showFlashBanner(flash[0], flash[1]);
 *
 * Face Login (Admin) :
 *   - AdminLoginController → setPendingFaceEmail(email) → navigateTo("AdminFaceVerify.fxml")
 *   - AdminFaceVerifyController → getPendingFaceEmail() → vérifie la face → clearPendingFaceEmail()
 */
public class SessionManager {
    private static boolean openSecurityTab = false;

    private static final IUserService userService = new UserServiceImpl();
    private static User selectedUser = null;
    private static Team selectedTeam = null;
    private static Complaint selectedComplaint = null;

    // ─────────────────────────────────────────────────────────
    //  État de session
    // ─────────────────────────────────────────────────────────
    private static User    currentUser        = null;
    private static User    pending2FAUser     = null;
    private static boolean twoFactorCompleted = false;
    private static String  pendingFaceEmail   = null; // ← PATCH : Face Login Admin

    // ─────────────────────────────────────────────────────────
    //  Flash messages (miroir de addFlash() Symfony)
    // ─────────────────────────────────────────────────────────
    private static String pendingFlashType    = null;
    private static String pendingFlashMessage = null;

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

    public static void setOpenSecurityTab(boolean value) {
        openSecurityTab = value;
    }

    public static boolean consumeOpenSecurityTab() {
        boolean val = openSecurityTab;
        openSecurityTab = false;
        return val;
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
    //  Flash Messages
    //  Miroir de $this->addFlash() / $this->getFlashes() Symfony
    // ─────────────────────────────────────────────────────────

    /**
     * Stocke un message flash à afficher sur la prochaine vue.
     * Appelé AVANT navigateTo() — miroir de addFlash() Symfony.
     *
     * Types standards : "success" | "info" | "warning" | "error"
     *
     * Exemple :
     *   SessionManager.setPendingFlash("success", "Application submitted!");
     *   navigateTo("UserProfile.fxml");
     */
    public static void setPendingFlash(String type, String message) {
        pendingFlashType    = type;
        pendingFlashMessage = message;
        System.out.println("[SessionManager] Flash set — [" + type + "] " + message);
    }

    /**
     * Consomme le flash (lecture unique + effacement automatique).
     * Appelez dans initialize() de la vue cible.
     *
     * Exemple dans UserProfileController#initialize() :
     *
     *   String[] flash = SessionManager.consumeFlash();
     *   if (flash != null) {
     *       // flash[0] = type  ("success" | "info" | "warning" | "error")
     *       // flash[1] = message
     *       showFlashBanner(flash[0], flash[1]);
     *   }
     *
     * @return String[2] { type, message } ou null si aucun flash en attente
     */
    public static String[] consumeFlash() {
        if (pendingFlashType == null) return null;
        String[] result = { pendingFlashType, pendingFlashMessage };
        pendingFlashType    = null;
        pendingFlashMessage = null;
        System.out.println("[SessionManager] Flash consumed — [" + result[0] + "] " + result[1]);
        return result;
    }

    /**
     * Vérifie si un flash est en attente sans le consommer.
     */
    public static boolean hasPendingFlash() {
        return pendingFlashType != null;
    }

    /**
     * Efface le flash en attente sans le lire.
     */
    public static void clearFlash() {
        pendingFlashType    = null;
        pendingFlashMessage = null;
    }

    // ─────────────────────────────────────────────────────────
    //  FACE LOGIN — email en attente de vérification faciale
    //  Miroir de la redirection Symfony vers /admin/face-verify
    //
    //  Usage :
    //    1. AdminLoginController détecte que l'user a une face
    //       → SessionManager.setPendingFaceEmail(email);
    //       → navigateTo("AdminFaceVerify.fxml");
    //    2. AdminFaceVerifyController récupère l'email
    //       → String email = SessionManager.getPendingFaceEmail();
    //       → [vérifie la face]
    //       → SessionManager.clearPendingFaceEmail();
    // ─────────────────────────────────────────────────────────

    public static void setPendingFaceEmail(String email) {
        pendingFaceEmail = email;
        System.out.println("[SessionManager] Face email set — " + email);
    }

    public static String getPendingFaceEmail() {
        return pendingFaceEmail;
    }

    public static void clearPendingFaceEmail() {
        System.out.println("[SessionManager] Face email cleared — " + pendingFaceEmail);
        pendingFaceEmail = null;
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
     */
    public static boolean isTrustedDevice(int userId) {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE + "/" + userId);
            String storedDate = prefs.get("trusted_until", null);
            if (storedDate == null || storedDate.isEmpty()) {
                System.out.println("[SessionManager] Aucun appareil de confiance pour userId=" + userId);
                return false;
            }
            LocalDateTime until   = LocalDateTime.parse(storedDate);
            boolean       trusted = LocalDateTime.now().isBefore(until);
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
     */
    private static void saveTrustedDevice(int userId) {
        try {
            Preferences prefs  = Preferences.userRoot().node(PREFS_NODE + "/" + userId);
            String      expiry = LocalDateTime.now().plusDays(30).toString();
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
                User refreshed = userService.findById(current.getId());
                if (refreshed != null) {
                    setCurrentUser(refreshed);
                    System.out.println("[SessionManager] 🔄 Utilisateur rechargé : " + refreshed.getEmail());
                }
            } catch (Exception e) {
                System.err.println("[SessionManager] Erreur refresh : " + e.getMessage());
            }
        }
    }

    public static void setSelectedUser(User user) {
        selectedUser = user;
    }

    public static User getSelectedUser() {
        return selectedUser;
    }

    public static void clearSelectedUser() {
        selectedUser = null;
    }


    public static void setSelectedTeam(Team team)  { selectedTeam = team; }
    public static Team getSelectedTeam()           { return selectedTeam; }
    public static void clearSelectedTeam()         { selectedTeam = null; }




    public static void    setSelectedComplaint(Complaint c) { selectedComplaint = c; }
    public static Complaint getSelectedComplaint()          { return selectedComplaint; }
    public static void    clearSelectedComplaint()          { selectedComplaint = null; }

    // ─────────────────────────────────────────────────────────
    //  Déconnexion
    // ─────────────────────────────────────────────────────────
    public static void logout() {
        System.out.println("👋 Déconnexion : "
                + (currentUser != null ? currentUser.getEmail() : "?"));
        currentUser         = null;
        pending2FAUser      = null;
        twoFactorCompleted  = false;
        pendingFlashType    = null;
        pendingFlashMessage = null;
        selectedUser = null;
        selectedTeam = null;
        selectedComplaint = null;

        openSecurityTab = false;
        pendingFaceEmail    = null; // ← PATCH : on efface aussi le face email
        // NE PAS supprimer le trusted device ici —
        // il doit persister entre les sessions (comme un cookie "remember_me")
    }
}