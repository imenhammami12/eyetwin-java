package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.PlanningLevel;
import com.eyetwin.entities.PlanningType;
import com.eyetwin.entities.Review;
import com.eyetwin.entities.TrainingSession;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IPlanningService;
import com.eyetwin.interfaces.IReviewService;
import com.eyetwin.interfaces.ITrainingSessionService;
import com.eyetwin.services.AiCoachService;
import com.eyetwin.services.ChatbotService;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.services.ReviewServiceImpl;
import com.eyetwin.services.TrainingSessionServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PlanningController {

    private static final String VIEWS = "/com/eyetwin/views/";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    // ── Root views ────────────────────────────────────────────
    @FXML private StackPane mainStack;
    @FXML private VBox viewDashboard;
    @FXML private VBox viewList;
    @FXML private VBox viewShow;
    @FXML private VBox viewCreate;
    @FXML private VBox viewEdit;

    // ── Dashboard ──────────────────────────────────────────────
    @FXML private Label welcomeLabel;

    // ── List view ──────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterLevelCombo;
    @FXML private HBox filtersBar;
    @FXML private FlowPane allPlanningsPane;
    @FXML private VBox sectionAvailableSessions;
    @FXML private VBox sectionMySessions;
    @FXML private VBox mySessionsList;
    @FXML private VBox sectionMyHistory;
    @FXML private VBox myHistoryList;

    // ── AI coach banner ────────────────────────────────────────
    @FXML private HBox coachBanner;
    @FXML private Label coachRecommendationLabel;

    // ── Chatbot UI (planning list) ──────────────────────────────
    @FXML private StackPane chatbotContainer;
    @FXML private Button chatbotFabBtn;
    @FXML private VBox chatbotWindow;
    @FXML private ScrollPane chatbotScroll;
    @FXML private VBox chatbotMessages;
    @FXML private TextField chatbotInput;
    @FXML private Button chatbotSendBtn;
    @FXML private Label chatbotStatus;
    @FXML private Button chatbotQuickSessionsBtn;
    @FXML private Button chatbotQuickNextBtn;
    @FXML private Button chatbotQuickWhatBtn;

    // ── Show view ──────────────────────────────────────────────
    @FXML private ImageView showImage;
    @FXML private Label showDate;
    @FXML private Label showTime;
    @FXML private Label showLocalisation;
    @FXML private Label showLevel;
    @FXML private Label showType;
    @FXML private Label showNeedPartner;
    @FXML private Label showDescription;
    @FXML private Label showParticipantCount;
    @FXML private Label showAverageRating;
    @FXML private VBox participantsList;
    @FXML private VBox mapContainer;
    @FXML private WebView mapWebView;
    @FXML private Button showEditBtn;
    @FXML private Button showDeleteBtn;

    // ── Feedback modal (rating) ────────────────────────────────
    @FXML private StackPane feedbackOverlay;
    @FXML private HBox feedbackStarsRow;
    @FXML private TextArea feedbackTextArea;
    @FXML private Button feedbackSendBtn;
    @FXML private Label feedbackErrorLabel;

    // ── Join modal (registration) ──────────────────────────────
    @FXML private StackPane joinOverlay;
    @FXML private ImageView joinImage;
    @FXML private Label joinTypeLabel;
    @FXML private Label joinTitleLabel;
    @FXML private Label joinLevelLabel;
    @FXML private Label joinDateLabel;
    @FXML private Label joinTimeLabel;
    @FXML private Label joinLocationLabel;
    @FXML private TextField joinParticipantField;
    @FXML private TextField joinRegDateTimeField;
    @FXML private Label joinStatusPill;
    @FXML private Button joinConfirmBtn;
    @FXML private Label joinErrorLabel;
    private Planning joinPlanning;

    // ── Create view ────────────────────────────────────────────
    @FXML private DatePicker createDatePicker;
    @FXML private TextField createTimeField;
    @FXML private TextField createLocalisationField;
    @FXML private TextArea createDescriptionArea;
    @FXML private ComboBox<String> createLevelCombo;
    @FXML private ComboBox<String> createTypeCombo;
    @FXML private CheckBox createNeedPartnerCheck;
    @FXML private Label createImageLabel;
    @FXML private Label createGeneralError;

    // ── Edit view ──────────────────────────────────────────────
    @FXML private DatePicker editDatePicker;
    @FXML private TextField editTimeField;
    @FXML private TextField editLocalisationField;
    @FXML private TextArea editDescriptionArea;
    @FXML private ComboBox<String> editLevelCombo;
    @FXML private ComboBox<String> editTypeCombo;
    @FXML private CheckBox editNeedPartnerCheck;
    @FXML private Label editImageLabel;
    @FXML private Label editGeneralError;

    private final IPlanningService planningService = new PlanningServiceImpl();
    private final ITrainingSessionService sessionService = new TrainingSessionServiceImpl();
    private final IReviewService reviewService = new ReviewServiceImpl();
    private final AiCoachService aiCoachService = new AiCoachService();
    private final ChatbotService chatbotService = new ChatbotService();

    private Planning selectedPlanning;
    private boolean showAllMode = true;
    private Planning feedbackPlanning;
    private int feedbackRating = 0;

    private File createSelectedImageFile;
    private File editSelectedImageFile;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            MainApp.navigateTo(VIEWS + "login.fxml", "Login");
            return;
        }

        if (welcomeLabel != null) {
            String name = user.getUsername() != null ? user.getUsername() : "Player";
            welcomeLabel.setText("Welcome, " + name);
        }

        setupCombos();
        showView(viewDashboard);
        setupChatbotUi();
        // Chatbot must exist only in "View All Planning"
        setVisibleManaged(chatbotContainer, false);
    }

    private void setupChatbotUi() {
        if (chatbotInput != null) {
            chatbotInput.setOnAction(e -> handleChatbotSend());
        }
        // Avoid missing-icon fonts: use simple glyphs
        if (chatbotFabBtn != null) chatbotFabBtn.setText("💬");
        if (chatbotSendBtn != null) chatbotSendBtn.setText("➤");

        // Force correct placement: bottom-right with margin (next to the white scrollbar arrow)
        if (chatbotContainer != null) {
            StackPane.setAlignment(chatbotContainer, Pos.BOTTOM_RIGHT);
            // Tight to the corner (just left of the scrollbar down-arrow)
            StackPane.setMargin(chatbotContainer, new Insets(0, 12, 12, 0));
            // CRITICAL: prevent container from stretching (otherwise bubble ends up centered)
            chatbotContainer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            chatbotContainer.setPickOnBounds(false);
            Platform.runLater(chatbotContainer::toFront);
        }
        if (chatbotFabBtn != null) {
            StackPane.setAlignment(chatbotFabBtn, Pos.BOTTOM_RIGHT);
        }
        if (chatbotWindow != null) {
            StackPane.setAlignment(chatbotWindow, Pos.BOTTOM_RIGHT);
        }
    }

    @FXML
    public void handleToggleChatbot() {
        if (chatbotWindow == null) return;
        boolean next = !chatbotWindow.isVisible();
        chatbotWindow.setVisible(next);
        chatbotWindow.setManaged(next);
        if (chatbotFabBtn != null) {
            chatbotFabBtn.setVisible(!next);
            chatbotFabBtn.setManaged(!next);
        }
        if (next) {
            ensureGreeting();
            if (chatbotInput != null) chatbotInput.requestFocus();
            scrollChatToBottom();
        }
    }

    @FXML
    public void handleChatbotSend() {
        if (chatbotInput == null || chatbotMessages == null) return;
        String q = chatbotInput.getText() != null ? chatbotInput.getText().trim() : "";
        if (q.isBlank()) return;

        chatbotInput.clear();
        addChatBubble(q, true);
        setChatBusy(true, "Thinking...");

        chatbotService.getResponse(q).thenAccept(resp -> Platform.runLater(() -> {
            addChatBubble(resp, false);
            setChatBusy(false, "");
            scrollChatToBottom();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                addChatBubble("Sorry, I couldn't answer right now.", false);
                setChatBusy(false, "");
                scrollChatToBottom();
            });
            return null;
        });
    }

    private void ensureGreeting() {
        if (chatbotMessages == null) return;
        if (!chatbotMessages.getChildren().isEmpty()) return;
        addChatBubble("Bonjour ! 👋 Je suis votre assistant virtuel. Je peux vous aider avec des questions sur nos sessions d'entraînement et l'e-sport en général. Comment puis-je vous aider ?", false);
    }

    @FXML
    public void handleChatbotQuickSessions() {
        if (chatbotWindow != null && !chatbotWindow.isVisible()) handleToggleChatbot();
        if (chatbotInput != null) chatbotInput.setText("Sessions disponibles");
        handleChatbotSend();
    }

    @FXML
    public void handleChatbotQuickNext() {
        if (chatbotWindow != null && !chatbotWindow.isVisible()) handleToggleChatbot();
        if (chatbotInput != null) chatbotInput.setText("Prochaine session");
        handleChatbotSend();
    }

    @FXML
    public void handleChatbotQuickWhat() {
        if (chatbotWindow != null && !chatbotWindow.isVisible()) handleToggleChatbot();
        if (chatbotInput != null) chatbotInput.setText("C'est quoi l'e-sport ?");
        handleChatbotSend();
    }

    private void setChatBusy(boolean busy, String statusText) {
        if (chatbotSendBtn != null) chatbotSendBtn.setDisable(busy);
        if (chatbotInput != null) chatbotInput.setDisable(busy);
        if (chatbotStatus != null) {
            chatbotStatus.setText(statusText == null ? "" : statusText);
            boolean show = statusText != null && !statusText.isBlank();
            chatbotStatus.setVisible(show);
            chatbotStatus.setManaged(show);
        }
    }

    private void scrollChatToBottom() {
        if (chatbotScroll == null) return;
        Platform.runLater(() -> chatbotScroll.setVvalue(1.0));
    }

    private void addChatBubble(String text, boolean fromUser) {
        if (chatbotMessages == null) return;
        Label msg = new Label(text == null ? "" : text);
        msg.setWrapText(true);
        msg.setMaxWidth(255);

        String style = fromUser
                ? "-fx-background-color: rgba(232,55,42,0.16); -fx-border-color: rgba(232,55,42,0.30);"
                : "-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.12);";
        msg.setStyle(style + " -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 12 14; " +
                "-fx-text-fill: rgba(255,255,255,0.92); -fx-font-weight: 800;");

        HBox row = new HBox(10);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.TOP_LEFT);

        if (!fromUser) {
            Label bot = new Label("🤖");
            bot.setMinSize(34, 34);
            bot.setPrefSize(34, 34);
            bot.setAlignment(Pos.CENTER);
            bot.setStyle("-fx-background-color: rgba(232,55,42,0.18); -fx-border-color: rgba(232,55,42,0.35); " +
                    "-fx-border-radius: 999; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-weight: 900;");
            row.getChildren().addAll(bot, msg);
        } else {
            row.getChildren().add(msg);
        }
        chatbotMessages.getChildren().add(row);
    }

    // ─────────────────────────────────────────────
    //  NAV / VIEW SWITCHING
    // ─────────────────────────────────────────────
    private void showView(Pane target) {
        setVisibleManaged(viewDashboard, false);
        setVisibleManaged(viewList, false);
        setVisibleManaged(viewShow, false);
        setVisibleManaged(viewCreate, false);
        setVisibleManaged(viewEdit, false);
        setVisibleManaged(target, true);
        // Default: hide chatbot unless explicitly enabled by the current action
        setVisibleManaged(chatbotContainer, false);
    }

    private void setVisibleManaged(Pane node, boolean v) {
        if (node == null) return;
        node.setVisible(v);
        node.setManaged(v);
    }

    @FXML
    public void handleBackToDashboard() {
        showView(viewDashboard);
        // Chatbot exists only on "View All Planning"
        setVisibleManaged(chatbotContainer, false);
    }

    @FXML
    public void handleBackToList() {
        showView(viewList);
        refreshList();
    }

    // ─────────────────────────────────────────────
    //  DASHBOARD ACTIONS
    // ─────────────────────────────────────────────
    @FXML
    public void handleShowAllPlanning() {
        showView(viewList);
        setVisibleManaged(sectionMySessions, false);
        setVisibleManaged(sectionMyHistory, false);
        setVisibleManaged(sectionAvailableSessions, true);
        setVisibleManaged(filtersBar, true);
        setVisibleManaged(chatbotContainer, true);
        if (chatbotContainer != null) Platform.runLater(chatbotContainer::toFront);
        showAllMode = true;
        refreshList();
    }

    @FXML
    public void handleShowMySessions() {
        showView(viewList);
        setVisibleManaged(sectionMySessions, true);
        setVisibleManaged(sectionMyHistory, false);
        setVisibleManaged(sectionAvailableSessions, false);
        setVisibleManaged(filtersBar, false);
        setVisibleManaged(chatbotContainer, false);
        showAllMode = false;
        refreshList();
        refreshMySessions();
    }

    @FXML
    public void handleShowMyHistory() {
        showView(viewList);
        setVisibleManaged(sectionMySessions, false);
        setVisibleManaged(sectionMyHistory, true);
        setVisibleManaged(sectionAvailableSessions, false);
        setVisibleManaged(filtersBar, false);
        setVisibleManaged(chatbotContainer, false);
        showAllMode = false;
        refreshList();
        refreshMyHistory();
    }

    // ─────────────────────────────────────────────
    //  LIST FILTER / SEARCH
    // ─────────────────────────────────────────────
    @FXML
    public void handleFilter() {
        refreshList();
    }

    @FXML
    public void handleSearch() {
        refreshList();
    }

    @FXML
    public void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (filterTypeCombo != null) filterTypeCombo.setValue(null);
        if (filterLevelCombo != null) filterLevelCombo.setValue(null);
        refreshList();
    }

    private void refreshList() {
        if (allPlanningsPane == null) return;
        allPlanningsPane.getChildren().clear();

        new Thread(() -> {
            try {
                String keyword = searchField != null ? searchField.getText().trim() : "";
                String type = filterTypeCombo != null ? filterTypeCombo.getValue() : null;
                String level = filterLevelCombo != null ? filterLevelCombo.getValue() : null;

                List<Planning> plannings;

                boolean hasKeyword = keyword != null && !keyword.isBlank();
                boolean hasType = type != null && !type.isBlank();
                boolean hasLevel = level != null && !level.isBlank();

                if (hasKeyword) {
                    plannings = planningService.searchPlannings(keyword);
                } else if (hasType) {
                    plannings = planningService.getPlanningsByType(type);
                } else if (hasLevel) {
                    plannings = planningService.getPlanningsByLevel(level);
                } else {
                    plannings = showAllMode ? planningService.getAllPlannings() : planningService.getUpcomingPlannings();
                }

                // Post-filter if both selected
                if (hasType && !Objects.equals(hasType ? type : null, null)) {
                    plannings = plannings.stream()
                            .filter(p -> type.equalsIgnoreCase(p.getType()))
                            .toList();
                }
                if (hasLevel && !Objects.equals(hasLevel ? level : null, null)) {
                    plannings = plannings.stream()
                            .filter(p -> level.equalsIgnoreCase(p.getLevel()))
                            .toList();
                }

                List<Planning> finalPlannings = plannings;
                Platform.runLater(() -> renderPlanningCards(finalPlannings));
            } catch (Exception e) {
                System.err.println("[Planning] refreshList error: " + e.getMessage());
            }
        }, "Planning-LoadList").start();
    }

    private void renderPlanningCards(List<Planning> plannings) {
        if (allPlanningsPane == null) return;
        allPlanningsPane.getChildren().clear();

        if (plannings == null || plannings.isEmpty()) {
            Label empty = new Label("No sessions found.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 13;");
            allPlanningsPane.getChildren().add(empty);
            return;
        }

        for (Planning p : plannings) {
            allPlanningsPane.getChildren().add(buildPlanningCard(p));
        }
    }

    private Pane buildPlanningCard(Planning p) {
        StackPane card = new StackPane();
        card.getStyleClass().add("fe-planning-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        // Background + content container
        VBox body = new VBox(12);
        body.getStyleClass().add("fe-card-body");

        // Image header with overlays
        StackPane header = new StackPane();
        header.getStyleClass().add("fe-card-header");

        ImageView cover = new ImageView();
        cover.setFitWidth(320);
        cover.setFitHeight(160);
        cover.setPreserveRatio(false);
        cover.getStyleClass().add("fe-card-cover");
        renderCardImage(cover, p.getImage());
        // Round corners to keep image fully visible (no tinted overlay)
        Rectangle clip = new Rectangle(320, 160);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        cover.setClip(clip);

        // Type pill (top-left)
        Label type = new Label(p.getType() != null && !p.getType().isBlank() ? p.getType() : "SESSION");
        type.getStyleClass().add("fe-pill");
        StackPane.setAlignment(type, Pos.TOP_LEFT);
        StackPane.setMargin(type, new Insets(12, 0, 0, 12));

        // Date badge (top-right)
        VBox dateBadge = new VBox(0);
        dateBadge.getStyleClass().add("fe-date-badge");
        // IMPORTANT: VBox is resizable; in a StackPane it can stretch and tint the whole image.
        // Keep it at its preferred size only (like the Symfony badge).
        dateBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        String day = p.getDate() != null ? String.format(Locale.ROOT, "%02d", p.getDate().getDayOfMonth()) : "--";
        String mon = p.getDate() != null ? p.getDate().getMonth().toString().substring(0, 3) : "---";
        Label dayL = new Label(day);
        dayL.getStyleClass().add("fe-date-day");
        Label monL = new Label(mon);
        monL.getStyleClass().add("fe-date-mon");
        dateBadge.getChildren().addAll(dayL, monL);
        StackPane.setAlignment(dateBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(dateBadge, new Insets(12, 12, 0, 0));

        header.getChildren().addAll(cover, type, dateBadge);

        // Level badge
        Label level = new Label(p.getLevel() != null && !p.getLevel().isBlank() ? p.getLevel() : "—");
        level.getStyleClass().add("fe-level-badge");

        // Title (use description snippet like capture)
        Label title = new Label(snip(p.getDescription(), 22));
        title.getStyleClass().add("fe-card-title");
        title.setWrapText(false);

        // Meta row time + location
        HBox meta = new HBox(12);
        meta.getStyleClass().add("fe-card-meta");
        Label time = new Label("⏱ " + safe(p.getTime()));
        time.getStyleClass().add("fe-meta");
        Label loc = new Label("📍 " + safe(p.getLocalisation()));
        loc.getStyleClass().add("fe-meta");
        loc.setWrapText(false);
        meta.getChildren().addAll(time, loc);

        // Rating row (interactive + opens feedback modal)
        VBox ratingBox = new VBox(6);
        ratingBox.setAlignment(Pos.CENTER);
        Label rateLab = new Label("Rate this session:");
        rateLab.getStyleClass().add("fe-rate-label");
        HBox stars = starsRowInteractive(p);
        ratingBox.getChildren().addAll(rateLab, stars);

        Button join = new Button("Join Session");
        join.getStyleClass().add("btn-blue");
        join.setMaxWidth(Double.MAX_VALUE);
        join.setOnAction(e -> {
            e.consume();
            openJoinModal(p);
        });

        body.getChildren().addAll(header, level, title, meta, new Separator(), ratingBox, join);

        card.getChildren().add(body);
        card.setOnMouseClicked(e -> openPlanningDetails(p.getIdPlanning()));
        return card;
    }

    private void renderCardImage(ImageView iv, String filename) {
        if (iv == null) return;
        try {
            if (filename == null || filename.isBlank()) {
                iv.setImage(null);
                return;
            }
            File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + filename);
            if (f.exists()) {
                iv.setImage(new Image(f.toURI().toString(), 320, 160, false, true));
            } else {
                iv.setImage(null);
            }
        } catch (Exception e) {
            iv.setImage(null);
        }
    }

    private HBox starsRow(int filled) {
        int f = Math.max(0, Math.min(5, filled));
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 5; i++) {
            Label s = new Label(i <= f ? "★" : "☆");
            s.getStyleClass().add(i <= f ? "fe-star-on" : "fe-star-off");
            box.getChildren().add(s);
        }
        return box;
    }

    private HBox starsRowInteractive(Planning p) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            Label star = new Label("☆");
            star.getStyleClass().add("fe-star-off");
            star.setOnMouseEntered(e -> highlightStars(box, rating));
            star.setOnMouseExited(e -> highlightStars(box, 0));
            star.setOnMouseClicked(e -> openFeedbackModal(p, rating));
            box.getChildren().add(star);
        }
        return box;
    }

    private void highlightStars(HBox box, int filled) {
        for (int i = 0; i < box.getChildren().size(); i++) {
            Node n = box.getChildren().get(i);
            if (!(n instanceof Label star)) continue;
            boolean on = (i + 1) <= filled;
            star.setText(on ? "★" : "☆");
            star.getStyleClass().removeAll("fe-star-on", "fe-star-off");
            star.getStyleClass().add(on ? "fe-star-on" : "fe-star-off");
        }
    }

    private void openFeedbackModal(Planning p, int rating) {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        feedbackPlanning = p;
        feedbackRating = rating;
        clearFeedbackError();

        if (feedbackTextArea != null) feedbackTextArea.clear();
        buildFeedbackStars(rating);

        if (feedbackOverlay != null) {
            feedbackOverlay.setVisible(true);
            feedbackOverlay.setManaged(true);
        }
    }

    private void buildFeedbackStars(int selected) {
        if (feedbackStarsRow == null) return;
        feedbackStarsRow.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            final int r = i;
            Label star = new Label(i <= selected ? "★" : "☆");
            star.getStyleClass().add(i <= selected ? "fe-star-on" : "fe-star-off");
            star.setOnMouseEntered(e -> buildFeedbackStars(r));
            star.setOnMouseClicked(e -> {
                feedbackRating = r;
                buildFeedbackStars(r);
            });
            feedbackStarsRow.getChildren().add(star);
        }
    }

    @FXML
    public void handleCancelFeedback() {
        if (feedbackOverlay != null) {
            feedbackOverlay.setVisible(false);
            feedbackOverlay.setManaged(false);
        }
        feedbackPlanning = null;
        feedbackRating = 0;
    }

    @FXML
    public void handleSendFeedback() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        if (feedbackPlanning == null) return;
        // Symfony logic: comment is required, rating may be 0 if user didn't select stars.
        String content = feedbackTextArea != null ? feedbackTextArea.getText().trim() : "";
        if (content.isBlank()) {
            showFeedbackError("Please provide a comment.");
            return;
        }

        clearFeedbackError();
        if (feedbackSendBtn != null) feedbackSendBtn.setDisable(true);

        new Thread(() -> {
            try {
                Review r = new Review();
                r.setUserId(u.getId());
                r.setIdPlanning(feedbackPlanning.getIdPlanning());
                r.setRating(feedbackRating);
                r.setContent(content);
                reviewService.createReview(r);
                Platform.runLater(() -> {
                    handleCancelFeedback();
                    if (feedbackSendBtn != null) feedbackSendBtn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showFeedbackError("Failed to send feedback: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    if (feedbackSendBtn != null) feedbackSendBtn.setDisable(false);
                });
            }
        }, "Planning-Feedback").start();
    }

    private void showFeedbackError(String msg) {
        if (feedbackErrorLabel == null) return;
        feedbackErrorLabel.setText("⚠ " + msg);
        feedbackErrorLabel.setVisible(true);
        feedbackErrorLabel.setManaged(true);
    }

    private void clearFeedbackError() {
        if (feedbackErrorLabel == null) return;
        feedbackErrorLabel.setText("");
        feedbackErrorLabel.setVisible(false);
        feedbackErrorLabel.setManaged(false);
    }

    private void handleJoinSession(int idPlanning) {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        new Thread(() -> {
            try {
                sessionService.joinSession(idPlanning, u.getId());
                Platform.runLater(() -> SessionManager.setPendingFlash("success", "Joined successfully."));
            } catch (Exception ex) {
                Platform.runLater(() -> SessionManager.setPendingFlash("error", "Join failed: " + ex.getMessage()));
            }
        }, "Planning-Join").start();
    }

    private void openJoinModal(Planning p) {
        User u = SessionManager.getCurrentUser();
        if (u == null || p == null) return;

        // If user already joined this planning, show it directly in "My Sessions" instead of re-joining.
        new Thread(() -> {
            try {
                if (sessionService.hasUserJoinedPlanning(u.getId(), p.getIdPlanning())) {
                    Platform.runLater(() -> {
                        handleShowMySessions();
                        SessionManager.setPendingFlash("info", "You already joined this session. See it in My Sessions.");
                    });
                    return;
                }
            } catch (Exception ignore) {
            }
        }, "Planning-CheckJoined").start();

        joinPlanning = p;

        if (joinErrorLabel != null) {
            joinErrorLabel.setText("");
            joinErrorLabel.setVisible(false);
            joinErrorLabel.setManaged(false);
        }

        // Left preview
        if (joinImage != null) {
            try {
                if (p.getImage() != null && !p.getImage().isBlank()) {
                    File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + p.getImage());
                    joinImage.setImage(f.exists() ? new Image(f.toURI().toString(), 410, 230, false, true) : null);
                } else {
                    joinImage.setImage(null);
                }
                Rectangle clip = new Rectangle(410, 230);
                clip.setArcWidth(28);
                clip.setArcHeight(28);
                joinImage.setClip(clip);
            } catch (Exception ignore) {
                joinImage.setImage(null);
            }
        }
        if (joinTypeLabel != null) joinTypeLabel.setText(safe(p.getType()));
        if (joinTitleLabel != null) joinTitleLabel.setText(snip(p.getDescription(), 34));
        if (joinLevelLabel != null) joinLevelLabel.setText(safe(p.getLevel()));
        if (joinDateLabel != null) joinDateLabel.setText("📅 " + safe(p.getDate()));
        if (joinTimeLabel != null) joinTimeLabel.setText("⏱ " + safe(p.getTime()));
        if (joinLocationLabel != null) joinLocationLabel.setText("📍 " + safe(p.getLocalisation()));

        // Right form
        if (joinParticipantField != null) joinParticipantField.setText(safe(u.getUsername()));
        if (joinRegDateTimeField != null) {
            String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm", Locale.ROOT));
            joinRegDateTimeField.setText(dt);
        }
        if (joinStatusPill != null) joinStatusPill.setText("Pending");
        if (joinConfirmBtn != null) joinConfirmBtn.setDisable(false);

        if (joinOverlay != null) {
            joinOverlay.setVisible(true);
            joinOverlay.setManaged(true);
        }
    }

    @FXML
    public void handleCancelRegistration() {
        if (joinOverlay != null) {
            joinOverlay.setVisible(false);
            joinOverlay.setManaged(false);
        }
        joinPlanning = null;
    }

    @FXML
    public void handleConfirmRegistration() {
        User u = SessionManager.getCurrentUser();
        if (u == null || joinPlanning == null) return;

        if (joinErrorLabel != null) {
            joinErrorLabel.setText("");
            joinErrorLabel.setVisible(false);
            joinErrorLabel.setManaged(false);
        }
        if (joinConfirmBtn != null) joinConfirmBtn.setDisable(true);

        int planningId = joinPlanning.getIdPlanning();
        new Thread(() -> {
            try {
                sessionService.joinSession(planningId, u.getId());
                Platform.runLater(() -> {
                    handleCancelRegistration();
                    refreshMySessions();
                    SessionManager.setPendingFlash("success", "Registration submitted. Status: Pending.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (joinErrorLabel != null) {
                        joinErrorLabel.setText("⚠ " + ex.getMessage());
                        joinErrorLabel.setVisible(true);
                        joinErrorLabel.setManaged(true);
                    }
                    if (joinConfirmBtn != null) joinConfirmBtn.setDisable(false);
                });
            }
        }, "Planning-JoinConfirm").start();
    }

    private void refreshMySessions() {
        if (mySessionsList == null) return;
        mySessionsList.getChildren().clear();

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                List<TrainingSession> sessions = sessionService.getSessionsByUser(user.getId());
                // Load planning details for each session (needed for Symfony-like cards)
                for (TrainingSession s : sessions) {
                    if (s.getPlanning() == null) {
                        try {
                            s.setPlanning(planningService.getPlanningById(s.getIdPlanning()));
                        } catch (Exception ignore) {
                        }
                    }
                }
                Platform.runLater(() -> renderMySessions(sessions));
            } catch (Exception e) {
                System.err.println("[Planning] refreshMySessions error: " + e.getMessage());
            }
        }, "Planning-MySessions").start();
    }

    private void renderMySessions(List<TrainingSession> sessions) {
        if (mySessionsList == null) return;
        mySessionsList.getChildren().clear();

        if (sessions == null || sessions.isEmpty()) {
            Label empty = new Label("You have not joined any session yet.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12;");
            mySessionsList.getChildren().add(empty);
            return;
        }

        for (TrainingSession s : sessions) {
            // My sessions should be presented as the same "card" style (like Symfony /my-sessions)
            mySessionsList.getChildren().add(buildSessionCard(s, true));
        }
    }

    private void refreshMyHistory() {
        if (myHistoryList == null) return;
        myHistoryList.getChildren().clear();

        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                List<TrainingSession> sessions = sessionService.getSessionsByUser(user.getId());
                for (TrainingSession s : sessions) {
                    if (s.getPlanning() == null) {
                        try {
                            s.setPlanning(planningService.getPlanningById(s.getIdPlanning()));
                        } catch (Exception ignore) {
                        }
                    }
                }
                // History = cancelled or past date/time
                LocalDateTime now = LocalDateTime.now();
                List<TrainingSession> history = sessions.stream().filter(s -> {
                    if (s == null) return false;
                    String st = s.getStatus() != null ? s.getStatus() : "";
                    if ("CANCELLED".equalsIgnoreCase(st)) return true;
                    Planning p = s.getPlanning();
                    if (p == null || p.getDate() == null) return false;
                    LocalTime t = p.getTime() != null ? p.getTime() : LocalTime.MIDNIGHT;
                    return p.getDate().atTime(t).isBefore(now);
                }).toList();
                Platform.runLater(() -> renderMyHistory(history));
            } catch (Exception e) {
                System.err.println("[Planning] refreshMyHistory error: " + e.getMessage());
            }
        }, "Planning-MyHistory").start();
    }

    private void renderMyHistory(List<TrainingSession> sessions) {
        if (myHistoryList == null) return;
        myHistoryList.getChildren().clear();

        if (sessions == null || sessions.isEmpty()) {
            Label empty = new Label("No history yet.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12;");
            myHistoryList.getChildren().add(empty);
            return;
        }

        for (TrainingSession s : sessions) {
            myHistoryList.getChildren().add(buildSessionCard(s, false));
        }
    }

    private Pane buildSessionCard(TrainingSession s, boolean showCancel) {
        Planning p = s != null ? s.getPlanning() : null;

        HBox root = new HBox(18);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: rgba(255,255,255,0.035); -fx-background-radius: 16; " +
                "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16;");

        // Optional thumbnail (keep very subtle, not mandatory)
        ImageView thumb = new ImageView();
        thumb.setFitWidth(90);
        thumb.setFitHeight(62);
        thumb.setPreserveRatio(false);
        try {
            if (p != null && p.getImage() != null && !p.getImage().isBlank()) {
                File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + p.getImage());
                thumb.setImage(f.exists() ? new Image(f.toURI().toString(), 90, 62, false, true) : null);
            }
        } catch (Exception ignore) {
            thumb.setImage(null);
        }
        Rectangle clip = new Rectangle(90, 62);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        thumb.setClip(clip);

        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER_LEFT);
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label type = new Label(p != null ? safe(p.getType()) : "SESSION");
        type.setStyle("-fx-background-color: rgba(99,102,241,0.25); -fx-border-color: rgba(99,102,241,0.55); " +
                "-fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 6 12; " +
                "-fx-text-fill: rgba(255,255,255,0.9); -fx-font-weight: 900;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String st = s != null && s.getStatus() != null ? s.getStatus() : "Pending";
        Label status = new Label(st.equalsIgnoreCase("en attente") ? "Pending" : st);
        status.setStyle("-fx-background-color: rgba(255,214,102,0.10); -fx-border-color: rgba(255,214,102,0.35); " +
                "-fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 6 12; " +
                "-fx-text-fill: #ffd166; -fx-font-weight: 900;");

        top.getChildren().addAll(type, spacer, status);

        Label name = new Label(safe(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getUsername() : "—"));
        name.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 20;");

        HBox info = new HBox(40);
        info.setAlignment(Pos.CENTER_LEFT);
        Label level = new Label(p != null ? safe(p.getLevel()) : "—");
        level.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 800;");
        Label date = new Label(p != null && p.getDate() != null ? p.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ROOT)) : "—");
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 800;");
        Label time = new Label(p != null && p.getTime() != null ? p.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "—");
        time.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 800;");
        Label loc = new Label(p != null ? safe(p.getLocalisation()) : "—");
        loc.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 800;");
        info.getChildren().addAll(level, date, time, loc);

        Label joined = new Label("Joined on " + (s != null && s.getJoinedAt() != null
                ? s.getJoinedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm", Locale.ROOT))
                : "—"));
        joined.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-weight: 700;");

        center.getChildren().addAll(top, name, info, new Separator(), joined);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (showCancel) {
            Button cancel = new Button("Cancel Registration");
            cancel.getStyleClass().add("btn-danger");
            cancel.setMinWidth(190);
            cancel.setOnAction(e -> cancelMyRegistration(s));
            actions.getChildren().add(cancel);
        }

        root.getChildren().addAll(thumb, center, actions);
        HBox.setHgrow(center, Priority.ALWAYS);
        root.setOnMouseClicked(e -> {
            if (p != null) openPlanningDetails(p.getIdPlanning());
        });
        return root;
    }

    private void cancelMyRegistration(TrainingSession s) {
        User u = SessionManager.getCurrentUser();
        if (u == null || s == null) return;
        new Thread(() -> {
            try {
                sessionService.cancelSession(s.getIdTraining(), u.getId());
                Platform.runLater(() -> {
                    refreshMySessions();
                    refreshMyHistory();
                    SessionManager.setPendingFlash("success", "Registration cancelled.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> SessionManager.setPendingFlash("error", "Cancel failed: " + ex.getMessage()));
            }
        }, "Planning-CancelRegistration").start();
    }

    // ─────────────────────────────────────────────
    //  DETAILS
    // ─────────────────────────────────────────────
    private void openPlanningDetails(int idPlanning) {
        showView(viewShow);
        selectedPlanning = null;

        new Thread(() -> {
            try {
                Planning planning = planningService.getPlanningWithDetails(idPlanning);
                double avg = planningService.getAverageRating(idPlanning);
                int participants = planningService.countParticipants(idPlanning);
                Platform.runLater(() -> renderPlanningDetails(planning, participants, avg));
            } catch (Exception e) {
                System.err.println("[Planning] openPlanningDetails: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to load planning details: " + e.getMessage(), ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                    showView(viewList);
                });
            }
        }, "Planning-Detail").start();
    }

    private void renderPlanningDetails(Planning p, int participants, double avgRating) {
        selectedPlanning = p;

        setLabel(showDate, safe(p.getDate()));
        setLabel(showTime, safe(p.getTime()));
        setLabel(showLocalisation, safe(p.getLocalisation()));
        setLabel(showLevel, safe(p.getLevel()));
        setLabel(showType, safe(p.getType()));
        setLabel(showNeedPartner, p.isNeedPartner() ? "Yes" : "No");
        setLabel(showDescription, safe(p.getDescription()));
        setLabel(showParticipantCount, String.valueOf(participants));
        setLabel(showAverageRating, String.format("%.2f", avgRating));

        renderImage(showImage, p.getImage());

        renderParticipants(p.getTrainingSessions());
        // Reviews are submitted by users but are visible only in Admin → Reviews.

        // Admin actions
        boolean canEdit = SessionManager.isAdmin() || SessionManager.isCoach();
        if (showEditBtn != null) showEditBtn.setVisible(canEdit);
        if (showDeleteBtn != null) showDeleteBtn.setVisible(canEdit);

        // Map: optional, show only if we can load a simple map html
        if (mapContainer != null) {
            mapContainer.setVisible(false);
            mapContainer.setManaged(false);
        }
    }

    private void renderParticipants(List<TrainingSession> sessions) {
        if (participantsList == null) return;
        participantsList.getChildren().clear();

        if (sessions == null || sessions.isEmpty()) {
            Label empty = new Label("No participants yet.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12;");
            participantsList.getChildren().add(empty);
            return;
        }

        for (TrainingSession s : sessions) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10;");

            String username = (s.getUser() != null && s.getUser().getUsername() != null)
                    ? s.getUser().getUsername()
                    : ("User#" + s.getIdCurrentUser());
            Label name = new Label(username);
            name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(s.getStatus() != null ? s.getStatus() : "—");
            status.setStyle("-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 11;");

            row.getChildren().addAll(name, spacer, status);
            participantsList.getChildren().add(row);
        }
    }

    // ─────────────────────────────────────────────
    //  CREATE / EDIT / DELETE
    // ─────────────────────────────────────────────
    @FXML
    public void handleSelectImage() {
        createSelectedImageFile = chooseImageFile();
        if (createImageLabel != null) {
            createImageLabel.setText(createSelectedImageFile != null ? createSelectedImageFile.getName() : "No image selected");
        }
    }

    @FXML
    public void handleEditSelectImage() {
        editSelectedImageFile = chooseImageFile();
        if (editImageLabel != null) {
            editImageLabel.setText(editSelectedImageFile != null ? editSelectedImageFile.getName() : "No image selected");
        }
    }

    private File chooseImageFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        return fc.showOpenDialog(MainApp.getPrimaryStage());
    }

    @FXML
    public void handleCreateSubmit() {
        clearError(createGeneralError);

        Planning p = new Planning();
        try {
            p.setDate(requireDate(createDatePicker));
            p.setTime(requireTime(createTimeField));
            p.setLocalisation(requireText(createLocalisationField, "La localisation est obligatoire"));
            p.setDescription(requireText(createDescriptionArea, "La description est obligatoire"));
            p.setNeedPartner(createNeedPartnerCheck != null && createNeedPartnerCheck.isSelected());

            String lvl = createLevelCombo != null ? createLevelCombo.getValue() : null;
            String type = createTypeCombo != null ? createTypeCombo.getValue() : null;
            if (lvl == null || lvl.isBlank()) throw new IllegalArgumentException("Le niveau est obligatoire");
            if (type == null || type.isBlank()) throw new IllegalArgumentException("Le type est obligatoire");
            p.setLevel(lvl);
            p.setType(type);

            byte[] bytes = null;
            String ext = null;
            if (createSelectedImageFile != null) {
                bytes = Files.readAllBytes(createSelectedImageFile.toPath());
                ext = getExtension(createSelectedImageFile.getName());
            }

            byte[] finalBytes = bytes;
            String finalExt = ext;
            new Thread(() -> {
                try {
                    if (finalBytes != null && finalExt != null) planningService.createPlanning(p, finalBytes, finalExt);
                    else planningService.createPlanning(p);
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("success", "Session créée avec succès.");
                        showView(viewList);
                        refreshList();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError(createGeneralError, e.getMessage()));
                }
            }, "Planning-Create").start();

        } catch (Exception e) {
            showError(createGeneralError, e.getMessage());
        }
    }

    @FXML
    public void handleShowEdit() {
        if (selectedPlanning == null) return;
        clearError(editGeneralError);

        // Fill fields
        if (editDatePicker != null) editDatePicker.setValue(selectedPlanning.getDate());
        if (editTimeField != null) editTimeField.setText(selectedPlanning.getTime() != null ? selectedPlanning.getTime().toString() : "");
        if (editLocalisationField != null) editLocalisationField.setText(selectedPlanning.getLocalisation());
        if (editDescriptionArea != null) editDescriptionArea.setText(selectedPlanning.getDescription());
        if (editNeedPartnerCheck != null) editNeedPartnerCheck.setSelected(selectedPlanning.isNeedPartner());
        if (editLevelCombo != null) editLevelCombo.setValue(selectedPlanning.getLevel());
        if (editTypeCombo != null) editTypeCombo.setValue(selectedPlanning.getType());

        editSelectedImageFile = null;
        if (editImageLabel != null) editImageLabel.setText(selectedPlanning.getImage() != null ? selectedPlanning.getImage() : "No image selected");

        showView(viewEdit);
    }

    @FXML
    public void handleEditSubmit() {
        if (selectedPlanning == null) return;
        clearError(editGeneralError);

        try {
            selectedPlanning.setDate(requireDate(editDatePicker));
            selectedPlanning.setTime(requireTime(editTimeField));
            selectedPlanning.setLocalisation(requireText(editLocalisationField, "La localisation est obligatoire"));
            selectedPlanning.setDescription(requireText(editDescriptionArea, "La description est obligatoire"));
            selectedPlanning.setNeedPartner(editNeedPartnerCheck != null && editNeedPartnerCheck.isSelected());

            String lvl = editLevelCombo != null ? editLevelCombo.getValue() : null;
            String type = editTypeCombo != null ? editTypeCombo.getValue() : null;
            if (lvl == null || lvl.isBlank()) throw new IllegalArgumentException("Le niveau est obligatoire");
            if (type == null || type.isBlank()) throw new IllegalArgumentException("Le type est obligatoire");
            selectedPlanning.setLevel(lvl);
            selectedPlanning.setType(type);

            byte[] bytes = null;
            String ext = null;
            if (editSelectedImageFile != null) {
                bytes = Files.readAllBytes(editSelectedImageFile.toPath());
                ext = getExtension(editSelectedImageFile.getName());
            }

            byte[] finalBytes = bytes;
            String finalExt = ext;
            new Thread(() -> {
                try {
                    if (finalBytes != null && finalExt != null) planningService.updatePlanning(selectedPlanning, finalBytes, finalExt);
                    else planningService.updatePlanning(selectedPlanning);
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("success", "Session mise à jour avec succès.");
                        openPlanningDetails(selectedPlanning.getIdPlanning());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError(editGeneralError, e.getMessage()));
                }
            }, "Planning-Edit").start();

        } catch (Exception e) {
            showError(editGeneralError, e.getMessage());
        }
    }

    @FXML
    public void handleDeletePlanning() {
        if (selectedPlanning == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this planning permanently?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            new Thread(() -> {
                try {
                    planningService.deletePlanning(selectedPlanning.getIdPlanning());
                    Platform.runLater(() -> {
                        SessionManager.setPendingFlash("success", "Planning deleted.");
                        showView(viewList);
                        refreshList();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Delete failed: " + e.getMessage(), ButtonType.OK);
                        a.setHeaderText(null);
                        a.showAndWait();
                    });
                }
            }, "Planning-Delete").start();
        });
    }

    // ─────────────────────────────────────────────
    //  AI COACH + CHATBOT
    // ─────────────────────────────────────────────
    @FXML
    public void handleRefreshCoachAdvice() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        if (coachBanner != null) {
            coachBanner.setVisible(true);
            coachBanner.setManaged(true);
        }
        if (coachRecommendationLabel != null) {
            coachRecommendationLabel.setText("Analyzing your profile...");
        }

        aiCoachService.getRecommendations(user).whenComplete((text, err) -> {
            Platform.runLater(() -> {
                if (err != null) {
                    if (coachRecommendationLabel != null) coachRecommendationLabel.setText("AI coach unavailable right now.");
                } else {
                    if (coachRecommendationLabel != null) coachRecommendationLabel.setText(text);
                }
            });
        });
    }

    @FXML
    public void handleCloseCoachBanner() {
        if (coachBanner != null) {
            coachBanner.setVisible(false);
            coachBanner.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────
    private void setupCombos() {
        if (filterTypeCombo != null) {
            filterTypeCombo.getItems().setAll(enumDbValues(PlanningType.values()));
        }
        if (filterLevelCombo != null) {
            filterLevelCombo.getItems().setAll(enumDbValues(PlanningLevel.values()));
        }
        if (createTypeCombo != null) createTypeCombo.getItems().setAll(enumDbValues(PlanningType.values()));
        if (createLevelCombo != null) createLevelCombo.getItems().setAll(enumDbValues(PlanningLevel.values()));
        if (editTypeCombo != null) editTypeCombo.getItems().setAll(enumDbValues(PlanningType.values()));
        if (editLevelCombo != null) editLevelCombo.getItems().setAll(enumDbValues(PlanningLevel.values()));
    }

    private List<String> enumDbValues(PlanningType[] values) {
        return java.util.Arrays.stream(values).map(PlanningType::getDbValue).toList();
    }

    private List<String> enumDbValues(PlanningLevel[] values) {
        return java.util.Arrays.stream(values).map(PlanningLevel::getDbValue).toList();
    }

    private void setLabel(Label l, String v) {
        if (l != null) l.setText(v != null ? v : "—");
    }

    private String safe(Object o) {
        return o != null ? String.valueOf(o) : "—";
    }

    private String snip(String s, int max) {
        if (s == null) return "—";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    private void renderImage(ImageView iv, String filename) {
        if (iv == null) return;
        try {
            if (filename == null || filename.isBlank()) {
                iv.setImage(null);
                return;
            }
            File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + filename);
            if (f.exists()) {
                iv.setImage(new Image(f.toURI().toString(), 430, 220, true, true));
            } else {
                iv.setImage(null);
            }
        } catch (Exception e) {
            iv.setImage(null);
        }
    }

    private void showError(Label label, String message) {
        if (label == null) return;
        label.setText("⚠ " + message);
        label.setStyle("-fx-text-fill: #ff6b7a; -fx-font-size: 12;");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearError(Label label) {
        if (label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private LocalDate requireDate(DatePicker dp) {
        if (dp == null || dp.getValue() == null) throw new IllegalArgumentException("Date is required");
        return dp.getValue();
    }

    private LocalTime requireTime(TextField tf) {
        String raw = tf != null ? tf.getText().trim() : "";
        if (raw.isBlank()) throw new IllegalArgumentException("Time is required");
        try {
            // Accept "HH:mm" or "H:mm"
            return LocalTime.parse(raw, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:mm (e.g. 18:30)");
        }
    }

    private String requireText(TextInputControl tf, String msg) {
        String raw = tf != null ? tf.getText().trim() : "";
        if (raw.isBlank()) throw new IllegalArgumentException(msg);
        return raw;
    }

    private String getExtension(String name) {
        if (name == null) return "jpg";
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "jpg";
        return name.substring(dot + 1).toLowerCase();
    }
}

