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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.eyetwin.entities.*;
import com.eyetwin.interfaces.*;
import com.eyetwin.services.*;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import java.io.File;
import java.io.IOException;

public class TournoiController {

    @FXML private BorderPane rootPane;
    @FXML private Node       navbar;
    @FXML private Node       adminSidebar;
    @FXML private Node       adminTopbar;

    @FXML private NavbarController       navbarController;
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    @FXML private FlowPane tournoiGrid;
    @FXML private Label    statsLabel;
    @FXML private Button   btnAdd;

    // --- Dashboard UI ---
    @FXML private VBox     listView;
    @FXML private VBox     detailsView;
    @FXML private Label    detailNameLabel;
    @FXML private Label    detailTypeLabel;
    @FXML private Label    detailDateLabel;
    @FXML private FlowPane detailMatchGrid;
    @FXML private Label    noMatchesLabel;

    private ITournoiService                      tournoiService;
    private com.eyetwin.interfaces.IMatchService matchService;
    private ITournoiInscriptionService           inscriptionService;
    private StripeService                        stripeService;
    private StripePaymentChecker                 paymentChecker;

    public TournoiController() {
        tournoiService      = new TournoiServiceImpl();
        matchService        = new com.eyetwin.services.MatchServiceImpl();
        inscriptionService  = new TournoiInscriptionServiceImpl();
        stripeService       = new StripeService();
        paymentChecker      = new StripePaymentChecker();
    }

    private boolean canManage() {
        return SessionManager.isAdmin() || SessionManager.isCoach();
    }

    @FXML public void initialize() {
        boolean isStaff = canManage();

        // 1. Adaptive Layout: Toggle Sidebar vs Navbar
        if (isStaff) {
            setupAdminLayout();
        } else {
            setupUserLayout();
        }

        // 2. RBAC: Hide Add button for regular users
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

        if (adminSidebarController != null) adminSidebarController.setActivePage("tournaments");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Tournament Management");
    }

    private void setupUserLayout() {
        if (navbar != null) { navbar.setVisible(true); navbar.setManaged(true); }
        if (adminSidebar != null) { adminSidebar.setVisible(false); adminSidebar.setManaged(false); }
        if (adminTopbar != null) { adminTopbar.setVisible(false); adminTopbar.setManaged(false); }

        if (navbarController != null) navbarController.setActivePage("tournois");

        // Adjust background for user layout
        if (rootPane != null) rootPane.setStyle("-fx-background-color: #080810;");
    }

    // ─── Grid ──────────────────────────────────────────────────────────────────

    public void refreshGrid() {
        tournoiGrid.getChildren().clear();
        ObservableList<Tournoi> list = FXCollections.observableArrayList(tournoiService.getAll());
        for (Tournoi t : list) {
            tournoiGrid.getChildren().add(createCard(t));
        }
        if (statsLabel != null) {
            statsLabel.setText(list.size() + " tournament" + (list.size() != 1 ? "s" : ""));
        }
    }

    // ─── Card builder ──────────────────────────────────────────────────────────

    private VBox createCard(Tournoi t) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(220);

