package com.eyetwin.controller;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ILiveStreamService;
import com.eyetwin.services.FFmpegStreamingService;
import com.eyetwin.services.LiveStreamServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LiveCreateController {

    @FXML private NavbarController navbarController;
    @FXML private TextField titleField;
    @FXML private TextArea  descriptionField;
    @FXML private TextField coinPriceField;
    @FXML private VBox      flashBox;
    @FXML private Label     flashLabel;

    private final ILiveStreamService liveService = new LiveStreamServiceImpl();

    private boolean ffmpegAvailable = false;

    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("live");

        // ✅ FIX: Guard against non-coach users before doing anything
        if (!SessionManager.isCoach()) {
            SessionManager.setPendingFlash("error", "Only coaches can create live streams.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }

        // ✅ FIX: flashBox starts fully hidden — only show after FFmpeg check completes
        hideFlash();

        // Check FFmpeg availability in background thread
        new Thread(() -> {
            ffmpegAvailable = FFmpegStreamingService.isFFmpegAvailable();

            // ✅ FIX: All UI updates safely dispatched to JavaFX thread
            if (!ffmpegAvailable) {
                Platform.runLater(() ->
                        showFlash("info",
                                "⚠ FFmpeg not found — desktop capture will be unavailable. Install FFmpeg to stream without OBS.")
                );
            } else {
                Platform.runLater(() ->
                        showFlash("success", "✔ FFmpeg detected — desktop capture ready.")
                );
            }
        }, "ffmpeg-check").start();
    }

    @FXML
    private void handleCreate() {
        User user = SessionManager.getCurrentUser();
        if (user == null || !user.isCoach()) {
            showFlash("error", "Coach session required.");
            return;
        }

        String title       = titleField.getText() != null ? titleField.getText().trim() : "";
        String description = descriptionField.getText();
        int    coinPrice;

        if (title.isBlank()) {
            showFlash("error", "Title is required.");
            return;
        }

        try {
            String raw = coinPriceField.getText();
            if (raw == null || raw.isBlank()) {
                // ✅ FIX: treat empty coin price field as 0 (free)
                coinPrice = 0;
            } else {
                coinPrice = Integer.parseInt(raw.trim());
                if (coinPrice < 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showFlash("error", "Coin price must be a positive number or zero.");
            return;
        }

        try {
            LiveStream live = liveService.createStream(user, title, description, coinPrice);
            SessionManager.setSelectedLiveStream(live);
            SessionManager.setPendingFlash("success",
                    ffmpegAvailable
                            ? "Stream created ✔ — click 'Go Live' then 'Start Capture' to stream your desktop."
                            : "Stream created ✔ — use OBS with the RTMP key shown on the next page."
            );
            navigateTo("LiveManage.fxml");
        } catch (SQLException e) {
            showFlash("error", "Unable to create live stream.");
        }
    }

    @FXML
    private void goBack() { navigateTo("Live.fxml"); }

    // ── Flash helpers ─────────────────────────────────────────────────────────

    /**
     * ✅ FIX: showFlash must only be called on the JavaFX Application Thread.
     * Always wrap calls from background threads with Platform.runLater().
     */
    private void showFlash(String type, String message) {
        if (flashBox == null || flashLabel == null) return;

        String color = switch (type) {
            case "success" -> "#70d0a0";
            case "error"   -> "#ff8a8a";
            default        -> "#9eb1ff";   // "info"
        };

        flashLabel.setText(message);
        flashLabel.setStyle("-fx-text-fill: " + color + ";");

        // ✅ FIX: both visible AND managed must be true so the VBox takes space
        flashBox.setVisible(true);
        flashBox.setManaged(true);
    }

    /** ✅ NEW: explicit hide helper used in initialize() */
    private void hideFlash() {
        if (flashBox == null) return;
        flashBox.setVisible(false);
        flashBox.setManaged(false);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage    = (Stage) titleField.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null)
                newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            // ✅ FIX: print the full stack trace, not just the filename
            System.err.println("[LiveCreateController] Navigation error: " + fxml);
            e.printStackTrace();                          // ← add this
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
                e.getCause().printStackTrace();           // ← and this
            }
        }
    }

}
}
