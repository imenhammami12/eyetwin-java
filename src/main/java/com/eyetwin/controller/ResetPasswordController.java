package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.services.PasswordResetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Rectangle;

/**
 * ResetPasswordController
 *
 * Mirrors Symfony's PasswordResetController::reset()
 * and reset_password.html.twig
 *
 * Features:
 *  - Password + confirm fields
 *  - Real-time strength bar (weak / medium / strong)
 *  - Requirements checklist (length, uppercase, lowercase, digit)
 *  - Match indicator
 *  - BCrypt hashing before DB write
 *  - Redirects to login on success
 */
public class ResetPasswordController {

    // ── Form fields ───────────────────────────────────────────────────────────
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private TextField     passwordVisible;   // shown when eye toggled
    @FXML private TextField     confirmVisible;

    // ── Strength bar ─────────────────────────────────────────────────────────
    @FXML private Rectangle     strengthBar;       // width driven by score
    @FXML private Label         strengthLabel;     // WEAK / MEDIUM / STRONG

    // ── Requirement labels ────────────────────────────────────────────────────
    @FXML private Label ruleLength;
    @FXML private Label ruleUpper;
    @FXML private Label ruleLower;
    @FXML private Label ruleDigit;

    // ── Match indicator ───────────────────────────────────────────────────────
    @FXML private Label matchLabel;

    // ── Actions ───────────────────────────────────────────────────────────────
    @FXML private Label  errorLabel;
    @FXML private Button submitButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final PasswordResetService resetService = new PasswordResetService();
    private String token;

