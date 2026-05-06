package com.eyetwin.controller;

import com.eyetwin.entities.Game;
import com.eyetwin.repository.GameRepository;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.List;

public class GamesSelectionController {

    @FXML private NavbarController navbarController;
    @FXML private TextField searchField;
    @FXML private FlowPane gamesGrid;
    @FXML private Label totalGamesLabel;
    @FXML private Label visibleGamesLabel;
    @FXML private Label resultsNote;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox rootVBox;

    private final GameRepository gameRepository = new GameRepository();
    private List<Game> allGames;

    // ═══════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════
    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("guides");
        new Thread(this::loadGames).start();
    }

    // ═══════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════
    private void loadGames() {
        try {
            allGames = gameRepository.findAllOrderedByName();
            Platform.runLater(() -> {
                renderGames(allGames);
                updateStats(allGames.size(), allGames.size());
                bindSearch();
            });
        } catch (Exception e) {
            System.err.println("[GamesSelectionController] Error loading games: " + e.getMessage());
        }
    }

    private void bindSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        long shown = allGames.stream().filter(g ->
                q.isEmpty() || g.getName().toLowerCase().contains(q)
                        || (g.getDescription() != null && g.getDescription().toLowerCase().contains(q))
        ).count();

        renderGames(allGames.stream().filter(g ->
                q.isEmpty() || g.getName().toLowerCase().contains(q)
                        || (g.getDescription() != null && g.getDescription().toLowerCase().contains(q))
        ).toList());

        updateStats(allGames.size(), (int) shown);
    }

    private void updateStats(int total, int visible) {
        if (totalGamesLabel != null) totalGamesLabel.setText(String.valueOf(total));
        if (visibleGamesLabel != null) visibleGamesLabel.setText(String.valueOf(visible));
        if (resultsNote != null) resultsNote.setText(visible + " résultat(s)");
    }

    // ═══════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════
    private void renderGames(List<Game> games) {
        gamesGrid.getChildren().clear();
        for (int i = 0; i < games.size(); i++) {
            VBox card = buildGameCard(games.get(i));
            // staggered entrance animation
            card.setOpacity(0);
            card.setTranslateY(30);
            int delay = i * 80;
            Timeline anim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(card.opacityProperty(), 0),
                            new KeyValue(card.translateYProperty(), 30)),
                    new KeyFrame(Duration.millis(400 + delay),
                            new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT))
            );
            anim.play();
            gamesGrid.getChildren().add(card);
        }
    }

    private VBox buildGameCard(Game game) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle(
                "-fx-background-color: linear-gradient(135deg, rgba(20,25,45,0.9) 0%, rgba(15,19,35,0.9) 100%);" +
                "-fx-border-color: rgba(255,255,255,0.08);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-cursor: hand;"
        );
        card.setEffect(new DropShadow(30, Color.web("#000000", 0.4)));

        // ── Image Container ──
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(200);
        imgContainer.setMaxWidth(300);
        imgContainer.setStyle(
                "-fx-background-color: linear-gradient(135deg, rgba(0,0,0,0.4), rgba(0,0,0,0.2));" +
                "-fx-background-radius: 20 20 0 0;"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        if (game.getIcon() != null && !game.getIcon().isEmpty()) {
            try {
                imageView.setImage(new Image(game.getIcon(), true));
            } catch (Exception ignored) {}
        }

        imgContainer.getChildren().add(imageView);

        // ── Info Section ──
        VBox info = new VBox(12);
        info.setPadding(new Insets(24, 24, 24, 24));

        Label nameLabel = new Label(game.getName());
        nameLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Arial Black';"
        );
        nameLabel.setWrapText(true);

        Label descLabel = new Label(game.getDescription() != null ? game.getDescription() : "");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 13;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(55);

        Button btn = new Button("Accéder aux Guides  →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: linear-gradient(135deg, rgba(255,8,68,0.2), rgba(0,217,255,0.2));" +
                "-fx-border-color: rgba(255,8,68,0.4);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 12;" +
                "-fx-padding: 13 0 13 0;" +
                "-fx-cursor: hand;"
        );
        btn.setOnAction(e -> navigateToGame(game));

        info.getChildren().addAll(nameLabel, descLabel, btn);
        card.getChildren().addAll(imgContainer, info);

        // ── Hover Effects ──
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg, rgba(20,25,45,0.95) 0%, rgba(15,19,35,0.95) 100%);" +
                    "-fx-border-color: rgba(255,8,68,0.6);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 20;" +
                    "-fx-background-radius: 20;" +
                    "-fx-cursor: hand;"
            );
            card.setEffect(new DropShadow(50, Color.web("#ff0844", 0.3)));
            animateTranslate(card, 0, -10, 200);
            nameLabel.setStyle(
                    "-fx-background-color: linear-gradient(135deg,#ff0844,#00d9ff);" +
                    "-fx-text-fill: #00d9ff; -fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Arial Black';"
            );
            btn.setStyle(
                    "-fx-background-color: linear-gradient(135deg, #ff0844, #00d9ff);" +
                    "-fx-border-color: #00d9ff;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 13 0 13 0;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,217,255,0.4),14,0,0,4);"
            );
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg, rgba(20,25,45,0.9) 0%, rgba(15,19,35,0.9) 100%);" +
                    "-fx-border-color: rgba(255,255,255,0.08);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 20;" +
                    "-fx-background-radius: 20;" +
                    "-fx-cursor: hand;"
            );
            card.setEffect(new DropShadow(30, Color.web("#000000", 0.4)));
            animateTranslate(card, 0, 0, 200);
            nameLabel.setStyle(
                    "-fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Arial Black';"
            );
            btn.setStyle(
                    "-fx-background-color: linear-gradient(135deg, rgba(255,8,68,0.2), rgba(0,217,255,0.2));" +
                    "-fx-border-color: rgba(255,8,68,0.4);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 12;" +
                    "-fx-padding: 13 0 13 0;" +
                    "-fx-cursor: hand;"
            );
        });

        return card;
    }

    // ═══════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════
    private void navigateToGame(Game game) {
        // Pass the selected game to AgentsListController and navigate
        navbarController.navigateToWithData("AgentsList.fxml", "game", game);
    }

    @FXML public void goToUpload() {
        navbarController.navigateTo("GuideUpload.fxml");
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════
    private void animateTranslate(VBox node, double toX, double toY, int ms) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(ms),
                        new KeyValue(node.translateXProperty(), toX, Interpolator.EASE_BOTH),
                        new KeyValue(node.translateYProperty(), toY, Interpolator.EASE_BOTH))
        );
        tl.play();
    }
}
