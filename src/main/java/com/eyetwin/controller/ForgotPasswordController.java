package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.services.PasswordResetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * ForgotPasswordController
 *
 * Mirrors Symfony's PasswordResetController::request()
 * and forgot_password.html.twig
 *
 * Flow:
 *   1. User enters email + selects channel (email / sms / telegram)
 *   2. Token is generated, persisted, sent
 *   3. Redirects to VerifyResetCode screen with email + channel
 */
public class ForgotPasswordController {

    @FXML private TextField    emailField;
    @FXML private ToggleGroup  channelGroup;
    @FXML private RadioButton  channelEmail;
    @FXML private RadioButton  channelSms;
    @FXML private RadioButton  channelTelegram;
    @FXML private Label        errorLabel;
    @FXML private Label        successLabel;
    @FXML private Button       submitButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final PasswordResetService resetService = new PasswordResetService();

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Default channel: email (mirrors Symfony form default)
        if (channelEmail != null) channelEmail.setSelected(true);
        if (errorLabel   != null) errorLabel.setText("");
        if (successLabel != null) successLabel.setText("");
        if (loadingSpinner != null) loadingSpinner.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBMIT — mirrors PasswordResetController::request()
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleSubmit() {
        clearMessages();

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            setError("Please enter your email address.");
            return;
        }
        if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
            setError("Please enter a valid email address.");
            return;
        }

        String channel = getSelectedChannel();

        setLoading(true);

        // Run in background to avoid freezing the UI
        Thread worker = new Thread(() -> {
            try {
                String token = resetService.requestPasswordReset(email, channel);

                Platform.runLater(() -> {
                    setLoading(false);
                    if (token == null) {
                        // Mirror Symfony: don't reveal if user exists
                        setSuccess("If an account exists with this email, you will receive a reset code.");
                    } else {
                        // Navigate to verify screen — mirrors redirectToRoute('app_verify_reset_code')
                        navigateToVerify(email, channel);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    setError("Error sending code: " + e.getMessage());
                    System.err.println("[ForgotPassword] " + e.getMessage());
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────
    private void navigateToVerify(String email, String channel) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/eyetwin/views/VerifyResetCode.fxml")
            );
            Parent root = loader.load();

            // Pass email to next controller — mirrors ?email= query param
            VerifyResetCodeController ctrl = loader.getController();
            ctrl.initData(email, channel);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));

        } catch (Exception e) {
            setError("Navigation error: " + e.getMessage());
            System.err.println("[ForgotPassword] navigate error: " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private String getSelectedChannel() {
        if (channelGroup == null) return "email";
        Toggle sel = channelGroup.getSelectedToggle();
        if (sel == channelSms)      return "sms";
        if (sel == channelTelegram) return "telegram";
        return "email";
    }

    private void setLoading(boolean on) {
        if (submitButton   != null) submitButton.setDisable(on);
        if (loadingSpinner != null) loadingSpinner.setVisible(on);
    }

    private void setError(String msg) {
        if (errorLabel   != null) { errorLabel.setText(msg);  errorLabel.setVisible(true); }
        if (successLabel != null) successLabel.setText("");
    }

    private void setSuccess(String msg) {
        if (successLabel != null) { successLabel.setText(msg); successLabel.setVisible(true); }
        if (errorLabel   != null) errorLabel.setText("");
    }

    private void clearMessages() {
        if (errorLabel   != null) errorLabel.setText("");
        if (successLabel != null) successLabel.setText("");
    }
}