    // ─────────────────────────────────────────────────────────────────────────
    /** Called by VerifyResetCodeController before scene is shown */
    public void initData(String token) {
        this.token = token;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (errorLabel     != null) errorLabel.setText("");
        if (loadingSpinner != null) loadingSpinner.setVisible(false);
        if (submitButton   != null) submitButton.setDisable(true);

        // Live validation listeners
        if (passwordField != null) {
            passwordField.textProperty().addListener((o, ov, nv) -> onPasswordChanged(nv));
        }
        if (confirmField != null) {
            confirmField.textProperty().addListener((o, ov, nv) -> onConfirmChanged());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REAL-TIME VALIDATION — mirrors JS in reset_password.html.twig
    // ─────────────────────────────────────────────────────────────────────────
    private void onPasswordChanged(String pw) {
        boolean hasLength  = pw.length() >= 8;
        boolean hasUpper   = pw.matches(".*[A-Z].*");
        boolean hasLower   = pw.matches(".*[a-z].*");
        boolean hasDigit   = pw.matches(".*\\d.*");

        styleRule(ruleLength, hasLength);
        styleRule(ruleUpper,  hasUpper);
        styleRule(ruleLower,  hasLower);
        styleRule(ruleDigit,  hasDigit);

        int score = (hasLength ? 1 : 0) + (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0);
        updateStrengthBar(score);

        onConfirmChanged();
        updateSubmitState(hasLength && hasUpper && hasLower && hasDigit);
    }

    private void onConfirmChanged() {
        String pw  = passwordField  != null ? passwordField.getText()  : "";
        String cfg = confirmField   != null ? confirmField.getText()    : "";

        if (cfg.isEmpty()) {
            if (matchLabel != null) matchLabel.setText("");
            return;
        }

        if (pw.equals(cfg)) {
            if (matchLabel != null) {
                matchLabel.setText("✓ Passwords match");
                matchLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 11;");
            }
            updateSubmitState(allRulesValid(pw));
        } else {
            if (matchLabel != null) {
                matchLabel.setText("✗ Passwords do not match");
                matchLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11;");
            }
            if (submitButton != null) submitButton.setDisable(true);
        }
    }

    private void updateStrengthBar(int score) {
        String color, label;
        double pct;
        if (score <= 2)     { color = "#f85149"; label = "WEAK";   pct = 0.33; }
        else if (score == 3){ color = "#d29922"; label = "MEDIUM"; pct = 0.66; }
        else                { color = "#3fb950"; label = "STRONG"; pct = 1.0;  }

        if (strengthBar != null) {
            strengthBar.setWidth(pct * 417); // 417px = full width of form
            strengthBar.setStyle("-fx-fill: " + color + ";");
        }
        if (strengthLabel != null) {
            strengthLabel.setText(label);
            strengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10; -fx-font-weight: bold;");
        }
    }

    private boolean allRulesValid(String pw) {
        return pw.length() >= 8
                && pw.matches(".*[A-Z].*")
                && pw.matches(".*[a-z].*")
                && pw.matches(".*\\d.*");
    }

    private void updateSubmitState(boolean rulesOk) {
        if (submitButton == null) return;
        String pw  = passwordField != null ? passwordField.getText() : "";
        String cfg = confirmField  != null ? confirmField.getText()  : "";
        submitButton.setDisable(!(rulesOk && pw.equals(cfg) && !cfg.isEmpty()));
    }

    private void styleRule(Label rule, boolean valid) {
        if (rule == null) return;
        if (valid) {
            rule.setStyle("-fx-text-fill: #3fb950;");
            rule.setText("✓ " + ruleText(rule));
        } else {
            rule.setStyle("-fx-text-fill: #484f58;");
            rule.setText("○ " + ruleText(rule));
        }
    }

    // Strip leading ✓/○ to get clean text
    private String ruleText(Label rule) {
        String t = rule.getText();
        if (t.startsWith("✓ ") || t.startsWith("○ ")) return t.substring(2);
        return t;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBMIT — mirrors PasswordResetController::reset() POST
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleReset() {
        if (errorLabel != null) errorLabel.setText("");

        String pw  = passwordField != null ? passwordField.getText() : "";
        String cfg = confirmField  != null ? confirmField.getText()  : "";

        if (!pw.equals(cfg)) { setError("Passwords do not match."); return; }
        if (!allRulesValid(pw)) { setError("Password does not meet requirements."); return; }

        setLoading(true);

        Thread worker = new Thread(() -> {
            // BCrypt hash — mirrors Symfony's UserPasswordHasherInterface
            String hashed = BCryptHelper.hash(pw);
            boolean ok = resetService.applyNewPassword(token, hashed);

            Platform.runLater(() -> {
                setLoading(false);
                if (ok) {
                    showSuccessAndNavigate();
                } else {
                    setError("Reset link is invalid or has expired.");
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showSuccessAndNavigate() {
        // Small delay so user can read success
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5)
        );
        pause.setOnFinished(e -> MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login"));

        if (errorLabel != null) {
            errorLabel.setStyle("-fx-text-fill: #3fb950;");
            errorLabel.setText("✓ Password reset successfully! Redirecting to login…");
        }
        pause.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EYE TOGGLE (optional — only wired up if passwordVisible/confirmVisible exist)
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void togglePasswordVisibility() {
        toggleVisibility(passwordField, passwordVisible);
    }

    @FXML
    public void toggleConfirmVisibility() {
        toggleVisibility(confirmField, confirmVisible);
    }

    private void toggleVisibility(PasswordField hidden, TextField visible) {
        if (hidden == null || visible == null) return;
        if (hidden.isVisible()) {
            visible.setText(hidden.getText());
            hidden.setVisible(false); hidden.setManaged(false);
            visible.setVisible(true); visible.setManaged(true);
        } else {
            hidden.setText(visible.getText());
            visible.setVisible(false); visible.setManaged(false);
            hidden.setVisible(true); hidden.setManaged(true);
            hidden.textProperty().addListener((o, ov, nv) -> onPasswordChanged(nv));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void goToLogin() {
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void setLoading(boolean on) {
        if (submitButton   != null) submitButton.setDisable(on);
        if (loadingSpinner != null) loadingSpinner.setVisible(on);
    }

    private void setError(String msg) {
        if (errorLabel != null) {
            errorLabel.setStyle("-fx-text-fill: #f85149;");
            errorLabel.setText(msg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INNER BCrypt HELPER
    //  Uses jBCrypt (org.mindrot:jbcrypt:0.4) — add to pom.xml / build.gradle
    //  Matches Symfony's default bcrypt cost (12).
    // ─────────────────────────────────────────────────────────────────────────
    private static class BCryptHelper {
        static String hash(String plain) {
            // If jBCrypt is on classpath:
            // return org.mindrot.jbcrypt.BCrypt.hashpw(plain, org.mindrot.jbcrypt.BCrypt.gensalt(12));

            // Fallback using Spring Security's BCrypt if available:
            try {
                Class<?> cls = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
                Object encoder = cls.getDeclaredConstructor(int.class).newInstance(12);
                return (String) cls.getMethod("encode", CharSequence.class).invoke(encoder, plain);
            } catch (Exception ignored) {}

            // Last resort: at.favre.lib:bcrypt  (also common in Java projects)
            try {
                Class<?> cls    = Class.forName("at.favre.lib.crypto.bcrypt.BCrypt");
                Object hasher   = cls.getMethod("withDefaults").invoke(null);
                Object result   = hasher.getClass()
                        .getMethod("hashToString", int.class, char[].class)
                        .invoke(hasher, 12, plain.toCharArray());
                return result.toString();
            } catch (Exception ignored) {}

            throw new RuntimeException(
                    "No BCrypt library found. Add jbcrypt, spring-security-crypto, or at.favre.lib:bcrypt to your dependencies.");
        }
    }
}
