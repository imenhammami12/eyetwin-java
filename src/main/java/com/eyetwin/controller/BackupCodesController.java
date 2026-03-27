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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BackupCodesController
 *
 * Mirrors: Symfony TwoFactorController::backupCodes()
 *          Template: two_factor/backup_codes.html.twig
 *
 * Features (identical to Symfony version):
 *  - Display backup codes list
 *  - Copy single code to clipboard
 *  - Copy all codes to clipboard
 *  - Download codes as .txt file
 *  - Print codes (opens save dialog)
 *  - Success banner when is_new = true
 */
public class BackupCodesController {

    // ── Success banner (shown when is_new = true) ──
    @FXML private HBox    successBanner;

    // ── Warning card ──
    @FXML private Label  warningLabel;

    // ── Codes container ──
    @FXML private VBox   codesContainer;
    @FXML private Label  codesCountLabel;

    // ── Action buttons ──
    @FXML private Button copyAllBtn;
    @FXML private Button downloadBtn;

    // ── Navigation buttons ──
    @FXML private Button savedBtn;     // "I've Saved My Codes" → settings
    @FXML private Button dashboardBtn; // "Go to Dashboard" → home

    // ── Toast / flash area ──
    @FXML private Label  toastLabel;
    @FXML private VBox   toastBox;

    private boolean                  isNew = false;
    private List<String>             backupCodes;
    private TwoFactorAuthService     twoFactorService;

    // Called by TwoFactorSettingsController / TwoFactorEnableController
    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        twoFactorService = new TwoFactorAuthService(new UserDAO());

        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        if (!twoFactorService.isTwoFactorEnabled(user)) {
            navigateTo("TwoFactor.fxml");
            return;
        }

        backupCodes = user.getBackupCodes();
        if (backupCodes == null || backupCodes.isEmpty()) {
            navigateTo("TwoFactor.fxml");
            return;
        }

