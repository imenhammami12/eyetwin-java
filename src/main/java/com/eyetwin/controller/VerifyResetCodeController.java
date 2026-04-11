package com.eyetwin.controller;

import com.eyetwin.services.PasswordResetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * VerifyResetCodeController
 *
 * Mirrors Symfony's PasswordResetController::verifyCode()
 * and verify_reset_code.html.twig
 *
 * Receives: email (display only), channel (display hint)
 * Accepts the token pasted by the user, validates it,
 * then redirects to ResetPasswordController.
 */
public class VerifyResetCodeController {

    @FXML private Label        emailDisplayLabel;
    @FXML private Label        channelHintLabel;
    @FXML private TextField    codeField;
    @FXML private Label        errorLabel;
    @FXML private Label        countdownLabel;
    @FXML private Button       verifyButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final PasswordResetService resetService = new PasswordResetService();

    private String email;
    private String channel;

    // Countdown — mirrors 10:00 JS timer in Twig
    private int countdownSeconds = 10 * 60;
    private Thread countdownThread;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Called by ForgotPasswordController before the scene is shown.
     * Mirrors the ?email= query param pattern from Symfony redirect.
     */
    public void initData(String email, String channel) {
        this.email   = email;
        this.channel = channel;

        if (emailDisplayLabel != null) emailDisplayLabel.setText(email);
        if (channelHintLabel  != null) channelHintLabel.setText(getChannelHint(channel));

        startCountdown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (errorLabel     != null) errorLabel.setText("");
        if (loadingSpinner != null) loadingSpinner.setVisible(false);
        if (countdownLabel != null) countdownLabel.setText("10:00");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY — mirrors verifyCode() POST handler
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleVerify() {
        if (errorLabel != null) errorLabel.setText("");

        String code = codeField != null ? codeField.getText().trim() : "";
        if (code.isEmpty()) {
            setError("Please paste your reset code.");
            return;
        }

        setLoading(true);

        Thread worker = new Thread(() -> {
            boolean valid = resetService.verifyToken(code);

            Platform.runLater(() -> {
                setLoading(false);
                if (valid) {
                    stopCountdown();
                    navigateToReset(code);
                } else {
                    setError("Invalid or expired code. Please request a new one.");
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────
    private void navigateToReset(String token) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/eyetwin/views/ResetPassword.fxml")
            );
            Parent root = loader.load();

            ResetPasswordController ctrl = loader.getController();
            ctrl.initData(token);

            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));

        } catch (Exception e) {
            setError("Navigation error: " + e.getMessage());
            System.err.println("[VerifyCode] navigate error: " + e.getMessage());
        }
    }

    @FXML
    public void goBackToForgot() {
        stopCountdown();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/eyetwin/views/forgot-password.fxml")
            );
            Parent root = loader.load();
            Stage stage  = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (Exception e) {
            System.err.println("[VerifyCode] back error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COUNTDOWN — mirrors Twig JS timer
    // ─────────────────────────────────────────────────────────────────────────
    private void startCountdown() {
        countdownThread = new Thread(() -> {
            while (countdownSeconds > 0) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                countdownSeconds--;
                final int s = countdownSeconds;
                Platform.runLater(() -> {
                    if (countdownLabel != null) {
                        int mm = s / 60, ss = s % 60;
                        String txt = String.format("%02d:%02d", mm, ss);
                        countdownLabel.setText(txt);
                        // Turn red in last 2 minutes
                        if (s < 120) {
                            countdownLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        } else {
                            countdownLabel.setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                        }
                    }
                });
            }
            Platform.runLater(() -> {
                if (countdownLabel != null) {
                    countdownLabel.setText("00:00");
                    countdownLabel.setStyle("-fx-text-fill: #f85149;");
                }
                setError("Code expired. Please request a new one.");
            });
        });
        countdownThread.setDaemon(true);
        countdownThread.start();
    }

    private void stopCountdown() {
        if (countdownThread != null) countdownThread.interrupt();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private String getChannelHint(String channel) {
        return switch (channel) {
            case "sms"      -> "Check your SMS messages";
            case "telegram" -> "Check your Telegram app";
            default         -> "Check your email inbox";
        };
    }

    private void setLoading(boolean on) {
        if (verifyButton   != null) verifyButton.setDisable(on);
        if (loadingSpinner != null) loadingSpinner.setVisible(on);
    }

    private void setError(String msg) {
        if (errorLabel != null) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    }
}
