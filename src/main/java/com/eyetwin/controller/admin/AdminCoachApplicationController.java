package com.eyetwin.controller.admin;

import com.eyetwin.entities.ApplicationStatus;
import com.eyetwin.entities.CoachApplication;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoachApplicationService;
import com.eyetwin.services.CoachApplicationServiceImpl;
import com.eyetwin.services.EmailService;
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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminCoachApplicationController {

    // ── Sidebar / Topbar ─────────────────────────────────────
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ── LIST — KPI ───────────────────────────────────────────
    @FXML private Label       totalLabel;
    @FXML private Label       pendingLabel;
    @FXML private Label       approvedLabel;
    @FXML private Label       rejectedLabel;
    @FXML private Label       approvalRateLabel;
    @FXML private Label       awaitingLabel;
    @FXML private Label       coachCountLabel;
    @FXML private Label       rejectionRateLabel;
    @FXML private ProgressBar progressTotal;
    @FXML private ProgressBar progressPending;
    @FXML private ProgressBar progressApproved;
    @FXML private ProgressBar progressRejected;

    // ── LIST — Filters ───────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label            resultCountLabel;

    // ── LIST — Table ─────────────────────────────────────────
    @FXML private TableView<CoachApplication>           applicationsTable;
    @FXML private TableColumn<CoachApplication, Void>   colAvatar;
    @FXML private TableColumn<CoachApplication, String> colApplicant;
    @FXML private TableColumn<CoachApplication, String> colEmail;
    @FXML private TableColumn<CoachApplication, String> colSubmitted;
    @FXML private TableColumn<CoachApplication, String> colStatus;
    @FXML private TableColumn<CoachApplication, String> colReviewed;
    @FXML private TableColumn<CoachApplication, Void>   colActions;

    // ── LIST — Pagination ────────────────────────────────────
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  pageNumberLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // ── DETAIL — Left card ───────────────────────────────────
    @FXML private Label    avatarInitialLabel;
    @FXML private Label    applicantNameLabel;
    @FXML private Label    applicantUsernameLabel;
    @FXML private Label    statusBadgeLabel;
    @FXML private VBox     bioBox;
    @FXML private Label    bioLabel;
    @FXML private Label    emailLabel;
    @FXML private Label    submittedLabel;
    @FXML private Label    reviewedLabel;
    @FXML private Label    memberSinceLabel;
    @FXML private Button   viewProfileBtn;
    @FXML private Button   contactBtn;

    // ── DETAIL — Right content ───────────────────────────────
    @FXML private Label    certificationsLabel;
    @FXML private Label    experienceLabel;
    @FXML private VBox     cvBox;
    @FXML private Button   cvFileLabel;        // ← Label → Button
    @FXML private VBox     reviewCommentBox;
    @FXML private Label    reviewCommentTitleLabel;
    @FXML private Label    reviewCommentLabel;
    @FXML private HBox     actionBox;
    @FXML private TextArea approveCommentField;
    @FXML private TextArea rejectCommentField;
    @FXML private Label    rejectErrLabel;

    // ── State ────────────────────────────────────────────────
    private ICoachApplicationService   appService;
    private ObservableList<CoachApplication> allApps = FXCollections.observableArrayList();
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages  = 1;

    // Selected application (passed between list ↔ detail)
    private static CoachApplication selectedApplication;

    // ── Style constants ──────────────────────────────────────
    private static final String BG_FIELD   = "#160a22";
    private static final String RED_BORDER = "rgba(255,60,100,0.30)";

    // ═══════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        appService = new CoachApplicationServiceImpl();

        if (adminSidebarController != null) adminSidebarController.setActivePage("coachapps");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Coach Applications");

        if (applicationsTable != null) initListView();
        if (avatarInitialLabel != null) initDetailView();

        Platform.runLater(this::applyTheme);
    }

    // ═══════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════
    private void initListView() {
        setupFilterCombos();
        setupTable();
        loadAll();
    }


    @FXML
    public void handleOpenCv() {
        if (selectedApplication == null) return;
        String cvPath = selectedApplication.getCvFile();
        if (cvPath == null || cvPath.isBlank()) return;

        new Thread(() -> {
            try {
                java.io.File file;

                if (cvPath.startsWith("http://") || cvPath.startsWith("https://")) {
                    // URL distante → navigateur
                    Desktop.getDesktop().browse(new URI(cvPath));
                    return;
                }

                if (new java.io.File(cvPath).isAbsolute()) {
                    // Chemin absolu stocké en base
                    file = new java.io.File(cvPath);
                } else {
                    // Nom de fichier seul → chercher dans les dossiers connus
                    java.io.File found = resolveUploadedFile(cvPath);
                    if (found != null) {
                        file = found;
                    } else {
                        final String name = cvPath;
                        Platform.runLater(() ->
                                alert(Alert.AlertType.WARNING, "CV Not Found",
                                        "File not found:\n" + name
                                                + "\n\nExpected in:\n" + getUploadDir()));
                        return;
                    }
                }

                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    final java.io.File f = file;
                    Platform.runLater(() ->
                            alert(Alert.AlertType.WARNING, "CV Not Found",
                                    "Cannot open file:\n" + f.getAbsolutePath()));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        alert(Alert.AlertType.ERROR, "Error", "Cannot open CV: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Cherche le fichier dans les dossiers d'upload possibles.
     */
    private java.io.File resolveUploadedFile(String filename) {
        String[] searchDirs = {
                getUploadDir(),
                System.getProperty("user.home") + "/uploads/cv",
                System.getProperty("user.home") + "/uploads",
                System.getProperty("user.dir") + "/uploads/cv",
                System.getProperty("user.dir") + "/uploads",
                "C:/eyetwin/uploads/cv",
                "C:/eyetwin/uploads"
        };
        for (String dir : searchDirs) {
            java.io.File f = new java.io.File(dir, filename);
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Dossier principal d'upload — adapte ce chemin à ton projet.
     */
    private String getUploadDir() {
        return System.getProperty("user.dir") + "/uploads/cv";
    }

    private void setupFilterCombos() {
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Statuses", "PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED"));
            statusFilterCombo.setValue("All Statuses");
            styleCombo(statusFilterCombo);
            statusFilterCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : statusLabel(item));
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:" + BG_FIELD + ";");
                }
            });
            statusFilterCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : statusLabel(item));
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:" + BG_FIELD + ";-fx-padding:8 14;");
                }
            });
        }
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First", "Status"));
            sortCombo.setValue("Newest First");
            styleCombo(sortCombo);
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PENDING"      -> "⏳ Pending";
            case "UNDER_REVIEW" -> "👁 Under Review";
            case "APPROVED"     -> "✓ Approved";
            case "REJECTED"     -> "✕ Rejected";
            default             -> s;
        };
    }

    private void loadAll() {
        new Thread(() -> {
            try {
                List<CoachApplication> apps   = appService.getAllApplications();
                List<User>             coaches = appService.getAllCoaches();
                Map<String,Integer>    stats   = appService.getGlobalStats();
                Platform.runLater(() -> {
                    allApps.setAll(apps);
                    applyFilters();
                    refreshKPI(stats, coaches.size());
                });
            } catch (Exception e) {
                System.err.println("[AdminCoachApplicationController] loadAll: " + e.getMessage());
            }
        }, "LoadApps").start();
    }

    private void refreshKPI(Map<String,Integer> stats, int coachCount) {
        int total    = stats.getOrDefault("total",    0);
        int pending  = stats.getOrDefault("pending",  0);
        int approved = stats.getOrDefault("approved", 0);
        int rejected = stats.getOrDefault("rejected", 0);
        int awaiting = pending + stats.getOrDefault("under_review", 0);
        int approvalRate  = total > 0 ? (int) Math.round(approved  * 100.0 / total) : 0;
        int rejectionRate = total > 0 ? (int) Math.round(rejected  * 100.0 / total) : 0;

        animateCount(totalLabel,        total);
        animateCount(pendingLabel,       pending);
        animateCount(approvedLabel,      approved);
        animateCount(rejectedLabel,      rejected);
        animateCount(awaitingLabel,      awaiting);
        animateCount(coachCountLabel,    coachCount);

        setLabelText(approvalRateLabel,  approvalRate  + "%");
        setLabelText(rejectionRateLabel, rejectionRate + "%");

        setProgress(progressTotal,    1.0);
        setProgress(progressPending,  total > 0 ? (double) pending  / total : 0);
        setProgress(progressApproved, total > 0 ? (double) approved / total : 0);
        setProgress(progressRejected, total > 0 ? (double) rejected / total : 0);

        applyProgressStyle(progressTotal,    "progress-purple");
        applyProgressStyle(progressPending,  "progress-orange");
        applyProgressStyle(progressApproved, "progress-green");
        applyProgressStyle(progressRejected, "progress-red");
    }

    @FXML public void handleFilter()       { currentPage = 1; applyFilters(); }
    @FXML public void handleClearFilters() {
        if (searchField       != null) searchField.clear();
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Statuses");
        if (sortCombo         != null) sortCombo.setValue("Newest First");
        currentPage = 1; applyFilters();
    }

    private void applyFilters() {
        String search = searchField       != null ? searchField.getText().toLowerCase().trim() : "";
        String status = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";
        String sort   = sortCombo         != null ? sortCombo.getValue()         : "Newest First";

        List<CoachApplication> filtered = allApps.stream().filter(a -> {
            // Search in user fields + certifications + experience
            if (!search.isBlank()) {
                User u = a.getUser();
                boolean match = (u != null && (
                        (u.getUsername()    != null && u.getUsername().toLowerCase().contains(search))
                     || (u.getEmail()       != null && u.getEmail().toLowerCase().contains(search))
                     || (u.getFullName()    != null && u.getFullName().toLowerCase().contains(search))))
                     || (a.getCertifications() != null && a.getCertifications().toLowerCase().contains(search))
                     || (a.getExperience()     != null && a.getExperience().toLowerCase().contains(search));
                if (!match) return false;
            }
            // Status filter
            if (status != null && !status.equals("All Statuses")) {
                if (!a.getStatus().name().equalsIgnoreCase(status)) return false;
            }
            return true;
        }).sorted((a, b) -> switch (sort != null ? sort : "Newest First") {
            case "Oldest First" -> {
                if (a.getSubmittedAt() == null) yield 1;
                if (b.getSubmittedAt() == null) yield -1;
                yield a.getSubmittedAt().compareTo(b.getSubmittedAt());
            }
            case "Status" -> a.getStatus().name().compareTo(b.getStatus().name());
            default -> { // Newest First
                if (a.getSubmittedAt() == null) yield 1;
                if (b.getSubmittedAt() == null) yield -1;
                yield b.getSubmittedAt().compareTo(a.getSubmittedAt());
            }
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        if (applicationsTable != null) {
            applicationsTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
            applyTableTheme();
        }

        setLabelText(resultCountLabel,    "Found " + filtered.size() + " application" + (filtered.size() != 1 ? "s" : ""));
        setLabelText(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabelText(paginationInfoLabel, filtered.isEmpty() ? "" :
                "Showing " + (from + 1) + " – " + to + " of " + filtered.size() + " entries");
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }
    @FXML public void goToCoachesList() { navigateTo("AdminCoachesList.fxml"); }

    // ─────────────────────────────────────────────────────
    //  TABLE SETUP
    // ─────────────────────────────────────────────────────
    private void setupTable() {
        if (applicationsTable == null) return;

        // Avatar
        if (colAvatar != null) {
            colAvatar.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<CoachApplication> row = getTableRow();
                    if (row == null || row.getItem() == null) return;
                    User u = row.getItem().getUser();
                    String initials = u != null && u.getUsername() != null && u.getUsername().length() >= 2
                            ? u.getUsername().substring(0, 2).toUpperCase() : "??";
                    Label avatar = new Label(initials);
                    avatar.setStyle(
                        "-fx-background-color:linear-gradient(to bottom right,#667eea,#764ba2);"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;"
                        + "-fx-min-width:38;-fx-min-height:38;-fx-max-width:38;-fx-max-height:38;"
                        + "-fx-background-radius:19;-fx-alignment:center;");
                    setGraphic(avatar);
                    setAlignment(Pos.CENTER);
                }
            });
        }

        // Applicant name + username
        if (colApplicant != null) {
            colApplicant.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null
                        ? (u.getFullName() != null ? u.getFullName() : u.getUsername())
                        + "\n@" + u.getUsername() : "—");
            });
            colApplicant.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    String[] parts = item.split("\n");
                    Label name = new Label(parts[0]);
                    name.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;");
                    VBox box = new VBox(2, name);
                    if (parts.length > 1) {
                        Label uname = new Label(parts[1]);
                        uname.setStyle("-fx-text-fill:rgba(255,255,255,0.45);-fx-font-size:11;");
                        box.getChildren().add(uname);
                    }
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        // Email
        if (colEmail != null) {
            colEmail.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? u.getEmail() : "—");
            });
            colEmail.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:#4facfe;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Submitted date
        if (colSubmitted != null) {
            colSubmitted.setCellValueFactory(d -> {
                var dt = d.getValue().getSubmittedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().substring(0, 10) : "—");
            });
            colSubmitted.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.60);-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Status badge
        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    ApplicationStatus s = ApplicationStatus.fromValue(item);
                    String bg, border, color;
                    switch (s) {
                        case APPROVED    -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.45)";  color="#43e97b"; }
                        case REJECTED    -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.45)";  color="#ff6b7a"; }
                        case UNDER_REVIEW-> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.45)";  color="#4facfe"; }
                        default          -> { bg="rgba(255,193,7,0.15)";   border="rgba(255,193,7,0.45)";   color="#ffd54f"; }
                    }
                    Label badge = new Label(s.getIcon() + " " + s.getLabel());
                    badge.setStyle(
                        "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
                        + "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;"
                        + "-fx-text-fill:" + color + ";-fx-font-size:11;"
                        + "-fx-font-weight:bold;-fx-padding:4 10;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // Reviewed date
        if (colReviewed != null) {
            colReviewed.setCellValueFactory(d -> {
                var dt = d.getValue().getReviewedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().substring(0, 10) : "—");
            });
            colReviewed.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    boolean pending = "—".equals(item);
                    setStyle("-fx-text-fill:" + (pending ? "rgba(255,255,255,0.30)" : "rgba(255,255,255,0.60)")
                            + ";-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Actions
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn    = makeBtn("👁",  "info");
                private final Button approveBtn = makeBtn("✓",  "success");
                private final Button rejectBtn  = makeBtn("✕",  "danger");
                private final HBox   box        = new HBox(5, viewBtn, approveBtn, rejectBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    viewBtn.setOnAction(e -> {
                        CoachApplication a = app(); if (a != null) openDetail(a);
                    });
                    approveBtn.setOnAction(e -> {
                        CoachApplication a = app();
                        if (a != null && a.canBeReviewed()) {
                            selectedApplication = a; openDetail(a);
                        }
                    });
                    rejectBtn.setOnAction(e -> {
                        CoachApplication a = app();
                        if (a != null && a.canBeReviewed()) {
                            selectedApplication = a; openDetail(a);
                        }
                    });
                    tableRowProperty().addListener((obs, o, n) -> {
                        if (n != null) n.itemProperty().addListener((o2, ov, nv) -> refresh(nv));
                    });
                }
                private CoachApplication app() {
                    TableRow<CoachApplication> r = getTableRow();
                    return r != null ? r.getItem() : null;
                }
                private void refresh(CoachApplication a) {
                    if (a == null) { setGraphic(null); return; }
                    approveBtn.setDisable(!a.canBeReviewed());
                    rejectBtn.setDisable(!a.canBeReviewed());
                    approveBtn.setOpacity(a.canBeReviewed() ? 1.0 : 0.35);
                    rejectBtn.setOpacity(a.canBeReviewed()  ? 1.0 : 0.35);
                    setGraphic(box);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<CoachApplication> r = getTableRow();
                    if (r != null && r.getItem() != null) refresh(r.getItem());
                }
            });
        }

        applicationsTable.setRowFactory(tv -> {
            TableRow<CoachApplication> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem()); });
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle(
                "-fx-background-color:rgba(255,255,255,0.08);-fx-background-radius:10;"
                + "-fx-border-color:rgba(255,60,100,0.30);-fx-border-width:1;"
                + "-fx-border-radius:10;-fx-cursor:hand;"); });
            row.setOnMouseExited(e -> { if (!row.isEmpty()) applyRowStyle(row); });
            return row;
        });

        applicationsTable.setPlaceholder(new Label("No applications found"));
    }

    private void applyRowStyle(TableRow<CoachApplication> row) {
        String bg = (row.getIndex() % 2 == 0)
                ? "-fx-background-color:rgba(20,10,35,0.85);"
                : "-fx-background-color:rgba(30,15,45,0.70);";
        row.setStyle(bg + "-fx-background-radius:10;"
                + "-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1;-fx-border-radius:10;");
    }

    private void openDetail(CoachApplication app) {
        selectedApplication = app;
        navigateTo("AdminCoachApplicationDetail.fxml");
    }

    // ═══════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════
    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("Application Details");
        if (selectedApplication == null) { navigateTo("AdminCoachApplications.fxml"); return; }
        populateDetail(selectedApplication);

        Platform.runLater(() -> {
            if (approveCommentField != null) {
                approveCommentField.lookup(".content").setStyle(
                        "-fx-background-color:#160a22;-fx-background-radius:8;");
            }
            if (rejectCommentField != null) {
                rejectCommentField.lookup(".content").setStyle(
                        "-fx-background-color:#160a22;-fx-background-radius:8;");
            }
        });

    }

    private void populateDetail(CoachApplication app) {
        User u = app.getUser();

        // ── Avatar + name ──────────────────────────────────
        String initials = u != null && u.getUsername() != null && u.getUsername().length() >= 2
                ? u.getUsername().substring(0, 2).toUpperCase() : "??";
        setLabelText(avatarInitialLabel,      initials);
        setLabelText(applicantNameLabel,      u != null ? nvl(u.getFullName(), u.getUsername()) : "—");
        setLabelText(applicantUsernameLabel,  u != null ? "@" + u.getUsername() : "@—");
        setLabelText(emailLabel,              u != null ? u.getEmail() : "—");
        setLabelText(memberSinceLabel,        u != null && u.getCreatedAt() != null
                ? u.getCreatedAt().toString().substring(0, 10) : "—");

        // ── Bio ────────────────────────────────────────────
        boolean hasBio = u != null && u.getBio() != null && !u.getBio().isBlank();
        showNode(bioBox, hasBio);
        if (hasBio) {
            String bio = u.getBio().length() > 140 ? u.getBio().substring(0, 140) + "…" : u.getBio();
            setLabelText(bioLabel, bio);
        }

        // ── Status badge ───────────────────────────────────
        ApplicationStatus s = app.getStatus();
        String bg, border, color;
        switch (s) {
            case APPROVED    -> { bg="rgba(67,233,123,0.15)"; border="rgba(67,233,123,0.40)"; color="#43e97b"; }
            case REJECTED    -> { bg="rgba(255,60,100,0.15)"; border="rgba(255,60,100,0.40)"; color="#ff6b7a"; }
            case UNDER_REVIEW-> { bg="rgba(79,172,254,0.15)"; border="rgba(79,172,254,0.40)"; color="#4facfe"; }
            default          -> { bg="rgba(255,193,7,0.15)";  border="rgba(255,193,7,0.40)";  color="#ffd54f"; }
        }
        if (statusBadgeLabel != null) {
            statusBadgeLabel.setText(s.getIcon() + " " + s.getLabel().toUpperCase());
            statusBadgeLabel.setStyle(
                "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-text-fill:" + color + ";-fx-font-size:11;"
                + "-fx-font-weight:bold;-fx-padding:5 14;");
        }

        // ── Dates ──────────────────────────────────────────
        setLabelText(submittedLabel, app.getSubmittedAt() != null
                ? app.getSubmittedAt().toString().substring(0, 16).replace("T", " ") : "—");
        setLabelText(reviewedLabel, app.getReviewedAt() != null
                ? app.getReviewedAt().toString().substring(0, 16).replace("T", " ") : "Not reviewed yet");

        // ── Certifications / Experience ────────────────────
        setLabelText(certificationsLabel, nvl(app.getCertifications(), "No certifications provided."));
        setLabelText(experienceLabel,     nvl(app.getExperience(), "No experience provided."));

        // ── CV ─────────────────────────────────────────────
        boolean hasCv = app.getCvFile() != null && !app.getCvFile().isBlank();
        showNode(cvBox, hasCv);
        if (hasCv) {
            // Affiche seulement le nom du fichier, pas le chemin complet
            String cvPath = app.getCvFile();
            String cvName = cvPath.contains("/") || cvPath.contains("\\")
                    ? new java.io.File(cvPath).getName()
                    : cvPath;
            if (cvFileLabel != null) cvFileLabel.setText("📄  " + cvName);
        }

        // ── Review comment (if already reviewed) ──────────
        boolean hasComment = app.getReviewComment() != null && !app.getReviewComment().isBlank();
        boolean wasReviewed = s == ApplicationStatus.APPROVED || s == ApplicationStatus.REJECTED;
        showNode(reviewCommentBox, wasReviewed && hasComment);
        if (wasReviewed && hasComment) {
            setLabelText(reviewCommentTitleLabel,
                    s == ApplicationStatus.APPROVED ? "💬  Approval Comment" : "💬  Rejection Reason");
            setLabelText(reviewCommentLabel, app.getReviewComment());
            if (reviewCommentLabel != null) {
                reviewCommentLabel.setStyle(
                    "-fx-font-size:13;-fx-padding:16;-fx-line-spacing:4;-fx-text-fill:"
                    + (s == ApplicationStatus.APPROVED ? "#43e97b" : "#ff6b7a") + ";");
            }
            if (reviewCommentBox != null) {
                reviewCommentBox.setStyle(
                    "-fx-background-color:" + (s == ApplicationStatus.APPROVED
                        ? "rgba(67,233,123,0.08)" : "rgba(255,60,100,0.08)") + ";"
                    + "-fx-border-color:" + (s == ApplicationStatus.APPROVED
                        ? "rgba(67,233,123,0.30)" : "rgba(255,60,100,0.30)") + ";"
                    + "-fx-border-radius:12;-fx-background-radius:12;");
            }
        }

        // ── Action forms (only for pending/under_review) ──
        showNode(actionBox, app.canBeReviewed());
    }

    @FXML public void handleViewProfile() {
        if (selectedApplication == null || selectedApplication.getUser() == null) return;
        SessionManager.setSelectedUser(selectedApplication.getUser());
        navigateTo("AdminUserDetail.fxml");
    }

    @FXML public void handleContactEmail() {
        if (selectedApplication == null || selectedApplication.getUser() == null) return;
        String email = selectedApplication.getUser().getEmail();
        try {
            Desktop.getDesktop().mail(new URI("mailto:" + email));
        } catch (Exception e) {
            alert(Alert.AlertType.INFORMATION, "Email", "Contact: " + email);
        }
    }

    @FXML public void handleApprove() {
        if (selectedApplication == null) return;
        String comment = approveCommentField != null ? approveCommentField.getText().trim() : "";
        if (!confirm("Approve Application",
                "Approve application from \"" + getApplicantName() + "\"?\n"
                + "The user will receive the COACH role and a confirmation email.")) return;

        if (approveCommentField != null) approveCommentField.setDisable(true);
        new Thread(() -> {
            try {
                appService.approve(selectedApplication.getId(), comment,
                        SessionManager.getCurrentUser().getId());
                // Send email
                User u = selectedApplication.getUser();
                if (u != null) {
                    sendApprovalEmail(u, comment);
                }
                Platform.runLater(() -> {
                    alert(Alert.AlertType.INFORMATION, "Approved",
                            "Application approved successfully.\nThe user is now a Coach.");
                    // Refresh
                    selectedApplication = appService.findById(selectedApplication.getId());
                    populateDetail(selectedApplication);
                    if (approveCommentField != null) {
                        approveCommentField.clear();
                        approveCommentField.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (approveCommentField != null) approveCommentField.setDisable(false);
                    alert(Alert.AlertType.ERROR, "Error", e.getMessage());
                });
            }
        }).start();
    }

    @FXML public void handleReject() {
        if (selectedApplication == null) return;
        String comment = rejectCommentField != null ? rejectCommentField.getText().trim() : "";
        if (comment.isBlank()) {
            showNode(rejectErrLabel, true);
            setLabelText(rejectErrLabel, "A rejection reason is required.");
            return;
        }
        showNode(rejectErrLabel, false);

        if (!confirm("Reject Application",
                "Reject application from \"" + getApplicantName() + "\"?\n"
                + "The user will be notified by email.")) return;

        if (rejectCommentField != null) rejectCommentField.setDisable(true);
        new Thread(() -> {
            try {
                appService.reject(selectedApplication.getId(), comment,
                        SessionManager.getCurrentUser().getId());
                // Send email
                User u = selectedApplication.getUser();
                if (u != null) {
                    sendRejectionEmail(u, comment);
                }
                Platform.runLater(() -> {
                    alert(Alert.AlertType.INFORMATION, "Rejected",
                            "Application rejected and user notified.");
                    selectedApplication = appService.findById(selectedApplication.getId());
                    populateDetail(selectedApplication);
                    if (rejectCommentField != null) {
                        rejectCommentField.clear();
                        rejectCommentField.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (rejectCommentField != null) rejectCommentField.setDisable(false);
                    alert(Alert.AlertType.ERROR, "Error", e.getMessage());
                });
            }
        }).start();
    }

    private String getApplicantName() {
        if (selectedApplication == null) return "?";
        User u = selectedApplication.getUser();
        return u != null ? nvl(u.getFullName(), u.getUsername()) : "?";
    }

    @FXML public void goBackToList() { navigateTo("AdminCoachApplications.fxml"); }

    // ─────────────────────────────────────────────────────
//  EMAIL NOTIFICATIONS
// ─────────────────────────────────────────────────────
    private void sendApprovalEmail(User user, String comment) {
        int year = java.time.LocalDate.now().getYear();
        String name = user.getFullName() != null ? user.getFullName() : user.getUsername();

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#0a0514;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' border='0' style='background:#0a0514;padding:32px 16px;'>"
                + "<tr><td align='center'>"
                + "<table width='560' cellpadding='0' cellspacing='0' border='0'"
                + " style='background:#0d0618;border-radius:18px;overflow:hidden;"
                + "border:1px solid rgba(67,233,123,0.25);'>"
                + "<tr><td height='5' style='background:linear-gradient(to right,#43e97b,#38f9d7);font-size:0;'>&nbsp;</td></tr>"
                + "<tr><td style='background:#1a0a22;padding:22px 32px;border-bottom:1px solid rgba(67,233,123,0.20);'>"
                + "<span style='font-size:20px;font-weight:900;color:white;'>EYE<span style='color:#43e97b;'>TWIN</span></span>"
                + "</td></tr>"
                + "<tr><td style='padding:36px 32px 24px;text-align:center;'>"
                + "<div style='font-size:48px;margin-bottom:16px;'>&#127881;</div>"
                + "<h1 style='margin:0 0 10px;color:white;font-size:24px;'>Congratulations, " + name + "!</h1>"
                + "<p style='margin:0 0 24px;color:rgba(255,255,255,0.55);font-size:14px;line-height:1.7;'>"
                + "Your <strong style='color:#43e97b;'>Coach application</strong> has been <strong style='color:#43e97b;'>approved</strong>!<br>"
                + "You now have full Coach access on the EyeTwin platform."
                + "</p></td></tr>"
                + "<tr><td style='padding:0 32px 24px;'>"
                + "<div style='background:#160a22;border:1px solid rgba(67,233,123,0.22);border-radius:12px;padding:20px;'>"
                + "<p style='margin:0 0 8px;color:rgba(255,255,255,0.50);font-size:12px;'>Your new role:</p>"
                + "<p style='margin:0;color:#43e97b;font-size:18px;font-weight:bold;'>&#9889; Coach</p>"
                + (comment != null && !comment.isBlank()
                ? "<hr style='border-color:rgba(255,255,255,0.08);margin:14px 0;'>"
                + "<p style='margin:0 0 6px;color:rgba(255,255,255,0.50);font-size:12px;'>Admin comment:</p>"
                + "<p style='margin:0;color:rgba(255,255,255,0.80);font-size:13px;line-height:1.6;'>" + comment + "</p>"
                : "")
                + "</div></td></tr>"
                + "<tr><td align='center' style='padding:8px 32px 36px;'>"
                + "<a href='https://eye2win-metamind.onrender.com'"
                + " style='display:inline-block;padding:14px 36px;"
                + "background:linear-gradient(to right,#43e97b,#38f9d7);"
                + "color:#0a0514;text-decoration:none;border-radius:10px;"
                + "font-weight:bold;font-size:14px;'>Access the Platform &rarr;</a>"
                + "</td></tr>"
                + "<tr><td style='padding:18px 32px;text-align:center;"
                + "border-top:1px solid rgba(255,255,255,0.07);background:rgba(0,0,0,0.15);'>"
                + "<p style='margin:0;color:rgba(255,255,255,0.25);font-size:10px;'>"
                + "&#169; " + year + " EyeTwin E-Sport Platform</p>"
                + "</td></tr></table></td></tr></table></body></html>";

        new Thread(() -> {
            try {
                EmailService.getInstance().sendHtml(
                        user.getEmail(),
                        "&#127881; Your Coach application has been approved!",
                        html
                );
            } catch (Exception e) {
                System.err.println("[CoachApp] Approval email failed: " + e.getMessage());
            }
        }, "CoachApprovalEmail").start();
    }

    private void sendRejectionEmail(User user, String comment) {
        int year = java.time.LocalDate.now().getYear();
        String name = user.getFullName() != null ? user.getFullName() : user.getUsername();

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#0a0514;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' border='0' style='background:#0a0514;padding:32px 16px;'>"
                + "<tr><td align='center'>"
                + "<table width='560' cellpadding='0' cellspacing='0' border='0'"
                + " style='background:#0d0618;border-radius:18px;overflow:hidden;"
                + "border:1px solid rgba(255,60,100,0.25);'>"
                + "<tr><td height='5' style='background:linear-gradient(to right,#ff3c64,#c0132f);font-size:0;'>&nbsp;</td></tr>"
                + "<tr><td style='background:#1a0a22;padding:22px 32px;border-bottom:1px solid rgba(255,60,100,0.20);'>"
                + "<span style='font-size:20px;font-weight:900;color:white;'>EYE<span style='color:#ff3c64;'>TWIN</span></span>"
                + "</td></tr>"
                + "<tr><td style='padding:36px 32px 24px;text-align:center;'>"
                + "<h1 style='margin:0 0 10px;color:white;font-size:22px;'>Application Update</h1>"
                + "<p style='margin:0 0 24px;color:rgba(255,255,255,0.55);font-size:14px;line-height:1.7;'>"
                + "Hello <strong style='color:#ff8fa3;'>" + name + "</strong>,<br>"
                + "After careful review, your Coach application has not been approved at this time."
                + "</p></td></tr>"
                + "<tr><td style='padding:0 32px 28px;'>"
                + "<div style='background:rgba(255,60,100,0.08);border:1px solid rgba(255,60,100,0.30);border-radius:12px;padding:20px;'>"
                + "<p style='margin:0 0 8px;color:rgba(255,255,255,0.50);font-size:12px;'>Reason from our team:</p>"
                + "<p style='margin:0;color:rgba(255,255,255,0.80);font-size:13px;line-height:1.6;'>" + comment + "</p>"
                + "</div></td></tr>"
                + "<tr><td style='padding:0 32px 28px;'>"
                + "<p style='margin:0;color:rgba(255,255,255,0.50);font-size:13px;line-height:1.7;text-align:center;'>"
                + "You are welcome to reapply in the future with additional experience and certifications."
                + "</p></td></tr>"
                + "<tr><td align='center' style='padding:8px 32px 36px;'>"
                + "<a href='https://eye2win-metamind.onrender.com'"
                + " style='display:inline-block;padding:14px 36px;"
                + "background:linear-gradient(to right,#ff3c64,#c0132f);"
                + "color:white;text-decoration:none;border-radius:10px;"
                + "font-weight:bold;font-size:14px;'>Visit Platform &rarr;</a>"
                + "</td></tr>"
                + "<tr><td style='padding:18px 32px;text-align:center;"
                + "border-top:1px solid rgba(255,255,255,0.07);background:rgba(0,0,0,0.15);'>"
                + "<p style='margin:0;color:rgba(255,255,255,0.25);font-size:10px;'>"
                + "&#169; " + year + " EyeTwin E-Sport Platform</p>"
                + "</td></tr></table></td></tr></table></body></html>";

        new Thread(() -> {
            try {
                EmailService.getInstance().sendHtml(
                        user.getEmail(),
                        "Your Coach application status update",
                        html
                );
            } catch (Exception e) {
                System.err.println("[CoachApp] Rejection email failed: " + e.getMessage());
            }
        }, "CoachRejectionEmail").start();
    }

    // ─────────────────────────────────────────────────────
    //  THEME
    // ─────────────────────────────────────────────────────
    private void applyTheme() {
        applyProgressStyle(progressTotal,    "progress-purple");
        applyProgressStyle(progressPending,  "progress-orange");
        applyProgressStyle(progressApproved, "progress-green");
        applyProgressStyle(progressRejected, "progress-red");
        applyTableTheme();
    }

    private void applyTableTheme() {
        if (applicationsTable == null) return;
        applicationsTable.setStyle(
            "-fx-background-color:transparent;-fx-border-color:transparent;"
            + "-fx-table-cell-border-color:transparent;"
            + "-fx-control-inner-background:rgba(20,10,35,0.80);"
            + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node hBg = applicationsTable.lookup(".column-header-background");
            if (hBg != null) hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-padding:0;");
            javafx.scene.Node filler = applicationsTable.lookup(".column-header-background .filler");
            if (filler != null) filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);");
            applicationsTable.lookupAll(".column-header").forEach(n -> n.setStyle(
                "-fx-background-color:rgba(8,4,16,0.98);"
                + "-fx-border-color:transparent transparent rgba(255,60,100,0.35) transparent;"
                + "-fx-border-width:0 0 1 0;-fx-size:48px;"));
            applicationsTable.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;"
                + "-fx-background-color:transparent;-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
    }

    // ─────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────
    private void styleCombo(ComboBox<String> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:" + BG_FIELD + ";-fx-padding:8 14;");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:" + BG_FIELD + ";");
            }
        });
    }

    private Button makeBtn(String text, String variant) {
        Button b = new Button(text);
        String bg, border, color;
        switch (variant) {
            case "success" -> { bg="rgba(67,233,123,0.08)";  border="rgba(67,233,123,0.40)";  color="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.08)";  border="rgba(255,60,100,0.40)";  color="#ff6b7a"; }
            case "info"    -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.40)";  color="#4facfe"; }
            default        -> { bg="rgba(255,255,255,0.05)"; border="rgba(255,255,255,0.15)"; color="white";  }
        }
        b.setStyle(
            "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
            + "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;"
            + "-fx-text-fill:" + color + ";-fx-font-size:12;-fx-padding:5 10;"
            + "-fx-cursor:hand;-fx-font-weight:bold;");
        b.setOnMouseEntered(e -> b.setOpacity(0.8));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private void animateCount(Label label, int target) {
        if (label == null) return;
        int steps = 40; double stepDur = 800.0 / steps;
        final int[] cur = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(stepDur), e -> {
            cur[0]++;
            label.setText(String.valueOf((int) Math.round(target * cur[0] / (double) steps)));
            if (cur[0] >= steps) label.setText(String.valueOf(target));
        }));
        tl.setCycleCount(steps); tl.play();
        label.setStyle("-fx-font-size:44px;-fx-font-weight:bold;-fx-text-fill:white;");
    }

    private void applyProgressStyle(ProgressBar pb, String styleClass) {
        if (pb == null) return;
        pb.getStyleClass().removeIf(s -> s.startsWith("progress-"));
        pb.getStyleClass().add(styleClass);
    }

    private void setLabelText(Label l, String v)        { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v)  { if (pb != null) pb.setProgress(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }
    private String nvl(String s, String fallback)       { return s != null && !s.isBlank() ? s : fallback; }

    private boolean confirm(String title, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }
    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    // ─────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────
    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) { System.err.println("[AdminCoachAppController] FXML not found: " + fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminCoachAppController] Nav error: " + e.getMessage());
        }
    }
    private URL resolveUrl(String fxml) {
        for (String p : new String[]{
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml}) {
            URL u = getClass().getResource(p);
            if (u != null) return u;
        }
        return null;
    }
    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                searchField, applicationsTable,
                avatarInitialLabel, certificationsLabel, approveCommentField};
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    @FXML public void goToDashboard()         { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToTeams()             { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToPlanning()          { navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { navigateTo("AdminTournois.fxml"); }
    @FXML public void goToVideos()            { navigateTo("AdminVideos.fxml"); }
    @FXML public void goToCoachApplications() { navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { navigateTo("AdminMessages.fxml"); }
    @FXML public void goToSite()              { navigateTo("home.fxml"); }
    @FXML public void goToProfile()           { navigateTo("AdminProfile.fxml"); }
    @FXML public void goToAuditLogs()         { if (!SessionManager.isSuperAdmin()) return; navigateTo("AdminAuditLogs.fxml"); }
    @FXML public void handleLogout()          { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }
}
