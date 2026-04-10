package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * RegisterController — miroir exact du RegistrationController Symfony.
 *
 * Validations identiques (dans le même ordre) :
 *   1. Username : obligatoire, min 3 chars, alphanumérique + underscore
 *   2. Email    : obligatoire, format valide
 *   3. Full name: obligatoire, min 2 chars
 *   4. Phone    : optionnel, format +?[0-9\s\-().]{7,20}
 *   5. Password : obligatoire, min 6 chars
 *   6. Password : doit contenir majuscule + minuscule + chiffre
 *   7. Confirm  : doit correspondre
 *   8. Terms    : doit être accepté
 *   9. Username unicité  (via service → DB)
 *  10. Email    unicité  (via service → DB)
 */
public class RegisterController {

    // ─────────────────────────────────────────────────────────
    //  FXML bindings
    // ─────────────────────────────────────────────────────────

    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox      agreeTermsCheckBox;

    // Feedback labels (inline sous chaque champ)
    @FXML private Label usernameFeedback;
    @FXML private Label emailFeedback;
    @FXML private Label fullNameFeedback;
    @FXML private Label phoneFeedback;
    @FXML private Label passwordFeedback;
    @FXML private Label confirmFeedback;
    @FXML private Label termsFeedback;

    // Barre de force du mot de passe
    @FXML private Rectangle strengthBar;
    @FXML private Label     strengthLabel;

    // Label d'erreur global (optionnel, laissé pour compatibilité)
    @FXML private Label errorLabel;

    // ─────────────────────────────────────────────────────────
    //  Service
    // ─────────────────────────────────────────────────────────

    private final IUserService userService = new UserServiceImpl();

