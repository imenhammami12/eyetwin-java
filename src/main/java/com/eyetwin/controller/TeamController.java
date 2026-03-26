package com.eyetwin.controller;

import com.eyetwin.dao.TeamDAO;
import com.eyetwin.model.*;
import com.eyetwin.service.TeamService;
import com.eyetwin.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TeamController — controller unique pour toute la gestion des teams.
 * Remplace TeamListController + TeamCreateController + TeamShowController + TeamEditController
 * Utilise un StackPane central avec 4 vues : LIST, CREATE, SHOW, EDIT
 */
public class TeamController {

    // ════════════════════════════════════════════════════════════
    //  VUES (StackPane avec 4 panels)
    // ════════════════════════════════════════════════════════════
    @FXML private StackPane mainStack;
    @FXML private VBox viewList;
    @FXML private VBox viewCreate;
    @FXML private VBox viewShow;
    @FXML private VBox viewEdit;

    // ── NAVBAR ──
    @FXML private Label coinsLabel;
    @FXML private Label navBreadcrumb;

    // ════════════════ LIST VIEW ════════════════
    @FXML private Label statOwned;
    @FXML private Label statMember;
    @FXML private Label statInvitations;
    @FXML private Label statRequests;
    @FXML private VBox  invitationsBlock;
    @FXML private Label invitationsSubLabel;
    @FXML private Label invitationsCountBadge;
    @FXML private FlowPane invitationsPane;
    @FXML private VBox  requestsBlock;
    @FXML private Label requestsSubLabel;
    @FXML private Label requestsCountBadge;
    @FXML private FlowPane pendingRequestsPane;
    @FXML private VBox  ownedBlock;
    @FXML private Label ownedSubLabel;
    @FXML private Label ownedCountBadge;
    @FXML private FlowPane ownedTeamsPane;
    @FXML private VBox  memberBlock;
    @FXML private Label memberSubLabel;
    @FXML private Label memberCountBadge;
    @FXML private FlowPane memberTeamsPane;
    @FXML private FlowPane allTeamsPane;
    @FXML private VBox discoverEmpty;
    @FXML private VBox globalEmpty;

    // ════════════════ CREATE VIEW ════════════════
    @FXML private TextField  createNameField;
    @FXML private TextArea   createDescField;
    @FXML private TextField  createMaxField;
    @FXML private CheckBox   createActiveCheck;
    @FXML private Label      createNameError;
    @FXML private Label      createMaxError;
    @FXML private Label      createGeneralError;
    @FXML private Button     createSubmitBtn;

    // ════════════════ SHOW VIEW ════════════════
    @FXML private Label showTeamName;
    @FXML private Label showOwner;
    @FXML private Label showCreatedAt;
    @FXML private Label showStatusBadge;
    @FXML private Button showEditBtn;
    @FXML private Button showLeaveBtn;
    @FXML private Button showJoinBtn;
    @FXML private Button showPendingBtn;
    @FXML private Label  showPendingAlert;
    @FXML private Label  showStatMembers;
    @FXML private Label  showStatMax;
    @FXML private Label  showStatPending;
    @FXML private Label  showStatCapacity;
    @FXML private Label  showDescription;
    @FXML private Label  showCapacityLabel;
    @FXML private Region showCapacityBar;
    @FXML private VBox   showPendingCard;
    @FXML private Label  showPendingCount;
    @FXML private VBox   showPendingList;
    @FXML private VBox   showMembersList;
    @FXML private Label  showMembersCount;
    @FXML private VBox   showInviteCard;
    @FXML private Label  showFullCapacity;
    @FXML private VBox   showInviteArea;
    @FXML private TextField showSearchField;
    @FXML private VBox   showSearchResults;
    @FXML private HBox   showSelectedUser;
    @FXML private Label  showSelectedName;
    @FXML private Button showInviteBtn;

