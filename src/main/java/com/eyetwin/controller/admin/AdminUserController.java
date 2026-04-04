package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.entities.TeamMembership;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;

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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * AdminUserController — Contrôleur unique pour la gestion des utilisateurs.
 *
 * CORRECTIONS apportées :
 *  - accountStatus est un String dans User.java ("active","suspended","banned","pending")
 *    → toutes les comparaisons utilisent .equalsIgnoreCase() au lieu de == AccountStatus.XXX
 *  - getTeamMemberships() n'existe pas dans User.java
 *    → remplacé par userService.getTeamMemberships(user.getId())
 *  - AccountStatus (enum) n'est plus importé / utilisé ici
 *  - getRolesJson() utilisé directement (String) pour les checks de rôle
 */
public class AdminUserController {

    // ═══════════════════════════════════════════════════════════
    //  SHARED — Sidebar & Topbar
    // ═══════════════════════════════════════════════════════════
    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    // ═══════════════════════════════════════════════════════════
    //  VUE ① — LISTE  (AdminUsers.fxml)
    // ═══════════════════════════════════════════════════════════

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label coachesLabel;
    @FXML private Label adminsLabel;
    @FXML private Label inactiveUsersLabel;
    @FXML private Label activeRateLabel;

    @FXML private ProgressBar progressActive;
    @FXML private ProgressBar progressCoaches;
    @FXML private ProgressBar progressAdmins;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private Label resultCountLabel;

    @FXML private TableView<User>           usersTable;
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
    //  VUE ② — DÉTAIL  (AdminUserDetail.fxml)
    // ═══════════════════════════════════════════════════════════

    @FXML private Label avatarInitialLabel;
    @FXML private Label fullNameHeaderLabel;
    @FXML private Label usernameHeaderLabel;
    @FXML private Label roleChipLabel;
    @FXML private Label statusChipLabel;
    @FXML private Label youBadgeLabel;

    @FXML private VBox  bioBox;
    @FXML private Label bioLabel;

    @FXML private VBox limitedPermBanner;
    @FXML private VBox selfViewBanner;

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
    @FXML private Label lastLoginLabel;
    @FXML private Label statusInfoLabel;

    @FXML private Label         teamsBadge;
    @FXML private VBox          teamsEmptyState;
    @FXML private TableView<?>  teamsTable;

    // ═══════════════════════════════════════════════════════════
    //  VUE ③ — CREATE (modal AdminCreateUser.fxml)
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

    @FXML private ProgressBar createPasswordStrength;
    @FXML private Label       createStrengthLabel;

    @FXML private VBox  createRoleDescBox;
    @FXML private Label createRoleTitleLabel;
    @FXML private Label createRoleTextLabel;

    @FXML private Label errFullName;
    @FXML private Label errUsername;
    @FXML private Label errEmail;
    @FXML private Label errPassword;
    @FXML private Label errRole;

