package com.eyetwin.controller;

import com.eyetwin.entities.LiveStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ILiveStreamService;
import com.eyetwin.services.LiveStreamServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyLiveStreamsController {

    @FXML private NavbarController navbarController;
    @FXML private VBox streamListBox;
    @FXML private VBox emptyStateBox;

    private final ILiveStreamService liveService = new LiveStreamServiceImpl();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("live");
        if (!SessionManager.isCoach()) {
            SessionManager.setPendingFlash("error", "Only coaches can access this page.");
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }
        loadStreams();
    }

    private void loadStreams() {
        try {
            User user = SessionManager.getCurrentUser();
            List<LiveStream> streams = liveService.getStreamsByCoach(user);
            streamListBox.getChildren().clear();
            boolean empty = streams.isEmpty();
            emptyStateBox.setVisible(empty);
            emptyStateBox.setManaged(empty);
            streamListBox.setVisible(!empty);
            streamListBox.setManaged(!empty);

            for (LiveStream stream : streams) {
                streamListBox.getChildren().add(buildRow(stream));
            }
        } catch (SQLException e) {
            SessionManager.setPendingFlash("error", "Unable to load your streams.");
            navigateTo("Live.fxml");
        }
    }

    private HBox buildRow(LiveStream stream) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(14));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10;");

        Label title = new Label(stream.getTitle());
        title.setPrefWidth(240);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label status = new Label(stream.getStatus().toUpperCase());
        status.setPrefWidth(110);
        status.setStyle("-fx-text-fill: " + (stream.isLive() ? "#ff4d3d" : stream.isEnded() ? "#9a9a9a" : "#9eb1ff") + "; -fx-font-weight: bold;");

        Label price = new Label(stream.getCoinPrice() == 0 ? "Free" : stream.getCoinPrice() + " coins");
        price.setPrefWidth(110);
        price.setStyle("-fx-text-fill: #f6d860;");

        Label viewers = new Label(stream.getAccessCount() + " viewers");
        viewers.setPrefWidth(110);
        viewers.setStyle("-fx-text-fill: rgba(255,255,255,0.6);");

        Label created = new Label(stream.getCreatedAt() == null ? "—" : fmt.format(stream.getCreatedAt()));
        created.setStyle("-fx-text-fill: rgba(255,255,255,0.45);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manage = new Button(stream.isEnded() ? "View" : "Manage");
        manage.setStyle("-fx-background-color: rgba(102,126,234,0.18); -fx-text-fill: white; -fx-background-radius: 8;");
        manage.setOnAction(e -> {
            SessionManager.setSelectedLiveStream(stream);
            navigateTo("LiveManage.fxml");
        });

        row.getChildren().addAll(title, status, price, viewers, created, spacer, manage);
        return row;
    }

    @FXML
    private void goToCreate() {
        navigateTo("LiveCreate.fxml");
    }

    @FXML
    private void goBack() {
        navigateTo("Live.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/" + fxml));
            Stage stage = (Stage) streamListBox.getScene().getWindow();
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null) newScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[MyLiveStreamsController] Navigation error: " + fxml);
        }
    }
}