    // ════════════════ EDIT VIEW ════════════════
    @FXML private Label    editHeroName;
    @FXML private TextField editNameField;
    @FXML private TextArea  editDescField;
    @FXML private TextField editMaxField;
    @FXML private CheckBox  editActiveCheck;
    @FXML private Label     editNameError;
    @FXML private Label     editMaxError;
    @FXML private Label     editGeneralError;
    @FXML private Button    editSubmitBtn;

    // ════════════════════════════════════════════════════════════
    //  STATE
    // ════════════════════════════════════════════════════════════
    private final TeamService teamService = new TeamService();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private Team currentTeam;
    private int selectedInviteUserId = -1;
    private Thread searchThread;

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user != null && coinsLabel != null)
            coinsLabel.setText(String.valueOf(user.getCoinBalance()));
        showView("list");
        loadListData();
    }

    // ════════════════════════════════════════════════════════════
    //  VIEW SWITCHER
    // ════════════════════════════════════════════════════════════
    private void showView(String view) {
        viewList.setVisible(false);   viewList.setManaged(false);
        viewCreate.setVisible(false); viewCreate.setManaged(false);
        viewShow.setVisible(false);   viewShow.setManaged(false);
        viewEdit.setVisible(false);   viewEdit.setManaged(false);

        switch (view) {
            case "list"   -> { viewList.setVisible(true);   viewList.setManaged(true);   }
            case "create" -> { viewCreate.setVisible(true); viewCreate.setManaged(true); }
            case "show"   -> { viewShow.setVisible(true);   viewShow.setManaged(true);   }
            case "edit"   -> { viewEdit.setVisible(true);   viewEdit.setManaged(true);   }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LIST — chargement données
    // ════════════════════════════════════════════════════════════
    private void loadListData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        int userId = user.getId();

        new Thread(() -> {
            try {
                List<TeamMembership> invitations = teamService.getPendingInvitations(userId);
                List<TeamMembership> myRequests  = teamService.getUserPendingRequests(userId);
                List<Team> ownedTeams  = teamService.getOwnedTeams(userId);
                List<Team> memberTeams = teamService.getMemberTeams(userId);
                List<Team> allTeams    = teamService.getAllActiveTeams();

                Platform.runLater(() ->
                        renderList(invitations, myRequests, ownedTeams, memberTeams, allTeams, userId));
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Database error: " + e.getMessage()));
            }
        }).start();
    }

    private void renderList(List<TeamMembership> invitations,
                            List<TeamMembership> myRequests,
                            List<Team> ownedTeams,
                            List<Team> memberTeams,
                            List<Team> allTeams,
                            int userId) {

        statOwned.setText(String.valueOf(ownedTeams.size()));
        statMember.setText(String.valueOf(memberTeams.size()));
        statInvitations.setText(String.valueOf(invitations.size()));
        statRequests.setText(String.valueOf(myRequests.size()));

        if (!invitations.isEmpty()) {
            invitationsBlock.setVisible(true); invitationsBlock.setManaged(true);
            invitationsCountBadge.setText(String.valueOf(invitations.size()));
            invitationsSubLabel.setText("You have " + invitations.size() + " invite(s)");
            invitationsPane.getChildren().clear();
            invitations.forEach(inv -> invitationsPane.getChildren().add(buildInvCard(inv)));
        }

        if (!myRequests.isEmpty()) {
            requestsBlock.setVisible(true); requestsBlock.setManaged(true);
            requestsCountBadge.setText(String.valueOf(myRequests.size()));
            requestsSubLabel.setText(myRequests.size() + " request(s) pending");
            pendingRequestsPane.getChildren().clear();
            myRequests.forEach(req -> pendingRequestsPane.getChildren().add(buildReqCard(req)));
        }

        if (!ownedTeams.isEmpty()) {
            ownedBlock.setVisible(true); ownedBlock.setManaged(true);
            ownedCountBadge.setText(String.valueOf(ownedTeams.size()));
            ownedSubLabel.setText("Commander of " + ownedTeams.size() + " team(s)");
            ownedTeamsPane.getChildren().clear();
            ownedTeams.forEach(t -> ownedTeamsPane.getChildren().add(buildTeamCard(t, true)));
        }

        if (!memberTeams.isEmpty()) {
            memberBlock.setVisible(true); memberBlock.setManaged(true);
            memberCountBadge.setText(String.valueOf(memberTeams.size()));
            memberSubLabel.setText("Active in " + memberTeams.size() + " team(s)");
            memberTeamsPane.getChildren().clear();
            memberTeams.forEach(t -> memberTeamsPane.getChildren().add(buildTeamCard(t, false)));
        }

        allTeamsPane.getChildren().clear();
        boolean found = false;
        for (Team t : allTeams) {
            boolean isMember = t.getTeamMemberships().stream()
                    .anyMatch(m -> m.getUserId() == userId &&
                            (m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.INVITED));
            if (!isMember) {
                allTeamsPane.getChildren().add(buildTeamCard(t, false));
                found = true;
            }
        }
        if (!found && discoverEmpty != null) {
            discoverEmpty.setVisible(true); discoverEmpty.setManaged(true);
        }
        if (ownedTeams.isEmpty() && memberTeams.isEmpty() && globalEmpty != null) {
            globalEmpty.setVisible(true); globalEmpty.setManaged(true);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  LIST — ACTIONS NAVBAR
    // ════════════════════════════════════════════════════════════
    @FXML private void goToCreateView() {
        clearCreateForm();
        showView("create");
    }

    @FXML private void backToList() {
        showView("list");
        loadListData();
    }

    // ════════════════════════════════════════════════════════════
    //  CREATE — logique
    // ════════════════════════════════════════════════════════════
    @FXML
    private void handleCreate() {
        clearCreateErrors();
        String name   = createNameField.getText() != null ? createNameField.getText().trim() : "";
        String desc   = createDescField.getText()  != null ? createDescField.getText().trim()  : "";
        String maxStr = createMaxField.getText()   != null ? createMaxField.getText().trim()   : "";
        boolean valid = true;

        if (name.length() < 3) {
            createNameError.setText("Name must be at least 3 characters.");
            createNameError.setVisible(true); createNameError.setManaged(true);
            valid = false;
        }
        int max = 10;
        try {
            max = Integer.parseInt(maxStr);
            if (max < 1 || max > 50) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            createMaxError.setText("Max members must be 1–50.");
            createMaxError.setVisible(true); createMaxError.setManaged(true);
            valid = false;
        }
        if (!valid) return;

        Team team = new Team();
        team.setName(name);
        team.setDescription(desc.isEmpty() ? null : desc);
        team.setMaxMembers(max);
        team.setActive(createActiveCheck.isSelected());

        createSubmitBtn.setDisable(true);
        createSubmitBtn.setText("Creating…");

        int ownerId = SessionManager.getCurrentUser().getId();
        new Thread(() -> {
            try {
                teamService.createTeam(team, ownerId);
                Platform.runLater(() -> loadShowView(team.getId()));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createSubmitBtn.setDisable(false);
                    createSubmitBtn.setText("🚀  Launch Team");
                    createGeneralError.setText("Error: " + e.getMessage());
                    createGeneralError.setVisible(true); createGeneralError.setManaged(true);
                });
            }
        }).start();
    }

    private void clearCreateForm() {
        if (createNameField != null) createNameField.clear();
        if (createDescField  != null) createDescField.clear();
        if (createMaxField   != null) createMaxField.setText("10");
        if (createActiveCheck != null) createActiveCheck.setSelected(true);
        if (createSubmitBtn  != null) { createSubmitBtn.setDisable(false); createSubmitBtn.setText("🚀  Launch Team"); }
        clearCreateErrors();
    }

    private void clearCreateErrors() {
        hide(createNameError); hide(createMaxError); hide(createGeneralError);
    }

    // ════════════════════════════════════════════════════════════
    //  SHOW — chargement
    // ════════════════════════════════════════════════════════════
    private void loadShowView(int teamId) {
        showView("show");
        new Thread(() -> {
            try {
                Team team = teamService.getTeamWithDetails(teamId);
                List<TeamMembership> members  = teamService.getActiveMembers(teamId);
                List<TeamMembership> pending  = teamService.getPendingRequests(teamId);
                Platform.runLater(() -> renderShow(team, members, pending));
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("DB Error: " + e.getMessage()));
            }
        }).start();
    }

    private void renderShow(Team team, List<TeamMembership> members, List<TeamMembership> pending) {
        this.currentTeam = team;
        User me = SessionManager.getCurrentUser();
        boolean isOwner = me != null && team.getOwnerId() == me.getId();

        showTeamName.setText(team.getName());
        showOwner.setText("👤 " + (team.getOwner() != null ? team.getOwner().getUsername() : "?"));
        showCreatedAt.setText("📅 " + (team.getCreatedAt() != null ? team.getCreatedAt().format(DATE_FMT) : "—"));

        long cnt = members.size(); int maxM = team.getMaxMembers();
        int pct = maxM > 0 ? (int) Math.round(cnt * 100.0 / maxM) : 0;

        showStatMembers.setText(String.valueOf(cnt));
        showStatMax.setText(String.valueOf(maxM));
        showStatPending.setText(String.valueOf(pending.size()));
        showStatCapacity.setText(pct + "%");
        showDescription.setText(team.getDescription() != null ? team.getDescription() : "(No description)");
        showCapacityLabel.setText(cnt + "/" + maxM);

        // Action buttons
        hide(showEditBtn); hide(showLeaveBtn); hide(showJoinBtn);
        hide(showPendingBtn); hide(showPendingAlert);

        if (isOwner) {
            show(showEditBtn);
            if (!pending.isEmpty()) {
                showPendingAlert.setText("🔔 " + pending.size() + " pending request(s)");
                show(showPendingAlert);
            }
        } else if (me != null) {
            TeamMembership myM = team.getTeamMemberships().stream()
                    .filter(m -> m.getUserId() == me.getId()).findFirst().orElse(null);
            if (myM != null && myM.getStatus() == MembershipStatus.ACTIVE) show(showLeaveBtn);
            else if (myM != null && myM.getStatus() == MembershipStatus.PENDING) show(showPendingBtn);
            else if (team.isActive() && cnt < maxM) show(showJoinBtn);
        }

        // Members list
        showMembersCount.setText(cnt + " active player(s)");
        showMembersList.getChildren().clear();
        members.forEach(m -> showMembersList.getChildren().add(buildMemberRow(m, isOwner)));

        // Pending requests (owner)
        if (isOwner && !pending.isEmpty()) {
            show(showPendingCard);
            showPendingCount.setText(pending.size() + " player(s) waiting");
            showPendingList.getChildren().clear();
            pending.forEach(r -> showPendingList.getChildren().add(buildRequestRow(r)));
        }

        // Invite panel (owner)
        if (isOwner) {
            show(showInviteCard);
            if (cnt >= maxM) { show(showFullCapacity); hide(showInviteArea); }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  SHOW — ACTIONS
    // ════════════════════════════════════════════════════════════
    @FXML private void handleShowEdit() {
        if (currentTeam != null) loadEditView(currentTeam.getId());
    }

    @FXML private void handleShowLeave() {
        if (currentTeam == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Leave this team?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        teamService.leaveTeam(currentTeam.getId(), SessionManager.getCurrentUser().getId());
                        Platform.runLater(() -> { showAlert("You left the team."); backToList(); });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML private void handleShowJoin() {
        if (currentTeam == null) return;
        new Thread(() -> {
            try {
                teamService.requestJoin(currentTeam.getId(), SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { showInfo("Request sent!"); loadShowView(currentTeam.getId()); });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML private void handleSearchUsers() {
        if (searchThread != null) searchThread.interrupt();
        String q = showSearchField.getText() != null ? showSearchField.getText().trim() : "";
        if (q.length() < 2) { hide(showSearchResults); return; }
        searchThread = new Thread(() -> {
            try {
                Thread.sleep(300);
                List<User> users = teamService.searchUsers(q);
                Platform.runLater(() -> {
                    showSearchResults.getChildren().clear();
                    if (users.isEmpty()) {
                        Label lbl = new Label("No players found");
                        lbl.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-padding: 10;");
                        showSearchResults.getChildren().add(lbl);
                    } else {
                        users.forEach(u -> showSearchResults.getChildren().add(buildSearchItem(u)));
                    }
                    show(showSearchResults);
                });
            } catch (InterruptedException ignored) {
            } catch (SQLException e) { Platform.runLater(() -> hide(showSearchResults)); }
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void selectUser(User user) {
        selectedInviteUserId = user.getId();
        showSelectedName.setText(user.getUsername());
        show(showSelectedUser);
        hide(showSearchResults);
        showSearchField.clear();
        showInviteBtn.setDisable(false);
        showInviteBtn.setOpacity(1.0);
    }

    @FXML private void clearInviteSelection() {
        selectedInviteUserId = -1;
        hide(showSelectedUser);
        showInviteBtn.setDisable(true);
        showInviteBtn.setOpacity(0.5);
    }

    @FXML private void handleInvite() {
        if (selectedInviteUserId == -1 || currentTeam == null) return;
        showInviteBtn.setDisable(true);
        showInviteBtn.setText("Sending…");
        int tid = currentTeam.getId(), uid = selectedInviteUserId;
        new Thread(() -> {
            try {
                teamService.inviteUser(tid, uid, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> {
                    clearInviteSelection();
                    showInviteBtn.setText("📨  Send Invitation");
                    showInfo("Invitation sent!");
                    loadShowView(tid);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showInviteBtn.setDisable(false);
                    showInviteBtn.setText("📨  Send Invitation");
                    showAlert("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    //  EDIT — chargement
    // ════════════════════════════════════════════════════════════
    private void loadEditView(int teamId) {
        showView("edit");
        new Thread(() -> {
            try {
                Team team = teamService.getTeamWithDetails(teamId);
                Platform.runLater(() -> populateEdit(team));
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("DB Error: " + e.getMessage()));
            }
        }).start();
    }

    private void populateEdit(Team team) {
        this.currentTeam = team;
        User me = SessionManager.getCurrentUser();
        if (me == null || team.getOwnerId() != me.getId()) {
            showAlert("Not authorized."); backToList(); return;
        }
        editHeroName.setText(team.getName());
        editNameField.setText(team.getName());
        editDescField.setText(team.getDescription() != null ? team.getDescription() : "");
        editMaxField.setText(String.valueOf(team.getMaxMembers()));
        editActiveCheck.setSelected(team.isActive());
        editSubmitBtn.setDisable(false);
        editSubmitBtn.setText("💾  Save Changes");
        clearEditErrors();
    }

    @FXML private void handleEditSubmit() {
        clearEditErrors();
        String name   = editNameField.getText() != null ? editNameField.getText().trim() : "";
        String desc   = editDescField.getText()  != null ? editDescField.getText().trim()  : "";
        String maxStr = editMaxField.getText()   != null ? editMaxField.getText().trim()   : "";
        boolean valid = true;

        if (name.length() < 3) {
            editNameError.setText("Name must be at least 3 characters.");
            show(editNameError); valid = false;
        }
        int max = 10;
        try {
            max = Integer.parseInt(maxStr);
            if (max < 1 || max > 50) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            editMaxError.setText("Max members must be 1–50.");
            show(editMaxError); valid = false;
        }
        if (!valid) return;

        currentTeam.setName(name);
        currentTeam.setDescription(desc.isEmpty() ? null : desc);
        currentTeam.setMaxMembers(max);
        currentTeam.setActive(editActiveCheck.isSelected());

        editSubmitBtn.setDisable(true);
        editSubmitBtn.setText("Saving…");

        Team t = currentTeam;
        new Thread(() -> {
            try {
                teamService.updateTeam(t, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> loadShowView(t.getId()));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    editSubmitBtn.setDisable(false);
                    editSubmitBtn.setText("💾  Save Changes");
                    editGeneralError.setText("Error: " + e.getMessage());
                    show(editGeneralError);
                });
            }
        }).start();
    }

    @FXML private void cancelEdit() {
        if (currentTeam != null) loadShowView(currentTeam.getId());
        else backToList();
    }

    private void clearEditErrors() {
        hide(editNameError); hide(editMaxError); hide(editGeneralError);
    }

    // ════════════════════════════════════════════════════════════
    //  CARD / ROW BUILDERS
    // ════════════════════════════════════════════════════════════
    private VBox buildInvCard(TeamMembership inv) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setStyle(cardStyle("rgba(232,55,42,0.04)", "rgba(232,55,42,0.15)"));

        String teamName  = inv.getTeam() != null ? inv.getTeam().getName() : "Team #" + inv.getTeamId();
        String ownerName = inv.getTeam() != null && inv.getTeam().getOwner() != null
                ? inv.getTeam().getOwner().getUsername() : "Unknown";

        Label name  = styledLabel(teamName, "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");
        Label owner = styledLabel("👤 " + ownerName, "-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.38);");
        Label date  = styledLabel("🕐 " + (inv.getInvitedAt() != null ? inv.getInvitedAt().format(DATE_FMT) : "—"),
                "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.35);");

        HBox actions = new HBox(8);
        Button accept  = actionBtn("✓ Accept",  "#00e676", "rgba(0,230,118,0.12)", "rgba(0,230,118,0.3)");
        Button decline = actionBtn("✗ Decline", "#ff4d3d", "rgba(232,55,42,0.1)",  "rgba(232,55,42,0.3)");
        accept.setOnAction(e -> handleAcceptInvitation(inv.getId()));
        decline.setOnAction(e -> handleDeclineInvitation(inv.getId()));
        HBox.setHgrow(accept, Priority.ALWAYS);
        HBox.setHgrow(decline, Priority.ALWAYS);
        actions.getChildren().addAll(accept, decline);

        card.getChildren().addAll(new VBox(3, name, owner, date), actions);
        return card;
    }

    private VBox buildReqCard(TeamMembership req) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setStyle(cardStyle("rgba(255,107,43,0.04)", "rgba(255,107,43,0.15)"));

        String teamName  = req.getTeam() != null ? req.getTeam().getName() : "Team #" + req.getTeamId();
        Label name  = styledLabel(teamName, "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");
        Label badge = styledLabel("PENDING", "-fx-text-fill:#ff6b2b;-fx-font-size:9;-fx-font-weight:bold;");

        Button cancel = actionBtn("✗ Cancel Request", "rgba(255,255,255,0.4)",
                "rgba(255,255,255,0.05)", "rgba(255,255,255,0.1)");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setOnAction(e -> handleCancelRequest(req.getId()));

        card.getChildren().addAll(new HBox(8, name, badge), cancel);
        return card;
    }

    private VBox buildTeamCard(Team team, boolean isOwner) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setStyle(cardStyle(
                isOwner ? "rgba(232,55,42,0.06)" : "rgba(232,55,42,0.03)",
                isOwner ? "rgba(232,55,42,0.22)" : "rgba(232,55,42,0.1)"));

        String initials = team.getName().length() >= 2
                ? team.getName().substring(0, 2).toUpperCase() : team.getName().toUpperCase();
        Label init = styledLabel(initials, "-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#ff4d3d;");
        StackPane banner = new StackPane(init);
        banner.setMinHeight(70);
        banner.setStyle("-fx-background-color:linear-gradient(135deg,#1a0308,#12050e);-fx-background-radius:8;");

        Label nameLbl = styledLabel(team.getName(),
                "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.93);");
        String owner = team.getOwner() != null ? team.getOwner().getUsername() : "Owner";
        Label meta = styledLabel("👤 " + owner,
                "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.38);");

        long cnt = team.getActiveMembersCount(); int maxM = team.getMaxMembers();
        Label count = styledLabel(cnt + "/" + maxM,
                "-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.7);");

        HBox btnRow = new HBox(6);
        Button viewBtn = actionBtn("👁 View", "#ff4d3d", "rgba(232,55,42,0.1)", "rgba(232,55,42,0.3)");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(viewBtn, Priority.ALWAYS);
        int tid = team.getId();
        viewBtn.setOnAction(e -> loadShowView(tid));
        btnRow.getChildren().add(viewBtn);

        if (isOwner) {
            Button editBtn = actionBtn("✏ Edit", "#ff6b2b", "rgba(255,107,43,0.08)", "rgba(255,107,43,0.25)");
            editBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setOnAction(e -> loadEditView(tid));
            btnRow.getChildren().add(editBtn);
        } else if (team.isActive() && cnt < maxM) {
            Button joinBtn = actionBtn("👤+ Join", "#00e676", "rgba(0,230,118,0.1)", "rgba(0,230,118,0.3)");
            joinBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(joinBtn, Priority.ALWAYS);
            joinBtn.setOnAction(e -> {
                new Thread(() -> {
                    try {
                        teamService.requestJoin(tid, SessionManager.getCurrentUser().getId());
                        Platform.runLater(() -> { showInfo("Request sent!"); loadListData(); });
                    } catch (Exception ex) {
                        Platform.runLater(() -> showAlert("Error: " + ex.getMessage()));
                    }
                }).start();
            });
            btnRow.getChildren().add(joinBtn);
        }

        card.getChildren().addAll(banner, nameLbl, meta, count, btnRow);
        return card;
    }

    private HBox buildMemberRow(TeamMembership m, boolean isOwner) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:rgba(255,255,255,0.025);-fx-border-color:rgba(255,255,255,0.065);" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 14 12 14;");

        String uname = m.getUser() != null ? m.getUser().getUsername() : "User #" + m.getUserId();
        String role  = m.getRole() != null ? m.getRole().getLabel() : "Member";
        String color = m.getRole() == MemberRole.OWNER ? "#ff4d3d" : "rgba(255,255,255,0.4)";

        Label nameLbl = styledLabel(uname, "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.93);");
        Label roleLbl = styledLabel(role, "-fx-font-size:10;-fx-text-fill:" + color + ";");
        VBox info = new VBox(3, nameLbl, roleLbl);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.getChildren().addAll(buildAvatar(m.getUser(), m.getRole()), info);

        if (isOwner && m.getRole() != MemberRole.OWNER) {
            Button rem = actionBtn("🚫", "#ff4d3d", "rgba(232,55,42,0.1)", "rgba(232,55,42,0.25)");
            rem.setPrefSize(32, 32);
            rem.setOnAction(e -> handleRemoveMember(currentTeam.getId(), m.getId()));
            row.getChildren().add(rem);
        }
        return row;
    }

    private HBox buildRequestRow(TeamMembership req) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:rgba(255,107,43,0.03);-fx-border-color:rgba(255,107,43,0.15);" +
                "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 14 12 14;");

        String uname = req.getUser() != null ? req.getUser().getUsername() : "User #" + req.getUserId();
        Label nameLbl = styledLabel(uname, "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.93);");
        VBox info = new VBox(3, nameLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button acc = actionBtn("✓", "#00e676", "rgba(0,230,118,0.1)", "rgba(0,230,118,0.3)");
        acc.setPrefSize(32, 32);
        acc.setOnAction(e -> handleAcceptRequest(req.getId()));

        Button rej = actionBtn("✗", "#ff4d3d", "rgba(232,55,42,0.1)", "rgba(232,55,42,0.25)");
        rej.setPrefSize(32, 32);
        rej.setOnAction(e -> handleRejectRequest(req.getId()));

        row.getChildren().addAll(buildAvatar(req.getUser(), null), info, acc, rej);
        return row;
    }

    private HBox buildSearchItem(User user) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-padding:10 14 10 14;-fx-cursor:hand;");
        Label n = styledLabel(user.getUsername(), "-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:rgba(255,255,255,0.93);");
        Label e = styledLabel(user.getEmail(), "-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.38);");
        item.getChildren().add(new VBox(2, n, e));
        item.setOnMouseClicked(ev -> selectUser(user));
        item.setOnMouseEntered(ev -> item.setStyle(item.getStyle() + "-fx-background-color:rgba(232,55,42,0.08);"));
        item.setOnMouseExited(ev -> item.setStyle("-fx-padding:10 14 10 14;-fx-cursor:hand;"));
        return item;
    }

    private StackPane buildAvatar(User user, MemberRole role) {
        StackPane sp = new StackPane();
        sp.setMinSize(46, 46); sp.setMaxSize(46, 46);
        sp.setStyle("-fx-background-color:rgba(232,55,42,0.1);-fx-border-color:rgba(232,55,42,0.25);" +
                "-fx-border-width:1;-fx-background-radius:10;-fx-border-radius:10;");
        String initials = user != null && user.getUsername() != null && !user.getUsername().isEmpty()
                ? String.valueOf(user.getUsername().charAt(0)).toUpperCase() : "?";
        Label lbl = styledLabel(initials, "-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#ff4d3d;");
        sp.getChildren().add(lbl);
        return sp;
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIONS COMMUNES
    // ════════════════════════════════════════════════════════════
    private void handleAcceptInvitation(int id) {
        new Thread(() -> {
            try {
                teamService.acceptInvitation(id, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { showInfo("Accepted!"); backToList(); });
            } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
        }).start();
    }

    private void handleDeclineInvitation(int id) {
        new Thread(() -> {
            try {
                teamService.declineInvitation(id, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { showInfo("Declined."); backToList(); });
            } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
        }).start();
    }

    private void handleCancelRequest(int id) {
        new Thread(() -> {
            try {
                teamService.cancelRequest(id, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { showInfo("Cancelled."); backToList(); });
            } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
        }).start();
    }

    private void handleAcceptRequest(int id) {
        new Thread(() -> {
            try {
                teamService.acceptRequest(id, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> { showInfo("Accepted!"); loadShowView(currentTeam.getId()); });
            } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
        }).start();
    }

    private void handleRejectRequest(int id) {
        new Thread(() -> {
            try {
                teamService.rejectRequest(id, SessionManager.getCurrentUser().getId());
                Platform.runLater(() -> loadShowView(currentTeam.getId()));
            } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
        }).start();
    }

    private void handleRemoveMember(int teamId, int membershipId) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Remove member?", ButtonType.OK, ButtonType.CANCEL);
        c.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        teamService.removeMember(teamId, membershipId, SessionManager.getCurrentUser().getId());
                        Platform.runLater(() -> loadShowView(teamId));
                    } catch (Exception e) { Platform.runLater(() -> showAlert("Error: " + e.getMessage())); }
                }).start();
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION EXTERNE
    // ════════════════════════════════════════════════════════════
    @FXML private void goHome() { navigateExternal("Home.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateExternal("Login.fxml"); }

    private void navigateExternal(String fxml) {
        try {
            var url = getClass().getResource("/com/eyetwin/view/" + fxml);
            if (url == null) url = getClass().getResource("/com/eyetwin/views/" + fxml);
            if (url == null) { System.err.println("❌ FXML not found: " + fxml); return; }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) viewList.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            showAlert("Navigation error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════
    private void show(javafx.scene.Node n) { if (n != null) { n.setVisible(true); n.setManaged(true); } }
    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }

    private Label styledLabel(String text, String style) {
        Label l = new Label(text); l.setStyle(style); return l;
    }

    private Button actionBtn(String text, String textColor, String bg, String border) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border + ";" +
                "-fx-border-width:1;-fx-border-radius:7;-fx-background-radius:7;" +
                "-fx-text-fill:" + textColor + ";-fx-font-size:11;-fx-font-weight:bold;" +
                "-fx-padding:7 14 7 14;-fx-cursor:hand;");
        return b;
    }

    private String cardStyle(String bg, String border) {
        return "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";" +
                "-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14 16 14 16;";
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}