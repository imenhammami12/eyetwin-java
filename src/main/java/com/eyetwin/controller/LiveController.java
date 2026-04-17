package com.eyetwin.controller;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ILiveStreamService;
import com.eyetwin.services.LiveStreamServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class LiveController {

    @FXML private NavbarController navbarController;
    @FXML private VBox flashBox;
    @FXML private Label flashLabel;
    @FXML private Label totalStreamsLabel;
    @FXML private Label liveNowLabel;
    @FXML private Label comingSoonLabel;
    @FXML private Label freeStreamsLabel;
    @FXML private HBox coachActions;
    @FXML private VBox emptyStateBox;
    @FXML private FlowPane liveGrid;

    private final ILiveStreamService liveService = new LiveStreamServiceImpl();

    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("live");

        User user = SessionManager.getCurrentUser();
        if (coachActions != null) {
            boolean isCoach = user != null && user.isCoach();
            coachActions.setVisible(isCoach);
            coachActions.setManaged(isCoach);
        }

        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlash(flash[0], flash[1]);

        loadStreams();
    }

    private void loadStreams() {
        try {
            List<LiveStream> lives = liveService.getAvailableStreams();
            updateStats(lives);
            renderLives(lives);
        } catch (SQLException e) {
            showFlash("error", "Unable to load live streams.");
        }
    }

    private void updateStats(List<LiveStream> lives) {
        long liveCount = lives.stream().filter(LiveStream::isLive).count();
        long freeCount = lives.stream().filter(l -> l.getCoinPrice() == 0).count();
        if (totalStreamsLabel != null) totalStreamsLabel.setText(String.valueOf(lives.size()));
        if (liveNowLabel != null) liveNowLabel.setText(String.valueOf(liveCount));
        if (comingSoonLabel != null) comingSoonLabel.setText(String.valueOf(Math.max(0, lives.size() - liveCount)));
        if (freeStreamsLabel != null) freeStreamsLabel.setText(String.valueOf(freeCount));
    }

    private void renderLives(List<LiveStream> lives) {
        if (liveGrid == null || emptyStateBox == null) return;
        liveGrid.getChildren().clear();
        boolean empty = lives == null || lives.isEmpty();
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        if (empty) return;

        for (LiveStream live : lives) {
            liveGrid.getChildren().add(buildCard(live));
        }
    }

    private VBox buildCard(LiveStream live) {
        VBox card = new VBox();
        card.setPrefWidth(320);
        card.getStyleClass().addAll("live-card", live.isLive() ? "live-card-live" : "live-card-soon");

        // status pill (top-right in twig; here top-left for simplicity)
        Label pill = new Label(live.isLive() ? "LIVE" : "SOON");
        pill.getStyleClass().addAll("live-pill", live.isLive() ? "live-pill-live" : "live-pill-soon");

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("live-thumb");
        Label icon = new Label("▶");
        icon.getStyleClass().add("live-thumb-icon");
        StackPane.setAlignment(pill, Pos.TOP_RIGHT);
        StackPane.setMargin(pill, new Insets(10, 10, 0, 0));
        thumb.getChildren().addAll(icon, pill);

        VBox body = new VBox(6);
        body.getStyleClass().add("live-body");

        Label title = new Label(live.getTitle());
        title.getStyleClass().add("live-title");

        String coachName = live.getCoach() != null
                ? (live.getCoach().getUsername() != null ? live.getCoach().getUsername() : "—")
                : "—";
        Label coach = new Label("@" + coachName);
        coach.getStyleClass().add("live-meta");

        Label desc = new Label(live.getDescription() != null && !live.getDescription().isBlank()
                ? shorten(live.getDescription(), 90)
                : "");
        desc.setWrapText(true);
        desc.getStyleClass().add("live-desc");
        desc.setManaged(desc.getText() != null && !desc.getText().isBlank());
        desc.setVisible(desc.isManaged());

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("live-footer");

        Label price = new Label(live.getCoinPrice() == 0 ? "FREE" : live.getCoinPrice() + " coins");
        price.getStyleClass().addAll("live-price", live.getCoinPrice() == 0 ? "live-price-free" : "");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button watchBtn = new Button(live.isLive() ? "Watch Now" : "Preview");
        watchBtn.getStyleClass().addAll("live-watch-btn", live.isLive() ? "live-watch-btn-live" : "");
        watchBtn.setOnAction(e -> openWatch(live));

        footer.getChildren().addAll(price, spacer, watchBtn);

        body.getChildren().addAll(title, coach);
        if (desc.isManaged()) body.getChildren().add(desc);
        body.getChildren().add(footer);

        card.getChildren().addAll(thumb, body);
        return card;
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        String clean = text.trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }

    private void showFlash(String type, String message) {
        if (flashBox == null || flashLabel == null) return;
        String border = switch (type) {
            case "success" -> "#3a8a60";
            case "error" -> "#b02b20";
            default -> "#667eea";
        };
        String color = switch (type) {
            case "success" -> "#70d0a0";
            case "error" -> "#ff8a8a";
            default -> "#9eb1ff";
        };
        flashLabel.setText(message);
        flashLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13;");
        flashBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 0 0 0 3;" +
                "-fx-background-radius: 8; -fx-padding: 12 16 12 16;"
        );
        flashBox.setVisible(true);
        flashBox.setManaged(true);
    }

    private void openWatch(LiveStream live) {
        SessionManager.setSelectedLiveStream(live);
        navigateTo("LiveWatch.fxml");
    }

    @FXML
    private void goToCreate() {
        if (!SessionManager.isCoach()) {
            showFlash("error", "Only coaches can create a live stream.");
            return;
        }
        navigateTo("LiveCreate.fxml");
    }

    @FXML
    private void goToMyStreams() {
        navigateTo("MyLiveStreams.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage = (Stage) liveGrid.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null) newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[LiveController] Navigation error: " + fxml);
        }
    }
}