        renderCodes(user);
    }

    // ─────────────────────────────────────────────────────────
    //  RENDER
    // ─────────────────────────────────────────────────────────

    private void renderCodes(User user) {
        // Success banner
        if (isNew) show(successBanner);
        else        hide(successBanner);

        // Count label
        if (codesCountLabel != null)
            codesCountLabel.setText("Your Backup Codes (" + backupCodes.size() + ")");

        // Build code rows — mirrors {% for code in backup_codes %}
        if (codesContainer != null) {
            codesContainer.getChildren().clear();
            for (String code : backupCodes) {
                codesContainer.getChildren().add(buildCodeRow(code));
            }
        }
    }

    /**
     * Build one code box row — mirrors .code-box template
     * [  XXXXX-XXXXX  ] [Copy]
     */
    private HBox buildCodeRow(String code) {
        HBox row = new HBox(10);
        row.setStyle(
            "-fx-background-color: linear-gradient(135deg, #1a1f2e, #0b111f);" +
            "-fx-border-color: rgba(255,0,0,0.2); -fx-border-radius: 12; " +
            "-fx-background-radius: 12; -fx-padding: 12 15 12 15; " +
            "-fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
        );

        // Code display
        TextField tf = new TextField(code);
        tf.setEditable(false);
        tf.setStyle(
            "-fx-background-color: transparent; -fx-border-color: transparent; " +
            "-fx-text-fill: white; -fx-font-family: 'Courier New'; " +
            "-fx-font-size: 1.3em; -fx-letter-spacing: 0.15em; " +
            "-fx-font-weight: bold; -fx-alignment: CENTER; " +
            "-fx-pref-width: 300;"
        );

        // Copy button
        Button copyBtn = new Button("⎘");
        copyBtn.setStyle(
            "-fx-background-color: linear-gradient(135deg, #ff0000, #c6019a); " +
            "-fx-text-fill: white; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 13;"
        );
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            Clipboard.getSystemClipboard().setContent(content);
            copyBtn.setText("✓");
            copyBtn.setStyle(
                "-fx-background-color: linear-gradient(135deg, #4cd3e3, #00bcd4); " +
                "-fx-text-fill: white; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 13;"
            );
            // Reset after 2s
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    copyBtn.setText("⎘");
                    copyBtn.setStyle(
                        "-fx-background-color: linear-gradient(135deg, #ff0000, #c6019a); " +
                        "-fx-text-fill: white; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-padding: 8 14; -fx-cursor: hand; -fx-font-size: 13;"
                    );
                });
            }).start();
        });

        HBox.setHgrow(tf, Priority.ALWAYS);
        row.getChildren().addAll(tf, copyBtn);

        // Hover effect
        row.setOnMouseEntered(e -> row.setStyle(row.getStyle()
            .replace("rgba(255,0,0,0.2)", "#ff0000")
            + "-fx-effect: dropshadow(gaussian, rgba(255,0,0,0.3), 20, 0, 5, 0);"
        ));
        row.setOnMouseExited(e -> row.setStyle(
            "-fx-background-color: linear-gradient(135deg, #1a1f2e, #0b111f);" +
            "-fx-border-color: rgba(255,0,0,0.2); -fx-border-radius: 12; " +
            "-fx-background-radius: 12; -fx-padding: 12 15 12 15; " +
            "-fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
        ));

        return row;
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS — mirrors Symfony JS functions
    // ─────────────────────────────────────────────────────────

    /** Copy all codes — mirrors copyAllCodes() */
    @FXML
    public void handleCopyAll() {
        if (backupCodes == null) return;
        String allCodes = String.join("\n", backupCodes);
        ClipboardContent content = new ClipboardContent();
        content.putString(allCodes);
        Clipboard.getSystemClipboard().setContent(content);
        showToast("All backup codes copied to clipboard!", false);
    }

    /** Download codes as .txt — mirrors downloadCodes() */
    @FXML
    public void handleDownload() {
        if (backupCodes == null) return;

        User   user = SessionManager.getCurrentUser();
        String email = user != null ? user.getEmail() : "user";
        String date  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Build file content — identical to Symfony downloadCodes()
        StringBuilder sb = new StringBuilder();
        sb.append("===================================================\n");
        sb.append("   TWO-FACTOR AUTHENTICATION BACKUP CODES\n");
        sb.append("===================================================\n\n");
        sb.append("Account: ").append(email).append("\n");
        sb.append("Generated: ").append(LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        sb.append("⚠️  CRITICAL SECURITY NOTICE  ⚠️\n");
        sb.append("Keep these codes secure and confidential!\n");
        sb.append("Treat them like passwords!\n\n");
        sb.append("---------------------------------------------------\n");
        sb.append("YOUR BACKUP CODES:\n");
        sb.append("---------------------------------------------------\n\n");

        for (int i = 0; i < backupCodes.size(); i++) {
            sb.append((i + 1)).append(". ").append(backupCodes.get(i)).append("\n");
        }

        sb.append("\n---------------------------------------------------\n");
        sb.append("IMPORTANT INFORMATION:\n");
        sb.append("---------------------------------------------------\n\n");
        sb.append("• Each code can only be used once\n");
        sb.append("• Use these codes if you lose access to your authenticator app\n");
        sb.append("• Store this file in a secure, encrypted location\n");
        sb.append("• Never share these codes with anyone\n");
        sb.append("• These codes are as sensitive as your password\n\n");
        sb.append("===================================================\n");
        sb.append("           Keep this document safe!\n");
        sb.append("===================================================\n");

        // Save to Downloads folder
        String fileName = "eyetwin-2fa-backup-codes-" + date + ".txt";
        String home     = System.getProperty("user.home");
        File   file     = new File(home + "/Downloads/" + fileName);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
            showToast("Backup codes downloaded: " + file.getAbsolutePath(), false);
        } catch (IOException e) {
            showToast("Failed to download codes: " + e.getMessage(), true);
        }
    }

    /** "I've Saved My Codes" → settings */
    @FXML
    public void handleSaved() {
        navigateTo("TwoFactor.fxml");
    }

    /** "Go to Dashboard" → home */
    @FXML
    public void handleDashboard() {
        navigateTo("home.fxml");
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    private void showToast(String msg, boolean isError) {
        if (toastLabel == null || toastBox == null) return;
        toastLabel.setText(msg);
        toastBox.setStyle(isError
            ? "-fx-background-color: linear-gradient(135deg, #f44a40, #ff0000); " +
              "-fx-padding: 18 28; -fx-background-radius: 12; " +
              "-fx-effect: dropshadow(gaussian, rgba(244,74,64,0.5), 30, 0, 0, 0);"
            : "-fx-background-color: linear-gradient(135deg, #4cd3e3, #00bcd4); " +
              "-fx-padding: 18 28; -fx-background-radius: 12; " +
              "-fx-effect: dropshadow(gaussian, rgba(76,211,227,0.5), 30, 0, 0, 0);"
        );
        show(toastBox);
        // Auto-hide after 3s
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> hide(toastBox));
        }).start();
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
            System.err.println("[BackupCodes] Nav error: " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        if (codesContainer != null && codesContainer.getScene() != null)
            return (Stage) codesContainer.getScene().getWindow();
        if (copyAllBtn != null && copyAllBtn.getScene() != null)
            return (Stage) copyAllBtn.getScene().getWindow();
        return null;
    }

    private void show(javafx.scene.Node n) { if (n != null) { n.setVisible(true);  n.setManaged(true);  } }
    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }
}
