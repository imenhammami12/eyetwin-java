package com.eyetwin.services;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.tools.SessionManager;

/**
 * AuthService — délègue tout à IUserService.
 * Plus de UserDAO direct : conforme à l'architecture interfaces/services.
 */
public class AuthService {

    private final IUserService userService;

    public AuthService(IUserService userService) {
        this.userService = userService;
    }

    public User login(String email, String password) {
        return userService.login(email, password);
    }

    public boolean register(String fullName, String email, String password) {
        return userService.register(fullName, email, password);
    }

    public boolean hasRole(String role) {
        return userService.hasRole(role);
    }

    public boolean isAdmin() {
        return userService.isAdmin();
    }

    public boolean isCoach() {
        return userService.isCoach();
    }

    public boolean isUser() {
        return userService.isLoggedIn();
    }

    public void logout() {
        userService.logout();
    }
}