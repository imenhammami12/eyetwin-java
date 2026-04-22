package com.eyetwin.tools;

import com.eyetwin.entities.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    @AfterEach
    void tearDown() {
        SessionManager.logout();
    }

    @Test
    void setCurrentUser_shouldReportLoggedIn() {
        User user = new User();
        user.setRolesJson("[\"ROLE_USER\"]");
        user.setEmail("user@test.com");

        SessionManager.setCurrentUser(user);

        assertTrue(SessionManager.isLoggedIn());
        assertFalse(SessionManager.isGuest());
        assertEquals("ROLE_USER", SessionManager.getHighestRole());
    }

    @Test
    void roleChecks_shouldIdentifyAdminAndCoach() {
        User admin = new User();
        admin.setRolesJson("[\"ROLE_USER\", \"ROLE_ADMIN\"]");
        SessionManager.setCurrentUser(admin);

        assertTrue(SessionManager.isAdmin());
        assertFalse(SessionManager.isCoach());
        assertEquals("ROLE_ADMIN", SessionManager.getHighestRole());

        User coach = new User();
        coach.setRolesJson("[\"ROLE_USER\", \"ROLE_COACH\"]");
        SessionManager.setCurrentUser(coach);

        assertTrue(SessionManager.isCoach());
        assertFalse(SessionManager.isAdmin());
        assertEquals("ROLE_COACH", SessionManager.getHighestRole());
    }

    @Test
    void logout_shouldClearCurrentUser() {
        User user = new User();
        user.setRolesJson("[\"ROLE_USER\"]");
        SessionManager.setCurrentUser(user);

        assertTrue(SessionManager.isLoggedIn());
        SessionManager.logout();
        assertFalse(SessionManager.isLoggedIn());
        assertTrue(SessionManager.isGuest());
    }
}
