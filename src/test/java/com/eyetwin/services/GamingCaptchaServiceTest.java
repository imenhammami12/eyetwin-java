package com.eyetwin.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GamingCaptchaServiceTest {

    private final GamingCaptchaService captchaService = new GamingCaptchaService();

    @Test
    void verify_shouldReturnFalseForNullToken() {
        assertFalse(captchaService.verify(null));
    }

    @Test
    void verify_shouldReturnFalseForBlankToken() {
        assertFalse(captchaService.verify("   "));
    }

    @Test
    void verify_shouldReturnFalseForInvalidPrefix() {
        assertFalse(captchaService.verify("BAD_123_3_ok"));
    }

    @Test
    void verify_shouldReturnFalseForInvalidSuffix() {
        assertFalse(captchaService.verify("GC_1234567890_3_no"));
    }

    @Test
    void verify_shouldReturnFalseForExpiredToken() {
        String token = "GC_1000000000000_3_ok"; // timestamp ancien
        assertFalse(captchaService.verify(token));
    }

    @Test
    void verify_shouldReturnFalseForLowScore() {
        long now = System.currentTimeMillis();
        String token = "GC_" + now + "_1_ok";
        assertFalse(captchaService.verify(token));
    }

    @Test
    void verify_shouldReturnTrueForValidToken() {
        long now = System.currentTimeMillis();
        String token = "GC_" + now + "_2_ok";
        assertTrue(captchaService.verify(token));
    }
}
