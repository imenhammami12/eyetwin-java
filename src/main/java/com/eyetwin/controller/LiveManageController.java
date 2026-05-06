package com.eyetwin.controller;

import java.util.concurrent.CountDownLatch;
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
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

// VLCJ imports commented out - VLCJ is an unnamed module not accessible via module-info
// import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
// import uk.co.caprica.vlcj.player.base.MediaPlayer;
// import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
// import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
// import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
// import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
// import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
// import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
// import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import java.nio.ByteBuffer;


public class LiveManageController {

    // ── FXML injections ───────────────────────────────────────────────────────
    @FXML private NavbarController navbarController;
    @FXML private Label   titleLabel;
    @FXML private Label   statusLabel;
    @FXML private Label   descriptionLabel;
    @FXML private Label   viewersLabel;
    @FXML private Label   priceLabel;
    @FXML private Label   startedAtLabel;
    @FXML private Label   shareLinkLabel;
    @FXML private Label   streamKeyLabel;
    @FXML private Label   revenueLabel;
    @FXML private Button  startButton;
    @FXML private Button  endButton;
    @FXML private VBox    previewBox;
    @FXML private VBox    flashBox;
    @FXML private Label   flashLabel;

    // Copy fields
    @FXML private TextField rtmpField;
    @FXML private TextField streamKeyField;
    @FXML private Button    copyRtmpButton;
    @FXML private Button    copyKeyButton;

    // FFmpeg streaming controls
    @FXML private VBox     ffmpegBox;
    @FXML private Button   ffmpegStartButton;
    @FXML private Button   ffmpegStopButton;
    @FXML private Label    ffmpegStatusLabel;
    @FXML private TextArea ffmpegLogArea;

    // ── Capture mode controls (new) ───────────────────────────────────────────
    @FXML private ToggleGroup captureToggleGroup;
    @FXML private ToggleButton modeScreen;
    @FXML private ToggleButton modeWebcam;
    @FXML private ToggleButton modeFile;
    @FXML private HBox    filePickerBox;
    @FXML private Label   selectedFileLabel;
    // ─────────────────────────────────────────────────────────────────────────

    // ── Ajouter ces champs dans la classe ────────────────────────────────────
    // VLCJ fields commented out - VLCJ is an unnamed module not accessible
    // private MediaPlayerFactory    vlcFactory;
    // private EmbeddedMediaPlayer   vlcPlayer;
    private PixelBuffer<ByteBuffer> pixelBuffer;
    private WritableImage           vlcImage;
    private ImageView               vlcImageView;

    private static final String RTMP_SERVER = "rtmp://localhost:1935";

    private final ILiveStreamService     liveService = new LiveStreamServiceImpl();
    private final DateTimeFormatter      fmt         = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final FFmpegStreamingService ffmpeg      = new FFmpegStreamingService();

    private LiveStream  live;
    private File        selectedVideoFile;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("live");
        hideFlash();
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        // Wire FFmpeg callbacks
        ffmpeg.setOnLog(this::appendLog);
        ffmpeg.setOnError(msg -> Platform.runLater(() -> showFlash("error", msg)));
        ffmpeg.setOnStarted(() -> Platform.runLater(() -> {
            setFfmpegRunning(true);
            showFlash("success", "Desktop capture is streaming ✔");
            startPreviewPlayer();
        }));
        ffmpeg.setOnStopped(() -> Platform.runLater(() -> {
            setFfmpegRunning(false);
            stopPreviewPlayer();
        }));

        // Wire capture mode toggle
        if (captureToggleGroup != null) {
            captureToggleGroup.selectedToggleProperty().addListener((obs, old, neu) -> {
                if (neu == null) { captureToggleGroup.selectToggle(old); return; }
                boolean isFile = neu == modeFile;
                if (filePickerBox != null) {
                    filePickerBox.setVisible(isFile);
                    filePickerBox.setManaged(isFile);
                }
            });
        }

