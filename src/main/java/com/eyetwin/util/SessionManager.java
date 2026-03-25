package com.eyetwin.util;

public class SessionManager {

    private static Object currentUser;

    public static void setCurrentUser(Object user) { currentUser = user; }
    public static Object getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }

    public static void logout() {
        currentUser = null;
        System.out.println("👋 Déconnecté");
    }
}