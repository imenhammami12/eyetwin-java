package com.eyetwin.service;

import com.eyetwin.model.User;
import com.eyetwin.dao.UserDAO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * TwoFactorAuthService — mirrors App\Service\TwoFactorAuthService (Symfony)
 *
 * Handles:
 *  - TOTP secret generation
 *  - QR code URI generation (otpauth://)
 *  - Code verification (RFC 6238)
 *  - Backup codes generation / validation
 *  - Enable / disable 2FA
 */
public class TwoFactorAuthService {

    private static final String ISSUER        = "EyeTwin";
    private static final int    TOTP_DIGITS   = 6;
    private static final int    TOTP_PERIOD   = 30;       // seconds
    private static final int    TOTP_WINDOW   = 1;        // ±1 period tolerance
    private static final int    BACKUP_COUNT  = 8;
    private static final String BASE32_CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String BACKUP_CHARS  = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UserDAO userDAO;

    public TwoFactorAuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // ═══════════════════════════════════════════════════════════
    //  ENABLE / DISABLE  — mirrors Symfony methods
    // ═══════════════════════════════════════════════════════════

    /**
     * Generate a TOTP secret for the user (does NOT enable 2FA yet).
     * User must verify a code first — mirrors enableTwoFactorAuth()
     */
    public String prepareTwoFactorAuth(User user) {
        String secret = generateBase32Secret();
        user.setTotpSecret(secret);
        userDAO.update(user);
        return secret;
    }

    /**
     * Verify code and fully enable 2FA + generate backup codes.
     * Mirrors verifyAndEnableTwoFactorAuth()
     */
    public boolean verifyAndEnableTwoFactorAuth(User user, String code) {
        if (user.getTotpSecret() == null) return false;

        if (verifyTotpCode(user.getTotpSecret(), code)) {
            List<String> backupCodes = generateBackupCodes(BACKUP_COUNT);
            user.setIsTotpEnabled(true);
            user.setBackupCodes(backupCodes);
            userDAO.update(user);
            return true;
        }
        return false;
    }

    /**
     * Disable 2FA and clear all 2FA data.
     * Mirrors disableTwoFactorAuth()
     */
    public void disableTwoFactorAuth(User user) {
        user.setIsTotpEnabled(false);
        user.setTotpSecret(null);
        user.setBackupCodes(null);
        userDAO.update(user);
    }

    // ═══════════════════════════════════════════════════════════
    //  QR CODE URI  — mirrors getQrCodeContent()
    // ═══════════════════════════════════════════════════════════

    /**
     * Build the otpauth:// URI — identical format to Symfony service.
     * Pass this to a QR library or display as text for manual entry.
     */
    public String getQrCodeContent(User user) {
        if (user.getTotpSecret() == null)
            throw new IllegalStateException("TOTP secret not set for user");
        if (user.getEmail() == null)
            throw new IllegalStateException("User email not set");

        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            urlEncode(ISSUER),
            urlEncode(user.getEmail()),
            user.getTotpSecret(),
            urlEncode(ISSUER),
            TOTP_DIGITS,
            TOTP_PERIOD
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  TOTP VERIFICATION  — RFC 6238
    // ═══════════════════════════════════════════════════════════

    /**
     * Verify a 6-digit TOTP code.
     * Checks current period ± TOTP_WINDOW for clock drift.
     */
    public boolean verifyTotpCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) return false;

        try {
            byte[] secretBytes = decodeBase32(base32Secret);
            long   timeStep    = Instant.now().getEpochSecond() / TOTP_PERIOD;

            for (int i = -TOTP_WINDOW; i <= TOTP_WINDOW; i++) {
                String expected = generateTotp(secretBytes, timeStep + i);
                if (expected.equals(code)) return true;
            }
        } catch (Exception e) {
            System.err.println("[TwoFactorAuthService] TOTP verify error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Verify a backup code — invalidates it on success.
     * Mirrors verifyBackupCode()
     */
    public boolean verifyBackupCode(User user, String code) {
        List<String> codes = user.getBackupCodes();
        if (codes == null || codes.isEmpty()) return false;

        String normalised = code.trim().toUpperCase();
        if (codes.remove(normalised)) {
            userDAO.update(user);
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  BACKUP CODES  — mirrors generateBackupCodes()
    // ═══════════════════════════════════════════════════════════

    /** Generate fresh backup codes and persist them. Mirrors regenerateBackupCodes() */
    public List<String> regenerateBackupCodes(User user) {
        List<String> codes = generateBackupCodes(BACKUP_COUNT);
        user.setBackupCodes(codes);
        userDAO.update(user);
        return codes;
    }

    /** Generate a list of backup codes (not persisted). */
    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) codes.add(generateSingleBackupCode());
        return codes;
    }

    // ═══════════════════════════════════════════════════════════
    //  STATE CHECKS
    // ═══════════════════════════════════════════════════════════

    /** Mirrors isTwoFactorEnabled() */
    public boolean isTwoFactorEnabled(User user) {
        return user.isTotpAuthenticationEnabled();
    }

    /** Mirrors getRemainingBackupCodesCount() */
    public int getRemainingBackupCodesCount(User user) {
        List<String> codes = user.getBackupCodes();
        return codes != null ? codes.size() : 0;
    }

    // ═══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Generate a random 160-bit Base32 secret (same entropy as Symfony bundle) */
    private String generateBase32Secret() {
        SecureRandom rng   = new SecureRandom();
        byte[]       bytes = new byte[20]; // 160 bits
        rng.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /** Generate a single TOTP code for a given time step (HMAC-SHA1 / RFC 6238) */
    private String generateTotp(byte[] secret, long timeStep) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] msg  = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac    mac  = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = mac.doFinal(msg);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset]     & 0x7F) << 24)
                   | ((hash[offset + 1] & 0xFF) << 16)
                   | ((hash[offset + 2] & 0xFF) << 8)
                   |  (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", otp);
    }

    /** Encode bytes to Base32 (RFC 4648) */
    private String encodeBase32(byte[] data) {
        StringBuilder sb      = new StringBuilder();
        int           buffer  = 0;
        int           bitsLeft = 0;

        for (byte b : data) {
            buffer    = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        return sb.toString();
    }

    /** Decode Base32 string to bytes */
    private byte[] decodeBase32(String base32) {
        String        upper  = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int           outLen = (upper.length() * 5) / 8;
        byte[]        out    = new byte[outLen];
        int           buffer = 0, bitsLeft = 0, idx = 0;

        for (char c : upper.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer    = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte) (buffer >> bitsLeft);
            }
        }
        return out;
    }

    /** Generate a XXXXX-XXXXX backup code — mirrors generateBackupCode() */
    private String generateSingleBackupCode() {
        SecureRandom rng  = new SecureRandom();
        StringBuilder sb  = new StringBuilder(11);
        for (int i = 0; i < 10; i++) {
            if (i == 5) sb.append('-');
            sb.append(BACKUP_CHARS.charAt(rng.nextInt(BACKUP_CHARS.length())));
        }
        return sb.toString();
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
