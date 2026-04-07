package com.eyetwin.controller;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IComplaintService;
import com.eyetwin.services.ComplaintServiceImpl;
import com.eyetwin.services.SentimentService;
import com.eyetwin.tools.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ComplaintController {

    // ── Navbar ────────────────────────────────────────────────
    @FXML private NavbarController navbarController;

    // ── Vues ─────────────────────────────────────────────────
    @FXML private StackPane mainStack;
    @FXML private VBox viewList;
    @FXML private VBox viewNew;
    @FXML private VBox viewShow;

    // ══ LIST VIEW ════════════════════════════════════════════
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statResolved;
    @FXML private Label statCritical;
    @FXML private VBox  complaintsListBox;
    @FXML private Label emptyStateLabel;
    @FXML private VBox  emptyStateBox;

    // ══ NEW VIEW ═════════════════════════════════════════════
    @FXML private VBox  categoryBox;
    @FXML private TextField subjectField;
    @FXML private TextArea  descriptionArea;
    @FXML private Button    attachmentBtn;
    @FXML private Label     attachmentLabel;
    @FXML private Button    submitBtn;
    @FXML private Button    cancelNewBtn;
    @FXML private Label errSubject;
    @FXML private Label errDescription;
    @FXML private Label errCategory;
    @FXML private Label errGeneral;

    // ══ SHOW VIEW ════════════════════════════════════════════
    @FXML private Label showSubjectLabel;
    @FXML private Label showIdLabel;
    @FXML private Label showStatusBadge;
    @FXML private Label showPriorityBadge;
    @FXML private Label showCategoryBadge;
    @FXML private Label showDescriptionLabel;
    @FXML private Label showCreatedLabel;
    @FXML private Label showUpdatedLabel;
    @FXML private Label showResolvedLabel;

    @FXML private VBox  adminResponseBox;
    @FXML private Label adminResponseLabel;
    @FXML private VBox  resolutionBox;
    @FXML private Label resolutionLabel;

    // Sentiment
    @FXML private VBox  sentimentBox;
    @FXML private Label sentimentEmoji;
    @FXML private Label sentimentTextLabel;
    @FXML private Label sentimentScoreLabel;
    @FXML private ProgressBar sentimentBar;
    @FXML private Label sentimentSourceLabel;
    @FXML private Label sentimentSuggestionLabel;
    @FXML private VBox  sentimentSuggestionBox;
    @FXML private VBox  noSentimentBox;

    // Info card (right column) — renamed to avoid duplicate fx:id
    @FXML private Label infoStatusBadge;
    @FXML private Label infoPriorityBadge;
    @FXML private Label infoCategoryLabel;

    // Actions
    @FXML private Button deleteBtn;
    @FXML private VBox   actionsBox;

    // ── State ─────────────────────────────────────────────────
    private IComplaintService complaintService;
    private SentimentService  sentimentService;
    private ComplaintCategory selectedCategory;
    private File              selectedAttachment;
    private Complaint         currentComplaint;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Style constants
    private static final String DARK       = "#080810";
    private static final String DARK2      = "rgba(6,5,16,0.88)";
    private static final String RED        = "#e8372a";
    private static final String RED_HOT    = "#ff4d3d";
    private static final String MUTED      = "rgba(255,255,255,0.38)";
    private static final String TEXT       = "rgba(255,255,255,0.93)";

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        complaintService = new ComplaintServiceImpl();
        sentimentService  = new SentimentService();

        if (navbarController != null) navbarController.setActivePage("support");

        showView("list");
        loadListData();
    }

    // ═══════════════════════════════════════════════════════════
    //  VIEW SWITCHER
    // ═══════════════════════════════════════════════════════════

    private void showView(String view) {
        for (VBox v : new VBox[]{viewList, viewNew, viewShow}) {
            if (v != null) { v.setVisible(false); v.setManaged(false); }
        }
        VBox target = switch (view) {
            case "new"  -> viewNew;
            case "show" -> viewShow;
            default     -> viewList;
        };
        if (target != null) { target.setVisible(true); target.setManaged(true); }
    }

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════

    private void loadListData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                List<Complaint> complaints = complaintService.getByUser(user.getId());
                Platform.runLater(() -> renderList(complaints));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error loading complaints: " + e.getMessage()));
            }
        }, "LoadComplaints").start();
    }

    private void renderList(List<Complaint> complaints) {
        int total    = complaints.size();
        int pending  = (int) complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING).count();
        int resolved = (int) complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED
                        || c.getStatus() == ComplaintStatus.CLOSED).count();
        int critical = (int) complaints.stream()
                .filter(c -> c.getPriority() == ComplaintPriority.URGENT
                        || c.getPriority() == ComplaintPriority.HIGH).count();

        setLabel(statTotal,    String.valueOf(total));
        setLabel(statPending,  String.valueOf(pending));
        setLabel(statResolved, String.valueOf(resolved));
        setLabel(statCritical, String.valueOf(critical));

        if (complaintsListBox != null) complaintsListBox.getChildren().clear();

        boolean isEmpty = complaints.isEmpty();
        showNode(emptyStateBox,    isEmpty);
        showNode(complaintsListBox, !isEmpty);

        for (Complaint c : complaints) {
            if (complaintsListBox != null)
                complaintsListBox.getChildren().add(buildTicketCard(c));
        }
    }

    private javafx.scene.Node buildTicketCard(Complaint c) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color:" + DARK2 + ";" +
                        "-fx-border-color:rgba(255,255,255,0.065);" +
                        "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-padding:16 20;");

        Label subjectLbl = new Label(c.getSubject());
        subjectLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;");
        Label idLbl = new Label("#" + c.getId());
        idLbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11;" +
                "-fx-background-color:rgba(255,255,255,0.05);" +
                "-fx-border-color:rgba(255,255,255,0.07);-fx-border-radius:4;-fx-background-radius:4;" +
                "-fx-padding:2 7;");
        HBox top = new HBox(10, subjectLbl, idLbl);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(subjectLbl, Priority.ALWAYS);

        String desc = c.getDescription();
        if (desc != null && desc.length() > 140) desc = desc.substring(0, 140) + "…";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        descLbl.setWrapText(true);

        HBox badges = new HBox(6,
                makeBadge(c.getStatus().getLabel(),   getStatusBg(c.getStatus()),
                        getStatusBorder(c.getStatus()), getStatusColor(c.getStatus())),
                makeBadge(c.getPriority().getLabel(),  getPriorityBg(c.getPriority()),
                        getPriorityBorder(c.getPriority()), getPriorityColor(c.getPriority())),
                makeBadge(c.getCategory().getLabel(), "rgba(255,255,255,0.04)",
                        "rgba(255,255,255,0.08)", "rgba(255,255,255,0.45)")
        );

        if (c.hasSentiment()) {
            Label sentBadge = new Label(c.getSentimentEmoji() + " " + c.getSentimentTextLabel());
            sentBadge.setStyle("-fx-text-fill:" + c.getSentimentColor() +
                    ";-fx-font-size:11;-fx-font-weight:bold;");
            badges.getChildren().add(sentBadge);
        }

        // ── Date: LocalDateTime directly, no toLocalDateTime() needed ──
        String dateStr = c.getCreatedAt() != null
                ? c.getCreatedAt().format(DATE_FMT)
                : "—";
        Label dateLbl = new Label("📅 " + dateStr);
        dateLbl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11;");

        Button viewBtn = new Button("👁  View");
        viewBtn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-border-color:rgba(255,255,255,0.09);" +
                        "-fx-border-radius:7;-fx-background-radius:7;" +
                        "-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:11;" +
                        "-fx-cursor:hand;-fx-padding:6 14;");
        viewBtn.setOnAction(e -> openShow(c));
        viewBtn.setOnMouseEntered(e -> viewBtn.setStyle(viewBtn.getStyle()
                .replace("rgba(255,255,255,0.55)", RED_HOT)));
        viewBtn.setOnMouseExited(e -> viewBtn.setStyle(viewBtn.getStyle()
                .replace(RED_HOT, "rgba(255,255,255,0.55)")));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, dateLbl, spacer, viewBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, descLbl, badges, bottom);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("rgba(255,255,255,0.065)", "rgba(232,55,42,0.35)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()
                .replace("rgba(232,55,42,0.35)", "rgba(255,255,255,0.065)")));

        return card;
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW VIEW
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void goToNewView() {
        clearNewForm();
        buildCategoryGrid();
        showView("new");
    }

    @FXML
    public void handleCancelNew() {
        showView("list");
    }

    private void buildCategoryGrid() {
        if (categoryBox == null) return;
        categoryBox.getChildren().clear();
        selectedCategory = null;

        int col = 0;
        HBox row = null;

        for (ComplaintCategory cat : ComplaintCategory.values()) {
            if (col % 2 == 0) {
                row = new HBox(10);
                categoryBox.getChildren().add(row);
            }
            Button btn = buildCategoryBtn(cat);
            btn.setOnAction(e -> selectCategory(cat, btn));
            if (row != null) {
                HBox.setHgrow(btn, Priority.ALWAYS);
                row.getChildren().add(btn);
            }
            col++;
        }
        if (col % 2 != 0 && row != null) {
            Region filler = new Region();
            HBox.setHgrow(filler, Priority.ALWAYS);
            row.getChildren().add(filler);
        }
    }

    private Button buildCategoryBtn(ComplaintCategory cat) {
        Button btn = new Button(getCategoryIcon(cat) + "  " + cat.getLabel());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(inactiveCatStyle());
        btn.setWrapText(true);
        return btn;
    }

    private void selectCategory(ComplaintCategory cat, Button clicked) {
        selectedCategory = cat;
        clearErr(errCategory);
        if (categoryBox != null) {
            categoryBox.getChildren().forEach(node -> {
                if (node instanceof HBox hbox)
                    hbox.getChildren().forEach(child -> {
                        if (child instanceof Button b) b.setStyle(inactiveCatStyle());
                    });
            });
        }
        clicked.setStyle(activeCatStyle());
    }

    @FXML
    public void handleChooseAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Attachment");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Allowed Files", "*.png", "*.jpg", "*.jpeg", "*.pdf", "*.doc", "*.docx"));
        Stage stage = resolveStage();
        if (stage == null) return;
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                showAlert("File size must be less than 5MB");
                return;
            }
            selectedAttachment = file;
            setLabel(attachmentLabel, "📎 " + file.getName());
        }
    }

    @FXML
    public void handleSubmit() {
        clearAllErrors();

        boolean valid = true;
        String subject = subjectField != null ? subjectField.getText().trim() : "";
        if (subject.isEmpty()) {
            setErr(errSubject, "Subject is required.");
            valid = false;
        } else if (subject.length() < 5) {
            setErr(errSubject, "Subject must contain at least 5 characters.");
            valid = false;
        }
        String description = descriptionArea != null ? descriptionArea.getText().trim() : "";
        if (description.length() < 10) {
            setErr(errDescription, "Description must contain at least 10 characters.");
            valid = false;
        }
        if (selectedCategory == null) {
            setErr(errCategory, "Category is required.");
            valid = false;
        }
        if (!valid) return;

        setLabel(submitBtn, "Submitting…");
        if (submitBtn != null) submitBtn.setDisable(true);

        String subjectFinal = subject;
        String descFinal    = description;

        new Thread(() -> {
            try {
                String textToAnalyse = subjectFinal + " " + descFinal;
                SentimentService.SentimentResult sentiment =
                        sentimentService.analyse(textToAnalyse);

                Complaint complaint = new Complaint();
                complaint.setSubject(subjectFinal);
                complaint.setDescription(descFinal);
                complaint.setCategory(selectedCategory);
                complaint.setSubmittedBy(SessionManager.getCurrentUser());

                complaint.setSentimentLabel(sentiment.label);
                complaint.setSentimentScore(sentiment.score);      // Double.valueOf() auto-boxes
                complaint.setSentimentSource(sentiment.source);
                complaint.setSentimentPrioritySuggestion(sentiment.prioritySuggestion);

                if ("URGENT".equals(sentiment.prioritySuggestion)
                        && complaint.getPriority() != ComplaintPriority.URGENT) {
                    complaint.setPriority(ComplaintPriority.URGENT);
                } else if ("HIGH".equals(sentiment.prioritySuggestion)
                        && (complaint.getPriority() == ComplaintPriority.LOW
                        || complaint.getPriority() == ComplaintPriority.MEDIUM)) {
                    complaint.setPriority(ComplaintPriority.HIGH);
                }

                if (selectedAttachment != null) {
                    byte[] bytes = Files.readAllBytes(selectedAttachment.toPath());
                    String ext   = getExtension(selectedAttachment.getName());
                    String filename = "complaint-" + System.currentTimeMillis() + "." + ext;
                    File dest = new File(
                            System.getProperty("user.dir") + "/uploads/complaints/" + filename);
                    dest.getParentFile().mkdirs();
                    Files.write(dest.toPath(), bytes);
                    complaint.setAttachmentPath(filename);
                }

                complaintService.create(complaint);

                Platform.runLater(() -> {
                    resetSubmitBtn();
                    openShow(complaint);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resetSubmitBtn();
                    setErr(errGeneral, "Error: " + ex.getMessage());
                });
            }
        }, "SubmitComplaint").start();
    }

    // ═══════════════════════════════════════════════════════════
    //  SHOW VIEW
    // ═══════════════════════════════════════════════════════════

    private void openShow(Complaint c) {
        currentComplaint = c;
        populateShow(c);
        showView("show");
    }

    private void populateShow(Complaint c) {
        setLabel(showSubjectLabel, c.getSubject());
        setLabel(showIdLabel, "#" + c.getId());

        // Main header badges
        setBadge(showStatusBadge,   c.getStatus().getLabel(),
                getStatusBg(c.getStatus()), getStatusColor(c.getStatus()));
        setBadge(showPriorityBadge, c.getPriority().getLabel(),
                getPriorityBg(c.getPriority()), getPriorityColor(c.getPriority()));
        setBadge(showCategoryBadge, c.getCategory().getLabel(),
                "rgba(255,255,255,0.04)", "rgba(255,255,255,0.45)");

        // Info card badges (different fx:id — infoStatusBadge / infoPriorityBadge)
        setBadge(infoStatusBadge,   c.getStatus().getLabel(),
                getStatusBg(c.getStatus()), getStatusColor(c.getStatus()));
        setBadge(infoPriorityBadge, c.getPriority().getLabel(),
                getPriorityBg(c.getPriority()), getPriorityColor(c.getPriority()));
        if (infoCategoryLabel != null)
            infoCategoryLabel.setText(c.getCategory().getLabel());

        setLabel(showDescriptionLabel, c.getDescription());

        // ── LocalDateTime — format directly, no .toLocalDateTime() ──
        setLabel(showCreatedLabel,
                c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FMT) : "—");
        setLabel(showUpdatedLabel,
                c.getUpdatedAt() != null ? c.getUpdatedAt().format(DATE_FMT) : "—");
        setLabel(showResolvedLabel,
                c.getResolvedAt() != null ? c.getResolvedAt().format(DATE_FMT) : "—");

        boolean hasResponse = c.getAdminResponse() != null && !c.getAdminResponse().isBlank();
        showNode(adminResponseBox, hasResponse);
        if (hasResponse) setLabel(adminResponseLabel, c.getAdminResponse());

        boolean hasNotes = c.getResolutionNotes() != null && !c.getResolutionNotes().isBlank();
        showNode(resolutionBox, hasNotes);
        if (hasNotes) setLabel(resolutionLabel, c.getResolutionNotes());

        populateSentimentShow(c);

        boolean canDelete = c.getStatus() == ComplaintStatus.PENDING;
        showNode(actionsBox, canDelete);
    }

    private void populateSentimentShow(Complaint c) {
        boolean has = c.hasSentiment();
        showNode(noSentimentBox, !has);
        showNode(sentimentBox, has);
        if (!has) return;

        setLabel(sentimentEmoji,    c.getSentimentEmoji());
        setLabel(sentimentTextLabel, c.getSentimentTextLabel());

        // ── getSentimentScore() returns Double (nullable) — safe to check ──
        Double score = c.getSentimentScore();
        setLabel(sentimentScoreLabel, score != null ? (int)(score * 100) + "%" : "—");
        if (sentimentBar != null && score != null)
            sentimentBar.setProgress(score);

        boolean isApi = "api".equals(c.getSentimentSource());
        setLabel(sentimentSourceLabel, isApi ? "✓ AI model (HuggingFace)" : "⚡ Keyword analysis");
        if (sentimentSourceLabel != null)
            sentimentSourceLabel.setStyle("-fx-text-fill:" + (isApi ? "#43e97b" : "#ffd54f")
                    + ";-fx-font-size:11;");

        boolean hasSuggestion = c.getSentimentPrioritySuggestion() != null;
        showNode(sentimentSuggestionBox, hasSuggestion);
        if (hasSuggestion)
            setLabel(sentimentSuggestionLabel,
                    "⚠ Priority auto-elevated to " + c.getSentimentPrioritySuggestion()
                            + " based on sentiment");
    }

    @FXML
    public void handleDelete() {
        if (currentComplaint == null) return;
        if (currentComplaint.getStatus() != ComplaintStatus.PENDING) {
            showAlert("You can only delete pending complaints.");
            return;
        }
        if (!confirm("Delete", "Are you sure you want to delete this complaint?\nThis cannot be undone.")) return;

        new Thread(() -> {
            try {
                complaintService.delete(currentComplaint.getId());
                Platform.runLater(() -> {
                    currentComplaint = null;
                    showView("list");
                    loadListData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void goBackToList() {
        showView("list");
        loadListData();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS — Styles
    // ═══════════════════════════════════════════════════════════

    private Label makeBadge(String text, String bg, String border, String color) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";" +
                        "-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
                        "-fx-text-fill:" + color + ";-fx-font-size:10;" +
                        "-fx-font-weight:bold;-fx-padding:3 8;");
        return l;
    }

    private void setBadge(Label l, String text, String bg, String color) {
        if (l == null) return;
        l.setText(text);
        l.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:rgba(255,255,255,0.12);" +
                        "-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;" +
                        "-fx-text-fill:" + color + ";-fx-font-size:11;" +
                        "-fx-font-weight:bold;-fx-padding:4 12;");
    }

    private String inactiveCatStyle() {
        return "-fx-background-color:rgba(255,255,255,0.022);" +
                "-fx-border-color:rgba(255,255,255,0.065);" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-text-fill:" + TEXT + ";-fx-font-size:12;-fx-font-weight:bold;" +
                "-fx-padding:12 14;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;";
    }

    private String activeCatStyle() {
        return "-fx-background-color:rgba(232,55,42,0.10);" +
                "-fx-border-color:" + RED + ";" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-text-fill:" + RED_HOT + ";-fx-font-size:12;-fx-font-weight:bold;" +
                "-fx-padding:12 14;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;" +
                "-fx-effect:dropshadow(gaussian,rgba(232,55,42,0.18),10,0,0,0);";
    }

    private String getStatusBg(ComplaintStatus s) {
        return switch (s) {
            case PENDING     -> "rgba(255,193,7,0.12)";
            case IN_PROGRESS -> "rgba(13,202,240,0.10)";
            case RESOLVED    -> "rgba(61,214,140,0.10)";
            case CLOSED      -> "rgba(255,255,255,0.05)";
            case REJECTED    -> "rgba(232,55,42,0.12)";
        };
    }
    private String getStatusBorder(ComplaintStatus s) {
        return switch (s) {
            case PENDING     -> "rgba(255,193,7,0.25)";
            case IN_PROGRESS -> "rgba(13,202,240,0.25)";
            case RESOLVED    -> "rgba(61,214,140,0.25)";
            case REJECTED    -> "rgba(232,55,42,0.30)";
            default          -> "rgba(255,255,255,0.10)";
        };
    }
    private String getStatusColor(ComplaintStatus s) {
        return switch (s) {
            case PENDING     -> "#ffc107";
            case IN_PROGRESS -> "#0dcaf0";
            case RESOLVED    -> "#3dd68c";
            case REJECTED    -> "#ff6b4a";
            default          -> MUTED;
        };
    }
    private String getPriorityBg(ComplaintPriority p) {
        return switch (p) {
            case URGENT -> "rgba(232,55,42,0.12)";
            case HIGH   -> "rgba(255,107,43,0.10)";
            case MEDIUM -> "rgba(255,193,7,0.08)";
            default     -> "rgba(255,255,255,0.04)";
        };
    }
    private String getPriorityBorder(ComplaintPriority p) {
        return switch (p) {
            case URGENT -> "rgba(232,55,42,0.28)";
            case HIGH   -> "rgba(255,107,43,0.25)";
            case MEDIUM -> "rgba(255,193,7,0.20)";
            default     -> "rgba(255,255,255,0.08)";
        };
    }
    private String getPriorityColor(ComplaintPriority p) {
        return switch (p) {
            case URGENT -> "#ff6b4a";
            case HIGH   -> "#ff6b2b";
            case MEDIUM -> "#ffc107";
            default     -> MUTED;
        };
    }
    private String getCategoryIcon(ComplaintCategory cat) {
        return switch (cat) {
            case TECHNICAL  -> "🔧";
            case ACCOUNT    -> "🛡";
            case TOURNAMENT -> "🏆";
            case TEAM       -> "👥";
            case PAYMENT    -> "💳";
            case CONTENT    -> "📄";
            case HARASSMENT -> "✋";
            case BUG        -> "🐛";
            default         -> "❓";
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  FORM HELPERS
    // ═══════════════════════════════════════════════════════════

    private void clearNewForm() {
        if (subjectField    != null) subjectField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        selectedCategory   = null;
        selectedAttachment = null;
        setLabel(attachmentLabel, "No file chosen");
        clearAllErrors();
        resetSubmitBtn();
    }

    private void clearAllErrors() {
        for (Label l : new Label[]{errSubject, errDescription, errCategory, errGeneral})
            clearErr(l);
    }

    private void resetSubmitBtn() {
        if (submitBtn != null) {
            submitBtn.setText("🚀  Submit Complaint");
            submitBtn.setDisable(false);
        }
    }

    private void setErr(Label l, String msg) {
        if (l == null) return;
        l.setText(msg); l.setVisible(true); l.setManaged(true);
        l.setStyle("-fx-text-fill:#ff6b7a;-fx-font-size:11;");
    }
    private void clearErr(Label l) {
        if (l != null) { l.setText(""); l.setVisible(false); l.setManaged(false); }
    }
    private void setLabel(Label l, String v)  { if (l != null) l.setText(v); }
    private void setLabel(Button b, String v) { if (b != null) b.setText(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
    private boolean confirm(String title, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════

    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) return;
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[ComplaintController] Nav error: " + e.getMessage());
        }
    }

    private URL resolveUrl(String fxml) {
        for (String p : new String[]{
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml}) {
            URL u = getClass().getResource(p); if (u != null) return u;
        }
        return null;
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                viewList, viewNew, viewShow, mainStack}) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}