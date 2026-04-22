package com.eyetwin.controller.admin;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IComplaintService;
import com.eyetwin.services.ComplaintServiceImpl;
import com.eyetwin.tools.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * AdminComplaintController — JavaFX equivalent of Symfony's AdminComplaintController.
 *
 * Two FXML views share this controller:
 *   AdminComplaints.fxml  → list view  (index action)
 *   AdminComplaintDetail.fxml → detail view (show + all write actions)
 */
public class AdminComplaintController {

    // ── Injected sub-controllers ──────────────────────────────────
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ── LIST VIEW — KPI Labels ────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label pendingLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label resolvedLabel;
    @FXML private Label unassignedLabel;
    @FXML private Label rejectedLabel;
    @FXML private Label avgResolutionLabel;
    @FXML private Label resolutionRateLabel;

    @FXML private ProgressBar progressTotal;
    @FXML private ProgressBar progressPending;
    @FXML private ProgressBar progressInProgress;
    @FXML private ProgressBar progressResolved;

    // ── LIST VIEW — Filters ───────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> priorityFilterCombo;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private ComboBox<String> sentimentFilterCombo;
    @FXML private Label            resultCountLabel;

    // ── LIST VIEW — Table ─────────────────────────────────────────
    @FXML private TableView<Complaint>           complaintsTable;
    @FXML private TableColumn<Complaint, String> colUser;
    @FXML private TableColumn<Complaint, String> colSubject;
    @FXML private TableColumn<Complaint, Void>   colCategory;
    @FXML private TableColumn<Complaint, Void>   colPriority;
    @FXML private TableColumn<Complaint, Void>   colStatus;
    @FXML private TableColumn<Complaint, Void>   colSentiment;
    @FXML private TableColumn<Complaint, String> colAssigned;
    @FXML private TableColumn<Complaint, String> colCreated;
    @FXML private TableColumn<Complaint, Void>   colActions;
    @FXML private TableColumn<Complaint, Void> colAvatar;

    // ── LIST VIEW — Pagination ────────────────────────────────────
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  pageNumberLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // ── DETAIL VIEW — Header ──────────────────────────────────────
    @FXML private Label   detailSubjectLabel;
    @FXML private Label   detailStatusBadge;
    @FXML private Label   detailPriorityBadge;
    @FXML private Label   detailCategoryBadge;
    @FXML private Label   detailIdLabel;

    // ── DETAIL VIEW — Body ────────────────────────────────────────
    @FXML private Label       detailDescriptionLabel;
    @FXML private VBox        adminResponseBox;
    @FXML private Label       detailAdminResponseLabel;
    @FXML private VBox        resolutionNotesBox;
    @FXML private Label       detailResolutionNotesLabel;
    @FXML private VBox        attachmentBox;

    // ── DETAIL VIEW — Sidebar info ────────────────────────────────
    @FXML private Label   infoSubmittedBy;
    @FXML private Label   infoAssignedTo;
    @FXML private Label   infoCreatedAt;
    @FXML private Label   infoUpdatedAt;
    @FXML private Label   infoResolvedAt;

    // ── DETAIL VIEW — Sentiment ───────────────────────────────────
    @FXML private VBox    sentimentBox;
    @FXML private Label   sentimentEmoji;
    @FXML private Label   sentimentBadge;
    @FXML private Label   sentimentSourceLabel;
    @FXML private Label   sentimentConfidencePct;
    @FXML private ProgressBar sentimentConfidenceBar;
    @FXML private Label   sentimentSuggestionLabel;
    @FXML private VBox    sentimentSuggestionBox;
    @FXML private Label   noSentimentLabel;

    // ── DETAIL VIEW — Actions ─────────────────────────────────────
    @FXML private ComboBox<String> assignAdminCombo;
    @FXML private ComboBox<String> changeStatusCombo;
    @FXML private ComboBox<String> changePriorityCombo;

    // ── DETAIL VIEW — Response form ───────────────────────────────
    @FXML private TextArea responseTextArea;
    @FXML private VBox     responseFormBox;

    // ── DETAIL VIEW — Resolve form ────────────────────────────────
    @FXML private TextArea resolutionTextArea;
    @FXML private VBox     resolveFormBox;

    // ── State ─────────────────────────────────────────────────────
    private IComplaintService            complaintService;
    private ObservableList<Complaint>    allComplaints = FXCollections.observableArrayList();
    private static final int             PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages  = 1;

