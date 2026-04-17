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
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

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
                ? "No description provided."
                : live.getDescription());

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
                playerWebView.getEngine().loadContent("<html><body style='background:#080810;color:white;font-family:Arial;padding:24px'>Stream is not live yet.</body></html>");
            }
        } else {
            int missing = Math.max(0, live.getCoinPrice() - user.getCoinBalance());
            userBalanceLabel.setText(user.getCoinBalance() + " coins");
            neededCoinsLabel.setText(missing == 0 ? "You can join now." : "Need " + missing + " more coins.");
            joinButton.setDisable(missing > 0 || live.isEnded());
        }
    }

    private void loadPlayer() {
        String hlsUrl = "http://127.0.0.1:8888/" + live.getStreamKey() + "/index.m3u8";
        String html = """
                <html><head><style>body{margin:0;background:#000}video{width:100%%;height:100%%;object-fit:cover}</style></head>
                <body>
                <video id='video' controls autoplay muted></video>
                <script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script>
                <script>
                const video=document.getElementById('video');const src='%s';
                if(window.Hls&&Hls.isSupported()){const hls=new Hls({lowLatencyMode:true,liveSyncDurationCount:2,liveMaxLatencyDurationCount:4});
                hls.loadSource(src);hls.attachMedia(video);hls.on(Hls.Events.MANIFEST_PARSED,()=>video.play());}
                else{video.src=src;video.play();}
                </script></body></html>
                """.formatted(hlsUrl);
        playerWebView.getEngine().loadContent(html);
    }

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
        navigateTo("Live.fxml");
    }

    @FXML
    private void goToCoins() {
        navigateTo("Coins.fxml");
    }

    private void showFlash(String type, String message) {
        if (flashBox == null || flashLabel == null) return;
        String color = switch (type) {
            case "success" -> "#70d0a0";
            case "error" -> "#ff8a8a";
            default -> "#9eb1ff";
        };
        flashLabel.setText(message);
        flashLabel.setStyle("-fx-text-fill: " + color + ";");
        flashBox.setVisible(true);
        flashBox.setManaged(true);
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null) newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[LiveWatchController] Navigation error: " + fxml);
        }
    }
}