        LiveStream selected = SessionManager.getSelectedLiveStream();
        User user = SessionManager.getCurrentUser();
        if (selected == null || user == null) {
            SessionManager.setPendingFlash("error", "No live stream selected.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }

        try {
            live = liveService.getById(selected.getId());
            if (live == null || live.getCoach() == null
                    || live.getCoach().getId() != user.getId()) {
                SessionManager.setPendingFlash("error", "You cannot manage this live stream.");
                Platform.runLater(() -> navigateTo("Live.fxml"));
                return;
            }
        } catch (SQLException e) {
            SessionManager.setPendingFlash("error", "Unable to load live stream.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }

        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlash(flash[0], flash[1]);

        render();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void render() {
        if (live == null) return;

        titleLabel.setText(live.getTitle());
        statusLabel.setText(live.isLive() ? "LIVE" : live.isEnded() ? "ENDED" : "SCHEDULED");
        descriptionLabel.setText(
                live.getDescription() == null || live.getDescription().isBlank()
                        ? "No description provided." : live.getDescription());
        viewersLabel.setText(String.valueOf(live.getAccessCount()));
        priceLabel.setText(live.getCoinPrice() == 0 ? "Free" : live.getCoinPrice() + " coins");
        startedAtLabel.setText(live.getStartedAt() == null ? "—" : fmt.format(live.getStartedAt()));
        shareLinkLabel.setText("Share from EyeTwin app via Live page");
        revenueLabel.setText(live.getRevenueCoins() + " coins");

        if (rtmpField      != null) rtmpField.setText(RTMP_SERVER);
        if (streamKeyField != null) streamKeyField.setText(live.getStreamKey());
        if (streamKeyLabel != null) streamKeyLabel.setText(live.getStreamKey());

        setVisible(startButton, !live.isLive() && !live.isEnded());
        setVisible(endButton,    live.isLive());
        setVisible(ffmpegBox,    live.isLive());
        setVisible(previewBox,   live.isLive());

        if (live.isLive() && ffmpeg.isRunning()) startPreviewPlayer();
    }

    // ── Stream control ────────────────────────────────────────────────────────

    @FXML
    private void handleStart() {
        try {
            if (liveService.startStream(live.getId(), SessionManager.getCurrentUser())) {
                live = liveService.getById(live.getId());
                SessionManager.setSelectedLiveStream(live);
                showFlash("success", "Stream is LIVE — choose a capture mode and click Start.");
                render();
            }
        } catch (SQLException e) { showFlash("error", "Unable to start stream."); }
    }

    @FXML
    private void handleEnd() {
        if (ffmpeg.isRunning()) ffmpeg.stop();
        stopPreviewPlayer();
        try {
            if (liveService.endStream(live.getId(), SessionManager.getCurrentUser())) {
                SessionManager.setPendingFlash("info", "Your live stream has ended.");
                navigateTo("Live.fxml");
            }
        } catch (SQLException e) { showFlash("error", "Unable to end stream."); }
    }

    // ── Capture mode ──────────────────────────────────────────────────────────

    @FXML
    private void handlePickFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select video file");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video files", "*.mp4", "*.mkv", "*.avi", "*.mov"));
        File f = fc.showOpenDialog(titleLabel.getScene().getWindow());
        if (f != null) {
            selectedVideoFile = f;
            if (selectedFileLabel != null)
                selectedFileLabel.setText(f.getName());
        }
    }

    @FXML
    private void handleFfmpegStart() {
        if (live == null) return;
        if (ffmpeg.isRunning()) { showFlash("info", "Capture already running."); return; }

        ffmpeg.setFrameRate(30);
        ffmpeg.setResolution("1920x1080");
        ffmpeg.setVideoBitrate("2500k");
        ffmpeg.setPreset("ultrafast");

        // Detect selected mode
        Toggle selected = captureToggleGroup != null ? captureToggleGroup.getSelectedToggle() : null;

        if (selected == modeWebcam) {
            ffmpeg.setCaptureScreen(false);
            ffmpeg.setCaptureAudio(true);
            ffmpeg.setCaptureWebcam(true);
            ffmpeg.setCaptureFile(null);
            appendLog("Launching webcam capture…");
        } else if (selected == modeFile) {
            if (selectedVideoFile == null) { showFlash("error", "Please select a video file first."); return; }
            ffmpeg.setCaptureScreen(false);
            ffmpeg.setCaptureAudio(false);
            ffmpeg.setCaptureWebcam(false);
            ffmpeg.setCaptureFile(selectedVideoFile.getAbsolutePath());
            appendLog("Streaming file: " + selectedVideoFile.getName());
        } else {
            // Default: screen
            ffmpeg.setCaptureScreen(true);
            ffmpeg.setCaptureAudio(true);
            ffmpeg.setCaptureWebcam(false);
            ffmpeg.setCaptureFile(null);
            appendLog("Launching desktop capture…");
        }

        ffmpeg.start(live.getStreamKey());
    }

    @FXML
    private void handleFfmpegStop() {
        ffmpeg.stop();
        stopPreviewPlayer();
        showFlash("info", "Capture stopped.");
    }

    // ── Preview (MediaPlayer) ─────────────────────────────────────────────────

    private void startPreviewPlayer() {
        // Ouvre le preview dans le navigateur par défaut
        String url = "http://127.0.0.1:8888/live/" + live.getStreamKey();
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("[Preview] Cannot open browser: " + e.getMessage());
        }

        // Affiche un message dans le previewBox
        Platform.runLater(() -> {
            if (previewBox != null) {
                while (previewBox.getChildren().size() > 1)
                    previewBox.getChildren().remove(1);
                Label msg = new Label("🔴 Stream en direct — aperçu ouvert dans le navigateur");
                msg.setStyle("-fx-text-fill: #00e676; -fx-font-size: 13; -fx-padding: 20;");
                previewBox.getChildren().add(msg);
            }
        });
    }

