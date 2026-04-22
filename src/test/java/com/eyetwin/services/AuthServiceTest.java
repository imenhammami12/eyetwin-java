package com.eyetwin.services;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private static class StubUserService implements IUserService {
        private boolean loggedIn;

        @Override
        public User findByEmail(String email) {
            return null;
        }

        @Override
        public User findById(int id) {
            return null;
        }

        @Override
        public boolean emailExists(String email) {
            return false;
        }

        @Override
        public User findByUsername(String username) {
            return null;
        }

        @Override
        public boolean save(String fullName, String email, String hashedPassword) {
            return false;
        }

        @Override
        public void update(User user) {
        }

        @Override
        public void saveProfilePicture(int userId, byte[] imageBytes, String filename) {
        }

        @Override
        public java.util.List<User> getAllUsers() {
            return java.util.Collections.emptyList();
        }

        @Override
        public User login(String email, String password) {
            return new User();
        }

        @Override
        public boolean register(String fullName, String email, String password) {
            return true;
        }

        @Override
        public boolean verifyPassword(String email, String plainPassword) {
            return false;
        }

        @Override
        public void logout() {
            loggedIn = false;
        }

        @Override
        public boolean hasRole(String role) {
            return "ROLE_ADMIN".equals(role);
        }

        @Override
        public boolean isAdmin() {
            return true;
        }

        @Override
        public boolean isCoach() {
            return false;
        }

        @Override
        public boolean isLoggedIn() {
            return loggedIn;
        }

        @Override
        public void adminCreateUser(String fullName, String username, String email, String plainPassword, String role) {
        }

        @Override
        public void updateUserRole(int userId, String newRole) {
        }

        @Override
        public void suspendUser(int userId) {
        }

        @Override
        public void banUser(int userId) {
        }

        @Override
        public void activateUser(int userId) {
        }

        @Override
        public void deleteUser(int userId) {
        }

        @Override
        public java.util.List<com.eyetwin.entities.TeamMembership> getTeamMemberships(int userId) {
            return java.util.Collections.emptyList();
        }
    }

    private final AuthService authService = new AuthService(new StubUserService());

    @Test
    void login_shouldDelegateToUserService() {
        assertNotNull(authService.login("user@test.com", "Password123"));
    }

    @Test
    void register_shouldDelegateToUserService() {
        assertTrue(authService.register("Jane Doe", "jane@test.com", "Password123"));
    }

    @Test
    void hasRole_shouldReturnTrueForAdminRole() {
        assertTrue(authService.hasRole("ROLE_ADMIN"));
        assertFalse(authService.hasRole("ROLE_USER"));
    }

    @Test
    void isAdmin_shouldReturnTrue() {
        assertTrue(authService.isAdmin());
    }

    @Test
    void isCoach_shouldReturnFalse() {
        assertFalse(authService.isCoach());
    }
}
