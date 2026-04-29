package com.eyetwin.controller;

import com.eyetwin.entities.GuideVideo;
import com.eyetwin.entities.User;
import com.eyetwin.repository.GuideVideoRepository;
import com.eyetwin.tools.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MyUploadsController {

    @FXML private NavbarController navbarController;
    @FXML private TabPane uploadTabs;

    // All Uploads Tab
    @FXML private TextField searchFieldAll;
    @FXML private ComboBox<String> typeFilterAll;
    @FXML private ComboBox<String> statusFilterAll;
    @FXML private TableView<GuideVideo> allUploadsTable;
    @FXML private TableColumn<GuideVideo, String> titleColAll;
    @FXML private TableColumn<GuideVideo, String> typeColAll;
    @FXML private TableColumn<GuideVideo, String> statusColAll;
    @FXML private TableColumn<GuideVideo, String> dateColAll;
    @FXML private TableColumn<GuideVideo, Integer> viewsColAll;
    @FXML private TableColumn<GuideVideo, Void> actionsColAll;
    @FXML private VBox emptyStateAll;

    // Guides Tab
    @FXML private TextField searchFieldGuides;
    @FXML private FlowPane guidesGrid;
    @FXML private VBox emptyStateGuides;

    // Videos Tab
    @FXML private TextField searchFieldVideos;
    @FXML private FlowPane videosGrid;
    @FXML private VBox emptyStateVideos;

    // Stats Tab
    @FXML private Label statTotalUploads;
    @FXML private Label statTotalViews;
    @FXML private Label statApproved;
    @FXML private Label statPending;
    @FXML private Label statRejected;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> performanceBarChart;
    @FXML private Label kpiApprovalRate;
    @FXML private Label kpiAvgViews;
    @FXML private Label kpiPublishedImpact;
    @FXML private Label kpiQualityScore;

    // Header stats (quick view)
    @FXML private Label headerTotalUploads;
    @FXML private Label headerApproved;
    @FXML private Label headerPending;
    @FXML private Label headerViews;

    private final GuideVideoRepository guideVideoRepository = new GuideVideoRepository();
    private List<GuideVideo> allUploads;
    private List<GuideVideo> filteredUploads;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ═══════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            navigateTo("login.fxml");
            return;
        }

        if (navbarController != null) navbarController.setActivePage("myuploads");

        setupFilters();
        loadUserUploads();
    }

    private void setupFilters() {
        // Type filter
        if (typeFilterAll != null) {
            typeFilterAll.getItems().setAll("All types", "Guide", "Video", "Clip");
            typeFilterAll.getSelectionModel().selectFirst();
            typeFilterAll.setOnAction(e -> applyFilters());
        }

        // Status filter
        if (statusFilterAll != null) {
            statusFilterAll.getItems().setAll("All status", "approved", "pending", "rejected");
            statusFilterAll.getSelectionModel().selectFirst();
            statusFilterAll.setOnAction(e -> applyFilters());
        }

        // Search listeners
        if (searchFieldAll != null) searchFieldAll.textProperty().addListener((o, ov, nv) -> applyFilters());
        if (searchFieldGuides != null) searchFieldGuides.textProperty().addListener((o, ov, nv) -> applyFilters());
        if (searchFieldVideos != null) searchFieldVideos.textProperty().addListener((o, ov, nv) -> applyFilters());
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════════════════
    private void loadUserUploads() {
        new Thread(() -> {
            try {
                User user = SessionManager.getCurrentUser();
                allUploads = guideVideoRepository.findByUploader(user);
                Platform.runLater(this::displayUploads);
            } catch (Exception e) {
                System.err.println("[MyUploadsController] Error loading uploads: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void displayUploads() {
        if (allUploads.isEmpty()) {
            showEmpty();
            updateStats();
            return;
        }

        populateAllUploadsTable();
        populateGuidesTab();
        populateVideosTab();
        updateStats();
    }

    private void showEmpty() {
        if (emptyStateAll != null) {
            emptyStateAll.setVisible(true);
            emptyStateAll.setManaged(true);
        }
        if (emptyStateGuides != null) {
            emptyStateGuides.setVisible(true);
            emptyStateGuides.setManaged(true);
        }
        if (emptyStateVideos != null) {
            emptyStateVideos.setVisible(true);
            emptyStateVideos.setManaged(true);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ALL UPLOADS TAB
    // ═══════════════════════════════════════════════════════════════
    private void populateAllUploadsTable() {
        if (allUploadsTable == null) return;

        titleColAll.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTitle() != null ? cellData.getValue().getTitle() : "N/A"
            )
        );

        typeColAll.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty("Guide")
        );

        statusColAll.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus() : "pending"
            )
        );

        dateColAll.setCellValueFactory(cellData -> {
            LocalDateTime dt = cellData.getValue().getCreatedAt();
            String dateStr = dt != null ? dt.format(dateFormatter) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        viewsColAll.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getViews())
        );

        // Actions column
        actionsColAll.setCellFactory(param -> new TableCell<GuideVideo, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                } else {
                    GuideVideo guide = getTableRow().getItem();
                    HBox actions = createActionButtons(guide);
                    setGraphic(actions);
                }
            }
        });

        applyFilters();
    }

    private HBox createActionButtons(GuideVideo guide) {
        HBox hbox = new HBox(8);
        hbox.setAlignment(Pos.CENTER_LEFT);

        Button viewBtn = new Button("👁  View");
        viewBtn.setStyle("-fx-background-color: rgba(76,211,227,0.15); -fx-text-fill: #4cd3e3; -fx-font-size: 11; -fx-padding: 6 12 6 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> openGuideVideo(guide));

        Button editBtn = new Button("✏  Edit");
        editBtn.setStyle("-fx-background-color: rgba(255,193,7,0.15); -fx-text-fill: #ffc107; -fx-font-size: 11; -fx-padding: 6 12 6 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        editBtn.setOnAction(e -> editGuideVideo(guide));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.setStyle("-fx-background-color: rgba(232,55,42,0.15); -fx-text-fill: #e8372a; -fx-font-size: 11; -fx-padding: 6 12 6 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteGuideVideo(guide));

        hbox.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        return hbox;
    }

    // ═══════════════════════════════════════════════════════════════
    //  GUIDES TAB
    // ═══════════════════════════════════════════════════════════════
    private void populateGuidesTab() {
        if (guidesGrid == null) return;
        guidesGrid.getChildren().clear();

        List<GuideVideo> guides = allUploads.stream()
                .filter(g -> g.getTitle() != null)
                .toList();

        if (guides.isEmpty()) {
            if (emptyStateGuides != null) {
                emptyStateGuides.setVisible(true);
                emptyStateGuides.setManaged(true);
            }
            return;
        }

        if (emptyStateGuides != null) {
            emptyStateGuides.setVisible(false);
            emptyStateGuides.setManaged(false);
        }

        for (GuideVideo guide : guides) {
            VBox card = createGuideCard(guide);
            guidesGrid.getChildren().add(card);
        }
    }

    private VBox createGuideCard(GuideVideo guide) {
        VBox card = new VBox(8);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: rgba(76,211,227,0.08); -fx-border-color: rgba(76,211,227,0.2); -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12; -fx-cursor: hand;");

        Label title = new Label(guide.getTitle() != null ? guide.getTitle() : "Untitled");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: white; -fx-wrap-text: true;");
        title.setWrapText(true);

        Label game = new Label(guide.getGame() != null ? guide.getGame().getName() : "Unknown");
        game.setStyle("-fx-text-fill: #4cd3e3; -fx-font-size: 11;");

        Label status = new Label(guide.getStatus() != null ? guide.getStatus().toUpperCase() : "PENDING");
        String statusColor = getStatusColor(guide.getStatus());
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 10; -fx-font-weight: bold;");

        Label stats = new Label("👁 " + guide.getViews() + " views");
        stats.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10;");

        card.getChildren().addAll(title, game, status, stats);
        card.setOnMouseClicked(e -> openGuideVideo(guide));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  VIDEOS TAB (same as guides for now)
    // ═══════════════════════════════════════════════════════════════
    private void populateVideosTab() {
        if (videosGrid == null) return;
        videosGrid.getChildren().clear();

        List<GuideVideo> videos = allUploads.stream()
                .filter(v -> v.getVideoUrl() != null && !v.getVideoUrl().isEmpty())
                .toList();

        if (videos.isEmpty()) {
            if (emptyStateVideos != null) {
                emptyStateVideos.setVisible(true);
                emptyStateVideos.setManaged(true);
            }
            return;
        }

        if (emptyStateVideos != null) {
            emptyStateVideos.setVisible(false);
            emptyStateVideos.setManaged(false);
        }

        for (GuideVideo video : videos) {
            VBox card = createVideoCard(video);
            videosGrid.getChildren().add(card);
        }
    }

    private VBox createVideoCard(GuideVideo guide) {
        return createGuideCard(guide); // Same structure for now
    }

    // ═══════════════════════════════════════════════════════════════
    //  STATISTICS TAB
    // ═══════════════════════════════════════════════════════════════
    private void updateStats() {
        int total = allUploads.size();
        int views = allUploads.stream().mapToInt(GuideVideo::getViews).sum();
        int approved = (int) allUploads.stream().filter(g -> "approved".equalsIgnoreCase(g.getStatus())).count();
        int pending = (int) allUploads.stream().filter(g -> "pending".equalsIgnoreCase(g.getStatus())).count();
        int rejected = (int) allUploads.stream().filter(g -> "rejected".equalsIgnoreCase(g.getStatus())).count();

        double approvalRate = total > 0 ? (approved * 100.0) / total : 0.0;
        double avgViews = total > 0 ? (views * 1.0) / total : 0.0;
        double publishedImpact = approved > 0 ? (views * 1.0) / approved : 0.0;

        // Business-oriented quality score balancing moderation success and audience reach.
        double engagementNormalized = Math.min(100.0, avgViews * 10.0);
        double penalty = Math.min(100.0, (pending * 6.0) + (rejected * 10.0));
        int qualityScore = (int) Math.max(0.0, Math.min(100.0, (approvalRate * 0.55) + (engagementNormalized * 0.45) - (penalty * 0.15)));

        // Update stats tab
        if (statTotalUploads != null) statTotalUploads.setText(String.valueOf(total));
        if (statTotalViews != null) statTotalViews.setText(String.valueOf(views));
        if (statApproved != null) statApproved.setText(String.valueOf(approved));
        if (statPending != null) statPending.setText(String.valueOf(pending));
        if (statRejected != null) statRejected.setText(String.valueOf(rejected));

        if (kpiApprovalRate != null) kpiApprovalRate.setText(String.format(Locale.US, "%.1f%%", approvalRate));
        if (kpiAvgViews != null) kpiAvgViews.setText(String.format(Locale.US, "%.1f", avgViews));
        if (kpiPublishedImpact != null) kpiPublishedImpact.setText(String.format(Locale.US, "%.1f", publishedImpact));
        if (kpiQualityScore != null) kpiQualityScore.setText(String.valueOf(qualityScore));

        updateCharts(approved, pending, rejected, total, views);

        // Update header quick stats
        if (headerTotalUploads != null) headerTotalUploads.setText(String.valueOf(total));
        if (headerApproved != null) headerApproved.setText(String.valueOf(approved));
        if (headerPending != null) headerPending.setText(String.valueOf(pending));
        if (headerViews != null) headerViews.setText(String.valueOf(views));
    }

    private void updateCharts(int approved, int pending, int rejected, int total, int views) {
        if (statusPieChart != null) {
            statusPieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Approved", approved),
                    new PieChart.Data("Pending", pending),
                    new PieChart.Data("Rejected", rejected)
            ));
            statusPieChart.setLegendVisible(true);
            statusPieChart.setLabelsVisible(false);
            statusPieChart.setClockwise(true);
        }

        if (performanceBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Uploads", total));
            series.getData().add(new XYChart.Data<>("Views", views));
            series.getData().add(new XYChart.Data<>("Approved", approved));
            series.getData().add(new XYChart.Data<>("Pending", pending));
            series.getData().add(new XYChart.Data<>("Rejected", rejected));
            performanceBarChart.getData().setAll(series);
            performanceBarChart.setAnimated(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FILTERS & SEARCH
    // ═══════════════════════════════════════════════════════════════
    private void applyFilters() {
        String searchText = (searchFieldAll != null ? searchFieldAll.getText() : "").toLowerCase();
        String typeFilter = typeFilterAll != null ? typeFilterAll.getValue() : "All types";
        String statusFilter = statusFilterAll != null ? statusFilterAll.getValue() : "All status";

        filteredUploads = allUploads.stream()
                .filter(g -> g.getTitle() != null && g.getTitle().toLowerCase().contains(searchText))
                .filter(g -> "All types".equals(typeFilter) || "Guide".equals(typeFilter))
                .filter(g -> "All status".equals(statusFilter) || statusFilter.equalsIgnoreCase(g.getStatus()))
                .toList();

        if (allUploadsTable != null) {
            allUploadsTable.getItems().setAll(filteredUploads);
        }

        filterGuidesTab();
        filterVideosTab();
    }

    private void filterGuidesTab() {
        String searchText = (searchFieldGuides != null ? searchFieldGuides.getText() : "").toLowerCase();
        List<GuideVideo> filtered = allUploads.stream()
                .filter(g -> g.getTitle() != null && g.getTitle().toLowerCase().contains(searchText))
                .toList();

        guidesGrid.getChildren().clear();
        if (filtered.isEmpty()) {
            if (emptyStateGuides != null) {
                emptyStateGuides.setVisible(true);
                emptyStateGuides.setManaged(true);
            }
            return;
        }

        if (emptyStateGuides != null) {
            emptyStateGuides.setVisible(false);
            emptyStateGuides.setManaged(false);
        }

        for (GuideVideo guide : filtered) {
            guidesGrid.getChildren().add(createGuideCard(guide));
        }
    }

    private void filterVideosTab() {
        String searchText = (searchFieldVideos != null ? searchFieldVideos.getText() : "").toLowerCase();
        List<GuideVideo> filtered = allUploads.stream()
                .filter(v -> v.getVideoUrl() != null && !v.getVideoUrl().isEmpty())
                .filter(v -> v.getTitle() != null && v.getTitle().toLowerCase().contains(searchText))
                .toList();

        videosGrid.getChildren().clear();
        if (filtered.isEmpty()) {
            if (emptyStateVideos != null) {
                emptyStateVideos.setVisible(true);
                emptyStateVideos.setManaged(true);
            }
            return;
        }

        if (emptyStateVideos != null) {
            emptyStateVideos.setVisible(false);
            emptyStateVideos.setManaged(false);
        }

        for (GuideVideo video : filtered) {
            videosGrid.getChildren().add(createVideoCard(video));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void goToUploadVideo() {
        navigateTo("GuideUpload.fxml");
    }

    private void openGuideVideo(GuideVideo guide) {
        String url = guide != null ? guide.getVideoUrl() : null;
        if (url == null || url.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Missing video", "This guide does not have a video URL.");
            return;
        }

        try {
            showVideoViewer(url, guide != null ? guide.getTitle() : "Guide Video");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Open video error", e.getMessage());
        }
    }

    private void editGuideVideo(GuideVideo guide) {
        if (guide == null) {
            showAlert(Alert.AlertType.WARNING, "Edit guide", "No guide selected.");
            return;
        }

        if (navbarController != null) {
            navbarController.navigateToWithData("GuideUpload.fxml", "guideToEdit", guide);
            return;
        }

        // Fallback when included navbar controller is unavailable.
        navigateTo("GuideUpload.fxml");
    }

    private void showVideoViewer(String url, String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title != null && !title.isBlank() ? title : "Guide Video");

        WebView webView = new WebView();
        webView.setPrefSize(960, 540);
        webView.setStyle("-fx-background-color: black;");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        String html = """
                <!doctype html>
                <html>
                    <head>
                        <meta charset=\"utf-8\" />
                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                        <style>
                            html, body {
                                margin: 0;
                                width: 100%%;
                                height: 100%%;
                                background: #000;
                                overflow: hidden;
                            }
                            video {
                                width: 100%%;
                                height: 100%%;
                                object-fit: contain;
                                background: #000;
                            }
                        </style>
                    </head>
                    <body>
                        <video controls autoplay playsinline>
                            <source src=\"%s\" type=\"video/mp4\" />
                            Your browser does not support the video tag.
                        </video>
                    </body>
                </html>
                """.formatted(escapeHtml(url));

        engine.loadContent(html, "text/html");

        Scene scene = new Scene(new StackPane(webView), 960, 540);
        stage.setScene(scene);
        stage.show();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void deleteGuideVideo(GuideVideo guide) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Upload");
        alert.setHeaderText("Delete \"" + guide.getTitle() + "\"?");
        alert.setContentText("Are you sure you want to delete this upload? This action cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                guideVideoRepository.delete(guide);
                allUploads.remove(guide);
                displayUploads();
                System.out.println("[MyUploads] Deleted: " + guide.getTitle());
            } catch (Exception e) {
                System.err.println("[MyUploads] Error deleting: " + e.getMessage());
            }
        }
    }

    private String getStatusColor(String status) {
        return switch (status != null ? status.toLowerCase() : "pending") {
            case "approved" -> "#00e676";
            case "pending" -> "#ffc107";
            case "rejected" -> "#e8372a";
            default -> "rgba(255,255,255,0.5)";
        };
    }

    private void navigateTo(String fxml) {
        try {
            URL url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) {
                System.err.println("[MyUploads] FXML not found: " + fxml);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
            Scene scene = new Scene(loader.load());
            javafx.stage.Stage stage = resolveStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.show();
            }
        } catch (Exception e) {
            System.err.println("[MyUploads] Navigation error: " + e.getMessage());
        }
    }

    private javafx.stage.Stage resolveStage() {
        return (javafx.stage.Stage) uploadTabs.getScene().getWindow();
    }
}
