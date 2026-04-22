package com.eyetwin.services;

import com.eyetwin.entities.Tournoi;
import com.eyetwin.entities.User;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final int    SMTP_PORT    = 587;
    private static final String SMTP_USER    = "ferferferfer1288@gmail.com";
    private static final String SMTP_PASSWORD= "znpf mula zqdg stll";
    private static final String FROM_NAME    = "E-Sport Platform";
    private static final String FROM_EMAIL   = "ferferferfer1288@gmail.com";
    private static final String PLATFORM_URL = "https://eye2win-metamind.onrender.com";
    private static final String LOGO_URL     =
            "https://eye2win-metamind.onrender.com/assets/img/eyetwin-logo.png";

    private static EmailService instance;
    public static EmailService getInstance() {
        if (instance == null) instance = new EmailService();
        return instance;
    }

    private final Session session;

    private EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });
    }

    public void sendHtml(String toEmail, String subject, String htmlBody)
            throws MessagingException {
        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(FROM_EMAIL));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
        System.out.println("[EmailService] ✅ Email sent to " + toEmail);
    }

    public void sendWelcomeEmail(String toEmail, String fullName,
                                 String username, String rawPassword,
                                 String role) {
        String roleLabel, roleColor, roleIcon;
        switch (role) {
            case "ROLE_COACH"       -> { roleLabel = "Coach";               roleColor = "#f5a623"; roleIcon = "⚡"; }
            case "ROLE_ADMIN"       -> { roleLabel = "Administrator";       roleColor = "#ff3c64"; roleIcon = "🛡"; }
            case "ROLE_SUPER_ADMIN" -> { roleLabel = "Super Administrator"; roleColor = "#a78bfa"; roleIcon = "👑"; }
            default                 -> { roleLabel = "Member";              roleColor = "#4facfe"; roleIcon = "🎮"; }
        }

        int    year     = java.time.LocalDate.now().getYear();
        String initials = (fullName != null && fullName.length() >= 2)
                ? fullName.substring(0, 2).toUpperCase() : "??";

        String html = "<!DOCTYPE html><html lang='en'><head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "</head>"
                + "<body style='margin:0;padding:0;background:#0a0514;font-family:Arial,sans-serif;'>"

                // ── Outer wrapper ──────────────────────────────────
                + "<table width='100%' cellpadding='0' cellspacing='0' border='0'"
                + " style='background:#0a0514;padding:32px 16px;'>"
                + "<tr><td align='center'>"

                // ── Card ───────────────────────────────────────────
                + "<table width='560' cellpadding='0' cellspacing='0' border='0'"
                + " style='max-width:560px;width:100%;background:#0d0618;"
                + "border-radius:18px;overflow:hidden;"
                + "border:1px solid rgba(255,60,100,0.25);'>"

                // ── Top gradient bar ───────────────────────────────
                + "<tr><td height='5'"
                + " style='background:linear-gradient(to right,#ff3c64,#a78bfa,#4facfe);"
                + "font-size:0;line-height:0;'>&nbsp;</td></tr>"

                // ── Header with logo ───────────────────────────────
                + "<tr><td style='background:#1a0a22;padding:22px 32px;"
                + "border-bottom:1px solid rgba(255,60,100,0.20);'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr>"
                + "<td>"
                + "<img src='" + LOGO_URL + "' alt='EyeTwin' width='130'"
                + " style='display:block;height:38px;width:auto;"
                + "border:0;outline:none;' />"
                + "</td>"
                + "<td align='right'"
                + " style='color:rgba(255,255,255,0.28);font-size:10px;"
                + "font-family:monospace;letter-spacing:1px;'>ADMIN PORTAL</td>"
                + "</tr></table>"
                + "</td></tr>"

                // ── Avatar + greeting ──────────────────────────────
                + "<tr><td style='padding:36px 32px 24px;text-align:center;"
                + "background:#0d0618;'>"
                + "<div style='width:68px;height:68px;border-radius:50%;"
                + "background:linear-gradient(135deg,#ff3c64,#764ba2);"
                + "margin:0 auto 18px;line-height:68px;"
                + "font-size:24px;font-weight:bold;color:white;text-align:center;'>"
                + initials
                + "</div>"
                + "<h1 style='margin:0 0 10px;color:white;font-size:22px;"
                + "font-weight:700;'>Welcome, " + fullName + "! &#127881;</h1>"
                + "<p style='margin:0;color:rgba(255,255,255,0.50);font-size:13px;"
                + "line-height:1.7;'>"
                + "Your <strong style='color:#ff8fa3;'>EyeTwin E-Sport Platform</strong>"
                + " account has been created by an administrator.<br>"
                + "Here are your login credentials &mdash; keep them safe."
                + "</p>"
                + "</td></tr>"

                // ── Credentials card ───────────────────────────────
                + "<tr><td style='padding:0 32px 24px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'"
                + " style='background:#160a22;border-radius:12px;"
                + "border:1px solid rgba(255,60,100,0.20);overflow:hidden;'>"

                // Full Name row
                + "<tr><td style='padding:13px 20px;"
                + "border-bottom:1px solid rgba(255,255,255,0.06);'>"
                + "<table width='100%'><tr>"
                + "<td style='color:rgba(255,255,255,0.40);font-size:12px;'>Full Name</td>"
                + "<td align='right' style='color:white;font-size:13px;"
                + "font-weight:700;'>" + fullName + "</td>"
                + "</tr></table></td></tr>"

                // Username row
                + "<tr><td style='padding:13px 20px;"
                + "border-bottom:1px solid rgba(255,255,255,0.06);'>"
                + "<table width='100%'><tr>"
                + "<td style='color:rgba(255,255,255,0.40);font-size:12px;'>Username</td>"
                + "<td align='right' style='color:#4facfe;font-size:13px;"
                + "font-weight:700;'>@" + username + "</td>"
                + "</tr></table></td></tr>"

                // Email row
                + "<tr><td style='padding:13px 20px;"
                + "border-bottom:1px solid rgba(255,255,255,0.06);'>"
                + "<table width='100%'><tr>"
                + "<td style='color:rgba(255,255,255,0.40);font-size:12px;'>Email</td>"
                + "<td align='right' style='color:white;font-size:13px;"
                + "font-weight:700;'>" + toEmail + "</td>"
                + "</tr></table></td></tr>"

                // Role row
                + "<tr><td style='padding:13px 20px;'>"
                + "<table width='100%'><tr>"
                + "<td style='color:rgba(255,255,255,0.40);font-size:12px;'>Role</td>"
                + "<td align='right'>"
                + "<span style='display:inline-block;padding:4px 14px;"
                + "border-radius:20px;font-size:11px;font-weight:700;"
                + "color:" + roleColor + ";"
                + "background:rgba(255,255,255,0.06);"
                + "border:1px solid " + roleColor + ";'>"
                + roleIcon + " " + roleLabel
                + "</span>"
                + "</td></tr></table></td></tr>"

                + "</table></td></tr>"

                // ── Password section ───────────────────────────────
                + "<tr><td style='padding:0 32px 24px;'>"
                + "<p style='margin:0 0 10px;color:rgba(255,255,255,0.55);"
                + "font-size:13px;'>&#128273;&nbsp; Your temporary password:</p>"
                + "<div style='background:rgba(255,60,100,0.08);"
                + "border:1px solid rgba(255,60,100,0.35);border-radius:10px;"
                + "padding:16px 20px;text-align:center;"
                + "font-family:monospace;font-size:22px;font-weight:bold;"
                + "color:#ff8fa3;letter-spacing:5px;'>"
                + rawPassword
                + "</div>"
                + "<p style='margin:10px 0 0;color:rgba(255,193,7,0.85);"
                + "font-size:12px;text-align:center;'>"
                + "&#9888;&#65039;&nbsp; For security reasons, please change your"
                + " password immediately after your first login."
                + "</p>"
                + "</td></tr>"

                // ── Divider ────────────────────────────────────────
                + "<tr><td style='padding:0 32px 24px;'>"
                + "<div style='height:1px;"
                + "background:linear-gradient(to right,"
                + "transparent,rgba(255,60,100,0.30),transparent);'>"
                + "</div></td></tr>"

                // ── Features grid ──────────────────────────────────
                + "<tr><td style='padding:0 32px 28px;'>"
                + "<p style='margin:0 0 14px;color:white;font-size:14px;"
                + "font-weight:700;'>What you can do on the platform:</p>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr>"

                // Feature 1
                + "<td width='50%' style='padding:0 6px 10px 0;vertical-align:top;'>"
                + "<div style='background:#160a22;"
                + "border:1px solid rgba(255,255,255,0.07);"
                + "border-radius:10px;padding:14px;'>"
                + "<div style='font-size:18px;margin-bottom:6px;'>&#127942;</div>"
                + "<div style='color:white;font-size:12px;font-weight:700;"
                + "margin-bottom:4px;'>Tournaments</div>"
                + "<div style='color:rgba(255,255,255,0.40);font-size:11px;'>"
                + "Join &amp; compete in e-sport events</div>"
                + "</div></td>"

                // Feature 2
                + "<td width='50%' style='padding:0 0 10px 6px;vertical-align:top;'>"
                + "<div style='background:#160a22;"
                + "border:1px solid rgba(255,255,255,0.07);"
                + "border-radius:10px;padding:14px;'>"
                + "<div style='font-size:18px;margin-bottom:6px;'>&#128101;</div>"
                + "<div style='color:white;font-size:12px;font-weight:700;"
                + "margin-bottom:4px;'>Teams</div>"
                + "<div style='color:rgba(255,255,255,0.40);font-size:11px;'>"
                + "Create or join competitive teams</div>"
                + "</div></td>"

                + "</tr><tr>"

                // Feature 3
                + "<td width='50%' style='padding:0 6px 0 0;vertical-align:top;'>"
                + "<div style='background:#160a22;"
                + "border:1px solid rgba(255,255,255,0.07);"
                + "border-radius:10px;padding:14px;'>"
                + "<div style='font-size:18px;margin-bottom:6px;'>&#127909;</div>"
                + "<div style='color:white;font-size:12px;font-weight:700;"
                + "margin-bottom:4px;'>Live Streams</div>"
                + "<div style='color:rgba(255,255,255,0.40);font-size:11px;'>"
                + "Watch &amp; interact with coaches</div>"
                + "</div></td>"

                // Feature 4
                + "<td width='50%' style='padding:0 0 0 6px;vertical-align:top;'>"
                + "<div style='background:#160a22;"
                + "border:1px solid rgba(255,255,255,0.07);"
                + "border-radius:10px;padding:14px;'>"
                + "<div style='font-size:18px;margin-bottom:6px;'>&#128172;</div>"
                + "<div style='color:white;font-size:12px;font-weight:700;"
                + "margin-bottom:4px;'>Community</div>"
                + "<div style='color:rgba(255,255,255,0.40);font-size:11px;'>"
                + "Chat, channels &amp; events</div>"
                + "</div></td>"

                + "</tr></table></td></tr>"

                // ── CTA button ─────────────────────────────────────
                + "<tr><td align='center' style='padding:8px 32px 36px;'>"
                + "<a href='" + PLATFORM_URL + "'"
                + " style='display:inline-block;padding:14px 40px;"
                + "background:linear-gradient(to right,#ff3c64,#c0132f);"
                + "color:white;text-decoration:none;border-radius:10px;"
                + "font-weight:700;font-size:14px;letter-spacing:0.5px;'>"
                + "Access the Platform &rarr;"
                + "</a>"
                + "<p style='margin:10px 0 0;color:rgba(255,255,255,0.25);"
                + "font-size:11px;'>" + PLATFORM_URL + "</p>"
                + "</td></tr>"

                // ── Footer ─────────────────────────────────────────
                + "<tr><td style='padding:18px 32px;text-align:center;"
                + "border-top:1px solid rgba(255,255,255,0.07);"
                + "background:rgba(0,0,0,0.20);'>"
                + "<p style='margin:0 0 5px;color:rgba(255,255,255,0.25);"
                + "font-size:11px;'>"
                + "&#169; " + year + " EyeTwin E-Sport Platform &mdash;"
                + " All rights reserved."
                + "</p>"
                + "<p style='margin:0;color:rgba(255,255,255,0.15);font-size:10px;'>"
                + "This is an automated message. Please do not reply.<br>"
                + "If you did not expect this email, contact our support team."
                + "</p>"
                + "</td></tr>"

                + "</table>"   // ── end card
                + "</td></tr>"
                + "</table>"   // ── end outer
                + "</body></html>";

        new Thread(() -> {
            try {
                sendHtml(toEmail,
                        "\uD83C\uDFAE Welcome to EyeTwin \u2014 Your account is ready!",
                        html);
            } catch (Exception e) {
                System.err.println("[EmailService] ❌ Failed: " + e.getMessage());
            }
        }, "EmailSender").start();
    }

    public void sendTournamentRegistrationEmail(User user, Tournoi tournoi) {
        int year = java.time.LocalDate.now().getYear();
        String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='margin:0;padding:0;background:#0a0514;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' border='0' style='background:#0a0514;padding:32px 16px;'><tr><td align='center'>"
                + "<table width='560' cellpadding='0' cellspacing='0' border='0' style='max-width:560px;width:100%;background:#0d0618;border-radius:18px;overflow:hidden;border:1px solid rgba(255,60,100,0.25);'>"
                + "<tr><td height='5' style='background:linear-gradient(to right,#ff3c64,#a78bfa,#4facfe);font-size:0;line-height:0;'>&nbsp;</td></tr>"
                + "<tr><td style='background:#1a0a22;padding:22px 32px;border-bottom:1px solid rgba(255,60,100,0.20);'>"
                + "<table width='100%'><tr><td><img src='" + LOGO_URL + "' alt='EyeTwin' height='38'/></td>"
                + "<td align='right' style='color:rgba(255,255,255,0.28);font-size:10px;'>CONFIRMATION</td></tr></table></td></tr>"
                + "<tr><td style='padding:36px 32px 24px;text-align:center;'>"
                + "<div style='font-size:48px;margin-bottom:20px;'>🏆</div>"
                + "<h1 style='margin:0 0 10px;color:white;font-size:22px;'>Registration Confirmed!</h1>"
                + "<p style='color:rgba(255,255,255,0.6);font-size:14px;'>You are now officially registered for <strong>" + tournoi.getNom() + "</strong>.</p></td></tr>"
                + "<tr><td style='padding:0 32px 24px;'><table width='100%' style='background:#160a22;border-radius:12px;border:1px solid rgba(255,60,100,0.2);'>"
                + "<tr><td style='padding:15px;color:rgba(255,255,255,0.4);font-size:12px;'>Tournament</td>"
                + "<td align='right' style='padding:15px;color:white;font-weight:bold;'>" + tournoi.getNom() + "</td></tr>"
                + "<tr><td style='padding:15px;color:rgba(255,255,255,0.4);font-size:12px;'>Start Date</td>"
                + "<td align='right' style='padding:15px;color:#4facfe;font-weight:bold;'>" + tournoi.getDateDebut() + "</td></tr>"
                + "<tr><td style='padding:15px;color:rgba(255,255,255,0.4);font-size:12px;'>Amount Paid</td>"
                + "<td align='right' style='padding:15px;color:#ff3c64;font-weight:bold;'>" + tournoi.getPrix() + " EUR</td></tr>"
                + "</table></td></tr>"
                + "<tr><td align='center' style='padding:0 32px 36px;'><p style='color:rgba(255,255,255,0.5);font-size:13px;'>Prepare your gear, warrior. Victory awaits!</p></td></tr>"
                + "<tr><td style='padding:18px 32px;text-align:center;border-top:1px solid rgba(255,255,255,0.07);background:rgba(0,0,0,0.20);'>"
                + "<p style='margin:0;color:rgba(255,255,255,0.25);font-size:11px;'>&#169; " + year + " EyeTwin Platform</p></td></tr>"
                + "</table></td></tr></table></body></html>";

        new Thread(() -> {
            try {
                sendHtml(user.getEmail(), "🏆 Registration Confirmed: " + tournoi.getNom(), html);
            } catch (Exception e) {
                System.err.println("[EmailService] ❌ Failed to send registration email: " + e.getMessage());
            }
        }, "EmailSender").start();
    }
}