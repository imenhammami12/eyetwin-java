package com.eyetwin.controller;

import com.eyetwin.MainApp;
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
 * Flux :
 *  1. Affiché après username/password OK si 2FA activée ET appareil non trusted
 *  2. User entre code TOTP (6 chiffres, auto-submit) ou backup code
 *  3. Success → completeTwoFactorLogin(user, trustDevice) → home
 *  4. Failure → message d'erreur + champ vidé
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

    // L'utilisateur qui a passé username/password mais pas encore 2FA
    private User pendingUser;

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        twoFactorService = new TwoFactorAuthService(new UserDAO());
        pendingUser      = SessionManager.getPending2FAUser();

        // Sécurité : si pas d'utilisateur en attente → retour login
        if (pendingUser == null) {
            System.err.println("[2FA Verify] Aucun utilisateur en attente → retour login");
            navigateTo("login.fxml");
            return;
        }

        System.out.println("[2FA Verify] En attente pour : " + pendingUser.getEmail());

        hide(errorBox);
        hide(backupCodeSection);
        setupTotpField();

        if (totpCodeField != null) totpCodeField.requestFocus();
    }

    // ─────────────────────────────────────────────────────────
    //  TOTP CODE VERIFICATION
    // ─────────────────────────────────────────────────────────

    /**
     * Vérifier le code TOTP à 6 chiffres.
     * Appelé manuellement via le bouton OU automatiquement après 6 chiffres.
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
     * Vérifier un backup code (format XXXXX-XXXXX).
     */
    @FXML
    public void handleVerifyBackup() {
        String code = backupCodeField != null
                ? backupCodeField.getText().trim().toUpperCase()
                : "";
        if (code.isEmpty()) {
            showError("Please enter a backup code.");
            return;
        }
        performVerification(code, true);
    }

    /**
     * Afficher/masquer la section backup code.
     */
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

    /**
     * Annuler → déconnexion et retour au login.
     */
    @FXML
    public void handleCancel() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }

    // ─────────────────────────────────────────────────────────
    //  LOGIQUE DE VÉRIFICATION CENTRALE
    // ─────────────────────────────────────────────────────────

    private void performVerification(String code, boolean isBackupCode) {
        // Désactiver les boutons pendant la vérification
        setButtonsDisabled(true);

        new Thread(() -> {
            boolean success;
            try {
                if (isBackupCode) {
                    success = twoFactorService.verifyBackupCode(pendingUser, code);
                } else {
                    success = twoFactorService.verifyTotpCode(pendingUser.getTotpSecret(), code);
                }
            } catch (Exception e) {
                System.err.println("[2FA Verify] Erreur vérification : " + e.getMessage());
                success = false;
            }

            final boolean ok = success;
            Platform.runLater(() -> {
                setButtonsDisabled(false);

                if (ok) {
                    // ✅ Récupérer l'état du checkbox trusted device
                    boolean trustDevice = trustedDeviceCheck != null
                            && trustedDeviceCheck.isSelected();

                    System.out.println("[2FA Verify] ✅ Code valide — trustDevice=" + trustDevice);

                    // Compléter la connexion (enregistre l'appareil si trusted)
                    SessionManager.completeTwoFactorLogin(pendingUser, trustDevice);

                    // Navigation vers home ou dashboard
                    navigateAfterLogin(pendingUser);

                } else {
                    // ❌ Code invalide
                    String msg = isBackupCode
                            ? "Invalid backup code. Each code can only be used once."
                            : "Invalid authentication code. Please try again.";
                    showError(msg);

                    // Vider les champs
                    if (totpCodeField   != null && !isBackupCode) totpCodeField.clear();
                    if (backupCodeField != null &&  isBackupCode) backupCodeField.clear();
                    if (totpCodeField   != null && !isBackupCode) totpCodeField.requestFocus();
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  NAVIGATION APRÈS LOGIN
    // ─────────────────────────────────────────────────────────

    private void navigateAfterLogin(User user) {
        try {
            String fxml = user.isAdmin() ? "dashboard.fxml" : "home.fxml";
            MainApp.navigateTo("/com/eyetwin/views/" + fxml,
                    user.isAdmin() ? "Dashboard" : "Home");
        } catch (Exception e) {
            // Fallback si MainApp.navigateTo non disponible
            navigateTo("home.fxml");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Auto-submit quand 6 chiffres sont entrés (comme le JS Symfony).
     * Avec délai de 300ms pour laisser l'utilisateur voir ce qu'il a tapé.
     */
    private void setupTotpField() {
        if (totpCodeField == null) return;
        totpCodeField.textProperty().addListener((obs, old, nw) -> {
            // Filtrer les non-chiffres
            String digits = nw.replaceAll("\\D", "");
            if (digits.length() > 6) digits = digits.substring(0, 6);
            if (!nw.equals(digits)) {
                totpCodeField.setText(digits);
                return;
            }
            // Auto-submit sur 6 chiffres
            if (digits.length() == 6) {
                String finalDigits = digits;
                new Thread(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> performVerification(finalDigits, false));
                }).start();
            }
        });
    }

    private void setButtonsDisabled(boolean disabled) {
        if (verifyBtn != null) {
            verifyBtn.setDisable(disabled);
            verifyBtn.setText(disabled ? "Verifying…" : "✓  Verify");
        }
        if (verifyBackupBtn != null) {
            verifyBackupBtn.setDisable(disabled);
            verifyBackupBtn.setText(disabled ? "Verifying…" : "Verify Backup Code");
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText("⚠  " + msg);
        if (errorBox   != null) show(errorBox);
    }

    private void navigateTo(String fxml) {
        try {
            var url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) url = getClass().getResource("/com/eyetwin/view/" + fxml);
            if (url == null) {
                System.err.println("[2FA Verify] FXML introuvable : " + fxml);
                return;
            }
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[2FA Verify] Erreur navigation : " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        javafx.scene.Node[] nodes = { totpCodeField, verifyBtn, cancelBtn };
        for (javafx.scene.Node n : nodes) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    private void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true);  }
    }

    private void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}