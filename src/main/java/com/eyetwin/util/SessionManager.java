package com.eyetwin.util;

import com.eyetwin.model.User;
import com.eyetwin.service.RememberMeService;

public class SessionManager {

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser()          { return currentUser; }
    public static boolean isLoggedIn()           { return currentUser != null; }

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

    public static void logout() {
        System.out.println("👋 Logout: "
                + (currentUser != null ? currentUser.getEmail() : "?"));
        currentUser = null;
        // NE PAS appeler RememberMeService.clear() ici —
        // le remember me doit persister entre les sessions,
        // exactement comme un cookie "remember_me" Symfony.
        // clear() est appelé uniquement quand l'utilisateur
        // décoche manuellement la checkbox dans le formulaire.
    }
}