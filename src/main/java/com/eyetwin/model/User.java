package com.eyetwin.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User.java — Miroir exact de l'entité Symfony User.php
 *
 * Colonnes DB (partagées avec Symfony) :
 *   id, email, username, roles_json, password, account_status,
 *   created_at, last_login, full_name, bio, profile_picture,
 *   coin_balance, totp_secret, is_totp_enabled, backup_codes_json,
 *   totp_enabled_at, phone, telegram_chat_id,
 *   face_descriptor, face_image
 *
 * POINTS CRITIQUES (différences avec ancienne version) :
 *   - fullName          : colonne full_name (pas firstName/lastName séparés)
 *   - coinBalance       : colonne coin_balance (pas "coins")
 *   - backupCodesJson   : TEXT en DB (JSON array), pas une List directe
 *   - isTotpAuthenticationEnabled() : true SEULEMENT si enabled ET secret != null
 *   - totpEnabledAt     : colonne totp_enabled_at, géré automatiquement
 *   - rolesJson         : JSON array ["ROLE_USER","ROLE_ADMIN"] comme Symfony
 */
public class User {

    // ─────────────────────────────────────────────────────────
    //  Champs de base
    // ─────────────────────────────────────────────────────────

    private int           id;
    private String        email;
    private String        username;
    private String        rolesJson      = "[\"ROLE_USER\"]";
    private String        password;
    private String        accountStatus  = "active";
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String        fullName;
    private String        bio;
    private String        profilePicture;

    // ─────────────────────────────────────────────────────────
    //  Coins — colonne coin_balance
    // ─────────────────────────────────────────────────────────

    private int coinBalance = 0;

    // ─────────────────────────────────────────────────────────
    //  2FA — colonnes : totp_secret, is_totp_enabled,
    //                   backup_codes_json, totp_enabled_at
    // ─────────────────────────────────────────────────────────

    private String        totpSecret      = null;
    private boolean       isTotpEnabled   = false;
    private String        backupCodesJson = null;   // JSON stocké tel quel en DB
    private LocalDateTime totpEnabledAt   = null;

    // ─────────────────────────────────────────────────────────
    //  Phone / Telegram / Face
    // ─────────────────────────────────────────────────────────

    private String phone          = null;
    private String telegramChatId = null;
    private String faceDescriptor = null;
    private String faceImage      = null;

    // ─────────────────────────────────────────────────────────
    //  Constructeur
    // ─────────────────────────────────────────────────────────

    public User() {
        this.createdAt     = LocalDateTime.now();
        this.lastLogin     = LocalDateTime.now();
        this.rolesJson     = "[\"ROLE_USER\"]";
        this.isTotpEnabled = false;
        this.coinBalance   = 0;
        this.accountStatus = "active";
    }

    // ═════════════════════════════════════════════════════════
    //  GETTERS / SETTERS — champs de base
    // ═════════════════════════════════════════════════════════

