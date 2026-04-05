package com.eyetwin.controller.admin;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.ITeamService;
import com.eyetwin.services.TeamServiceImpl;
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
import java.util.List;
import java.util.Optional;

public class AdminTeamController {

    // ── Sidebar / Topbar ──────────────────────────────────────────
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ── LIST VIEW — KPI labels ────────────────────────────────────
    @FXML private Label totalTeamsLabel;
    @FXML private Label activeTeamsLabel;
    @FXML private Label inactiveTeamsLabel;
    @FXML private Label totalMembersLabel;

    @FXML private ProgressBar progressTotal;
    @FXML private ProgressBar progressActive;
    @FXML private ProgressBar progressInactive;
    @FXML private ProgressBar progressMembers;

    // ── LIST VIEW — Filters ───────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label            resultCountLabel;

    // ── LIST VIEW — Table ─────────────────────────────────────────
    @FXML private TableView<Team>           teamsTable;
    @FXML private TableColumn<Team, Void>   colLogo;
    @FXML private TableColumn<Team, String> colName;
    @FXML private TableColumn<Team, String> colOwner;
    @FXML private TableColumn<Team, Void>   colMembers;
    @FXML private TableColumn<Team, String> colStatus;
    @FXML private TableColumn<Team, String> colCreated;
    @FXML private TableColumn<Team, Void>   colActions;

    // ── LIST VIEW — Pagination ────────────────────────────────────
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  pageNumberLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // ── DETAIL VIEW — Header ──────────────────────────────────────
    @FXML private Label       teamAvatarLabel;
    @FXML private Label       teamNameLabel;
    @FXML private Label       teamStatusChip;
    @FXML private VBox        descBox;
    @FXML private Label       descLabel;
    @FXML private Label       capacityLabel;
    @FXML private Label       capacityPctLabel;
    @FXML private ProgressBar capacityBar;
    @FXML private Label       createdLabel;
    @FXML private Label       ownerLabel;
    @FXML private Label       maxMembersLabel;
    @FXML private Label       activeMembersLabel;
    @FXML private Label       pendingLabel;
    @FXML private Button      toggleStatusBtn;
    @FXML private Button      deleteTeamBtn;

    // ── DETAIL VIEW — Members table ───────────────────────────────
    @FXML private Label                              activeMembersBadge;
    @FXML private VBox                               membersEmptyState;
    @FXML private TableView<TeamMembership>          membersTable;
    @FXML private TableColumn<TeamMembership, Void>  colMemberAvatar;
    @FXML private TableColumn<TeamMembership, String>colMemberUsername;
    @FXML private TableColumn<TeamMembership, String>colMemberEmail;
    @FXML private TableColumn<TeamMembership, String>colMemberRole;
    @FXML private TableColumn<TeamMembership, String>colMemberJoined;
    @FXML private TableColumn<TeamMembership, String>colMemberStatus;

    // ── DETAIL VIEW — Pending table ───────────────────────────────
    @FXML private Label                              pendingBadge;
    @FXML private VBox                               pendingEmptyState;
    @FXML private TableView<TeamMembership>          pendingTable;
    @FXML private TableColumn<TeamMembership, String>colPendingUsername;
    @FXML private TableColumn<TeamMembership, String>colPendingEmail;
    @FXML private TableColumn<TeamMembership, String>colPendingDate;
    @FXML private TableColumn<TeamMembership, Void>  colPendingActions;

    // ── State ─────────────────────────────────────────────────────
    private ITeamService             teamService;
    private ObservableList<Team>     allTeams  = FXCollections.observableArrayList();
    private static final int         PAGE_SIZE = 8;
    private int currentPage = 1;
    private int totalPages  = 1;

    // ── Style constants ───────────────────────────────────────────
    private static final String BG_DARK    = "#0d0618";
    private static final String BG_FIELD   = "#160a22";
    private static final String RED_BORDER = "rgba(255,60,100,0.30)";

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        teamService = new TeamServiceImpl();