    // ─────────────────────────────────────────────────────────
    //  FXML initialize — live validation listeners
    // ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        /* Live validation on focus-lost (comme registration-validation.js) */
        usernameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateUsername(false);
        });
        usernameField.textProperty().addListener((obs, o, n) -> clearFeedback(usernameFeedback));

        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateEmail(false);
        });
        emailField.textProperty().addListener((obs, o, n) -> clearFeedback(emailFeedback));

        fullNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateFullName(false);
        });
        fullNameField.textProperty().addListener((obs, o, n) -> clearFeedback(fullNameFeedback));

        phoneField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validatePhone(false);
        });
        phoneField.textProperty().addListener((obs, o, n) -> {
            clearFeedback(phoneFeedback);
            validatePhone(false); // phone validates live
        });

        passwordField.textProperty().addListener((obs, o, n) -> {
            clearFeedback(passwordFeedback);
            updateStrengthBar(n);
            // Re-validate confirm if already typed
            if (confirmPasswordField.getText() != null && !confirmPasswordField.getText().isEmpty()) {
                validateConfirm(false);
            }
        });
        passwordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validatePassword(false);
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> {
            clearFeedback(confirmFeedback);
        });
        confirmPasswordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateConfirm(false);
        });
    }

    // ═════════════════════════════════════════════════════════
    //  SUBMIT — handleRegister
    // ═════════════════════════════════════════════════════════

    @FXML
    public void handleRegister() {
        // Clear global error
        if (errorLabel != null) errorLabel.setText("");

        // Run ALL validations — stop at first failure (miroir Symfony)
        if (!validateUsername(true))  return;
        if (!validateEmail(true))     return;
        if (!validateFullName(true))  return;
        if (!validatePhone(true))     return;
        if (!validatePassword(true))  return;
        if (!validateConfirm(true))   return;
        if (!validateTerms())         return;

        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim().toLowerCase();
        String fullName = fullNameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText();

        // ── 9. Username uniqueness (DB) ──────────────────────
        if (userService.findByUsername(username) != null) {
            showError(usernameFeedback, "This username is already taken.");
            return;
        }

        // ── 10. Email uniqueness + accountStatus (DB) ────────
        com.eyetwin.entities.User existing = userService.findByEmail(email);
        if (existing != null) {
            String status = existing.getAccountStatus();
            if ("banned".equalsIgnoreCase(status)) {
                showError(emailFeedback,
                        "This email is associated with a banned account.");
            } else if ("suspended".equalsIgnoreCase(status)) {
                showError(emailFeedback,
                        "This email is associated with a suspended account. Contact support.");
            } else {
                showError(emailFeedback, "This email address is already registered.");
            }
            return;
        }

        // ── Persist ──────────────────────────────────────────
        try {
            boolean success = userService.register(fullName, email, password);

            if (!success) {
                showError(emailFeedback, "This email is already registered.");
                return;
            }

            // Save phone if provided (update after register)
            if (!phone.isEmpty()) {
                com.eyetwin.entities.User created = userService.findByEmail(email);
                if (created != null) {
                    created.setPhone(phone);
                    // Override username with the one chosen by the user
                    created.setUsername(username);
                    userService.update(created);
                }
            } else {
                // Still override username
                com.eyetwin.entities.User created = userService.findByEmail(email);
                if (created != null) {
                    created.setUsername(username);
                    userService.update(created);
                }
            }

            // Success → redirect to login
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");

        } catch (IllegalArgumentException e) {
            if (errorLabel != null) errorLabel.setText(e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════
    //  INDIVIDUAL VALIDATORS
    //  (retourne true = valide, false = erreur affichée)
    //  Chaque règle est une copie exacte du PHP Symfony.
    // ═════════════════════════════════════════════════════════

    /**
     * Règle Symfony 1 : Username obligatoire, min 3, alphanum + underscore.
     */
    private boolean validateUsername(boolean submit) {
        String val = usernameField.getText().trim();
        if (val.isEmpty()) {
            if (submit) showError(usernameFeedback, "Username is required.");
            return !submit;
        }
        if (val.length() < 3) {
            showError(usernameFeedback, "Username must contain at least 3 characters.");
            return false;
        }
        if (!val.matches("^[a-zA-Z0-9_]+$")) {
            showError(usernameFeedback,
                    "Username can only contain letters, numbers, and underscores.");
            return false;
        }
        showSuccess(usernameFeedback);
        return true;
    }

    /**
     * Règle Symfony 2 : Email obligatoire + format.
     */
    private boolean validateEmail(boolean submit) {
        String val = emailField.getText().trim();
        if (val.isEmpty()) {
            if (submit) showError(emailFeedback, "Email is required.");
            return !submit;
        }
        if (!val.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showError(emailFeedback, "Please provide a valid email address.");
            return false;
        }
        showSuccess(emailFeedback);
        return true;
    }

    /**
     * Règle Symfony 3 : Full name obligatoire, min 2 chars.
     */
    private boolean validateFullName(boolean submit) {
        String val = fullNameField.getText().trim();
        if (val.isEmpty()) {
            if (submit) showError(fullNameFeedback, "Full name is required.");
            return !submit;
        }
        if (val.length() < 2) {
            showError(fullNameFeedback, "Full name must contain at least 2 characters.");
            return false;
        }
        showSuccess(fullNameFeedback);
        return true;
    }

    /**
     * Règle Symfony 4 (implicite) : Phone optionnel, format +?[0-9\s\-().]{7,20}
     * Miroir du JS registration-validation.js dans le Twig.
     */
    private boolean validatePhone(boolean submit) {
        String val = phoneField.getText().trim();
        if (val.isEmpty()) {
            clearFeedback(phoneFeedback); // optionnel → pas d'erreur
            return true;
        }
        if (!val.matches("^\\+?[0-9\\s\\-().]{7,20}$")) {
            showError(phoneFeedback,
                    "Enter a valid phone number (e.g. +216 XX XXX XXX).");
            return false;
        }
        showSuccess(phoneFeedback);
        return true;
    }

    /**
     * Règles Symfony 5 + 6 : Password obligatoire, min 6 chars,
     * doit contenir majuscule + minuscule + chiffre.
     */
    private boolean validatePassword(boolean submit) {
        String val = passwordField.getText();
        if (val == null || val.isEmpty()) {
            if (submit) showError(passwordFeedback, "Password is required.");
            return !submit;
        }
        if (val.length() < 6) {
            showError(passwordFeedback, "Password must contain at least 6 characters.");
            return false;
        }
        if (!val.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            showError(passwordFeedback,
                    "Password must contain at least one uppercase letter, "
                            + "one lowercase letter, and one number.");
            return false;
        }
        showSuccess(passwordFeedback);
        return true;
    }

    /**
     * Règle client : confirm = password.
     */
    private boolean validateConfirm(boolean submit) {
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        if (confirm == null || confirm.isEmpty()) {
            if (submit) showError(confirmFeedback, "Please confirm your password.");
            return !submit;
        }
        if (!pass.equals(confirm)) {
            showError(confirmFeedback, "Passwords do not match.");
            return false;
        }
        showSuccess(confirmFeedback);
        return true;
    }

    /**
     * Règle Symfony 6 : Terms obligatoire.
     */
    private boolean validateTerms() {
        if (agreeTermsCheckBox == null || !agreeTermsCheckBox.isSelected()) {
            showError(termsFeedback, "You must accept the terms and conditions.");
            return false;
        }
        clearFeedback(termsFeedback);
        return true;
    }

    // ═════════════════════════════════════════════════════════
    //  PASSWORD STRENGTH BAR (miroir JS registration-validation.js)
    // ═════════════════════════════════════════════════════════

    private void updateStrengthBar(String password) {
        if (strengthBar == null || strengthLabel == null) return;

        if (password == null || password.isEmpty()) {
            strengthBar.setWidth(0);
            strengthLabel.setText("Enter password to see strength");
            return;
        }

        int score = 0;
        if (password.length() >= 6)                             score++;
        if (password.matches(".*[A-Z].*"))                      score++;
        if (password.matches(".*[a-z].*"))                      score++;
        if (password.matches(".*\\d.*"))                        score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}].*")) score++;

        double maxWidth = 340; // même largeur que les champs
        double pct      = score / 5.0;

        strengthBar.setWidth(maxWidth * pct);

        String color, label;
        switch (score) {
            case 1 -> { color = "#ff1744"; label = "Very Weak"; }
            case 2 -> { color = "#ff6d00"; label = "Weak";      }
            case 3 -> { color = "#ffd600"; label = "Fair";      }
            case 4 -> { color = "#00e676"; label = "Strong";    }
            default-> { color = "#00e676"; label = score >= 5 ? "Very Strong" : "Enter password"; }
        }

        strengthBar.setStyle("-fx-fill: " + color + ";");
        strengthLabel.setText(label);
        strengthLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    // ═════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════

    private void showError(Label label, String message) {
        if (label == null) return;
        label.setText("⚠ " + message);
        label.setStyle("-fx-text-fill: #ff1744; -fx-font-size: 11;");
        label.setVisible(true);
        label.setManaged(true);
        animateFade(label);
    }

    private void showSuccess(Label label) {
        if (label == null) return;
        label.setText("✓ Looks good");
        label.setStyle("-fx-text-fill: #00e676; -fx-font-size: 11;");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearFeedback(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void animateFade(Label label) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ═════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════

    @FXML public void goToLogin() {
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    @FXML public void goToHome() {
        MainApp.navigateTo("/com/eyetwin/views/home.fxml", "Home");
    }
}