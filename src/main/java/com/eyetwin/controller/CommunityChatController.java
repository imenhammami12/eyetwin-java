package com.eyetwin.controller;

import com.eyetwin.config.AISummaryConfig;
import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.Community.ChatSummaryResult;
import com.eyetwin.entities.Community.GiphyGif;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.Community.MessageAttachment;
import com.eyetwin.entities.Community.MessageModerationResult;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.AudioRecorderService;
import com.eyetwin.services.Community.ChatSummaryService;
import com.eyetwin.services.Community.CloudinaryUploadService;
import com.eyetwin.services.Community.HuggingFaceSpeechToTextService;
import com.eyetwin.services.Community.MessageModerationService;
import com.eyetwin.services.Community.MessageServiceImpl;
import com.eyetwin.services.Community.PiperTextToSpeechService;
import com.eyetwin.tools.CommunityFileValidator;
import com.eyetwin.tools.CommunityValidator;
import com.eyetwin.tools.SessionManager;
import com.eyetwin.websocket.ChatWebSocketConfig;
import com.eyetwin.websocket.client.ChatSocketListener;
import com.eyetwin.websocket.client.CommunityWebSocketClient;
import com.eyetwin.websocket.model.SocketEnvelope;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommunityChatController {

    @FXML private Label lblChannelName;
    @FXML private Label lblChannelDescription;
    @FXML private Label lblChannelStatus;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScrollPane;

    @FXML private TextArea taNewMessage;
    @FXML private Button btnSend;
    @FXML private VBox composerBox;
    @FXML private Label lblComposerInfo;

    @FXML private Button btnAttach;
    @FXML private Button btnClearAttachments;
    @FXML private Label lblAttachmentNames;

    @FXML private Button btnRecordVoice;
    @FXML private Button btnStopVoice;
    @FXML private Label lblSpeechInfo;

    @FXML private VBox summaryBannerBox;
    @FXML private Label lblMissedMessages;
    @FXML private Button btnSummarizeMissed;
    @FXML private Label lblSummaryLoading;

    @FXML private VBox summaryCardBox;
    @FXML private Label lblSummaryTitle;
    @FXML private Label lblSummaryOverview;
    @FXML private VBox summaryKeyPointsBox;
    @FXML private VBox summaryActionItemsBox;
    @FXML private VBox summaryOpenQuestionsBox;

    @FXML private Button btnGif;

    private final AudioRecorderService audioRecorderService = new AudioRecorderService();
    private final HuggingFaceSpeechToTextService speechToTextService = new HuggingFaceSpeechToTextService();
    private final PiperTextToSpeechService textToSpeechService = new PiperTextToSpeechService();
    private final CloudinaryUploadService cloudinaryUploadService = new CloudinaryUploadService();
    private final MessageServiceImpl messageService = new MessageServiceImpl();
    private final MessageModerationService moderationService = new MessageModerationService();
    private final ChatSummaryService chatSummaryService = new ChatSummaryService();

    private MediaPlayer ttsPlayer;

    private final List<File> selectedAttachments = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Channel channel;
    private Integer editingMessageId = null;
    private Integer actionMenuMessageId = null;
    private Integer deleteConfirmMessageId = null;
    private Integer reactionPickerMessageId = null;

    private CommunityWebSocketClient socketClient;
    private boolean realtimeReady = false;

    public void setChannel(Channel channel) {
        stopRealtimeChat();

        this.channel = channel;
        refreshHeader();
        refreshComposerVisibility();

        hideSummaryBanner();
        clearSummaryCard();

        loadMessagesToBottom();
        refreshMissedSummaryBanner();

        startRealtimeChat();
    }

    @FXML
    public void initialize() {
        refreshHeader();
        refreshComposerVisibility();
        hideSummaryBanner();
        clearSummaryCard();
        refreshSpeechUi();
    }

    private void refreshSpeechUi() {
        boolean recording = audioRecorderService.isRecording();

        if (btnRecordVoice != null) {
            btnRecordVoice.setDisable(recording);
        }

        if (btnStopVoice != null) {
            btnStopVoice.setDisable(!recording);
        }

        if (lblSpeechInfo != null && !recording && (lblSpeechInfo.getText() == null || lblSpeechInfo.getText().isBlank())) {
            lblSpeechInfo.setVisible(false);
            lblSpeechInfo.setManaged(false);
        }
    }

    private void showSpeechInfo(String text) {
        if (lblSpeechInfo == null) return;
        lblSpeechInfo.setText(text);
        lblSpeechInfo.setVisible(true);
        lblSpeechInfo.setManaged(true);
    }

    private void hideSpeechInfoLater() {
        if (lblSpeechInfo == null) return;

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            lblSpeechInfo.setText("");
            lblSpeechInfo.setVisible(false);
            lblSpeechInfo.setManaged(false);
        });
        pause.play();
    }

    private void stopSpeechPlayback() {
        if (ttsPlayer != null) {
            try {
                ttsPlayer.stop();
                ttsPlayer.dispose();
            } catch (Exception ignored) {
            }
            ttsPlayer = null;
        }
    }

    private void playSpeechFile(File audioFile) {
        stopSpeechPlayback();

        Media media = new Media(audioFile.toURI().toString());
        ttsPlayer = new MediaPlayer(media);
        ttsPlayer.setOnEndOfMedia(this::stopSpeechPlayback);
        ttsPlayer.play();
    }

    private void startRealtimeChat() {
        if (channel == null) return;

        try {
            realtimeReady = false;

            socketClient = new CommunityWebSocketClient(
                    URI.create(ChatWebSocketConfig.SERVER_URL),
                    new ChatSocketListener() {
                        @Override
                        public void onConnected() {
                            realtimeReady = true;
                            System.out.println("Realtime chat connected for channel " + channel.getId());
                        }

                        @Override
                        public void onMessageReceived(SocketEnvelope envelope) {
                            if (envelope == null) return;
                            if (envelope.getChannelId() == null) return;
                            if (!envelope.getChannelId().equals(channel.getId())) return;

                            if (!SocketEnvelope.TYPE_NEW_MESSAGE.equals(envelope.getType())
                                    && !SocketEnvelope.TYPE_EDIT_MESSAGE.equals(envelope.getType())
                                    && !SocketEnvelope.TYPE_DELETE_MESSAGE.equals(envelope.getType())
                                    && !SocketEnvelope.TYPE_REACTION_CHANGED.equals(envelope.getType())) {
                                return;
                            }

                            if (envelope.getUserEmail() != null
                                    && envelope.getUserEmail().equalsIgnoreCase(getRealtimeUserEmail())) {
                                return;
                            }

                            Platform.runLater(() -> {
                                if (SocketEnvelope.TYPE_NEW_MESSAGE.equals(envelope.getType())) {
                                    loadMessagesToBottom();
                                    markCurrentChannelAsReadSilently();
                                    hideSummaryBanner();
                                } else if (SocketEnvelope.TYPE_REACTION_CHANGED.equals(envelope.getType())) {
                                    loadMessagesPreservingViewport();
                                } else {
                                    loadMessagesPreservingViewport();
                                }
                            });
                        }

                        @Override
                        public void onDisconnected(String reason) {
                            realtimeReady = false;
                            System.out.println("Realtime chat disconnected: " + reason);
                        }

                        @Override
                        public void onError(Exception ex) {
                            realtimeReady = false;
                            ex.printStackTrace();
                        }
                    }
            );

            boolean joined = socketClient.connectAndJoin(
                    channel.getId(),
                    getRealtimeUserId(),
                    getRealtimeUserName(),
                    getRealtimeUserEmail()
            );

            System.out.println("Realtime join result = " + joined);

            if (!joined) {
                showError("Realtime chat could not connect to the server.");
            }

        } catch (Exception ex) {
            realtimeReady = false;
            ex.printStackTrace();
            showError("Failed to connect realtime chat: " + ex.getMessage());
        }
    }

    private void stopRealtimeChat() {
        if (socketClient == null) return;

        try {
            if (channel != null && socketClient.isOpen()) {
                socketClient.leaveChannel(
                        channel.getId(),
                        getRealtimeUserId(),
                        getRealtimeUserName(),
                        getRealtimeUserEmail()
                );
            }

            socketClient.close();
        } catch (Exception ignored) {
        } finally {
            socketClient = null;
        }
    }

    private int getRealtimeUserId() {
        User currentUser = SessionManager.getCurrentUser();
        return currentUser != null ? currentUser.getId() : 0;
    }

    private String getRealtimeUserName() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return "Guest";

        if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            return currentUser.getUsername();
        }

        return currentUser.getEmail() != null ? currentUser.getEmail() : "Unknown";
    }

    private String getRealtimeUserEmail() {
        User currentUser = SessionManager.getCurrentUser();
        return currentUser != null && currentUser.getEmail() != null
                ? currentUser.getEmail()
                : "guest@local";
    }

    private void refreshHeader() {
        if (channel == null) return;

        if (lblChannelName != null) {
            lblChannelName.setText(channel.getName());
        }

        if (lblChannelDescription != null) {
            String desc = (channel.getDescription() == null || channel.getDescription().isBlank())
                    ? "No description."
                    : channel.getDescription();
            lblChannelDescription.setText(desc);
        }

        if (lblChannelStatus != null) {
            lblChannelStatus.setText(channel.getStatus().toUpperCase());

            String style = switch (channel.getStatus().toLowerCase()) {
                case "approved" ->
                        "-fx-text-fill:#00e676; -fx-border-color:rgba(0,230,118,0.35); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:6 14 6 14; -fx-font-size:11; -fx-font-weight:bold;";
                case "pending" ->
                        "-fx-text-fill:#f6d860; -fx-border-color:rgba(246,216,96,0.35); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:6 14 6 14; -fx-font-size:11; -fx-font-weight:bold;";
                case "rejected" ->
                        "-fx-text-fill:#ff4d3d; -fx-border-color:rgba(232,55,42,0.35); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:6 14 6 14; -fx-font-size:11; -fx-font-weight:bold;";
                default ->
                        "-fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.25); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:6 14 6 14; -fx-font-size:11; -fx-font-weight:bold;";
            };

            lblChannelStatus.setStyle(style);
        }
    }

    private void refreshComposerVisibility() {
        boolean canWrite = SessionManager.canWriteCommunityMessages();

        if (composerBox != null) {
            composerBox.setVisible(canWrite);
            composerBox.setManaged(canWrite);
        }

        if (lblComposerInfo != null) {
            if (SessionManager.getCurrentUser() == null) {
                lblComposerInfo.setText("Sign in as a player to send messages.");
            } else if (!SessionManager.canWriteCommunityMessages()) {
                lblComposerInfo.setText("Only a plain player can write messages here.");
            } else {
                lblComposerInfo.setText("");
            }

            boolean showInfo = !canWrite;
            lblComposerInfo.setVisible(showInfo);
            lblComposerInfo.setManaged(showInfo);
        }
    }

    private void loadMessagesToBottom() {
        loadMessagesInternal(true, null);
    }

    private void loadMessagesKeepingMessage(Integer messageId) {
        loadMessagesInternal(false, messageId);
    }

    private void loadMessagesPreservingViewport() {
        Integer anchorId = captureTopVisibleMessageId();
        loadMessagesInternal(false, anchorId);
    }

    private void loadMessagesInternal(boolean scrollToBottomAfterLoad, Integer anchorMessageId) {
        if (channel == null || messagesContainer == null) return;

        messagesContainer.getChildren().clear();

        try {
            List<Message> messages = messageService.findByChannelForUser(channel.getId(), SessionManager.getCurrentUser());

            if (messages.isEmpty()) {
                VBox emptyBox = new VBox(8);
                emptyBox.setPadding(new Insets(18));
                emptyBox.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.02);" +
                                "-fx-border-color: rgba(255,255,255,0.06);" +
                                "-fx-border-radius: 12;" +
                                "-fx-background-radius: 12;"
                );

                Label title = new Label("No messages yet");
                title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

                Label subtitle = new Label("Be the first to start the discussion.");
                subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-size: 12px;");

                emptyBox.getChildren().addAll(title, subtitle);
                messagesContainer.getChildren().add(emptyBox);

                if (scrollToBottomAfterLoad) {
                    scrollToBottom();
                }
                return;
            }

            for (Message message : messages) {
                messagesContainer.getChildren().add(buildMessageRow(message));
            }

            if (scrollToBottomAfterLoad) {
                scrollToBottom();
            } else if (anchorMessageId != null) {
                scrollToMessage(anchorMessageId);
            }

        } catch (SQLException e) {
            showError("Failed to load messages: " + e.getMessage());
        }
    }

    private Integer captureTopVisibleMessageId() {
        if (messagesScrollPane == null || messagesContainer == null || messagesContainer.getChildren().isEmpty()) {
            return null;
        }

        messagesScrollPane.applyCss();
        messagesScrollPane.layout();
        messagesContainer.applyCss();
        messagesContainer.layout();

        double contentHeight = messagesContainer.getBoundsInLocal().getHeight();
        double viewportHeight = messagesScrollPane.getViewportBounds().getHeight();
        double maxScroll = Math.max(0, contentHeight - viewportHeight);
        double currentY = messagesScrollPane.getVvalue() * maxScroll;

        for (Node node : messagesContainer.getChildren()) {
            Bounds bounds = node.getBoundsInParent();
            if (bounds.getMaxY() >= currentY + 1) {
                Object data = node.getUserData();
                if (data instanceof Integer id) {
                    return id;
                }
            }
        }

        return null;
    }

    private void scrollToBottom() {
        if (messagesScrollPane == null) return;

        Platform.runLater(() -> {
            messagesScrollPane.applyCss();
            messagesScrollPane.layout();
            messagesContainer.applyCss();
            messagesContainer.layout();
            messagesScrollPane.setVvalue(1.0);

            Platform.runLater(() -> {
                messagesScrollPane.applyCss();
                messagesScrollPane.layout();
                messagesContainer.applyCss();
                messagesContainer.layout();
                messagesScrollPane.setVvalue(1.0);
            });
        });
    }

    private void scrollToMessage(Integer messageId) {
        if (messageId == null || messagesScrollPane == null || messagesContainer == null) return;

        Platform.runLater(() -> {
            messagesScrollPane.applyCss();
            messagesScrollPane.layout();
            messagesContainer.applyCss();
            messagesContainer.layout();

            Node target = null;
            for (Node node : messagesContainer.getChildren()) {
                if (messageId.equals(node.getUserData())) {
                    target = node;
                    break;
                }
            }

            if (target == null) return;

            double contentHeight = messagesContainer.getBoundsInLocal().getHeight();
            double viewportHeight = messagesScrollPane.getViewportBounds().getHeight();
            double targetY = target.getBoundsInParent().getMinY();

            double desiredY = Math.max(0, targetY - 80);
            double maxScroll = Math.max(1, contentHeight - viewportHeight);
            double vValue = desiredY / maxScroll;

            messagesScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
        });
    }

    private HBox buildMessageRow(Message message) {
        User currentUser = SessionManager.getCurrentUser();
        boolean isMine = currentUser != null
                && message.getSender_email() != null
                && message.getSender_email().equalsIgnoreCase(currentUser.getEmail());

        HBox row = new HBox();
        row.setUserData(message.getId());
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox wrapper = new VBox(6);
        wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setMaxWidth(560);

        VBox bubble = new VBox(8);
        bubble.setMaxWidth(540);
        bubble.setPadding(new Insets(14));
        bubble.setStyle(
                isMine
                        ? "-fx-background-color: rgba(232,55,42,0.12);" +
                        "-fx-border-color: rgba(232,55,42,0.26);" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;"
                        : "-fx-background-color: rgba(255,255,255,0.03);" +
                        "-fx-border-color: rgba(255,255,255,0.07);" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;"
        );

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);

        Label sender = new Label(message.getSender_name() == null ? "Unknown" : message.getSender_name());
        sender.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(formatTimestamp(message.getSentAt()));
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.38); -fx-font-size: 10px;");

        String speakableText = message.getContent() == null ? "" : message.getContent().trim();
        if (!message.isIs_deleted() && !speakableText.isBlank()) {
            Button btnSpeak = new Button("🔊");
            btnSpeak.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: rgba(255,255,255,0.75);" +
                            "-fx-font-size: 12px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 0 6 0 6;"
            );
            btnSpeak.setOnAction(e -> handleSpeakMessage(message));
            top.getChildren().add(btnSpeak);
        }

        top.getChildren().addAll(sender, spacer, date);

        if (isMine && !message.isIs_deleted() && SessionManager.canWriteCommunityMessages()) {
            Button btnMenu = new Button("⋮");
            btnMenu.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: rgba(255,255,255,0.70);" +
                            "-fx-font-size: 15px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 0 0 0 8;"
            );
            btnMenu.setOnAction(e -> {
                if (actionMenuMessageId != null && actionMenuMessageId.equals(message.getId())) {
                    actionMenuMessageId = null;
                } else {
                    actionMenuMessageId = message.getId();
                    deleteConfirmMessageId = null;
                }
                loadMessagesKeepingMessage(message.getId());
            });
            top.getChildren().add(btnMenu);
        }

        bubble.getChildren().add(top);

        if (editingMessageId != null && editingMessageId.equals(message.getId())) {
            bubble.getChildren().add(buildInlineEditBox(message));
        } else {
            String visibleText = message.getDisplayContent();
            boolean showText = visibleText != null && !visibleText.isBlank();

            if (showText) {
                Label content = new Label(visibleText);
                content.setWrapText(true);

                if (message.isIs_deleted()) {
                    content.setStyle("-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 13px; -fx-font-style: italic;");
                } else {
                    content.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 13px;");
                }

                bubble.getChildren().add(content);
            }

            if (!message.isIs_deleted() && message.hasAttachments()) {
                for (MessageAttachment attachment : message.getAttachments()) {
                    bubble.getChildren().add(buildAttachmentNode(attachment));
                }
            }

            if (!message.isIs_deleted()) {
                bubble.getChildren().add(buildReactionUi(message));
            }
        }

        wrapper.getChildren().add(bubble);

        if (isMine && !message.isIs_deleted() && actionMenuMessageId != null && actionMenuMessageId.equals(message.getId())) {
            wrapper.getChildren().add(buildMessageActionMenu(message));
        }

        if (isMine && !message.isIs_deleted() && deleteConfirmMessageId != null && deleteConfirmMessageId.equals(message.getId())) {
            wrapper.getChildren().add(buildInlineDeleteConfirm(message));
        }

        row.getChildren().add(wrapper);
        return row;
    }

    private VBox buildReactionUi(Message message) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 0, 0, 0));

        HBox summary = buildReactionSummary(message);
        if (!summary.getChildren().isEmpty()) {
            box.getChildren().add(summary);
        }

        if (reactionPickerMessageId != null && reactionPickerMessageId.equals(message.getId())) {
            box.getChildren().add(buildReactionPicker(message));
        } else {
            box.getChildren().add(buildReactionLauncher(message));
        }

        return box;
    }

    private HBox buildReactionSummary(Message message) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        addReactionChip(row, message, Message.REACTION_LIKE);
        addReactionChip(row, message, Message.REACTION_LOVE);
        addReactionChip(row, message, Message.REACTION_HAHA);
        addReactionChip(row, message, Message.REACTION_ANGRY);

        return row;
    }

    private void addReactionChip(HBox row, Message message, String reactionType) {
        int count = message.getReactionCount(reactionType);
        if (count <= 0) {
            return;
        }

        boolean selected = message.isUserReaction(reactionType);

        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4, 8, 4, 6));
        chip.setStyle(
                selected
                        ? "-fx-background-color: rgba(232,55,42,0.16);" +
                        "-fx-border-color: rgba(232,55,42,0.32);" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
                        : "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;"
        );

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        chip.getChildren().addAll(buildReactionIconView(reactionType, 16), countLabel);
        row.getChildren().add(chip);
    }

    private HBox buildReactionPicker(Message message) {
        HBox picker = new HBox(8);
        picker.setAlignment(Pos.CENTER_LEFT);
        picker.setPadding(new Insets(6, 8, 6, 8));
        picker.setStyle(
                "-fx-background-color: rgba(20,24,33,0.96);" +
                        "-fx-border-color: rgba(255,255,255,0.10);" +
                        "-fx-border-radius: 24;" +
                        "-fx-background-radius: 24;"
        );

        picker.getChildren().add(buildReactionIconButton(message, Message.REACTION_LOVE));
        picker.getChildren().add(buildReactionIconButton(message, Message.REACTION_HAHA));
        picker.getChildren().add(buildReactionIconButton(message, Message.REACTION_ANGRY));
        picker.getChildren().add(buildReactionIconButton(message, Message.REACTION_LIKE));

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: rgba(255,255,255,0.70);" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> {
            reactionPickerMessageId = null;
            loadMessagesKeepingMessage(message.getId());
        });

        picker.getChildren().add(closeBtn);
        return picker;
    }

    private Button buildReactionIconButton(Message message, String reactionType) {
        Button button = new Button();
        button.setGraphic(buildReactionIconView(reactionType, 24));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-padding: 2;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setScaleX(1.15);
            button.setScaleY(1.15);
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        button.setOnAction(e -> handleToggleReaction(message, reactionType));
        return button;
    }

    private Button buildReactionLauncher(Message message) {
        String iconType = message.getUserReaction() != null ? message.getUserReaction() : Message.REACTION_LIKE;

        Button button = new Button(message.getUserReaction() != null ? "Reacted" : "React");
        button.setGraphic(buildReactionIconView(iconType, 16));
        button.setStyle(
                message.getUserReaction() != null
                        ? "-fx-background-color: rgba(232,55,42,0.16);" +
                        "-fx-border-color: rgba(232,55,42,0.28);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 5 10 5 10;" +
                        "-fx-cursor: hand;"
                        : "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-text-fill: rgba(255,255,255,0.88);" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 5 10 5 10;" +
                        "-fx-cursor: hand;"
        );

        button.setOnAction(e -> {
            reactionPickerMessageId = message.getId();
            loadMessagesKeepingMessage(message.getId());
        });

        return button;
    }

    private ImageView buildReactionIconView(String reactionType, double size) {
        String path = switch (reactionType.toUpperCase()) {
            case "LOVE" -> "/com/eyetwin/assets/reactions/love.png";
            case "HAHA" -> "/com/eyetwin/assets/reactions/haha.png";
            case "ANGRY" -> "/com/eyetwin/assets/reactions/angry.png";
            default -> "/com/eyetwin/assets/reactions/like.png";
        };

        ImageView view = new ImageView(new Image(getClass().getResourceAsStream(path)));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private void handleToggleReaction(Message message, String reactionType) {
        try {
            if (!SessionManager.canWriteCommunityMessages()) {
                showError("Only a plain player can react to messages.");
                return;
            }

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    messageService.toggleReaction(message.getId(), reactionType, SessionManager.getCurrentUser());
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                reactionPickerMessageId = null;

                if (socketClient != null && socketClient.isOpen() && realtimeReady) {
                    socketClient.publishReactionEvent(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail()
                    );
                }

                refreshSingleMessageRow(message.getId());
            });

            task.setOnFailed(event -> {
                reactionPickerMessageId = null;
                Throwable ex = task.getException();
                showError("Failed to update reaction: " + (ex != null ? ex.getMessage() : "Unknown error"));
            });

            Thread thread = new Thread(task, "community-message-reaction");
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            reactionPickerMessageId = null;
            showError("Failed to update reaction: " + e.getMessage());
        }
    }

    private void handleSpeakMessage(Message message) {
        try {
            if (message == null || message.getContent() == null || message.getContent().trim().isBlank()) {
                showError("Only text messages can be read aloud.");
                return;
            }

            showSpeechInfo("Generating speech...");

            Task<File> task = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return textToSpeechService.synthesizeToFile(message.getContent().trim());
                }
            };

            task.setOnSucceeded(event -> {
                try {
                    File audioFile = task.getValue();
                    playSpeechFile(audioFile);
                    showSpeechInfo("Playing message audio.");
                    hideSpeechInfoLater();
                } catch (Exception ex) {
                    showError("Failed to play message audio: " + ex.getMessage());
                }
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                showError("Failed to speak message: " + (ex != null ? ex.getMessage() : "Unknown error"));
            });

            Thread thread = new Thread(task, "community-tts");
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            showError("Failed to speak message: " + e.getMessage());
        }
    }

    private VBox buildInlineEditBox(Message message) {
        VBox box = new VBox(8);

        TextArea editArea = new TextArea(message.getContent());
        editArea.setWrapText(true);
        editArea.setPrefRowCount(3);
        editArea.setStyle(
                "-fx-control-inner-background: #14192b;" +
                        "-fx-background-color: #14192b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #9CA3AF;" +
                        "-fx-highlight-fill: rgba(232,55,42,0.35);" +
                        "-fx-highlight-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.08);"
        );

        Label error = new Label();
        error.setStyle("-fx-text-fill: #ff7b7b; -fx-font-size: 11px;");
        error.setVisible(false);
        error.setManaged(false);

        HBox actions = new HBox(8);

        Button btnSave = new Button("Save");
        btnSave.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        btnSave.setOnAction(e -> {
            try {
                String validation = CommunityValidator.validateMessageContent(editArea.getText());
                if (validation != null) {
                    error.setText(validation);
                    error.setVisible(true);
                    error.setManaged(true);
                    return;
                }

                MessageModerationResult moderationPreview = previewModeration(editArea.getText());

                messageService.updateOwnMessage(message.getId(), editArea.getText(), SessionManager.getCurrentUser());

                editingMessageId = null;
                actionMenuMessageId = null;
                deleteConfirmMessageId = null;
                reactionPickerMessageId = null;

                if (socketClient != null && socketClient.isOpen()) {
                    socketClient.publishEditEvent(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail()
                    );
                }

                loadMessagesKeepingMessage(message.getId());
                showModerationHint(moderationPreview, true);

            } catch (Exception ex) {
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
            }
        });

        btnCancel.setOnAction(e -> {
            editingMessageId = null;
            loadMessagesKeepingMessage(message.getId());
        });

        actions.getChildren().addAll(btnSave, btnCancel);
        box.getChildren().addAll(editArea, error, actions);
        return box;
    }

    private VBox buildMessageActionMenu(Message message) {
        VBox menu = new VBox(6);
        menu.setPadding(new Insets(8));
        menu.setMaxWidth(170);
        menu.setStyle(
                "-fx-background-color: #141821;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        Button editBtn = new Button("✏ Edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setAlignment(Pos.CENTER_LEFT);
        editBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> {
            editingMessageId = message.getId();
            actionMenuMessageId = null;
            deleteConfirmMessageId = null;
            reactionPickerMessageId = null;
            loadMessagesKeepingMessage(message.getId());
        });

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setAlignment(Pos.CENTER_LEFT);
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #ff6b6b;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> {
            actionMenuMessageId = null;
            deleteConfirmMessageId = message.getId();
            reactionPickerMessageId = null;
            loadMessagesKeepingMessage(message.getId());
        });

        menu.getChildren().addAll(editBtn, deleteBtn);
        return menu;
    }

    private VBox buildInlineDeleteConfirm(Message message) {
        VBox confirmBox = new VBox(8);
        confirmBox.setPadding(new Insets(10));
        confirmBox.setMaxWidth(240);
        confirmBox.setStyle(
                "-fx-background-color: #141821;" +
                        "-fx-border-color: rgba(232,55,42,0.20);" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        Label text = new Label("Delete this message?");
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox actions = new HBox(8);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> {
            try {
                messageService.softDeleteOwnMessage(message.getId(), SessionManager.getCurrentUser());

                deleteConfirmMessageId = null;
                actionMenuMessageId = null;
                editingMessageId = null;
                reactionPickerMessageId = null;

                if (socketClient != null && socketClient.isOpen()) {
                    socketClient.publishDeleteEvent(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail()
                    );
                }

                loadMessagesKeepingMessage(message.getId());

            } catch (Exception ex) {
                showError("Failed to delete message: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 14 8 14;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> {
            deleteConfirmMessageId = null;
            loadMessagesKeepingMessage(message.getId());
        });

        actions.getChildren().addAll(deleteBtn, cancelBtn);
        confirmBox.getChildren().addAll(text, actions);
        return confirmBox;
    }

    @FXML
    private void handleChooseAttachments() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose attachments");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Allowed files",
                        "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp",
                        "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx",
                        "*.ppt", "*.pptx", "*.txt", "*.zip"
                )
        );

        List<File> files = chooser.showOpenMultipleDialog(btnSend.getScene().getWindow());
        if (files == null || files.isEmpty()) {
            return;
        }

        try {
            for (File file : files) {
                CommunityFileValidator.validate(file);
            }

            selectedAttachments.clear();
            selectedAttachments.addAll(files);
            refreshAttachmentSelectionUi();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleClearAttachments() {
        selectedAttachments.clear();
        refreshAttachmentSelectionUi();
    }

    private void refreshAttachmentSelectionUi() {
        boolean hasFiles = !selectedAttachments.isEmpty();

        if (lblAttachmentNames != null) {
            lblAttachmentNames.setVisible(hasFiles);
            lblAttachmentNames.setManaged(hasFiles);

            if (!hasFiles) {
                lblAttachmentNames.setText("");
            } else if (selectedAttachments.size() == 1) {
                lblAttachmentNames.setText(selectedAttachments.get(0).getName());
            } else {
                lblAttachmentNames.setText(selectedAttachments.size() + " files selected");
            }
        }

        if (btnClearAttachments != null) {
            btnClearAttachments.setVisible(hasFiles);
            btnClearAttachments.setManaged(hasFiles);
        }
    }

    private void setComposerBusy(boolean busy) {
        if (taNewMessage != null) taNewMessage.setDisable(busy);
        if (btnSend != null) btnSend.setDisable(busy);
        if (btnAttach != null) btnAttach.setDisable(busy);
        if (btnGif != null) btnGif.setDisable(busy);
        if (btnClearAttachments != null) btnClearAttachments.setDisable(busy);
        if (btnRecordVoice != null) btnRecordVoice.setDisable(busy || audioRecorderService.isRecording());
        if (btnStopVoice != null) btnStopVoice.setDisable(busy || !audioRecorderService.isRecording());
    }

    @FXML
    private void handleOpenGifPicker() {
        try {
            if (!SessionManager.canWriteCommunityMessages()) {
                showError("Only a plain player can send messages.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/GiphyPicker.fxml"));
            Parent root = loader.load();

            GiphyPickerController controller = loader.getController();
            controller.setOnGifSelected(this::sendGifMessage);

            Stage popup = new Stage();
            popup.setTitle("Choose GIF");
            popup.setScene(new Scene(root));
            popup.setWidth(700);
            popup.setHeight(620);
            popup.setMinWidth(650);
            popup.setMinHeight(560);
            popup.initOwner((Stage) lblChannelName.getScene().getWindow());
            popup.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open GIF picker: " + e);
        }
    }

    private void sendGifMessage(GiphyGif gif) {
        if (gif == null || channel == null) {
            return;
        }

        setComposerBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                MessageAttachment gifAttachment = new MessageAttachment();
                gifAttachment.setOriginalName(
                        (gif.getTitle() == null || gif.getTitle().isBlank() ? "giphy" : gif.getTitle()) + ".gif"
                );
                gifAttachment.setStoredName("giphy-" + gif.getId() + ".gif");
                gifAttachment.setMimeType("image/gif");
                gifAttachment.setSize(0);
                gifAttachment.setUrl(gif.getSendUrl());
                gifAttachment.setPublicId("giphy:" + gif.getId());
                gifAttachment.setCloudResourceType("image");

                List<MessageAttachment> attachments = new ArrayList<>();
                attachments.add(gifAttachment);

                messageService.sendMessage(
                        channel.getId(),
                        "",
                        SessionManager.getCurrentUser(),
                        attachments
                );

                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setComposerBusy(false);
            reactionPickerMessageId = null;

            if (socketClient != null && socketClient.isOpen() && realtimeReady) {
                socketClient.publishMessage(
                        channel.getId(),
                        getRealtimeUserId(),
                        getRealtimeUserName(),
                        getRealtimeUserEmail(),
                        "[GIF]"
                );
            }

            loadMessagesToBottom();
            markCurrentChannelAsReadSilently();
            hideSummaryBanner();
        });

        task.setOnFailed(event -> {
            setComposerBusy(false);
            Throwable ex = task.getException();
            showError("Failed to send GIF: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(task, "community-send-gif");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleStartVoiceRecording() {
        try {
            if (!SessionManager.canWriteCommunityMessages()) {
                showError("Only a plain player can send messages.");
                return;
            }

            audioRecorderService.startRecording();
            showSpeechInfo("Recording... click Stop when finished.");
            refreshSpeechUi();

        } catch (Exception e) {
            showError("Failed to start recording: " + e.getMessage());
        }
    }

    @FXML
    private void handleStopVoiceRecording() {
        if (!audioRecorderService.isRecording()) {
            return;
        }

        setComposerBusy(true);
        showSpeechInfo("Transcribing voice...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                File audioFile = audioRecorderService.stopRecording();
                return speechToTextService.transcribe(audioFile);
            }
        };

        task.setOnSucceeded(event -> {
            setComposerBusy(false);
            refreshSpeechUi();

            String transcript = task.getValue() == null ? "" : task.getValue().trim();
            if (transcript.isBlank()) {
                showSpeechInfo("No speech detected.");
                hideSpeechInfoLater();
                return;
            }

            String existing = taNewMessage.getText() == null ? "" : taNewMessage.getText().trim();
            if (existing.isBlank()) {
                taNewMessage.setText(transcript);
            } else {
                taNewMessage.setText(existing + " " + transcript);
            }

            showSpeechInfo("Voice converted to text.");
            hideSpeechInfoLater();
        });

        task.setOnFailed(event -> {
            setComposerBusy(false);
            refreshSpeechUi();
            Throwable ex = task.getException();
            showError("Failed to transcribe voice: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(task, "community-stt");
        thread.setDaemon(true);
        thread.start();
    }

    private String formatBytes(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private void openAttachment(String url) {
        try {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Attachment URL is empty.");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            showError("Cannot open attachment: " + e.getMessage());
        }
    }

    private Node buildAttachmentNode(MessageAttachment attachment) {
        if (attachment.isImage()) {
            VBox box = new VBox(6);
            box.setPadding(new Insets(8));
            box.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.04);" +
                            "-fx-border-color: rgba(255,255,255,0.08);" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;"
            );

            ImageView imageView = new ImageView();
            Image image = new Image(attachment.getUrl(), false);
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(260);
            imageView.setSmooth(true);
            imageView.setStyle("-fx-cursor: hand;");
            imageView.setOnMouseClicked(e -> openAttachment(attachment.getUrl()));

            Hyperlink link = new Hyperlink(
                    attachment.getOriginalName() != null ? attachment.getOriginalName() : "Open image"
            );
            link.setStyle("-fx-text-fill: #ff8a7a; -fx-font-weight: bold;");
            link.setOnAction(e -> openAttachment(attachment.getUrl()));

            Label meta = new Label(
                    (attachment.getMimeType() != null ? attachment.getMimeType() : "image")
                            + " • " + formatBytes(attachment.getSize())
            );
            meta.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");

            box.getChildren().addAll(imageView, link, meta);
            return box;
        }

        VBox box = new VBox(4);
        box.setPadding(new Insets(10));
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        Hyperlink link = new Hyperlink(
                attachment.getOriginalName() != null ? attachment.getOriginalName() : "Open attachment"
        );
        link.setStyle("-fx-text-fill: #ff8a7a; -fx-font-weight: bold;");
        link.setOnAction(e -> openAttachment(attachment.getUrl()));

        Label meta = new Label(
                (attachment.getMimeType() != null ? attachment.getMimeType() : "file")
                        + " • " + formatBytes(attachment.getSize())
        );
        meta.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");

        box.getChildren().addAll(link, meta);
        return box;
    }

    @FXML
    private void handleSendMessage() {
        if (channel == null) return;

        try {
            if (!SessionManager.canWriteCommunityMessages()) {
                showError("Only a plain player can send messages.");
                return;
            }

            String content = taNewMessage.getText() == null ? "" : taNewMessage.getText().trim();

            String validation = CommunityValidator.validateMessageForSend(content, !selectedAttachments.isEmpty());
            if (validation != null) {
                showError(validation);
                return;
            }

            final MessageModerationResult moderationPreview = previewModeration(content);
            final String contentToSend = content;
            final List<File> filesToUpload = new ArrayList<>(selectedAttachments);

            setComposerBusy(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<MessageAttachment> uploadedAttachments = new ArrayList<>();

                    for (File file : filesToUpload) {
                        MessageAttachment uploaded = cloudinaryUploadService.upload(file);
                        uploadedAttachments.add(uploaded);
                    }

                    messageService.sendMessage(
                            channel.getId(),
                            contentToSend,
                            SessionManager.getCurrentUser(),
                            uploadedAttachments
                    );

                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                taNewMessage.clear();
                selectedAttachments.clear();
                editingMessageId = null;
                actionMenuMessageId = null;
                deleteConfirmMessageId = null;
                reactionPickerMessageId = null;

                refreshAttachmentSelectionUi();
                setComposerBusy(false);

                if (socketClient != null && socketClient.isOpen() && realtimeReady) {
                    socketClient.publishMessage(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail(),
                            contentToSend
                    );
                }

                loadMessagesToBottom();
                markCurrentChannelAsReadSilently();
                hideSummaryBanner();
                showModerationHint(moderationPreview, false);
            });

            task.setOnFailed(event -> {
                setComposerBusy(false);
                Throwable ex = task.getException();
                showError("Failed to send message: " + (ex != null ? ex.getMessage() : "Unknown error"));
            });

            Thread thread = new Thread(task, "community-chat-send-message");
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            setComposerBusy(false);
            showError("Failed to send message: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        stopSpeechPlayback();
        markCurrentChannelAsReadSilently();
        stopRealtimeChat();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/Community.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) lblChannelName.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showError("Failed to go back: " + e.getMessage());
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        return DATE_FMT.format(timestamp.toLocalDateTime());
    }

    private void showModerationHint(MessageModerationResult result, boolean editing) {
        if (result == null || !result.wasModified() || lblComposerInfo == null) {
            return;
        }

        String level = result.isSevere() ? "Severe moderation" : "Moderation";
        String action = editing ? "edited message" : "message";

        lblComposerInfo.setText(level + ": your " + action + " was cleaned automatically (" +
                result.getMatchedCount() + " masked word" + (result.getMatchedCount() > 1 ? "s" : "") + ").");
        lblComposerInfo.setStyle("-fx-text-fill: #f6d860; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblComposerInfo.setVisible(true);
        lblComposerInfo.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(event -> {
            if (SessionManager.canWriteCommunityMessages()) {
                lblComposerInfo.setText("");
                lblComposerInfo.setVisible(false);
                lblComposerInfo.setManaged(false);
            }
        });
        pause.play();
    }

    private MessageModerationResult previewModeration(String content) {
        return moderationService.moderate(content == null ? "" : content.trim());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshMissedSummaryBanner() {
        User currentUser = SessionManager.getCurrentUser();

        if (currentUser == null || channel == null) {
            hideSummaryBanner();
            return;
        }

        try {
            int missedCount = chatSummaryService.getMissedCount(currentUser.getId(), channel.getId());

            if (missedCount >= AISummaryConfig.getThreshold()) {
                summaryBannerBox.setVisible(true);
                summaryBannerBox.setManaged(true);
                lblMissedMessages.setText("You missed " + missedCount + (missedCount == 1 ? " message" : " messages"));
            } else {
                hideSummaryBanner();
            }
        } catch (Exception e) {
            hideSummaryBanner();
        }
    }

    private void hideSummaryBanner() {
        if (summaryBannerBox != null) {
            summaryBannerBox.setVisible(false);
            summaryBannerBox.setManaged(false);
        }

        if (lblSummaryLoading != null) {
            lblSummaryLoading.setVisible(false);
            lblSummaryLoading.setManaged(false);
            lblSummaryLoading.setText("Generating summary...");
        }

        if (btnSummarizeMissed != null) {
            btnSummarizeMissed.setDisable(false);
        }
    }

    private void clearSummaryCard() {
        if (summaryCardBox != null) {
            summaryCardBox.setVisible(false);
            summaryCardBox.setManaged(false);
        }

        if (lblSummaryTitle != null) {
            lblSummaryTitle.setText("");
        }

        if (lblSummaryOverview != null) {
            lblSummaryOverview.setText("");
        }

        clearSectionBox(summaryKeyPointsBox);
        clearSectionBox(summaryActionItemsBox);
        clearSectionBox(summaryOpenQuestionsBox);
    }

    private void clearSectionBox(VBox box) {
        if (box != null) {
            box.getChildren().clear();
            box.setVisible(false);
            box.setManaged(false);
        }
    }

    @FXML
    private void handleHideSummary() {
        clearSummaryCard();
    }

    @FXML
    private void handleSummarizeMissedMessages() {
        User currentUser = SessionManager.getCurrentUser();

        if (currentUser == null || channel == null) {
            return;
        }

        btnSummarizeMissed.setDisable(true);
        lblSummaryLoading.setText("Generating summary...");
        lblSummaryLoading.setVisible(true);
        lblSummaryLoading.setManaged(true);

        Task<ChatSummaryResult> task = new Task<>() {
            @Override
            protected ChatSummaryResult call() throws Exception {
                return chatSummaryService.summarizeMissedMessages(
                        currentUser.getId(),
                        channel.getId(),
                        channel.getName()
                );
            }
        };

        task.setOnSucceeded(event -> {
            btnSummarizeMissed.setDisable(false);
            lblSummaryLoading.setVisible(false);
            lblSummaryLoading.setManaged(false);

            ChatSummaryResult result = task.getValue();
            if (result == null) {
                showError("No missed messages to summarize.");
                hideSummaryBanner();
                clearSummaryCard();
                return;
            }

            renderSummaryCard(result);
            markCurrentChannelAsReadSilently();
            hideSummaryBanner();
        });

        task.setOnFailed(event -> {
            btnSummarizeMissed.setDisable(false);
            Throwable ex = task.getException();

            lblSummaryLoading.setText("Summary failed.");
            showError("Failed to generate summary: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(task, "community-chat-summary");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderSummaryCard(ChatSummaryResult result) {
        if (result == null) return;

        summaryCardBox.setVisible(true);
        summaryCardBox.setManaged(true);

        lblSummaryTitle.setText(result.getTitle() != null && !result.getTitle().isBlank()
                ? result.getTitle()
                : "Missed messages summary");

        lblSummaryOverview.setText(result.getOverview() != null ? result.getOverview() : "");

        renderBulletSection(summaryKeyPointsBox, "Key points", result.getKeyPoints());
        renderBulletSection(summaryActionItemsBox, "Action items", result.getActionItems());
        renderBulletSection(summaryOpenQuestionsBox, "Open questions", result.getOpenQuestions());
    }

    private void renderBulletSection(VBox targetBox, String sectionTitle, List<String> items) {
        clearSectionBox(targetBox);

        if (targetBox == null || items == null || items.isEmpty()) {
            return;
        }

        targetBox.setVisible(true);
        targetBox.setManaged(true);

        Label title = new Label(sectionTitle);
        title.setStyle("-fx-text-fill: #ff8a7a; -fx-font-size: 13px; -fx-font-weight: bold;");
        targetBox.getChildren().add(title);

        for (String item : items) {
            Label bullet = new Label("• " + item);
            bullet.setWrapText(true);
            bullet.setStyle("-fx-text-fill: rgba(255,255,255,0.82); -fx-font-size: 12px;");
            targetBox.getChildren().add(bullet);
        }
    }

    private void markCurrentChannelAsReadSilently() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null || channel == null) return;
            chatSummaryService.markSeenUpToLatest(currentUser.getId(), channel.getId());
        } catch (Exception ignored) {
        }
    }

    private void refreshSingleMessageRow(int messageId) {
        try {
            Message freshMessage = messageService.findByIdForUser(messageId, SessionManager.getCurrentUser());
            if (freshMessage == null) {
                loadMessagesPreservingViewport();
                return;
            }

            for (int i = 0; i < messagesContainer.getChildren().size(); i++) {
                Node node = messagesContainer.getChildren().get(i);
                if (node.getUserData() instanceof Integer id && id == messageId) {
                    messagesContainer.getChildren().set(i, buildMessageRow(freshMessage));
                    return;
                }
            }

            loadMessagesPreservingViewport();

        } catch (Exception e) {
            showError("Failed to refresh reaction: " + e.getMessage());
        }
    }
}