    // ═══════════════════════════════════════════════════════════
    //  ÉTAT INTERNE
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
    }

    // ═══════════════════════════════════════════════════════════
    //  ① LISTE
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

        if (colUsername != null) colUsername.setCellValueFactory(d ->
                new SimpleStringProperty("@" + d.getValue().getUsername()));

        if (colFullName != null) colFullName.setCellValueFactory(d ->
                new SimpleStringProperty(nvl(d.getValue().getFullName(), "N/A")));

        if (colEmail != null) colEmail.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEmail()));

        if (colRole != null) colRole.setCellValueFactory(d ->
                new SimpleStringProperty(getRoleLabel(d.getValue())));

        // FIX : accountStatus est un String → pas besoin de .name()
        if (colStatus != null) colStatus.setCellValueFactory(d -> {
            String s = d.getValue().getAccountStatus();
            return new SimpleStringProperty(s != null ? s.toUpperCase() : "ACTIVE");
        });

        if (colJoined != null) colJoined.setCellValueFactory(d -> {
            String date = d.getValue().getCreatedAt() != null
                    ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—";
            return new SimpleStringProperty(date);
        });

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = makeBtn("👁", "#4facfe");
                private final Button actBtn  = makeBtn("⚠", "#ffa751");
                private final Button delBtn  = makeBtn("🗑", "#ff6b7a");
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

                    // FIX : isActiveStatus() compare des String
                    boolean isActive = isActiveStatus(u.getAccountStatus());
                    actBtn.setText(isActive ? "⚠" : "✓");
                    actBtn.setStyle(actBtn.getStyle().replace(
                            "#ffa751", isActive ? "#ffa751" : "#43e97b"));
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

    private Button makeBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 13; -fx-padding: 3 8; -fx-cursor: hand;");
        return b;
    }

    // ── Chargement des données ────────────────────────────────
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

    private void refreshKPICards(List<User> users) {
        int total   = users.size();
        // FIX : comparaison String pour accountStatus
        int active  = (int) users.stream().filter(u -> isActiveStatus(u.getAccountStatus())).count();
        int coaches = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_COACH")).count();
        int admins  = (int) users.stream().filter(u -> hasRoleStr(u, "ROLE_ADMIN")).count();
        int inactive = total - active;
        double rate  = total > 0 ? (active * 100.0 / total) : 0;

        setLabel(totalUsersLabel,    String.valueOf(total));
        setLabel(activeUsersLabel,   String.valueOf(active));
        setLabel(coachesLabel,       String.valueOf(coaches));
        setLabel(adminsLabel,        String.valueOf(admins));
        setLabel(inactiveUsersLabel, String.valueOf(inactive));
        setLabel(activeRateLabel,    String.format("%.1f%%", rate));

        setProgress(progressActive,  total > 0 ? (double) active  / total : 0);
        setProgress(progressCoaches, total > 0 ? (double) coaches / total : 0);
        setProgress(progressAdmins,  total > 0 ? (double) admins  / total : 0);
    }

    // ── Filtres & pagination ──────────────────────────────────
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
            // FIX : comparaison String insensible à la casse
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

    // ── Actions depuis la table ───────────────────────────────
    private void handleSuspendToggle(User user) {
        if (!canModify(user)) {
            alert(Alert.AlertType.WARNING, "Permission Denied",
                    "Vous n'avez pas la permission de modifier ce compte.");
            return;
        }
        // FIX : isActiveStatus() compare des String
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

    @FXML public void handleNewUser() {
        openModal("AdminCreateUser.fxml", "Créer un utilisateur");
        loadAllUsers();
    }

    // ═══════════════════════════════════════════════════════════
    //  ② DÉTAIL
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

        // ── Avatar ────────────────────────────────────────────
        String initials = !user.getUsername().isEmpty()
                ? user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase()
                : "??";
        setLabel(avatarInitialLabel,  initials);
        setLabel(fullNameHeaderLabel, nvl(user.getFullName(), user.getUsername()));
        setLabel(usernameHeaderLabel, "@" + user.getUsername());
        setLabel(roleChipLabel,       getRoleLabel(user));

        // FIX : accountStatus est un String
        String accStatus = user.getAccountStatus() != null ? user.getAccountStatus() : "active";
        setLabel(statusChipLabel, accStatus.toUpperCase());

        showNode(youBadgeLabel, isMe);

        boolean hasBio = user.getBio() != null && !user.getBio().isBlank();
        showNode(bioBox, hasBio);
        if (hasBio) setLabel(bioLabel, user.getBio());

        // ── Bannières de permission ───────────────────────────
        showNode(limitedPermBanner, !canModify && !isMe);
        showNode(selfViewBanner,    isMe);

        // ── Formulaire rôle ───────────────────────────────────
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(
                    "ROLE_USER", "ROLE_COACH", "ROLE_ADMIN"));
            roleComboBox.setValue(getPrimaryRole(user));
            roleComboBox.setDisable(!canModify);
        }
        if (updateRoleBtn != null) updateRoleBtn.setDisable(!canModify);
        showNode(rolePermNote, !canModify);
        setLabel(rolePermNote, isMe
                ? "Impossible de modifier votre propre rôle"
                : "Seuls les Super Admins peuvent modifier un Admin");

        // ── Alerte statut ─────────────────────────────────────
        // FIX : comparaison String insensible à la casse
        boolean notActive = !isActiveStatus(accStatus);
        showNode(statusAlertBox, notActive);
        if (notActive) setLabel(statusAlertLabel,
                "Compte " + accStatus.toUpperCase() + " : " + getStatusDescription(accStatus));

        // ── Boutons d'action ──────────────────────────────────
        boolean activeStatus = isActiveStatus(accStatus);
        showNode(suspendBtn,    activeStatus);
        showNode(banBtn,        activeStatus);
        showNode(reactivateBtn, !activeStatus);

        if (suspendBtn    != null) suspendBtn.setDisable(!canModify);
        if (banBtn        != null) banBtn.setDisable(!canModify);
        if (reactivateBtn != null) reactivateBtn.setDisable(!canModify);
        if (deleteBtn     != null) deleteBtn.setDisable(!canModify);

        showNode(actionPermNote, !canModify);
        setLabel(actionPermNote, isMe
                ? "Auto-modification interdite"
                : "Permission Super Admin requise");

        // ── Infos personnelles ────────────────────────────────
        setLabel(emailInfoLabel,    user.getEmail());
        setLabel(usernameInfoLabel, "@" + user.getUsername());
        setLabel(fullNameInfoLabel, nvl(user.getFullName(), "Non renseigné"));
        setLabel(registeredLabel,   user.getCreatedAt() != null ? user.getCreatedAt().toString() : "—");
        setLabel(lastLoginLabel,    user.getLastLogin() != null ? user.getLastLogin().toString() : "Jamais");
        setLabel(statusInfoLabel,   accStatus.toUpperCase());

        // ── Teams ─────────────────────────────────────────────
        // FIX : User n'a pas getTeamMemberships() → on appelle le service
        List<TeamMembership> memberships = loadTeamMemberships(user.getId());
        int teamCount = memberships != null ? memberships.size() : 0;
        setLabel(teamsBadge, String.valueOf(teamCount));
        showNode(teamsEmptyState, teamCount == 0);
        if (teamsTable != null) {
            teamsTable.setVisible(teamCount > 0);
            teamsTable.setManaged(teamCount > 0);
        }
    }

    /**
     * Charge les memberships via le service.
     * Si IUserService n'expose pas encore cette méthode, elle retourne une liste vide.
     */
    private List<TeamMembership> loadTeamMemberships(int userId) {
        try {
            return userService.getTeamMemberships(userId);
        } catch (Exception e) {
            // La méthode n'est pas encore implémentée → liste vide sans crash
            return List.of();
        }
    }

    // ── Handlers détail ───────────────────────────────────────
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
            try {
                userService.suspendUser(user.getId());
                Platform.runLater(() -> refreshDetail(user.getId()));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleBan() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Bannir", "Bannir définitivement \"" + user.getUsername() + "\" ? Action sérieuse.")) return;
        new Thread(() -> {
            try {
                userService.banUser(user.getId());
                Platform.runLater(() -> refreshDetail(user.getId()));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleReactivate() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Réactiver", "Réactiver le compte de \"" + user.getUsername() + "\" ?")) return;
        new Thread(() -> {
            try {
                userService.activateUser(user.getId());
                Platform.runLater(() -> refreshDetail(user.getId()));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML public void handleDeleteUser() {
        User user = SessionManager.getSelectedUser();
        if (user == null || !canModify(user)) return;
        if (!confirm("Supprimer",
                "Supprimer définitivement \"" + user.getUsername() + "\" ?\nIrréversible !")) return;
        new Thread(() -> {
            try {
                userService.deleteUser(user.getId());
                Platform.runLater(() -> navigateTo("AdminUsers.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()));
            }
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
        createRoleCombo.setItems(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_COACH", "ROLE_ADMIN"));
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
            case "ROLE_COACH" -> { title = "🏆 Coach";           text = "Toutes les permissions User + créer des équipes et gérer les membres."; }
            case "ROLE_ADMIN" -> { title = "🛡️ Administrateur";  text = "Accès complet : gérer tous les utilisateurs, équipes, contenu et paramètres."; }
            default           -> { title = "👤 Utilisateur";     text = "Peut rejoindre des équipes, participer aux activités et gérer son profil."; }
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
        String color, label;
        if      (s <= 1) { color = "#ff6b6b"; label = "Faible"; }
        else if (s == 2) { color = "#ffa751"; label = "Moyen"; }
        else if (s == 3) { color = "#fee140"; label = "Bon"; }
        else             { color = "#43e97b"; label = "Fort"; }
        if (createPasswordStrength != null) {
            createPasswordStrength.setProgress(progress);
            createPasswordStrength.setStyle("-fx-accent: " + color + ";");
        }
        setLabel(createStrengthLabel, label);
        if (createStrengthLabel != null)
            createStrengthLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private void setupCreateValidation() {
        addBlurValidation(createFullNameField, errFullName,
                f -> !f.isBlank(), "Nom requis.");
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
        if (createPasswordField   != null) { createPasswordField.setVisible(!passwordVisible);   createPasswordField.setManaged(!passwordVisible); }
        if (createPasswordVisible != null) { createPasswordVisible.setVisible(passwordVisible);  createPasswordVisible.setManaged(passwordVisible); }
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
                    Platform.runLater(() -> { setErr(errUsername, "Ce username est déjà pris."); resetCreateBtn(); });
                    return;
                }
                if (userService.emailExists(email)) {
                    Platform.runLater(() -> { setErr(errEmail, "Cet email est déjà enregistré."); resetCreateBtn(); });
                    return;
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
        if (trim(createFullNameField).isBlank())
        { setErr(errFullName, "Nom requis."); ok = false; }
        if (!trim(createUsernameField).matches("[a-zA-Z0-9_-]{3,50}"))
        { setErr(errUsername, "Username invalide (3-50 chars)."); ok = false; }
        if (!trim(createEmailField).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
        { setErr(errEmail, "Email invalide."); ok = false; }
        String pwd = createPasswordField != null ? createPasswordField.getText() : "";
        if (pwd.isBlank())    { setErr(errPassword, "Mot de passe requis."); ok = false; }
        if (pwd.length() < 6) { setErr(errPassword, "Minimum 6 caractères."); ok = false; }
        return ok;
    }

    private void resetCreateBtn() {
        if (createSubmitBtn != null) {
            createSubmitBtn.setText("Créer l'utilisateur");
            createSubmitBtn.setDisable(false);
        }
    }

    private void clearAllErrors() {
        for (Label l : new Label[]{ errFullName, errUsername, errEmail, errPassword, errRole })
            clearErr(l);
    }

    private void closeModal() {
        Stage stage = null;
        for (javafx.scene.Node n : new javafx.scene.Node[]{ createSubmitBtn, createCancelBtn }) {
            if (n != null && n.getScene() != null) { stage = (Stage) n.getScene().getWindow(); break; }
        }
        if (stage != null) stage.close();
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITAIRES COMMUNS
    // ═══════════════════════════════════════════════════════════

    /**
     * FIX CENTRAL : accountStatus est un String dans User.java.
     * "active" (Symfony lowercase) → true, tout le reste → false.
     */
    private boolean isActiveStatus(String status) {
        return status == null || "active".equalsIgnoreCase(status);
    }

    private boolean canModify(User user) {
        User me = SessionManager.getCurrentUser();
        if (me == null || user == null) return false;
        if (me.getId() == user.getId()) return false;
        boolean isTargetAdmin = hasRoleStr(user, "ROLE_ADMIN");
        return !isTargetAdmin || SessionManager.isSuperAdmin();
    }

    // FIX : getRolesJson() retourne un String JSON — on cherche dedans directement
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

    // FIX : paramètre String (pas AccountStatus enum)
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
            case "ROLE_COACH" -> "🏆 Coach — Gestion d'équipes";
            case "ROLE_ADMIN" -> "🛡️ Administrateur — Accès complet"
                    + (SessionManager.isSuperAdmin() ? "" : " (Super Admin uniquement)");
            default           -> "👤 Utilisateur — Accès standard";
        };
    }

    // ── UI helpers ────────────────────────────────────────────
    private void setLabel(Label l, String v)       { if (l != null) l.setText(v); }
    private void setProgress(ProgressBar pb, double v) { if (pb != null) pb.setProgress(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }
    private String trim(TextField f)               { return f != null && f.getText() != null ? f.getText().trim() : ""; }
    private String nvl(String s, String fallback)  { return s != null && !s.isBlank() ? s : fallback; }
    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }
    private void setErr(Label l, String msg) {
        if (l != null) { l.setText(msg); l.setVisible(true); l.setManaged(true); }
    }
    private void clearErr(Label l) {
        if (l != null) { l.setText(""); l.setVisible(false); l.setManaged(false); }
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

    private void openModal(String fxml, String title) {
        URL url = resolveUrl(fxml);
        if (url == null) return;
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(title);
            modal.setScene(new Scene(root));
            modal.showAndWait();
        } catch (IOException e) {
            System.err.println("[AdminUserController] Modal error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════════
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

    private void navigateTo(String fxml) {
        URL url = resolveUrl(fxml);
        if (url == null) { System.err.println("[AdminUserController] FXML introuvable : " + fxml); return; }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminUserController] Nav error: " + e.getMessage());
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
                fullNameHeaderLabel, createSubmitBtn, totalUsersLabel
        };
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}