    // Stored admin list for assign combo (loaded once)
    private List<User> adminUsers;

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        complaintService = new ComplaintServiceImpl();

        if (adminSidebarController != null) adminSidebarController.setActivePage("complaints");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Complaints Management");

        if (complaintsTable != null) initListView();
        if (detailSubjectLabel != null) initDetailView();

        Platform.runLater(this::applyTheme);
    }

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════

    private void initListView() {
        setupFilterCombos();
        setupComplaintsTable();
        loadAllComplaints();
    }

    private void setupFilterCombos() {
        fillCombo(statusFilterCombo,   "All Statuses",   "PENDING","IN_PROGRESS","RESOLVED","CLOSED","REJECTED");
        fillCombo(priorityFilterCombo, "All Priorities", "LOW","MEDIUM","HIGH","URGENT");
        fillCombo(categoryFilterCombo, "All Categories",
                  "TECHNICAL","ACCOUNT","TOURNAMENT","TEAM","PAYMENT","CONTENT","HARASSMENT","BUG","OTHER");
        fillCombo(sentimentFilterCombo,"All Sentiments", "NEGATIVE","NEUTRAL","POSITIVE");
    }

    private void fillCombo(ComboBox<String> combo, String placeholder, String... values) {
        if (combo == null) return;
        combo.getItems().clear();
        combo.getItems().add(placeholder);
        combo.getItems().addAll(values);
        combo.setValue(placeholder);
        styleCombo(combo);
    }

    private void styleCombo(ComboBox<String> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:#160a22;-fx-padding:8 14;");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:#160a22;");
            }
        });
    }

    private void loadAllComplaints() {
        new Thread(() -> {
            try {
                List<Complaint> complaints = complaintService.getAll();
                ComplaintStats  stats      = complaintService.getStatistics();
                Platform.runLater(() -> {
                    allComplaints.setAll(complaints);
                    applyFilters();
                    refreshKPICards(stats);
                });
            } catch (SQLException e) {
                System.err.println("[AdminComplaintController] loadAllComplaints: " + e.getMessage());
            }
        }, "LoadComplaints").start();
    }

    private void refreshKPICards(ComplaintStats s) {
        animateCount(totalLabel,      s.getTotal());
        animateCount(pendingLabel,    s.getPending());
        animateCount(inProgressLabel, s.getInProgress());
        animateCount(resolvedLabel,   s.getResolved());
        animateCount(unassignedLabel, s.getUnassigned());
        animateCount(rejectedLabel,   s.getRejected());

        if (avgResolutionLabel  != null) avgResolutionLabel.setText(s.getAvgResolutionLabel());
        if (resolutionRateLabel != null) resolutionRateLabel.setText(s.getResolutionRate() + "%");

        int total = s.getTotal();
        setProgress(progressTotal,      1.0);
        setProgress(progressPending,    total > 0 ? (double) s.getPending()    / total : 0);
        setProgress(progressInProgress, total > 0 ? (double) s.getInProgress() / total : 0);
        setProgress(progressResolved,   total > 0 ? (double) s.getResolved()   / total : 0);

        applyProgressStyle(progressTotal,      "progress-purple");
        applyProgressStyle(progressPending,    "progress-red");
        applyProgressStyle(progressInProgress, "progress-blue");
        applyProgressStyle(progressResolved,   "progress-green");
    }

    @FXML public void handleFilter()       { currentPage = 1; applyFilters(); }
    @FXML public void handleClearFilters() {
        if (searchField          != null) searchField.clear();
        if (statusFilterCombo    != null) statusFilterCombo.setValue(statusFilterCombo.getItems().get(0));
        if (priorityFilterCombo  != null) priorityFilterCombo.setValue(priorityFilterCombo.getItems().get(0));
        if (categoryFilterCombo  != null) categoryFilterCombo.setValue(categoryFilterCombo.getItems().get(0));
        if (sentimentFilterCombo != null) sentimentFilterCombo.setValue(sentimentFilterCombo.getItems().get(0));
        currentPage = 1; applyFilters();
    }

    private void applyFilters() {
        String keyword   = searchField          != null ? searchField.getText().toLowerCase().trim() : "";
        String statusRaw = statusFilterCombo    != null ? statusFilterCombo.getValue()    : null;
        String prioRaw   = priorityFilterCombo  != null ? priorityFilterCombo.getValue()  : null;
        String catRaw    = categoryFilterCombo  != null ? categoryFilterCombo.getValue()  : null;
        String sentRaw   = sentimentFilterCombo != null ? sentimentFilterCombo.getValue() : null;

        ComplaintStatus   statusF   = isPlaceholder(statusRaw)  ? null : ComplaintStatus.fromValue(statusRaw);
        ComplaintPriority priorityF = isPlaceholder(prioRaw)    ? null : ComplaintPriority.fromValue(prioRaw);
        ComplaintCategory categoryF = isPlaceholder(catRaw)     ? null : ComplaintCategory.fromValue(catRaw);
        String sentimentF = (sentRaw == null || sentRaw.startsWith("All")) ? null : sentRaw;

        List<Complaint> filtered = allComplaints.stream().filter(c -> {
            if (!keyword.isBlank()) {
                String hay = (c.getSubject()                                         + " "
                            + c.getDescription()                                     + " "
                            + (c.getSubmittedBy() != null ? c.getSubmittedBy().getUsername() : ""))
                            .toLowerCase();
                if (!hay.contains(keyword)) return false;
            }
            if (statusF   != null && c.getStatus()   != statusF)   return false;
            if (priorityF != null && c.getPriority() != priorityF) return false;
            if (categoryF != null && c.getCategory() != categoryF) return false;
            if (sentimentF != null && !sentimentF.equalsIgnoreCase(c.getSentimentLabel())) return false;
            return true;
        }).sorted((a, b) -> {
            int pCmp = Integer.compare(b.getPriority().getWeight(), a.getPriority().getWeight());
            if (pCmp != 0) return pCmp;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        if (complaintsTable != null) {
            complaintsTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
            applyTableTheme(complaintsTable);
        }

        setLabel(resultCountLabel,    "Found " + filtered.size() + " complaint" + (filtered.size() != 1 ? "s" : ""));
        setLabel(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabel(paginationInfoLabel, filtered.isEmpty() ? "" :
                "Showing " + (from + 1) + "–" + to + " of " + filtered.size());
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    private boolean isPlaceholder(String val) {
        return val == null || val.startsWith("All");
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }

    // ─────────────────────────────────────────────────────────────
    //  COMPLAINTS TABLE SETUP
    // ─────────────────────────────────────────────────────────────

    private void setupComplaintsTable() {
        if (complaintsTable == null) return;

        // ── User column ──────────────────────────────────────────────

        // ── Avatar column ────────────────────────────────────────────
        if (colAvatar != null) {
            colAvatar.setCellFactory(col -> new TableCell<>() {
                {
                    setAlignment(Pos.CENTER);
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(null);
                    if (c == null) return;
                    User u = c.getSubmittedBy();
                    if (u == null) {
                        setGraphic(makeInitialsAvatar("?", "linear-gradient(to bottom right,#667eea,#764ba2)"));
                        return;
                    }

                    // Essayer de charger la photo de profil
                    String photoFile = u.getProfilePicture();
                    if (photoFile != null && !photoFile.isBlank()) {
                        try {
                            java.io.File file = new java.io.File(
                                    System.getProperty("user.dir") + "/uploads/profiles/" + photoFile);
                            if (file.exists()) {
                                javafx.scene.image.Image img =
                                        new javafx.scene.image.Image(file.toURI().toString(), 36, 36, true, true);
                                if (!img.isError()) {
                                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                                    iv.setFitWidth(36); iv.setFitHeight(36); iv.setPreserveRatio(false);
                                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                                    iv.setClip(clip);
                                    StackPane pane = new StackPane(iv);
                                    pane.setMinSize(36, 36); pane.setMaxSize(36, 36);
                                    pane.setStyle(
                                            "-fx-background-radius:18;" +
                                                    "-fx-border-radius:18;" +
                                                    "-fx-border-color:rgba(255,255,255,0.20);" +
                                                    "-fx-border-width:2;");
                                    setGraphic(pane);
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // Fallback initiales
                    String initials = u.getUsername() != null && !u.getUsername().isEmpty()
                            ? u.getUsername().substring(0, Math.min(2, u.getUsername().length())).toUpperCase()
                            : "?";
                    String gradient = getAvatarGradient(u);
                    setGraphic(makeInitialsAvatar(initials, gradient));
                }

                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }
        if (colUser != null) {
            colUser.setCellValueFactory(d -> {
                User u = d.getValue().getSubmittedBy();
                return new SimpleStringProperty(u != null ? "@" + u.getUsername() : "—");
            });
            colUser.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:#43e97b;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:0 12;");
                }
            });
        }

        // ── Subject column ───────────────────────────────────────────
        if (colSubject != null) {
            colSubject.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSubject()));
            colSubject.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); setText(null); return; }
                    String truncated = item.length() > 40 ? item.substring(0, 40) + "…" : item;
                    Label lbl = new Label(truncated);
                    lbl.setStyle("-fx-text-fill:white;-fx-font-size:12;");
                    Tooltip.install(lbl, new Tooltip(item));
                    setGraphic(lbl);
                    setText(null);
                }
            });
        }

        // ── Category column — FIX: listener pattern ──────────────────
        if (colCategory != null) {
            colCategory.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(null);
                    if (c == null) return;
                    setGraphic(makeBadge(
                            c.getCategory().getLabel(),
                            "rgba(255,255,255,0.15)",
                            "rgba(255,255,255,0.3)",
                            "rgba(255,255,255,0.7)"));
                    setText(null);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }

        // ── Priority column — FIX: listener pattern ──────────────────
        if (colPriority != null) {
            colPriority.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(null);
                    if (c == null) return;
                    ComplaintPriority p = c.getPriority();
                    setGraphic(makeBadge(
                            p.getLabel(),
                            "rgba(255,255,255,0.08)",
                            p.getColor().replace(")", ",0.4)").replace("rgb(", "rgba("),
                            p.getColor()));
                    setText(null);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }

        // ── Status column — FIX: listener pattern ───────────────────
        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(null);
                    if (c == null) return;
                    ComplaintStatus s = c.getStatus();
                    setGraphic(makeBadge(
                            s.getLabel(),
                            s.getBgColor(),
                            s.getColor().replace(")", ",0.4)"),
                            s.getColor()));
                    setText(null);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }

        // ── Sentiment column — FIX: listener pattern ─────────────────
        if (colSentiment != null) {
            colSentiment.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(null);
                    if (c == null) return;
                    if (!c.hasSentiment()) {
                        Label l = new Label("—");
                        l.setStyle("-fx-text-fill:rgba(255,255,255,0.3);-fx-font-size:12;");
                        setGraphic(l);
                        return;
                    }
                    Label badge = new Label(c.getSentimentEmoji() + " " + c.getSentimentTextLabel());
                    badge.setStyle("-fx-text-fill:" + c.getSentimentColor()
                            + ";-fx-font-size:11;-fx-font-weight:bold;");
                    setGraphic(badge);
                    setText(null);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }

        // ── Assigned column ──────────────────────────────────────────
        if (colAssigned != null) {
            colAssigned.setCellValueFactory(d -> {
                User a = d.getValue().getAssignedTo();
                return new SimpleStringProperty(a != null ? a.getUsername() : null);
            });
            colAssigned.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setText(null); return; }
                    if (item == null) {
                        setText("Unassigned");
                        setStyle("-fx-text-fill:rgba(255,255,255,0.35);-fx-font-size:11;-fx-padding:0 12;");
                    } else {
                        setText("@" + item);
                        setStyle("-fx-text-fill:#4facfe;-fx-font-size:12;-fx-padding:0 12;");
                    }
                }
            });
        }

        // ── Created column ───────────────────────────────────────────
        if (colCreated != null) {
            colCreated.setCellValueFactory(d -> {
                var dt = d.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().substring(0, 10) : "—");
            });
            colCreated.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.5);-fx-font-size:11;-fx-padding:0 12;");
                }
            });
        }

        // ── Actions column — FIX: listener pattern ───────────────────
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = makeBtn("👁", "info");
                private final HBox   box     = new HBox(6, viewBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null)
                            newRow.itemProperty().addListener((o, oldC, newC) -> refresh(newC));
                    });
                    viewBtn.setOnAction(e -> {
                        TableRow<Complaint> row = getTableRow();
                        if (row != null && row.getItem() != null) openDetail(row.getItem());
                    });
                }
                private void refresh(Complaint c) {
                    setGraphic(c == null ? null : box);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Complaint> row = getTableRow();
                    if (row != null && row.getItem() != null) refresh(row.getItem());
                    else setGraphic(null);
                }
            });
        }

        // ── Row factory ──────────────────────────────────────────────
        complaintsTable.setRowFactory(tv -> {
            TableRow<Complaint> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem());
            });
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle(
                        "-fx-background-color:rgba(255,255,255,0.07);-fx-cursor:hand;");
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) row.setStyle("");
            });
            return row;
        });

        complaintsTable.setPlaceholder(new Label("No complaints found"));
    }

    // helper for cell factories (needs to call getTableRow() in context)
    private Label makeBadge(String text, String bg, String border, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
                + "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;"
                + "-fx-text-fill:" + color + ";-fx-font-size:11;"
                + "-fx-font-weight:bold;-fx-padding:3 9;");
        return l;
    }

    private javafx.scene.layout.StackPane makeInitialsAvatar(String initials, String gradient) {
        Label lbl = new Label(initials);
        lbl.setStyle(
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;" +
                        "-fx-background-color:" + gradient + ";" +
                        "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;" +
                        "-fx-background-radius:18;-fx-alignment:center;" +
                        "-fx-border-color:rgba(255,255,255,0.15);-fx-border-width:2;-fx-border-radius:18;");
        StackPane pane = new StackPane(lbl);
        pane.setMinSize(36, 36); pane.setMaxSize(36, 36);
        return pane;
    }

    private String getAvatarGradient(User u) {
        if (u == null) return "linear-gradient(to bottom right,#667eea,#764ba2)";
        String roles = u.getRolesJson() != null ? u.getRolesJson() : "";
        if (roles.contains("ROLE_ADMIN"))  return "linear-gradient(to bottom right,#ff3c64,#ff1744)";
        if (roles.contains("ROLE_COACH"))  return "linear-gradient(to bottom right,#f093fb,#f5576c)";
        return "linear-gradient(to bottom right,#667eea,#764ba2)";
    }

    private void openDetail(Complaint complaint) {
        SessionManager.setSelectedComplaint(complaint);
        navigateTo("AdminComplaintDetail.fxml");
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════════

    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("Complaint Details");
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null) { navigateTo("AdminComplaints.fxml"); return; }
        populateDetail(c);
        loadAdminUsersForCombo();
    }

    private void populateDetail(Complaint c) {
        // ── Header ────────────────────────────────────────────
        setLabel(detailSubjectLabel,  c.getSubject());
        setLabel(detailIdLabel,       "Complaint #" + c.getId());

        setBadgeLabel(detailStatusBadge,   c.getStatus().getLabel(),
                c.getStatus().getBgColor(), c.getStatus().getColor());
        setBadgeLabel(detailPriorityBadge, c.getPriority().getLabel(),
                "rgba(255,255,255,0.08)", c.getPriority().getColor());
        setBadgeLabel(detailCategoryBadge, c.getCategory().getLabel(),
                "rgba(255,255,255,0.08)", "rgba(255,255,255,0.7)");

        // ── Description ───────────────────────────────────────
        setLabel(detailDescriptionLabel, c.getDescription());

        // ── Admin response ────────────────────────────────────
        boolean hasResponse = c.getAdminResponse() != null && !c.getAdminResponse().isBlank();
        showNode(adminResponseBox, hasResponse);
        if (hasResponse) setLabel(detailAdminResponseLabel, c.getAdminResponse());

        // ── Resolution notes ──────────────────────────────────
        boolean hasNotes = c.getResolutionNotes() != null && !c.getResolutionNotes().isBlank();
        showNode(resolutionNotesBox, hasNotes);
        if (hasNotes) setLabel(detailResolutionNotesLabel, c.getResolutionNotes());

        // ── Attachment ────────────────────────────────────────
        showNode(attachmentBox, c.getAttachmentPath() != null);

        // ── Sidebar info ──────────────────────────────────────
        setLabel(infoSubmittedBy, c.getSubmittedBy() != null ? "@" + c.getSubmittedBy().getUsername() : "—");
        setLabel(infoAssignedTo,  c.getAssignedTo()  != null ? "@" + c.getAssignedTo().getUsername()  : "Not assigned");
        setLabel(infoCreatedAt,   c.getCreatedAt()   != null ? formatDt(c.getCreatedAt().toString())  : "—");
        setLabel(infoUpdatedAt,   c.getUpdatedAt()   != null ? formatDt(c.getUpdatedAt().toString())  : "—");
        setLabel(infoResolvedAt,  c.getResolvedAt()  != null ? formatDt(c.getResolvedAt().toString()) : "—");

        // ── Sentiment ─────────────────────────────────────────
        populateSentiment(c);

        // ── Action combos ─────────────────────────────────────
        populateStatusCombo(c);
        populatePriorityCombo(c);

        // ── Forms visibility (hide if resolved) ───────────────
        boolean editable = !c.isResolved();
        showNode(responseFormBox, editable);
        showNode(resolveFormBox,  editable);
    }

    private void populateSentiment(Complaint c) {
        boolean has = c.hasSentiment();
        showNode(noSentimentLabel, !has);
        showNode(sentimentBox, has);
        if (!has) return;

        setLabel(sentimentEmoji, c.getSentimentEmoji());
        setBadgeLabel(sentimentBadge, c.getSentimentTextLabel(),
                "rgba(255,255,255,0.08)", c.getSentimentColor());

        if (sentimentSourceLabel != null) {
            boolean isApi = "api".equals(c.getSentimentSource());
            sentimentSourceLabel.setText(isApi ? "✓ AI model (HuggingFace)" : "⚡ Keyword analysis");
            sentimentSourceLabel.setStyle("-fx-text-fill:" + (isApi ? "#43e97b" : "#ffd54f")
                    + ";-fx-font-size:11;");
        }

        int pct = (int) Math.round(c.getSentimentScore() * 100);
        setLabel(sentimentConfidencePct, pct + "%");
        if (sentimentConfidenceBar != null) sentimentConfidenceBar.setProgress(c.getSentimentScore());

        boolean hasSuggestion = c.getSentimentPrioritySuggestion() != null;
        showNode(sentimentSuggestionBox, hasSuggestion);
        if (hasSuggestion) setLabel(sentimentSuggestionLabel, c.getSentimentPrioritySuggestion());
    }

    private void populateStatusCombo(Complaint c) {
        if (changeStatusCombo == null) return;
        changeStatusCombo.getItems().clear();
        for (ComplaintStatus s : ComplaintStatus.values())
            changeStatusCombo.getItems().add(s.name());
        changeStatusCombo.setValue(c.getStatus().name());
        styleCombo(changeStatusCombo);
    }

    private void populatePriorityCombo(Complaint c) {
        if (changePriorityCombo == null) return;
        changePriorityCombo.getItems().clear();
        for (ComplaintPriority p : ComplaintPriority.values())
            changePriorityCombo.getItems().add(p.name());
        changePriorityCombo.setValue(c.getPriority().name());
        styleCombo(changePriorityCombo);
    }

    private void loadAdminUsersForCombo() {
        if (assignAdminCombo == null) return;
        assignAdminCombo.getItems().clear();
        assignAdminCombo.getItems().add("— Unassign —");
        assignAdminCombo.setValue("— Unassign —");
        styleCombo(assignAdminCombo);

        new Thread(() -> {
            try {
                // Charger les admins depuis la DB via UserService
                com.eyetwin.interfaces.IUserService userService =
                        new com.eyetwin.services.UserServiceImpl();
                List<User> admins = userService.getAllUsers().stream()
                        .filter(u -> {
                            String roles = u.getRolesJson() != null ? u.getRolesJson() : "";
                            return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN");
                        })
                        .toList();

                adminUsers = admins;

                Platform.runLater(() -> {
                    Complaint c = SessionManager.getSelectedComplaint();

                    for (User admin : admins) {
                        assignAdminCombo.getItems().add(admin.getUsername());
                    }

                    // Sélectionner l'admin déjà assigné si existant
                    if (c != null && c.getAssignedTo() != null) {
                        assignAdminCombo.setValue(c.getAssignedTo().getUsername());
                    }

                    styleCombo(assignAdminCombo);
                });
            } catch (Exception e) {
                System.err.println("[AdminComplaintController] loadAdminUsersForCombo: " + e.getMessage());
            }
        }, "LoadAdminUsers").start();
    }
    // ─────────────────────────────────────────────────────────────
    //  DETAIL ACTIONS  (mirrors each Symfony route)
    // ─────────────────────────────────────────────────────────────

    /** POST /admin/complaints/{id}/assign */
    @FXML public void handleAssign() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null || assignAdminCombo == null) return;
        String selected = assignAdminCombo.getValue();
        if (selected == null || selected.startsWith("Select")) return;

        new Thread(() -> {
            try {
                if (selected.contains("Unassign")) {
                    complaintService.unassign(c.getId());
                } else {
                    // Trouver l'admin par username dans la liste chargée
                    User targetAdmin = null;
                    if (adminUsers != null) {
                        targetAdmin = adminUsers.stream()
                                .filter(u -> u.getUsername().equals(selected))
                                .findFirst()
                                .orElse(null);
                    }
                    int adminId = targetAdmin != null
                            ? targetAdmin.getId()
                            : SessionManager.getCurrentUser().getId();
                    complaintService.assign(c.getId(), adminId);
                }
                reloadDetail(c.getId());
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }
    /** POST /admin/complaints/{id}/update-status */
    @FXML public void handleUpdateStatus() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null || changeStatusCombo == null) return;
        String val = changeStatusCombo.getValue();
        if (val == null) return;

        ComplaintStatus newStatus = ComplaintStatus.fromValue(val);
        if (!c.getStatus().canTransitionTo(newStatus)) {
            alert(Alert.AlertType.WARNING, "Invalid Transition",
                    "Cannot change from " + c.getStatus().getLabel()
                    + " to " + newStatus.getLabel() + ".\n\n"
                    + "Allowed: " + allowedTransitionNames(c.getStatus()));
            return;
        }

        new Thread(() -> {
            try {
                complaintService.updateStatus(c.getId(), newStatus);
                reloadDetail(c.getId());
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    /** POST /admin/complaints/{id}/update-priority */
    @FXML public void handleUpdatePriority() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null || changePriorityCombo == null) return;
        String val = changePriorityCombo.getValue();
        if (val == null) return;

        ComplaintPriority newPriority = ComplaintPriority.fromValue(val);
        new Thread(() -> {
            try {
                complaintService.updatePriority(c.getId(), newPriority);
                reloadDetail(c.getId());
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    /** POST /admin/complaints/{id}/respond */
    @FXML public void handleRespond() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null || responseTextArea == null) return;
        String text = responseTextArea.getText();
        if (text == null || text.isBlank()) {
            alert(Alert.AlertType.WARNING, "Empty Response", "Response cannot be empty.");
            return;
        }
        new Thread(() -> {
            try {
                complaintService.addAdminResponse(c.getId(), text,
                        SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { if (responseTextArea != null) responseTextArea.clear(); });
                reloadDetail(c.getId());
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    /** POST /admin/complaints/{id}/resolve */
    @FXML public void handleResolve() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null || resolutionTextArea == null) return;
        String notes = resolutionTextArea.getText();
        if (notes == null || notes.isBlank()) {
            alert(Alert.AlertType.WARNING, "Missing Notes", "Resolution notes are required.");
            return;
        }
        if (!confirm("Resolve Complaint",
                "Mark complaint #" + c.getId() + " as resolved?")) return;

        new Thread(() -> {
            try {
                complaintService.resolve(c.getId(), notes);
                Platform.runLater(() -> { if (resolutionTextArea != null) resolutionTextArea.clear(); });
                reloadDetail(c.getId());
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    /** POST /admin/complaints/{id}/delete */
    @FXML public void handleDelete() {
        Complaint c = SessionManager.getSelectedComplaint();
        if (c == null) return;
        if (!confirm("Delete Complaint",
                "Permanently delete complaint #" + c.getId() + "?\nThis cannot be undone!")) return;

        new Thread(() -> {
            try {
                complaintService.delete(c.getId());
                SessionManager.clearSelectedComplaint();
                Platform.runLater(() -> navigateTo("AdminComplaints.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    @FXML public void goBackToList() {
        SessionManager.clearSelectedComplaint();
        navigateTo("AdminComplaints.fxml");
    }

    // ─────────────────────────────────────────────────────────────
    //  RELOAD HELPER
    // ─────────────────────────────────────────────────────────────

    private void reloadDetail(int complaintId) {
        new Thread(() -> {
            try {
                Complaint refreshed = complaintService.getById(complaintId);
                SessionManager.setSelectedComplaint(refreshed);
                Platform.runLater(() -> populateDetail(refreshed));
            } catch (Exception ex) {
                System.err.println("[AdminComplaintController] reloadDetail: " + ex.getMessage());
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════
    //  THEME
    // ═══════════════════════════════════════════════════════════

    private void applyTheme() {
        applyProgressStyle(progressTotal,      "progress-purple");
        applyProgressStyle(progressPending,    "progress-red");
        applyProgressStyle(progressInProgress, "progress-blue");
        applyProgressStyle(progressResolved,   "progress-green");
        if (complaintsTable != null) applyTableTheme(complaintsTable);
    }

    private void applyTableTheme(TableView<?> table) {
        table.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;"
                        + "-fx-table-cell-border-color:transparent;"
                        + "-fx-control-inner-background:rgba(20,10,35,0.80);"
                        + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");

        Platform.runLater(() -> Platform.runLater(() -> {
            // ── Filler (le carré blanc) ───────────────────────────
            javafx.scene.Node filler = table.lookup(".column-header-background .filler");
            if (filler != null)
                filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-border-color:transparent;");

            // ── Header background ─────────────────────────────────
            javafx.scene.Node hBg = table.lookup(".column-header-background");
            if (hBg != null)
                hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);");

            // ── Chaque colonne header ─────────────────────────────
            table.lookupAll(".column-header").forEach(n -> n.setStyle(
                    "-fx-background-color:rgba(8,4,16,0.98);"
                            + "-fx-border-color:transparent transparent rgba(79,172,254,0.35) transparent;"
                            + "-fx-border-width:0 0 1 0;-fx-size:46px;"));

            // ── Labels dans les headers ───────────────────────────
            table.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                    "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;"));
        }));
    }
    // ═══════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════

    private void animateCount(Label label, int target) {
        if (label == null) return;
        int steps = 40;
        double stepDur = 800.0 / steps;
        final int[] cur = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(stepDur), e -> {
            cur[0]++;
            int val = (int) Math.round(target * cur[0] / (double) steps);
            if (cur[0] >= steps) val = target;
            label.setText(String.valueOf(val));
        }));
        tl.setCycleCount(steps);
        tl.play();
    }

    private void applyProgressStyle(ProgressBar pb, String styleClass) {
        if (pb == null) return;
        pb.getStyleClass().removeIf(s -> s.startsWith("progress-") || s.startsWith("strength-"));
        pb.getStyleClass().add(styleClass);
    }

    private void setLabel(Label l, String v)    { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v) { if (pb != null) pb.setProgress(v); }

    private void setBadgeLabel(Label l, String text, String bg, String color) {
        if (l == null) return;
        l.setText(text);
        l.setStyle("-fx-background-color:" + bg + ";"
                + "-fx-border-color:" + color.replace(")", ",0.4)") + ";"
                + "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;"
                + "-fx-text-fill:" + color + ";-fx-font-size:12;"
                + "-fx-font-weight:bold;-fx-padding:4 12;");
    }

    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }

    private String formatDt(String isoString) {
        if (isoString == null || isoString.length() < 16) return isoString;
        return isoString.substring(0, 10) + " " + isoString.substring(11, 16);
    }

    private String allowedTransitionNames(ComplaintStatus s) {
        ComplaintStatus[] allowed = s.allowedTransitions();
        if (allowed.length == 0) return "none (final status)";
        StringBuilder sb = new StringBuilder();
        for (ComplaintStatus t : allowed) sb.append(t.getLabel()).append(", ");
        return sb.substring(0, sb.length() - 2);
    }

    private Button makeBtn(String text, String variant) {
        Button b = new Button(text);
        String color = switch (variant) {
            case "success" -> "#43e97b";
            case "danger"  -> "#ff6b7a";
            case "warning" -> "#ffb700";
            case "info"    -> "#4facfe";
            default        -> "white";
        };
        b.setStyle("-fx-background-color:rgba(255,255,255,0.05);"
                + "-fx-border-color:" + color.replace(")", ",0.4)") + ";"
                + "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;"
                + "-fx-text-fill:" + color + ";-fx-font-size:12;"
                + "-fx-padding:5 10;-fx-cursor:hand;-fx-font-weight:bold;");
        b.setOnMouseEntered(e -> b.setOpacity(0.75));
        b.setOnMouseExited( e -> b.setOpacity(1.0));
        return b;
    }

    private boolean confirm(String title, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════

    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) { System.err.println("[AdminComplaintController] FXML not found: " + fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminComplaintController] Nav error: " + e.getMessage());
        }
    }

    private URL resolveUrl(String fxml) {
        for (String prefix : new String[]{
                "/com/eyetwin/views/", "/com/eyetwin/view/", "/com/eyetwin/"}) {
            URL u = getClass().getResource(prefix + fxml);
            if (u != null) return u;
        }
        return null;
    }

    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                searchField, complaintsTable, detailSubjectLabel, responseTextArea};
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    // ── Sidebar shortcuts ─────────────────────────────────────────
    @FXML public void goToDashboard()   { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()       { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToTeams()       { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToComplaints()  { navigateTo("AdminComplaints.fxml"); }
    @FXML public void handleLogout()    { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }
}
