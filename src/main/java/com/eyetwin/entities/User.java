package com.eyetwin.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User.java — Miroir exact de l'entité Symfony User.php
 */
public class User {

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
    private int           coinBalance = 0;
    private String        totpSecret      = null;
    private boolean       isTotpEnabled   = false;
    private String        backupCodesJson = null;
    private LocalDateTime totpEnabledAt   = null;
    private String        phone          = null;
    private String        telegramChatId = null;
    private String        faceDescriptor = null;
    private String        faceImage      = null;

    public User() {
        this.createdAt     = LocalDateTime.now();
        this.lastLogin     = LocalDateTime.now();
        this.rolesJson     = "[\"ROLE_USER\"]";
        this.isTotpEnabled = false;
        this.coinBalance   = 0;
        this.accountStatus = "active";
    }

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

    public String getRolesJson()                  { return rolesJson; }
    public void   setRolesJson(String rolesJson)  { this.rolesJson = rolesJson; }

    public List<String> getRoles() {
        List<String> roles = parseJsonStringArray(rolesJson);
        if (!roles.contains("ROLE_USER")) roles.add("ROLE_USER");
        return roles;
    }

    public int  getCoinBalance()            { return coinBalance; }
    public void setCoinBalance(int balance) { this.coinBalance = Math.max(0, balance); }

    public String getPhone()                          { return phone; }
    public void   setPhone(String phone)              { this.phone = phone; }

    public String getTelegramChatId()                       { return telegramChatId; }
    public void   setTelegramChatId(String telegramChatId)  { this.telegramChatId = telegramChatId; }

    public String getFaceDescriptor()                       { return faceDescriptor; }
    public void   setFaceDescriptor(String faceDescriptor)  { this.faceDescriptor = faceDescriptor; }

    public String getFaceImage()                  { return faceImage; }
    public void   setFaceImage(String faceImage)  { this.faceImage = faceImage; }

    public boolean isTotpAuthenticationEnabled() {
        return isTotpEnabled && totpSecret != null;
    }

    public boolean isTotpEnabled()  { return isTotpEnabled; }

    public void setIsTotpEnabled(boolean b) {
        this.isTotpEnabled = b;
        if (b && this.totpEnabledAt == null) {
            this.totpEnabledAt = LocalDateTime.now();
        } else if (!b) {
            this.totpEnabledAt = null;
        }
    }

    public void setTotpEnabled(boolean b) { setIsTotpEnabled(b); }

    public String getTotpSecret()                 { return totpSecret; }
    public void   setTotpSecret(String secret)    { this.totpSecret = secret; }

    public LocalDateTime getTotpEnabledAt()                 { return totpEnabledAt; }
    public void          setTotpEnabledAt(LocalDateTime d)  { this.totpEnabledAt = d; }

    public String getBackupCodesJson()              { return backupCodesJson; }
    public void   setBackupCodesJson(String json)   { this.backupCodesJson = json; }

    public List<String> getBackupCodes() {
        if (backupCodesJson == null || backupCodesJson.isBlank()) return new ArrayList<>();
        return parseJsonStringArray(backupCodesJson);
    }

    public void setBackupCodes(List<String> codes) {
        this.backupCodesJson = (codes != null) ? serializeToJson(codes) : null;
    }

    public boolean isAdmin() {
        return rolesJson != null && (rolesJson.contains("ROLE_ADMIN") || rolesJson.contains("ROLE_SUPER_ADMIN"));
    }

    public boolean isCoach() {
        return rolesJson != null && rolesJson.contains("ROLE_COACH");
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(accountStatus);
    }

    private List<String> parseJsonStringArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        String cleaned = json.trim();
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]"))   cleaned = cleaned.substring(0, cleaned.length() - 1);
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) return result;
        for (String item : cleaned.split(",")) {
            String val = item.trim();
            if (val.startsWith("\"")) val = val.substring(1);
            if (val.endsWith("\""))   val = val.substring(0, val.length() - 1);
            if (!val.isEmpty()) result.add(val);
        }
        return result;
    }

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
}