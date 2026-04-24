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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Tournoi;
import org.example.services.TournoiService;

import java.io.File;
import java.io.IOException;

public class TournoiController {

    @FXML private FlowPane tournoiGrid;
    @FXML private Label    statsLabel;

    private TournoiService tournoiService;

    public TournoiController() { tournoiService = new TournoiService(); }

    @FXML public void initialize() { refreshGrid(); }

    // ─── Grid ──────────────────────────────────────────────────────────────────

    public void refreshGrid() {
        tournoiGrid.getChildren().clear();
        ObservableList<Tournoi> list = FXCollections.observableArrayList(tournoiService.getAll());
        for (Tournoi t : list) tournoiGrid.getChildren().add(createCard(t));
        if (statsLabel != null)
            statsLabel.setText(list.size() + " tournament" + (list.size() != 1 ? "s" : ""));
    }

    // ─── Card builder ──────────────────────────────────────────────────────────

    private VBox createCard(Tournoi t) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(220);

        // --- Top accent band ---
        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4); band.setPrefHeight(4);

        // --- Image or placeholder ---
        Node imgNode = buildImageNode(t.getImage(), 220, 110);

        // --- Body ---
        VBox body = new VBox(7);
        body.setPadding(new Insets(10, 14, 10, 14));

        Label badge = new Label(t.getTypeTournoi() != null ? t.getTypeTournoi().toString() : "N/A");
        badge.getStyleClass().add("card-badge");

        Label name = new Label(t.getNom() != null ? t.getNom() : "—");
        name.getStyleClass().add("card-name");
        name.setMaxWidth(192);

        String dateStr = "📅 " +
            (t.getDateDebut() != null ? t.getDateDebut() : "?") + " → " +
            (t.getDateFin()   != null ? t.getDateFin()   : "?");
        Label dates = new Label(dateStr);
        dates.getStyleClass().add("card-meta");

        Label price = new Label(t.getPrix() + " DT");
        price.getStyleClass().add("card-price");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox priceRow = new HBox(spacer, price);

        body.getChildren().addAll(new HBox(badge), name, dates, priceRow);

        // --- Button row ---
        Button btnDetails = new Button("👁 Details");
        btnDetails.getStyleClass().add("card-btn-details");
        btnDetails.setOnAction(e -> openDetail(t));

        Button btnEdit = new Button("✏");
        btnEdit.getStyleClass().add("card-btn-edit");
        btnEdit.setOnAction(e -> openForm(t));

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("card-btn-delete");
        btnDelete.setOnAction(e -> confirmDelete(t.getId()));

        Region btnSpacer = new Region(); HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        HBox btnRow = new HBox(6, btnDetails, btnSpacer, btnEdit, btnDelete);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(band, imgNode, body, btnRow);
        return card;
    }

    private Node buildImageNode(String imagePath, double w, double h) {
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File f = new File(imagePath);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), w, h, false, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(w); iv.setFitHeight(h);
                    iv.setPreserveRatio(false); iv.setSmooth(true);
                    // clip to rounded top
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
                    clip.setArcWidth(0); clip.setArcHeight(0);
                    iv.setClip(clip);
                    return iv;
                }
            } catch (Exception ignored) {}
        }
        // Placeholder
        Label ph = new Label("🏆");
        ph.setStyle("-fx-font-size:38px;-fx-text-fill:#333344;-fx-alignment:CENTER;-fx-pref-width:" + w + ";-fx-pref-height:" + h + ";-fx-background-color:#0d0d18;");
        ph.setAlignment(Pos.CENTER);
        ph.setPrefSize(w, h);
        return ph;
    }

    // ─── Modals ───────────────────────────────────────────────────────────────

    @FXML void onOpenAddForm(ActionEvent event) { openForm(null); }

    private void openForm(Tournoi tournoi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tournoi-form.fxml"));
            Parent root = loader.load();
            TournoiFormController fc = loader.getController();
            fc.setTournoi(tournoi);
            fc.setOnSaved(this::refreshGrid);

            Stage stage = new Stage();
            stage.setTitle(tournoi == null ? "Add Tournament" : "Edit Tournament");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 680, 520));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openDetail(Tournoi t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tournoi-detail.fxml"));
            Parent root = loader.load();
            TournoiDetailController dc = loader.getController();
            dc.setTournoi(t);

            Stage stage = new Stage();
            stage.setTitle("Tournament — " + t.getNom());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 660, 560));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void confirmDelete(int id) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete this tournament?");
        alert.setContentText("This action cannot be undone.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    tournoiService.delete(id);
                    refreshGrid();
                } catch (RuntimeException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                }
            }
        });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML void onGoToTournois(ActionEvent event) { /* already here */ }

    @FXML void onGoToMatches(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/match-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Matches - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) { e.printStackTrace(); }
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