        if (adminSidebarController != null) adminSidebarController.setActivePage("teams");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Team Management");

        // List view
        if (teamsTable != null) initListView();

        // Detail view
        if (teamAvatarLabel != null) initDetailView();

        Platform.runLater(this::applyTheme);
    }

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════
    private void initListView() {
        setupFilterCombos();
        setupTeamsTable();
        loadAllTeams();
    }

    private void setupFilterCombos() {
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Statuses", "active", "inactive"));
            statusFilterCombo.setValue("All Statuses");
            styleCombo(statusFilterCombo);
        }
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Newest First", "Oldest First", "Name A→Z", "Name Z→A", "Most Members"));
            sortCombo.setValue("Newest First");
            styleCombo(sortCombo);
        }
    }

    private void styleCombo(ComboBox<String> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) setText(item); else setText(null);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:" + BG_FIELD + ";-fx-padding:8 14;");
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) setText(item); else setText(null);
                setStyle("-fx-text-fill:rgba(255,255,255,0.85);"
                       + "-fx-background-color:" + BG_FIELD + ";");
            }
        });
    }

    private void loadAllTeams() {
        new Thread(() -> {
            try {
                List<Team> teams = teamService.getAllActiveTeams();
                // Charger aussi les inactives via findAll si dispo,
                // sinon on travaille avec getAllActiveTeams + logique filtre
                Platform.runLater(() -> {
                    allTeams.setAll(teams);
                    applyFilters();
                    refreshKPICards(teams);
                });
            } catch (Exception e) {
                System.err.println("[AdminTeamController] loadAllTeams: " + e.getMessage());
            }
        }, "LoadTeams").start();
    }

    private void refreshKPICards(List<Team> teams) {
        int total    = teams.size();
        int active   = (int) teams.stream().filter(Team::isActive).count();
        int inactive = total - active;
        int members  = teams.stream()
                .mapToInt(t -> (int) t.getActiveMembersCount())
                .sum();

        animateCount(totalTeamsLabel,    total);
        animateCount(activeTeamsLabel,   active);
        animateCount(inactiveTeamsLabel, inactive);
        animateCount(totalMembersLabel,  members);

        setProgress(progressTotal,    1.0);
        setProgress(progressActive,   total > 0 ? (double) active   / total : 0);
        setProgress(progressInactive, total > 0 ? (double) inactive / total : 0);
        setProgress(progressMembers,  total > 0 ? Math.min(1.0, members / (total * 10.0)) : 0);

        applyProgressStyle(progressTotal,    "progress-purple");
        applyProgressStyle(progressActive,   "progress-green");
        applyProgressStyle(progressInactive, "progress-red");
        applyProgressStyle(progressMembers,  "progress-pink");
    }

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
        label.setStyle("-fx-font-size:44px;-fx-font-weight:bold;-fx-text-fill:white;");
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

        List<Team> filtered = allTeams.stream().filter(t -> {
            // Search
            if (!search.isBlank()) {
                boolean match = (t.getName()        != null && t.getName().toLowerCase().contains(search))
                             || (t.getDescription() != null && t.getDescription().toLowerCase().contains(search))
                             || (t.getOwner()       != null && t.getOwner().getUsername() != null
                                 && t.getOwner().getUsername().toLowerCase().contains(search));
                if (!match) return false;
            }
            // Status
            if (status != null && !status.equals("All Statuses")) {
                if ("active".equals(status)   && !t.isActive()) return false;
                if ("inactive".equals(status) &&  t.isActive()) return false;
            }
            return true;
        }).sorted((a, b) -> switch (sort != null ? sort : "Newest First") {
            case "Oldest First"  -> a.getCreatedAt().compareTo(b.getCreatedAt());
            case "Name A→Z"      -> a.getName().compareToIgnoreCase(b.getName());
            case "Name Z→A"      -> b.getName().compareToIgnoreCase(a.getName());
            case "Most Members"  -> Long.compare(b.getActiveMembersCount(), a.getActiveMembersCount());
            default              -> b.getCreatedAt().compareTo(a.getCreatedAt()); // Newest First
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        if (teamsTable != null) {
            teamsTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
            applyTeamsTableTheme();
        }

        setLabel(resultCountLabel,    "Found " + filtered.size() + " team" + (filtered.size() != 1 ? "s" : ""));
        setLabel(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabel(paginationInfoLabel, filtered.isEmpty() ? "" :
                "Showing " + (from + 1) + " – " + to + " of " + filtered.size() + " entries");
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }

    // ─────────────────────────────────────────────────────────────
    //  TEAMS TABLE SETUP
    // ─────────────────────────────────────────────────────────────
    private void setupTeamsTable() {
        if (teamsTable == null) return;

        // Logo column
        if (colLogo != null) {
            colLogo.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Team> row = getTableRow();
                    if (row == null || row.getItem() == null) return;
                    Team t = row.getItem();
                    String initials = t.getName() != null && t.getName().length() >= 2
                            ? t.getName().substring(0, 2).toUpperCase() : "??";
                    Label avatar = new Label(initials);
                    avatar.setStyle(
                        "-fx-background-color: linear-gradient(to bottom right,#667eea,#764ba2);"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;"
                        + "-fx-min-width:40;-fx-min-height:40;-fx-max-width:40;-fx-max-height:40;"
                        + "-fx-background-radius:10;-fx-alignment:center;");
                    setGraphic(avatar);
                    setAlignment(Pos.CENTER);
                }
            });
        }

        // Name column
        if (colName != null) {
            colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
            colName.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    Team t = getTableRow() != null ? getTableRow().getItem() : null;
                    Label name = new Label(item);
                    name.setStyle("-fx-text-fill:#43e97b;-fx-font-weight:bold;-fx-font-size:13;");
                    VBox box = new VBox(2, name);
                    if (t != null && t.getDescription() != null && !t.getDescription().isBlank()) {
                        String desc = t.getDescription().length() > 35
                                ? t.getDescription().substring(0, 35) + "…" : t.getDescription();
                        Label sub = new Label(desc);
                        sub.setStyle("-fx-text-fill:rgba(255,255,255,0.40);-fx-font-size:11;");
                        box.getChildren().add(sub);
                    }
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        // Owner column
        if (colOwner != null) {
            colOwner.setCellValueFactory(d -> {
                Team t = d.getValue();
                String owner = t.getOwner() != null ? "@" + t.getOwner().getUsername() : "—";
                return new SimpleStringProperty(owner);
            });
            colOwner.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle("-fx-text-fill:#4facfe;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Members column — progress bar
        if (colMembers != null) {
            colMembers.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Team> row = getTableRow();
                    if (row == null || row.getItem() == null) return;
                    Team t = row.getItem();
                    long active = t.getActiveMembersCount();
                    int  max    = t.getMaxMembers();
                    double pct  = max > 0 ? (double) active / max : 0;

                    Label countLbl = new Label(active + "/" + max);
                    countLbl.setStyle("-fx-text-fill:white;-fx-font-size:11;-fx-font-weight:bold;");

                    ProgressBar pb = new ProgressBar(pct);
                    pb.setPrefWidth(130);
                    pb.setPrefHeight(6);
                    pb.getStyleClass().add("progress-purple");

                    VBox box = new VBox(4, countLbl, pb);
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        // Status column
        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    boolean active = "ACTIVE".equals(item);
                    Label badge = new Label(active ? "✓ Active" : "✕ Inactive");
                    badge.setStyle(
                        "-fx-background-color:" + (active ? "rgba(67,233,123,0.15)" : "rgba(255,255,255,0.07)") + ";"
                        + "-fx-border-color:" + (active ? "rgba(67,233,123,0.45)" : "rgba(255,255,255,0.20)") + ";"
                        + "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;"
                        + "-fx-text-fill:" + (active ? "#43e97b" : "rgba(255,255,255,0.50)") + ";"
                        + "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:4 10;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // Created column
        if (colCreated != null) {
            colCreated.setCellValueFactory(d -> {
                String date = d.getValue().getCreatedAt() != null
                        ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—";
                return new SimpleStringProperty(date);
            });
            colCreated.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Actions column
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn   = makeBtn("👁",  "info");
                private final Button toggleBtn = makeBtn("⏸",  "warning");
                private final Button delBtn    = makeBtn("🗑",  "danger");
                private final HBox   box       = new HBox(5, viewBtn, toggleBtn, delBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    viewBtn.setOnAction(e -> {
                        Team t = row(); if (t != null) openDetail(t);
                    });
                    toggleBtn.setOnAction(e -> {
                        Team t = row(); if (t != null) handleToggleFromList(t);
                    });
                    delBtn.setOnAction(e -> {
                        Team t = row(); if (t != null) handleDeleteFromList(t);
                    });
                    tableRowProperty().addListener((obs, o, n) -> {
                        if (n != null) n.itemProperty().addListener((o2, ov, nv) -> refresh(nv));
                    });
                }
                private Team row() {
                    TableRow<Team> r = getTableRow();
                    return r != null ? r.getItem() : null;
                }
                private void refresh(Team t) {
                    if (t == null) { setGraphic(null); return; }
                    toggleBtn.setText(t.isActive() ? "⏸" : "▶");
                    toggleBtn.setTooltip(new Tooltip(t.isActive() ? "Deactivate" : "Activate"));
                    setGraphic(box);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<Team> r = getTableRow();
                    if (r != null && r.getItem() != null) refresh(r.getItem());
                }
            });
        }

        teamsTable.setRowFactory(tv -> {
            TableRow<Team> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem()); });
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08);-fx-background-radius:10;"
                    + "-fx-border-color:rgba(255,60,100,0.30);-fx-border-width:1;"
                    + "-fx-border-radius:10;-fx-cursor:hand;"); });
            row.setOnMouseExited(e -> { if (!row.isEmpty()) applyRowStyle(row); });
            return row;
        });

        teamsTable.setPlaceholder(new Label("No teams found"));
    }

    private void applyRowStyle(TableRow<Team> row) {
        String bg = (row.getIndex() % 2 == 0)
                ? "-fx-background-color:rgba(20,10,35,0.85);"
                : "-fx-background-color:rgba(30,15,45,0.70);";
        row.setStyle(bg + "-fx-background-radius:10;"
                + "-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1;-fx-border-radius:10;");
    }

    private void applyTeamsTableTheme() {
        if (teamsTable == null) return;
        teamsTable.setStyle(
            "-fx-background-color:transparent;-fx-border-color:transparent;"
            + "-fx-table-cell-border-color:transparent;"
            + "-fx-control-inner-background:rgba(20,10,35,0.80);"
            + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node hBg = teamsTable.lookup(".column-header-background");
            if (hBg != null) hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-padding:0;");
            javafx.scene.Node filler = teamsTable.lookup(".column-header-background .filler");
            if (filler != null) filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);");
            teamsTable.lookupAll(".column-header").forEach(n -> n.setStyle(
                "-fx-background-color:rgba(8,4,16,0.98);"
                + "-fx-border-color:transparent transparent rgba(255,60,100,0.35) transparent;"
                + "-fx-border-width:0 0 1 0;-fx-size:48px;"));
            teamsTable.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;"
                + "-fx-background-color:transparent;-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
    }

    // ─────────────────────────────────────────────────────────────
    //  LIST ACTIONS
    // ─────────────────────────────────────────────────────────────
    private void openDetail(Team team) {
        SessionManager.setSelectedTeam(team);
        navigateTo("AdminTeamDetail.fxml");
    }

    private void handleToggleFromList(Team team) {
        String msg = team.isActive()
                ? "Deactivate team \"" + team.getName() + "\"?"
                : "Activate team \"" + team.getName() + "\"?";
        if (!confirm("Confirm", msg)) return;
        new Thread(() -> {
            try {
                teamService.toggleActive(team.getId(), !team.isActive(),
                        SessionManager.getCurrentUser().getId());
                Platform.runLater(this::loadAllTeams);
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    private void handleDeleteFromList(Team team) {
        if (!confirm("Delete Team",
                "Permanently delete \"" + team.getName() + "\"?\nThis cannot be undone!")) return;
        new Thread(() -> {
            try {
                teamService.deleteTeam(team.getId(), SessionManager.getCurrentUser().getId());
                Platform.runLater(this::loadAllTeams);
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════════
    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("Team Details");
        Team team = SessionManager.getSelectedTeam();
        if (team == null) { navigateTo("AdminTeams.fxml"); return; }
        populateDetail(team);
    }

    private void populateDetail(Team team) {
        // ── Avatar ────────────────────────────────────────────
        String initials = team.getName() != null && team.getName().length() >= 2
                ? team.getName().substring(0, 2).toUpperCase() : "??";
        setLabel(teamAvatarLabel, initials);
        setLabel(teamNameLabel,   team.getName());

        // ── Status chip ───────────────────────────────────────
        if (teamStatusChip != null) {
            if (team.isActive()) {
                teamStatusChip.setText("✓ ACTIVE");
                teamStatusChip.setStyle(
                    "-fx-background-color:rgba(67,233,123,0.15);"
                    + "-fx-border-color:rgba(67,233,123,0.3);"
                    + "-fx-border-radius:8;-fx-background-radius:8;"
                    + "-fx-text-fill:#43e97b;-fx-font-size:11;"
                    + "-fx-font-weight:bold;-fx-padding:5 12;");
            } else {
                teamStatusChip.setText("✕ INACTIVE");
                teamStatusChip.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.06);"
                    + "-fx-border-color:rgba(255,255,255,0.20);"
                    + "-fx-border-radius:8;-fx-background-radius:8;"
                    + "-fx-text-fill:rgba(255,255,255,0.50);-fx-font-size:11;"
                    + "-fx-font-weight:bold;-fx-padding:5 12;");
            }
        }

        // ── Description ───────────────────────────────────────
        boolean hasDesc = team.getDescription() != null && !team.getDescription().isBlank();
        showNode(descBox, hasDesc);
        if (hasDesc) setLabel(descLabel, team.getDescription());

        // ── Capacity ──────────────────────────────────────────
        long active = team.getActiveMembersCount();
        int  max    = team.getMaxMembers();
        double pct  = max > 0 ? (double) active / max : 0;
        setLabel(capacityLabel,    active + " / " + max);
        setLabel(capacityPctLabel, team.getFillPercent() + "%");
        if (capacityBar != null) {
            capacityBar.setProgress(pct);
            capacityBar.getStyleClass().removeIf(s -> s.startsWith("progress-"));
            capacityBar.getStyleClass().add(pct >= 0.9 ? "progress-red" : "progress-purple");
        }

        // ── Stats ─────────────────────────────────────────────
        setLabel(createdLabel,       team.getCreatedAt() != null
                ? team.getCreatedAt().toString().substring(0, 10) : "—");
        setLabel(ownerLabel,         team.getOwner() != null
                ? "@" + team.getOwner().getUsername() : "—");
        setLabel(maxMembersLabel,    String.valueOf(max));
        setLabel(activeMembersLabel, String.valueOf(active));

        // ── Toggle button label ───────────────────────────────
        if (toggleStatusBtn != null) {
            if (team.isActive()) {
                toggleStatusBtn.setText("⏸  Deactivate Team");
                toggleStatusBtn.setStyle(
                    "-fx-background-color:rgba(255,171,0,0.08);"
                    + "-fx-border-color:rgba(255,171,0,0.35);-fx-border-radius:8;-fx-background-radius:8;"
                    + "-fx-text-fill:#ffb700;-fx-font-weight:bold;-fx-padding:10;-fx-cursor:hand;");
            } else {
                toggleStatusBtn.setText("▶  Activate Team");
                toggleStatusBtn.setStyle(
                    "-fx-background-color:rgba(67,233,123,0.08);"
                    + "-fx-border-color:rgba(67,233,123,0.35);-fx-border-radius:8;-fx-background-radius:8;"
                    + "-fx-text-fill:#43e97b;-fx-font-weight:bold;-fx-padding:10;-fx-cursor:hand;");
            }
        }

        // ── Load members ──────────────────────────────────────
        loadDetailMembers(team);
    }

    private void loadDetailMembers(Team team) {
        new Thread(() -> {
            try {
                List<TeamMembership> activeMembers  = teamService.getActiveMembers(team.getId());
                List<TeamMembership> pendingMembers = teamService.getPendingRequests(team.getId());
                Platform.runLater(() -> {
                    // Active members
                    int ac = activeMembers.size();
                    setLabel(activeMembersBadge, String.valueOf(ac));
                    setLabel(activeMembersLabel, String.valueOf(ac));
                    showNode(membersEmptyState, ac == 0);
                    if (membersTable != null) {
                        if (ac > 0) {
                            setupMembersTable();
                            membersTable.setItems(FXCollections.observableArrayList(activeMembers));
                            applySubTableTheme(membersTable);
                        }
                        showNode(membersTable, ac > 0);
                    }
                    // Pending
                    int pc = pendingMembers.size();
                    setLabel(pendingBadge,  String.valueOf(pc));
                    setLabel(pendingLabel,  String.valueOf(pc));
                    showNode(pendingEmptyState, pc == 0);
                    if (pendingTable != null) {
                        if (pc > 0) {
                            setupPendingTable(team);
                            pendingTable.setItems(FXCollections.observableArrayList(pendingMembers));
                            applySubTableTheme(pendingTable);
                        }
                        showNode(pendingTable, pc > 0);
                    }
                });
            } catch (Exception e) {
                System.err.println("[AdminTeamController] loadDetailMembers: " + e.getMessage());
            }
        }, "LoadMembers").start();
    }

    // ─────────────────────────────────────────────────────────────
    //  MEMBERS TABLE
    // ─────────────────────────────────────────────────────────────
    private void setupMembersTable() {
        if (membersTable == null) return;

        // Avatar
        if (colMemberAvatar != null) {
            colMemberAvatar.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<TeamMembership> row = getTableRow();
                    if (row == null || row.getItem() == null) return;
                    User u = row.getItem().getUser();
                    String initials = (u != null && u.getUsername() != null && u.getUsername().length() >= 2)
                            ? u.getUsername().substring(0, 2).toUpperCase() : "??";
                    Label avatar = new Label(initials);
                    avatar.setStyle(
                        "-fx-background-color:linear-gradient(to bottom right,#667eea,#764ba2);"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;"
                        + "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;"
                        + "-fx-background-radius:18;-fx-alignment:center;");
                    setGraphic(avatar);
                    setAlignment(Pos.CENTER);
                }
            });
        }

        // Username
        if (colMemberUsername != null) {
            colMemberUsername.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? "@" + u.getUsername() : "—");
            });
            colMemberUsername.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Email
        if (colMemberEmail != null) {
            colMemberEmail.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? u.getEmail() : "—");
            });
            colMemberEmail.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle("-fx-text-fill:#4facfe;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Role
        if (colMemberRole != null) {
            colMemberRole.setCellValueFactory(d -> {
                MemberRole r = d.getValue().getRole();
                return new SimpleStringProperty(r != null ? r.name() : "MEMBER");
            });
            colMemberRole.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    boolean isOwner = "OWNER".equals(item);
                    String bg     = isOwner ? "rgba(251,191,36,0.15)" : "rgba(79,172,254,0.15)";
                    String border = isOwner ? "rgba(251,191,36,0.45)" : "rgba(79,172,254,0.45)";
                    String color  = isOwner ? "#fbbf24"               : "#4facfe";
                    String prefix = isOwner ? "👑 "                   : "";
                    Label badge = new Label(prefix + item);
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

        // Joined
        if (colMemberJoined != null) {
            colMemberJoined.setCellValueFactory(d -> {
                java.time.LocalDateTime dt = d.getValue().getJoinedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().substring(0, 10) : "—");
            });
            colMemberJoined.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        // Status
        if (colMemberStatus != null) {
            colMemberStatus.setCellValueFactory(d -> {
                MembershipStatus s = d.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "—");
            });
            colMemberStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    String bg, border, color;
                    switch (item) {
                        case "ACTIVE"  -> { bg="rgba(67,233,123,0.15)"; border="rgba(67,233,123,0.45)"; color="#43e97b"; }
                        case "INVITED" -> { bg="rgba(79,172,254,0.15)"; border="rgba(79,172,254,0.45)"; color="#4facfe"; }
                        default        -> { bg="rgba(255,255,255,0.07)"; border="rgba(255,255,255,0.20)"; color="rgba(255,255,255,0.55)"; }
                    }
                    Label badge = new Label(item);
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
    }

    // ─────────────────────────────────────────────────────────────
    //  PENDING TABLE
    // ─────────────────────────────────────────────────────────────
    private void setupPendingTable(Team team) {
        if (pendingTable == null) return;

        if (colPendingUsername != null) {
            colPendingUsername.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? "@" + u.getUsername() : "—");
            });
            colPendingUsername.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        if (colPendingEmail != null) {
            colPendingEmail.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? u.getEmail() : "—");
            });
            colPendingEmail.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:#4facfe;-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        if (colPendingDate != null) {
            colPendingDate.setCellValueFactory(d -> {
                java.time.LocalDateTime dt = d.getValue().getInvitedAt();
                return new SimpleStringProperty(dt != null ? dt.toString().substring(0, 10) : "—");
            });
            colPendingDate.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill:rgba(255,255,255,0.55);-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }

        if (colPendingActions != null) {
            colPendingActions.setCellFactory(col -> new TableCell<>() {
                private final Button acceptBtn = makeBtn("✓", "success");
                private final Button rejectBtn = makeBtn("✕", "danger");
                private final HBox   box       = new HBox(6, acceptBtn, rejectBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    acceptBtn.setOnAction(e -> {
                        TableRow<TeamMembership> r = getTableRow();
                        if (r != null && r.getItem() != null) handleAcceptRequest(r.getItem(), team);
                    });
                    rejectBtn.setOnAction(e -> {
                        TableRow<TeamMembership> r = getTableRow();
                        if (r != null && r.getItem() != null) handleRejectRequest(r.getItem(), team);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    private void handleAcceptRequest(TeamMembership m, Team team) {
        if (!confirm("Accept", "Accept request from \"" +
                (m.getUser() != null ? m.getUser().getUsername() : m.getUserId()) + "\"?")) return;
        new Thread(() -> {
            try {
                teamService.acceptRequest(m.getId(), SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> loadDetailMembers(team));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    private void handleRejectRequest(TeamMembership m, Team team) {
        if (!confirm("Reject", "Reject request from \"" +
                (m.getUser() != null ? m.getUser().getUsername() : m.getUserId()) + "\"?")) return;
        new Thread(() -> {
            try {
                teamService.rejectRequest(m.getId(), SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> loadDetailMembers(team));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    //  DETAIL ACTIONS
    // ─────────────────────────────────────────────────────────────
    @FXML public void handleToggleStatus() {
        Team team = SessionManager.getSelectedTeam();
        if (team == null) return;
        String msg = team.isActive()
                ? "Deactivate team \"" + team.getName() + "\"?"
                : "Activate team \"" + team.getName() + "\"?";
        if (!confirm("Confirm", msg)) return;
        new Thread(() -> {
            try {
                teamService.toggleActive(team.getId(), !team.isActive(),
                        SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> {
                    // Refresh
                    try {
                        Team refreshed = teamService.getTeamWithDetails(team.getId());
                        SessionManager.setSelectedTeam(refreshed);
                        populateDetail(refreshed);
                    } catch (Exception ex) { navigateTo("AdminTeams.fxml"); }
                });
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleDeleteTeam() {
        Team team = SessionManager.getSelectedTeam();
        if (team == null) return;
        if (!confirm("Delete Team",
                "Permanently delete \"" + team.getName() + "\"?\nThis cannot be undone!")) return;
        new Thread(() -> {
            try {
                teamService.deleteTeam(team.getId(), SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> navigateTo("AdminTeams.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage()));
            }
        }).start();
    }

    @FXML public void goBackToList() { navigateTo("AdminTeams.fxml"); }

    // ─────────────────────────────────────────────────────────────
    //  THEME
    // ─────────────────────────────────────────────────────────────
    private void applyTheme() {
        applyProgressStyle(progressTotal,    "progress-purple");
        applyProgressStyle(progressActive,   "progress-green");
        applyProgressStyle(progressInactive, "progress-red");
        applyProgressStyle(progressMembers,  "progress-pink");
        applyTeamsTableTheme();
    }

    private void applySubTableTheme(TableView<?> table) {
        if (table == null) return;
        table.setStyle(
            "-fx-background-color:transparent;-fx-border-color:transparent;"
            + "-fx-table-cell-border-color:transparent;"
            + "-fx-control-inner-background:rgba(20,10,35,0.80);"
            + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node hBg = table.lookup(".column-header-background");
            if (hBg != null) hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-padding:0;");
            javafx.scene.Node filler = table.lookup(".column-header-background .filler");
            if (filler != null) filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);");
            table.lookupAll(".column-header").forEach(n -> n.setStyle(
                "-fx-background-color:rgba(8,4,16,0.98);"
                + "-fx-border-color:transparent transparent rgba(255,60,100,0.35) transparent;"
                + "-fx-border-width:0 0 1 0;-fx-size:44px;"));
            table.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;"
                + "-fx-background-color:transparent;-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
    }

    private void applyProgressStyle(ProgressBar pb, String styleClass) {
        if (pb == null) return;
        pb.getStyleClass().removeIf(s -> s.startsWith("progress-") || s.startsWith("strength-"));
        pb.getStyleClass().add(styleClass);
    }

    // ─────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────
    private Button makeBtn(String text, String variant) {
        Button b = new Button(text);
        String bg, border, color;
        switch (variant) {
            case "success" -> { bg="rgba(67,233,123,0.08)";  border="rgba(67,233,123,0.40)";  color="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.08)";  border="rgba(255,60,100,0.40)";  color="#ff6b7a"; }
            case "warning" -> { bg="rgba(255,171,0,0.08)";   border="rgba(255,171,0,0.40)";   color="#ffb700"; }
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

    private void setLabel(Label l, String v)             { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v)   { if (pb != null) pb.setProgress(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
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

    // ─────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────
    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) { System.err.println("[AdminTeamController] FXML not found: " + fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminTeamController] Nav error: " + e.getMessage());
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
                searchField, teamsTable, teamAvatarLabel, teamNameLabel,
                toggleStatusBtn, totalTeamsLabel};
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    // ── Nav shortcuts (from sidebar) ──────────────────────────────
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
