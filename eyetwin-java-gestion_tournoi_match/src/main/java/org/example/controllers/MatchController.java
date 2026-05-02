package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Match;
import org.example.entities.Tournoi;
import org.example.services.MatchService;
import org.example.services.TournoiService;

import java.io.IOException;
import java.time.LocalDate;

public class MatchController {

    @FXML private FlowPane matchGrid;
    @FXML private Label    statsLabel;

    private MatchService   matchService;
    private ObservableList<Match> matchList;

    public MatchController() {
        matchService   = new MatchService();
    }

    @FXML
    public void initialize() {
        refreshGrid();
    }

    // ─── Grid refresh ────────────────────────────────────────────────────────

    private void refreshGrid() {
        matchGrid.getChildren().clear();
        matchList = FXCollections.observableArrayList(matchService.getAll());

        for (Match m : matchList) {
            matchGrid.getChildren().add(createCard(m));
        }
        if (statsLabel != null) {
            statsLabel.setText(matchList.size() + " match" + (matchList.size() != 1 ? "es" : ""));
        }
    }

    // ─── Card builder ────────────────────────────────────────────────────────

    private VBox createCard(Match m) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(215);

        // Top accent band
        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4);
        band.setPrefHeight(4);

        // Body
        VBox body = new VBox(7);
        body.setPadding(new Insets(11, 14, 12, 14));

        // Mode badge
        Label badge = new Label(m.getPlayMode() != null ? m.getPlayMode() : "N/A");
        badge.getStyleClass().add("card-badge");
        HBox badgeRow = new HBox(badge);

        // VS row
        Label team1 = new Label(m.getEquipe1() != null ? m.getEquipe1() : "?");
        team1.getStyleClass().add("card-name");
        team1.setMaxWidth(80);

        Label vs = new Label(" VS ");
        vs.getStyleClass().add("card-badge");
        vs.setStyle("-fx-text-fill: white; -fx-border-color: #444455;");

        Label team2 = new Label(m.getEquipe2() != null ? m.getEquipe2() : "?");
        team2.getStyleClass().add("card-name");
        team2.setMaxWidth(80);

        HBox vsRow = new HBox(4, team1, vs, team2);
        vsRow.setAlignment(Pos.CENTER_LEFT);

        // Score
        Label score = new Label("Score: " + m.getScore());
        score.getStyleClass().add("card-meta");

        // Date & location
        String dateStr = m.getDateMatch() != null ? "📅 " + m.getDateMatch().toString() : "📅 N/A";
        Label dateLbl = new Label(dateStr);
        dateLbl.getStyleClass().add("card-meta");

        // Price row
        Label price = new Label(m.getPrix() + " DT");
        price.getStyleClass().add("card-price");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox priceRow = new HBox(spacer, price);
        priceRow.setAlignment(Pos.CENTER_RIGHT);

        body.getChildren().addAll(badgeRow, vsRow, score, dateLbl, priceRow);

        // Action Buttons
        Button btnEdit = new Button("✏");
        btnEdit.getStyleClass().add("card-btn-edit");
        btnEdit.setOnAction(e -> openForm(m));

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("card-btn-delete");
        btnDelete.setOnAction(e -> confirmDelete(m.getId()));

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        HBox btnRow = new HBox(6, btnSpacer, btnEdit, btnDelete);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setPadding(new Insets(0, 14, 10, 14));

        card.getChildren().addAll(band, body, btnRow);
        return card;
    }

    // ─── Modals ───────────────────────────────────────────────────────────────

    @FXML
    void onOpenAddForm(ActionEvent event) {
        openForm(null);
    }

    private void openForm(Match m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/match-form.fxml"));
            Parent root = loader.load();

            MatchFormController controller = loader.getController();
            controller.setMatch(m);
            controller.setOnSaved(this::refreshGrid);

            Stage stage = new Stage();
            stage.setTitle(m == null ? "Add Match" : "Edit Match");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 700, 500));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confirmDelete(int id) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete this match?");
        alert.setContentText("This action cannot be undone.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    matchService.delete(id);
                    refreshGrid();
                } catch (RuntimeException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                }
            }
        });
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML void onGoToMatches(ActionEvent event) {
        // already here
    }

    @FXML void onGoToTournois(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/tournoi-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Tournaments - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML void onLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Login - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
