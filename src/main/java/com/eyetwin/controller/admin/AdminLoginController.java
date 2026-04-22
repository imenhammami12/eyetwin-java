package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * AdminLoginController — Traduction de AdminSecurityController.php (Symfony)
 *
 * Flux identique à Symfony :
 *  1. Si l'utilisateur connecté est ROLE_ADMIN → redirect vers AdminDashboard
 *  2. Si l'email saisi a une faceDescriptor → redirect vers AdminFaceVerify (bouton visible)
 *  3. Sinon → formulaire email/password classique
 */
public class AdminLoginController {

    // ── Formulaire ──
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      rememberMeCheck;
    @FXML private Button        loginBtn;
    @FXML private Button        faceLoginBtn;   // visible seulement si l'user a une face
    @FXML private Label         errorLabel;
    @FXML private VBox          faceSection;    // conteneur du bouton face (masqué par défaut)
    @FXML private Label         checkingLabel;  // spinner "Checking…" pendant vérification

    private final IUserService userService = new UserServiceImpl();

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE  (miroir de login() Symfony)
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        // ── Miroir : if ($this->isGranted('ROLE_ADMIN')) redirect admin_dashboard ──
        if (SessionManager.isAdmin()) {
            navigateTo("Admin.fxml");   // → AdminDashboard
            return;
        }

        // Masquer la section face par défaut (comme has_face_user = false)
        hideFaceSection();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // ── Miroir du JS "checkEmail on blur/input" de admin/security/login.html.twig ──
        emailField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !emailField.getText().isBlank()) {
                checkEmailForFace(emailField.getText().trim());
            }
        });

        // ── Flash message de session (miroir addFlash Symfony) ──
        String[] flash = SessionManager.consumeFlash();
        if (flash != null) {
            showError(flash[1]);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  CHECK EMAIL — miroir de /admin/face-check + has_face_user
    // ════════════════════════════════════════════════════════════

    /**
     * Vérifie si l'email correspond à un admin ayant une faceDescriptor.
     * Miroir de l'endpoint POST /admin/face-check (AdminFaceController).
     * Et de la variable Twig {{ has_face_user }}.
     */
    private void checkEmailForFace(String email) {
        if (!email.contains("@")) return;

        showChecking(true);

        // Exécuté en thread pour ne pas bloquer l'UI (comme le fetch() JS)
        new Thread(() -> {
            User user = userService.findByEmail(email);
            javafx.application.Platform.runLater(() -> {
                showChecking(false);
                if (user != null
                        && user.getFaceDescriptor() != null
                        && !user.getFaceDescriptor().isBlank()) {
                    // ── Miroir : data.requiresFace → show face button ──
                    showFaceSection();
                } else {
                    hideFaceSection();
                }
            });
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    //  LOGIN CLASSIQUE — miroir du formulaire POST admin_login
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            showError("Please fill in all fields.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Logging in…");

        new Thread(() -> {
            User user = userService.findByEmail(email);

            javafx.application.Platform.runLater(() -> {
                loginBtn.setDisable(false);
                loginBtn.setText("Log in");

                // ── Miroir : vérification mot de passe + rôle ──
                if (user == null || !userService.verifyPassword(email, password)) {
                    showError("Invalid credentials.");
                    return;
                }

                if (!isAdmin(user)) {
                    showError("Access denied. Admin role required.");
                    return;
                }

                // ── Miroir : AccountStatusChecker (user_checker Symfony) ──
                if (user.getAccountStatus() != null
                        && !"ACTIVE".equalsIgnoreCase(user.getAccountStatus().toString())) {
                    showError("Your account is suspended or banned.");
                    return;
                }

                // ── 2FA ? (NB : admin firewall a NO 2FA dans security.yaml) ──
                // → Login direct, pas de 2FA pour les admins (comme dans security.yaml)
                SessionManager.setCurrentUser(user);
                navigateTo("Admin.fxml");   // → admin_dashboard
            });
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    //  FACE LOGIN — miroir du bouton "Log in with face recognition"
    //  → path('admin_face_verify')
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleFaceLogin() {
        // Pré-renseigne l'email pour que AdminFaceVerifyController sache quel user vérifier
        SessionManager.setPendingFaceEmail(emailField.getText().trim());
        navigateTo("AdminFaceVerify.fxml");   // → /admin/face-verify
    }

    // ════════════════════════════════════════════════════════════
    //  LOGOUT — miroir de admin_logout
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleLogout() {
        SessionManager.logout();
        // Reste sur la page login (déjà là)
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION — retour au site principal
    //  miroir du lien "Back to home" → path('app_home')
    // ════════════════════════════════════════════════════════════
    @FXML
    public void goToHome() {
        navigateTo("home.fxml");
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════════════════════

    private void showFaceSection() {
        if (faceSection != null) {
            faceSection.setVisible(true);
            faceSection.setManaged(true);
        }
    }

    private void hideFaceSection() {
        if (faceSection != null) {
            faceSection.setVisible(false);
            faceSection.setManaged(false);
        }
    }

    private void showChecking(boolean visible) {
        if (checkingLabel != null) {
            checkingLabel.setVisible(visible);
            checkingLabel.setManaged(visible);
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    // ── Vérifie si un User a ROLE_ADMIN ou ROLE_SUPER_ADMIN ──
    private boolean isAdmin(User user) {
        String roles = user.getRolesJson();
        return roles != null
                && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN"));
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION INTERNE
    // ════════════════════════════════════════════════════════════
    private void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        URL url = null;
        for (String path : paths) {
            url = getClass().getResource(path);
            if (url != null) break;
        }
        if (url == null) {
            System.err.println("[AdminLoginController] FXML introuvable : " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminLoginController] Erreur : " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                emailField, loginBtn, errorLabel
        }) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}
