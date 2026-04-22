package com.eyetwin.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceImplTest {

    private final UserServiceImpl userService = new UserServiceImpl();

    @Test
    void login_throwsWhenEmailIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.login(null, "Password123"));
        assertEquals("Email is required.", exception.getMessage());
    }

    @Test
    void login_throwsWhenEmailIsMalformed() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.login("not-an-email", "Password123"));
        assertEquals("Invalid email format.", exception.getMessage());
    }

    @Test
    void login_throwsWhenPasswordIsBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.login("user@test.com", ""));
        assertEquals("Password is required.", exception.getMessage());
    }

    @Test
    void register_throwsWhenFullNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(null, "user@test.com", "Password123"));
        assertEquals("Full name is required.", exception.getMessage());
    }

    @Test
    void register_throwsWhenFullNameTooShort() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("A", "user@test.com", "Password123"));
        assertEquals("Full name must be at least 2 characters.", exception.getMessage());
    }

    @Test
    void register_throwsWhenEmailIsInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("Jane Doe", "bad-email", "Password123"));
        assertEquals("Invalid email format.", exception.getMessage());
    }

    @Test
    void register_throwsWhenPasswordTooShort() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("Jane Doe", "user@test.com", "P1a"));
        assertEquals("Password must be at least 6 characters.", exception.getMessage());
    }

    @Test
    void register_throwsWhenPasswordHasNoUppercase() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("Jane Doe", "user@test.com", "password123"));
        assertEquals("Password must contain uppercase, lowercase and a number.", exception.getMessage());
    }

    @Test
    void register_throwsWhenPasswordHasNoDigit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("Jane Doe", "user@test.com", "Password"));
        assertEquals("Password must contain uppercase, lowercase and a number.", exception.getMessage());
    }
}
