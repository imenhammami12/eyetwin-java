package com.eyetwin.controller.admin;

import com.eyetwin.entities.Community.AdminChannelMessageStat;
import com.eyetwin.entities.Community.Message;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.MessageServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class AdminMessagesController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbSort;
    @FXML private Label lblFound;

    @FXML private ToggleButton btnConversationView;
    @FXML private ToggleButton btnTableView;

    @FXML private HBox conversationModeRoot;
    @FXML private VBox tableModeRoot;

    @FXML private VBox channelsContainer;
    @FXML private VBox conversationContainer;
    @FXML private VBox rowsContainer;

    @FXML private Label lblSelectedChannelTitle;

    @FXML private Label lblInspectorSender;
    @FXML private Label lblInspectorEmail;
    @FXML private Label lblInspectorChannel;
    @FXML private Label lblInspectorSentAt;
    @FXML private Label lblInspectorEditedAt;
    @FXML private Label lblInspectorStatus;
    @FXML private Label lblInspectorContent;
    @FXML private Button btnInspectorDelete;
    @FXML private Button btnInspectorRestore;

    private final MessageServiceImpl messageService = new MessageServiceImpl();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Integer selectedChannelId = null;
    private String selectedChannelName = null;
    private Message selectedMessage = null;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("messages");
        }

        if (adminTopbarController != null) {
            adminTopbarController.setTitle("Messages Management");
        }

        cbStatus.getItems().addAll("all", "active", "deleted");
        cbStatus.setValue("all");

        cbSort.getItems().addAll("newest", "oldest");
        cbSort.setValue("newest");

        btnConversationView.setSelected(true);
        showConversationView();

        loadChannelSidebar();
        loadTableMessages();
        clearInspector();
    }

    @FXML
    private void handleFilter() {
        loadChannelSidebar();
        loadTableMessages();

        if (selectedChannelId != null) {
            loadConversation(selectedChannelId, selectedChannelName);
        }
    }

    @FXML
    private void handleClear() {
        tfSearch.clear();
        cbStatus.setValue("all");
        cbSort.setValue("newest");

        loadChannelSidebar();
        loadTableMessages();

        if (selectedChannelId != null) {
            loadConversation(selectedChannelId, selectedChannelName);
        }
    }

    @FXML
    private void showConversationView() {
        btnConversationView.setSelected(true);
        btnTableView.setSelected(false);

        conversationModeRoot.setVisible(true);
        conversationModeRoot.setManaged(true);

        tableModeRoot.setVisible(false);
        tableModeRoot.setManaged(false);
    }

    @FXML
    private void showTableView() {
        btnTableView.setSelected(true);
        btnConversationView.setSelected(false);

        tableModeRoot.setVisible(true);
        tableModeRoot.setManaged(true);

        conversationModeRoot.setVisible(false);
        conversationModeRoot.setManaged(false);
    }

    private void loadChannelSidebar() {
        channelsContainer.getChildren().clear();

        try {
            List<AdminChannelMessageStat> channels = messageService.findAdminChannelStats(tfSearch.getText());

            if (channels.isEmpty()) {
                Label empty = new Label("No channels with messages.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 13px;");
                channelsContainer.getChildren().add(empty);
                return;
            }

            for (AdminChannelMessageStat item : channels) {
                channelsContainer.getChildren().add(buildChannelCard(item));
            }

            if (selectedChannelId == null && !channels.isEmpty()) {
                selectedChannelId = channels.get(0).getChannelId();
                selectedChannelName = channels.get(0).getChannelName();
                loadConversation(selectedChannelId, selectedChannelName);
            }

        } catch (SQLException e) {
            showError("Failed to load channels: " + e.getMessage());
        }
    }

    private VBox buildChannelCard(AdminChannelMessageStat item) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(
                (selectedChannelId != null && selectedChannelId == item.getChannelId()
                        ? "-fx-background-color: rgba(255,60,100,0.12); -fx-border-color: rgba(255,60,100,0.35);"
                        : "-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.06);")
                        + "-fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand;"
        );

        Label name = new Label(item.getChannelName());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label meta = new Label(
                safe(item.getGame()) + " • " + safe(item.getType())
        );
        meta.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");

        HBox badges = new HBox(8);

        Label total = badge(item.getTotalMessages() + " msgs", "#60a5fa");
        Label deleted = badge(item.getDeletedMessages() + " deleted", "#fb7185");

        badges.getChildren().addAll(total, deleted);

        card.getChildren().addAll(name, meta, badges);

        card.setOnMouseClicked(e -> {
            selectedChannelId = item.getChannelId();
            selectedChannelName = item.getChannelName();
            loadChannelSidebar();
            loadConversation(item.getChannelId(), item.getChannelName());
        });

        return card;
    }

    private void loadConversation(int channelId, String channelName) {
        conversationContainer.getChildren().clear();
        lblSelectedChannelTitle.setText(channelName == null ? "Conversation" : channelName);

        try {
            List<Message> messages = messageService.findAdminMessagesByChannel(
                    channelId,
                    tfSearch.getText(),
                    cbStatus.getValue(),
                    cbSort.getValue()
            );

            lblFound.setText("Found " + messages.size() + " messages");

            if (messages.isEmpty()) {
                Label empty = new Label("No messages in this channel for the selected filters.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 14px;");
                conversationContainer.getChildren().add(empty);
                return;
            }

            for (Message message : messages) {
                conversationContainer.getChildren().add(buildConversationCard(message));
            }

        } catch (SQLException e) {
            showError("Failed to load channel messages: " + e.getMessage());
        }
    }

    private VBox buildConversationCard(Message message) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
                (selectedMessage != null && selectedMessage.getId() == message.getId()
                        ? "-fx-background-color: rgba(255,60,100,0.10); -fx-border-color: rgba(255,60,100,0.28);"
                        : "-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.06);")
                        + "-fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand;"
        );

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label sender = new Label(safe(message.getSender_name()));
        sender.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label email = new Label(safe(message.getSender_email()));
        email.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(message.getSentAt() == null ? "-" : dateFormat.format(message.getSentAt()));
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");

        top.getChildren().addAll(sender, email, spacer, date);

        Label content = new Label(safe(message.getContent()));
        content.setWrapText(true);
        content.setStyle(
                message.isIs_deleted()
                        ? "-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 13px;"
                        : "-fx-text-fill: white; -fx-font-size: 13px;"
        );

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);

        Label status = statusBadge(message.isIs_deleted());
        bottom.getChildren().add(status);

        if (message.getEditedAt() != null && message.getSentAt() != null
                && !message.getEditedAt().equals(message.getSentAt())) {
            Label edited = badge("Edited", "#f6d860");
            bottom.getChildren().add(edited);
        }

        card.getChildren().addAll(top, content, bottom);

        card.setOnMouseClicked(e -> {
            selectedMessage = message;
            updateInspector(message);
            loadConversation(selectedChannelId, selectedChannelName);
        });

        return card;
    }

    private void loadTableMessages() {
        rowsContainer.getChildren().clear();

        try {
            List<Message> messages = messageService.findAdminMessages(
                    tfSearch.getText(),
                    cbStatus.getValue()
            );

            if (messages.isEmpty()) {
                Label empty = new Label("No messages found.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 14px;");
                rowsContainer.getChildren().add(empty);
                return;
            }

            for (Message message : messages) {
                rowsContainer.getChildren().add(buildTableRow(message));
            }

        } catch (SQLException e) {
            showError("Failed to load table messages: " + e.getMessage());
        }
    }

    private GridPane buildTableRow(Message message) {
        GridPane row = new GridPane();
        row.setHgap(10);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                        "-fx-border-color: rgba(255,255,255,0.05);" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        row.getColumnConstraints().addAll(
                fixedCol(70),
                fixedCol(320),
                fixedCol(180),
                fixedCol(220),
                fixedCol(170),
                fixedCol(110),
                fixedCol(150)
        );

        Label id = text(String.valueOf(message.getId()));

        Label content = text(message.getContent());
        content.setWrapText(true);
        content.setMaxWidth(320);

        String senderValue = safe(message.getSender_name()) + "\n" + safe(message.getSender_email());
        Label sender = text(senderValue);
        sender.setWrapText(true);
        sender.setMaxWidth(180);

        String channelValue = message.getChannelName() != null && !message.getChannelName().isBlank()
                ? message.getChannelName() + " (#" + message.getChannel_id() + ")"
                : "#" + message.getChannel_id();
        Label channel = text(channelValue);
        channel.setWrapText(true);
        channel.setMaxWidth(220);

        Label sentAt = text(
                message.getSentAt() == null ? "-" : dateFormat.format(message.getSentAt())
        );

        Label status = statusBadge(message.isIs_deleted());

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnView = iconBtn("👁", "#60a5fa", "Inspect");
        btnView.setOnAction(e -> {
            selectedMessage = message;
            selectedChannelId = message.getChannel_id();
            selectedChannelName = message.getChannelName();
            updateInspector(message);
            showConversationView();
            loadChannelSidebar();
            loadConversation(selectedChannelId, selectedChannelName);
        });

        Button btnAction;
        if (message.isIs_deleted()) {
            btnAction = iconBtn("↺", "#4ade80", "Restore");
            btnAction.setOnAction(e -> handleRestore(message));
        } else {
            btnAction = iconBtn("🗑", "#fb7185", "Delete");
            btnAction.setOnAction(e -> handleDelete(message));
        }

        actions.getChildren().addAll(btnView, btnAction);

        row.add(id, 0, 0);
        row.add(content, 1, 0);
        row.add(sender, 2, 0);
        row.add(channel, 3, 0);
        row.add(sentAt, 4, 0);
        row.add(status, 5, 0);
        row.add(actions, 6, 0);

        return row;
    }

    private void updateInspector(Message message) {
        if (message == null) {
            clearInspector();
            return;
        }

        lblInspectorSender.setText("Sender: " + safe(message.getSender_name()));
        lblInspectorEmail.setText("Email: " + safe(message.getSender_email()));
        lblInspectorChannel.setText("Channel: " + safe(message.getChannelName()) + " (#" + message.getChannel_id() + ")");
        lblInspectorSentAt.setText("Sent at: " + (message.getSentAt() == null ? "-" : dateFormat.format(message.getSentAt())));
        lblInspectorEditedAt.setText("Edited at: " + (message.getEditedAt() == null ? "-" : dateFormat.format(message.getEditedAt())));
        lblInspectorStatus.setText("Status: " + (message.isIs_deleted() ? "Deleted" : "Active"));
        lblInspectorContent.setText(safe(message.getContent()));

        btnInspectorDelete.setDisable(message.isIs_deleted());
        btnInspectorRestore.setDisable(!message.isIs_deleted());
    }

    private void clearInspector() {
        lblInspectorSender.setText("Sender: -");
        lblInspectorEmail.setText("Email: -");
        lblInspectorChannel.setText("Channel: -");
        lblInspectorSentAt.setText("Sent at: -");
        lblInspectorEditedAt.setText("Edited at: -");
        lblInspectorStatus.setText("Status: -");
        lblInspectorContent.setText("Select a message to inspect it.");
        btnInspectorDelete.setDisable(true);
        btnInspectorRestore.setDisable(true);
        selectedMessage = null;
    }

    @FXML
    private void handleInspectorDelete() {
        if (selectedMessage != null) {
            handleDelete(selectedMessage);
        }
    }

    @FXML
    private void handleInspectorRestore() {
        if (selectedMessage != null) {
            handleRestore(selectedMessage);
        }
    }

    private void handleDelete(Message message) {
        try {
            URL url = resolveUrl("AdminConfirmDialog.fxml");
            if (url == null) {
                showError("AdminConfirmDialog.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminConfirmDialogController controller = loader.getController();
            controller.setData(
                    "Delete Message",
                    "Delete this message from " + safe(message.getSender_name()) + " ?",
                    "Delete"
            );

            Stage popup = new Stage();
            popup.setTitle("Delete Message");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);

            controller.setStage(popup);
            popup.showAndWait();

            if (controller.isConfirmed()) {
                User admin = SessionManager.getCurrentUser();
                messageService.adminDeleteMessage(message.getId(), admin);
                refreshAfterModeration(message.getChannel_id(), message.getChannelName(), message.getId());
            }

        } catch (Exception e) {
            showError("Failed to delete message: " + e.getMessage());
        }
    }

    private void handleRestore(Message message) {
        try {
            URL url = resolveUrl("AdminConfirmDialog.fxml");
            if (url == null) {
                showError("AdminConfirmDialog.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminConfirmDialogController controller = loader.getController();
            controller.setData(
                    "Restore Message",
                    "Restore this deleted message from " + safe(message.getSender_name()) + " ?",
                    "Restore"
            );

            Stage popup = new Stage();
            popup.setTitle("Restore Message");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);

            controller.setStage(popup);
            popup.showAndWait();

            if (controller.isConfirmed()) {
                User admin = SessionManager.getCurrentUser();
                messageService.adminRestoreMessage(message.getId(), admin);
                refreshAfterModeration(message.getChannel_id(), message.getChannelName(), message.getId());
            }

        } catch (Exception e) {
            showError("Failed to restore message: " + e.getMessage());
        }
    }

    private void refreshAfterModeration(int channelId, String channelName, int messageId) {
        loadTableMessages();
        loadChannelSidebar();
        loadConversation(channelId, channelName);

        try {
            selectedMessage = messageService.findById(messageId);
            updateInspector(selectedMessage);
        } catch (SQLException e) {
            clearInspector();
        }
    }

    private ColumnConstraints fixedCol(double width) {
        ColumnConstraints c = new ColumnConstraints();
        c.setMinWidth(width);
        c.setPrefWidth(width);
        c.setMaxWidth(width);
        c.setHgrow(Priority.NEVER);
        return c;
    }

    private Label text(String value) {
        Label l = new Label(value == null ? "-" : value);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        l.setWrapText(false);
        return l;
    }

    private Label statusBadge(boolean deleted) {
        Label l = new Label(deleted ? "Deleted" : "Active");
        l.setStyle(
                (deleted
                        ? "-fx-background-color: rgba(255,107,107,0.12); -fx-border-color: rgba(255,107,107,0.30); -fx-text-fill: #ff8b8b;"
                        : "-fx-background-color: rgba(34,197,94,0.12); -fx-border-color: rgba(34,197,94,0.30); -fx-text-fill: #4ade80;")
                        + " -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: bold;"
        );
        return l;
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-border-color: " + toRgbaBorder(color) + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 4 10 4 10;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );
        return l;
    }

    private Button iconBtn(String symbol, String color, String tooltipText) {
        Button b = new Button(symbol);
        b.setTooltip(new Tooltip(tooltipText));

        b.setMinSize(34, 34);
        b.setPrefSize(34, 34);
        b.setMaxSize(34, 34);

        b.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-border-color: " + toRgbaBorder(color) + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Segoe UI Symbol';" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-alignment: center;"
        );

        return b;
    }

    private void redirectToAdminLogin() {
        try {
            URL url = resolveUrl("AdminLogin.fxml");
            if (url == null) {
                showError("AdminLogin.fxml not found");
                return;
            }

            Parent root = FXMLLoader.load(url);

            Stage stage = resolveStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException e) {
            showError("Failed to open admin login: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private URL resolveUrl(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/" + fxml,
                "/com/eyetwin/" + fxml
        };
        for (String p : paths) {
            URL u = getClass().getResource(p);
            if (u != null) return u;
        }
        return null;
    }

    private Stage resolveStage() {
        if (channelsContainer != null && channelsContainer.getScene() != null) {
            return (Stage) channelsContainer.getScene().getWindow();
        }
        if (tfSearch != null && tfSearch.getScene() != null) {
            return (Stage) tfSearch.getScene().getWindow();
        }
        return null;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String toRgbaBorder(String color) {
        return switch (color.toLowerCase()) {
            case "#4ade80" -> "rgba(74,222,128,0.45)";
            case "#fb7185" -> "rgba(251,113,133,0.45)";
            case "#60a5fa" -> "rgba(96,165,250,0.45)";
            case "#f6d860" -> "rgba(246,216,96,0.45)";
            default -> "rgba(255,255,255,0.20)";
        };
    }
}
