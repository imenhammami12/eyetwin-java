package com.eyetwin.interfaces;

import com.eyetwin.entities.User;

import java.util.List;

/**
 * ITwoFactorService — contrat 2FA TOTP.
 * Fusionne TwoFactorAuthService Symfony.
 */
public interface ITwoFactorService {

    // ── Activation / Désactivation ─────────────────────────────
    /** Génère et stocke le secret TOTP (sans activer la 2FA encore) */
    String prepareTwoFactorAuth(User user);

    /** Vérifie le code OTP puis active la 2FA + génère les codes backup */
    boolean verifyAndEnableTwoFactorAuth(User user, String code);

    /** Désactive la 2FA et efface toutes les données TOTP */
    void disableTwoFactorAuth(User user);

    // ── QR Code ────────────────────────────────────────────────
    /** Retourne l'URI otpauth:// pour générer le QR code */
    String getQrCodeContent(User user);

    // ── Vérification ───────────────────────────────────────────
    boolean verifyTotpCode(String base32Secret, String code);
    boolean verifyBackupCode(User user, String code);

    // ── Codes backup ───────────────────────────────────────────
    List<String> regenerateBackupCodes(User user);
    List<String> generateBackupCodes(int count);

    // ── État ───────────────────────────────────────────────────
    boolean isTwoFactorEnabled(User user);
    int getRemainingBackupCodesCount(User user);
}