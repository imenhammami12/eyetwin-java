package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.entities.ApplicationStatus;
import com.eyetwin.entities.CoachApplication;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoachApplicationService;
import com.eyetwin.services.CoachApplicationServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class CoachApplicationController {

    private static final String VIEWS        = "/com/eyetwin/views/";
    private static final long   MAX_CV_BYTES = 5L * 1024 * 1024;
    private static final int    MIN_CERT_LEN = 50;
    private static final int    MIN_EXP_LEN  = 100;

    // ── Service (interface, plus de DAO direct) ──
    private final ICoachApplicationService coachApplicationService = new CoachApplicationServiceImpl();

    // ── State ──
    private File selectedCvFile = null;

    // ── FXML — Navbar ──
    @FXML private Label navUsername;
    @FXML private Label navAvatarInitial;
    @FXML private Label coinsNavLabel;

    // ── FXML — Flash messages ──
    @FXML private VBox  flashErrorBox;
    @FXML private Label flashErrorLabel;

    // ── FXML — Step 01: Certifications ──
    @FXML private TextArea certificationsField;
    @FXML private Label    certCounter;
    @FXML private Label    certErrorLabel;

    // ── FXML — Step 02: Experience ──
    @FXML private TextArea experienceField;
    @FXML private Label    expCounter;
    @FXML private Label    expErrorLabel;

    // ── FXML — Step 03: CV Upload ──
    @FXML private VBox  dropZone;
    @FXML private HBox  filePreview;
    @FXML private Label cvFileName;
    @FXML private Label cvErrorLabel;

    // ── FXML — Submit ──
    @FXML private Button submitBtn;

    // ─────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();

        // Guard 1 — must be logged in
        if (user == null) { navigateTo("login.fxml"); return; }

        fillNavbar(user);

        // Guard 2 — already a coach
        if (hasRole(user, "ROLE_COACH")) {
            Platform.runLater(() -> {
                SessionManager.setPendingFlash("info", "You are already a coach!");
                navigateTo("UserProfile.fxml");
            });
            return;
        }

        // Guard 3 — pending application exists (async DB check)
        new Thread(() -> {
            try {
                if (coachApplicationService.hasPendingApplication(user.getId())) {
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("warning",
                                "You already have a pending application.");
                        navigateTo("UserProfile.fxml");
                    });
                }
            } catch (SQLException e) {
                System.err.println("[CoachApplication] Guard check error: " + e.getMessage());
            }
        }).start();

        wireCounter(certificationsField, certCounter, MIN_CERT_LEN);
        wireCounter(experienceField,     expCounter,  MIN_EXP_LEN);
        Platform.runLater(() -> {
            styleTextArea(certificationsField);
            styleTextArea(experienceField);
        });
    }
    private void styleTextArea(TextArea area) {
        if (area == null) return;
        area.applyCss();
        area.layout();
        javafx.scene.Node content = area.lookup(".content");
        if (content != null) {
            content.setStyle("-fx-background-color: rgba(255,255,255,0.03);");
        }
        // Also style the scroll-pane viewport
        javafx.scene.Node viewport = area.lookup(".scroll-pane");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: transparent;");
        }
    }

    // ─────────────────────────────────────────────────────────
    //  NAVBAR
    // ─────────────────────────────────────────────────────────
    private void fillNavbar(User user) {
        if (navUsername != null) {
            String uname = user.getUsername();
            navUsername.setText(uname != null ? uname.toUpperCase() : "PLAYER");
        }
        if (navAvatarInitial != null) {
            String uname = user.getUsername();
            if (uname != null && !uname.isEmpty())
                navAvatarInitial.setText(String.valueOf(uname.charAt(0)).toUpperCase());
        }
        if (coinsNavLabel != null)
            coinsNavLabel.setText(String.valueOf(user.getCoinBalance()));
    }

    // ─────────────────────────────────────────────────────────
    //  CHARACTER COUNTERS
    // ─────────────────────────────────────────────────────────
    private void wireCounter(TextArea field, Label counter, int min) {
        if (field == null || counter == null) return;
        Runnable update = () -> {
            int len = field.getText().length();
            counter.setText(len + " / " + min + " min");
            counter.setStyle(len >= min
                    ? "-fx-text-fill: #4cd3e3; -fx-font-size: 11; -fx-font-weight: bold;"
                    : "-fx-text-fill: #f44a40; -fx-font-size: 11; -fx-font-weight: bold;");
        };
        field.textProperty().addListener((obs, o, n) -> update.run());
        update.run();
    }

    // ─────────────────────────────────────────────────────────
    //  CV FILE PICKER
    // ─────────────────────────────────────────────────────────
    @FXML
    public void handlePickCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CV / Portfolio");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documents (PDF, DOC, DOCX)",
                        "*.pdf", "*.doc", "*.docx"));
        File file = chooser.showOpenDialog(resolveStage());
        if (file == null) return;

        if (file.length() > MAX_CV_BYTES) {
            showError(cvErrorLabel, "File size must be less than 5 MB.");
            return;
        }

        hideNode(cvErrorLabel);
        selectedCvFile = file;
        if (cvFileName != null) cvFileName.setText(file.getName());
        show(filePreview, true);
        show(dropZone, false);
    }

    @FXML
    public void handleRemoveCv() {
        selectedCvFile = null;
        show(filePreview, false);
        show(dropZone, true);
        hideNode(cvErrorLabel);
    }

    // ─────────────────────────────────────────────────────────
    //  SUBMIT
    // ─────────────────────────────────────────────────────────
    @FXML
    public void handleSubmit() {
        hideAllErrors();

        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        String certText = certificationsField != null ? certificationsField.getText().trim() : "";
        String expText  = experienceField     != null ? experienceField.getText().trim()     : "";

        boolean valid = true;
        if (certText.isEmpty()) {
            showError(certErrorLabel, "Certifications are required.");
            valid = false;
        } else if (certText.length() < MIN_CERT_LEN) {
            showError(certErrorLabel,
                    "Certifications must be at least " + MIN_CERT_LEN + " characters.");
            valid = false;
        }
        if (expText.isEmpty()) {
            showError(expErrorLabel, "Experience is required.");
            valid = false;
        } else if (expText.length() < MIN_EXP_LEN) {
            showError(expErrorLabel,
                    "Experience must be at least " + MIN_EXP_LEN + " characters.");
            valid = false;
        }
        if (!valid) return;

        if (submitBtn != null) {
            submitBtn.setDisable(true);
            submitBtn.setText("⏳  Sending…");
        }

        final File   cvFile    = selectedCvFile;
        final String certFinal = certText;
        final String expFinal  = expText;

        new Thread(() -> {
            try {
                // Server-side guards (defensive re-check)
                if (hasRole(user, "ROLE_COACH")) {
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("info", "You are already a coach!");
                        navigateTo("UserProfile.fxml");
                    });
                    return;
                }
                if (coachApplicationService.hasPendingApplication(user.getId())) {
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("warning",
                                "You already have a pending application.");
                        navigateTo("UserProfile.fxml");
                    });
                    return;
                }

                CoachApplication application = new CoachApplication();
                application.setUserId(user.getId());
                application.setStatus(ApplicationStatus.PENDING);
                application.setCertifications(certFinal);
                application.setExperience(expFinal);

                if (cvFile != null) {
                    String savedName = saveCvFile(cvFile, user.getId());
                    if (savedName != null) application.setCvFile(savedName);
                }

                coachApplicationService.save(application);

                System.out.println("[CoachApplication] New application submitted by user id="
                        + user.getId() + " (application id=" + application.getId() + ")");

                Platform.runLater(() -> {
                    SessionManager.setPendingFlash("success",
                            "Your coach application has been submitted successfully!");
                    navigateTo("UserProfile.fxml");
                });

            } catch (Exception e) {
                System.err.println("[CoachApplication] Submit error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    showGlobalError("An error occurred. Please try again.");
                    resetSubmitBtn();
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  CV FILE SAVE
    // ─────────────────────────────────────────────────────────
    private String saveCvFile(File source, int userId) {
        try {
            Path cvDir = Paths.get(System.getProperty("user.dir"), "uploads", "cv");
            Files.createDirectories(cvDir);

            String original = source.getName();
            String stem     = original.contains(".")
                    ? original.substring(0, original.lastIndexOf('.')) : original;
            String ext      = original.contains(".")
                    ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "pdf";

            String safeStem = stem.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
            String newName  = safeStem + "-" + userId + "-" + System.currentTimeMillis() + "." + ext;

            Files.copy(source.toPath(), cvDir.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
            return newName;
        } catch (IOException e) {
            System.err.println("[CoachApplication] CV upload error: " + e.getMessage());
            Platform.runLater(() -> showError(cvErrorLabel, "Error uploading CV."));
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────
    @FXML public void goToProfile()     { navigateTo("UserProfile.fxml"); }
    @FXML public void goToEditProfile() { navigateTo("UserEditProfile.fxml"); }
    @FXML public void goHome()          { MainApp.navigateTo(VIEWS + "home.fxml", "Home"); }
    @FXML public void goToCoins()       { MainApp.navigateTo(VIEWS + "Coins.fxml", "Coins"); }
    @FXML public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }

    private void navigateTo(String fxml) {
        String[] paths = { VIEWS + fxml, "/com/eyetwin/view/" + fxml };
        try {
            java.net.URL url = null;
            for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
            if (url == null) { System.err.println("[CoachApp] FXML not found: " + fxml); return; }
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[CoachApp] Nav error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                certificationsField, experienceField, submitBtn, navUsername
        };
        for (javafx.scene.Node n : candidates)
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        return null;
    }

    // ─────────────────────────────────────────────────────────
    //  UI HELPERS
    // ─────────────────────────────────────────────────────────
    private void show(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
    private void hideNode(javafx.scene.Node node) { show(node, false); }

    private void showError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }
    private void showGlobalError(String msg) {
        if (flashErrorLabel != null) flashErrorLabel.setText(msg);
        show(flashErrorBox, true);
    }
    private void hideAllErrors() {
        show(flashErrorBox,  false);
        show(certErrorLabel, false);
        show(expErrorLabel,  false);
        show(cvErrorLabel,   false);
    }
    private void resetSubmitBtn() {
        if (submitBtn != null) {
            submitBtn.setDisable(false);
            submitBtn.setText("✈  Submit Application");
        }
    }

    private boolean hasRole(User user, String role) {
        return user.getRolesJson() != null && user.getRolesJson().contains(role);
    }
}
