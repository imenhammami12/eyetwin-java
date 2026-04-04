package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.entities.TeamMembership;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class AdminUserController {

    // ═══════════════════════════════════════════════════════════
    //  SHARED
    // ═══════════════════════════════════════════════════════════
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label coachesLabel;
    @FXML private Label adminsLabel;
    @FXML private Label inactiveUsersLabel;
    @FXML private Label activeRateLabel;

    @FXML private ProgressBar progressTotal;
    @FXML private ProgressBar progressActive;
    @FXML private ProgressBar progressCoaches;
    @FXML private ProgressBar progressAdmins;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private Label resultCountLabel;

    @FXML private TableView<User>           usersTable;
    @FXML private TableColumn<User, Void>   colAvatar;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colJoined;
    @FXML private TableColumn<User, Void>   colActions;

    @FXML private Label  paginationInfoLabel;
    @FXML private Label  pageNumberLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    // ═══════════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════════
    @FXML private Label avatarInitialLabel;
    @FXML private Label fullNameHeaderLabel;
    @FXML private Label usernameHeaderLabel;
    @FXML private Label roleChipLabel;
    @FXML private Label statusChipLabel;
    @FXML private Label youBadgeLabel;

    @FXML private VBox  bioBox;
    @FXML private Label bioLabel;
    @FXML private VBox  limitedPermBanner;
    @FXML private VBox  selfViewBanner;

    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button           updateRoleBtn;
    @FXML private Label            rolePermNote;

    @FXML private VBox  statusAlertBox;
    @FXML private Label statusAlertLabel;

    @FXML private Button suspendBtn;
    @FXML private Button banBtn;
    @FXML private Button reactivateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label  actionPermNote;

    @FXML private Label emailInfoLabel;
    @FXML private Label usernameInfoLabel;
    @FXML private Label fullNameInfoLabel;
    @FXML private Label registeredLabel;
    @FXML private Label registeredAgoLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label statusInfoLabel;
    @FXML private Label teamsBadge;
    @FXML private VBox  teamsEmptyState;
    @FXML private TableView<?> teamsTable;

    // ═══════════════════════════════════════════════════════════
    //  CREATE VIEW
    // ═══════════════════════════════════════════════════════════
    @FXML private TextField     createFullNameField;
    @FXML private TextField     createUsernameField;
    @FXML private TextField     createEmailField;
    @FXML private PasswordField createPasswordField;
    @FXML private TextField     createPasswordVisible;
    @FXML private Button        createTogglePasswordBtn;
    @FXML private ComboBox<String> createRoleCombo;
    @FXML private Button        createSubmitBtn;
    @FXML private Button        createCancelBtn;
    @FXML private ProgressBar   createPasswordStrength;
    @FXML private Label         createStrengthLabel;
    @FXML private VBox          createRoleDescBox;
    @FXML private Label         createRoleTitleLabel;
    @FXML private Label         createRoleTextLabel;
    @FXML private Label errFullName, errUsername, errEmail, errPassword, errRole;

    // ═══════════════════════════════════════════════════════════
    //  STATE
    // ═══════════════════════════════════════════════════════════
    private IUserService         userService;
    private ObservableList<User> allUsers        = FXCollections.observableArrayList();
    private boolean              passwordVisible = false;

    private static final int PAGE_SIZE = 20;
    private int currentPage = 1;
    private int totalPages  = 1;

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }

        userService = new UserServiceImpl();

        if (adminSidebarController != null) adminSidebarController.setActivePage("users");
        if (adminTopbarController  != null) adminTopbarController.setTitle("User Management");

        if (usersTable         != null) initListView();
        if (avatarInitialLabel != null) initDetailView();
        if (createSubmitBtn    != null) initCreateView();

        Platform.runLater(this::applyGamingTheme);
    }

    // ─── Apply gaming theme after scene is ready ──────────────
    private void applyGamingTheme() {
        applyProgressBarStyles();
        applyTableTheme();
    }

    private void applyProgressBarStyles() {
        applyProgressStyle(progressTotal,   "progress-purple");
        applyProgressStyle(progressActive,  "progress-green");
        applyProgressStyle(progressCoaches, "progress-pink");
        applyProgressStyle(progressAdmins,  "progress-blue");
    }

    private void applyProgressStyle(ProgressBar pb, String styleClass) {
        if (pb == null) return;
        pb.getStyleClass().removeIf(s -> s.startsWith("progress-") || s.startsWith("strength-"));
        pb.getStyleClass().add(styleClass);
    }

    private void applyTableTheme() {
        if (usersTable == null) return;

        usersTable.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: transparent;" +
                        "-fx-control-inner-background: rgba(20,10,35,0.80);" +
                        "-fx-control-inner-background-alt: rgba(30,15,45,0.60);"
        );

        // Attendre que le scenegraph soit complètement rendu
        Platform.runLater(() -> Platform.runLater(() -> {

            // ── Fond global du header ──────────────────────────
            javafx.scene.Node headerBg = usersTable.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle(
                        "-fx-background-color: rgba(8,4,16,0.98);" +
                                "-fx-padding: 0;"
                );
            }

            // ── Filler (coin droit du header) ──────────────────
            javafx.scene.Node filler = usersTable.lookup(".column-header-background .filler");
            if (filler != null) {
                filler.setStyle("-fx-background-color: rgba(8,4,16,0.98);");
            }

            // ── Chaque cellule header ──────────────────────────
            usersTable.lookupAll(".column-header").forEach(node -> {
                node.setStyle(
                        "-fx-background-color: rgba(8,4,16,0.98);" +
                                "-fx-border-color: transparent transparent rgba(255,60,100,0.35) transparent;" +
                                "-fx-border-width: 0 0 1 0;" +
                                "-fx-size: 48px;"
                );
            });

            // ── Texte de chaque header : blanc, uppercase, lisible ──
            usersTable.lookupAll(".column-header .label").forEach(node -> {
                node.setStyle(
                        "-fx-text-fill: rgba(255,255,255,0.90);" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 11px;" +
                                "-fx-background-color: transparent;" +
                                "-fx-alignment: CENTER_LEFT;" +
                                "-fx-padding: 0 16;"
                );
            });

        }));

        applyDarkRowFactory();
    }

    private void applyDarkRowFactory() {
        if (usersTable == null) return;

        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setStyle(
                                "-fx-background-color: transparent;" +
                                        "-fx-border-color: transparent;" +
                                        "-fx-border-width: 0;"
                        );
                    } else {
                        applyRowStyle(this, user, false);
                    }
                }
            };

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.08);" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-color: rgba(255,60,100,0.30);" +
                                    "-fx-border-width: 1;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-cursor: hand;"
                    );
                }
            });

            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    applyRowStyle(row, row.getItem(), row.isSelected());
                }
            });

            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!row.isEmpty() && row.getItem() != null) {
                    applyRowStyle(row, row.getItem(), isSelected);
                }
            });

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetail(row.getItem());
                }
            });

            return row;
        });
    }

    private void applyRowStyle(TableRow<User> row, User user, boolean selected) {
        if (selected) {
            row.setStyle(
                    "-fx-background-color: linear-gradient(to right, rgba(255,60,100,0.25), rgba(120,40,200,0.25));" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: rgba(255,60,100,0.55);" +
                            "-fx-border-width: 1 1 1 3;" +
                            "-fx-border-radius: 10;"
            );
        } else {
            int index = row.getIndex();
            String bg = (index % 2 == 0)
                    ? "-fx-background-color: rgba(20,10,35,0.85);"
                    : "-fx-background-color: rgba(30,15,45,0.70);";
            row.setStyle(
                    bg +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: rgba(255,255,255,0.06);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 10;"
            );
        }
    }
    // ═══════════════════════════════════════════════════════════
    //  ① LIST VIEW
    // ═══════════════════════════════════════════════════════════
    private void initListView() {
        setupFilterCombos();
        setupTable();
        loadAllUsers();
    }

    private void setupFilterCombos() {
        if (roleFilterCombo != null) {
            roleFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Roles", "ROLE_USER", "ROLE_COACH", "ROLE_ADMIN"));
            roleFilterCombo.setValue("All Roles");
        }
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Statuses", "active", "suspended", "banned"));
            statusFilterCombo.setValue("All Statuses");
        }
    }

    private void setupTable() {
        if (usersTable == null) return;

        // Avatar column — circular initials with gradient background
        if (colAvatar != null) {
            colAvatar.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    User u = (User) getTableRow().getItem();
                    String initials = u.getUsername() != null && !u.getUsername().isEmpty()
                            ? u.getUsername().substring(0, Math.min(2, u.getUsername().length())).toUpperCase()
                            : "?";
                    String gradient = getAvatarGradient(u);
                    Label avatar = new Label(initials);
                    avatar.setStyle(
                            "-fx-background-color: " + gradient + ";" +
                                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;" +
                                    "-fx-min-width: 40; -fx-min-height: 40;" +
                                    "-fx-max-width: 40; -fx-max-height: 40;" +
                                    "-fx-background-radius: 20; -fx-alignment: center;" +
                                    "-fx-border-color: rgba(255,255,255,0.15);" +
                                    "-fx-border-width: 2; -fx-border-radius: 20;");
                    setGraphic(avatar);
                    setAlignment(Pos.CENTER);
                }
            });
        }

        if (colUsername != null) colUsername.setCellValueFactory(d ->
                new SimpleStringProperty("@" + d.getValue().getUsername()));

        if (colFullName != null) colFullName.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getFullName(), "N/A")));

        if (colEmail != null) colEmail.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEmail()));

        // Role column — colored badge labels
        if (colRole != null) {
            colRole.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    User u = (User) getTableRow().getItem();
                    Label badge = makeBadgeLabel(getRoleLabel(u), getRoleBadgeStyle(u));
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            });
        }

        // Status column — colored badge labels
        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    User u = (User) getTableRow().getItem();
                    String status = u.getAccountStatus() != null ? u.getAccountStatus() : "active";
                    Label badge = makeBadgeLabel(status.toUpperCase(), getStatusBadgeStyle(status));
                    setGraphic(badge);
                    setAlignment(Pos.CENTER_LEFT);
                }
            });
        }

        if (colJoined != null) colJoined.setCellValueFactory(d -> {
            String date = d.getValue().getCreatedAt() != null
                    ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—";
            return new SimpleStringProperty(date);
        });

        // Actions column
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = makeActionBtn("👁", "info");
                private final Button actBtn  = makeActionBtn("⚠", "warning");
                private final Button delBtn  = makeActionBtn("🗑", "danger");
                private final HBox   box     = new HBox(5, viewBtn, actBtn, delBtn);

                {
                    box.setAlignment(Pos.CENTER);
                    viewBtn.setOnAction(e -> {
                        if (!isEmpty()) openDetail(getTableView().getItems().get(getIndex()));
                    });
                    actBtn.setOnAction(e -> {
                        if (!isEmpty()) handleSuspendToggle(getTableView().getItems().get(getIndex()));
                    });
                    delBtn.setOnAction(e -> {
                        if (!isEmpty()) handleDelete(getTableView().getItems().get(getIndex()));
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    User u = getTableView().getItems().get(getIndex());
                    boolean canModify = canModify(u);
                    actBtn.setDisable(!canModify);
                    delBtn.setDisable(!canModify);
                    boolean isActive = isActiveStatus(u.getAccountStatus());
                    actBtn.setText(isActive ? "⚠" : "✓");
                    actBtn.setTooltip(new Tooltip(isActive ? "Suspend" : "Reactivate"));
                    setGraphic(box);
                }
            });
        }

        usersTable.setPlaceholder(new Label("No users found"));
        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem());
            });
            return row;
        });
    }

    /** Returns the avatar gradient string based on user role (matches Symfony color coding) */
    private String getAvatarGradient(User u) {
        if (hasRoleStr(u, "ROLE_ADMIN"))  return "linear-gradient(to bottom right, #ff3c64, #ff1744)";
        if (hasRoleStr(u, "ROLE_COACH"))  return "linear-gradient(to bottom right, #f093fb, #f5576c)";
        return "linear-gradient(to bottom right, #667eea, #764ba2)";
    }

    /** Returns badge styling for role labels */
    private String getRoleBadgeStyle(User u) {
        if (hasRoleStr(u, "ROLE_SUPER_ADMIN")) return "dark";
        if (hasRoleStr(u, "ROLE_ADMIN"))       return "danger";
        if (hasRoleStr(u, "ROLE_COACH"))       return "warning";
        return "info";
    }

    /** Returns badge styling for status */
    private String getStatusBadgeStyle(String status) {
        if (status == null) return "success";
        return switch (status.toLowerCase()) {
            case "active"    -> "success";
            case "suspended" -> "warning";
            case "banned"    -> "danger";
            default          -> "info";
        };
    }

    /** Creates a styled badge Label matching Symfony badge design */
    private Label makeBadgeLabel(String text, String variant) {
        Label badge = new Label(text);
        String bg, border, color;
        switch (variant) {
            case "success" -> { bg = "rgba(67,233,123,0.15)";  border = "rgba(67,233,123,0.45)";  color = "#43e97b"; }
            case "danger"  -> { bg = "rgba(255,60,100,0.15)";  border = "rgba(255,60,100,0.45)";  color = "#ff3c64"; }
            case "warning" -> { bg = "rgba(255,193,7,0.15)";   border = "rgba(255,193,7,0.45)";   color = "#ffd54f"; }
            case "info"    -> { bg = "rgba(79,172,254,0.15)";  border = "rgba(79,172,254,0.45)";  color = "#4facfe"; }
            case "dark"    -> { bg = "rgba(30,30,30,0.60)";    border = "rgba(255,255,255,0.25)"; color = "rgba(255,255,255,0.85)"; }
            default        -> { bg = "rgba(255,255,255,0.05)"; border = "rgba(255,255,255,0.15)"; color = "white"; }
        }
        badge.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11;" +
                        "-fx-padding: 5 12;"
        );
        return badge;
    }

    /** Creates a small action button for the table */
    private Button makeActionBtn(String text, String variant) {
        Button b = new Button(text);
        String bg, border, color;
        switch (variant) {
            case "info"    -> { bg = "rgba(79,172,254,0.15)";  border = "rgba(79,172,254,0.40)";  color = "#4facfe"; }
            case "warning" -> { bg = "rgba(255,171,0,0.08)";   border = "rgba(255,171,0,0.40)";   color = "#ffb700"; }
            case "danger"  -> { bg = "rgba(255,60,100,0.08)";  border = "rgba(255,60,100,0.40)";  color = "#ff6b7a"; }
            default        -> { bg = "rgba(255,255,255,0.05)"; border = "rgba(255,255,255,0.15)"; color = "white";   }
        }
        b.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 5 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-weight: bold;"
        );
        b.setOnMouseEntered(e -> b.setOpacity(0.8));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<User> users = userService.getAllUsers();
                Platform.runLater(() -> {
                    allUsers.setAll(users);
                    applyFilters();
                    refreshKPICards(users);
                });
            } catch (Exception e) {
                System.err.println("[AdminUserController] loadAllUsers: " + e.getMessage());
            }
        }, "LoadUsers").start();
    }

    /**
     * Refreshes KPI cards with count-up animation (matching Symfony JS animateTo).
     * Numbers count up from 0 to target over 1.2 seconds.
     */
    private void refreshKPICards(List<User> users) {
        int total    = users.size();
        int active   = (int) users.stream().filter(u -> isActiveStatus(u.getAccountStatus())).count();
        int coaches  = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_COACH")).count();
        int admins   = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_ADMIN")).count();
        int inactive = total - active;
        double rate  = total > 0 ? (active * 100.0 / total) : 0;

        // Count-up animation — same effect as Symfony JS animateTo()
        animateCountUp(totalUsersLabel,    total,    false, "purple");
        animateCountUp(activeUsersLabel,   active,   false, "green");
        animateCountUp(coachesLabel,       coaches,  false, "pink");
        animateCountUp(adminsLabel,        admins,   false, "blue");
        animateCountUp(inactiveUsersLabel, inactive, false, null);
        animateCountUp(activeRateLabel,    (int) rate, true, null);

        // Progress bars
        setProgress(progressTotal,   1.0);
        setProgress(progressActive,  total > 0 ? (double) active  / total : 0);
        setProgress(progressCoaches, total > 0 ? (double) coaches / total : 0);
        setProgress(progressAdmins,  total > 0 ? (double) admins  / total : 0);

        // Re-apply color styles
        applyProgressStyle(progressTotal,   "progress-purple");
        applyProgressStyle(progressActive,  "progress-green");
        applyProgressStyle(progressCoaches, "progress-pink");
        applyProgressStyle(progressAdmins,  "progress-blue");
    }

    /**
     * Animates a label from 0 to target like the Symfony JS count-up.
     * @param label     the Label to animate
     * @param target    the final integer value
     * @param isPercent if true, appends "%" to the displayed value
     * @param color     optional accent color hint ("purple","green","pink","blue") — unused but kept for extensibility
     */
    private void animateCountUp(Label label, int target, boolean isPercent, String color) {
        if (label == null) return;
        int steps = 60;
        double stepDuration = 1200.0 / steps; // 1.2s total

        final int[] current = {0};
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(stepDuration), e -> {
                    current[0]++;
                    int val = (int) Math.round(target * current[0] / (double) steps);
                    if (current[0] >= steps) val = target;
                    label.setText(isPercent ? val + "%" : String.valueOf(val));
                })
        );
        tl.setCycleCount(steps);
        tl.play();

        // Apply KPI large number style
        label.setStyle("-fx-font-size: 44px; -fx-font-weight: bold; -fx-text-fill: white;");
    }

    @FXML public void handleFilter() { currentPage = 1; applyFilters(); }

    @FXML public void handleClearFilters() {
        if (searchField       != null) searchField.clear();
        if (roleFilterCombo   != null) roleFilterCombo.setValue("All Roles");
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Statuses");
        currentPage = 1;
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField       != null ? searchField.getText().toLowerCase().trim() : "";
        String role   = roleFilterCombo   != null ? roleFilterCombo.getValue()   : "All Roles";
        String status = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";

        List<User> filtered = allUsers.stream().filter(u -> {
            if (!search.isBlank()) {
                boolean m = contains(u.getFullName(), search)
                        || contains(u.getUsername(), search)
                        || contains(u.getEmail(),    search);
                if (!m) return false;
            }
            if (role != null && !role.equals("All Roles")) {
                if (!hasRoleStr(u, role)) return false;
            }
            if (status != null && !status.equals("All Statuses")) {
                String s = u.getAccountStatus() != null ? u.getAccountStatus() : "active";
                if (!s.equalsIgnoreCase(status)) return false;
            }
            return true;
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages);
        int from    = (currentPage - 1) * PAGE_SIZE;
        int to      = Math.min(from + PAGE_SIZE, filtered.size());

        if (usersTable != null)
            usersTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));

        setLabel(resultCountLabel,    "Found " + filtered.size() + " user" + (filtered.size() != 1 ? "s" : ""));
        setLabel(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabel(paginationInfoLabel, filtered.isEmpty() ? ""
                : "Showing " + (from + 1) + " to " + to + " of " + filtered.size() + " entries");

        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }

    private void openDetail(User user) {
        SessionManager.setSelectedUser(user);
        navigateTo("AdminUserDetail.fxml");
    }

    private void handleSuspendToggle(User user) {
        if (!canModify(user)) {
            alert(Alert.AlertType.WARNING, "Permission Denied",
                    "Vous n'avez pas la permission de modifier ce compte.");
            return;
        }
        boolean isActive = isActiveStatus(user.getAccountStatus());
        String msg = isActive
                ? "Suspendre l'utilisateur \"" + user.getUsername() + "\" ?"
                : "Réactiver l'utilisateur \""  + user.getUsername() + "\" ?";
        if (!confirm("Confirmation", msg)) return;
        new Thread(() -> {
            try {
                if (isActive) userService.suspendUser(user.getId());
                else          userService.activateUser(user.getId());
                Platform.runLater(this::loadAllUsers);
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    private void handleDelete(User user) {
        if (!canModify(user)) {
            alert(Alert.AlertType.WARNING, "Permission Denied",
                    "Seul un Super Administrateur peut supprimer un compte Administrateur.");
            return;
        }
        if (!confirm("Supprimer l'utilisateur",
                "Supprimer définitivement \"" + user.getUsername() + "\" ?\nCette action est irréversible !"))
            return;
        new Thread(() -> {
            try {
                userService.deleteUser(user.getId());
                Platform.runLater(this::loadAllUsers);
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleNewUser() { openCreateUserModal(); loadAllUsers(); }

    private void openCreateUserModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(resolveStage());
        modal.setTitle("Créer un utilisateur");
        modal.setResizable(false);

        // ── Build all fields ──────────────────────────────────
        createFullNameField    = new TextField();
        createUsernameField    = new TextField();
        createEmailField       = new TextField();
        createPasswordField    = new PasswordField();
        createPasswordVisible  = new TextField();
        createTogglePasswordBtn= new Button("👁");
        createRoleCombo        = new ComboBox<>();
        createSubmitBtn        = new Button("✓  Create User");
        createCancelBtn        = new Button("Cancel");
        createPasswordStrength = new ProgressBar(0);
        createStrengthLabel    = new Label("");
        createRoleDescBox      = new VBox(4);
        createRoleTitleLabel   = new Label("");
        createRoleTextLabel    = new Label("");
        errFullName = errLabel(); errUsername = errLabel();
        errEmail    = errLabel(); errPassword = errLabel(); errRole = errLabel();

        String fieldStyle = "-fx-background-color: rgba(30,15,45,0.95);" +
                "-fx-border-color: rgba(255,60,100,0.30);" +
                "-fx-border-radius: 10; -fx-background-radius: 10;" +
                "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.35);" +
                "-fx-padding: 10 14;";

        createFullNameField.setPromptText("e.g. John Smith");
        createFullNameField.setPrefWidth(270); createFullNameField.setStyle(fieldStyle);

        createUsernameField.setPromptText("e.g. johnsmith");
        createUsernameField.setPrefWidth(230);
        createUsernameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.35); -fx-padding: 10;");

        createEmailField.setPromptText("e.g. john@example.com");
        createEmailField.setPrefWidth(270); createEmailField.setStyle(fieldStyle);

        createPasswordField.setPromptText("Min 6 characters");
        createPasswordField.setPrefWidth(220);
        createPasswordField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.35); -fx-padding: 10;");

        createPasswordVisible.setPromptText("Min 6 characters");
        createPasswordVisible.setPrefWidth(220);
        createPasswordVisible.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.35); -fx-padding: 10;");
        createPasswordVisible.setVisible(false); createPasswordVisible.setManaged(false);

        createTogglePasswordBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: rgba(170,170,200,1); -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 4;");
        createTogglePasswordBtn.setOnAction(e -> handleTogglePassword());

        createPasswordStrength.setPrefWidth(270); createPasswordStrength.setPrefHeight(4);
        createStrengthLabel.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(170,170,204,1);");

        createRoleTitleLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 13;");
        createRoleTextLabel.setStyle("-fx-text-fill: rgba(170,170,204,1); -fx-font-size: 12;");
        createRoleTextLabel.setWrapText(true);
        createRoleDescBox.getChildren().addAll(createRoleTitleLabel, createRoleTextLabel);
        createRoleDescBox.setStyle("-fx-background-color: rgba(26,26,58,1);" +
                "-fx-border-color: rgba(74,90,170,1);" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12;");
        createRoleDescBox.setVisible(false); createRoleDescBox.setManaged(false);

        createSubmitBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);" +
                "-fx-text-fill: white; -fx-background-radius: 10; -fx-border-color: transparent;" +
                "-fx-padding: 11 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13;" +
                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 12, 0, 0, 3);");
        createSubmitBtn.setOnAction(e -> handleCreateSubmit());

        createCancelBtn.setStyle("-fx-background-color: rgba(30,21,48,1);" +
                "-fx-border-color: rgba(68,68,102,1); -fx-border-radius: 10; -fx-background-radius: 10;" +
                "-fx-text-fill: rgba(170,170,204,1); -fx-padding: 11 20;" +
                "-fx-cursor: hand; -fx-font-size: 13;");
        createCancelBtn.setOnAction(e -> modal.close());

        // ── @ Username prefix ─────────────────────────────────
        Label atLabel = new Label("@");
        atLabel.setStyle("-fx-background-color: rgba(85,34,51,1); -fx-background-radius: 10;" +
                "-fx-text-fill: rgba(238,221,238,1); -fx-font-weight: bold; -fx-padding: 10;");
        HBox usernameBox = new HBox(0, atLabel, createUsernameField);
        usernameBox.setPrefWidth(270);
        usernameBox.setStyle("-fx-background-color: rgba(30,21,48,1);" +
                "-fx-border-color: rgba(255,60,100,0.30); -fx-border-radius: 10; -fx-background-radius: 10;");

        // ── Password row ──────────────────────────────────────
        HBox passwordBox = new HBox(0, createPasswordField, createPasswordVisible, createTogglePasswordBtn);
        passwordBox.setPrefWidth(270);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setStyle("-fx-background-color: rgba(30,21,48,1);" +
                "-fx-border-color: rgba(255,60,100,0.30); -fx-border-radius: 10; -fx-background-radius: 10;");

        String labelStyle = "-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: rgba(221,221,238,1);";
        String hintStyle  = "-fx-font-size: 10; -fx-text-fill: rgba(119,119,136,1);";

        Label lbFN = lbl("Full Name *",     labelStyle); Label htFN = lbl("Enter the user's full name", hintStyle);
        Label lbUN = lbl("Username *",      labelStyle); Label htUN = lbl("3-50 chars: letters, digits, - _", hintStyle);
        Label lbEM = lbl("Email Address *", labelStyle); Label htEM = lbl("Must be unique in the system", hintStyle);
        Label lbPW = lbl("Password *",      labelStyle); Label htPW = lbl("Minimum 6 characters recommended", hintStyle);

        VBox colFN = col(lbFN, createFullNameField, errFullName, htFN);
        VBox colUN = col(lbUN, usernameBox, errUsername, htUN);
        VBox colEM = col(lbEM, createEmailField, errEmail, htEM);
        HBox strengthRow = new HBox(createStrengthLabel);
        VBox colPW = new VBox(6, lbPW, passwordBox, createPasswordStrength, strengthRow, errPassword, htPW);
        colPW.setPrefWidth(280);

        HBox row1 = new HBox(16, colFN, colUN);
        HBox row2 = new HBox(16, colEM, colPW);

        Label lbRole = lbl("User Role *", labelStyle);
        Label htRole = lbl("Defines the user's permissions in the system", hintStyle);
        createRoleCombo.setPrefWidth(576);
        createRoleCombo.setStyle("-fx-background-color: rgba(30,21,48,1);" +
                "-fx-border-color: rgba(255,60,100,0.30); -fx-border-radius: 10; -fx-background-radius: 10;");
        VBox roleSection = new VBox(8, lbRole, createRoleCombo, errRole, htRole, createRoleDescBox);

        // ── Welcome notice ────────────────────────────────────
        Label lbWelcomeTitle = lbl("Welcome Email",
                "-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 13;");
        Label lbWelcomeText  = lbl("A welcome email will automatically be sent to the new user.",
                "-fx-text-fill: rgba(170,170,204,1); -fx-font-size: 12;");
        lbWelcomeText.setWrapText(true);
        HBox notice = new HBox(12, new VBox(2, lbWelcomeTitle, lbWelcomeText));
        notice.setAlignment(Pos.CENTER_LEFT);
        notice.setStyle("-fx-background-color: rgba(26,26,58,1);" +
                "-fx-border-color: rgba(74,90,170,1); -fx-border-radius: 12; -fx-background-radius: 12;" +
                "-fx-padding: 14;");

        VBox formBody = new VBox(20, row1, row2, roleSection, notice);
        formBody.setStyle("-fx-padding: 26; -fx-background-color: #0d0618;");

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(formBody);
        scroll.setFitToWidth(true); scroll.setMaxHeight(520);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ── Modal header ──────────────────────────────────────
        Label headerTitle = lbl("Create New User",
                "-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub   = lbl("Fill in the details to add a new member to the platform.",
                "-fx-font-size: 12; -fx-text-fill: rgba(204,204,255,1);");
        Label headerIcon  = lbl("👤",
                "-fx-font-size: 20; -fx-background-color: rgba(123,143,224,1);" +
                        "-fx-background-radius: 12; -fx-padding: 10;");
        HBox header = new HBox(14, headerIcon, new VBox(3, headerTitle, headerSub));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #5a6fd6, #667eea); -fx-padding: 24;");

        // ── Footer ────────────────────────────────────────────
        HBox footer = new HBox(10, createSubmitBtn, createCancelBtn);
        footer.setStyle("-fx-padding: 16; -fx-background-color: #0d0618;" +
                "-fx-border-color: rgba(51,51,68,1); -fx-border-width: 1 0 0 0;");

        VBox root = new VBox(0, header, scroll, footer);
        root.setStyle("-fx-background-color: #0d0618;");

        initCreateView();
        modal.setScene(new Scene(root, 640, Region.USE_COMPUTED_SIZE));
        modal.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  ② DETAIL VIEW
    // ═══════════════════════════════════════════════════════════
    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("User Details");
        User user = SessionManager.getSelectedUser();
        if (user == null) { navigateTo("AdminUsers.fxml"); return; }
        populateDetail(user);
    }

    private void populateDetail(User user) {
        User    me        = SessionManager.getCurrentUser();
        boolean isMe      = me != null && me.getId() == user.getId();
        boolean isAdmin   = hasRoleStr(user, "ROLE_ADMIN");
        boolean canModify = !isMe && (!isAdmin || SessionManager.isSuperAdmin());

        String initials = !user.getUsername().isEmpty()
                ? user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase()
                : "??";
        setLabel(avatarInitialLabel,  initials);
        setLabel(fullNameHeaderLabel, nvl(user.getFullName(), user.getUsername()));
        setLabel(usernameHeaderLabel, "@" + user.getUsername());
        setLabel(roleChipLabel,       getRoleLabel(user));

        String accStatus = user.getAccountStatus() != null ? user.getAccountStatus() : "active";
        setLabel(statusChipLabel, accStatus.toUpperCase());
        showNode(youBadgeLabel, isMe);

        boolean hasBio = user.getBio() != null && !user.getBio().isBlank();
        showNode(bioBox, hasBio);
        if (hasBio) setLabel(bioLabel, user.getBio());

        showNode(limitedPermBanner, !canModify && !isMe);
        showNode(selfViewBanner,    isMe);

        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_COACH", "ROLE_ADMIN"));
            roleComboBox.setValue(getPrimaryRole(user));
            roleComboBox.setDisable(!canModify);
        }
        if (updateRoleBtn != null) updateRoleBtn.setDisable(!canModify);
        showNode(rolePermNote, !canModify);
        setLabel(rolePermNote, isMe ? "Impossible de modifier votre propre rôle"
                : "Seuls les Super Admins peuvent modifier un Admin");

        boolean notActive = !isActiveStatus(accStatus);
        showNode(statusAlertBox, notActive);
        if (notActive) setLabel(statusAlertLabel,
                "Compte " + accStatus.toUpperCase() + " : " + getStatusDescription(accStatus));

        boolean activeStatus = isActiveStatus(accStatus);
        showNode(suspendBtn,    activeStatus);
        showNode(banBtn,        activeStatus);
        showNode(reactivateBtn, !activeStatus);

        if (suspendBtn    != null) suspendBtn.setDisable(!canModify);
        if (banBtn        != null) banBtn.setDisable(!canModify);
        if (reactivateBtn != null) reactivateBtn.setDisable(!canModify);
        if (deleteBtn     != null) deleteBtn.setDisable(!canModify);

        showNode(actionPermNote, !canModify);
        setLabel(actionPermNote, isMe ? "Auto-modification interdite" : "Permission Super Admin requise");

        setLabel(emailInfoLabel,    user.getEmail());
        setLabel(usernameInfoLabel, "@" + user.getUsername());
        setLabel(fullNameInfoLabel, nvl(user.getFullName(), "Non renseigné"));
        setLabel(registeredLabel,   user.getCreatedAt() != null ? user.getCreatedAt().toString() : "—");
        setLabel(registeredAgoLabel, "");
        setLabel(lastLoginLabel,    user.getLastLogin() != null ? user.getLastLogin().toString() : "Jamais");
        setLabel(statusInfoLabel,   accStatus.toUpperCase());

        List<TeamMembership> memberships = loadTeamMemberships(user.getId());
        int teamCount = memberships != null ? memberships.size() : 0;
        setLabel(teamsBadge, String.valueOf(teamCount));
        showNode(teamsEmptyState, teamCount == 0);
        if (teamsTable != null) { teamsTable.setVisible(teamCount > 0); teamsTable.setManaged(teamCount > 0); }
    }

    private List<TeamMembership> loadTeamMemberships(int userId) {
        try { return userService.getTeamMemberships(userId); } catch (Exception e) { return List.of(); }
    }

    @FXML public void handleUpdateRole() {
        if (roleComboBox == null) return;
        User user = SessionManager.getSelectedUser();
        if (user == null) return;
        String newRole = roleComboBox.getValue();
        if ("ROLE_ADMIN".equals(newRole) && !SessionManager.isSuperAdmin()) {
            alert(Alert.AlertType.WARNING, "Permission refusée",
                    "Seuls les Super Administrateurs peuvent assigner le rôle Admin.");
            return;
        }
        new Thread(() -> {
            try {
                userService.updateUserRole(user.getId(), newRole);
                Platform.runLater(() -> {
                    alert(Alert.AlertType.INFORMATION, "Succès", "Rôle mis à jour avec succès.");
                    User refreshed = userService.findById(user.getId());
                    SessionManager.setSelectedUser(refreshed);
                    populateDetail(refreshed);
                });
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleSuspend() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Suspendre", "Suspendre l'utilisateur \"" + user.getUsername() + "\" ?")) return;
        new Thread(() -> {
            try { userService.suspendUser(user.getId()); Platform.runLater(() -> refreshDetail(user.getId())); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage())); }
        }).start();
    }

    @FXML public void handleBan() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Bannir", "Bannir définitivement \"" + user.getUsername() + "\" ? Action sérieuse.")) return;
        new Thread(() -> {
            try { userService.banUser(user.getId()); Platform.runLater(() -> refreshDetail(user.getId())); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage())); }
        }).start();
    }

    @FXML public void handleReactivate() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Réactiver", "Réactiver le compte de \"" + user.getUsername() + "\" ?")) return;
        new Thread(() -> {
            try { userService.activateUser(user.getId()); Platform.runLater(() -> refreshDetail(user.getId())); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage())); }
        }).start();
    }

    @FXML public void handleDeleteUser() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Supprimer", "Supprimer définitivement \"" + user.getUsername() + "\" ?\nIrréversible !")) return;
        new Thread(() -> {
            try { userService.deleteUser(user.getId()); Platform.runLater(() -> navigateTo("AdminUsers.fxml")); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage())); }
        }).start();
    }

    @FXML public void goBackToList() { navigateTo("AdminUsers.fxml"); }

    private void refreshDetail(int userId) {
        User refreshed = userService.findById(userId);
        SessionManager.setSelectedUser(refreshed);
        populateDetail(refreshed);
    }

    // ═══════════════════════════════════════════════════════════
    //  ③ CREATE
    // ═══════════════════════════════════════════════════════════
    private void initCreateView() {
        setupCreateRoleCombo();
        setupPasswordStrength();
        setupCreateValidation();
    }

    private void setupCreateRoleCombo() {
        if (createRoleCombo == null) return;
        createRoleCombo.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_COACH", "ROLE_ADMIN"));
        createRoleCombo.setValue("ROLE_USER");
        createRoleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(roleDisplayName(item));
                    setDisable("ROLE_ADMIN".equals(item) && !SessionManager.isSuperAdmin());
                }
            }
        });
        createRoleCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) setText(roleDisplayName(item));
            }
        });
        createRoleCombo.setOnAction(e -> updateRoleDesc(createRoleCombo.getValue()));
        updateRoleDesc("ROLE_USER");
    }

    private void updateRoleDesc(String role) {
        if (createRoleDescBox == null) return;
        String title, text;
        switch (nvl(role, "")) {
            case "ROLE_COACH" -> { title = "Coach";          text = "Toutes les permissions User + créer des équipes et gérer les membres."; }
            case "ROLE_ADMIN" -> { title = "Administrateur"; text = "Accès complet : gérer tous les utilisateurs, équipes, contenu et paramètres."; }
            default           -> { title = "Utilisateur";    text = "Peut rejoindre des équipes, participer aux activités et gérer son profil."; }
        }
        setLabel(createRoleTitleLabel, title);
        setLabel(createRoleTextLabel,  text);
        createRoleDescBox.setVisible(true);
        createRoleDescBox.setManaged(true);
    }

    private void setupPasswordStrength() {
        if (createPasswordField == null || createPasswordStrength == null) return;
        createPasswordField.textProperty().addListener((obs, o, n) -> {
            evaluateStrength(n);
            if (createPasswordVisible != null) createPasswordVisible.setText(n);
        });
        if (createPasswordVisible != null) {
            createPasswordVisible.setVisible(false);
            createPasswordVisible.setManaged(false);
            createPasswordVisible.textProperty().addListener((obs, o, n) -> {
                if (!createPasswordField.getText().equals(n)) {
                    createPasswordField.setText(n);
                    evaluateStrength(n);
                }
            });
        }
    }

    private void evaluateStrength(String pwd) {
        int s = 0;
        if (pwd.length() >= 6)  s++;
        if (pwd.length() >= 10) s++;
        if (pwd.matches(".*[a-z].*") && pwd.matches(".*[A-Z].*")) s++;
        if (pwd.matches(".*[0-9].*")) s++;
        if (pwd.matches(".*[^a-zA-Z0-9].*")) s++;
        double progress = Math.min(s, 4) / 4.0;
        String color, label, styleClass;
        if      (s <= 1) { color = "#ff6b6b"; label = "Faible"; styleClass = "strength-weak"; }
        else if (s == 2) { color = "#ffa751"; label = "Moyen";  styleClass = "strength-fair"; }
        else if (s == 3) { color = "#fee140"; label = "Bon";    styleClass = "strength-good"; }
        else             { color = "#43e97b"; label = "Fort";   styleClass = "strength-strong"; }
        if (createPasswordStrength != null) {
            createPasswordStrength.setProgress(progress);
            applyProgressStyle(createPasswordStrength, styleClass);
        }
        setLabel(createStrengthLabel, label);
        if (createStrengthLabel != null)
            createStrengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10;");
    }

    private void setupCreateValidation() {
        addBlurValidation(createFullNameField, errFullName, f -> !f.isBlank(), "Nom requis.");
        addBlurValidation(createUsernameField, errUsername,
                f -> f.matches("[a-zA-Z0-9_-]{3,50}"), "3-50 caractères : lettres, chiffres, - et _.");
        addBlurValidation(createEmailField, errEmail,
                f -> f.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Format email invalide.");
    }

    private void addBlurValidation(TextField field, Label errLabel,
                                   java.util.function.Predicate<String> rule, String msg) {
        if (field == null) return;
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) {
                String v = field.getText().trim();
                if (!rule.test(v)) setErr(errLabel, msg);
                else               clearErr(errLabel);
            }
        });
    }

    @FXML public void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (createPasswordField   != null) { createPasswordField.setVisible(!passwordVisible);  createPasswordField.setManaged(!passwordVisible); }
        if (createPasswordVisible != null) { createPasswordVisible.setVisible(passwordVisible); createPasswordVisible.setManaged(passwordVisible); }
        if (createTogglePasswordBtn != null) createTogglePasswordBtn.setText(passwordVisible ? "🙈" : "👁");
    }

    @FXML public void handleCreateSubmit() {
        clearAllErrors();
        if (!validateCreateForm()) return;
        String role = createRoleCombo != null ? createRoleCombo.getValue() : "ROLE_USER";
        if ("ROLE_ADMIN".equals(role) && !SessionManager.isSuperAdmin()) {
            setErr(errRole, "Seuls les Super Admins peuvent créer un compte Admin.");
            return;
        }
        String fullName = trim(createFullNameField);
        String username = trim(createUsernameField);
        String email    = trim(createEmailField);
        String password = createPasswordField != null ? createPasswordField.getText() : "";
        if (createSubmitBtn != null) { createSubmitBtn.setText("Création…"); createSubmitBtn.setDisable(true); }
        new Thread(() -> {
            try {
                if (userService.findByUsername(username) != null) {
                    Platform.runLater(() -> { setErr(errUsername, "Ce username est déjà pris."); resetCreateBtn(); }); return;
                }
                if (userService.emailExists(email)) {
                    Platform.runLater(() -> { setErr(errEmail, "Cet email est déjà enregistré."); resetCreateBtn(); }); return;
                }
                userService.adminCreateUser(fullName, username, email, password, role);
                Platform.runLater(this::closeModal);
            } catch (Exception e) {
                Platform.runLater(() -> { resetCreateBtn(); alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); });
            }
        }).start();
    }

    @FXML public void handleCreateCancel() { closeModal(); }

    private boolean validateCreateForm() {
        boolean ok = true;
        if (trim(createFullNameField).isBlank())               { setErr(errFullName, "Nom requis."); ok = false; }
        if (!trim(createUsernameField).matches("[a-zA-Z0-9_-]{3,50}")) { setErr(errUsername, "Username invalide (3-50 chars)."); ok = false; }
        if (!trim(createEmailField).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { setErr(errEmail, "Email invalide."); ok = false; }
        String pwd = createPasswordField != null ? createPasswordField.getText() : "";
        if (pwd.isBlank())     { setErr(errPassword, "Mot de passe requis."); ok = false; }
        else if (pwd.length() < 6) { setErr(errPassword, "Minimum 6 caractères."); ok = false; }
        return ok;
    }

    private void resetCreateBtn() {
        if (createSubmitBtn != null) { createSubmitBtn.setText("✓  Create User"); createSubmitBtn.setDisable(false); }
    }

    private void clearAllErrors() {
        for (Label l : new Label[]{ errFullName, errUsername, errEmail, errPassword, errRole }) clearErr(l);
    }

    private void closeModal() {
        javafx.scene.Node[] candidates = { createSubmitBtn, createCancelBtn, createFullNameField,
                createUsernameField, createEmailField, createPasswordField };
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null && n.getScene().getWindow() != null) {
                ((Stage) n.getScene().getWindow()).close();
                return;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════
    private boolean isActiveStatus(String status) { return status == null || "active".equalsIgnoreCase(status); }

    private boolean canModify(User user) {
        User me = SessionManager.getCurrentUser();
        if (me == null || user == null) return false;
        if (me.getId() == user.getId()) return false;
        boolean isTargetAdmin = hasRoleStr(user, "ROLE_ADMIN");
        return !isTargetAdmin || SessionManager.isSuperAdmin();
    }

    private boolean hasRoleStr(User user, String role) {
        if (user == null) return false;
        String json = user.getRolesJson();
        if (json != null) return json.contains(role);
        List<String> roles = user.getRoles();
        return roles != null && roles.contains(role);
    }

    private String getRoleLabel(User u) {
        if (hasRoleStr(u, "ROLE_SUPER_ADMIN")) return "Super Admin";
        if (hasRoleStr(u, "ROLE_ADMIN"))       return "Admin";
        if (hasRoleStr(u, "ROLE_COACH"))       return "Coach";
        return "User";
    }

    private String getPrimaryRole(User u) {
        if (hasRoleStr(u, "ROLE_ADMIN")) return "ROLE_ADMIN";
        if (hasRoleStr(u, "ROLE_COACH")) return "ROLE_COACH";
        return "ROLE_USER";
    }

    private String getStatusDescription(String status) {
        if (status == null) return "";
        return switch (status.toLowerCase()) {
            case "suspended" -> "Ce compte est temporairement suspendu.";
            case "banned"    -> "Ce compte est définitivement banni.";
            case "pending"   -> "Ce compte est en attente d'approbation.";
            default          -> "";
        };
    }

    private String roleDisplayName(String role) {
        return switch (nvl(role, "")) {
            case "ROLE_COACH" -> "Coach — Gestion d'équipes";
            case "ROLE_ADMIN" -> "Administrateur — Accès complet"
                    + (SessionManager.isSuperAdmin() ? "" : " (Super Admin uniquement)");
            default           -> "Utilisateur — Accès standard";
        };
    }

    // ── UI helpers ─────────────────────────────────────────────
    private void setLabel(Label l, String v)           { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v) { if (pb != null) pb.setProgress(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }
    private String trim(TextField f)              { return f != null && f.getText() != null ? f.getText().trim() : ""; }
    private String nvl(String s, String fallback) { return s != null && !s.isBlank() ? s : fallback; }
    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }
    private void setErr(Label l, String msg) {
        if (l == null) return;
        l.setText(msg); l.setVisible(true); l.setManaged(true);
        l.setStyle("-fx-text-fill: #ff6b7a; -fx-font-size: 11;");
    }
    private void clearErr(Label l) {
        if (l != null) { l.setText(""); l.setVisible(false); l.setManaged(false); }
    }

    /** Creates a styled error label (hidden by default) */
    private Label errLabel() {
        Label l = new Label("");
        l.setVisible(false); l.setManaged(false);
        l.setStyle("-fx-text-fill: #ff6b7a; -fx-font-size: 11;");
        return l;
    }

    /** Quick Label factory */
    private Label lbl(String text, String style) {
        Label l = new Label(text); l.setStyle(style); return l;
    }

    /** Quick VBox column for form fields */
    private VBox col(javafx.scene.Node... nodes) {
        VBox v = new VBox(6); v.getChildren().addAll(nodes); v.setPrefWidth(280); return v;
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
        if (url == null) { System.err.println("[AdminUserController] FXML introuvable : " + fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setClassLoader(getClass().getClassLoader());
            Parent root  = loader.load();
            Stage  stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminUserController] Nav error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private URL resolveUrl(String fxml) {
        for (String p : new String[]{
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml }) {
            URL u = getClass().getResource(p);
            if (u != null) return u;
        }
        return null;
    }

    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                searchField, usersTable, avatarInitialLabel,
                fullNameHeaderLabel, createSubmitBtn, totalUsersLabel,
                prevPageBtn, nextPageBtn
        };
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    // ── Navigation handlers ────────────────────────────────────
    @FXML public void goToDashboard()         { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToPlanning()          { navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { navigateTo("AdminTournois.fxml"); }
    @FXML public void goToVideos()            { navigateTo("AdminVideos.fxml"); }
    @FXML public void goToCoachApplications() { navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { navigateTo("AdminMessages.fxml"); }
    @FXML public void goToTeams()             { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToSite()              { navigateTo("home.fxml"); }
    @FXML public void goToProfile()           { navigateTo("AdminProfile.fxml"); }
    @FXML public void goToAuditLogs() {
        if (!SessionManager.isSuperAdmin()) return;
        navigateTo("AdminAuditLogs.fxml");
    }
    @FXML public void handleLogout() { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }






}