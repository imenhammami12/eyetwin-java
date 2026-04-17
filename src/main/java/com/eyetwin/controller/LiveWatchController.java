package com.eyetwin.controller;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ILiveStreamService;
import com.eyetwin.services.LiveStreamServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveWatchController {

    @FXML private NavbarController navbarController;
    @FXML private VBox flashBox;
    @FXML private Label flashLabel;
    @FXML private Label titleLabel;
    @FXML private Label coachLabel;
    @FXML private Label statusLabel;
    @FXML private Label priceLabel;
    @FXML private Label startedAtLabel;
    @FXML private Label descriptionLabel;
    @FXML private VBox paywallBox;
    @FXML private Label userBalanceLabel;
    @FXML private Label neededCoinsLabel;
    @FXML private Button joinButton;
    @FXML private VBox playerBox;
    @FXML private Label waitingLabel;
    @FXML private WebView playerWebView;

    private final ILiveStreamService liveService = new LiveStreamServiceImpl();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private LiveStream live;
    private User user;
    private boolean hasAccess;
    private Process ffmpegPreviewProcess;

    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("live");
        user = SessionManager.getCurrentUser();
        if (user == null) {
            SessionManager.setPendingFlash("error", "Please sign in to watch a live stream.");
            Platform.runLater(() -> navigateTo("login.fxml"));
            return;
        }

        LiveStream selected = SessionManager.getSelectedLiveStream();
        if (selected == null) {
            SessionManager.setPendingFlash("error", "No live stream selected.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }

        try {
            live = liveService.getById(selected.getId());
            if (live == null) {
                SessionManager.setPendingFlash("error", "Live stream not found.");
                Platform.runLater(() -> navigateTo("Live.fxml"));
                return;
            }
            hasAccess = liveService.userHasAccess(user, live);
        } catch (SQLException e) {
            SessionManager.setPendingFlash("error", "Unable to open live stream.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }

        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlash(flash[0], flash[1]);
        render();
    }

    private void render() {
        titleLabel.setText(live.getTitle());
        coachLabel.setText(live.getCoach() != null ? live.getCoach().getUsername() : "Unknown");
        statusLabel.setText(live.isLive() ? "Live" : live.isEnded() ? "Ended" : "Upcoming");
        priceLabel.setText(live.getCoinPrice() == 0 ? "FREE" : live.getCoinPrice() + " coins");
        startedAtLabel.setText(live.getStartedAt() == null ? "—" : fmt.format(live.getStartedAt()));
        descriptionLabel.setText(live.getDescription() == null || live.getDescription().isBlank()
                ? "No description provided." : live.getDescription());

        if (!hasAccess && live.getCoinPrice() == 0 && !live.isEnded()) {
            hasAccess = true;
        }

        playerBox.setVisible(hasAccess);
        playerBox.setManaged(hasAccess);
        paywallBox.setVisible(!hasAccess);
        paywallBox.setManaged(!hasAccess);

        if (hasAccess) {
            if (live.isLive()) {
                waitingLabel.setText("Live now");
                loadPlayer();
            } else {
                waitingLabel.setText(live.isEnded()
                        ? "This live stream has ended."
                        : "The coach will go live soon.");
                if (playerWebView != null)
                    playerWebView.getEngine().loadContent(
                            "<html><body style='background:#080810;color:white;" +
                                    "font-family:Arial;padding:24px'>Stream is not live yet.</body></html>");
            }
        } else {
            int missing = Math.max(0, live.getCoinPrice() - user.getCoinBalance());
            userBalanceLabel.setText(user.getCoinBalance() + " coins");
            neededCoinsLabel.setText(missing == 0 ? "You can join now." : "Need " + missing + " more coins.");
            joinButton.setDisable(missing > 0 || live.isEnded());
        }
    }

    // ── Detect real stream dimensions ─────────────────────────────────────────

    private int[] detectStreamSize(String ffmpegExe, String url) {
        try {
            Process p = new ProcessBuilder(ffmpegExe, "-i", url)
                    .redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("Video:") && line.contains("x")) {
                        Matcher m = Pattern.compile("(\\d{2,4})x(\\d{2,4})").matcher(line);
                        if (m.find()) {
                            int w = Integer.parseInt(m.group(1));
                            int h = Integer.parseInt(m.group(2));
                            if (w > 100 && h > 100) {
                                p.destroyForcibly();
                                System.out.println("[LiveWatch] Detected: " + w + "x" + h);
                                return new int[]{w, h};
                            }
                        }
                    }
                }
            }
            p.destroyForcibly();
        } catch (Exception e) {
            System.err.println("[LiveWatch] Size detection failed: " + e.getMessage());
        }
        return new int[]{640, 360}; // fallback
    }

    // ── Player ────────────────────────────────────────────────────────────────

    private void loadPlayer() {
        String hlsUrl = "http://127.0.0.1:8888/live/" + live.getStreamKey() + "/index.m3u8";

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(860);
        imageView.setFitHeight(430);

        // Remplace le WebView par l'ImageView dans playerBox
        Platform.runLater(() -> {
            if (playerBox.getChildren().size() > 1)
                playerBox.getChildren().set(1, imageView);
        });

        new Thread(() -> {
            try {
                String ffmpegExe = com.eyetwin.services.FFmpegStreamingService.resolveFfmpegPath();
                if (ffmpegExe == null) {
                    Platform.runLater(() -> showFlash("error", "FFmpeg not found."));
                    return;
                }

                // ✅ Détecte les dimensions réelles
                int[] size   = detectStreamSize(ffmpegExe, hlsUrl);
                int width    = size[0];
                int height   = size[1];
                System.out.println("[LiveWatch] Stream size: " + width + "x" + height);

                // Met à jour l'ImageView avec les bonnes dimensions
                Platform.runLater(() -> {
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                });

                ProcessBuilder pb = new ProcessBuilder(
                        ffmpegExe,
                        "-i", hlsUrl,
                        "-f", "rawvideo",
                        "-pix_fmt", "bgr24",
                        "-r", "25",
                        "pipe:1"
                );
                pb.redirectErrorStream(false);
                ffmpegPreviewProcess = pb.start();

                // Drain stderr silencieusement
                new Thread(() -> {
                    try (BufferedReader err = new BufferedReader(
                            new InputStreamReader(ffmpegPreviewProcess.getErrorStream()))) {
                        while (err.readLine() != null) {} // ignore
                    } catch (Exception ignored) {}
                }, "ffmpeg-preview-err").start();

                int frameSize     = width * height * 3;
                byte[] frameBuffer = new byte[frameSize];
                byte[] rgbBuffer   = new byte[frameSize];
                var is             = ffmpegPreviewProcess.getInputStream();

                while (ffmpegPreviewProcess != null && ffmpegPreviewProcess.isAlive()) {
                    // Lit exactement un frame complet
                    int read = 0;
                    while (read < frameSize) {
                        int r = is.read(frameBuffer, read, frameSize - read);
                        if (r < 0) break;
                        read += r;
                    }
                    if (read < frameSize) break;

                    // BGR → RGB
                    for (int i = 0; i < width * height; i++) {
                        rgbBuffer[i * 3]     = frameBuffer[i * 3 + 2]; // R
                        rgbBuffer[i * 3 + 1] = frameBuffer[i * 3 + 1]; // G
                        rgbBuffer[i * 3 + 2] = frameBuffer[i * 3];     // B
                    }

                    // Copie pour le lambda
                    byte[] copy = rgbBuffer.clone();
                    final int w = width, h = height;

                    Platform.runLater(() -> {
                        WritableImage img = new WritableImage(w, h);
                        img.getPixelWriter().setPixels(
                                0, 0, w, h,
                                PixelFormat.getByteRgbInstance(),
                                copy, 0, w * 3);
                        imageView.setImage(img);
                    });
                }

            } catch (Exception e) {
                System.err.println("[LiveWatch] Preview error: " + e.getMessage());
            }
        }, "live-preview").start();
    }

    // ── Stop preview ──────────────────────────────────────────────────────────

    private void stopLivePreview() {
        if (ffmpegPreviewProcess != null && ffmpegPreviewProcess.isAlive()) {
            ffmpegPreviewProcess.destroyForcibly();
            ffmpegPreviewProcess = null;
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void handleJoin() {
        try {
            boolean granted = liveService.grantPaidAccess(user, live);
            if (!granted) {
                showFlash("error", live.isEnded()
                        ? "This live stream has ended."
                        : "You do not have enough EyeTwin Coins.");
                return;
            }
            hasAccess = true;
            SessionManager.setCurrentUser(user);
            SessionManager.setPendingFlash("success", "Access granted. Enjoy the live stream.");
            render();
            String[] flash = SessionManager.consumeFlash();
            if (flash != null) showFlash(flash[0], flash[1]);
        } catch (SQLException e) {
            showFlash("error", "Unable to grant access.");
        }
    }

    @FXML
    private void goBack() {
        stopLivePreview();
        navigateTo("Live.fxml");
    }

    @FXML
    private void goToCoins() {
        navigateTo("Coins.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showFlash(String type, String message) {
        if (flashBox == null || flashLabel == null) return;
        String color = switch (type) {
            case "success" -> "#70d0a0";
            case "error"   -> "#ff8a8a";
            default        -> "#9eb1ff";
        };
        flashLabel.setText(message);
        flashLabel.setStyle("-fx-text-fill: " + color + ";");
        flashBox.setVisible(true);
        flashBox.setManaged(true);
    }

    private void navigateTo(String fxml) {
        stopLivePreview();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null)
                newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[LiveWatchController] Navigation error: " + fxml);
        }
    }
}