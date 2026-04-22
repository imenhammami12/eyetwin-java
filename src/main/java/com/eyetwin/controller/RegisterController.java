package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * RegisterController — miroir exact du RegistrationController Symfony.
 *
 * Validations (dans l'ordre Symfony) :
 *   1. Username   : obligatoire, min 3 chars, ^[a-zA-Z0-9_]+$
 *   2. Email      : obligatoire, format valide
 *   3. Full name  : obligatoire, min 2 chars
 *   4. Phone      : optionnel, format +?[0-9\s\-().]{7,20}
 *   5. Password   : obligatoire, min 6 chars
 *   6. Password   : majuscule + minuscule + chiffre
 *   7. Confirm    : correspond au password
 *   8. Terms      : doit être accepté (via modal scroll-to-unlock)
 *   9. Username   : unicité DB
 *  10. Email      : unicité DB + vérif banned/suspended
 *
 * Terms modal (miroir du JS Symfony) :
 *   - Clic sur CheckBox → ouvre le modal (tick bloqué)
 *   - Bouton "Accept" désactivé jusqu'au scroll complet (≥98%)
 *   - "Decline" → redirige vers login
 *   - Fermeture via ✕ ou Escape → CheckBox reste décoché
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

    @FXML private Label usernameFeedback;
    @FXML private Label emailFeedback;
    @FXML private Label fullNameFeedback;
    @FXML private Label phoneFeedback;
    @FXML private Label passwordFeedback;
    @FXML private Label confirmFeedback;
    @FXML private Label termsFeedback;

    @FXML private Rectangle strengthBar;
    @FXML private Label     strengthLabel;
    @FXML private Label     errorLabel;

    // ─────────────────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────────────────

    private final IUserService userService    = new UserServiceImpl();
    private       boolean      acceptedViaModal = false;

    // ═════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // Live validation on focus-lost
        bindFocusValidator(usernameField,        () -> validateUsername(false));
        bindFocusValidator(emailField,           () -> validateEmail(false));
        bindFocusValidator(fullNameField,        () -> validateFullName(false));
        bindFocusValidator(passwordField,        () -> validatePassword(false));
        bindFocusValidator(confirmPasswordField, () -> validateConfirm(false));

        usernameField.textProperty().addListener((o, ov, nv) -> clearFeedback(usernameFeedback));
        emailField   .textProperty().addListener((o, ov, nv) -> clearFeedback(emailFeedback));
        fullNameField.textProperty().addListener((o, ov, nv) -> clearFeedback(fullNameFeedback));
        confirmPasswordField.textProperty().addListener((o, ov, nv) -> clearFeedback(confirmFeedback));

        phoneField.textProperty().addListener((o, ov, nv) -> validatePhone(false));

        passwordField.textProperty().addListener((o, ov, nv) -> {
            clearFeedback(passwordFeedback);
            updateStrengthBar(nv);
            if (confirmPasswordField.getText() != null && !confirmPasswordField.getText().isEmpty())
                validateConfirm(false);
        });

        // ── Intercept CheckBox direct click ────────────────
        // When the user tries to check it (selected goes true),
        // we block it and open the modal instead.
        agreeTermsCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected && !acceptedViaModal) {
                acceptedViaModal = false;
                agreeTermsCheckBox.setSelected(false); // undo the tick
                openTermsModal();
            }
        });
    }

    // ═════════════════════════════════════════════════════════
    //  TERMS MODAL  (scroll-to-unlock, mirrors Symfony JS)
    // ═════════════════════════════════════════════════════════

    private void openTermsModal() {
        Stage modal = new Stage(StageStyle.TRANSPARENT);
        modal.initModality(Modality.APPLICATION_MODAL);

        // ── Backdrop overlay ───────────────────────────────
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.82);");

        // ── Card ───────────────────────────────────────────
        VBox card = new VBox();
        card.setPrefWidth(640);
        card.setMaxWidth(640);
        card.setStyle(
                "-fx-background-color: #0f0f14;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.7),60,0,0,8);"
        );

        // ── Header ─────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 28, 18, 28));
        header.setStyle("-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 0 0 1 0;");

        VBox headerText = new VBox(3);
        Label titleLbl = new Label("Terms & Conditions");
        titleLbl.setStyle(
                "-fx-font-family:'Arial Black'; -fx-font-size:17;" +
                        "-fx-font-weight:bold; -fx-text-fill:white;"
        );
        Label subLbl = new Label("LAST UPDATED — 2025");
        subLbl.setStyle("-fx-text-fill:#5a6070; -fx-font-size:10;");
        headerText.getChildren().addAll(titleLbl, subLbl);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.05);" +
                        "-fx-border-color:rgba(255,255,255,0.1);" +
                        "-fx-border-radius:8; -fx-background-radius:8;" +
                        "-fx-text-fill:#8a9090; -fx-font-size:14;" +
                        "-fx-pref-width:34; -fx-pref-height:34; -fx-cursor:hand;"
        );
        closeBtn.setOnAction(e -> modal.close());

        header.getChildren().addAll(headerText, hSpacer, closeBtn);

        // ── Red progress bar ───────────────────────────────
        StackPane progressBar = new StackPane();
        progressBar.setPrefHeight(3);
        progressBar.setStyle("-fx-background-color:rgba(255,255,255,0.05);");

        Region progressFill = new Region();
        progressFill.setPrefHeight(3);
        progressFill.setPrefWidth(0);
        progressFill.setStyle(
                "-fx-background-color:linear-gradient(to right,#dc143c,#ff1744);" +
                        "-fx-background-radius:0 2 2 0;"
        );
        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
        progressBar.getChildren().add(progressFill);

        // ── Scroll pane with terms ─────────────────────────
        ScrollPane scrollPane = new ScrollPane(buildTermsContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(360);
        scrollPane.setStyle(
                "-fx-background:#0f0f14;" +
                        "-fx-background-color:#0f0f14;" +
                        "-fx-border-color:transparent;"
        );

        // ── Scroll hint ────────────────────────────────────
        Label scrollHint = new Label("▼  Scroll down to read all terms");
        scrollHint.setMaxWidth(Double.MAX_VALUE);
        scrollHint.setAlignment(Pos.CENTER);
        scrollHint.setStyle("-fx-text-fill:#4a5060; -fx-font-size:11; -fx-padding:6 0 4 0;");

        // ── Footer ─────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(16, 28, 24, 28));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-border-color:rgba(255,255,255,0.07); -fx-border-width:1 0 0 0;");

        Button declineBtn = new Button("Decline");
        declineBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-border-color:rgba(255,255,255,0.1);" +
                        "-fx-border-radius:10; -fx-background-radius:10;" +
                        "-fx-text-fill:#8a9090; -fx-font-size:13; -fx-font-weight:bold;" +
                        "-fx-padding:12 22 12 22; -fx-cursor:hand;"
        );
        // Decline → redirect to login (mirror Symfony JS)
        declineBtn.setOnAction(e -> {
            modal.close();
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
        });

        Button acceptBtn = new Button("✓  I Accept These Terms");
        acceptBtn.setDisable(true);
        acceptBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(acceptBtn, Priority.ALWAYS);
        styleAcceptDisabled(acceptBtn);

        footer.getChildren().addAll(declineBtn, acceptBtn);

        // ── Scroll listener → unlock accept at ≥98% ───────
        scrollPane.vvalueProperty().addListener((obs, ov, nv) -> {
            double pct = nv.doubleValue();
            progressFill.setPrefWidth(card.getPrefWidth() * pct);
            if (pct >= 0.98 && acceptBtn.isDisable()) {
                acceptBtn.setDisable(false);
                styleAcceptEnabled(acceptBtn);
                scrollHint.setVisible(false);
            }
        });

        // ── Accept action ──────────────────────────────────
        acceptBtn.setOnAction(e -> {
            if (!acceptBtn.isDisable()) {
                acceptedViaModal = true;
                agreeTermsCheckBox.setSelected(true);
                acceptedViaModal = false;
                clearFeedback(termsFeedback);
                modal.close();
            }
        });

        // ── Assemble ───────────────────────────────────────
        card.getChildren().addAll(header, progressBar, scrollPane, scrollHint, footer);
        StackPane.setMargin(card, new Insets(40));
        overlay.getChildren().add(card);

        // ── Close on overlay click or Escape ───────────────
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) modal.close(); });

        Scene scene = new Scene(overlay, 800, 640);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) modal.close(); });

        modal.setScene(scene);
        modal.show();
    }

    private void styleAcceptDisabled(Button btn) {
        btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.07);" +
                        "-fx-text-fill:#404550; -fx-font-size:13; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-border-radius:10;" +
                        "-fx-padding:12 22 12 22; -fx-cursor:default;"
        );
    }

    private void styleAcceptEnabled(Button btn) {
        btn.setStyle(
                "-fx-background-color:linear-gradient(to right,#dc143c,#ff1744);" +
                        "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-border-radius:10;" +
                        "-fx-padding:12 22 12 22; -fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(220,20,60,0.35),12,0,0,3);"
        );
    }

    /** Builds the scrollable terms content — 8 sections mirror of Symfony Twig */
    private VBox buildTermsContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setStyle("-fx-background-color:#0f0f14;");
        content.getChildren().addAll(
                termsSection("1. Acceptance of Terms",
                        "By creating an account on EyeTwin E-Sport Platform, you agree to be bound by " +
                                "these Terms and Conditions. If you do not agree with any part of these terms, " +
                                "you may not access the platform."),
                termsSection("2. Eligibility",
                        "You must be at least 13 years of age to use EyeTwin. By registering, you confirm " +
                                "that you meet this age requirement and that the information you provide is accurate " +
                                "and complete."),
                termsSection("3. Account Responsibility",
                        "You are solely responsible for:\n" +
                                "  •  Maintaining the confidentiality of your account credentials\n" +
                                "  •  All activity that occurs under your account\n" +
                                "  •  Notifying us immediately of any unauthorized use of your account"),
                termsSection("4. Platform Use",
                        "EyeTwin is an e-sport platform. You agree not to:\n" +
                                "  •  Use the platform for any unlawful purpose\n" +
                                "  •  Harass, threaten, or abuse other users\n" +
                                "  •  Attempt to gain unauthorized access to any part of the platform\n" +
                                "  •  Upload malicious code, spam, or any harmful content\n" +
                                "  •  Impersonate other users or EyeTwin staff"),
                termsSection("5. Privacy & Data",
                        "We collect and process personal data in accordance with our Privacy Policy. " +
                                "By using EyeTwin, you consent to the collection of data necessary to provide " +
                                "our services, including account information and usage analytics."),
                termsSection("6. Competitions & Events",
                        "Participation in tournaments and events hosted on EyeTwin may be subject to " +
                                "additional rules published at the time of each event. EyeTwin reserves the right " +
                                "to disqualify any participant for violation of fair play standards."),
                termsSection("7. Termination",
                        "EyeTwin reserves the right to suspend or permanently ban any account that violates " +
                                "these Terms and Conditions, without prior notice and at our sole discretion."),
                termsSection("8. Changes to Terms",
                        "We may update these Terms at any time. Continued use of the platform after changes " +
                                "constitutes acceptance of the updated terms. We will notify users of significant " +
                                "changes via email or in-platform notification.")
        );
        return content;
    }

    private VBox termsSection(String title, String body) {
        VBox section = new VBox(8);
        Label t = new Label(title);
        t.setStyle(
                "-fx-font-family:'Arial Black'; -fx-font-size:12;" +
                        "-fx-font-weight:bold; -fx-text-fill:#dc143c;"
        );
        t.setWrapText(true);
        Label b = new Label(body);
        b.setStyle("-fx-text-fill:#9aa0b0; -fx-font-size:13; -fx-line-spacing:3;");
        b.setWrapText(true);
        b.setMaxWidth(580);
        section.getChildren().addAll(t, b);
        return section;
    }

    // ═════════════════════════════════════════════════════════
    //  HANDLE REGISTER
    // ═════════════════════════════════════════════════════════

    @FXML
    public void handleRegister() {
        if (errorLabel != null) errorLabel.setText("");

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

        // DB: username uniqueness
        if (userService.findByUsername(username) != null) {
            showError(usernameFeedback, "This username is already taken.");
            return;
        }

        // DB: email uniqueness + account status
        com.eyetwin.entities.User existing = userService.findByEmail(email);
        if (existing != null) {
            String status = existing.getAccountStatus();
            if ("banned".equalsIgnoreCase(status)) {
                showError(emailFeedback,
                        "This email is associated with a banned account. You cannot register with this email.");
            } else if ("suspended".equalsIgnoreCase(status)) {
                showError(emailFeedback,
                        "This email is associated with a suspended account. Please contact support to resolve this issue.");
            } else {
                showError(emailFeedback, "This email address is already registered.");
            }
            return;
        }

        try {
            boolean success = userService.register(fullName, email, password);
            if (!success) {
                showError(emailFeedback, "This email is already registered.");
                return;
            }
            // Patch auto-generated username & optional phone
            com.eyetwin.entities.User created = userService.findByEmail(email);
            if (created != null) {
                created.setUsername(username);
                if (!phone.isEmpty()) created.setPhone(phone);
                userService.update(created);
            }
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");

        } catch (IllegalArgumentException e) {
            if (errorLabel != null) errorLabel.setText(e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════
    //  VALIDATORS
    // ═════════════════════════════════════════════════════════

    private boolean validateUsername(boolean submit) {
        String v = usernameField.getText().trim();
        if (v.isEmpty()) { if (submit) showError(usernameFeedback, "Username is required."); return !submit; }
        if (v.length() < 3) { showError(usernameFeedback, "Username must contain at least 3 characters."); return false; }
        if (!v.matches("^[a-zA-Z0-9_]+$")) { showError(usernameFeedback, "Username can only contain letters, numbers, and underscores."); return false; }
        showSuccess(usernameFeedback); return true;
    }

    private boolean validateEmail(boolean submit) {
        String v = emailField.getText().trim();
        if (v.isEmpty()) { if (submit) showError(emailFeedback, "Email is required."); return !submit; }
        if (!v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { showError(emailFeedback, "Please provide a valid email address."); return false; }
        showSuccess(emailFeedback); return true;
    }

    private boolean validateFullName(boolean submit) {
        String v = fullNameField.getText().trim();
        if (v.isEmpty()) { if (submit) showError(fullNameFeedback, "Full name is required."); return !submit; }
        if (v.length() < 2) { showError(fullNameFeedback, "Full name must contain at least 2 characters."); return false; }
        showSuccess(fullNameFeedback); return true;
    }

    private boolean validatePhone(boolean submit) {
        String v = phoneField.getText().trim();
        if (v.isEmpty()) { clearFeedback(phoneFeedback); return true; }
        if (!v.matches("^\\+?[0-9\\s\\-().]{7,20}$")) { showError(phoneFeedback, "Enter a valid phone number (e.g. +216 XX XXX XXX)."); return false; }
        showSuccess(phoneFeedback); return true;
    }

    private boolean validatePassword(boolean submit) {
        String v = passwordField.getText();
        if (v == null || v.isEmpty()) { if (submit) showError(passwordFeedback, "Password is required."); return !submit; }
        if (v.length() < 6) { showError(passwordFeedback, "Password must contain at least 6 characters."); return false; }
        if (!v.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) { showError(passwordFeedback, "Password must contain at least one uppercase letter, one lowercase letter, and one number."); return false; }
        showSuccess(passwordFeedback); return true;
    }

    private boolean validateConfirm(boolean submit) {
        String v = confirmPasswordField.getText();
        if (v == null || v.isEmpty()) { if (submit) showError(confirmFeedback, "Please confirm your password."); return !submit; }
        if (!passwordField.getText().equals(v)) { showError(confirmFeedback, "Passwords do not match."); return false; }
        showSuccess(confirmFeedback); return true;
    }

    private boolean validateTerms() {
        if (agreeTermsCheckBox == null || !agreeTermsCheckBox.isSelected()) {
            showError(termsFeedback, "You must accept the terms and conditions.");
            openTermsModal();
            return false;
        }
        clearFeedback(termsFeedback);
        return true;
    }

    // ═════════════════════════════════════════════════════════
    //  PASSWORD STRENGTH BAR
    // ═════════════════════════════════════════════════════════

    private void updateStrengthBar(String pw) {
        if (strengthBar == null || strengthLabel == null) return;
        if (pw == null || pw.isEmpty()) {
            strengthBar.setWidth(0);
            strengthLabel.setText("Enter password to see strength");
            strengthLabel.setStyle("-fx-text-fill:#444; -fx-font-size:10;");
            return;
        }
        int score = 0;
        if (pw.length() >= 6)                              score++;
        if (pw.matches(".*[A-Z].*"))                       score++;
        if (pw.matches(".*[a-z].*"))                       score++;
        if (pw.matches(".*\\d.*"))                         score++;
        if (pw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}].*")) score++;

        strengthBar.setWidth(340 * (score / 5.0));
        String[] colors = {"#ff1744","#ff6d00","#ffd600","#00e676","#00e676"};
        String[] labels = {"Very Weak","Weak","Fair","Strong","Very Strong"};
        int idx = Math.max(0, Math.min(score - 1, 4));
        strengthBar.setStyle("-fx-fill:" + colors[idx] + ";");
        strengthLabel.setText(labels[idx]);
        strengthLabel.setStyle("-fx-text-fill:" + colors[idx] + "; -fx-font-size:10;");
    }

    // ═════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═════════════════════════════════════════════════════════

    private void showError(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText("⚠ " + msg);
        lbl.setStyle("-fx-text-fill:#ff1744; -fx-font-size:11;");
        lbl.setVisible(true); lbl.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), lbl);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void showSuccess(Label lbl) {
        if (lbl == null) return;
        lbl.setText("✓ Looks good");
        lbl.setStyle("-fx-text-fill:#00e676; -fx-font-size:11;");
        lbl.setVisible(true); lbl.setManaged(true);
    }

    private void clearFeedback(Label lbl) {
        if (lbl == null) return;
        lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false);
    }

    private void bindFocusValidator(javafx.scene.control.Control field, Runnable r) {
        field.focusedProperty().addListener((obs, was, is) -> { if (!is) r.run(); });
    }

    // ═════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════════════════════════

    @FXML public void goToLogin() { MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login"); }
    @FXML public void goToHome()  { MainApp.navigateTo("/com/eyetwin/views/home.fxml",  "Home");  }
}
