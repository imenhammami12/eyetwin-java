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
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TwoFactorEnableController
 *
 * Mirrors: Symfony TwoFactorController::enable() + verify()
 *          Templates: two_factor/enable.html.twig
 *
 * Steps:
 *   1. Generate TOTP secret → show QR code
 *   2. User scans with authenticator app
 *   3. User enters 6-digit code → verify
 *   4. On success → navigate to BackupCodes with is_new=true
 */
public class TwoFactorEnableController {

    // ── Step indicator labels ──
    @FXML private Label step1Label;
    @FXML private Label step2Label;
    @FXML private Label step3Label;

    // ── QR Code section ──
    @FXML private ImageView qrCodeImage;
    @FXML private Label     secretCodeLabel;
    @FXML private Button    copySecretBtn;
    @FXML private Button    refreshQrBtn;
    @FXML private Label     qrErrorLabel;

    // ── Verification form ──
    @FXML private TextField codeField;
    @FXML private Button    verifyBtn;
    @FXML private Button    cancelBtn;

    // ── Flash message ──
    @FXML private Label errorLabel;
    @FXML private VBox  errorBox;

    private TwoFactorAuthService twoFactorService;
    private String               currentSecret;

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        twoFactorService = new TwoFactorAuthService(new UserDAO());
        setupCodeField();

        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        // If already enabled → redirect (mirrors Symfony redirect)
        if (twoFactorService.isTwoFactorEnabled(user)) {
            navigateTo("TwoFactor.fxml");
            return;
        }

        // Generate secret and show QR
        currentSecret = twoFactorService.prepareTwoFactorAuth(user);
        SessionManager.refresh();
        displayQrCode(user);
    }

    // ─────────────────────────────────────────────────────────
    //  QR CODE DISPLAY  — mirrors app_2fa_qr_code route
    // ─────────────────────────────────────────────────────────

    private void displayQrCode(User user) {
        try {
            String otpUri    = twoFactorService.getQrCodeContent(user);
            String qrApiUrl  = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data="
                              + URLEncoder.encode(otpUri, StandardCharsets.UTF_8);

            if (secretCodeLabel != null)
                secretCodeLabel.setText(user.getTotpSecret());

            // Load QR image from free QR API (no local library needed)
            loadQrImageAsync(qrApiUrl);

        } catch (Exception e) {
            showQrError("Could not generate QR code: " + e.getMessage());
        }
    }

    private void loadQrImageAsync(String url) {
        new Thread(() -> {
            try {
                Image img = new Image(url, 250, 250, true, true, true);
                Platform.runLater(() -> {
                    if (img.isError()) {
                        showQrError("QR Code could not be loaded. Use the secret key below.");
                    } else {
                        if (qrCodeImage != null) qrCodeImage.setImage(img);
                        hide(qrErrorLabel);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showQrError("QR Code network error."));
            }
        }).start();
    }

    private void showQrError(String msg) {
        if (qrErrorLabel != null) {
            qrErrorLabel.setText("⚠ " + msg);
            show(qrErrorLabel);
        }
        if (qrCodeImage != null) qrCodeImage.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS
    // ─────────────────────────────────────────────────────────

    /** Copy secret key to clipboard — mirrors JS copySecret() */
    @FXML
    public void handleCopySecret() {
        if (currentSecret == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(currentSecret);
        Clipboard.getSystemClipboard().setContent(content);

        if (copySecretBtn != null) {
            copySecretBtn.setText("✓ Copied!");
            copySecretBtn.setStyle(copySecretBtn.getStyle() +
                "-fx-background-color: linear-gradient(135deg, #4cd3e3, #00bcd4);");

            // Reset after 2 sec
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    copySecretBtn.setText("⎘  Copy Secret Key");
                    copySecretBtn.setStyle(copySecretBtn.getStyle()
                        .replace("-fx-background-color: linear-gradient(135deg, #4cd3e3, #00bcd4);", ""));
                });
            }).start();
        }
    }

    /** Refresh QR code — mirrors JS refreshQR() */
    @FXML
    public void handleRefreshQr() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        if (refreshQrBtn != null) {
            refreshQrBtn.setText("⟳ Refreshing...");
            refreshQrBtn.setDisable(true);
        }
        displayQrCode(user);
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                if (refreshQrBtn != null) {
                    refreshQrBtn.setText("⟳ Refresh QR Code");
                    refreshQrBtn.setDisable(false);
                }
            });
        }).start();
    }

    /**
     * Verify code and enable 2FA.
     * Mirrors: POST app_2fa_verify → TwoFactorController::verify()
     */
    @FXML
    public void handleVerify() {
        if (codeField == null) return;
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showError("Please enter a verification code.");
            return;
        }
        if (!code.matches("\\d{6}")) {
            showError("Invalid verification code. Please try again.");
            return;
        }

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        verifyBtn.setDisable(true);
        verifyBtn.setText("Verifying...");

        new Thread(() -> {
            boolean ok = twoFactorService.verifyAndEnableTwoFactorAuth(user, code);
            Platform.runLater(() -> {
                verifyBtn.setDisable(false);
                verifyBtn.setText("✓  Verify and Enable 2FA");

                if (ok) {
                    SessionManager.refresh();
                    // Navigate to backup codes with is_new=true
                    navigateToBackupCodesNew();
                } else {
                    showError("Invalid verification code. Please try again.");
                    codeField.clear();
                    codeField.requestFocus();
                }
            });
        }).start();
    }

    /** Cancel → settings */
    @FXML
    public void handleCancel() {
        navigateTo("TwoFactor.fxml");
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    private void setupCodeField() {
        if (codeField == null) return;
        // Only allow digits, max 6 chars
        codeField.textProperty().addListener((obs, old, nw) -> {
            String digits = nw.replaceAll("\\D", "");
            if (digits.length() > 6) digits = digits.substring(0, 6);
            if (!nw.equals(digits)) codeField.setText(digits);
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
        if (errorBox  != null) show(errorBox);
    }

    private void navigateToBackupCodesNew() {
        try {
            var url = getClass().getResource("/com/eyetwin/views/BackupCodes.fxml");
            if (url == null) url = getClass().getResource("/com/eyetwin/view/BackupCodes.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof BackupCodesController bcc) bcc.setIsNew(true);
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[2FA Enable] Nav error: " + e.getMessage());
        }
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
            System.err.println("[2FA Enable] Nav error: " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{ codeField, verifyBtn, qrCodeImage }) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    private void show(javafx.scene.Node n) { if (n != null) { n.setVisible(true);  n.setManaged(true);  } }
    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }
}
