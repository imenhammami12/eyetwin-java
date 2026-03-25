package com.eyetwin.model;

public class User {

    private int id;
    private String email;
    private String password;
    private String username;
    private String firstName;
    private String lastName;
    private String rolesJson;
    private String accountStatus;
    private String profilePicture;
    private int coins;
    private boolean totpEnabled;
    private String totpSecret;

    public User() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRolesJson() { return rolesJson; }
    public void setRolesJson(String rolesJson) { this.rolesJson = rolesJson; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public boolean isAdmin() {
        return rolesJson != null && rolesJson.contains("ROLE_ADMIN");
    }

    public boolean isSuperAdmin() {
        return rolesJson != null && rolesJson.contains("ROLE_SUPER_ADMIN");
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", username=" + username + "}";
    }
}