package com.eyetwin.tools;

import com.eyetwin.entities.Complaint;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.Team;
import com.eyetwin.entities.TrainingSession;
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

    private static final IUserService userService = new UserServiceImpl();

    // ─────────────────────────────────────────────────────────
    //  État de session
    // ─────────────────────────────────────────────────────────
    private static User    currentUser        = null;
    private static User    pending2FAUser     = null;
    private static boolean twoFactorCompleted = false;
    private static String  pendingFaceEmail   = null;
    private static boolean openSecurityTab    = false;

    // ─────────────────────────────────────────────────────────
    //  Flash messages
    // ─────────────────────────────────────────────────────────
    private static String pendingFlashType    = null;
    private static String pendingFlashMessage = null;

    // Nœud Preferences pour les appareils de confiance
    private static final String PREFS_NODE = "eyetwin/trusted";

    // ─────────────────────────────────────────────────────────
    //  Entités sélectionnées (toutes les branches)
    // ─────────────────────────────────────────────────────────
    private static User            selectedUser            = null;
    private static Team            selectedTeam            = null;
    private static Complaint       selectedComplaint       = null;
    private static Planning        selectedPlanning        = null;
    private static TrainingSession selectedTrainingSession = null;

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
    //  Security Tab
    // ─────────────────────────────────────────────────────────
    public static void setOpenSecurityTab(boolean value) {
        openSecurityTab = value;
    }

    public static boolean consumeOpenSecurityTab() {
        boolean val = openSecurityTab;
        openSecurityTab = false;
        return val;
    }

    // ─────────────────────────────────────────────────────────
    //  Flash Messages
    // ─────────────────────────────────────────────────────────
    public static void setPendingFlash(String type, String message) {
        pendingFlashType    = type;
        pendingFlashMessage = message;
        System.out.println("[SessionManager] Flash set — [" + type + "] " + message);
    }

    public static String[] consumeFlash() {
        if (pendingFlashType == null) return null;
        String[] result = { pendingFlashType, pendingFlashMessage };
        pendingFlashType    = null;
        pendingFlashMessage = null;
        System.out.println("[SessionManager] Flash consumed — [" + result[0] + "] " + result[1]);
        return result;
    }

    public static boolean hasPendingFlash() {
        return pendingFlashType != null;
    }

    public static void clearFlash() {
        pendingFlashType    = null;
        pendingFlashMessage = null;
    }

    // ─────────────────────────────────────────────────────────
    //  Face Login
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
    //  2FA — Étape 1
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
    //  2FA — Étape 2
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
    //  Trusted Device
    // ─────────────────────────────────────────────────────────
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
    //  Refresh
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

    // ─────────────────────────────────────────────────────────
    //  Selected Entities
    // ─────────────────────────────────────────────────────────
    public static void setSelectedUser(User user)  { selectedUser = user; }
    public static User getSelectedUser()           { return selectedUser; }
    public static void clearSelectedUser()         { selectedUser = null; }

    public static void setSelectedTeam(Team team)  { selectedTeam = team; }
    public static Team getSelectedTeam()           { return selectedTeam; }
    public static void clearSelectedTeam()         { selectedTeam = null; }

    public static void    setSelectedComplaint(Complaint c) { selectedComplaint = c; }
    public static Complaint getSelectedComplaint()          { return selectedComplaint; }
    public static void    clearSelectedComplaint()          { selectedComplaint = null; }

    public static void setSelectedPlanning(Planning planning) { selectedPlanning = planning; }
    public static Planning getSelectedPlanning()              { return selectedPlanning; }
    public static void clearSelectedPlanning()                { selectedPlanning = null; }

    public static void setSelectedTrainingSession(TrainingSession session) { selectedTrainingSession = session; }
    public static TrainingSession getSelectedTrainingSession()             { return selectedTrainingSession; }
    public static void clearSelectedTrainingSession()                      { selectedTrainingSession = null; }

    // ─────────────────────────────────────────────────────────
    //  Déconnexion
    // ─────────────────────────────────────────────────────────
    public static void logout() {
        System.out.println("👋 Déconnexion : "
                + (currentUser != null ? currentUser.getEmail() : "?"));
        currentUser            = null;
        pending2FAUser         = null;
        twoFactorCompleted     = false;
        pendingFlashType       = null;
        pendingFlashMessage    = null;
        pendingFaceEmail       = null;
        openSecurityTab        = false;
        selectedUser           = null;
        selectedTeam           = null;
        selectedComplaint      = null;
        selectedPlanning       = null;
        selectedTrainingSession = null;
        // NE PAS supprimer le trusted device ici —
        // il doit persister entre les sessions (comme un cookie "remember_me")
    }
}