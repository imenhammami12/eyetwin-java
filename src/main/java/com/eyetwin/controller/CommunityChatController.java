package com.eyetwin.controller;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.MessageServiceImpl;
import com.eyetwin.tools.CommunityValidator;
import com.eyetwin.tools.SessionManager;
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
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.eyetwin.websocket.ChatWebSocketConfig;
import com.eyetwin.websocket.client.ChatSocketListener;
import com.eyetwin.websocket.client.CommunityWebSocketClient;
import com.eyetwin.websocket.model.SocketEnvelope;
import javafx.application.Platform;

import java.net.URI;
import java.time.LocalDateTime;

import com.eyetwin.entities.Community.MessageAttachment;
import com.eyetwin.services.Community.CloudinaryUploadService;
import com.eyetwin.tools.CommunityFileValidator;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;

public class CommunityChatController {

    @FXML private Label lblChannelName;
    @FXML private Label lblChannelDescription;
    @FXML private Label lblChannelStatus;
    @FXML private VBox messagesContainer;
    @FXML private TextArea taNewMessage;
    @FXML private Button btnSend;
    @FXML private VBox composerBox;
    @FXML private Label lblComposerInfo;

    @FXML private Button btnAttach;
    @FXML private Button btnClearAttachments;
    @FXML private Label lblAttachmentNames;

    private final List<File> selectedAttachments = new ArrayList<>();

    private final CloudinaryUploadService cloudinaryUploadService = new CloudinaryUploadService();

    private final MessageServiceImpl messageService = new MessageServiceImpl();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Channel channel;
    private Integer editingMessageId = null;
    private Integer actionMenuMessageId = null;
    private Integer deleteConfirmMessageId = null;

    private CommunityWebSocketClient socketClient;
    private boolean realtimeReady = false;



    public void setChannel(Channel channel) {
        stopRealtimeChat();

        this.channel = channel;
        refreshHeader();
        refreshComposerVisibility();
        loadMessages();
        startRealtimeChat();
    }

    @FXML
    public void initialize() {
        refreshHeader();
        refreshComposerVisibility();
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
                                    && !SocketEnvelope.TYPE_DELETE_MESSAGE.equals(envelope.getType())) {
                                return;
                            }

                            // Ignore my own echoed websocket event
                            if (envelope.getUserEmail() != null
                                    && envelope.getUserEmail().equalsIgnoreCase(getRealtimeUserEmail())) {
                                return;
                            }

                            Platform.runLater(() -> loadMessages());
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

    private void appendRealtimeMessage(SocketEnvelope envelope) {
        if (messagesContainer == null) return;

        removeEmptyStateIfNeeded();

        Message message = new Message();
        message.setChannel_id(envelope.getChannelId());
        message.setContent(envelope.getContent());
        message.setSender_name(envelope.getUserName());
        message.setSender_email(envelope.getUserEmail());
        message.setIs_deleted(false);

        try {
            if (envelope.getSentAt() != null && !envelope.getSentAt().isBlank()) {
                message.setSentAt(Timestamp.valueOf(LocalDateTime.parse(envelope.getSentAt())));
            } else {
                message.setSentAt(new Timestamp(System.currentTimeMillis()));
            }
        } catch (Exception ex) {
            message.setSentAt(new Timestamp(System.currentTimeMillis()));
        }

        messagesContainer.getChildren().add(buildMessageRow(message));
        messagesContainer.requestLayout();
    }

    private void removeEmptyStateIfNeeded() {
        if (messagesContainer == null) return;

        if (messagesContainer.getChildren().size() == 1
                && messagesContainer.getChildren().get(0) instanceof VBox) {
            messagesContainer.getChildren().clear();
        }
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

    private void loadMessages() {
        if (channel == null || messagesContainer == null) return;

        messagesContainer.getChildren().clear();

        try {
            List<Message> messages = messageService.findByChannel(channel.getId());

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
                return;
            }

            for (Message message : messages) {
                messagesContainer.getChildren().add(buildMessageRow(message));
            }

        } catch (SQLException e) {
            showError("Failed to load messages: " + e.getMessage());
        }
    }

    private HBox buildMessageRow(Message message) {
        User currentUser = SessionManager.getCurrentUser();
        boolean isMine = currentUser != null
                && message.getSender_email() != null
                && message.getSender_email().equalsIgnoreCase(currentUser.getEmail());

        HBox row = new HBox();
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
                loadMessages();
            });
            top.getChildren().add(btnMenu);
        }

        bubble.getChildren().add(top);

        if (editingMessageId != null && editingMessageId == message.getId()) {
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
        }

        wrapper.getChildren().add(bubble);

        if (isMine && !message.isIs_deleted() && actionMenuMessageId != null && actionMenuMessageId == message.getId()) {
            wrapper.getChildren().add(buildMessageActionMenu(message, isMine));
        }

        if (isMine && !message.isIs_deleted() && deleteConfirmMessageId != null && deleteConfirmMessageId == message.getId()) {
            wrapper.getChildren().add(buildInlineDeleteConfirm(message, isMine));
        }

        row.getChildren().add(wrapper);
        return row;
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

                messageService.updateOwnMessage(message.getId(), editArea.getText(), SessionManager.getCurrentUser());

                editingMessageId = null;
                actionMenuMessageId = null;
                deleteConfirmMessageId = null;

                if (socketClient != null && socketClient.isOpen()) {
                    socketClient.publishEditEvent(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail()
                    );
                }

                loadMessages();

            } catch (Exception ex) {
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
            }
        });

        btnCancel.setOnAction(e -> {
            editingMessageId = null;
            loadMessages();
        });

        actions.getChildren().addAll(btnSave, btnCancel);
        box.getChildren().addAll(editArea, error, actions);
        return box;
    }

    private VBox buildMessageActionMenu(Message message, boolean isMine) {
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
            loadMessages();
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
            loadMessages();
        });

        menu.getChildren().addAll(editBtn, deleteBtn);
        return menu;
    }

    private VBox buildInlineDeleteConfirm(Message message, boolean isMine) {
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

                if (socketClient != null && socketClient.isOpen()) {
                    socketClient.publishDeleteEvent(
                            channel.getId(),
                            getRealtimeUserId(),
                            getRealtimeUserName(),
                            getRealtimeUserEmail()
                    );
                }

                loadMessages();
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
            loadMessages();
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
        if (btnClearAttachments != null) btnClearAttachments.setDisable(busy);
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
            Image image = new Image(attachment.getUrl(), true);
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

                loadMessages();
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}