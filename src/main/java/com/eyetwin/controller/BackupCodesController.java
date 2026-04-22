package com.eyetwin.controller;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ITwoFactorService;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.TwoFactorServiceImpl;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
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

public class BackupCodesController {

    // ── Success banner ──
    @FXML private HBox   successBanner;

    // ── Codes container ──
    @FXML private VBox   codesContainer;
    @FXML private Label  codesCountLabel;

    // ── Action buttons ──
    @FXML private Button copyAllBtn;
    @FXML private Button downloadBtn;

    // ── Navigation buttons ──
    @FXML private Button savedBtn;
    @FXML private Button dashboardBtn;

    // ── Toast ──
    @FXML private Label  toastLabel;
    @FXML private VBox   toastBox;

    private boolean          isNew = false;
    private List<String>     backupCodes;

    // ── Services (interfaces, pas de DAO direct) ──
    private ITwoFactorService twoFactorService;

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        IUserService userService  = new UserServiceImpl();
        twoFactorService          = new TwoFactorServiceImpl(userService);

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

        renderCodes();
    }

    // ─────────────────────────────────────────────────────────
    //  RENDER
    // ─────────────────────────────────────────────────────────

    private void renderCodes() {
        if (isNew) show(successBanner);
        else        hide(successBanner);

        if (codesCountLabel != null)
            codesCountLabel.setText("Your Backup Codes (" + backupCodes.size() + ")");

        if (codesContainer != null) {
            codesContainer.getChildren().clear();
            for (String code : backupCodes)
                codesContainer.getChildren().add(buildCodeRow(code));
        }
    }

    private HBox buildCodeRow(String code) {
        HBox row = new HBox(10);
        row.setStyle(
                "-fx-background-color: linear-gradient(135deg, #1a1f2e, #0b111f);" +
                        "-fx-border-color: rgba(255,0,0,0.2); -fx-border-radius: 12; " +
                        "-fx-background-radius: 12; -fx-padding: 12 15 12 15; " +
                        "-fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
        );

        TextField tf = new TextField(code);
        tf.setEditable(false);
        tf.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; " +
                        "-fx-text-fill: white; -fx-font-family: 'Courier New'; " +
                        "-fx-font-size: 1.3em; -fx-font-weight: bold; " +
                        "-fx-alignment: CENTER; -fx-pref-width: 300;"
        );

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
    //  ACTIONS
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleCopyAll() {
        if (backupCodes == null) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(String.join("\n", backupCodes));
        Clipboard.getSystemClipboard().setContent(content);
        showToast("All backup codes copied to clipboard!", false);
    }

    @FXML
    public void handleDownload() {
        if (backupCodes == null) return;

        User   user  = SessionManager.getCurrentUser();
        String email = user != null ? user.getEmail() : "user";
        String date  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

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

        for (int i = 0; i < backupCodes.size(); i++)
            sb.append((i + 1)).append(". ").append(backupCodes.get(i)).append("\n");

        sb.append("\n---------------------------------------------------\n");
        sb.append("IMPORTANT INFORMATION:\n");
        sb.append("---------------------------------------------------\n\n");
        sb.append("• Each code can only be used once\n");
        sb.append("• Use these codes if you lose access to your authenticator app\n");
        sb.append("• Store this file in a secure, encrypted location\n");
        sb.append("• Never share these codes with anyone\n\n");
        sb.append("===================================================\n");
        sb.append("           Keep this document safe!\n");
        sb.append("===================================================\n");

        String fileName = "eyetwin-2fa-backup-codes-" + date + ".txt";
        File   file     = new File(System.getProperty("user.home") + "/Downloads/" + fileName);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
            showToast("Backup codes downloaded: " + file.getAbsolutePath(), false);
        } catch (IOException e) {
            showToast("Failed to download codes: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleSaved() {
        navigateTo("TwoFactor.fxml");
    }

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
                "-fx-padding: 18 28; -fx-background-radius: 12;"
                : "-fx-background-color: linear-gradient(135deg, #4cd3e3, #00bcd4); " +
                "-fx-padding: 18 28; -fx-background-radius: 12;"
        );
        show(toastBox);
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