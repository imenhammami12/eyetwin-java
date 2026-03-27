package com.eyetwin.controller;

import com.eyetwin.model.User;
import com.eyetwin.service.TwoFactorAuthService;
import com.eyetwin.util.SessionManager;
import com.eyetwin.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * TwoFactorSettingsController
 *
 * Mirrors: Symfony TwoFactorController::settings()
 *          Template: two_factor/settings.html.twig
 *
 * Renders either:
 *  - "2FA Enabled"  panel (backup codes count, disable button)
 *  - "2FA Disabled" panel (benefits list, enable button)
 */
public class TwoFactorSettingsController {

    // ── Status Banner ──
    @FXML private VBox   enabledBanner;
    @FXML private VBox   disabledBanner;

    // ── Enabled panel ──
    @FXML private Label  backupCodesCountLabel;
    @FXML private Button viewBackupCodesBtn;
    @FXML private Button regenerateCodesBtn;
    @FXML private Button disableBtn;

    // ── Disabled panel ──
    @FXML private Button enableBtn;

    // ── Flash ──
    @FXML private Label  flashMessage;
    @FXML private VBox   flashBox;

    // ── Back button ──
    @FXML private Button backBtn;

    private TwoFactorAuthService twoFactorService;

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        twoFactorService = new TwoFactorAuthService(new UserDAO());
        loadSettings();
    }

    /** Load and render the current 2FA state — mirrors settings() action */
    private void loadSettings() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        boolean isEnabled     = twoFactorService.isTwoFactorEnabled(user);
        int     remainingCodes = twoFactorService.getRemainingBackupCodesCount(user);

        if (isEnabled) {
            show(enabledBanner);
            hide(disabledBanner);
            if (backupCodesCountLabel != null)
                backupCodesCountLabel.setText(remainingCodes + " code(s) remaining");
        } else {
            hide(enabledBanner);
            show(disabledBanner);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS — mirrors Symfony route handlers
    // ─────────────────────────────────────────────────────────

    /** → app_2fa_enable */
    @FXML
    public void handleEnable() {
        navigateTo("TwoFactorEnable.fxml");
    }

    /** → app_2fa_backup_codes */
    @FXML
    public void handleViewBackupCodes() {
        navigateTo("BackupCodes.fxml");
    }

    /** → app_2fa_regenerate_codes (POST equivalent) */
    @FXML
    public void handleRegenerate() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Regenerate Backup Codes");
        confirm.setHeaderText("⚠️ This will invalidate all current backup codes.");
        confirm.setContentText("New codes will be generated.\n\nContinue?");
        confirm.getDialogPane().setStyle(
            "-fx-background-color: #1a1f2e; -fx-font-size: 13px;"
        );

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                User user = SessionManager.getCurrentUser();
                twoFactorService.regenerateBackupCodes(user);
                SessionManager.refresh(); // reload user from DB
                showFlash("Backup codes have been regenerated. Your old codes are no longer valid.", false);
                // Navigate to backup codes page with new codes
                navigateToBackupCodesNew();
            }
        });
    }

    /** → app_2fa_disable (POST with password confirmation) */
    @FXML
    public void handleDisable() {
        // Show password confirmation dialog — mirrors Symfony modal
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Disable Two-Factor Authentication");
        dialog.setHeaderText("⚠️ Security Warning\n\nDisabling 2FA will significantly reduce your account security.");

        ButtonType disableType = new ButtonType("Disable 2FA", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(disableType, ButtonType.CANCEL);

        PasswordField pw = new PasswordField();
        pw.setPromptText("Enter your account password");
        pw.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; " +
                    "-fx-border-color: rgba(255,0,0,0.3); -fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 10 12; -fx-font-size: 13;");

        VBox content = new VBox(10,
            new Label("🔒 Confirm your password:"),
            pw
        );
        content.setStyle("-fx-padding: 20;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1f2e;");

        dialog.setResultConverter(btn -> btn == disableType ? pw.getText() : null);

        dialog.showAndWait().ifPresent(password -> {
            if (password == null || password.isBlank()) return;

            User user = SessionManager.getCurrentUser();
            // Verify password before disabling
            if (verifyUserPassword(user, password)) {
                twoFactorService.disableTwoFactorAuth(user);
                SessionManager.refresh();
                showFlash("Two-factor authentication has been disabled.", false);
                loadSettings(); // re-render
            } else {
                showFlash("Incorrect password. 2FA was NOT disabled.", true);
            }
        });
    }

    /** Back to profile */
    @FXML
    public void handleBack() {
        navigateTo("home.fxml"); // ✅ home
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    private boolean verifyUserPassword(User user, String password) {
        // Use your existing password verification — BCrypt check
        // Example using Spring Security BCrypt or jBCrypt:
        //   return BCrypt.checkpw(password, user.getPassword());
        // For now: delegate to UserDAO
        try {
            UserDAO dao = new UserDAO();
            return dao.verifyPassword(user.getEmail(), password);
        } catch (Exception e) {
            System.err.println("[2FA] Password verify error: " + e.getMessage());
            return false;
        }
    }

    private void showFlash(String message, boolean isError) {
        if (flashMessage == null || flashBox == null) return;
        flashMessage.setText(message);
        flashBox.setStyle(isError
            ? "-fx-background-color: rgba(244,74,64,0.2); -fx-border-color: #f44a40; " +
              "-fx-border-width: 0 0 0 5; -fx-border-radius: 10; -fx-padding: 15 20;"
            : "-fx-background-color: rgba(76,211,227,0.2); -fx-border-color: #4cd3e3; " +
              "-fx-border-width: 0 0 0 5; -fx-border-radius: 10; -fx-padding: 15 20;"
        );
        flashMessage.setStyle(isError ? "-fx-text-fill: #f44a40;" : "-fx-text-fill: #4cd3e3;");
        show(flashBox);
    }

    private void navigateToBackupCodesNew() {
        try {
            var url = getClass().getResource("/com/eyetwin/views/BackupCodes.fxml");
            if (url == null) url = getClass().getResource("/com/eyetwin/view/BackupCodes.fxml");
            if (url == null) return;
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            // Pass "isNew = true" to BackupCodesController
            Object ctrl = loader.getController();
            if (ctrl instanceof BackupCodesController bcc) {
                bcc.setIsNew(true);
            }
            Stage stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[2FA Settings] Nav error: " + e.getMessage());
        }
    }

    private void navigateTo(String fxml) {
        try {
            var url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) url = getClass().getResource("/com/eyetwin/view/" + fxml);
            if (url == null) { System.err.println("FXML not found: " + fxml); return; }
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[2FA Settings] Nav error: " + e.getMessage());
        }
    }

    private void show(javafx.scene.Node n) { if (n != null) { n.setVisible(true);  n.setManaged(true);  } }
    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }

    private Stage resolveStage() {
        if (enabledBanner != null && enabledBanner.getScene() != null)
            return (Stage) enabledBanner.getScene().getWindow();
        if (disabledBanner != null && disabledBanner.getScene() != null)
            return (Stage) disabledBanner.getScene().getWindow();
        return null;
    }
}
