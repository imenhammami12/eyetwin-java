package com.eyetwin.controller;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IFeedbackRepository;
import com.eyetwin.interfaces.IFeedbackService;
import com.eyetwin.interfaces.IReviewStreamRepository;
import com.eyetwin.interfaces.IComplaintService;
import com.eyetwin.repository.FeedbackRepository;
import com.eyetwin.repository.ReviewStreamRepository;
import com.eyetwin.services.ComplaintServiceImpl;
import com.eyetwin.services.FeedbackService;
import com.eyetwin.services.FeedbackServiceImpl;
import com.eyetwin.tools.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.util.List;

public class FeedbackController {

    // ── FXML Fields ───────────────────────────────────────────
    @FXML private StackPane mainStack;
    @FXML private VBox      viewForm;
    @FXML private VBox      viewSuccess;

    @FXML private Label     streamTitleLabel;
    @FXML private Slider    ratingSlider;
    @FXML private Label     ratingValueLabel;
    @FXML private TextArea  commentArea;
    @FXML private Button    submitBtn;
    @FXML private Label     errComment;
    @FXML private Label     errGeneral;

    @FXML private Label     successMessage;

    // ── State ─────────────────────────────────────────────────
    private IFeedbackService feedbackService;
    private LiveStream      currentStream;

    // Style constants (same palette as ComplaintController)
    private static final String RED      = "#e8372a";
    private static final String RED_HOT  = "#ff4d3d";
    private static final String MUTED    = "rgba(255,255,255,0.38)";

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        this.feedbackService = new FeedbackServiceImpl();

        // Rating slider: 1–5
        if (ratingSlider != null) {
            ratingSlider.setMin(1);
            ratingSlider.setMax(5);
            ratingSlider.setValue(3);
            ratingSlider.setMajorTickUnit(1);
            ratingSlider.setSnapToTicks(true);
            ratingSlider.setShowTickLabels(true);
            ratingValueLabel.setText("3 / 5");

            ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int v = newVal.intValue();
                ratingValueLabel.setText(v + " / 5");
            });
        }

        showView("form");
    }

    // ═══════════════════════════════════════════════════════════
    //  INJECT STREAM (called from parent controller before load)
    // ═══════════════════════════════════════════════════════════

    public void setLiveStream(LiveStream stream) {
        this.currentStream = stream;
        if (streamTitleLabel != null && stream != null)
            streamTitleLabel.setText("Stream : " + stream.getTitle());
    }

    // ═══════════════════════════════════════════════════════════
    //  SUBMIT
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void handleSubmit() {
        clearErrors();

        String comment = commentArea != null ? commentArea.getText().trim() : "";
        if (comment.length() < 5) {
            setErr(errComment, "Comment must be at least 5 characters.");
            return;
        }

        User user = SessionManager.getCurrentUser();
        if (user == null || currentStream == null) {
            setErr(errGeneral, "Session error. Please log in again.");
            return;
        }

        int rating = ratingSlider != null ? (int) ratingSlider.getValue() : 3;

        setLabel(submitBtn, "Submitting…");
        if (submitBtn != null) submitBtn.setDisable(true);

        new Thread(() -> {
            try {
                StreamFeedback fb = new StreamFeedback();
                fb.setSpectator(user);
                fb.setLiveStream(currentStream);
                fb.setRating(rating);
                fb.setComment(comment);

                feedbackService.processFeedback(fb);

                Platform.runLater(() -> {
                    resetSubmitBtn();
                    setLabel(successMessage,
                            rating >= 4
                                    ? "✅ Thank you for your positive feedback!"
                                    : rating <= 2
                                    ? "📋 Your complaint has been submitted."
                                    : "✅ Your feedback was recorded.");
                    showView("success");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    resetSubmitBtn();
                    setErr(errGeneral, "Error: " + e.getMessage());
                });
            }
        }, "SubmitFeedback").start();
    }

    @FXML
    public void handleCancel() {
        showView("form");
        clearErrors();
        if (commentArea  != null) commentArea.clear();
        if (ratingSlider != null) ratingSlider.setValue(3);
    }

    // ═══════════════════════════════════════════════════════════
    //  VIEW SWITCHER
    // ═══════════════════════════════════════════════════════════

    private void showView(String view) {
        for (VBox v : new VBox[]{viewForm, viewSuccess}) {
            if (v != null) { v.setVisible(false); v.setManaged(false); }
        }
        VBox target = "success".equals(view) ? viewSuccess : viewForm;
        if (target != null) { target.setVisible(true); target.setManaged(true); }
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private void setErr(Label l, String msg) {
        if (l == null) return;
        l.setText(msg);
        l.setVisible(true);
        l.setManaged(true);
        l.setStyle("-fx-text-fill:#ff6b7a;-fx-font-size:11;");
    }

    private void clearErrors() {
        for (Label l : new Label[]{errComment, errGeneral}) {
            if (l != null) { l.setText(""); l.setVisible(false); l.setManaged(false); }
        }
    }

    private void setLabel(Label  l, String v) { if (l != null) l.setText(v); }
    private void setLabel(Button b, String v) { if (b != null) b.setText(v); }

    private void resetSubmitBtn() {
        if (submitBtn != null) {
            submitBtn.setText("🚀  Submit Feedback");
            submitBtn.setDisable(false);
        }
    }
}