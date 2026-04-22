package com.eyetwin.services;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ITwoFactorService;
import com.eyetwin.interfaces.IUserService;

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
 * TwoFactorServiceImpl — implémentation de ITwoFactorService.
 * Fusionne l'ancien TwoFactorAuthService (logique TOTP RFC 6238).
 *
 * Dépend de IUserService pour la persistance (pas de DAO direct).
 */
public class TwoFactorServiceImpl implements ITwoFactorService {

    private static final String ISSUER       = "EyeTwin";
    private static final int    TOTP_DIGITS  = 6;
    private static final int    TOTP_PERIOD  = 30;
    private static final int    TOTP_WINDOW  = 1;
    private static final int    BACKUP_COUNT = 8;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String BACKUP_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final IUserService userService;

    /** Injection de IUserService (pas de new UserDAO() !) */
    public TwoFactorServiceImpl(IUserService userService) {
        this.userService = userService;
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIVATION / DÉSACTIVATION
    // ════════════════════════════════════════════════════════════

    @Override
    public String prepareTwoFactorAuth(User user) {
        String secret = generateBase32Secret();
        user.setTotpSecret(secret);
        userService.update(user);
        return secret;
    }

    @Override
    public boolean verifyAndEnableTwoFactorAuth(User user, String code) {
        if (user.getTotpSecret() == null) return false;
        if (verifyTotpCode(user.getTotpSecret(), code)) {
            List<String> backupCodes = generateBackupCodes(BACKUP_COUNT);
            user.setIsTotpEnabled(true);
            user.setBackupCodes(backupCodes);
            userService.update(user);
            return true;
        }
        return false;
    }

    @Override
    public void disableTwoFactorAuth(User user) {
        user.setIsTotpEnabled(false);
        user.setTotpSecret(null);
        user.setBackupCodes(null);
        userService.update(user);
    }

    // ════════════════════════════════════════════════════════════
    //  QR CODE URI
    // ════════════════════════════════════════════════════════════

    @Override
    public String getQrCodeContent(User user) {
        if (user.getTotpSecret() == null)
            throw new IllegalStateException("TOTP secret not set for user");
        if (user.getEmail() == null)
            throw new IllegalStateException("User email not set");
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                urlEncode(ISSUER), urlEncode(user.getEmail()),
                user.getTotpSecret(), urlEncode(ISSUER), TOTP_DIGITS, TOTP_PERIOD);
    }

    // ════════════════════════════════════════════════════════════
    //  VÉRIFICATION TOTP
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean verifyTotpCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) return false;
        try {
            byte[] secretBytes = decodeBase32(base32Secret);
            long   timeStep    = Instant.now().getEpochSecond() / TOTP_PERIOD;
            for (int i = -TOTP_WINDOW; i <= TOTP_WINDOW; i++) {
                if (generateTotp(secretBytes, timeStep + i).equals(code)) return true;
            }
        } catch (Exception e) {
            System.err.println("[TwoFactorService] TOTP verify error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean verifyBackupCode(User user, String code) {
        List<String> codes = user.getBackupCodes();
        if (codes == null || codes.isEmpty()) return false;
        String normalised = code.trim().toUpperCase();
        if (codes.remove(normalised)) {
            userService.update(user);
            return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    //  CODES BACKUP
    // ════════════════════════════════════════════════════════════

    @Override
    public List<String> regenerateBackupCodes(User user) {
        List<String> codes = generateBackupCodes(BACKUP_COUNT);
        user.setBackupCodes(codes);
        userService.update(user);
        return codes;
    }

    @Override
    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) codes.add(generateSingleBackupCode());
        return codes;
    }

    // ════════════════════════════════════════════════════════════
    //  ÉTAT
    // ════════════════════════════════════════════════════════════

    @Override
    public boolean isTwoFactorEnabled(User user) {
        return user.isTotpAuthenticationEnabled();
    }

    @Override
    public int getRemainingBackupCodesCount(User user) {
        List<String> codes = user.getBackupCodes();
        return codes != null ? codes.size() : 0;
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS (TOTP RFC 6238)
    // ════════════════════════════════════════════════════════════

    private String generateBase32Secret() {
        SecureRandom rng   = new SecureRandom();
        byte[]       bytes = new byte[20];
        rng.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    private String generateTotp(byte[] secret, long timeStep)
            throws NoSuchAlgorithmException, InvalidKeyException {
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

    private String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
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

    private byte[] decodeBase32(String base32) {
        String upper  = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int    outLen = (upper.length() * 5) / 8;
        byte[] out    = new byte[outLen];
        int    buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : upper.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer    = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[idx++] = (byte)(buffer >> bitsLeft);
            }
        }
        return out;
    }

    private String generateSingleBackupCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 10; i++) {
            if (i == 5) sb.append('-');
            sb.append(BACKUP_CHARS.charAt(rng.nextInt(BACKUP_CHARS.length())));
        }
        return sb.toString();
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
