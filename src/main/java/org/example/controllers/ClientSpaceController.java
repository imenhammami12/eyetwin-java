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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.entities.Match;
import org.example.entities.Tournoi;
import org.example.services.MatchService;
import org.example.services.TournoiService;

import java.io.File;
import java.io.IOException;

public class ClientSpaceController {

    @FXML private StackPane contentArea;
    @FXML private VBox dashboardView;
    @FXML private VBox explorerView;
    @FXML private FlowPane clientGrid;
    @FXML private Label viewTitle;
    @FXML private Label viewSubtitle;
    @FXML private Label statsLabel;

    @FXML private Button dashBtn;
    @FXML private Button tournoisBtn;
    @FXML private Button matchesBtn;

    private TournoiService tournoiService = new TournoiService();
    private MatchService matchService = new MatchService();

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        dashboardView.setVisible(true);
        explorerView.setVisible(false);
        setActiveBtn(dashBtn);
    }

    @FXML
    private void showTournaments() {
        dashboardView.setVisible(false);
        explorerView.setVisible(true);
        viewTitle.setText("Explore Tournaments");
        viewSubtitle.setText("Find the best competitions to join or watch");
        setActiveBtn(tournoisBtn);
        loadTournaments();
    }

    @FXML
    private void showMatches() {
        dashboardView.setVisible(false);
        explorerView.setVisible(true);
        viewTitle.setText("Live Matches");
        viewSubtitle.setText("Stay updated with ongoing battles");
        setActiveBtn(matchesBtn);
        loadMatches();
    }

    private void setActiveBtn(Button activeBtn) {
        dashBtn.getStyleClass().removeAll("nav-btn-active");
        dashBtn.getStyleClass().add("nav-btn");
        tournoisBtn.getStyleClass().removeAll("nav-btn-active");
        tournoisBtn.getStyleClass().add("nav-btn");
        matchesBtn.getStyleClass().removeAll("nav-btn-active");
        matchesBtn.getStyleClass().add("nav-btn");

        activeBtn.getStyleClass().removeAll("nav-btn");
        activeBtn.getStyleClass().add("nav-btn-active");
    }

    private void loadTournaments() {
        clientGrid.getChildren().clear();
        ObservableList<Tournoi> list = FXCollections.observableArrayList(tournoiService.getAll());
        for (Tournoi t : list) clientGrid.getChildren().add(createClientTournoiCard(t));
        statsLabel.setText(list.size() + " tournament" + (list.size() != 1 ? "s" : ""));
    }

    private void loadMatches() {
        clientGrid.getChildren().clear();
        ObservableList<Match> list = FXCollections.observableArrayList(matchService.getAll());
        for (Match m : list) clientGrid.getChildren().add(createClientMatchCard(m));
        statsLabel.setText(list.size() + " match" + (list.size() != 1 ? "es" : ""));
    }

    private VBox createClientTournoiCard(Tournoi t) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(220);

        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4);

        Node imgNode = buildImageNode(t.getImage(), 220, 110);

        VBox body = new VBox(7);
        body.setPadding(new Insets(10, 14, 10, 14));

        Label badge = new Label(t.getTypeTournoi() != null ? t.getTypeTournoi().toString() : "N/A");
        badge.getStyleClass().add("card-badge");

        Label name = new Label(t.getNom() != null ? t.getNom() : "—");
        name.getStyleClass().add("card-name");

        Label price = new Label(t.getPrix() + " DT");
        price.getStyleClass().add("card-price");

        body.getChildren().addAll(new HBox(badge), name, price);

        Button btnDetails = new Button("👁 View Details");
        btnDetails.getStyleClass().add("card-btn-details");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openTournoiDetail(t));

        HBox btnRow = new HBox(btnDetails);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(band, imgNode, body, btnRow);
        return card;
    }

    private VBox createClientMatchCard(Match m) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(215);

        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4);

        VBox body = new VBox(7);
        body.setPadding(new Insets(11, 14, 12, 14));

        Label badge = new Label(m.getPlayMode() != null ? m.getPlayMode() : "N/A");
        badge.getStyleClass().add("card-badge");

        Label vsRow = new Label(m.getEquipe1() + " VS " + m.getEquipe2());
        vsRow.getStyleClass().add("card-name");

        Label score = new Label("Score: " + m.getScore());
        score.getStyleClass().add("card-meta");

        Label price = new Label(m.getPrix() + " DT");
        price.getStyleClass().add("card-price");

        body.getChildren().addAll(new HBox(badge), vsRow, score, price);

        Button btnDetails = new Button("👁 Match Info");
        btnDetails.getStyleClass().add("card-btn-details");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> openMatchDetail(m));

        HBox btnRow = new HBox(btnDetails);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(band, body, btnRow);
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
                    return iv;
                }
            } catch (Exception ignored) {}
        }
        Label ph = new Label("🏆");
        ph.setStyle("-fx-font-size:38px;-fx-text-fill:#333344;-fx-alignment:CENTER;-fx-pref-width:" + w + ";-fx-pref-height:" + h + ";-fx-background-color:#0d0d18;");
        ph.setAlignment(Pos.CENTER);
        return ph;
    }

    private void openTournoiDetail(Tournoi t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tournoi-detail.fxml"));
            Parent root = loader.load();
            // Reuse existing TournoiDetailController
            Object dc = loader.getController();
            if (dc instanceof TournoiDetailController) {
                ((TournoiDetailController) dc).setTournoi(t);
            }

            Stage stage = new Stage();
            stage.setTitle("Tournament — " + t.getNom());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 660, 560));
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openMatchDetail(Match m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/match-detail.fxml"));
            Parent root = loader.load();
            MatchDetailController mdc = loader.getController();
            mdc.setMatch(m);

            Stage stage = new Stage();
            stage.setTitle("Match Info — " + m.getEquipe1() + " VS " + m.getEquipe2());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 600, 500));
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Login - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
