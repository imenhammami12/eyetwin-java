package com.eyetwin.controller;

import com.eyetwin.model.User;
import com.eyetwin.service.TwoFactorAuthService;
import com.eyetwin.util.SessionManager;
import com.eyetwin.dao.UserDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * TwoFactorVerifyController  (Login-time 2FA check)
 *
 * Mirrors: Symfony 2fa_login_check route (SchebTwoFactorBundle)
 *          Template: two_factor/2fa.html.twig
 *
 * Shown AFTER username/password login when 2FA is enabled.
 * User enters either:
 *  - 6-digit TOTP code  → auto-submits on 6th digit
 *  - XXXXX-XXXXX backup code
 *
 * On success → SessionManager.setAuthenticated(true) → home
 * On failure → show error message
 */
public class TwoFactorVerifyController {

    // ── TOTP code input ──
    @FXML private TextField totpCodeField;
    @FXML private Button    verifyBtn;

    // ── Backup code section (collapsed by default) ──
    @FXML private VBox      backupCodeSection;
    @FXML private TextField backupCodeField;
    @FXML private Button    verifyBackupBtn;
    @FXML private Button    showBackupBtn;

    // ── Trust device checkbox ──
    @FXML private CheckBox  trustedDeviceCheck;

    // ── Error / Flash ──
    @FXML private Label     errorLabel;
    @FXML private VBox      errorBox;

    // ── Cancel ──
    @FXML private Button    cancelBtn;

    private TwoFactorAuthService twoFactorService;

    // The user who passed username/password but hasn't completed 2FA yet
    // Set this BEFORE loading the FXML via SessionManager.getPending2FAUser()
    private User pendingUser;

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        twoFactorService = new TwoFactorAuthService(new UserDAO());
        pendingUser      = SessionManager.getPending2FAUser();

        if (pendingUser == null) {
            navigateTo("login.fxml");
            return;
        }

        hide(backupCodeSection);
        setupTotpField();
        if (totpCodeField != null) totpCodeField.requestFocus();
    }

    // ─────────────────────────────────────────────────────────
    //  TOTP CODE VERIFICATION
    // ─────────────────────────────────────────────────────────

    /**
     * Verify TOTP code.
     * Mirrors: POST 2fa_login_check with _auth_code (6 digits)
     */
    @FXML
    public void handleVerifyTotp() {
        String code = totpCodeField != null ? totpCodeField.getText().trim() : "";
        if (!code.matches("\\d{6}")) {
            showError("Please enter a valid 6-digit code.");
            return;
        }
        performVerification(code, false);
    }

    /**
     * Verify backup code.
     * Mirrors: POST 2fa_login_check with _auth_code (XXXXX-XXXXX backup)
     */
    @FXML
    public void handleVerifyBackup() {
        String code = backupCodeField != null ? backupCodeField.getText().trim().toUpperCase() : "";
        if (code.isEmpty()) {
            showError("Please enter a backup code.");
            return;
        }
        performVerification(code, true);
    }

    /** Toggle backup code section — mirrors Bootstrap collapse */
    @FXML
    public void handleShowBackupSection() {
        if (backupCodeSection == null) return;
        boolean visible = backupCodeSection.isVisible();
        if (visible) {
            hide(backupCodeSection);
            if (showBackupBtn != null) showBackupBtn.setText("Use Backup Code");
        } else {
            show(backupCodeSection);
            if (showBackupBtn != null) showBackupBtn.setText("Use Authenticator Instead");
            if (backupCodeField != null) backupCodeField.requestFocus();
        }
    }

    /** Cancel → logout (mirrors app_logout link) */
    @FXML
    public void handleCancel() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }

    // ─────────────────────────────────────────────────────────
    //  CORE VERIFICATION LOGIC
    // ─────────────────────────────────────────────────────────

    private void performVerification(String code, boolean isBackupCode) {
        if (verifyBtn != null)       { verifyBtn.setDisable(true);       verifyBtn.setText("Verifying..."); }
        if (verifyBackupBtn != null) { verifyBackupBtn.setDisable(true); verifyBackupBtn.setText("Verifying..."); }

        new Thread(() -> {
            boolean success;
            if (isBackupCode) {
                success = twoFactorService.verifyBackupCode(pendingUser, code);
            } else {
                success = twoFactorService.verifyTotpCode(pendingUser.getTotpSecret(), code);
            }

            final boolean ok = success;
            Platform.runLater(() -> {
                if (verifyBtn != null)       { verifyBtn.setDisable(false);       verifyBtn.setText("✓  Verify"); }
                if (verifyBackupBtn != null) { verifyBackupBtn.setDisable(false); verifyBackupBtn.setText("Verify Backup Code"); }

                if (ok) {
                    // Complete the login — set user as fully authenticated
                    SessionManager.completeTwoFactorLogin(pendingUser,
                        trustedDeviceCheck != null && trustedDeviceCheck.isSelected());
                    navigateTo("home.fxml");
                } else {
                    showError(isBackupCode
                        ? "Invalid backup code. Each code can only be used once."
                        : "Invalid authentication code. Please try again."
                    );
                    if (totpCodeField  != null) totpCodeField.clear();
                    if (backupCodeField != null) backupCodeField.clear();
                    if (totpCodeField  != null && !isBackupCode) totpCodeField.requestFocus();
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    /** Auto-submit when 6 digits entered — mirrors JS addEventListener('input') */
    private void setupTotpField() {
        if (totpCodeField == null) return;
        totpCodeField.textProperty().addListener((obs, old, nw) -> {
            // Strip non-digits
            String digits = nw.replaceAll("\\D", "");
            if (digits.length() > 6) digits = digits.substring(0, 6);
            if (!nw.equals(digits)) {
                totpCodeField.setText(digits);
                return;
            }
            // Auto-submit on 6 digits (with 300ms delay like Symfony template)
            if (digits.length() == 6) {
                String finalDigits = digits;
                new Thread(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> performVerification(finalDigits, false));
                }).start();
            }
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText("⚠ " + msg);
        if (errorBox   != null) show(errorBox);
    }

    private void navigateTo(String fxml) {
        try {
            var url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) url = getClass().getResource("/com/eyetwin/view/" + fxml);
            if (url == null) return;
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[2FA Verify] Nav error: " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{ totpCodeField, verifyBtn, cancelBtn }) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    private void show(javafx.scene.Node n) { if (n != null) { n.setVisible(true);  n.setManaged(true);  } }
    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }
}
