package com.eyetwin.controller;

import com.eyetwin.entities.ReviewStream;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IReviewStreamService;
import com.eyetwin.services.ReviewStreamServiceImpl;
import com.eyetwin.tools.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FeedbackDashboardController {

    @FXML private NavbarController navbarController;

    // ── Stats ──────────────────────────────────────────────
    @FXML private Label totalReviewsLabel;
    @FXML private Label avgRatingLabel;
    @FXML private Label positiveLabel;
    @FXML private Label negativeLabel;

    // ── List ───────────────────────────────────────────────
    @FXML private VBox  reviewsListBox;
    @FXML private VBox  emptyStateBox;
    @FXML private Label flashLabel;
    @FXML private VBox  flashBox;

    private IReviewStreamService reviewService;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Colors ─────────────────────────────────────────────
    private static final String DARK2  = "rgba(6,5,16,0.88)";
    private static final String MUTED  = "rgba(255,255,255,0.38)";
    private static final String RED    = "#e8372a";
    private static final String GREEN  = "#3dd68c";
    private static final String YELLOW = "#ffc107";
    private static final String WHITE  = "rgba(255,255,255,0.93)";

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null || !user.isCoach()) {
            // Use Platform.runLater to avoid calling navigateTo before the scene is set
            Platform.runLater(() -> navigateTo("Live.fxml"));
            return;
        }
        if (navbarController != null) navbarController.setActivePage("live");
        reviewService = new ReviewStreamServiceImpl();
        loadData();
    }

    private void loadData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                List<ReviewStream> reviews =
                        reviewService.findByCoachId(user.getId());
                double avg = reviewService.getGlobalAverageRating(user.getId());

                long positive = reviews.stream()
                        .filter(r -> r.getRating() >= 4).count();
                long negative = reviews.stream()
                        .filter(r -> r.getRating() <= 2).count();

                Platform.runLater(() -> renderDashboard(
                        reviews, avg, positive, negative));

            } catch (Exception e) {
                Platform.runLater(() ->
                        showFlash("error", "Error: " + e.getMessage()));
            }
        }, "LoadFeedback").start();
    }

    private void renderDashboard(List<ReviewStream> reviews,
                                 double avg,
                                 long positive, long negative) {
        // ── Stats ──────────────────────────────────────────
        setLabel(totalReviewsLabel, String.valueOf(reviews.size()));
        setLabel(avgRatingLabel,
                avg > 0 ? String.format("%.1f / 5", avg) : "—");
        setLabel(positiveLabel, String.valueOf(positive));
        setLabel(negativeLabel, String.valueOf(negative));

        // ── List ───────────────────────────────────────────
        if (reviewsListBox != null) reviewsListBox.getChildren().clear();

        boolean empty = reviews.isEmpty();
        showNode(emptyStateBox,   empty);
        showNode(reviewsListBox, !empty);

        for (ReviewStream r : reviews) {
            if (reviewsListBox != null)
                reviewsListBox.getChildren().add(buildReviewCard(r));
        }
    }

    private javafx.scene.Node buildReviewCard(ReviewStream r) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color:" + DARK2 + ";" +
                        "-fx-border-color:rgba(255,255,255,0.07);" +
                        "-fx-border-width:1;-fx-border-radius:12;" +
                        "-fx-background-radius:12;-fx-padding:18 22;");

        // ── Top row ────────────────────────────────────────
        String authorName = r.getAuthor() != null
                ? (r.getAuthor().getFullName() != null
                ? r.getAuthor().getFullName()
                : r.getAuthor().getEmail())
                : "Anonymous";

        Label authorLbl = new Label(authorName);
        authorLbl.setStyle("-fx-text-fill:" + WHITE +
                ";-fx-font-weight:bold;-fx-font-size:14;");

        String streamTitle = r.getLiveStream() != null
                ? r.getLiveStream().getTitle() : "—";
        Label streamLbl = new Label("📺 " + streamTitle);
        streamLbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11;" +
                "-fx-background-color:rgba(255,255,255,0.05);" +
                "-fx-border-color:rgba(255,255,255,0.08);" +
                "-fx-border-radius:4;-fx-background-radius:4;" +
                "-fx-padding:2 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, authorLbl, spacer, streamLbl);
        top.setAlignment(Pos.CENTER_LEFT);

        // ── Stars ──────────────────────────────────────────
        String stars = "⭐".repeat(r.getRating())
                + "☆".repeat(5 - r.getRating());
        String ratingColor = r.getRating() >= 4 ? GREEN
                : r.getRating() <= 2 ? RED : YELLOW;

        Label starsLbl = new Label(stars + "  " + r.getRating() + "/5");
        starsLbl.setStyle("-fx-text-fill:" + ratingColor +
                ";-fx-font-size:15;-fx-font-weight:bold;");

        // ── Comment ────────────────────────────────────────
        Label commentLbl = new Label(
                r.getComment() != null && !r.getComment().isBlank()
                        ? r.getComment() : "No comment.");
        commentLbl.setWrapText(true);
        commentLbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13;" +
                "-fx-font-style:italic;");

        // ── Date ───────────────────────────────────────────
        String dateStr = r.getCreatedAt() != null
                ? r.getCreatedAt().format(FMT) : "—";
        Label dateLbl = new Label("📅 " + dateStr);
        dateLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.25);" +
                "-fx-font-size:11;");

        // ── Verified badge ─────────────────────────────────
        Label verifiedLbl = new Label("✔ Verified spectator");
        verifiedLbl.setStyle("-fx-text-fill:" + GREEN +
                ";-fx-font-size:10;-fx-font-weight:bold;" +
                "-fx-background-color:rgba(61,214,140,0.08);" +
                "-fx-border-color:rgba(61,214,140,0.25);" +
                "-fx-border-radius:4;-fx-background-radius:4;" +
                "-fx-padding:2 8;");

        HBox bottom = new HBox(10, dateLbl, verifiedLbl);
        bottom.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, starsLbl, commentLbl, bottom);

        // Hover effect — store base style to avoid string-replace side effects
        final String baseStyle = card.getStyle();
        final String hoverStyle = baseStyle.replace(
                "rgba(255,255,255,0.07)", "rgba(232,55,42,0.25)");
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        return card;
    }

    @FXML
    public void goBack() { navigateTo("Live.fxml"); }

    @FXML
    public void refresh() { loadData(); }

    // ── Helpers ────────────────────────────────────────────

    private void setLabel(Label l, String v) {
        if (l != null) l.setText(v);
    }

    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }

    private void showFlash(String type, String msg) {
        if (flashLabel == null) return;
        flashLabel.setText(msg);
        flashLabel.setStyle("-fx-text-fill:" +
                (type.equals("error") ? "#ff6b7a" : "#3dd68c") + ";");
        showNode(flashBox, true);
    }

    /**
     * Navigate to another FXML view.
     * Uses a cascade of fallback nodes to obtain the Stage safely,
     * even if reviewsListBox has not yet been added to a scene.
     */
    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/eyetwin/views/" + fxml));

            // Try each injected node until we find one with a live scene/window
            Stage stage = resolveStage();
            if (stage == null) {
                System.err.println("[FeedbackDashboardController] Cannot resolve stage for navigation to " + fxml);
                return;
            }

            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            if (stage.getScene() != null)
                scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the first Stage found by probing all @FXML nodes that could
     * be attached to a Scene. Prevents NullPointerException when
     * reviewsListBox has not yet been laid out.
     */
    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                reviewsListBox, emptyStateBox, flashBox,
                totalReviewsLabel, avgRatingLabel, positiveLabel, negativeLabel
        };
        for (javafx.scene.Node node : candidates) {
            if (node != null
                    && node.getScene() != null
                    && node.getScene().getWindow() instanceof Stage) {
                return (Stage) node.getScene().getWindow();
            }
        }
        return null;
    }
}