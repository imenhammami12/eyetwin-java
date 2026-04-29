package com.eyetwin.controller.admin;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.entities.GuideVideo;
import com.eyetwin.entities.User;
import com.eyetwin.repository.AgentRepository;
import com.eyetwin.repository.GameRepository;
import com.eyetwin.repository.GuideVideoRepository;
import com.eyetwin.services.EmailService;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminGuidesController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private Label gamesCountLabel;
    @FXML private Label agentsCountLabel;
    @FXML private Label guidesCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;

    @FXML private TabPane guideTabPane;
    @FXML private Tab dashboardTab;
    @FXML private Tab gamesTab;
    @FXML private Tab agentsTab;
    @FXML private Tab guidesTab;
    @FXML private Tab pendingTab;

    @FXML private TextField gameSearchField;
    @FXML private TableView<Game> gamesTable;
    @FXML private TableColumn<Game, String> gameNameCol;
    @FXML private TableColumn<Game, String> gameSlugCol;
    @FXML private TableColumn<Game, String> gameColorCol;
    @FXML private TableColumn<Game, String> gameDescriptionCol;
    @FXML private TableColumn<Game, String> gameCreatedCol;
    @FXML private TableColumn<Game, Void> gameActionsCol;

    @FXML private TextField agentSearchField;
    @FXML private TableView<Agent> agentsTable;
    @FXML private TableColumn<Agent, String> agentNameCol;
    @FXML private TableColumn<Agent, String> agentSlugCol;
    @FXML private TableColumn<Agent, String> agentGameCol;
    @FXML private TableColumn<Agent, String> agentCreatedCol;
    @FXML private TableColumn<Agent, Void> agentActionsCol;

    @FXML private TextField guideSearchField;
    @FXML private ComboBox<String> guideStatusFilter;
    @FXML private ComboBox<String> guideGameFilter;
    @FXML private TableView<GuideVideo> guidesTable;
    @FXML private TableColumn<GuideVideo, String> guideTitleCol;
    @FXML private TableColumn<GuideVideo, String> guideGameCol;
    @FXML private TableColumn<GuideVideo, String> guideAgentCol;
    @FXML private TableColumn<GuideVideo, String> guideUploaderCol;
    @FXML private TableColumn<GuideVideo, String> guideStatusCol;
    @FXML private TableColumn<GuideVideo, String> guideCreatedCol;
    @FXML private TableColumn<GuideVideo, Void> guideActionsCol;

    @FXML private TextField pendingSearchField;
    @FXML private ComboBox<String> pendingGameFilter;
    @FXML private TableView<GuideVideo> pendingTable;
    @FXML private TableColumn<GuideVideo, String> pendingTitleCol;
    @FXML private TableColumn<GuideVideo, String> pendingGameCol;
    @FXML private TableColumn<GuideVideo, String> pendingAgentCol;
    @FXML private TableColumn<GuideVideo, String> pendingUploaderCol;
    @FXML private TableColumn<GuideVideo, String> pendingCreatedCol;
    @FXML private TableColumn<GuideVideo, Void> pendingActionsCol;

    private final GameRepository gameRepository = new GameRepository();
    private final AgentRepository agentRepository = new AgentRepository();
    private final GuideVideoRepository guideVideoRepository = new GuideVideoRepository();
    private final UserServiceImpl userService = new UserServiceImpl();

    private List<Game> allGames = List.of();
    private List<Agent> allAgents = List.of();
    private List<GuideVideo> allGuides = List.of();
    private List<GuideVideo> pendingGuides = List.of();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) {
            navigateTo("AdminLogin.fxml");
            return;
        }

        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("guides");
        }
        if (adminTopbarController != null) {
            adminTopbarController.setTitle("Guides Management");
        }

        setupGuideFilters();
        setupTables();
        bindSearchFields();
        loadData();

        Platform.runLater(this::applyThemeIfPossible);
    }

    private void setupGuideFilters() {
        if (guideStatusFilter != null) {
            guideStatusFilter.setItems(FXCollections.observableArrayList("All status", "approved", "pending", "rejected"));
            guideStatusFilter.getSelectionModel().selectFirst();
        }
        if (guideGameFilter != null) {
            guideGameFilter.setItems(FXCollections.observableArrayList("All games"));
            guideGameFilter.getSelectionModel().selectFirst();
        }
        if (pendingGameFilter != null) {
            pendingGameFilter.setItems(FXCollections.observableArrayList("All games"));
            pendingGameFilter.getSelectionModel().selectFirst();
        }
    }

    private void setupTables() {
        if (gameNameCol != null) gameNameCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getName())));
        if (gameSlugCol != null) gameSlugCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getSlug())));
        if (gameColorCol != null) gameColorCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getColor())));
        if (gameDescriptionCol != null) gameDescriptionCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getDescription())));
        if (gameCreatedCol != null) gameCreatedCol.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
        if (gameActionsCol != null) gameActionsCol.setCellFactory(col -> createGameActionsCell());

        if (agentNameCol != null) agentNameCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getName())));
        if (agentSlugCol != null) agentSlugCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getSlug())));
        if (agentGameCol != null) agentGameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGame() != null ? safe(c.getValue().getGame().getName()) : "-"));
        if (agentCreatedCol != null) agentCreatedCol.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
        if (agentActionsCol != null) agentActionsCol.setCellFactory(col -> createAgentActionsCell());

        if (guideTitleCol != null) guideTitleCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTitle())));
        if (guideGameCol != null) guideGameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGame() != null ? safe(c.getValue().getGame().getName()) : "-"));
        if (guideAgentCol != null) guideAgentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAgent() != null ? safe(c.getValue().getAgent().getName()) : "-"));
        if (guideUploaderCol != null) guideUploaderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUploadedBy() != null ? safe(c.getValue().getUploadedBy().getUsername()) : "-"));
        if (guideStatusCol != null) guideStatusCol.setCellValueFactory(c -> new SimpleStringProperty(statusLabel(c.getValue().getStatus())));
        if (guideCreatedCol != null) guideCreatedCol.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
        if (guideActionsCol != null) guideActionsCol.setCellFactory(col -> createGuideActionsCell(false));

        if (pendingTitleCol != null) pendingTitleCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTitle())));
        if (pendingGameCol != null) pendingGameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGame() != null ? safe(c.getValue().getGame().getName()) : "-"));
        if (pendingAgentCol != null) pendingAgentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAgent() != null ? safe(c.getValue().getAgent().getName()) : "-"));
        if (pendingUploaderCol != null) pendingUploaderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUploadedBy() != null ? safe(c.getValue().getUploadedBy().getUsername()) : "-"));
        if (pendingCreatedCol != null) pendingCreatedCol.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getCreatedAt())));
        if (pendingActionsCol != null) pendingActionsCol.setCellFactory(col -> createGuideActionsCell(true));
    }

    private void bindSearchFields() {
        if (gameSearchField != null) gameSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyGameFilters());
        if (agentSearchField != null) agentSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyAgentFilters());
        if (guideSearchField != null) guideSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyGuideFilters());
        if (guideStatusFilter != null) guideStatusFilter.setOnAction(e -> applyGuideFilters());
        if (guideGameFilter != null) guideGameFilter.setOnAction(e -> applyGuideFilters());
        if (pendingSearchField != null) pendingSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyPendingFilters());
        if (pendingGameFilter != null) pendingGameFilter.setOnAction(e -> applyPendingFilters());
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<Game> games = gameRepository.findAllOrderedByName();
                List<Agent> agents = agentRepository.findAll();
                List<GuideVideo> guides = guideVideoRepository.findAll();
                List<GuideVideo> pending = guideVideoRepository.findPending();

                Platform.runLater(() -> {
                    allGames = games;
                    allAgents = agents;
                    allGuides = guides;
                    pendingGuides = pending;

                    refreshGameFilterOptions();
                    refreshPendingFilterOptions();
                    refreshDashboardCounts();
                    applyGameFilters();
                    applyAgentFilters();
                    applyGuideFilters();
                    applyPendingFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Load error", e.getMessage()));
            }
        }, "LoadGuideAdminData").start();
    }

    private void refreshDashboardCounts() {
        if (gamesCountLabel != null) gamesCountLabel.setText(String.valueOf(allGames.size()));
        if (agentsCountLabel != null) agentsCountLabel.setText(String.valueOf(allAgents.size()));
        if (guidesCountLabel != null) guidesCountLabel.setText(String.valueOf(allGuides.size()));

        long pending = allGuides.stream().filter(g -> "pending".equalsIgnoreCase(safe(g.getStatus()))).count();
        long approved = allGuides.stream().filter(g -> "approved".equalsIgnoreCase(safe(g.getStatus()))).count();
        long rejected = allGuides.stream().filter(g -> "rejected".equalsIgnoreCase(safe(g.getStatus()))).count();

        if (pendingCountLabel != null) pendingCountLabel.setText(String.valueOf(pending));
        if (approvedCountLabel != null) approvedCountLabel.setText(String.valueOf(approved));
        if (rejectedCountLabel != null) rejectedCountLabel.setText(String.valueOf(rejected));
    }

    private void refreshGameFilterOptions() {
        if (guideGameFilter == null && pendingGameFilter == null) return;

        List<String> values = new ArrayList<>();
        values.add("All games");
        for (Game game : allGames) {
            String name = game != null ? game.getName() : null;
            if (name != null && !name.isBlank() && !values.contains(name)) {
                values.add(name);
            }
        }

        if (guideGameFilter != null) {
            guideGameFilter.setItems(FXCollections.observableArrayList(values));
            guideGameFilter.getSelectionModel().selectFirst();
        }
        if (pendingGameFilter != null) {
            pendingGameFilter.setItems(FXCollections.observableArrayList(values));
            pendingGameFilter.getSelectionModel().selectFirst();
        }
    }

    private void refreshPendingFilterOptions() {
        if (pendingGameFilter == null) return;
        if (pendingGameFilter.getItems().isEmpty()) {
            pendingGameFilter.setItems(FXCollections.observableArrayList("All games"));
            pendingGameFilter.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void refreshAll() {
        loadData();
    }

    @FXML
    public void addGame() {
        Game game = openGameDialog(null);
        if (game == null) return;
        gameRepository.save(game);
        refreshAll();
    }

    @FXML
    public void addAgent() {
        if (allGames.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Missing games", "Create at least one game before adding agents.");
            return;
        }
        Agent agent = openAgentDialog(null);
        if (agent == null) return;
        agentRepository.save(agent);
        refreshAll();
    }

    @FXML
    public void addGuide() {
        if (allGames.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Missing games", "Create at least one game before adding guides.");
            return;
        }
        GuideVideo guide = openGuideDialog(null);
        if (guide == null) return;
        guideVideoRepository.save(guide);
        notifyAdminsAboutPendingGuide(guide);
        refreshAll();
    }

    private void notifyAdminsAboutPendingGuide(GuideVideo guide) {
        User uploader = guide != null ? guide.getUploadedBy() : null;
        if (uploader == null) {
            return;
        }

        List<User> admins = userService.getAllUsers().stream()
                .filter(user -> user != null && user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> user.isAdmin() || user.isSuperAdmin())
                .toList();

        if (admins.isEmpty()) {
            return;
        }

        String uploaderName = uploader.getFullName() != null && !uploader.getFullName().isBlank()
                ? uploader.getFullName()
                : uploader.getUsername();

        String gameName = guide.getGame() != null ? guide.getGame().getName() : null;
        String agentName = guide.getAgent() != null ? guide.getAgent().getName() : null;

        admins.forEach(admin -> EmailService.getInstance().sendGuideApprovalRequestEmail(
                admin.getEmail(),
                uploaderName,
                uploader.getEmail(),
                guide.getTitle(),
                gameName,
                agentName,
                guide.getMap()));
    }

    @FXML
    public void showDashboardTab() { selectTabById("dashboardTab"); }

    @FXML
    public void showGamesTab() { selectTabById("gamesTab"); }

    @FXML
    public void showAgentsTab() { selectTabById("agentsTab"); }

    @FXML
    public void showGuidesTab() { selectTabById("guidesTab"); }

    @FXML
    public void showPendingTab() { selectTabById("pendingTab"); }

    private void applyGameFilters() {
        String query = safeLower(gameSearchField != null ? gameSearchField.getText() : null);
        List<Game> filtered = allGames.stream()
                .filter(game -> matchesQuery(game.getName(), query)
                        || matchesQuery(game.getSlug(), query)
                        || matchesQuery(game.getDescription(), query))
                .toList();

        if (gamesTable != null) gamesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyAgentFilters() {
        String query = safeLower(agentSearchField != null ? agentSearchField.getText() : null);
        List<Agent> filtered = allAgents.stream()
                .filter(agent -> matchesQuery(agent.getName(), query)
                        || matchesQuery(agent.getSlug(), query)
                        || (agent.getGame() != null && matchesQuery(agent.getGame().getName(), query)))
                .toList();

        if (agentsTable != null) agentsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyGuideFilters() {
        String query = safeLower(guideSearchField != null ? guideSearchField.getText() : null);
        String status = guideStatusFilter != null && guideStatusFilter.getValue() != null ? guideStatusFilter.getValue() : "All status";
        String game = guideGameFilter != null && guideGameFilter.getValue() != null ? guideGameFilter.getValue() : "All games";

        List<GuideVideo> filtered = allGuides.stream()
                .filter(guide -> {
                    boolean queryMatch = matchesQuery(guide.getTitle(), query)
                            || matchesQuery(guide.getMap(), query)
                            || (guide.getUploadedBy() != null && matchesQuery(guide.getUploadedBy().getUsername(), query))
                            || (guide.getGame() != null && matchesQuery(guide.getGame().getName(), query))
                            || (guide.getAgent() != null && matchesQuery(guide.getAgent().getName(), query));
                    boolean statusMatch = "All status".equalsIgnoreCase(status) || safe(guide.getStatus()).equalsIgnoreCase(status);
                    boolean gameMatch = "All games".equalsIgnoreCase(game) || (guide.getGame() != null && safe(guide.getGame().getName()).equalsIgnoreCase(game));
                    return queryMatch && statusMatch && gameMatch;
                })
                .toList();

        if (guidesTable != null) guidesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void applyPendingFilters() {
        String query = safeLower(pendingSearchField != null ? pendingSearchField.getText() : null);
        String game = pendingGameFilter != null && pendingGameFilter.getValue() != null ? pendingGameFilter.getValue() : "All games";

        List<GuideVideo> filtered = pendingGuides.stream()
                .filter(guide -> {
                    boolean queryMatch = matchesQuery(guide.getTitle(), query)
                            || (guide.getUploadedBy() != null && matchesQuery(guide.getUploadedBy().getUsername(), query))
                            || (guide.getAgent() != null && matchesQuery(guide.getAgent().getName(), query));
                    boolean gameMatch = "All games".equalsIgnoreCase(game) || (guide.getGame() != null && safe(guide.getGame().getName()).equalsIgnoreCase(game));
                    return queryMatch && gameMatch;
                })
                .toList();

        if (pendingTable != null) pendingTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private TableCell<Game, Void> createGameActionsCell() {
        return new TableCell<>() {
            private final Button editBtn = makeSmallButton("Edit", "#4facfe");
            private final Button deleteBtn = makeSmallButton("Delete", "#ff3c64");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> {
                    Game game = getTableRow() != null ? getTableRow().getItem() : null;
                    if (game == null) return;
                    Game updated = openGameDialog(game);
                    if (updated != null) {
                        gameRepository.update(updated);
                        refreshAll();
                    }
                });
                deleteBtn.setOnAction(e -> {
                    Game game = getTableRow() != null ? getTableRow().getItem() : null;
                    if (game == null) return;
                    if (confirm("Delete game", "Delete '" + safe(game.getName()) + "'?")) {
                        gameRepository.delete(game.getId());
                        refreshAll();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private TableCell<Agent, Void> createAgentActionsCell() {
        return new TableCell<>() {
            private final Button editBtn = makeSmallButton("Edit", "#4facfe");
            private final Button deleteBtn = makeSmallButton("Delete", "#ff3c64");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> {
                    Agent agent = getTableRow() != null ? getTableRow().getItem() : null;
                    if (agent == null) return;
                    Agent updated = openAgentDialog(agent);
                    if (updated != null) {
                        agentRepository.update(updated);
                        refreshAll();
                    }
                });
                deleteBtn.setOnAction(e -> {
                    Agent agent = getTableRow() != null ? getTableRow().getItem() : null;
                    if (agent == null) return;
                    if (confirm("Delete agent", "Delete '" + safe(agent.getName()) + "'?")) {
                        agentRepository.delete(agent.getId());
                        refreshAll();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private TableCell<GuideVideo, Void> createGuideActionsCell(boolean pendingOnly) {
        return new TableCell<>() {
            private final Button openBtn = makeSmallButton("Open", "#818cf8");
            private final Button editBtn = makeSmallButton("Edit", "#4facfe");
            private final Button approveBtn = makeSmallButton("Approve", "#43e97b");
            private final Button rejectBtn = makeSmallButton("Reject", "#f6d365");
            private final Button deleteBtn = makeSmallButton("Delete", "#ff3c64");
            private final HBox box = new HBox(6);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(openBtn, editBtn, approveBtn, rejectBtn, deleteBtn);

                openBtn.setOnAction(e -> {
                    GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                    if (guide == null) return;
                    openGuideVideo(guide);
                });
                editBtn.setOnAction(e -> {
                    GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                    if (guide == null) return;
                    String previousStatus = safe(guide.getStatus()).toLowerCase(Locale.ROOT);
                    GuideVideo updated = openGuideDialog(guide);
                    if (updated != null) {
                        guideVideoRepository.update(updated);
                        String newStatus = safe(updated.getStatus()).toLowerCase(Locale.ROOT);
                        boolean decisionChanged = !previousStatus.equals(newStatus)
                                && ("approved".equals(newStatus) || "rejected".equals(newStatus));
                        if (decisionChanged) {
                            String reason = "rejected".equals(newStatus)
                                    ? "Your guide was reviewed and needs improvements before publication."
                                    : null;
                            notifyUploaderAboutGuideDecision(updated, newStatus, reason);
                        }
                        refreshAll();
                    }
                });
                approveBtn.setOnAction(e -> {
                    GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                    if (guide == null) return;
                    if (confirm("Approve guide", "Approve '" + safe(guide.getTitle()) + "'?")) {
                        guide.setStatus("approved");
                        guide.setApprovedAt(LocalDateTime.now());
                        guideVideoRepository.update(guide);
                        notifyUploaderAboutGuideDecision(guide, "approved", null);
                        refreshAll();
                    }
                });
                rejectBtn.setOnAction(e -> {
                    GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                    if (guide == null) return;
                    if (confirm("Reject guide", "Reject '" + safe(guide.getTitle()) + "'?")) {
                        guide.setStatus("rejected");
                        guide.setApprovedAt(null);
                        guideVideoRepository.update(guide);
                        notifyUploaderAboutGuideDecision(guide, "rejected", "Please review title, content quality, or video relevance.");
                        refreshAll();
                    }
                });
                deleteBtn.setOnAction(e -> {
                    GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                    if (guide == null) return;
                    if (confirm("Delete guide", "Delete '" + safe(guide.getTitle()) + "'?")) {
                        guideVideoRepository.delete(guide);
                        refreshAll();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                GuideVideo guide = getTableRow() != null ? getTableRow().getItem() : null;
                boolean pending = guide != null && "pending".equalsIgnoreCase(safe(guide.getStatus()));
                approveBtn.setDisable(!pending && pendingOnly);
                rejectBtn.setDisable(!pending && pendingOnly);
                setGraphic(box);
            }
        };
    }

    private Game openGameDialog(Game base) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "🎮 New Game" : "✏️ Edit Game");
        dialog.setHeaderText(base == null ? 
            "Create a new game for your platform" : 
            "Update game information");
        dialog.setResizable(true);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f0f1a;");

        TextField nameField = new TextField(base != null ? safe(base.getName()) : "");
        TextField slugField = new TextField(base != null ? safe(base.getSlug()) : "");
        TextField iconField = new TextField(base != null ? safe(base.getIcon()) : "");
        ColorPicker colorPicker = new ColorPicker(parseColor(base != null ? base.getColor() : null, "#0a8cc9"));
        TextArea descriptionArea = new TextArea(base != null ? safe(base.getDescription()) : "");

        nameField.setPromptText("e.g., FPS, RPG, Strategy");
        slugField.setPromptText("Auto-generated from name");
        iconField.setPromptText("https://example.com/icon.png");
        descriptionArea.setPromptText("Brief description of the game type...");

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (base == null && slugField.getText().isBlank()) {
                slugField.setText(slugify(newVal));
            }
        });

        VBox formContent = new VBox(16);
        formContent.setPadding(new Insets(20));
        formContent.setStyle("-fx-background-color: #0f0f1a;");
        
        // Section: Basic Info
        Label basicLabel = new Label("📋 Basic Information");
        basicLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #00d4ff;");
        formContent.getChildren().addAll(
            basicLabel,
            row("Name", nameField),
            row("Slug", slugField)
        );
        
        // Section: Appearance
        Label appearanceLabel = new Label("🎨 Appearance");
        appearanceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            appearanceLabel,
            row("Icon URL", iconField),
            row("Color", colorPicker)
        );
        
        // Section: Details
        Label detailsLabel = new Label("📝 Details");
        detailsLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #00d4ff; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            detailsLabel,
            row("Description", descriptionArea)
        );

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setStyle("-fx-background-color: #0f0f1a; -fx-control-inner-background: #0f0f1a;");
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return null;
        }

        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Game name cannot be empty.");
            return openGameDialog(base);
        }

        Game game = base != null ? base : new Game();
        game.setName(nameField.getText().trim());
        game.setSlug(slugField.getText().trim().isEmpty() ? slugify(nameField.getText()) : slugField.getText().trim());
        game.setIcon(iconField.getText().trim().isEmpty() ? null : iconField.getText().trim());
        game.setColor(toHex(colorPicker.getValue()));
        game.setDescription(descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText().trim());
        if (game.getCreatedAt() == null) {
            game.setCreatedAt(LocalDateTime.now());
        }
        return game;
    }

    private Agent openAgentDialog(Agent base) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "👤 New Agent" : "✏️ Edit Agent");
        dialog.setHeaderText(base == null ? 
            "Create a new agent character for the platform" : 
            "Update agent information");
        dialog.setResizable(true);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f0f1a;");

        TextField nameField = new TextField(base != null ? safe(base.getName()) : "");
        TextField slugField = new TextField(base != null ? safe(base.getSlug()) : "");
        TextField imageField = new TextField(base != null ? safe(base.getImage()) : "");
        TextArea descriptionArea = new TextArea(base != null ? safe(base.getDescription()) : "");
        
        ComboBox<Game> gameCombo = new ComboBox<>(FXCollections.observableArrayList(allGames));
        gameCombo.setConverter(simpleGameConverter());
        if (base != null && base.getGame() != null) {
            gameCombo.getSelectionModel().select(findGame(base.getGame().getId()));
        } else {
            gameCombo.getSelectionModel().selectFirst();
        }

        nameField.setPromptText("e.g., Phoenix, Reyna, Sage");
        slugField.setPromptText("Auto-generated from name");
        imageField.setPromptText("https://example.com/agent.png");
        descriptionArea.setPromptText("Agent abilities, role, and characteristics...");

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (base == null && slugField.getText().isBlank()) {
                slugField.setText(slugify(newVal));
            }
        });

        VBox formContent = new VBox(16);
        formContent.setPadding(new Insets(20));
        formContent.setStyle("-fx-background-color: #0f0f1a;");
        
        // Section: Game Link
        Label gameLabel = new Label("🎮 Game Association");
        gameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff6b9d;");
        formContent.getChildren().addAll(
            gameLabel,
            row("Game", gameCombo)
        );
        
        // Section: Agent Info
        Label agentLabel = new Label("👤 Agent Information");
        agentLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff6b9d; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            agentLabel,
            row("Name", nameField),
            row("Slug", slugField)
        );
        
        // Section: Media
        Label mediaLabel = new Label("🖼️ Media");
        mediaLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff6b9d; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            mediaLabel,
            row("Image URL", imageField)
        );
        
        // Section: Details
        Label detailsLabel = new Label("📝 Details");
        detailsLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff6b9d; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            detailsLabel,
            row("Description", descriptionArea)
        );

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setStyle("-fx-background-color: #0f0f1a; -fx-control-inner-background: #0f0f1a;");
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return null;
        }

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Agent name is required.");
            return openAgentDialog(base);
        }

        Game selectedGame = gameCombo.getValue();
        if (selectedGame == null) {
            showAlert(Alert.AlertType.WARNING, "Missing game", "Please select a game.");
            return openAgentDialog(base);
        }

        Agent agent = base != null ? base : new Agent();
        agent.setGame(selectedGame);
        agent.setName(nameField.getText().trim());
        agent.setSlug(slugField.getText().trim().isEmpty() ? slugify(nameField.getText()) : slugField.getText().trim());
        agent.setImage(imageField.getText().trim().isEmpty() ? null : imageField.getText().trim());
        agent.setDescription(descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText().trim());
        if (agent.getCreatedAt() == null) {
            agent.setCreatedAt(LocalDateTime.now());
        }
        return agent;
    }

    private GuideVideo openGuideDialog(GuideVideo base) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "🎥 New Guide Video" : "✏️ Edit Guide Video");
        dialog.setHeaderText(base == null ? 
            "Create a comprehensive video guide for your community" : 
            "Update guide video information");
        dialog.setResizable(true);
        
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f0f1a;");

        TextField titleField = new TextField(base != null ? safe(base.getTitle()) : "");
        TextField mapField = new TextField(base != null ? safe(base.getMap()) : "All");
        TextField videoUrlField = new TextField(base != null ? safe(base.getVideoUrl()) : "");
        TextField thumbnailField = new TextField(base != null ? safe(base.getThumbnail()) : "");
        TextArea descriptionArea = new TextArea(base != null ? safe(base.getDescription()) : "");

        ComboBox<Game> gameCombo = new ComboBox<>(FXCollections.observableArrayList(allGames));
        gameCombo.setConverter(simpleGameConverter());
        if (base != null && base.getGame() != null) {
            gameCombo.getSelectionModel().select(findGame(base.getGame().getId()));
        } else {
            gameCombo.getSelectionModel().selectFirst();
        }

        ComboBox<Agent> agentCombo = new ComboBox<>();
        agentCombo.setConverter(simpleAgentConverter());
        refreshAgentsForGame(gameCombo.getValue(), agentCombo);

        if (base != null && base.getAgent() != null) {
            agentCombo.getItems().stream().filter(a -> a.getId() == base.getAgent().getId()).findFirst().ifPresent(agentCombo.getSelectionModel()::select);
        } else {
            agentCombo.getSelectionModel().clearSelection();
        }

        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("pending", "approved", "rejected"));
        statusCombo.getSelectionModel().select(base != null ? safe(base.getStatus()) : "pending");

        titleField.setPromptText("E.g., 'Clutch Ace Tutorial - Advanced Tactics'");
        mapField.setPromptText("E.g., 'Bind' or 'All' for all maps");
        videoUrlField.setPromptText("Your video URL or upload link");
        descriptionArea.setPromptText("Detailed guide description, tips, and strategies...");

        gameCombo.setOnAction(e -> refreshAgentsForGame(gameCombo.getValue(), agentCombo));

        VBox formContent = new VBox(16);
        formContent.setPadding(new Insets(20));
        formContent.setStyle("-fx-background-color: #0f0f1a;");
        
        // Section: Basic Info
        Label basicLabel = new Label("📋 Basic Information");
        basicLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #9d4edd;");
        formContent.getChildren().addAll(
            basicLabel,
            row("Title", titleField)
        );
        
        // Section: Game & Agent
        Label gameLabel = new Label("🎮 Game & Agent");
        gameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #9d4edd; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            gameLabel,
            row("Game", gameCombo),
            row("Agent", agentCombo),
            row("Map", mapField)
        );
        
        // Section: Media
        Label mediaLabel = new Label("📹 Media");
        mediaLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #9d4edd; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            mediaLabel,
            row("Video URL", videoUrlField),
            row("Thumbnail URL", thumbnailField)
        );
        
        // Section: Details
        Label statusLabel = new Label("⚙️ Status & Details");
        statusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #9d4edd; -fx-padding: 12 0 0 0;");
        formContent.getChildren().addAll(
            statusLabel,
            row("Status", statusCombo),
            row("Description", descriptionArea)
        );

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setStyle("-fx-background-color: #0f0f1a; -fx-control-inner-background: #0f0f1a;");
        scrollPane.setFitToWidth(true);
        dialog.getDialogPane().setContent(scrollPane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return null;
        }

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Guide title is required!");
            return openGuideDialog(base);
        }

        if (videoUrlField.getText() == null || videoUrlField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Video URL is required!");
            return openGuideDialog(base);
        }

        Game selectedGame = gameCombo.getValue();
        if (selectedGame == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Selection", "Please select a game.");
            return openGuideDialog(base);
        }

        GuideVideo guide = base != null ? base : new GuideVideo();
        guide.setTitle(titleField.getText().trim());
        guide.setGame(selectedGame);
        guide.setAgent(agentCombo.getValue());
        guide.setMap(mapField.getText().trim().isEmpty() ? "All" : mapField.getText().trim());
        guide.setVideoUrl(videoUrlField.getText().trim());
        guide.setThumbnail(thumbnailField.getText().trim().isEmpty() ? null : thumbnailField.getText().trim());
        guide.setDescription(descriptionArea.getText().trim().isEmpty() ? null : descriptionArea.getText().trim());
        guide.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "pending");
        if (guide.getUploadedBy() == null) {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null) {
                guide.setUploadedBy(currentUser);
            }
        }
        if (guide.getCreatedAt() == null) {
            guide.setCreatedAt(LocalDateTime.now());
        }
        if ("approved".equalsIgnoreCase(guide.getStatus())) {
            guide.setApprovedAt(base != null && base.getApprovedAt() != null ? base.getApprovedAt() : LocalDateTime.now());
        } else {
            guide.setApprovedAt(null);
        }
        return guide;
    }

    private void refreshAgentsForGame(Game selectedGame, ComboBox<Agent> agentCombo) {
        if (agentCombo == null) return;
        Integer selectedAgentId = agentCombo.getValue() != null ? agentCombo.getValue().getId() : null;
        List<Agent> agents = selectedGame == null ? allAgents : allAgents.stream()
                .filter(agent -> agent.getGame() != null && agent.getGame().getId() == selectedGame.getId())
                .toList();
        agentCombo.setItems(FXCollections.observableArrayList(agents));
        if (selectedAgentId != null) {
            agentCombo.getItems().stream()
                    .filter(agent -> agent.getId() == selectedAgentId)
                    .findFirst()
                    .ifPresentOrElse(
                            a -> agentCombo.getSelectionModel().select(a),
                            () -> agentCombo.getSelectionModel().clearSelection()
                    );
        } else {
            agentCombo.getSelectionModel().clearSelection();
        }
    }

    private void openGuideVideo(GuideVideo guide) {
        String url = guide != null ? guide.getVideoUrl() : null;
        if (url == null || url.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Missing video", "This guide does not have a video URL.");
            return;
        }

        try {
            showVideoViewer(url, guide != null ? guide.getTitle() : "Guide Video");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Open video error", e.getMessage());
        }
    }

    private void notifyUploaderAboutGuideDecision(GuideVideo guide, String decision, String reason) {
        if (guide == null || guide.getUploadedBy() == null) {
            return;
        }

        String uploaderEmail = guide.getUploadedBy().getEmail();
        if (uploaderEmail == null || uploaderEmail.isBlank()) {
            return;
        }

        String uploaderName = guide.getUploadedBy().getFullName();
        if (uploaderName == null || uploaderName.isBlank()) {
            uploaderName = safe(guide.getUploadedBy().getUsername());
        }

        EmailService.getInstance().sendGuideDecisionEmail(
                uploaderEmail,
                uploaderName,
                guide.getTitle(),
                decision,
                guide.getGame() != null ? guide.getGame().getName() : null,
                reason
        );
    }

    private void showVideoViewer(String url, String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title != null && !title.isBlank() ? title : "Guide Video");

        WebView webView = new WebView();
        webView.setPrefSize(960, 540);
        webView.setStyle("-fx-background-color: black;");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        String html = """
                <!doctype html>
                <html>
                    <head>
                        <meta charset=\"utf-8\" />
                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                        <style>
                            html, body {
                                margin: 0;
                                width: 100%%;
                                height: 100%%;
                                background: #000;
                                overflow: hidden;
                            }
                            video {
                                width: 100%%;
                                height: 100%%;
                                object-fit: contain;
                                background: #000;
                            }
                        </style>
                    </head>
                    <body>
                        <video controls autoplay playsinline>
                            <source src=\"%s\" type=\"video/mp4\" />
                            Your browser does not support the video tag.
                        </video>
                    </body>
                </html>
                """.formatted(escapeHtml(url));

        engine.loadContent(html, "text/html");

        Scene scene = new Scene(new StackPane(webView), 960, 540);
        stage.setScene(scene);
        stage.show();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private VBox buildFormGrid(Node... rows) {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(24, 20, 20, 20));
        wrapper.setStyle("-fx-background-color: #1e1e2e; -fx-border-radius: 12;");
        
        for (Node row : rows) {
            wrapper.getChildren().add(row);
        }
        return wrapper;
    }

    private HBox row(String labelText, Node input) {
        Label label = new Label(labelText);
        label.setMinWidth(140);
        label.setStyle(
            "-fx-text-fill: #e0e0e0;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 13;"
        );

        VBox inputContainer = new VBox(6);
        inputContainer.setStyle("-fx-border-color: #3a3a4a; -fx-border-radius: 8; -fx-border-width: 1;");
        inputContainer.setPadding(new Insets(8, 12, 8, 12));

        // Style input fields
        if (input instanceof TextArea textArea) {
            textArea.setStyle(
                "-fx-control-inner-background: #252530;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12;" +
                "-fx-padding: 8;" +
                "-fx-border-width: 0;"
            );
            textArea.setWrapText(true);
            textArea.setPrefRowCount(4);
            inputContainer.getChildren().add(textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);
        } else if (input instanceof TextField textField) {
            textField.setStyle(
                "-fx-control-inner-background: #252530;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12;" +
                "-fx-padding: 8;" +
                "-fx-border-width: 0;" +
                "-fx-prompt-text-fill: #666666;"
            );
            textField.setPrefHeight(38);
            textField.setPromptText("Enter " + labelText.toLowerCase());
            inputContainer.getChildren().add(textField);
        } else if (input instanceof ComboBox<?> comboBox) {
            comboBox.setStyle(
                "-fx-background-color: #252530;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12;" +
                "-fx-border-width: 0;" +
                "-fx-padding: 8;"
            );
            comboBox.setPrefHeight(38);
            inputContainer.getChildren().add(comboBox);
        } else if (input instanceof ColorPicker colorPicker) {
            colorPicker.setStyle("-fx-padding: 8;");
            colorPicker.setPrefHeight(38);
            inputContainer.getChildren().add(colorPicker);
        } else {
            inputContainer.getChildren().add(input);
        }

        HBox.setHgrow(inputContainer, Priority.ALWAYS);
        HBox row = new HBox(12, label, inputContainer);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPrefHeight(Region.USE_COMPUTED_SIZE);

        return row;
    }

    private Button makeSmallButton(String text, String accent) {
        Button button = new Button(text);
        button.setCursor(Cursor.HAND);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.05);" +
                "-fx-border-color: " + accent + ";" +
                "-fx-border-radius: 8; -fx-background-radius: 8;" +
                "-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold;" +
                "-fx-padding: 5 10;");
        return button;
    }

    private Game findGame(int id) {
        return allGames.stream().filter(game -> game.getId() == id).findFirst().orElse(null);
    }

    private StringConverter<Game> simpleGameConverter() {
        return new StringConverter<>() {
            @Override public String toString(Game object) { return object != null ? safe(object.getName()) : ""; }
            @Override public Game fromString(String string) { return findGameByName(string); }
        };
    }

    private StringConverter<Agent> simpleAgentConverter() {
        return new StringConverter<>() {
            @Override public String toString(Agent object) { return object != null ? safe(object.getName()) : ""; }
            @Override public Agent fromString(String string) { return findAgentByName(string); }
        };
    }

    private Game findGameByName(String name) {
        if (name == null) return null;
        return allGames.stream().filter(game -> safe(game.getName()).equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    private Agent findAgentByName(String name) {
        if (name == null) return null;
        return allAgents.stream().filter(agent -> safe(agent.getName()).equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    private void selectTabById(String tabId) {
        if (guideTabPane == null || tabId == null) return;
        Tab target = switch (tabId) {
            case "dashboardTab" -> dashboardTab;
            case "gamesTab" -> gamesTab;
            case "agentsTab" -> agentsTab;
            case "guidesTab" -> guidesTab;
            case "pendingTab" -> pendingTab;
            default -> null;
        };
        if (target != null) {
            guideTabPane.getSelectionModel().select(target);
        }
    }

    private void applyThemeIfPossible() {
        if (gamesTable != null) {
            gamesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        if (agentsTable != null) {
            agentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        if (guidesTable != null) {
            guidesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        if (pendingTable != null) {
            pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeLower(String value) {
        return safe(value).toLowerCase(Locale.ROOT).trim();
    }

    private boolean matchesQuery(String value, String query) {
        return query == null || query.isBlank() || safeLower(value).contains(query);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMAT.format(dateTime);
    }

    private String statusLabel(String status) {
        if (status == null) return "Pending";
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "approved" -> "Approved";
            case "rejected" -> "Rejected";
            default -> "Pending";
        };
    }

    private String slugify(String value) {
        if (value == null) return "";
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? value.toLowerCase(Locale.ROOT) : slug;
    }

    private Color parseColor(String value, String fallback) {
        try {
            return Color.web(value == null || value.isBlank() ? fallback : value);
        } catch (Exception e) {
            return Color.web(fallback);
        }
    }

    private String toHex(Color color) {
        if (color == null) return "#0a8cc9";
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private void navigateTo(String fxml) {
        if (adminSidebarController != null) {
            adminSidebarController.navigateTo(fxml);
        }
    }

    private void showAlert(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void applyThemeIfPossibleLater() {
        Platform.runLater(this::applyThemeIfPossible);
    }
}
