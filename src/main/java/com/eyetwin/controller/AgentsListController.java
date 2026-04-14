package com.eyetwin.controller;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.repository.AgentRepository;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

public class AgentsListController {

    @FXML private NavbarController navbarController;
    @FXML private Label gameTitleLabel;
    @FXML private TextField searchField;
    @FXML private VBox agentsList;
    @FXML private Label agentsTotalLabel;
    @FXML private Label agentsVisibleLabel;
    @FXML private Label resultsNote;

    private final AgentRepository agentRepository = new AgentRepository();
    private Game currentGame;
    private List<Agent> allAgents;

    // ═══════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════
    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("guides");
    }

    /** Called by NavbarController after navigation with data */
    public void initData(Game game) {
        this.currentGame = game;
        if (gameTitleLabel != null) gameTitleLabel.setText(game.getName());
        new Thread(this::loadAgents).start();
    }

    // ═══════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════
    private void loadAgents() {
        try {
            allAgents = agentRepository.findByGame(currentGame);
            Platform.runLater(() -> {
                renderAgents(allAgents);
                updateStats(allAgents.size(), allAgents.size());
                bindSearch();
            });
        } catch (Exception e) {
            System.err.println("[AgentsListController] Error loading agents: " + e.getMessage());
        }
    }

    private void bindSearch() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase();
            List<Agent> filtered = allAgents.stream()
                    .filter(a -> q.isEmpty() || a.getName().toLowerCase().contains(q))
                    .toList();
            renderAgents(filtered);
            updateStats(allAgents.size(), filtered.size());
        });
    }

    private void updateStats(int total, int visible) {
        if (agentsTotalLabel != null) agentsTotalLabel.setText(String.valueOf(total));
        if (agentsVisibleLabel != null) agentsVisibleLabel.setText(String.valueOf(visible));
        if (resultsNote != null) resultsNote.setText(visible + " agent(s) affiché(s)");
    }

    // ═══════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════
    private void renderAgents(List<Agent> agents) {
        agentsList.getChildren().clear();
        for (int i = 0; i < agents.size(); i++) {
            HBox row = buildAgentRow(agents.get(i));
            row.setOpacity(0);
            row.setTranslateX(-20);
            int delay = i * 60;
            Timeline anim = new Timeline(
                    new KeyFrame(Duration.millis(300 + delay),
                            new KeyValue(row.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(row.translateXProperty(), 0, Interpolator.EASE_OUT))
            );
            anim.play();
            agentsList.getChildren().add(row);
        }
    }

    private HBox buildAgentRow(Agent agent) {
        HBox row = new HBox(25);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20, 25, 20, 25));
        row.setStyle(
                "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.7),rgba(11,17,31,0.7));" +
                "-fx-border-color: rgba(255,0,0,0.12);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;" +
                "-fx-cursor: hand;"
        );
        row.setEffect(new DropShadow(20, Color.web("#000000", 0.3)));

        // Agent image
        StackPane imgWrap = new StackPane();
        imgWrap.setPrefSize(75, 75);
        imgWrap.setMinSize(75, 75);
        imgWrap.setMaxSize(75, 75);
        imgWrap.setStyle(
                "-fx-border-color: rgba(76,211,227,0.35);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-background-color: rgba(0,0,0,0.5);"
        );
        Rectangle clip = new Rectangle(75, 75);
        clip.setArcWidth(12); clip.setArcHeight(12);
        ImageView img = new ImageView();
        img.setFitWidth(75); img.setFitHeight(75);
        img.setPreserveRatio(false); img.setSmooth(true);
        img.setClip(clip);
        if (agent.getImage() != null && !agent.getImage().isEmpty()) {
            try { img.setImage(new Image(agent.getImage(), true)); } catch (Exception ignored) {}
        }
        imgWrap.getChildren().add(img);

        // Agent name
        Label nameLabel = new Label(agent.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold; -fx-font-family: 'Arial Black';");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Guides button
        Button guidesBtn = new Button("Guides  →");
        guidesBtn.setStyle(
                "-fx-background-color: linear-gradient(135deg,rgba(76,211,227,0.12),rgba(76,211,227,0.06));" +
                "-fx-border-color: rgba(76,211,227,0.35);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #4cd3e3;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 11;" +
                "-fx-padding: 10 22 10 22;" +
                "-fx-cursor: hand;"
        );
        guidesBtn.setOnAction(e -> navigateToAgent(agent));

        row.getChildren().addAll(imgWrap, nameLabel, guidesBtn);

        // Hover
        row.setOnMouseEntered(e -> {
            row.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.9),rgba(11,17,31,0.9));" +
                    "-fx-border-color: #ff0000;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;" +
                    "-fx-cursor: hand;"
            );
            row.setEffect(new DropShadow(35, Color.web("#ff0000", 0.2)));
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(250),
                    new KeyValue(row.translateXProperty(), 10, Interpolator.EASE_OUT)));
            tl.play();
            guidesBtn.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(76,211,227,0.25),rgba(76,211,227,0.12));" +
                    "-fx-border-color: #ff0000;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;" +
                    "-fx-text-fill: #ff0000;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 11;" +
                    "-fx-padding: 10 22 10 22;" +
                    "-fx-cursor: hand;"
            );
        });
        row.setOnMouseExited(e -> {
            row.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.7),rgba(11,17,31,0.7));" +
                    "-fx-border-color: rgba(255,0,0,0.12);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;" +
                    "-fx-cursor: hand;"
            );
            row.setEffect(new DropShadow(20, Color.web("#000000", 0.3)));
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(250),
                    new KeyValue(row.translateXProperty(), 0, Interpolator.EASE_OUT)));
            tl.play();
            guidesBtn.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(76,211,227,0.12),rgba(76,211,227,0.06));" +
                    "-fx-border-color: rgba(76,211,227,0.35);" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;" +
                    "-fx-text-fill: #4cd3e3;" +
                    "-fx-font-weight: bold;" +
                    "-fx-font-size: 11;" +
                    "-fx-padding: 10 22 10 22;" +
                    "-fx-cursor: hand;"
            );
        });

        return row;
    }

    // ═══════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════
    private void navigateToAgent(Agent agent) {
        navbarController.navigateToWithData("AgentVideos.fxml", "gameAndAgent",
                new Object[]{currentGame, agent});
    }

    @FXML public void goBack() {
        navbarController.navigateTo("GamesSelection.fxml");
    }
}
