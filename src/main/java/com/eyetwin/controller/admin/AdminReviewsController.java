package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.Review;
import com.eyetwin.entities.User;
import com.eyetwin.services.ReviewServiceImpl;
import com.eyetwin.services.SentimentAnalysisService;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AdminReviewsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);
    private static final String UPLOAD_DIR = "uploads/";

    @FXML private TableView<Review> reviewsTable;
    @FXML private TableColumn<Review, Review> colUser;
    @FXML private TableColumn<Review, Review> colPlanning;
    @FXML private TableColumn<Review, Review> colRating;
    @FXML private TableColumn<Review, Review> colComment;
    @FXML private TableColumn<Review, Review> colSentiment;
    @FXML private TableColumn<Review, Review> colDate;
    @FXML private TableColumn<Review, Review> colAction;

    @FXML private Label liveLabel;
    @FXML private Label countLabel;
    @FXML private Button analyzeAllBtn;
    @FXML private Label errorLabel;

    private final ReviewServiceImpl reviewService = new ReviewServiceImpl();
    private final SentimentAnalysisService sentimentService = new SentimentAnalysisService();
    private final ObservableList<Review> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) return;
        setupTable();
        loadReviews();
    }

    private void setupTable() {
        if (reviewsTable != null) {
            reviewsTable.setItems(rows);
            reviewsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            // Sentiment column contains 2 stacked controls → needs taller rows
            reviewsTable.setFixedCellSize(96);
        }

        colUser.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colPlanning.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colRating.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colComment.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colSentiment.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colAction.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));

        colUser.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                User u = r.getUser();
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                ImageView avatar = new ImageView();
                avatar.setFitWidth(34);
                avatar.setFitHeight(34);
                avatar.setPreserveRatio(true);
                avatar.getStyleClass().add("avatar-sm");
                if (u != null && u.getProfilePicture() != null && !u.getProfilePicture().isBlank()) {
                    Image img = loadLocalImage(UPLOAD_DIR + u.getProfilePicture());
                    if (img != null) avatar.setImage(img);
                }
                Label name = new Label(u != null && u.getUsername() != null ? u.getUsername() : "—");
                name.getStyleClass().add("cell-muted");
                box.getChildren().addAll(avatar, name);
                setGraphic(box);
                setText(null);
            }
        });

        colPlanning.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                Planning p = r.getPlanning();
                VBox v = new VBox(2);
                Label title = new Label(p != null && p.getType() != null && !p.getType().isBlank() ? p.getType() : "—");
                title.getStyleClass().add("cell-muted");
                String meta = "—";
                if (p != null) {
                    String lvl = p.getLevel() != null ? p.getLevel() : "";
                    String when = (p.getDate() != null ? p.getDate().toString() : "") + (p.getTime() != null ? (" " + p.getTime()) : "");
                    meta = (lvl.isBlank() ? "" : lvl) + (when.isBlank() ? "" : (" • " + when));
                    meta = meta.isBlank() ? "—" : meta;
                }
                Label sub = new Label(meta);
                sub.getStyleClass().add("muted-label");
                v.getChildren().addAll(title, sub);
                setGraphic(v);
                setText(null);
            }
        });

        colRating.setStyle("-fx-alignment: CENTER;");
        colRating.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                setGraphic(starRow(r.getRating()));
                setText(null);
            }
        });

        colComment.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                TextField tf = new TextField(r.getContent() != null ? r.getContent() : "");
                tf.setEditable(false);
                tf.getStyleClass().add("filter-input");
                tf.setPrefHeight(34);
                setGraphic(tf);
                setText(null);
            }
        });

        colSentiment.setStyle("-fx-alignment: CENTER;");
        colSentiment.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                VBox wrap = new VBox(10);
                wrap.setAlignment(Pos.CENTER);
                wrap.setFillWidth(true);

                HBox badge = sentimentPill(r.getSentiment());
                Button analyze = analyzeBtn(() -> analyzeOne(r));

                wrap.getChildren().addAll(badge, analyze);
                setGraphic(wrap);
                setText(null);
            }
        });

        colDate.setStyle("-fx-alignment: CENTER;");
        colDate.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                String t = r.getCreatedAt() != null ? DATE_FMT.format(r.getCreatedAt()) : "—";
                Label lab = new Label(t);
                lab.getStyleClass().add("muted-label");
                setGraphic(lab);
                setText(null);
            }
        });

        colAction.setStyle("-fx-alignment: CENTER;");
        colAction.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setGraphic(null); setText(null); return; }
                Button del = iconBtnSvg("trash", "icon-trash");
                del.setOnAction(e -> deleteReview(r));
                setGraphic(del);
                setText(null);
            }
        });
    }

    private void loadReviews() {
        clearError();
        new Thread(() -> {
            try {
                List<Review> list = reviewService.getAllReviewsWithPlanning();
                Platform.runLater(() -> {
                    rows.setAll(list);
                    updateCounters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setError(e.getMessage()));
            }
        }, "AdminReviews-Load").start();
    }

    private void updateCounters() {
        if (countLabel != null) {
            countLabel.setText(rows.size() + " reviews in total");
            countLabel.getStyleClass().removeAll("badge-no", "badge-yes");
            countLabel.getStyleClass().add("badge-yes");
        }
        if (liveLabel != null) liveLabel.setText("● Live");
    }

    private void analyzeOne(Review r) {
        if (r == null) return;
        clearError();
        new Thread(() -> {
            try {
                String s = sentimentService.analyze(r.getContent(), r.getRating()).join();
                reviewService.updateSentiment(r.getId(), s);
                r.setSentiment(s);
                Platform.runLater(reviewsTable::refresh);
            } catch (Exception e) {
                Platform.runLater(() -> setError("Analyze failed: " + e.getMessage()));
            }
        }, "AdminReviews-AnalyzeOne").start();
    }

    @FXML
    public void handleAnalyzeAll() {
        if (analyzeAllBtn != null) analyzeAllBtn.setDisable(true);
        clearError();
        new Thread(() -> {
            try {
                for (Review r : List.copyOf(rows)) {
                    try {
                        String s = sentimentService.analyze(r.getContent(), r.getRating()).join();
                        reviewService.updateSentiment(r.getId(), s);
                        r.setSentiment(s);
                    } catch (Exception ignored) {
                    }
                }
                Platform.runLater(() -> {
                    if (reviewsTable != null) reviewsTable.refresh();
                    if (analyzeAllBtn != null) analyzeAllBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setError("Analyze all failed: " + e.getMessage());
                    if (analyzeAllBtn != null) analyzeAllBtn.setDisable(false);
                });
            }
        }, "AdminReviews-AnalyzeAll").start();
    }

    private void deleteReview(Review r) {
        if (r == null) return;
        boolean ok = AdminDialogs.confirmDeleteSession();
        if (!ok) return;
        clearError();
        new Thread(() -> {
            try {
                reviewService.deleteReviewAdmin(r.getId());
                Platform.runLater(() -> {
                    rows.remove(r);
                    updateCounters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setError(e.getMessage()));
            }
        }, "AdminReviews-Delete").start();
    }

    @FXML
    public void handleBack() {
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
    }

    private Label sentimentBadge(String sentiment) {
        String s = sentiment == null ? "neutral" : sentiment.toLowerCase(Locale.ROOT);
        String text = switch (s) {
            case "positive" -> "Positive";
            case "negative" -> "Negative";
            default -> "Neutral";
        };
        Label l = new Label(text);
        l.getStyleClass().addAll("sentiment-badge", "sentiment-" + s);
        return l;
    }

    private HBox sentimentPill(String sentiment) {
        String s = sentiment == null ? "neutral" : sentiment.toLowerCase(Locale.ROOT);
        String text = switch (s) {
            case "positive" -> "Positive";
            case "negative" -> "Negative";
            default -> "Neutral";
        };
        HBox pill = new HBox(10);
        pill.setAlignment(Pos.CENTER);
        pill.getStyleClass().addAll("sentiment-pill", "sentiment-" + s);

        Circle dot = new Circle(7);
        dot.getStyleClass().addAll("sentiment-dot", "sentiment-dot-" + s);

        Label label = new Label(text);
        label.getStyleClass().add("sentiment-text");
        label.setMinWidth(Region.USE_PREF_SIZE);

        pill.getChildren().addAll(dot, label);
        pill.setMaxWidth(170);
        return pill;
    }

    private Button analyzeBtn(Runnable onClick) {
        Button b = new Button("Analyze");
        b.getStyleClass().addAll("analyze-pill", "outline-btn");
        b.setContentDisplay(ContentDisplay.LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent("M15.5 14h-.79l-.28-.27a6.471 6.471 0 0 0 1.57-4.23C15.99 6.01 13.98 4 11.49 4S7 6.01 7 8.5 9.01 13 11.5 13c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l4.25 4.25 1.27-1.27L15.5 14zm-4 0C9.01 14 7 11.99 7 9.5S9.01 5 11.5 5 16 7.01 16 9.5 13.99 14 11.5 14z");
        icon.getStyleClass().add("analyze-icon");
        icon.setScaleX(0.95);
        icon.setScaleY(0.95);
        b.setGraphic(icon);

        b.setOnAction(e -> onClick.run());
        b.setMaxWidth(160);
        return b;
    }

    private HBox starRow(int rating) {
        int r = Math.max(0, Math.min(5, rating));
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 5; i++) {
            SVGPath star = new SVGPath();
            star.setContent("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z");
            star.getStyleClass().addAll("star-svg", i <= r ? "star-filled" : "star-empty");
            star.setScaleX(0.9);
            star.setScaleY(0.9);
            box.getChildren().add(star);
        }
        return box;
    }

    private Button iconBtnSvg(String kind, String extraClass) {
        Button b = new Button();
        b.getStyleClass().addAll("icon-btn", extraClass);
        SVGPath svg = new SVGPath();
        svg.setContent(switch (kind) {
            case "trash" -> "M3 6h18v2H3V6zm2 3h14l-1.2 12.4c-.1.9-.9 1.6-1.8 1.6H8c-.9 0-1.7-.7-1.8-1.6L5 9zm3-5h8l1 2H7l1-2z";
            default -> "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
        });
        svg.getStyleClass().addAll("icon-svg", extraClass);
        b.setGraphic(svg);
        b.setMinWidth(34);
        b.setPrefWidth(34);
        b.setMaxWidth(34);
        b.setMinHeight(32);
        b.setPrefHeight(32);
        b.setMaxHeight(32);
        return b;
    }

    private Image loadLocalImage(String relPath) {
        try {
            Path p = Paths.get(relPath);
            if (Files.exists(p)) return new Image(p.toUri().toString(), true);
        } catch (Exception ignored) {
        }
        return null;
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }

    private void setError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg == null ? "" : msg);
    }
}