        // --- Top accent band ---
        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4);
        band.setPrefHeight(4);

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
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox priceRow = new HBox(spacer, price);

        body.getChildren().addAll(new HBox(badge), name, dates, priceRow);

        // --- Button row ---
        Button btnDetails = new Button("👁 Details");
        btnDetails.getStyleClass().add("card-btn-details");
        btnDetails.setOnAction(e -> openDetail(t));

        HBox btnRow = new HBox(6, btnDetails);
        btnRow.getStyleClass().add("card-btn-row");
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // ── Regular User Registration ──
        if (!canManage()) {
            User user = SessionManager.getCurrentUser();
            if (user != null) {
                boolean isRegistered = inscriptionService.isUserRegistered(user.getId(), t.getId());
                if (isRegistered) {
                    Label registeredLabel = new Label("✅ Inscribed");
                    registeredLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-padding: 0 10;");
                    btnRow.getChildren().add(registeredLabel);
                } else {
                    Button btnRegister = new Button("🏆 S'inscrire");
                    btnRegister.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5;");
                    btnRegister.setOnAction(e -> handleRegistration(t, btnRegister));
                    btnRow.getChildren().add(btnRegister);
                }
            }
        }

        // ── Admin Management ──
        if (canManage()) {
            Button btnEdit = new Button("✏");
            btnEdit.getStyleClass().add("card-btn-edit");
            btnEdit.setOnAction(e -> openForm(t));

            Button btnDelete = new Button("✕");
            btnDelete.getStyleClass().add("card-btn-delete");
            btnDelete.setOnAction(e -> confirmDelete(t.getId()));

            Region btnSpacer = new Region();
            HBox.setHgrow(btnSpacer, Priority.ALWAYS);
            btnRow.getChildren().addAll(btnSpacer, btnEdit, btnDelete);
        }

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
                    iv.setFitWidth(w);
                    iv.setFitHeight(h);
                    iv.setPreserveRatio(false);
                    iv.setSmooth(true);
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(w, h);
                    clip.setArcWidth(0);
                    clip.setArcHeight(0);
                    iv.setClip(clip);
                    return iv;
                }
            } catch (Exception ignored) {}
        }
        Label ph = new Label("🏆");
        ph.setStyle("-fx-font-size:38px;-fx-text-fill:#333344;-fx-alignment:CENTER;-fx-pref-width:" + w + ";-fx-pref-height:" + h + ";-fx-background-color:#0d0d18;");
        ph.setAlignment(Pos.CENTER);
        ph.setPrefSize(w, h);
        return ph;
    }

    // ─── Modals ───────────────────────────────────────────────────────────────

    @FXML void onOpenAddForm(ActionEvent event) {
        openForm(null);
    }

    private void openForm(Tournoi tournoi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/TournoiForm.fxml"));
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML void onBackToList(ActionEvent event) {
        detailsView.setVisible(false);
        detailsView.setManaged(false);
        listView.setVisible(true);
        listView.setManaged(true);
    }

    private void showTournamentDetail(Tournoi t) {
        // Toggle view
        listView.setVisible(false);
        listView.setManaged(false);
        detailsView.setVisible(true);
        detailsView.setManaged(true);

        // Populate header
        detailNameLabel.setText(t.getNom());
        detailTypeLabel.setText(t.getTypeTournoi() != null ? t.getTypeTournoi().toString() : "N/A");
        detailDateLabel.setText("📅 " + t.getDateDebut() + " → " + t.getDateFin());

        // Populate Matches
        detailMatchGrid.getChildren().clear();
        java.util.List<com.eyetwin.entities.Match> matches = matchService.getByTournoi(t.getId());
        
        if (matches.isEmpty()) {
            noMatchesLabel.setVisible(true);
            noMatchesLabel.setManaged(true);
        } else {
            noMatchesLabel.setVisible(false);
            noMatchesLabel.setManaged(false);
            for (com.eyetwin.entities.Match m : matches) {
                detailMatchGrid.getChildren().add(createMatchCard(m));
            }
        }
    }

    private VBox createMatchCard(com.eyetwin.entities.Match m) {
        VBox card = new VBox(0);
        card.getStyleClass().add("data-card");
        card.setPrefWidth(210);

        HBox band = new HBox();
        band.getStyleClass().add("card-header-band");
        band.setMinHeight(4);

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 12, 14));

        Label mode = new Label(m.getPlayMode() != null ? m.getPlayMode() : "Online");
        mode.getStyleClass().add("card-badge");

        Label teams = new Label(m.getEquipe1() + " VS " + m.getEquipe2());
        teams.getStyleClass().add("card-name");
        teams.setMaxWidth(180);

        Label score = new Label("Score: " + m.getScore());
        score.getStyleClass().add("card-meta");

        Label date = new Label("📅 " + m.getDateMatch());
        date.getStyleClass().add("card-meta");

        Label price = new Label(m.getPrix() + " DT");
        price.getStyleClass().add("card-price");
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        body.getChildren().addAll(new HBox(mode), teams, score, date, spacer, new HBox(new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, price));
        
        card.getChildren().addAll(band, body);
        return card;
    }

    private void openDetail(Tournoi t) {
        showTournamentDetail(t);
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

    // ─── Registration & Stripe ────────────────────────────────────────────────

    private void handleRegistration(Tournoi t, Button btn) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez vous connecter pour vous inscrire.").show();
            return;
        }

        try {
            btn.setDisable(true);
            btn.setText("⏳ Redirecting...");

            CheckoutResult result = stripeService.createTournamentCheckoutSession(user, t);
            
            // Create PENDING inscription
            TournoiInscription ins = new TournoiInscription(user.getId(), t.getId(), result.sessionId);
            inscriptionService.add(ins);

            // Open browser
            stripeService.openCheckoutInBrowser(result.url);

            // Start polling background task
            startPollingStatus(result.sessionId, t, user, btn);

        } catch (Exception e) {
            btn.setDisable(false);
            btn.setText("🏆 S'inscrire");
            new Alert(Alert.AlertType.ERROR, "Erreur Stripe : " + e.getMessage()).show();
        }
    }

    private void startPollingStatus(String sessionId, Tournoi t, User user, Button btn) {
        Thread pollThread = new Thread(() -> {
            boolean confirmed = false;
            int attempts = 0;
            while (!confirmed && attempts < 60) { // Poll for 5 minutes max (5s * 60)
                try {
                    Thread.sleep(5000); 
                    attempts++;
                    
                    if (paymentChecker.isSessionPaid(sessionId)) {
                        confirmed = true;
                        
                        // Update DB
                        inscriptionService.updateStatusBySession(sessionId, "PAID");

                        // Send Email
                        EmailService.getInstance().sendTournamentRegistrationEmail(user, t);

                        Platform.runLater(() -> {
                            btn.setText("✅ Inscribed");
                            btn.setDisable(true);
                            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4ade80; -fx-font-weight: bold;");
                            
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Félicitations !");
                            alert.setHeaderText("Inscription confirmée");
                            alert.setContentText("Vous avez été inscrit au tournoi " + t.getNom() + ". Un mail de confirmation a été envoyé.");
                            alert.show();
                        });
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (!confirmed) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🏆 S'inscrire");
                });
            }
        }, "StripePolling-" + sessionId);
        pollThread.setDaemon(true);
        pollThread.start();
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML void onGoToTournois(ActionEvent event) {
        /* already here */
    }

    @FXML void onGoToMatches(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/Matches.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Matches - EyeTwin Platform");
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML void onLogout(ActionEvent event) {
        // You might want to point this back to your main login page
        try {
            // Check where your login page is and update accordingly
            // For now, let's keep it consistent with your current project structure if possible
            // Parent root = FXMLLoader.load(getClass().getResource("/com/eyetwin/views/login/login.fxml"));
            // ...
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
