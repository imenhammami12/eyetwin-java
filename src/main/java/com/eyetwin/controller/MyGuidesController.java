package com.eyetwin.controller;

import com.eyetwin.entities.GuideVideo;
import com.eyetwin.repository.GuideVideoRepository;
import com.eyetwin.tools.SessionManager;
import com.eyetwin.entities.User;
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

public class MyGuidesController {

    @FXML private NavbarController navbarController;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> gameFilter;
    @FXML private FlowPane guidesGrid;
    @FXML private Label totalLabel;
    @FXML private Label visibleLabel;
    @FXML private Label approvedLabel;
    @FXML private Label resultsNote;
    @FXML private VBox emptyState;
    @FXML private VBox statsAndGridSection;

    private final GuideVideoRepository guideVideoRepository = new GuideVideoRepository();
    private List<GuideVideo> allGuides;

    // ═══════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════
    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("guides");

        if (statusFilter != null) {
            statusFilter.getItems().setAll("All status", "approved", "pending", "rejected");
            statusFilter.getSelectionModel().selectFirst();
        }

        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showEmpty("Veuillez vous connecter pour voir vos guides.");
            return;
        }

        new Thread(this::loadMyGuides).start();
    }

    // ═══════════════════════════════════════════
    //  DATA
    // ═══════════════════════════════════════════
    private void loadMyGuides() {
        try {
            User user = SessionManager.getCurrentUser();
            allGuides = guideVideoRepository.findByUploader(user);
            Platform.runLater(() -> {
                if (allGuides.isEmpty()) {
                    showEmpty("Vous n'avez pas encore de guides. Commencez à partager!");
                    return;
                }
                populateGameFilter();
                applyFilters();
                bindListeners();
                if (statsAndGridSection != null) statsAndGridSection.setVisible(true);
                if (emptyState != null) emptyState.setVisible(false);
            });
        } catch (Exception e) {
            System.err.println("[MyGuidesController] Error: " + e.getMessage());
        }
    }

    private void populateGameFilter() {
        List<String> games = allGuides.stream()
                .map(g -> g.getGame() != null ? g.getGame().getName() : "")
                .distinct().filter(s -> !s.isEmpty()).toList();
        if (gameFilter != null) {
            gameFilter.getItems().clear();
            gameFilter.getItems().add("All games");
            gameFilter.getItems().addAll(games);
            gameFilter.getSelectionModel().selectFirst();
        }
    }

    private void bindListeners() {
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        if (statusFilter != null) statusFilter.setOnAction(e -> applyFilters());
        if (gameFilter   != null) gameFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        String query  = searchField  != null ? searchField.getText().trim().toLowerCase()  : "";
        String status = statusFilter != null ? (statusFilter.getValue() == null ? "all" : statusFilter.getValue().toLowerCase()) : "all";
        String game   = gameFilter   != null ? (gameFilter.getValue()   == null ? "all" : gameFilter.getValue().toLowerCase())   : "all";

        List<GuideVideo> filtered = allGuides.stream().filter(g -> {
            boolean searchMatch = query.isEmpty()
                    || g.getTitle().toLowerCase().contains(query)
                    || (g.getMap() != null && g.getMap().toLowerCase().contains(query));
            boolean statusMatch = status.equals("all status") || status.equals("all") || g.getStatus().equals(status);
            boolean gameMatch   = game.equals("all games")    || game.equals("all")   ||
                    (g.getGame() != null && g.getGame().getName().toLowerCase().equals(game));
            return searchMatch && statusMatch && gameMatch;
        }).toList();

        long approved = filtered.stream().filter(g -> "approved".equals(g.getStatus())).count();
        if (totalLabel    != null) totalLabel.setText(String.valueOf(allGuides.size()));
        if (visibleLabel  != null) visibleLabel.setText(String.valueOf(filtered.size()));
        if (approvedLabel != null) approvedLabel.setText(String.valueOf(approved));
        if (resultsNote   != null) resultsNote.setText(filtered.size() + " guide(s) affiché(s)");

        renderGuides(filtered);
    }

    // ═══════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════
    private void renderGuides(List<GuideVideo> guides) {
        guidesGrid.getChildren().clear();
        for (int i = 0; i < guides.size(); i++) {
            VBox card = buildGuideCard(guides.get(i));
            card.setOpacity(0);
            int delay = i * 60;
            Timeline anim = new Timeline(new KeyFrame(Duration.millis(300 + delay),
                    new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT)));
            anim.play();
            guidesGrid.getChildren().add(card);
        }
    }

    private VBox buildGuideCard(GuideVideo guide) {
        VBox card = new VBox(0);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle(
                "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.8),rgba(11,17,31,0.8));" +
                "-fx-border-color: rgba(76,211,227,0.1);" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );
        card.setEffect(new DropShadow(20, Color.web("#000000", 0.3)));

        // Thumbnail
        StackPane thumbWrap = new StackPane();
        thumbWrap.setPrefHeight(162);
        thumbWrap.setStyle("-fx-background-color: #000; -fx-background-radius: 12 12 0 0;");
        if (guide.getThumbnail() != null && !guide.getThumbnail().isEmpty()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(320); iv.setFitHeight(162);
            iv.setPreserveRatio(false); iv.setSmooth(true);
            try { iv.setImage(new Image(guide.getThumbnail(), true)); } catch (Exception ignored) {}
            thumbWrap.getChildren().add(iv);
        } else {
            Label fallback = new Label("▶");
            fallback.setStyle("-fx-text-fill: #4cd3e3; -fx-font-size: 42;");
            thumbWrap.getChildren().add(fallback);
        }

        // Content
        VBox content = new VBox(10);
        content.setPadding(new Insets(18, 18, 18, 18));

        Label titleLbl = new Label(guide.getTitle());
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);

        // Meta row: game + status badge
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label gameLbl = new Label(guide.getGame() != null ? guide.getGame().getName() : "");
        gameLbl.setStyle("-fx-text-fill: #4cd3e3; -fx-font-weight: bold; -fx-font-size: 12;");
        HBox.setHgrow(gameLbl, Priority.ALWAYS);

        Label statusBadge = new Label(guide.getStatus().toUpperCase());
        statusBadge.setStyle(buildStatusStyle(guide.getStatus()));
        metaRow.getChildren().addAll(gameLbl, statusBadge);

        // Description
        if (guide.getDescription() != null && !guide.getDescription().isEmpty()) {
            Label desc = new Label(guide.getDescription().length() > 80
                    ? guide.getDescription().substring(0, 80) + "..." : guide.getDescription());
            desc.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 13;");
            desc.setWrapText(true);
            content.getChildren().add(desc);
        }

        // Stats row
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
                statChip("♥", String.valueOf(guide.getLikes())),
                statChip("👁", String.valueOf(guide.getViews())),
                statChip("🗺", guide.getMap() != null ? guide.getMap() : "All")
        );

        // Action buttons
        HBox actions = new HBox(8);
        if (!"approved".equals(guide.getStatus())) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle(
                    "-fx-background-color: rgba(76,211,227,0.2); -fx-text-fill: #4cd3e3;" +
                    "-fx-border-color: transparent; -fx-border-radius: 6; -fx-background-radius: 6;" +
                    "-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 7 14 7 14; -fx-cursor: hand;"
            );
            editBtn.setOnAction(e -> editGuide(guide));
            editBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            actions.getChildren().add(editBtn);
        }

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: rgba(255,0,0,0.2); -fx-text-fill: #ff6b6b;" +
                "-fx-border-color: transparent; -fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 7 14 7 14; -fx-cursor: hand;"
        );
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setOnAction(e -> confirmDelete(guide));
        actions.getChildren().add(deleteBtn);

        content.getChildren().addAll(titleLbl, metaRow, statsRow, actions);
        card.getChildren().addAll(thumbWrap, content);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.9),rgba(11,17,31,0.9));" +
                    "-fx-border-color: rgba(76,211,227,0.35);" +
                    "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;"
            );
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(200),
                    new KeyValue(card.translateYProperty(), -4, Interpolator.EASE_OUT)));
            tl.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.8),rgba(11,17,31,0.8));" +
                    "-fx-border-color: rgba(76,211,227,0.1);" +
                    "-fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;"
            );
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(200),
                    new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT)));
            tl.play();
        });

        return card;
    }

    private Label statChip(String icon, String value) {
        Label lbl = new Label(icon + " " + value);
        lbl.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 12;");
        return lbl;
    }

    private String buildStatusStyle(String status) {
        return switch (status) {
            case "approved" -> "-fx-background-color: rgba(76,211,227,0.2); -fx-text-fill: #4cd3e3;" +
                    "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 10;";
            case "rejected" -> "-fx-background-color: rgba(255,0,0,0.2); -fx-text-fill: #ff6b6b;" +
                    "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 10;";
            default         -> "-fx-background-color: rgba(255,193,7,0.2); -fx-text-fill: #ffc107;" +
                    "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 10;";
        };
    }

    // ═══════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════
    private void editGuide(GuideVideo guide) {
        navbarController.navigateToWithData("GuideUpload.fxml", "guideToEdit", guide);
    }

    private void confirmDelete(GuideVideo guide) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce guide : " + guide.getTitle() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        guideVideoRepository.delete(guide);
                        allGuides = allGuides.stream()
                                .filter(g -> !g.getId().equals(guide.getId())).toList();
                        Platform.runLater(() -> {
                            if (allGuides.isEmpty()) {
                                showEmpty("Vous n'avez plus de guides.");
                            } else {
                                applyFilters();
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("[MyGuidesController] Delete error: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    private void showEmpty(String message) {
        if (emptyState != null)         { emptyState.setVisible(true); emptyState.setManaged(true); }
        if (statsAndGridSection != null) { statsAndGridSection.setVisible(false); statsAndGridSection.setManaged(false); }
    }

    @FXML public void goToUpload() {
        navbarController.navigateTo("GuideUpload.fxml");
    }
}
