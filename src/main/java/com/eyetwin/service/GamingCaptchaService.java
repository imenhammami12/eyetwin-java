package com.eyetwin.service;

/**
 * Valide le token généré par le Gaming Captcha custom.
 * Format : GC_{timestamp_ms}_{score}_ok
 */
public class GamingCaptchaService {

    public boolean verify(String token) {
        if (token == null || token.isBlank()) return false;
        if (!token.startsWith("GC_") || !token.endsWith("_ok")) return false;

        try {
            String[] parts = token.split("_");
            if (parts.length < 4) return false;

            long timestamp = Long.parseLong(parts[1]);
            long now       = System.currentTimeMillis();

            if ((now - timestamp) > 5L * 60 * 1000) {
                System.out.println("⚠️ Gaming captcha token expiré.");
                return false;
            }

            int score = Integer.parseInt(parts[2]);
            boolean valid = score >= 2;
            System.out.println(valid
                    ? "✅ Captcha valide — score: " + score + "/3"
                    : "❌ Captcha invalide — score: " + score + "/3");
            return valid;

        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur parsing token: " + e.getMessage());
            return false;
        }
    }
}