    public int    getId()         { return id; }
    public void   setId(int id)   { this.id = id; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public String getUsername()               { return username; }
    public void   setUsername(String u)       { this.username = u; }

    public String getPassword()               { return password; }
    public void   setPassword(String p)       { this.password = p; }

    public String getAccountStatus()          { return accountStatus; }
    public void   setAccountStatus(String s)  { this.accountStatus = s; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void          setCreatedAt(LocalDateTime d)  { this.createdAt = d; }

    public LocalDateTime getLastLogin()                 { return lastLogin; }
    public void          setLastLogin(LocalDateTime d)  { this.lastLogin = d; }

    public String getFullName()           { return fullName; }
    public void   setFullName(String n)   { this.fullName = n; }

    public String getBio()                { return bio; }
    public void   setBio(String bio)      { this.bio = bio; }

    public String getProfilePicture()                       { return profilePicture; }
    public void   setProfilePicture(String profilePicture)  { this.profilePicture = profilePicture; }

    // ═════════════════════════════════════════════════════════
    //  ROLES — JSON array comme Symfony : ["ROLE_USER","ROLE_ADMIN"]
    // ═════════════════════════════════════════════════════════

    public String getRolesJson()                  { return rolesJson; }
    public void   setRolesJson(String rolesJson)  { this.rolesJson = rolesJson; }

    /**
     * Miroir de getRoles() Symfony.
     * Toujours inclut ROLE_USER (même logique que Symfony).
     */
    public List<String> getRoles() {
        List<String> roles = parseJsonStringArray(rolesJson);
        if (!roles.contains("ROLE_USER")) roles.add("ROLE_USER");
        return roles;
    }

    // ═════════════════════════════════════════════════════════
    //  COINS — colonne coin_balance (max 0, comme Symfony)
    // ═════════════════════════════════════════════════════════

    public int  getCoinBalance()            { return coinBalance; }
    public void setCoinBalance(int balance) { this.coinBalance = Math.max(0, balance); }

    // ═════════════════════════════════════════════════════════
    //  PHONE / TELEGRAM / FACE
    // ═════════════════════════════════════════════════════════

    public String getPhone()                          { return phone; }
    public void   setPhone(String phone)              { this.phone = phone; }

    public String getTelegramChatId()                       { return telegramChatId; }
    public void   setTelegramChatId(String telegramChatId)  { this.telegramChatId = telegramChatId; }

    public String getFaceDescriptor()                       { return faceDescriptor; }
    public void   setFaceDescriptor(String faceDescriptor)  { this.faceDescriptor = faceDescriptor; }

    public String getFaceImage()                  { return faceImage; }
    public void   setFaceImage(String faceImage)  { this.faceImage = faceImage; }

    // ═════════════════════════════════════════════════════════
    //  2FA — miroir exact Symfony
    // ═════════════════════════════════════════════════════════

    /**
     * Miroir EXACT de isTotpAuthenticationEnabled() Symfony.
     * ⚠️ DEUX conditions : isTotpEnabled == true ET totpSecret != null.
     * Si le secret est null même si enabled=true → retourne false.
     */
    public boolean isTotpAuthenticationEnabled() {
        return isTotpEnabled && totpSecret != null;
    }

    public boolean isTotpEnabled()  { return isTotpEnabled; }

    /**
     * Miroir de setIsTotpEnabled() Symfony :
     *   - Active  → enregistre totpEnabledAt (si pas encore défini)
     *   - Désactive → remet totpEnabledAt à null
     */
    public void setIsTotpEnabled(boolean b) {
        this.isTotpEnabled = b;
        if (b && this.totpEnabledAt == null) {
            this.totpEnabledAt = LocalDateTime.now();
        } else if (!b) {
            this.totpEnabledAt = null;
        }
    }

    /** Alias pour rétrocompatibilité */
    public void setTotpEnabled(boolean b) { setIsTotpEnabled(b); }

    public String getTotpSecret()                 { return totpSecret; }
    public void   setTotpSecret(String secret)    { this.totpSecret = secret; }

    public LocalDateTime getTotpEnabledAt()                 { return totpEnabledAt; }
    public void          setTotpEnabledAt(LocalDateTime d)  { this.totpEnabledAt = d; }

    // ─────────────────────────────────────────────────────────
    //  Backup codes
    //  DB : colonne backup_codes_json (TEXT, JSON array)
    //  Symfony stocke directement le JSON — on fait pareil.
    // ─────────────────────────────────────────────────────────

    /** JSON brut — utilisé par le DAO pour lire/écrire en DB */
    public String getBackupCodesJson()              { return backupCodesJson; }
    public void   setBackupCodesJson(String json)   { this.backupCodesJson = json; }

    /**
     * Miroir de getBackupCodes() Symfony.
     * Retourne null si aucun code stocké.
     */
    public List<String> getBackupCodes() {
        if (backupCodesJson == null || backupCodesJson.isBlank()) return null;
        List<String> codes = parseJsonStringArray(backupCodesJson);
        return codes.isEmpty() ? null : codes;
    }

    /**
     * Miroir de setBackupCodes() Symfony.
     * Sérialise la liste en JSON et la stocke.
     */
    public void setBackupCodes(List<String> codes) {
        this.backupCodesJson = (codes != null) ? serializeToJson(codes) : null;
    }

    /**
     * Miroir de invalidateBackupCode() Symfony.
     * Supprime le code après usage, retourne true si trouvé.
     */
    public boolean invalidateBackupCode(String code) {
        List<String> codes = getBackupCodes();
        if (codes == null) return false;
        boolean removed = codes.remove(code);
        if (removed) setBackupCodes(codes);
        return removed;
    }

    /**
     * Miroir de getRemainingBackupCodesCount() Symfony.
     */
    public int getRemainingBackupCodesCount() {
        List<String> codes = getBackupCodes();
        return codes != null ? codes.size() : 0;
    }

    // ═════════════════════════════════════════════════════════
    //  HELPERS DE RÔLE
    // ═════════════════════════════════════════════════════════

    public boolean isAdmin() {
        return rolesJson != null && rolesJson.contains("ROLE_ADMIN");
    }

    public boolean isSuperAdmin() {
        return rolesJson != null && rolesJson.contains("ROLE_SUPER_ADMIN");
    }

    public boolean isCoach() {
        return rolesJson != null && rolesJson.contains("ROLE_COACH");
    }

    /** Miroir de isActive() Symfony */
    public boolean isActive() {
        return "active".equalsIgnoreCase(accountStatus);
    }

    // ═════════════════════════════════════════════════════════
    //  JSON HELPERS — sans dépendance externe
    //  (Symfony utilise json_encode/json_decode PHP natif)
    // ═════════════════════════════════════════════════════════

    /**
     * Parse un JSON array de strings : ["a","b","c"] → List<String>
     * Compatible avec le format exact produit par PHP json_encode()
     */
    private List<String> parseJsonStringArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        String cleaned = json.trim();
        // Enlève les crochets [ ]
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]"))   cleaned = cleaned.substring(0, cleaned.length() - 1);
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return result;
        // Split sur les virgules (en dehors des guillemets)
        for (String item : cleaned.split(",")) {
            String val = item.trim();
            // Enlève les guillemets autour de la valeur
            if (val.startsWith("\"")) val = val.substring(1);
            if (val.endsWith("\""))   val = val.substring(0, val.length() - 1);
            if (!val.isEmpty()) result.add(val);
        }
        return result;
    }

    /**
     * Sérialise une List<String> en JSON array : ["a","b","c"]
     * Format identique à PHP json_encode()
     */
    private String serializeToJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"")
                    .append(list.get(i).replace("\\", "\\\\").replace("\"", "\\\""))
                    .append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", username=" + username
                + ", 2fa=" + isTotpAuthenticationEnabled() + "}";
    }
}