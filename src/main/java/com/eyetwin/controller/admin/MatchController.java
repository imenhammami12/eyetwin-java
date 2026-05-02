package com.eyetwin.controller.admin;

import com.eyetwin.controller.NavbarController;
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
import javafx.stage.Modality;
import com.eyetwin.entities.Match;
import com.eyetwin.services.MatchServiceImpl;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.tools.SessionManager;

import java.io.IOException;

public class MatchController {

    @FXML private BorderPane rootPane;
    @FXML private Node       navbar;
    @FXML private Node       adminSidebar;
    @FXML private Node       adminTopbar;

    @FXML private NavbarController       navbarController;
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    @FXML private FlowPane matchGrid;
    @FXML private Label    statsLabel;
    @FXML private Button   btnAdd;

    private IMatchService   matchService;

    public MatchController() {
        matchService   = new MatchServiceImpl();
    }

    private boolean canManage() {
        return SessionManager.isAdmin() || SessionManager.isCoach();
    }

    @FXML
    public void initialize() {
        boolean isStaff = canManage();

        // 1. Adaptive Layout
        if (isStaff) {
            setupAdminLayout();
        } else {
            setupUserLayout();
        }

        // 2. RBAC
        if (btnAdd != null) {
            btnAdd.setVisible(isStaff);
            btnAdd.setManaged(isStaff);
        }

        refreshGrid();
    }

    private void setupAdminLayout() {
        if (navbar != null) { navbar.setVisible(false); navbar.setManaged(false); }
        if (adminSidebar != null) { adminSidebar.setVisible(true); adminSidebar.setManaged(true); }
        if (adminTopbar != null) { adminTopbar.setVisible(true); adminTopbar.setManaged(true); }

        if (adminSidebarController != null) adminSidebarController.setActivePage("matches");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Match Management");
    }

    private void setupUserLayout() {
        if (navbar != null) { navbar.setVisible(true); navbar.setManaged(true); }
        if (adminSidebar != null) { adminSidebar.setVisible(false); adminSidebar.setManaged(false); }
        if (adminTopbar != null) { adminTopbar.setVisible(false); adminTopbar.setManaged(false); }

        if (navbarController != null) navbarController.setActivePage("tournois");

        // Adjust background for user layout
        if (rootPane != null) rootPane.setStyle("-fx-background-color: #080810;");
    }

    // ─── Grid refresh ────────────────────────────────────────────────────────

    private void refreshGrid() {
        matchGrid.getChildren().clear();
        ObservableList<Match> matchList = FXCollections.observableArrayList(matchService.getAll());

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
        Button btnDetails = new Button("👁 Details");
        btnDetails.getStyleClass().add("card-btn-details");
        btnDetails.setOnAction(e -> openDetail(m));

        Button btnEdit = new Button("✏");
        btnEdit.getStyleClass().add("card-btn-edit");
        btnEdit.setOnAction(e -> openForm(m));

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("card-btn-delete");
        btnDelete.setOnAction(e -> confirmDelete(m.getId()));

        HBox btnRow = new HBox(6, btnDetails);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setPadding(new Insets(0, 14, 10, 14));

        if (canManage()) {
            Region btnSpacer = new Region();
            HBox.setHgrow(btnSpacer, Priority.ALWAYS);
            btnRow.getChildren().addAll(btnSpacer, btnEdit, btnDelete);
        }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/MatchForm.fxml"));
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

    private void openDetail(Match m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/MatchDetail.fxml"));
            Parent root = loader.load();

            MatchDetailController dc = loader.getController();
            dc.setMatch(m);

            Stage stage = new Stage();
            stage.setTitle("Match Detail — " + m.getEquipe1() + " vs " + m.getEquipe2());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 620, 580));
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
            Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/Tournois.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Tournaments - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML void onLogout(ActionEvent event) {
        /* navigate to login */
    }
}
