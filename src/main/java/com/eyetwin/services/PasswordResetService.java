package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;
import com.eyetwin.tools.DatabaseConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

public class PasswordResetService {

    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String SMTP_USER      = ConfigLoader.get("SMTP_USER");
    private static final String SMTP_PASS      = ConfigLoader.get("SMTP_PASS");
    private static final String MAIL_FROM      = ConfigLoader.get("SMTP_USER");
    private static final String MAIL_FROM_NAME = "E-Sport Platform";

    private static final String TWILIO_SID         = ConfigLoader.get("TWILIO_SID");
    private static final String TWILIO_TOKEN       = ConfigLoader.get("TWILIO_TOKEN");
    private static final String TWILIO_FROM        = ConfigLoader.get("TWILIO_FROM");
    private static final String TELEGRAM_BOT_TOKEN = ConfigLoader.get("TELEGRAM_BOT_TOKEN");

    // ─────────────────────────────────────────────────────────────────────────

    public String requestPasswordReset(String email, String channel) throws Exception {
        try (Connection conn = getConnection()) {
            int userId = findUserIdByEmail(conn, email);
            if (userId == -1) return null;
            deleteOldTokens(conn, userId);
            String token = generateToken();
            persistToken(conn, userId, token, channel);
            sendNotification(conn, userId, email, token, channel);
            return token;
        }
    }

    public boolean verifyToken(String token) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT expires_at, used FROM password_reset_tokens WHERE token = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp exp  = rs.getTimestamp("expires_at");
                    boolean   used = rs.getBoolean("used");
                    return exp != null
                            && exp.toLocalDateTime().isAfter(LocalDateTime.now())
                            && !used;
                }
            }
        } catch (Exception e) {
            System.err.println("[PasswordResetService] verifyToken: " + e.getMessage());
        }
        return false;
    }

    public boolean applyNewPassword(String token, String hashedPassword) {
        try (Connection conn = getConnection()) {
            String sel    = "SELECT id, user_id FROM password_reset_tokens WHERE token = ?";
            int tokenId   = -1, userId = -1;
            try (PreparedStatement ps = conn.prepareStatement(sel)) {
                ps.setString(1, token);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tokenId = rs.getInt("id");
                    userId  = rs.getInt("user_id");
                }
            }
            if (userId == -1) return false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `user` SET password = ? WHERE id = ?")) {
                ps.setString(1, hashedPassword);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE password_reset_tokens SET used = 1 WHERE id = ?")) {
                ps.setInt(1, tokenId);
                ps.executeUpdate();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[PasswordResetService] applyNewPassword: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Connection getConnection() {
        return DatabaseConfig.getInstance().getCnx();
    }

    private int findUserIdByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM `user` WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private void deleteOldTokens(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM password_reset_tokens WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private void persistToken(Connection conn, int userId, String token, String channel)
            throws SQLException {
        String sql = "INSERT INTO password_reset_tokens "
                + "(user_id, token, created_at, expires_at, channel, used) "
                + "VALUES (?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
            ps.setString(5, channel);
            ps.executeUpdate();
        }
    }

    private void sendNotification(Connection conn, int userId, String email,
                                  String token, String channel) throws Exception {
        switch (channel) {
            case "sms"      -> sendSms(conn, userId, token);
            case "telegram" -> sendTelegram(conn, userId, token);
            default         -> sendEmail(email, token);
        }
    }

    private void sendEmail(String toEmail, String token) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(MAIL_FROM, MAIL_FROM_NAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("Password Reset — E-Sport Platform");

        String body = """
            <div style="font-family:'Courier New',monospace;background:#0d1117;color:#c9d1d9;
                        padding:32px;border-radius:8px;max-width:520px;">
              <h2 style="color:#dc143c;">🎮 Password Reset</h2>
              <code style="display:block;background:#161b22;padding:14px;
                           border-left:3px solid #dc143c;word-break:break-all;
                           font-size:11px;color:#79c0ff;">%s</code>
              <p style="color:#484f58;font-size:11px;">Expires in 1 hour.</p>
            </div>
            """.formatted(token);

        msg.setContent(body, "text/html; charset=UTF-8");
        Transport.send(msg);
    }

    private void sendSms(Connection conn, int userId, String token) throws Exception {
        String phone = getUserField(conn, userId, "phone");
        if (phone == null || phone.isBlank())
            throw new Exception("No phone number for this account.");

        String body        = "EyeTwin reset code: " + token + "\nExpires in 1h.";
        String credentials = TWILIO_SID + ":" + TWILIO_TOKEN;
        String encoded     = java.util.Base64.getEncoder()
                .encodeToString(credentials.getBytes());
        String formData    = "To="   + java.net.URLEncoder.encode(phone,       "UTF-8")
                + "&From=" + java.net.URLEncoder.encode(TWILIO_FROM, "UTF-8")
                + "&Body=" + java.net.URLEncoder.encode(body,        "UTF-8");

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/"
                                + TWILIO_SID + "/Messages.json"))
                        .header("Authorization", "Basic " + encoded)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400)
            throw new Exception("SMS failed: " + resp.body());
    }

    private void sendTelegram(Connection conn, int userId, String token) throws Exception {
        String chatId = getUserField(conn, userId, "telegram_chat_id");
        if (chatId == null || chatId.isBlank())
            throw new Exception("No Telegram Chat ID for this account.");

        String text = "🎮 EyeTwin Password Reset\n\nCode:\n" + token
                + "\n\nExpires in 1 hour.";
        String url  = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN
                + "/sendMessage?chat_id=" + java.net.URLEncoder.encode(chatId, "UTF-8")
                + "&text="               + java.net.URLEncoder.encode(text, "UTF-8");

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400)
            throw new Exception("Telegram failed: " + resp.body());
    }

    private String getUserField(Connection conn, int userId, String field) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " + field + " FROM `user` WHERE id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(field) : null;
        }
    }
}