    private void stopPreviewPlayer() {
        if (previewBox != null)
            Platform.runLater(() -> {
                while (previewBox.getChildren().size() > 1)
                    previewBox.getChildren().remove(1);
            });
    }


    // ── Copy helpers ──────────────────────────────────────────────────────────

    @FXML private void copyRtmp()      { copyToClipboard(RTMP_SERVER, copyRtmpButton); }
    @FXML private void copyStreamKey() { if (live != null) copyToClipboard(live.getStreamKey(), copyKeyButton); }

    private void copyToClipboard(String text, Button btn) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
        if (btn != null) {
            String orig = btn.getText();
            btn.setText("✔ Copied!");
            btn.setDisable(true);
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> { btn.setText(orig); btn.setDisable(false); });
            }).start();
        }
        showFlash("success", "Copied to clipboard!");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void openViewer() { SessionManager.setSelectedLiveStream(live); navigateTo("LiveWatch.fxml"); }
    @FXML private void goBack()     { navigateTo("Live.fxml"); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setFfmpegRunning(boolean running) {
        setVisible(ffmpegStartButton, !running);
        setVisible(ffmpegStopButton,   running);
        if (ffmpegStatusLabel != null) {
            ffmpegStatusLabel.setText(running ? "● Streaming…" : "○ Idle");
            ffmpegStatusLabel.setStyle("-fx-text-fill: "
                    + (running ? "#00e676" : "rgba(255,255,255,0.45)")
                    + "; -fx-font-weight: 800;");
        }
    }

    private void appendLog(String line) {
        if (ffmpegLogArea == null) return;
        Platform.runLater(() -> {
            ffmpegLogArea.appendText(line + "\n");
            String[] lines = ffmpegLogArea.getText().split("\n");
            if (lines.length > 200)
                ffmpegLogArea.setText(
                        String.join("\n", Arrays.copyOfRange(lines, lines.length - 200, lines.length)));
        });
    }

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

    private void hideFlash() {
        if (flashBox == null) return;
        flashBox.setVisible(false);
        flashBox.setManaged(false);
    }

    private static void setVisible(javafx.scene.Node node, boolean v) {
        if (node == null) return;
        node.setVisible(v);
        node.setManaged(v);
    }

    private void navigateTo(String fxml) {
        stopPreviewPlayer();
        if (ffmpeg.isRunning()) ffmpeg.stop();
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage    = (Stage) titleLabel.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null)
                newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[LiveManageController] Navigation error: " + fxml);
            e.printStackTrace();
        }
    }

}
