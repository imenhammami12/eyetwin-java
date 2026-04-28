package com.eyetwin.controller;

import com.eyetwin.entities.Community.GiphyGif;
import com.eyetwin.services.Community.GiphyApiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class GiphyPickerController {

    @FXML private TextField tfSearch;
    @FXML private Button btnSearch;
    @FXML private Label lblStatus;
    @FXML private TilePane tileResults;

    private final GiphyApiService giphyApiService = new GiphyApiService();
    private Consumer<GiphyGif> onGifSelected;

    public void setOnGifSelected(Consumer<GiphyGif> onGifSelected) {
        this.onGifSelected = onGifSelected;
    }

    @FXML
    public void initialize() {
        searchGifs("gaming");
    }

    @FXML
    private void handleSearch() {
        searchGifs(tfSearch.getText());
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tfSearch.getScene().getWindow();
        stage.close();
    }

    private void searchGifs(String query) {
        lblStatus.setText("Searching...");
        tileResults.getChildren().clear();

        Task<List<GiphyGif>> task = new Task<>() {
            @Override
            protected List<GiphyGif> call() throws Exception {
                return giphyApiService.searchGifs(query, 18);
            }
        };

        task.setOnSucceeded(event -> {
            List<GiphyGif> gifs = task.getValue();
            lblStatus.setText(gifs.isEmpty() ? "No GIFs found." : "Select a GIF");
            renderResults(gifs);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            lblStatus.setText("Failed to load GIFs.");
            ex.printStackTrace();
        });

        Thread thread = new Thread(task, "giphy-search");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderResults(List<GiphyGif> gifs) {
        tileResults.getChildren().clear();

        for (GiphyGif gif : gifs) {
            tileResults.getChildren().add(buildGifCard(gif));
        }
    }

    private VBox buildGifCard(GiphyGif gif) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8));
        card.setPrefWidth(180);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        ImageView preview = new ImageView(new Image(gif.getPreviewUrl(), true));
        preview.setFitWidth(160);
        preview.setFitHeight(120);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setStyle("-fx-cursor: hand;");
        preview.setOnMouseClicked(e -> selectGif(gif));

        Label title = new Label(
                gif.getTitle() == null || gif.getTitle().isBlank() ? "GIF" : gif.getTitle()
        );
        title.setWrapText(true);
        title.setMaxWidth(160);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");

        Button btnUse = new Button("Use GIF");
        btnUse.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        btnUse.setOnAction(e -> selectGif(gif));

        card.getChildren().addAll(preview, title, btnUse);
        return card;
    }

    private void selectGif(GiphyGif gif) {
        if (onGifSelected != null) {
            onGifSelected.accept(gif);
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) tfSearch.getScene().getWindow();
            stage.close();
        });
    }
}