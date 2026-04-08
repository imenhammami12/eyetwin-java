package com.eyetwin.controller.admin;

import com.eyetwin.entities.AuditLog;
import com.eyetwin.interfaces.IAuditLogService;
import com.eyetwin.services.AuditLogServiceImpl;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AdminAuditLogController {

    // ── Sidebar / Topbar ──────────────────────────────────────────
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ── LIST VIEW — KPI ───────────────────────────────────────────
    @FXML private Label       statTotalLabel;
    @FXML private Label       statTodayLabel;
    @FXML private Label       statWeekLabel;
    @FXML private Label       statMonthLabel;
    @FXML private ProgressBar progressToday;
    @FXML private ProgressBar progressWeek;
    @FXML private ProgressBar progressMonth;

    // ── LIST VIEW — Filters ───────────────────────────────────────
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private ComboBox<String> entityTypeFilterCombo;
    @FXML private DatePicker       dateFromPicker;
    @FXML private DatePicker       dateToPicker;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label            resultCountLabel;

    // ── LIST VIEW — Table ─────────────────────────────────────────
    @FXML private TableView<AuditLog>           logsTable;
    @FXML private TableColumn<AuditLog, String> colUser;
    @FXML private TableColumn<AuditLog, Void>   colAction;
    @FXML private TableColumn<AuditLog, String> colEntity;
    @FXML private TableColumn<AuditLog, String> colDate;
    @FXML private TableColumn<AuditLog, String> colDetails;
    @FXML private TableColumn<AuditLog, String> colIp;
    @FXML private TableColumn<AuditLog, Void>   colActions;

    // ── LIST VIEW — Pagination ────────────────────────────────────
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  pageNumberLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // ── DETAIL VIEW ───────────────────────────────────────────────
    @FXML private Label    detailActionIcon;
    @FXML private Label    detailIdLabel;
    @FXML private Label    detailActionBadge;
    @FXML private Label    detailUserLabel;
    @FXML private Label    detailEmailLabel;
    @FXML private Label    detailEntityLabel;
    @FXML private Label    detailEntityIdLabel;
    @FXML private Label    detailIpLabel;
    @FXML private Label    detailDateLabel;
    @FXML private Label    detailTimeLabel;
    @FXML private Label    detailDayLabel;
    @FXML private Label    detailElapsedLabel;
    @FXML private TextArea detailDetailsArea;
    @FXML private Label    summaryActionLabel;
    @FXML private Label    summaryEntityLabel;
    @FXML private Label    summaryUserLabel;

    // ── State ─────────────────────────────────────────────────────
    private IAuditLogService           auditLogService;
    private ObservableList<AuditLog>   allLogs   = FXCollections.observableArrayList();
    private static final int           PAGE_SIZE = 20;
    private int currentPage = 1;
    private int totalPages  = 1;

    // Shared selected log (list → detail navigation)
    private static AuditLog selectedLog;

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("EEEE");

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        auditLogService = new AuditLogServiceImpl();

        if (adminSidebarController != null) adminSidebarController.setActivePage("audit");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Audit Logs");

        // Detect which view is active by checking injected nodes
        if (logsTable != null)        initListView();
        if (detailIdLabel != null)    initDetailView();
    }

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════
    private void initListView() {
        setupFilterCombos();
        setupLogsTable();
        loadData();
    }

    private void setupFilterCombos() {
        styleCombo(actionFilterCombo);
        styleCombo(entityTypeFilterCombo);
        styleCombo(sortCombo);

        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First", "Action A→Z", "Action Z→A"));
            sortCombo.setValue("Newest First");
        }
    }

    private void styleCombo(ComboBox<String> combo) {
        if (combo == null) return;
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
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:#160a22;");
            }
        });
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<AuditLog>  logs        = auditLogService.findAll();
                List<String>    actions     = auditLogService.getDistinctActions();
                List<String>    entityTypes = auditLogService.getDistinctEntityTypes();
                int total  = auditLogService.countAll();
                int today  = auditLogService.countSince(LocalDateTime.now().toLocalDate().atStartOfDay());
                int week   = auditLogService.countSince(LocalDateTime.now().minusDays(7));
                int month  = auditLogService.countSince(LocalDateTime.now().minusDays(30));

                Platform.runLater(() -> {
                    // Populate filter dropdowns
                    if (actionFilterCombo != null) {
                        List<String> opts = new java.util.ArrayList<>();
                        opts.add("All Actions");
                        opts.addAll(actions);
                        actionFilterCombo.setItems(FXCollections.observableArrayList(opts));
                        actionFilterCombo.setValue("All Actions");
                    }
                    if (entityTypeFilterCombo != null) {
                        List<String> opts = new java.util.ArrayList<>();
                        opts.add("All Types");
                        opts.addAll(entityTypes);
                        entityTypeFilterCombo.setItems(FXCollections.observableArrayList(opts));
                        entityTypeFilterCombo.setValue("All Types");
                    }

                    allLogs.setAll(logs);
                    applyFilters();
                    refreshKPICards(total, today, week, month);
                });
            } catch (Exception e) {
                System.err.println("[AdminAuditLogController] loadData: " + e.getMessage());
            }
        }, "LoadAuditLogs").start();
    }

    private void refreshKPICards(int total, int today, int week, int month) {
        animateCount(statTotalLabel, total);
        animateCount(statTodayLabel, today);
        animateCount(statWeekLabel,  week);
        animateCount(statMonthLabel, month);

        setProgress(progressToday, total > 0 ? Math.min(1.0, (double) today  / total) : 0);
        setProgress(progressWeek,  total > 0 ? Math.min(1.0, (double) week   / total) : 0);
        setProgress(progressMonth, total > 0 ? Math.min(1.0, (double) month  / total) : 0);
    }

    private void animateCount(Label label, int target) {
        if (label == null) return;
        int steps = 40;
        double stepDur = 700.0 / steps;
        final int[] cur = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(stepDur), e -> {
            cur[0]++;
            int val = (int) Math.round(target * cur[0] / (double) steps);
            if (cur[0] >= steps) val = target;
            label.setText(String.valueOf(val));
        }));
        tl.setCycleCount(steps);
        tl.play();
        label.setStyle("-fx-font-size:44px;-fx-font-weight:bold;-fx-text-fill:white;");
    }

    // ── Filter handlers ───────────────────────────────────────────
    @FXML public void handleFilter()       { currentPage = 1; applyFilters(); }
    @FXML public void handleClearFilters() {
        if (actionFilterCombo     != null) actionFilterCombo.setValue("All Actions");
        if (entityTypeFilterCombo != null) entityTypeFilterCombo.setValue("All Types");
        if (dateFromPicker        != null) dateFromPicker.setValue(null);
        if (dateToPicker          != null) dateToPicker.setValue(null);
        if (sortCombo             != null) sortCombo.setValue("Newest First");
        currentPage = 1;
        applyFilters();
    }

    private void applyFilters() {
        String action     = val(actionFilterCombo,     "All Actions");
        String entityType = val(entityTypeFilterCombo, "All Types");
        String sort       = val(sortCombo,             "Newest First");
        LocalDate from    = dateFromPicker != null ? dateFromPicker.getValue() : null;
        LocalDate to      = dateToPicker   != null ? dateToPicker.getValue()   : null;

        List<AuditLog> filtered = allLogs.stream().filter(log -> {
            if (!"All Actions".equals(action) && !action.equals(log.getAction())) return false;
            if (!"All Types".equals(entityType) && !entityType.equals(log.getEntityType())) return false;
            if (from != null && log.getCreatedAt().toLocalDate().isBefore(from)) return false;
            if (to   != null && log.getCreatedAt().toLocalDate().isAfter(to))    return false;
            return true;
        }).sorted((a, b) -> switch (sort) {
            case "Oldest First" -> a.getCreatedAt().compareTo(b.getCreatedAt());
            case "Action A→Z"   -> (a.getAction() != null ? a.getAction() : "").compareTo(b.getAction() != null ? b.getAction() : "");
            case "Action Z→A"   -> (b.getAction() != null ? b.getAction() : "").compareTo(a.getAction() != null ? a.getAction() : "");
            default             -> b.getCreatedAt().compareTo(a.getCreatedAt()); // Newest First
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int fromIdx = (currentPage - 1) * PAGE_SIZE;
        int toIdx   = Math.min(fromIdx + PAGE_SIZE, filtered.size());

        if (logsTable != null) {
            logsTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIdx, toIdx)));
            applyTableTheme(logsTable);
        }

        setLabel(resultCountLabel,    "Found " + filtered.size() + " log" + (filtered.size() != 1 ? "s" : ""));
        setLabel(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabel(paginationInfoLabel, filtered.isEmpty() ? "" :
                "Showing " + (fromIdx + 1) + " – " + toIdx + " of " + filtered.size() + " entries");
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }

    // ── Table setup ───────────────────────────────────────────────
    private void setupLogsTable() {
        if (logsTable == null) return;

        // User column
        if (colUser != null) {
            colUser.setCellValueFactory(d -> {
                AuditLog log = d.getValue();
                String name = log.getUser() != null ? "@" + log.getUser().getUsername() : "System";
                return new SimpleStringProperty(name);
            });
            colUser.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    boolean isSystem = "System".equals(item);
                    setText(item);
                    setStyle("-fx-text-fill:" + (isSystem ? "rgba(255,255,255,0.40)" : "#43e97b")
                           + ";-fx-font-weight:bold;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Action column — coloured badge
        if (colAction != null) {
            colAction.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<AuditLog> row = getTableRow();
                    if (row == null || row.getItem() == null) return;
                    AuditLog log = row.getItem();
                    setGraphic(buildActionBadge(log.getAction(), log.getActionCategory()));
                    setText(null);
                }
            });
        }

        // Entity column
        if (colEntity != null) {
            colEntity.setCellValueFactory(d -> {
                AuditLog log = d.getValue();
                if (log.getEntityType() == null) return new SimpleStringProperty("—");
                String val = log.getEntityType();
                if (log.getEntityId() != null) val += " #" + log.getEntityId();
                return new SimpleStringProperty(val);
            });
            colEntity.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.65);-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Date column
        if (colDate != null) {
            colDate.setCellValueFactory(d -> {
                LocalDateTime dt = d.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.format(DATE_FMT) + "\n" + dt.format(TIME_FMT) : "—");
            });
            colDate.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    String[] parts = item.split("\n");
                    Label dateLbl = new Label(parts[0]);
                    dateLbl.setStyle("-fx-text-fill:white;-fx-font-size:12;-fx-font-weight:bold;");
                    Label timeLbl = new Label(parts.length > 1 ? parts[1] : "");
                    timeLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.40);-fx-font-size:11;");
                    VBox box = new VBox(2, dateLbl, timeLbl);
                    setGraphic(box); setText(null);
                }
            });
        }

        // Details column — truncated
        if (colDetails != null) {
            colDetails.setCellValueFactory(d -> {
                String det = d.getValue().getDetails();
                return new SimpleStringProperty(det != null ? det : "—");
            });
            colDetails.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    String display = item.length() > 60 ? item.substring(0, 60) + "…" : item;
                    Label lbl = new Label(display);
                    lbl.setStyle("-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:11;");
                    lbl.setTooltip(new Tooltip(item));
                    setGraphic(lbl); setText(null);
                }
            });
        }

        // IP column
        if (colIp != null) {
            colIp.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getIpAddress() != null
                        ? d.getValue().getIpAddress() : "N/A"));
            colIp.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:#fbbf24;-fx-font-family:monospace;"
                           + "-fx-font-size:11;-fx-padding:0 16;");
                }
            });
        }

        // Actions column — view button only
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = makeBtn("👁", "info");
                private final HBox   box     = new HBox(viewBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    viewBtn.setOnAction(e -> {
                        TableRow<AuditLog> r = getTableRow();
                        if (r != null && r.getItem() != null) openDetail(r.getItem());
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        // Double-click to open detail
        logsTable.setRowFactory(tv -> {
            TableRow<AuditLog> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem()); });
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08);-fx-cursor:hand;"); });
            row.setOnMouseExited(e  -> { if (!row.isEmpty()) row.setStyle(
                    (row.getIndex() % 2 == 0)
                    ? "-fx-background-color:rgba(20,10,35,0.85);"
                    : "-fx-background-color:rgba(30,15,45,0.70);"); });
            return row;
        });

        logsTable.setPlaceholder(new Label("No audit logs found"));
    }

    private Label buildActionBadge(String action, String category) {
        String text = action != null ? action.replace("_", " ") : "UNKNOWN";
        String bg, border, color;
        switch (category) {
            case "danger"  -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.45)";  color="#ff6b7a"; }
            case "success" -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.45)";  color="#43e97b"; }
            case "warning" -> { bg="rgba(255,171,0,0.15)";   border="rgba(255,171,0,0.45)";   color="#ffb700"; }
            default        -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.45)";  color="#4facfe"; }
        }
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
            + "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;"
            + "-fx-text-fill:" + color + ";-fx-font-size:11;"
            + "-fx-font-weight:bold;-fx-padding:4 10;");
        return badge;
    }

    private void openDetail(AuditLog log) {
        selectedLog = log;
        navigateTo("AdminAuditLogDetail.fxml");
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════════
    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("Audit Log Detail");
        if (selectedLog == null) { navigateTo("AdminAuditLogs.fxml"); return; }
        populateDetail(selectedLog);
    }

    private void populateDetail(AuditLog log) {
        // Icon based on category
        String icon = switch (log.getActionCategory()) {
            case "danger"  -> "🗑";
            case "success" -> "✅";
            case "warning" -> "✏️";
            default        -> "📋";
        };
        setLabel(detailActionIcon, icon);
        setLabel(detailIdLabel,    "Log #" + log.getId());

        // Action badge
        if (detailActionBadge != null) {
            String action   = log.getAction() != null ? log.getAction().replace("_", " ") : "UNKNOWN";
            String bg, border, color;
            switch (log.getActionCategory()) {
                case "danger"  -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.40)";  color="#ff6b7a"; }
                case "success" -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.40)";  color="#43e97b"; }
                case "warning" -> { bg="rgba(255,171,0,0.15)";   border="rgba(255,171,0,0.40)";   color="#ffb700"; }
                default        -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.40)";  color="#4facfe"; }
            }
            detailActionBadge.setText(action);
            detailActionBadge.setStyle(
                "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-text-fill:" + color + ";-fx-font-size:12;"
                + "-fx-font-weight:bold;-fx-padding:5 14;");
        }

        // User info
        if (log.getUser() != null) {
            setLabel(detailUserLabel,  "@" + log.getUser().getUsername());
            setLabel(detailEmailLabel, log.getUser().getEmail() != null ? log.getUser().getEmail() : "—");
        } else {
            setLabel(detailUserLabel,  "System");
            setLabel(detailEmailLabel, "—");
        }

        // Entity
        String entityDisplay = log.getEntityType() != null ? log.getEntityType() : "N/A";
        setLabel(detailEntityLabel,   entityDisplay);
        setLabel(detailEntityIdLabel, log.getEntityId() != null ? "#" + log.getEntityId() : "N/A");

        // IP
        setLabel(detailIpLabel, log.getIpAddress() != null ? log.getIpAddress() : "N/A");

        // Timeline
        if (log.getCreatedAt() != null) {
            setLabel(detailDateLabel,    log.getCreatedAt().format(DATE_FMT));
            setLabel(detailTimeLabel,    log.getCreatedAt().format(TIME_FMT));
            setLabel(detailDayLabel,     log.getCreatedAt().format(DAY_FMT));
            setLabel(detailElapsedLabel, elapsed(log.getCreatedAt()));
        }

        // Details area
        if (detailDetailsArea != null)
            detailDetailsArea.setText(log.getDetails() != null ? log.getDetails() : "No details available.");

        // Summary
        setLabel(summaryActionLabel, log.getAction() != null ? log.getAction().replace("_", " ") : "—");
        setLabel(summaryEntityLabel, entityDisplay);
        setLabel(summaryUserLabel,   log.getUser() != null ? "@" + log.getUser().getUsername() : "System");
    }

    private String elapsed(LocalDateTime dt) {
        long secs = ChronoUnit.SECONDS.between(dt, LocalDateTime.now());
        if (secs < 60)   return secs + " seconds ago";
        long mins = secs / 60;
        if (mins < 60)   return mins + " minutes ago";
        long hrs = mins / 60;
        if (hrs < 24)    return hrs + " hours ago";
        long days = hrs / 24;
        return days + " days ago";
    }

    @FXML public void goBackToList() { navigateTo("AdminAuditLogs.fxml"); }

    // ═══════════════════════════════════════════════════════════
    //  THEME HELPERS
    // ═══════════════════════════════════════════════════════════
    private void applyTableTheme(TableView<?> table) {
        if (table == null) return;
        table.setStyle(
            "-fx-background-color:transparent;-fx-border-color:transparent;"
            + "-fx-table-cell-border-color:transparent;"
            + "-fx-control-inner-background:rgba(20,10,35,0.80);"
            + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node hBg = table.lookup(".column-header-background");
            if (hBg != null) hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);");
            table.lookupAll(".column-header").forEach(n -> n.setStyle(
                "-fx-background-color:rgba(8,4,16,0.98);"
                + "-fx-border-color:transparent transparent rgba(255,60,100,0.35) transparent;"
                + "-fx-border-width:0 0 1 0;-fx-size:46px;"));
            table.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;"
                + "-fx-background-color:transparent;-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════
    private Button makeBtn(String text, String variant) {
        Button b = new Button(text);
        String bg, border, color;
        switch (variant) {
            case "info"    -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.40)";  color="#4facfe"; }
            case "success" -> { bg="rgba(67,233,123,0.08)";  border="rgba(67,233,123,0.40)";  color="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.08)";  border="rgba(255,60,100,0.40)";  color="#ff6b7a"; }
            default        -> { bg="rgba(255,255,255,0.05)"; border="rgba(255,255,255,0.15)"; color="white";  }
        }
        b.setStyle(
            "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
            + "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;"
            + "-fx-text-fill:" + color + ";-fx-font-size:12;-fx-padding:5 12;"
            + "-fx-cursor:hand;-fx-font-weight:bold;");
        b.setOnMouseEntered(e -> b.setOpacity(0.8));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private String val(ComboBox<String> combo, String defaultVal) {
        if (combo == null || combo.getValue() == null) return defaultVal;
        return combo.getValue();
    }

    private void setLabel(Label l, String v)           { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v) { if (pb != null) pb.setProgress(v); }

    // ═══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════
    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) { System.err.println("[AdminAuditLogController] Not found: " + fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminAuditLogController] Nav error: " + e.getMessage());
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
        javafx.scene.Node[] nodes = { logsTable, statTotalLabel, detailIdLabel, detailDetailsArea };
        for (javafx.scene.Node n : nodes) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    // ── Sidebar nav shortcuts ──────────────────────────────────────
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
    @FXML public void goToAuditLogs()         { navigateTo("AdminAuditLogs.fxml"); }
    @FXML public void handleLogout()          { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }
}
