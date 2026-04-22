package com.eyetwin.controller.admin;

import com.eyetwin.entities.MemberRole;
import com.eyetwin.entities.MembershipStatus;
import com.eyetwin.entities.User;
import com.eyetwin.entities.TeamMembership;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.EmailService;
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
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class AdminUserController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

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


    @FXML private TableView<TeamMembership>           teamsTable;
    @FXML private TableColumn<TeamMembership, String> colTeamName;
    @FXML private TableColumn<TeamMembership, String> colTeamRole;
    @FXML private TableColumn<TeamMembership, String> colTeamStatus;
    @FXML private TableColumn<TeamMembership, String> colTeamJoined;

    @FXML private StackPane avatarPane;


    @FXML private TextField        createFullNameField;
    @FXML private TextField        createUsernameField;
    @FXML private TextField        createEmailField;
    @FXML private PasswordField    createPasswordField;
    @FXML private TextField        createPasswordVisible;
    @FXML private Button           createTogglePasswordBtn;
    @FXML private ComboBox<String> createRoleCombo;
    @FXML private Button           createSubmitBtn;
    @FXML private Button           createCancelBtn;
    @FXML private ProgressBar      createPasswordStrength;
    @FXML private Label            createStrengthLabel;
    @FXML private VBox             createRoleDescBox;
    @FXML private Label            createRoleTitleLabel;
    @FXML private Label            createRoleTextLabel;
    @FXML private Label errFullName, errUsername, errEmail, errPassword, errRole;

    private IUserService         userService;
    private ObservableList<User> allUsers      = FXCollections.observableArrayList();
    private boolean              passwordVisible = false;

    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;
    private int totalPages  = 1;

    // ─────────────────────────────────────────────────────────────
    //  SHARED STYLE CONSTANTS
    // ─────────────────────────────────────────────────────────────
    private static final String BG_DARK        = "#0d0618";
    private static final String BG_FIELD       = "#160a22";
    private static final String BG_HEADER      = "#1a0a22";
    private static final String RED_PRIMARY    = "#ff3c64";
    private static final String RED_SOFT       = "#ff8fa3";
    private static final String RED_BORDER     = "rgba(255,60,100,0.30)";
    private static final String RED_BORDER_MED = "rgba(255,60,100,0.28)";
    private static final String RED_AT_BG      = "rgba(255,60,100,0.15)";

    private static final String FIELD_STYLE =
            "-fx-background-color:" + BG_FIELD + ";" +
                    "-fx-border-color:" + RED_BORDER + ";" +
                    "-fx-border-radius:9;-fx-background-radius:9;" +
                    "-fx-text-fill:white;" +
                    "-fx-prompt-text-fill:rgba(255,255,255,0.28);" +
                    "-fx-padding:10 14;";

    private static final String FIELD_TRANSPARENT =
            "-fx-background-color:transparent;" +
                    "-fx-border-color:transparent;" +
                    "-fx-text-fill:white;" +
                    "-fx-prompt-text-fill:rgba(255,255,255,0.28);" +
                    "-fx-padding:10 14;";

    private static final String LABEL_FIELD =
            "-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.80);";

    private static final String LABEL_HINT =
            "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.30);";

    private static final String LABEL_STAR =
            "-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:" + RED_PRIMARY + ";";

    private static final String NOTICE_BOX_STYLE =
            "-fx-background-color:" + BG_FIELD + ";" +
                    "-fx-border-color:" + RED_BORDER_MED + ";" +
                    "-fx-border-radius:12;-fx-background-radius:12;" +
                    "-fx-padding:14 16;";

    private static final String BTN_CREATE =
            "-fx-background-color:" + RED_PRIMARY + ";" +
                    "-fx-text-fill:white;" +
                    "-fx-background-radius:9;" +
                    "-fx-border-color:transparent;" +
                    "-fx-padding:11 24;" +
                    "-fx-cursor:hand;" +
                    "-fx-font-weight:bold;" +
                    "-fx-font-size:13;" +
                    "-fx-effect:dropshadow(gaussian,rgba(255,60,100,0.50),14,0,0,4);";

    private static final String BTN_CANCEL =
            "-fx-background-color:transparent;" +
                    "-fx-border-color:rgba(255,255,255,0.12);" +
                    "-fx-border-width:1;" +
                    "-fx-border-radius:9;-fx-background-radius:9;" +
                    "-fx-text-fill:rgba(255,255,255,0.50);" +
                    "-fx-padding:11 20;" +
                    "-fx-cursor:hand;" +
                    "-fx-font-size:13;";

    // ═══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════

    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color:" + BG_FIELD + ";" +
                        "-fx-border-color:" + RED_BORDER + ";" +
                        "-fx-border-radius:9;-fx-background-radius:9;" +
                        "-fx-text-fill:white;"
        );

        combo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-background-color:" + BG_FIELD + "; -fx-text-fill: white;");
            }
        });

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-background-color:" + BG_FIELD + "; -fx-text-fill: white;");
            }
        });

        // STYLE DU POPUP (LA LISTE QUI S'OUVRE)
        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) return;

            Platform.runLater(() -> {
                for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                    if (w instanceof javafx.stage.PopupWindow && w.isShowing()) {

                        javafx.scene.Node list = w.getScene().getRoot().lookup(".list-view");

                        if (list != null) {
                            list.setStyle(
                                    "-fx-background-color:" + BG_FIELD + ";" +
                                            "-fx-control-inner-background:" + BG_FIELD + ";" +
                                            "-fx-border-color:" + RED_BORDER + ";"
                            );
                        }

                        styleListCells(w.getScene().getRoot());
                    }
                }
            });
        });
    }

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        userService = new UserServiceImpl();
        if (adminSidebarController != null) adminSidebarController.setActivePage("users");
        if (adminTopbarController  != null) adminTopbarController.setTitle("User Management");
        if (usersTable         != null) initListView();
        if (avatarInitialLabel != null) initDetailView();

        if (createSubmitBtn != null) {
            initCreateView();
            if (createRoleCombo != null) {
                styleComboBox(createRoleCombo);
            }

            // Patch ScrollPane viewport — FXML form only
            Platform.runLater(() -> Platform.runLater(() -> {
                if (createRoleDescBox == null || createRoleDescBox.getScene() == null) return;
                createRoleDescBox.getScene().getRoot().lookupAll(".scroll-pane").forEach(n -> {
                    if (n instanceof ScrollPane sp) {
                        javafx.scene.Node vp = sp.lookup(".viewport");
                        if (vp instanceof javafx.scene.layout.Region r) {
                            r.setBackground(new Background(new BackgroundFill(
                                    Color.web(BG_DARK), CornerRadii.EMPTY, Insets.EMPTY)));
                            r.setStyle(
                                    "-fx-background-color:" + BG_DARK + ";" +
                                            "-fx-background:" + BG_DARK + ";");
                        }
                    }
                });
            }));
        }

        Platform.runLater(this::applyGamingTheme);
    }

    private void applyGamingTheme() {
        applyProgressBarStyles();
        applyTableTheme();
        Platform.runLater(() -> {
            for (ComboBox<String> combo : new ComboBox[]{roleFilterCombo, statusFilterCombo, createRoleCombo}){
                if (combo == null) continue;
                combo.setStyle(
                        "-fx-background-color:rgba(20,10,35,0.95);" +
                                "-fx-border-color:" + RED_BORDER + ";" +
                                "-fx-border-radius:10;-fx-background-radius:10;" +
                                "-fx-text-fill:white;");
                combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                    if (!isShowing) return;
                    Platform.runLater(() -> {
                        javafx.stage.Window popupWindow = null;
                        for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                            if (w instanceof javafx.stage.PopupWindow && w.isShowing()) {
                                popupWindow = w; break;
                            }
                        }
                        if (popupWindow == null || popupWindow.getScene() == null) return;
                        javafx.scene.Node listView = popupWindow.getScene().getRoot().lookup(".list-view");
                        if (listView != null) listView.setStyle(
                                "-fx-background-color:rgba(14,7,28,0.99);" +
                                        "-fx-border-color:" + RED_BORDER + ";-fx-border-width:1;" +
                                        "-fx-background-radius:10;-fx-border-radius:10;" +
                                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),20,0,0,4);");
                        styleListCells(popupWindow.getScene().getRoot());
                    });
                });
            }
        });
    }

    private void styleListCells(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof javafx.scene.control.ListCell) {
                javafx.scene.control.ListCell<?> cell = (javafx.scene.control.ListCell<?>) node;
                cell.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.85);-fx-padding:8 14;");
                cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color:rgba(255,60,100,0.15);-fx-text-fill:white;-fx-padding:8 14;"));
                cell.setOnMouseExited(e  -> cell.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.85);-fx-padding:8 14;"));
            } else if (node instanceof javafx.scene.Parent) {
                styleListCells((javafx.scene.Parent) node);
            }
        }
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
                "-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-table-cell-border-color:transparent;" +
                        "-fx-control-inner-background:rgba(20,10,35,0.80);" +
                        "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node headerBg = usersTable.lookup(".column-header-background");
            if (headerBg != null) headerBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-padding:0;");
            javafx.scene.Node filler = usersTable.lookup(".column-header-background .filler");
            if (filler != null) filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);");
            usersTable.lookupAll(".column-header").forEach(node -> node.setStyle(
                    "-fx-background-color:rgba(8,4,16,0.98);" +
                            "-fx-border-color:transparent transparent rgba(255,60,100,0.35) transparent;" +
                            "-fx-border-width:0 0 1 0;-fx-size:48px;"));
            usersTable.lookupAll(".column-header .label").forEach(node -> node.setStyle(
                    "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;" +
                            "-fx-background-color:transparent;-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
        applyDarkRowFactory();
    }

    private void applyDarkRowFactory() {
        if (usersTable == null) return;
        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>() {
                @Override protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null)
                        setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-border-width:0;");
                    else applyRowStyle(this, user, false);
                }
            };
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08);-fx-background-radius:10;" +
                            "-fx-border-color:rgba(255,60,100,0.30);-fx-border-width:1;" +
                            "-fx-border-radius:10;-fx-cursor:hand;"); });
            row.setOnMouseExited(e -> { if (!row.isEmpty()) applyRowStyle(row, row.getItem(), row.isSelected()); });
            row.selectedProperty().addListener((obs, was, is) -> { if (!row.isEmpty() && row.getItem() != null) applyRowStyle(row, row.getItem(), is); });
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) openDetail(row.getItem()); });
            return row;
        });
    }

    private void applyRowStyle(TableRow<User> row, User user, boolean selected) {
        if (selected) {
            row.setStyle(
                    "-fx-background-color:linear-gradient(to right,rgba(255,60,100,0.25),rgba(120,40,200,0.25));" +
                            "-fx-background-radius:10;-fx-border-color:rgba(255,60,100,0.55);" +
                            "-fx-border-width:1 1 1 3;-fx-border-radius:10;");
        } else {
            String bg = (row.getIndex() % 2 == 0)
                    ? "-fx-background-color:rgba(20,10,35,0.85);"
                    : "-fx-background-color:rgba(30,15,45,0.70);";
            row.setStyle(bg + "-fx-background-radius:10;-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1;-fx-border-radius:10;");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LIST VIEW
    // ═══════════════════════════════════════════════════════════
    private void initListView() {
        setupFilterCombos();
        setupTable();
        loadAllUsers();
    }

    private void setupFilterCombos() {
        if (roleFilterCombo != null) {
            roleFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Roles","ROLE_USER","ROLE_COACH","ROLE_ADMIN","ROLE_SUPER_ADMIN"));
            roleFilterCombo.setValue("All Roles");
            roleFilterCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) setText(roleDisplayNameFilter(item));
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:transparent;-fx-padding:8 14;");
                }
            });
            roleFilterCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) setText(roleDisplayNameFilter(item));
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:transparent;");
                }
            });
        }
        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Statuses","active","suspended","banned"));
            statusFilterCombo.setValue("All Statuses");
            statusFilterCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) setText(item != null ? item.substring(0,1).toUpperCase()+item.substring(1) : "");
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:transparent;-fx-padding:8 14;");
                }
            });
            statusFilterCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty) setText(item != null ? item.substring(0,1).toUpperCase()+item.substring(1) : "");
                    setStyle("-fx-text-fill:rgba(255,255,255,0.85);-fx-background-color:transparent;");
                }
            });
        }
    }

    private String roleDisplayNameFilter(String role) {
        if (role == null) return "";
        return switch (role) {
            case "All Roles"        -> "All Roles";
            case "ROLE_USER"        -> "🎮  Gamers";
            case "ROLE_COACH"       -> "⚡  Coaches";
            case "ROLE_ADMIN"       -> "🛡  Admins";
            case "ROLE_SUPER_ADMIN" -> "👑  Super Admins";
            default                 -> role;
        };
    }

    private void setupTable() {
        if (usersTable == null) return;

        if (colAvatar != null) {
            colAvatar.setCellFactory(col -> new TableCell<>() {
                {
                    setAlignment(Pos.CENTER);
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null) newRow.itemProperty().addListener((o, oldUser, newUser) -> refreshAvatar(newUser));
                    });
                }
                private void refreshAvatar(User u) {
                    setGraphic(null);
                    if (u == null) return;
                    String photoFile = u.getProfilePicture();
                    if (photoFile != null && !photoFile.isBlank()) {
                        try {
                            java.io.File file = new java.io.File(System.getProperty("user.dir") + "/uploads/profiles/" + photoFile);
                            if (file.exists()) {
                                javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 40, 40, true, true);
                                if (!img.isError()) {
                                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                                    iv.setFitWidth(40); iv.setFitHeight(40); iv.setPreserveRatio(false);
                                    iv.setClip(new javafx.scene.shape.Circle(20, 20, 20));
                                    StackPane pane = new StackPane(iv);
                                    pane.setMinSize(40,40); pane.setMaxSize(40,40);
                                    pane.setStyle("-fx-background-radius:20;-fx-border-radius:20;-fx-border-color:rgba(255,255,255,0.20);-fx-border-width:2;");
                                    setGraphic(pane); return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    String initials = u.getUsername() != null && !u.getUsername().isEmpty()
                            ? u.getUsername().substring(0, Math.min(2, u.getUsername().length())).toUpperCase() : "?";
                    Label avatar = new Label(initials);
                    avatar.setStyle(
                            "-fx-background-color:" + getAvatarGradient(u) + ";" +
                                    "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13;" +
                                    "-fx-min-width:40;-fx-min-height:40;-fx-max-width:40;-fx-max-height:40;" +
                                    "-fx-background-radius:20;-fx-alignment:center;" +
                                    "-fx-border-color:rgba(255,255,255,0.15);-fx-border-width:2;-fx-border-radius:20;");
                    setGraphic(avatar);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) refreshAvatar(row.getItem());
                }
            });
        }

        if (colUsername != null) colUsername.setCellValueFactory(d -> new SimpleStringProperty("@" + d.getValue().getUsername()));
        if (colFullName != null) colFullName.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getFullName(), "N/A")));
        if (colEmail    != null) colEmail.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().getEmail()));

        if (colRole != null) {
            colRole.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null) newRow.itemProperty().addListener((o, oldU, newU) -> refreshRole(newU));
                    });
                }
                private void refreshRole(User u) {
                    setGraphic(null);
                    if (u == null) return;
                    setGraphic(makeBadgeLabel(getRoleLabel(u), getRoleBadgeStyle(u)));
                    setAlignment(Pos.CENTER_LEFT);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) refreshRole(row.getItem());
                }
            });
        }

        if (colStatus != null) {
            colStatus.setCellFactory(col -> new TableCell<>() {
                {
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null) newRow.itemProperty().addListener((o, oldU, newU) -> refreshStatus(newU));
                    });
                }
                private void refreshStatus(User u) {
                    setGraphic(null);
                    if (u == null) return;
                    String status = u.getAccountStatus() != null ? u.getAccountStatus() : "active";
                    setGraphic(makeBadgeLabel(status.toUpperCase(), getStatusBadgeStyle(status)));
                    setAlignment(Pos.CENTER_LEFT);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) refreshStatus(row.getItem());
                }
            });
        }

        if (colJoined != null) colJoined.setCellValueFactory(d -> {
            String date = d.getValue().getCreatedAt() != null
                    ? d.getValue().getCreatedAt().toString().substring(0,10) : "—";
            return new SimpleStringProperty(date);
        });

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = makeActionBtn("👁", "info");
                private final Button actBtn  = makeActionBtn("⚠", "warning");
                private final Button delBtn  = makeActionBtn("🗑", "danger");
                private final HBox   box     = new HBox(5, viewBtn, actBtn, delBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    viewBtn.setOnAction(e -> { User u = currentUser(); if (u != null) openDetail(u); });
                    actBtn.setOnAction(e  -> { User u = currentUser(); if (u != null) handleSuspendToggle(u); });
                    delBtn.setOnAction(e  -> { User u = currentUser(); if (u != null) handleDelete(u); });
                    tableRowProperty().addListener((obs, oldRow, newRow) -> {
                        if (newRow != null) newRow.itemProperty().addListener((o, oldU, newU) -> refreshActions(newU));
                    });
                }
                private User currentUser() { TableRow<User> row = getTableRow(); return (row != null) ? row.getItem() : null; }
                private void refreshActions(User u) {
                    setGraphic(null);
                    if (u == null) return;
                    boolean canModify = canModify(u);
                    actBtn.setDisable(!canModify); delBtn.setDisable(!canModify);
                    boolean isActive = isActiveStatus(u.getAccountStatus());
                    actBtn.setText(isActive ? "⚠" : "✓");
                    actBtn.setTooltip(new Tooltip(isActive ? "Suspend" : "Reactivate"));
                    setGraphic(box);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    TableRow<User> row = getTableRow();
                    if (row != null && row.getItem() != null) refreshActions(row.getItem());
                }
            });
        }

        usersTable.setPlaceholder(new Label("No users found"));
    }

    private String getAvatarGradient(User u) {
        if (hasRoleStr(u, "ROLE_ADMIN")) return "linear-gradient(to bottom right,#ff3c64,#ff1744)";
        if (hasRoleStr(u, "ROLE_COACH")) return "linear-gradient(to bottom right,#f093fb,#f5576c)";
        return "linear-gradient(to bottom right,#667eea,#764ba2)";
    }

    private String getRoleBadgeStyle(User u) {
        if (hasRoleStr(u, "ROLE_SUPER_ADMIN")) return "dark";
        if (hasRoleStr(u, "ROLE_ADMIN"))       return "danger";
        if (hasRoleStr(u, "ROLE_COACH"))       return "warning";
        return "info";
    }

    private String getStatusBadgeStyle(String status) {
        if (status == null) return "success";
        return switch (status.toLowerCase()) {
            case "active"    -> "success";
            case "suspended" -> "warning";
            case "banned"    -> "danger";
            default          -> "info";
        };
    }

    private Label makeBadgeLabel(String text, String variant) {
        Label badge = new Label(text);
        String bg, border, color;
        switch (variant) {
            case "success" -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.45)";  color="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.45)";  color="#ff3c64"; }
            case "warning" -> { bg="rgba(255,193,7,0.15)";   border="rgba(255,193,7,0.45)";   color="#ffd54f"; }
            case "info"    -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.45)";  color="#4facfe"; }
            case "dark"    -> { bg="rgba(30,30,30,0.60)";    border="rgba(255,255,255,0.25)"; color="rgba(255,255,255,0.85)"; }
            default        -> { bg="rgba(255,255,255,0.05)"; border="rgba(255,255,255,0.15)"; color="white"; }
        }
        badge.setStyle(
                "-fx-background-color:"+bg+";-fx-border-color:"+border+";" +
                        "-fx-border-width:1;-fx-background-radius:8;-fx-border-radius:8;" +
                        "-fx-text-fill:"+color+";-fx-font-weight:bold;-fx-font-size:11;-fx-padding:5 12;");
        return badge;
    }

    private Button makeActionBtn(String text, String variant) {
        Button b = new Button(text);
        String bg, border, color;
        switch (variant) {
            case "info"    -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.40)";  color="#4facfe"; }
            case "warning" -> { bg="rgba(255,171,0,0.08)";   border="rgba(255,171,0,0.40)";   color="#ffb700"; }
            case "danger"  -> { bg="rgba(255,60,100,0.08)";  border="rgba(255,60,100,0.40)";  color="#ff6b7a"; }
            default        -> { bg="rgba(255,255,255,0.05)"; border="rgba(255,255,255,0.15)"; color="white"; }
        }
        b.setStyle(
                "-fx-background-color:"+bg+";-fx-border-color:"+border+";" +
                        "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;" +
                        "-fx-text-fill:"+color+";-fx-font-size:13;-fx-padding:5 10;-fx-cursor:hand;-fx-font-weight:bold;");
        b.setOnMouseEntered(e -> b.setOpacity(0.8));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<User> users = userService.getAllUsers();
                Platform.runLater(() -> { allUsers.setAll(users); applyFilters(); refreshKPICards(users); });
            } catch (Exception e) {
                System.err.println("[AdminUserController] loadAllUsers: " + e.getMessage());
            }
        }, "LoadUsers").start();
    }

    private void refreshKPICards(List<User> users) {
        int total    = users.size();
        int active   = (int) users.stream().filter(u -> isActiveStatus(u.getAccountStatus())).count();
        int coaches  = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_COACH")).count();
        int admins   = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_ADMIN")).count();
        int inactive = total - active;
        double rate  = total > 0 ? (active * 100.0 / total) : 0;
        animateCountUp(totalUsersLabel,    total,      false, "purple");
        animateCountUp(activeUsersLabel,   active,     false, "green");
        animateCountUp(coachesLabel,       coaches,    false, "pink");
        animateCountUp(adminsLabel,        admins,     false, "blue");
        animateCountUp(inactiveUsersLabel, inactive,   false, null);
        animateCountUp(activeRateLabel,    (int) rate, true,  null);
        setProgress(progressTotal,   1.0);
        setProgress(progressActive,  total > 0 ? (double) active  / total : 0);
        setProgress(progressCoaches, total > 0 ? (double) coaches / total : 0);
        setProgress(progressAdmins,  total > 0 ? (double) admins  / total : 0);
        applyProgressStyle(progressTotal,   "progress-purple");
        applyProgressStyle(progressActive,  "progress-green");
        applyProgressStyle(progressCoaches, "progress-pink");
        applyProgressStyle(progressAdmins,  "progress-blue");
    }

    private void animateCountUp(Label label, int target, boolean isPercent, String color) {
        if (label == null) return;
        int steps = 60;
        double stepDuration = 1200.0 / steps;
        final int[] current = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(stepDuration), e -> {
            current[0]++;
            int val = (int) Math.round(target * current[0] / (double) steps);
            if (current[0] >= steps) val = target;
            label.setText(isPercent ? val + "%" : String.valueOf(val));
        }));
        tl.setCycleCount(steps);
        tl.play();
        label.setStyle("-fx-font-size:44px;-fx-font-weight:bold;-fx-text-fill:white;");
    }

    @FXML public void handleFilter() { currentPage = 1; applyFilters(); }
    @FXML public void handleClearFilters() {
        if (searchField       != null) searchField.clear();
        if (roleFilterCombo   != null) roleFilterCombo.setValue("All Roles");
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Statuses");
        currentPage = 1; applyFilters();
    }

    private void applyFilters() {
        String search = searchField       != null ? searchField.getText().toLowerCase().trim() : "";
        String role   = roleFilterCombo   != null ? roleFilterCombo.getValue()   : "All Roles";
        String status = statusFilterCombo != null ? statusFilterCombo.getValue() : "All Statuses";

        List<User> filtered = allUsers.stream().filter(u -> {
            if (!search.isBlank() && !(contains(u.getFullName(),search)||contains(u.getUsername(),search)||contains(u.getEmail(),search))) return false;
            if (role   != null && !role.equals("All Roles")     && !hasRoleStr(u, role)) return false;
            if (status != null && !status.equals("All Statuses")) {
                String s = u.getAccountStatus() != null ? u.getAccountStatus() : "active";
                if (!s.equalsIgnoreCase(status)) return false;
            }
            return true;
        }).toList();

        totalPages  = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        if (usersTable != null) {
            usersTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));
            Platform.runLater(() -> Platform.runLater(() ->
                    usersTable.lookupAll(".column-header .label").forEach(node -> node.setStyle(
                            "-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;-fx-font-size:11px;" +
                                    "-fx-background-color:transparent;-fx-padding:0 16;"))));
        }

        setLabel(resultCountLabel,    "Found " + filtered.size() + " user" + (filtered.size() != 1 ? "s" : ""));
        setLabel(pageNumberLabel,     "Page " + currentPage + " / " + totalPages);
        setLabel(paginationInfoLabel, filtered.isEmpty() ? "" : "Showing " + (from+1) + " – " + to + " of " + filtered.size() + " entries");
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML public void handlePrevPage() { if (currentPage > 1)         { currentPage--; applyFilters(); } }
    @FXML public void handleNextPage() { if (currentPage < totalPages) { currentPage++; applyFilters(); } }

    private void openDetail(User user) { SessionManager.setSelectedUser(user); navigateTo("AdminUserDetail.fxml"); }

    private void handleSuspendToggle(User user) {
        if (!canModify(user)) { alert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to modify this account."); return; }
        boolean isActive = isActiveStatus(user.getAccountStatus());
        String msg = isActive ? "Suspend user \"" + user.getUsername() + "\"?" : "Reactivate user \"" + user.getUsername() + "\"?";
        if (!confirm("Confirm", msg)) return;
        new Thread(() -> {
            try { if (isActive) userService.suspendUser(user.getId()); else userService.activateUser(user.getId()); Platform.runLater(this::loadAllUsers); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage())); }
        }).start();
    }

    private void handleDelete(User user) {
        if (!canModify(user)) { alert(Alert.AlertType.WARNING, "Permission Denied", "Only a Super Administrator can delete an Admin account."); return; }
        if (!confirm("Delete User", "Permanently delete \"" + user.getUsername() + "\"?\nThis action cannot be undone!")) return;
        new Thread(() -> {
            try { userService.deleteUser(user.getId()); Platform.runLater(this::loadAllUsers); }
            catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage())); }
        }).start();
    }

    @FXML public void handleNewUser() { openCreateUserModal(); loadAllUsers(); }

    // ═══════════════════════════════════════════════════════════
    //  CREATE MODAL — NO ScrollPane, uses Pane+clip instead
    // ═══════════════════════════════════════════════════════════
    private void openCreateUserModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(resolveStage());
        modal.setTitle("Create User");
        modal.setResizable(false);

        // ── Instantiate controls ──────────────────────────────
        createFullNameField     = new TextField();
        createUsernameField     = new TextField();
        createEmailField        = new TextField();
        createPasswordField     = new PasswordField();
        createPasswordVisible   = new TextField();
        createTogglePasswordBtn = new Button("👁");
        createRoleCombo         = new ComboBox<>();
        createSubmitBtn         = new Button("✓  Create User");
        createCancelBtn         = new Button("Cancel");
        createPasswordStrength  = new ProgressBar(0);
        createStrengthLabel     = new Label("");
        createRoleDescBox       = new VBox(0);
        createRoleTitleLabel    = new Label("");
        createRoleTextLabel     = new Label("");
        errFullName = errLabel(); errUsername = errLabel();
        errEmail    = errLabel(); errPassword = errLabel();
        errRole     = errLabel();

        // ── Style fields ──────────────────────────────────────
        createFullNameField.setPromptText("e.g. John Smith");
        createFullNameField.setPrefWidth(270);
        createFullNameField.setStyle(FIELD_STYLE);

        createUsernameField.setPromptText("e.g. johnsmith");
        createUsernameField.setStyle(FIELD_TRANSPARENT);
        HBox.setHgrow(createUsernameField, Priority.ALWAYS);

        createEmailField.setPromptText("e.g. john@example.com");
        createEmailField.setPrefWidth(270);
        createEmailField.setStyle(FIELD_STYLE);

        createPasswordField.setPromptText("Min 6 characters");
        createPasswordField.setStyle(FIELD_TRANSPARENT);
        HBox.setHgrow(createPasswordField, Priority.ALWAYS);

        createPasswordVisible.setPromptText("Min 6 characters");
        createPasswordVisible.setStyle(FIELD_TRANSPARENT);
        createPasswordVisible.setVisible(false);
        createPasswordVisible.setManaged(false);
        HBox.setHgrow(createPasswordVisible, Priority.ALWAYS);

        createTogglePasswordBtn.setStyle(
                "-fx-background-color:transparent;-fx-border-color:transparent;" +
                        "-fx-text-fill:" + RED_PRIMARY + ";-fx-cursor:hand;-fx-font-size:14;-fx-padding:6 10;");
        createTogglePasswordBtn.setOnAction(e -> handleTogglePassword());

        createPasswordStrength.setPrefWidth(270);
        createPasswordStrength.setPrefHeight(4);
        applyProgressStyle(createPasswordStrength, "progress-red");
        createStrengthLabel.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.40);");

        // ── @ prefix ─────────────────────────────────────────
        Label atLabel = new Label("@");
        atLabel.setStyle(
                "-fx-background-color:" + RED_AT_BG + ";" +
                        "-fx-background-radius:9 0 0 9;-fx-text-fill:#ff6b7a;" +
                        "-fx-font-weight:bold;-fx-font-size:13;-fx-padding:10 12;" +
                        "-fx-border-color:" + RED_BORDER + ";-fx-border-width:0 1 0 0;");

        HBox usernameBox = new HBox(0, atLabel, createUsernameField);
        usernameBox.setPrefWidth(270);
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        usernameBox.setStyle(
                "-fx-background-color:" + BG_FIELD + ";" +
                        "-fx-border-color:" + RED_BORDER + ";-fx-border-radius:9;-fx-background-radius:9;");

        // ── Password box ──────────────────────────────────────
        HBox passwordBox = new HBox(0, createPasswordField, createPasswordVisible, createTogglePasswordBtn);
        passwordBox.setPrefWidth(270);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setStyle(
                "-fx-background-color:" + BG_FIELD + ";" +
                        "-fx-border-color:" + RED_BORDER + ";-fx-border-radius:9;-fx-background-radius:9;");

        // ── Welcome notice ────────────────────────────────────
        Label welcomeTitle = new Label("Welcome Email");
        welcomeTitle.setStyle("-fx-text-fill:" + RED_SOFT + ";-fx-font-weight:bold;-fx-font-size:13;");
        Label welcomeText = new Label("A welcome email will automatically be sent to the new user.");
        welcomeText.setStyle("-fx-text-fill:rgba(255,255,255,0.50);-fx-font-size:12;");
        welcomeText.setWrapText(true);
        Region accentBar2 = new Region();
        accentBar2.setPrefWidth(3); accentBar2.setMinWidth(3);
        accentBar2.setPrefHeight(36); accentBar2.setMinHeight(36);
        accentBar2.setStyle("-fx-background-color:" + RED_PRIMARY + ";-fx-background-radius:2;");
        VBox welcomeTextBox = new VBox(3, welcomeTitle, welcomeText);
        welcomeTextBox.setStyle("-fx-background-color:transparent;-fx-padding:0 0 0 12;");
        HBox notice = new HBox(0, accentBar2, welcomeTextBox);
        notice.setAlignment(Pos.CENTER_LEFT);
        notice.setStyle(NOTICE_BOX_STYLE);

        // ── Role combo ────────────────────────────────────────
        createRoleCombo.setPrefWidth(576);
        createRoleCombo.setStyle(
                "-fx-background-color:" + BG_FIELD + ";" +
                        "-fx-border-color:" + RED_BORDER + ";-fx-border-radius:9;-fx-background-radius:9;");

        // ── Buttons ───────────────────────────────────────────
        createSubmitBtn.setStyle(BTN_CREATE);
        createSubmitBtn.setOnAction(e -> handleCreateSubmit());
        createCancelBtn.setStyle(BTN_CANCEL);
        createCancelBtn.setOnAction(e -> modal.close());

        // ── Role description box ──────────────────────────────
        createRoleDescBox.setVisible(false);
        createRoleDescBox.setManaged(false);
        createRoleDescBox.setMaxWidth(Double.MAX_VALUE);

        // ── Layout columns ────────────────────────────────────
        VBox colFN = new VBox(6, fieldLabel("Full Name"), createFullNameField, errFullName, hint("Enter the user's full name"));
        colFN.setPrefWidth(280);
        colFN.setStyle("-fx-background-color:transparent;");

        VBox colUN = new VBox(6, fieldLabel("Username"), usernameBox, errUsername, hint("3-50 chars: letters, digits, - and _"));
        colUN.setPrefWidth(280);
        colUN.setStyle("-fx-background-color:transparent;");

        VBox colEM = new VBox(6, fieldLabel("Email Address"), createEmailField, errEmail, hint("Must be unique in the system"));
        colEM.setPrefWidth(280);
        colEM.setStyle("-fx-background-color:transparent;");

        VBox colPW = new VBox(6, fieldLabel("Password"), passwordBox, createPasswordStrength,
                new HBox(createStrengthLabel), errPassword, hint("Min 8 chars · uppercase · lowercase · number · special char"));
        colPW.setPrefWidth(280);
        colPW.setStyle("-fx-background-color:transparent;");

        HBox row1 = new HBox(16, colFN, colUN);
        row1.setStyle("-fx-background-color:transparent;");

        HBox row2 = new HBox(16, colEM, colPW);
        row2.setStyle("-fx-background-color:transparent;");

        VBox roleSection = new VBox(8, fieldLabel("User Role"), createRoleCombo, errRole,
                hint("Defines the user's permissions in the system"), createRoleDescBox);
        roleSection.setStyle("-fx-background-color:transparent;");

        // ── Form body ─────────────────────────────────────────
        VBox formBody = new VBox(22, row1, row2, roleSection, notice);
        formBody.setPadding(new Insets(26));
        formBody.setMaxWidth(Double.MAX_VALUE);
        // CLEF : setBackground() Java API — ignore totalement le CSS
        formBody.setBackground(new Background(new BackgroundFill(
                Color.web(BG_DARK), CornerRadii.EMPTY, Insets.EMPTY)));

        // ── SOLUTION FINALE : Pane + clip — ZERO ScrollPane ───
        double viewHeight = 520;

        // Pane conteneur avec clip — pas de ScrollPane du tout
        Pane clipPane = new Pane();
        clipPane.setMinHeight(viewHeight);
        clipPane.setMaxHeight(viewHeight);
        clipPane.setPrefHeight(viewHeight);
        clipPane.setBackground(new Background(new BackgroundFill(
                Color.web(BG_DARK), CornerRadii.EMPTY, Insets.EMPTY)));

        // Clip rectangle
        Rectangle clipRect = new Rectangle(620, viewHeight);
        clipPane.setClip(clipRect);
        clipPane.getChildren().add(formBody);

        // formBody doit s'étirer sur toute la largeur du clipPane
        formBody.prefWidthProperty().bind(clipPane.widthProperty());

        // ScrollBar verticale
        ScrollBar vBar = new ScrollBar();
        vBar.setOrientation(Orientation.VERTICAL);
        vBar.setPrefWidth(8);
        vBar.setMin(0);
        vBar.setValue(0);
        vBar.setStyle(
                "-fx-background-color:rgba(255,255,255,0.05);" +
                        "-fx-border-color:transparent;");

        // Conteneur final = clipPane + scrollbar
        HBox scrollContainer = new HBox(0, clipPane, vBar);
        scrollContainer.setMaxHeight(viewHeight);
        scrollContainer.setMinHeight(viewHeight);
        scrollContainer.setBackground(new Background(new BackgroundFill(
                Color.web(BG_DARK), CornerRadii.EMPTY, Insets.EMPTY)));
        HBox.setHgrow(clipPane, Priority.ALWAYS);

        // Liaison scroll : mise à jour quand formBody change de hauteur
        formBody.heightProperty().addListener((obs, oldH, newH) -> {
            double contentH = newH.doubleValue();
            if (contentH <= viewHeight) {
                vBar.setVisible(false);
                vBar.setManaged(false);
                formBody.setTranslateY(0);
            } else {
                vBar.setVisible(true);
                vBar.setManaged(true);
                vBar.setMax(contentH - viewHeight);
                vBar.setVisibleAmount(viewHeight * (contentH - viewHeight) / contentH);
                // Clip mis à jour aussi
                clipRect.setWidth(clipPane.getWidth() > 0 ? clipPane.getWidth() : 620);
                clipRect.setHeight(viewHeight);
            }
        });

        // Clip width suit la largeur du pane
        clipPane.widthProperty().addListener((obs, o, n) ->
                clipRect.setWidth(n.doubleValue()));

        // Scroll via la scrollbar
        vBar.valueProperty().addListener((obs, oldV, newV) ->
                formBody.setTranslateY(-newV.doubleValue()));

        // Scroll via la molette de la souris
        scrollContainer.setOnScroll(ev -> {
            if (!vBar.isVisible()) return;
            double delta = -ev.getDeltaY();
            double newVal = Math.max(vBar.getMin(),
                    Math.min(vBar.getMax(), vBar.getValue() + delta));
            vBar.setValue(newVal);
        });

        // ── Header ────────────────────────────────────────────
        Region topLine = new Region();
        topLine.setPrefHeight(3); topLine.setMaxHeight(3);
        topLine.setStyle("-fx-background-color:" + RED_PRIMARY + ";");

        Label iconEmoji = new Label("👤");
        iconEmoji.setStyle("-fx-font-size:20;-fx-text-fill:white;");
        StackPane iconPane = new StackPane(iconEmoji);
        iconPane.setStyle(
                "-fx-background-color:rgba(255,60,100,0.18);-fx-background-radius:12;" +
                        "-fx-min-width:48;-fx-min-height:48;-fx-max-width:48;-fx-max-height:48;" +
                        "-fx-border-color:rgba(255,60,100,0.40);-fx-border-radius:12;-fx-border-width:1;");

        Label headerTitle = new Label("Create New User");
        headerTitle.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:white;");
        Label headerSub = new Label("Fill in the details to add a new member to the platform.");
        headerSub.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.50);");

        StackPane redDot = new StackPane();
        redDot.setStyle(
                "-fx-background-color:" + RED_PRIMARY + ";-fx-background-radius:50;" +
                        "-fx-min-width:8;-fx-min-height:8;-fx-max-width:8;-fx-max-height:8;" +
                        "-fx-effect:dropshadow(gaussian," + RED_PRIMARY + ",8,0.6,0,0);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(14, iconPane, new VBox(4, headerTitle, headerSub), spacer, redDot);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle(
                "-fx-background-color:" + BG_HEADER + ";-fx-padding:20 24;" +
                        "-fx-border-color:rgba(255,60,100,0.25);-fx-border-width:0 0 1 0;");

        VBox header = new VBox(0, topLine, headerRow);
        header.setStyle("-fx-background-color:" + BG_HEADER + ";");

        // ── Footer ────────────────────────────────────────────
        HBox footer = new HBox(10, createSubmitBtn, createCancelBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-padding:16 24;-fx-background-color:" + BG_DARK + ";" +
                        "-fx-border-color:rgba(255,60,100,0.15);-fx-border-width:1 0 0 0;");

        // ── Root ──────────────────────────────────────────────
        VBox root = new VBox(0, header, scrollContainer, footer);
        root.setBackground(new Background(new BackgroundFill(
                Color.web(BG_DARK), CornerRadii.EMPTY, Insets.EMPTY)));

        // ── Scene ─────────────────────────────────────────────
        Scene scene = new Scene(root, 640, Region.USE_COMPUTED_SIZE);
        scene.setFill(Color.web(BG_DARK));

        URL cssUrl = getClass().getResource("/com/eyetwin/styles/admin.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        modal.setScene(scene);
        initCreateView();
        modal.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    //  Layout helpers
    // ─────────────────────────────────────────────────────────────
    private HBox fieldLabel(String text) {
        Label lbl  = new Label(text); lbl.setStyle(LABEL_FIELD);
        Label star = new Label("*"); star.setStyle(LABEL_STAR);
        HBox row = new HBox(5, lbl, star);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label hint(String text) {
        Label l = new Label(text); l.setStyle(LABEL_HINT); return l;
    }

    // ═══════════════════════════════════════════════════════════
    //  DETAIL VIEW
    // ═══════════════════════════════════════════════════════════
    private void initDetailView() {
        if (adminTopbarController != null) adminTopbarController.setTitle("User Details");
        User user = SessionManager.getSelectedUser();
        if (user == null) { navigateTo("AdminUsers.fxml"); return; }
        populateDetail(user);
    }

    private void populateDetail(User user) {
        User me = SessionManager.getCurrentUser();
        boolean isMe      = me != null && me.getId() == user.getId();
        boolean isAdmin   = hasRoleStr(user, "ROLE_ADMIN");
        boolean canModify = !isMe && (!isAdmin || SessionManager.isSuperAdmin());
        String initials = !user.getUsername().isEmpty()
                ? user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase() : "??";
        setLabel(avatarInitialLabel,  initials);
        loadDetailAvatar(user);
        setLabel(fullNameHeaderLabel, nvl(user.getFullName(), user.getUsername()));
        setLabel(usernameHeaderLabel, "@" + user.getUsername());
        setLabel(roleChipLabel,       getRoleLabel(user));
        String accStatus = user.getAccountStatus() != null ? user.getAccountStatus() : "active";
        setLabel(statusChipLabel, accStatus.toUpperCase());
        showNode(youBadgeLabel, isMe);
        boolean hasBio = user.getBio() != null && !user.getBio().isBlank();
        showNode(bioBox, hasBio); if (hasBio) setLabel(bioLabel, user.getBio());
        showNode(limitedPermBanner, !canModify && !isMe); showNode(selfViewBanner, isMe);
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList("ROLE_USER","ROLE_COACH","ROLE_ADMIN"));
            roleComboBox.setValue(getPrimaryRole(user)); roleComboBox.setDisable(!canModify);
        }
        if (updateRoleBtn != null) updateRoleBtn.setDisable(!canModify);
        showNode(rolePermNote, !canModify);
        setLabel(rolePermNote, isMe ? "Cannot modify your own role" : "Only Super Admins can modify an Admin");
        boolean notActive = !isActiveStatus(accStatus);
        showNode(statusAlertBox, notActive);
        if (notActive) setLabel(statusAlertLabel, "Account " + accStatus.toUpperCase() + ": " + getStatusDescription(accStatus));
        boolean activeStatus = isActiveStatus(accStatus);
        showNode(suspendBtn, activeStatus); showNode(banBtn, activeStatus); showNode(reactivateBtn, !activeStatus);
        if (suspendBtn    != null) suspendBtn.setDisable(!canModify);
        if (banBtn        != null) banBtn.setDisable(!canModify);
        if (reactivateBtn != null) reactivateBtn.setDisable(!canModify);
        if (deleteBtn     != null) deleteBtn.setDisable(!canModify);
        showNode(actionPermNote, !canModify);
        setLabel(actionPermNote, isMe ? "Self-modification not allowed" : "Super Admin permission required");
        setLabel(emailInfoLabel,     user.getEmail());
        setLabel(usernameInfoLabel,  "@" + user.getUsername());
        setLabel(fullNameInfoLabel,  nvl(user.getFullName(), "Not provided"));
        setLabel(registeredLabel,    user.getCreatedAt() != null ? user.getCreatedAt().toString() : "—");
        setLabel(registeredAgoLabel, "");
        setLabel(lastLoginLabel,     user.getLastLogin() != null ? user.getLastLogin().toString() : "Never");
        setLabel(statusInfoLabel,    accStatus.toUpperCase());
        List<TeamMembership> memberships = loadTeamMemberships(user.getId());
        int teamCount = memberships != null ? memberships.size() : 0;
        setLabel(teamsBadge, String.valueOf(teamCount));
        showNode(teamsEmptyState, teamCount == 0);
        if (teamsTable != null) {
            if (teamCount > 0) {
                setupTeamsTable();
                teamsTable.setItems(FXCollections.observableArrayList(memberships));
                applyTeamsTableTheme();
            }
            showNode(teamsTable, teamCount > 0);
        }
    }

    private List<TeamMembership> loadTeamMemberships(int userId) {
        try { return userService.getTeamMemberships(userId); } catch (Exception e) { return List.of(); }
    }

    // ── Setup cellules du tableau Teams ──────────────────────────
    private void setupTeamsTable() {
        if (teamsTable == null) return;

        // ── Colonne Team Name ─────────────────────────────────────
        if (colTeamName != null) {
            colTeamName.setCellValueFactory(d -> {
                TeamMembership m = d.getValue();
                // getTeam() est maintenant toujours non-null (JOIN dans le service)
                String name = (m.getTeam() != null && m.getTeam().getName() != null)
                        ? m.getTeam().getName()
                        : "Team #" + m.getTeamId();
                return new SimpleStringProperty(name);
            });
            colTeamName.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    Label icon = new Label("👥");
                    icon.setStyle("-fx-font-size:13;");
                    Label lbl = new Label(item);
                    lbl.setStyle(
                            "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;");
                    HBox box = new HBox(8, icon, lbl);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        // ── Colonne Role ──────────────────────────────────────────
        if (colTeamRole != null) {
            colTeamRole.setCellValueFactory(d -> {
                MemberRole r = d.getValue().getRole();
                return new SimpleStringProperty(r != null ? r.name() : "MEMBER");
            });
            colTeamRole.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    boolean isOwner  = "OWNER".equals(item) || "LEADER".equals(item);
                    boolean isCaptain= "CAPTAIN".equals(item);
                    String bg, border, color, prefix;
                    if (isOwner) {
                        bg="#fbbf2420"; border="rgba(251,191,36,0.50)";
                        color="#fbbf24"; prefix="👑 ";
                    } else if (isCaptain) {
                        bg="rgba(245,166,35,0.15)"; border="rgba(245,166,35,0.45)";
                        color="#f5a623"; prefix="⭐ ";
                    } else {
                        bg="rgba(79,172,254,0.15)"; border="rgba(79,172,254,0.45)";
                        color="#4facfe"; prefix="";
                    }
                    Label badge = new Label(prefix + item);
                    badge.setStyle(
                            "-fx-background-color:" + bg + ";"
                                    + "-fx-border-color:" + border + ";"
                                    + "-fx-border-width:1;-fx-background-radius:8;"
                                    + "-fx-border-radius:8;-fx-text-fill:" + color + ";"
                                    + "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:4 10;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // ── Colonne Status ────────────────────────────────────────
        if (colTeamStatus != null) {
            colTeamStatus.setCellValueFactory(d -> {
                MembershipStatus s = d.getValue().getStatus();
                return new SimpleStringProperty(s != null ? s.name() : "UNKNOWN");
            });
            colTeamStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    String bg, border, color;
                    switch (item) {
                        case "ACTIVE"   -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.45)";  color="#43e97b"; }
                        case "INVITED"  -> { bg="rgba(79,172,254,0.15)";  border="rgba(79,172,254,0.45)";  color="#4facfe"; }
                        case "PENDING"  -> { bg="rgba(255,193,7,0.15)";   border="rgba(255,193,7,0.45)";   color="#ffd54f"; }
                        case "INACTIVE",
                             "LEFT"     -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.45)";  color="#ff6b7a"; }
                        default         -> { bg="rgba(255,255,255,0.08)"; border="rgba(255,255,255,0.20)"; color="rgba(255,255,255,0.60)"; }
                    }
                    Label badge = new Label(item);
                    badge.setStyle(
                            "-fx-background-color:" + bg + ";"
                                    + "-fx-border-color:" + border + ";"
                                    + "-fx-border-width:1;-fx-background-radius:8;"
                                    + "-fx-border-radius:8;-fx-text-fill:" + color + ";"
                                    + "-fx-font-size:11;-fx-font-weight:bold;-fx-padding:4 10;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        // ── Colonne Joined ────────────────────────────────────────
        if (colTeamJoined != null) {
            colTeamJoined.setCellValueFactory(d -> {
                java.time.LocalDateTime dt = d.getValue().getJoinedAt();
                return new SimpleStringProperty(
                        dt != null ? dt.toString().substring(0, 10) : "Pending");
            });
            colTeamJoined.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle(
                            "-fx-text-fill:" + ("Pending".equals(item)
                                    ? "rgba(255,193,7,0.70)" : "rgba(255,255,255,0.55)") + ";"
                                    + "-fx-font-size:12;-fx-padding:0 16;");
                }
            });
        }
    }
    // ── Applique le thème sombre gaming au tableau Teams ──────────
    private void applyTeamsTableTheme() {
        if (teamsTable == null) return;

        teamsTable.setStyle(
                "-fx-background-color:transparent;"
                        + "-fx-border-color:transparent;"
                        + "-fx-table-cell-border-color:transparent;"
                        + "-fx-control-inner-background:rgba(20,10,35,0.80);"
                        + "-fx-control-inner-background-alt:rgba(30,15,45,0.60);");

        // Row factory sombre
        teamsTable.setRowFactory(tv -> {
            TableRow<TeamMembership> row = new TableRow<>() {
                @Override protected void updateItem(TeamMembership m, boolean empty) {
                    super.updateItem(m, empty);
                    if (empty || m == null) {
                        setStyle("-fx-background-color:transparent;"
                                + "-fx-border-color:transparent;");
                    } else {
                        String bg = (getIndex() % 2 == 0)
                                ? "rgba(20,10,35,0.85)" : "rgba(30,15,45,0.70)";
                        setStyle("-fx-background-color:" + bg + ";"
                                + "-fx-background-radius:8;"
                                + "-fx-border-color:rgba(255,255,255,0.05);"
                                + "-fx-border-width:1;-fx-border-radius:8;");
                    }
                }
            };
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle(
                        "-fx-background-color:rgba(255,255,255,0.07);"
                                + "-fx-background-radius:8;"
                                + "-fx-border-color:rgba(102,126,234,0.35);"
                                + "-fx-border-width:1;-fx-border-radius:8;-fx-cursor:hand;");
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    String bg = (row.getIndex() % 2 == 0)
                            ? "rgba(20,10,35,0.85)" : "rgba(30,15,45,0.70)";
                    row.setStyle("-fx-background-color:" + bg + ";"
                            + "-fx-background-radius:8;"
                            + "-fx-border-color:rgba(255,255,255,0.05);"
                            + "-fx-border-width:1;-fx-border-radius:8;");
                }
            });
            return row;
        });

        // Headers sombres — double runLater pour attendre le layout JavaFX
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Node hBg = teamsTable.lookup(".column-header-background");
            if (hBg != null)
                hBg.setStyle("-fx-background-color:rgba(8,4,16,0.98);-fx-padding:0;");

            javafx.scene.Node filler =
                    teamsTable.lookup(".column-header-background .filler");
            if (filler != null)
                filler.setStyle("-fx-background-color:rgba(8,4,16,0.98);");

            teamsTable.lookupAll(".column-header").forEach(n -> n.setStyle(
                    "-fx-background-color:rgba(8,4,16,0.98);"
                            + "-fx-border-color:transparent transparent "
                            + "rgba(102,126,234,0.35) transparent;"
                            + "-fx-border-width:0 0 1 0;-fx-size:44px;"));

            teamsTable.lookupAll(".column-header .label").forEach(n -> n.setStyle(
                    "-fx-text-fill:rgba(255,255,255,0.90);"
                            + "-fx-font-weight:bold;-fx-font-size:11px;"
                            + "-fx-background-color:transparent;"
                            + "-fx-alignment:CENTER_LEFT;-fx-padding:0 16;"));
        }));
    }

    @FXML public void handleUpdateRole() {
        if (roleComboBox == null) return;
        User user = SessionManager.getSelectedUser(); if (user == null) return;
        String newRole = roleComboBox.getValue();
        if ("ROLE_ADMIN".equals(newRole) && !SessionManager.isSuperAdmin()) {
            alert(Alert.AlertType.WARNING, "Permission Denied", "Only Super Administrators can assign the Admin role."); return;
        }
        new Thread(() -> {
            try {
                userService.updateUserRole(user.getId(), newRole);
                Platform.runLater(() -> {
                    alert(Alert.AlertType.INFORMATION, "Success", "Role updated successfully.");
                    User r = userService.findById(user.getId()); SessionManager.setSelectedUser(r); populateDetail(r);
                });
            } catch (Exception e) { Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Error", e.getMessage())); }
        }).start();
    }

    @FXML public void handleSuspend() {
        User user = SessionManager.getSelectedUser(); if (user==null||!canModify(user)) return;
        if (!confirm("Suspend","Suspend user \""+user.getUsername()+"\"?")) return;
        new Thread(() -> { try { userService.suspendUser(user.getId()); Platform.runLater(()->refreshDetail(user.getId())); } catch (Exception e) { Platform.runLater(()->alert(Alert.AlertType.ERROR,"Error",e.getMessage())); } }).start();
    }
    @FXML public void handleBan() {
        User user = SessionManager.getSelectedUser(); if (user==null||!canModify(user)) return;
        if (!confirm("Ban","Permanently ban \""+user.getUsername()+"\"? This is a serious action.")) return;
        new Thread(() -> { try { userService.banUser(user.getId()); Platform.runLater(()->refreshDetail(user.getId())); } catch (Exception e) { Platform.runLater(()->alert(Alert.AlertType.ERROR,"Error",e.getMessage())); } }).start();
    }
    @FXML public void handleReactivate() {
        User user = SessionManager.getSelectedUser(); if (user==null||!canModify(user)) return;
        if (!confirm("Reactivate","Reactivate account for \""+user.getUsername()+"\"?")) return;
        new Thread(() -> { try { userService.activateUser(user.getId()); Platform.runLater(()->refreshDetail(user.getId())); } catch (Exception e) { Platform.runLater(()->alert(Alert.AlertType.ERROR,"Error",e.getMessage())); } }).start();
    }
    @FXML public void handleDeleteUser() {
        User user = SessionManager.getSelectedUser(); if (user==null||!canModify(user)) return;
        if (!confirm("Delete","Permanently delete \""+user.getUsername()+"\"?\nThis cannot be undone!")) return;
        new Thread(() -> { try { userService.deleteUser(user.getId()); Platform.runLater(()->navigateTo("AdminUsers.fxml")); } catch (Exception e) { Platform.runLater(()->alert(Alert.AlertType.ERROR,"Error",e.getMessage())); } }).start();
    }
    @FXML public void goBackToList() { navigateTo("AdminUsers.fxml"); }
    private void refreshDetail(int userId) { User r = userService.findById(userId); SessionManager.setSelectedUser(r); populateDetail(r); }

    // ═══════════════════════════════════════════════════════════
    //  CREATE VIEW LOGIC
    // ═══════════════════════════════════════════════════════════
    private void initCreateView() { setupCreateRoleCombo(); setupPasswordStrength(); setupCreateValidation(); }

    private void setupCreateRoleCombo() {
        if (createRoleCombo == null) return;

        // Construire la liste selon le rang de l'admin connecté
        java.util.List<String> roles = new java.util.ArrayList<>();
        roles.add("ROLE_USER");
        roles.add("ROLE_COACH");
        roles.add("ROLE_ADMIN");
        if (SessionManager.isSuperAdmin()) {
            roles.add("ROLE_SUPER_ADMIN");
        }
        createRoleCombo.setItems(FXCollections.observableArrayList(roles));
        createRoleCombo.setValue("ROLE_USER");

        createRoleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(roleDisplayName(item));
                    // Admin → Super Admin only. Super Admin → Super Admin only
                    boolean restricted = ("ROLE_ADMIN".equals(item) || "ROLE_SUPER_ADMIN".equals(item))
                            && !SessionManager.isSuperAdmin();
                    setDisable(restricted);
                    setOpacity(restricted ? 0.4 : 1.0);
                } else {
                    setText(null);
                }
                setStyle(
                        "-fx-background-color:" + BG_FIELD + ";" +
                                "-fx-text-fill:rgba(255,255,255,0.85);" +
                                "-fx-padding:8 14;"
                );
            }
        });

        createRoleCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) setText(roleDisplayName(item));
                else setText(null);
                setStyle(
                        "-fx-background-color:" + BG_FIELD + ";" +
                                "-fx-text-fill:rgba(255,255,255,0.85);"
                );
            }
        });

        createRoleCombo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) return;
            Platform.runLater(() -> {
                for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                    if (!(w instanceof javafx.stage.PopupWindow) || !w.isShowing()) continue;
                    javafx.scene.Node listView = w.getScene().getRoot().lookup(".list-view");
                    if (listView != null) {
                        listView.setStyle(
                                "-fx-background-color:" + BG_FIELD + ";" +
                                        "-fx-control-inner-background:" + BG_FIELD + ";" +
                                        "-fx-border-color:" + RED_BORDER + ";" +
                                        "-fx-border-radius:10;-fx-background-radius:10;" +
                                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),20,0,0,4);"
                        );
                    }
                    applyDarkCellsToPopup(w.getScene().getRoot());
                }
            });
        });

        createRoleCombo.setOnAction(e -> updateRoleDesc(createRoleCombo.getValue()));
        updateRoleDesc("ROLE_USER");
    }

    /** Applique fond sombre + hover rouge sur toutes les ListCell du popup */
    private void applyDarkCellsToPopup(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof ListCell) {
                ListCell<?> cell = (ListCell<?>) node;
                cell.setStyle(
                        "-fx-background-color:" + BG_FIELD + ";" +
                                "-fx-text-fill:rgba(255,255,255,0.85);" +
                                "-fx-padding:8 14;"
                );
                cell.setOnMouseEntered(e -> cell.setStyle(
                        "-fx-background-color:" + RED_AT_BG + ";" +
                                "-fx-text-fill:white;" +
                                "-fx-padding:8 14;"
                ));
                cell.setOnMouseExited(e -> cell.setStyle(
                        "-fx-background-color:" + BG_FIELD + ";" +
                                "-fx-text-fill:rgba(255,255,255,0.85);" +
                                "-fx-padding:8 14;"
                ));
            } else if (node instanceof javafx.scene.Parent) {
                applyDarkCellsToPopup((javafx.scene.Parent) node);
            }
        }
    }
    private void updateRoleDesc(String role) {
        if (createRoleDescBox == null) return;

        String emoji, title, text, accentColor;
        switch (nvl(role, "")) {
            case "ROLE_COACH" -> {
                emoji       = "⚡";
                title       = "Coach";
                text        = "All User permissions + can host live streams and earn revenue from user subscriptions and tips.";
                accentColor = "#f5a623";
            }
            case "ROLE_ADMIN" -> {
                emoji       = "🛡";
                title       = "Administrator";
                text        = "Full platform access: manage all users, teams, content, tournaments and system settings.";
                accentColor = "#ff3c64";
            }
            case "ROLE_SUPER_ADMIN" -> {
                emoji       = "👑";
                title       = "Super Administrator";
                text        = "Unrestricted access: everything Admins can do + manage admin accounts, audit logs and platform-wide configuration. Only Super Admins can create or promote other Super Admins.";
                accentColor = "#a78bfa";
            }
            default -> {
                emoji       = "🎮";
                title       = "User";
                text        = "Standard access: join teams, participate in tournaments and activities, manage personal profile and interact with coaches.";
                accentColor = "#4facfe";
            }
        }

        createRoleDescBox.getChildren().clear();

        // Accent bar (couleur selon le rôle)
        Region bar = new Region();
        bar.setPrefWidth(3); bar.setMinWidth(3);
        bar.setPrefHeight(44); bar.setMinHeight(44);
        bar.setBackground(new Background(new BackgroundFill(
                Color.web(accentColor), new CornerRadii(2), Insets.EMPTY)));

        // Emoji badge
        Label emojiBadge = new Label(emoji);
        emojiBadge.setStyle(
                "-fx-font-size:15;" +
                        "-fx-background-color:" + accentColor.replace("#", "rgba(") + ",0.15);" +
                        "-fx-background-radius:8;" +
                        "-fx-padding:4 8;"
        );
        // Fallback simple si rgba ne parse pas bien
        emojiBadge.setStyle(
                "-fx-font-size:15;" +
                        "-fx-text-fill:" + accentColor + ";"
        );

        Label titleLbl = new Label(emoji + "  " + title);
        titleLbl.setStyle(
                "-fx-text-fill:" + accentColor + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13;"
        );
        titleLbl.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        Label textLbl = new Label(text);
        textLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.60);-fx-font-size:12;");
        textLbl.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        textLbl.setWrapText(true);
        textLbl.setMaxWidth(Double.MAX_VALUE);

        VBox texts = new VBox(5, titleLbl, textLbl);
        texts.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        texts.setPadding(new Insets(0, 0, 0, 12));
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox inner = new HBox(0, bar, texts);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setMaxWidth(Double.MAX_VALUE);
        inner.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        createRoleDescBox.getChildren().add(inner);

        // Style de la boîte — couleur de bordure selon le rôle
        Color borderColor = Color.web(accentColor, 0.30);
        createRoleDescBox.setBackground(new Background(new BackgroundFill(
                Color.web(BG_FIELD), new CornerRadii(10), Insets.EMPTY)));
        createRoleDescBox.setBorder(new Border(new BorderStroke(
                borderColor, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1))));
        createRoleDescBox.setPadding(new Insets(14));
        createRoleDescBox.setVisible(true);
        createRoleDescBox.setManaged(true);

        final String descStyle =
                "-fx-background-color:" + BG_FIELD + ";" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;";
        createRoleDescBox.setStyle(descStyle);

        Platform.runLater(() -> createRoleDescBox.setStyle(descStyle));
    }

    private void setupPasswordStrength() {
        if (createPasswordField == null || createPasswordStrength == null) return;
        createPasswordField.textProperty().addListener((obs, o, n) -> {
            evaluateStrength(n);
            if (createPasswordVisible != null) createPasswordVisible.setText(n);
        });
        if (createPasswordVisible != null) {
            createPasswordVisible.setVisible(false); createPasswordVisible.setManaged(false);
            createPasswordVisible.textProperty().addListener((obs, o, n) -> {
                if (!createPasswordField.getText().equals(n)) { createPasswordField.setText(n); evaluateStrength(n); }
            });
        }
    }

    // ── Remplacer evaluateStrength() ──────────────────────────────
    private void evaluateStrength(String pwd) {
        int s = 0;
        boolean hasUpper   = pwd.matches(".*[A-Z].*");
        boolean hasLower   = pwd.matches(".*[a-z].*");
        boolean hasDigit   = pwd.matches(".*[0-9].*");
        boolean hasSpecial = pwd.matches(".*[^a-zA-Z0-9].*");
        boolean hasLength  = pwd.length() >= 8;

        if (hasLength)  s++;
        if (hasUpper && hasLower) s++;
        if (hasDigit)   s++;
        if (hasSpecial) s++;

        double progress = s / 4.0;
        String color, label, styleClass;

        if      (s <= 1) { color="#ff6b6b"; label="Weak";   styleClass="strength-weak";   }
        else if (s == 2) { color="#ffa751"; label="Fair";   styleClass="strength-fair";   }
        else if (s == 3) { color="#fee140"; label="Good";   styleClass="strength-good";   }
        else             { color="#43e97b"; label="Strong"; styleClass="strength-strong"; }

        if (createPasswordStrength != null) {
            createPasswordStrength.setProgress(progress);
            applyProgressStyle(createPasswordStrength, styleClass);
        }
        setLabel(createStrengthLabel, label);
        if (createStrengthLabel != null)
            createStrengthLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:10;");
    }

    // ── Remplacer validateCreateForm() ───────────────────────────
    private boolean validateCreateForm() {
        boolean ok = true;

        if (trim(createFullNameField).isBlank()) {
            setErr(errFullName, "Name is required.");
            ok = false;
        }
        if (!trim(createUsernameField).matches("[a-zA-Z0-9_-]{3,50}")) {
            setErr(errUsername, "Invalid username (3-50 chars, letters/digits/- and _).");
            ok = false;
        }
        if (!trim(createEmailField).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            setErr(errEmail, "Invalid email address.");
            ok = false;
        }

        // ── Validation mot de passe renforcée ────────────────────
        String pwd = createPasswordField != null ? createPasswordField.getText() : "";
        if (pwd.isBlank()) {
            setErr(errPassword, "Password is required.");
            ok = false;
        } else {
            java.util.List<String> pwdErrors = new java.util.ArrayList<>();
            if (pwd.length() < 8)                        pwdErrors.add("at least 8 characters");
            if (!pwd.matches(".*[A-Z].*"))               pwdErrors.add("one uppercase letter");
            if (!pwd.matches(".*[a-z].*"))               pwdErrors.add("one lowercase letter");
            if (!pwd.matches(".*[0-9].*"))               pwdErrors.add("one number");
            if (!pwd.matches(".*[^a-zA-Z0-9].*"))        pwdErrors.add("one special character (!@#$…)");

            if (!pwdErrors.isEmpty()) {
                setErr(errPassword, "Password must contain: " + String.join(", ", pwdErrors) + ".");
                ok = false;
            }
        }

        return ok;
    }
    private void setupCreateValidation() {
        addBlurValidation(createFullNameField, errFullName, f -> !f.isBlank(), "Name is required.");
        addBlurValidation(createUsernameField, errUsername, f -> f.matches("[a-zA-Z0-9_-]{3,50}"), "3-50 chars: letters, digits, - and _.");
        addBlurValidation(createEmailField,    errEmail,    f -> f.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "Invalid email format.");
    }

    private void addBlurValidation(TextField field, Label errLabel,
                                   java.util.function.Predicate<String> rule, String msg) {
        if (field == null) return;
        final boolean[] dirty = {false};
        field.textProperty().addListener((obs, o, n) -> dirty[0] = true);
        field.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused && dirty[0]) {
                String v = field.getText().trim();
                if (!rule.test(v)) setErr(errLabel, msg); else clearErr(errLabel);
            }
        });
    }

    @FXML public void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (createPasswordField    != null) { createPasswordField.setVisible(!passwordVisible);  createPasswordField.setManaged(!passwordVisible); }
        if (createPasswordVisible  != null) { createPasswordVisible.setVisible(passwordVisible); createPasswordVisible.setManaged(passwordVisible); }
        if (createTogglePasswordBtn != null) createTogglePasswordBtn.setText(passwordVisible ? "🙈" : "👁");
    }

    @FXML public void handleCreateSubmit() {
        clearAllErrors();
        if (!validateCreateForm()) return;

        String role = createRoleCombo != null ? createRoleCombo.getValue() : "ROLE_USER";
        if (("ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role))
                && !SessionManager.isSuperAdmin()) {
            setErr(errRole, "Only Super Admins can create an Admin or Super Admin account.");
            return;
        }

        String fullName = trim(createFullNameField);
        String username = trim(createUsernameField);
        String email    = trim(createEmailField);
        // ── Garder le mot de passe brut AVANT hachage pour l'email ──
        String rawPassword = createPasswordField != null ? createPasswordField.getText() : "";

        if (createSubmitBtn != null) {
            createSubmitBtn.setText("Creating…");
            createSubmitBtn.setDisable(true);
        }

        new Thread(() -> {
            try {
                // Vérifications unicité
                if (userService.findByUsername(username) != null) {
                    Platform.runLater(() -> {
                        setErr(errUsername, "This username is already taken.");
                        resetCreateBtn();
                    });
                    return;
                }
                if (userService.emailExists(email)) {
                    Platform.runLater(() -> {
                        setErr(errEmail, "This email is already registered.");
                        resetCreateBtn();
                    });
                    return;
                }

                // Création du compte
                userService.adminCreateUser(fullName, username, email, rawPassword, role);

                // ── Envoi de l'email de bienvenue (thread séparé) ──
                EmailService.getInstance().sendWelcomeEmail(
                        email, fullName, username, rawPassword, role);

                Platform.runLater(this::closeModal);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    resetCreateBtn();
                    alert(Alert.AlertType.ERROR, "Error", e.getMessage());
                });
            }
        }).start();
    }
    @FXML public void handleCreateCancel() { closeModal(); }


    private void resetCreateBtn() { if (createSubmitBtn!=null) { createSubmitBtn.setText("✓  Create User"); createSubmitBtn.setDisable(false); } }
    private void clearAllErrors() { for (Label l : new Label[]{errFullName,errUsername,errEmail,errPassword,errRole}) clearErr(l); }

    private void closeModal() {
        javafx.scene.Node[] candidates = {createSubmitBtn,createCancelBtn,createFullNameField,createUsernameField,createEmailField,createPasswordField};
        for (javafx.scene.Node n : candidates) {
            if (n!=null && n.getScene()!=null && n.getScene().getWindow()!=null) { ((Stage)n.getScene().getWindow()).close(); return; }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════
    private boolean isActiveStatus(String status) { return status==null||"active".equalsIgnoreCase(status); }

    private boolean canModify(User user) {
        User me = SessionManager.getCurrentUser(); if (me==null||user==null) return false;
        if (me.getId()==user.getId()) return false;
        return !hasRoleStr(user,"ROLE_ADMIN")||SessionManager.isSuperAdmin();
    }

    private boolean hasRoleStr(User user, String role) {
        if (user==null) return false;
        String json = user.getRolesJson(); if (json!=null) return json.contains(role);
        List<String> roles = user.getRoles(); return roles!=null&&roles.contains(role);
    }

    private String getRoleLabel(User u) {
        if (hasRoleStr(u,"ROLE_SUPER_ADMIN")) return "Super Admin";
        if (hasRoleStr(u,"ROLE_ADMIN"))       return "Admin";
        if (hasRoleStr(u,"ROLE_COACH"))       return "Coach";
        return "User";
    }

    private String getPrimaryRole(User u) {
        if (hasRoleStr(u,"ROLE_ADMIN")) return "ROLE_ADMIN";
        if (hasRoleStr(u,"ROLE_COACH")) return "ROLE_COACH";
        return "ROLE_USER";
    }

    private String getStatusDescription(String status) {
        if (status==null) return "";
        return switch (status.toLowerCase()) {
            case "suspended" -> "This account is temporarily suspended.";
            case "banned"    -> "This account has been permanently banned.";
            case "pending"   -> "This account is pending approval.";
            default          -> "";
        };
    }

    private String roleDisplayName(String role) {
        return switch (nvl(role, "")) {
            case "ROLE_COACH"       -> "⚡  Coach — Live streams & revenue";
            case "ROLE_ADMIN"       -> "🛡  Administrator — Full access"
                    + (SessionManager.isSuperAdmin() ? "" : " (Super Admin only)");
            case "ROLE_SUPER_ADMIN" -> "👑  Super Administrator — Unrestricted"
                    + (SessionManager.isSuperAdmin() ? "" : " (Super Admin only)");
            default                 -> "🎮  User — Standard access";
        };
    }
    private void setLabel(Label l, String v)           { if (l!=null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v) { if (pb!=null) pb.setProgress(v); }
    private void showNode(javafx.scene.Node n, boolean show) { if (n!=null) { n.setVisible(show); n.setManaged(show); } }
    private String trim(TextField f)              { return f!=null&&f.getText()!=null ? f.getText().trim() : ""; }
    private String nvl(String s, String fallback) { return s!=null&&!s.isBlank() ? s : fallback; }
    private boolean contains(String h, String n)  { return h!=null&&h.toLowerCase().contains(n); }
    private void setErr(Label l, String msg)  { if (l==null) return; l.setText(msg); l.setVisible(true); l.setManaged(true); l.setStyle("-fx-text-fill:#ff6b7a;-fx-font-size:11;"); }
    private void clearErr(Label l)            { if (l!=null) { l.setText(""); l.setVisible(false); l.setManaged(false); } }
    private Label errLabel()                  { Label l=new Label(""); l.setVisible(false); l.setManaged(false); l.setStyle("-fx-text-fill:#ff6b7a;-fx-font-size:11;"); return l; }

    private boolean confirm(String title, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        Optional<ButtonType> r = a.showAndWait(); return r.isPresent()&&r.get()==ButtonType.OK;
    }
    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════
    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml); if (url==null) { System.err.println("[AdminUserController] FXML not found: "+fxml); return; }
        try {
            FXMLLoader loader = new FXMLLoader(url); loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load(); Stage stage = resolveStage();
            if (stage!=null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) { System.err.println("[AdminUserController] Nav error: "+e.getMessage()); e.printStackTrace(); }
    }

    private URL resolveUrl(String fxml) {
        for (String p : new String[]{"/com/eyetwin/views/"+fxml,"/com/eyetwin/view/"+fxml,"/com/eyetwin/"+fxml}) {
            URL u = getClass().getResource(p); if (u!=null) return u;
        }
        return null;
    }

    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {searchField,usersTable,avatarInitialLabel,fullNameHeaderLabel,createSubmitBtn,totalUsersLabel,prevPageBtn,nextPageBtn};
        for (javafx.scene.Node n : candidates) { if (n!=null&&n.getScene()!=null) return (Stage)n.getScene().getWindow(); }
        return null;
    }

    private void loadDetailAvatar(User u) {
        if (avatarPane == null) return;

        String photoFile = u.getProfilePicture();
        if (photoFile != null && !photoFile.isBlank()) {
            try {
                java.io.File file = new java.io.File(
                        System.getProperty("user.dir") + "/uploads/profiles/" + photoFile);
                if (file.exists()) {
                    javafx.scene.image.Image img =
                            new javafx.scene.image.Image(file.toURI().toString(), 110, 110, true, true);
                    if (!img.isError()) {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(110); iv.setFitHeight(110); iv.setPreserveRatio(false);
                        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
                        iv.setClip(clip);
                        // Remplace le contenu du StackPane
                        avatarPane.getChildren().setAll(iv);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback : cercle gradient + initiales (déjà dans le FXML, on ne touche à rien)
        // Juste s'assurer que les initiales sont bien là
        javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(55);
        circle.setFill(javafx.scene.paint.Color.web("#667eea"));
        String initials = u.getUsername() != null && !u.getUsername().isEmpty()
                ? u.getUsername().substring(0, Math.min(2, u.getUsername().length())).toUpperCase() : "??";
        Label lbl = new Label(initials);
        lbl.setStyle("-fx-font-size:36;-fx-font-weight:bold;-fx-text-fill:white;");
        avatarPane.getChildren().setAll(circle, lbl);
        if (avatarInitialLabel != null) avatarInitialLabel.setText(initials);
    }

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
    @FXML public void goToAuditLogs()         { if (!SessionManager.isSuperAdmin()) return; navigateTo("AdminAuditLogs.fxml"); }
    @FXML public void handleLogout()          { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }
}
