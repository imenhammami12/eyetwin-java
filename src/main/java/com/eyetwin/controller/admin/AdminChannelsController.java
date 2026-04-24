package com.eyetwin.controller.admin;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.ChannelServiceImpl;
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
import java.util.List;

public class AdminChannelsController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbType;
    @FXML private VBox rowsContainer;
    @FXML private Label lblFound;
    @FXML private ScrollPane tableScrollPane;

    private final ChannelServiceImpl channelService = new ChannelServiceImpl();

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) {
            redirectToAdminLogin();
            return;
        }

        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("channels");
        }

        if (adminTopbarController != null) {
            adminTopbarController.setTitle("Channels Management");
        }

        cbStatus.getItems().addAll("all", "pending", "approved", "rejected");
        cbType.getItems().addAll("all", "public", "private");
        cbStatus.setValue("all");
        cbType.setValue("all");

        loadChannels();
    }

    @FXML
    private void handleFilter() {
        loadChannels();
    }

    @FXML
    private void handleClear() {
        tfSearch.clear();
        cbStatus.setValue("all");
        cbType.setValue("all");
        loadChannels();
    }

    @FXML
    private void handleCreateChannel() {
        try {
            URL url = resolveUrl("AdminChannelForm.fxml");
            if (url == null) {
                showError("Failed to open create form: AdminChannelForm.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminChannelFormController controller = loader.getController();
            controller.setModeCreate(this::loadChannels);

            Stage popup = new Stage();
            popup.setTitle("Create Channel");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (IOException e) {
            showError("Failed to open create form: " + e.getMessage());
        }
    }

    private void loadChannels() {
        rowsContainer.getChildren().clear();

        try {
            List<Channel> channels = channelService.findAdminChannels(
                    tfSearch.getText(),
                    cbStatus.getValue(),
                    cbType.getValue()
            );

            lblFound.setText("Found " + channels.size() + " channels");

            if (channels.isEmpty()) {
                Label empty = new Label("No channels found.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 14px;");
                rowsContainer.getChildren().add(empty);
                return;
            }

            for (Channel channel : channels) {
                rowsContainer.getChildren().add(buildRow(channel));
            }

        } catch (SQLException e) {
            showError("Failed to load channels: " + e.getMessage());
        }
    }

    private GridPane buildRow(Channel channel) {
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
                fixedCol(220),
                fixedCol(160),
                fixedCol(130),
                fixedCol(150),
                fixedCol(110),
                fixedCol(220),
                fixedCol(260)
        );

        Label id = text(String.valueOf(channel.getId()));
        Label name = text(channel.getName());
        Label game = text(channel.getGame());
        Label type = badge(channel.getType(), "type");
        Label status = badge(channel.getStatus(), "status");
        Label active = activeBadge(channel.isActive());
        Label createdBy = text(channel.getCreatedBy() == null ? "-" : channel.getCreatedBy());

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnView = iconBtn("◉", "#38bdf8", "View");
        btnView.setOnAction(e -> handleView(channel));

        Button btnEdit = iconBtn("✎", "#fbbf24", "Edit");
        btnEdit.setOnAction(e -> handleEdit(channel));

        Button btnToggle = iconBtn("◐", "#cbd5e1", "Toggle active");
        btnToggle.setOnAction(e -> handleToggle(channel));

        Button btnDelete = iconBtn("🗑", "#fb7185", "Delete");
        btnDelete.setOnAction(e -> handleDelete(channel));

        actions.getChildren().addAll(btnView, btnEdit, btnToggle, btnDelete);

        if ("pending".equalsIgnoreCase(channel.getStatus())) {
            Button btnApprove = iconBtn("✓", "#4ade80", "Approve");
            btnApprove.setOnAction(e -> handleApprove(channel));

            Button btnReject = iconBtn("✕", "#ff8b8b", "Reject");
            btnReject.setOnAction(e -> handleReject(channel));

            actions.getChildren().add(0, btnApprove);
            actions.getChildren().add(1, btnReject);
        }

        row.add(id, 0, 0);
        row.add(name, 1, 0);
        row.add(game, 2, 0);
        row.add(type, 3, 0);
        row.add(status, 4, 0);
        row.add(active, 5, 0);
        row.add(createdBy, 6, 0);
        row.add(actions, 7, 0);

        return row;
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

    private Label badge(String value, String kind) {
        String v = value == null ? "-" : value.toLowerCase();
        String labelText = "-".equals(v) ? "-" : v.substring(0, 1).toUpperCase() + v.substring(1);

        Label l = new Label(labelText);
        String style;

        if ("type".equals(kind)) {
            style = "public".equals(v)
                    ? "-fx-background-color: rgba(0,183,255,0.12); -fx-border-color: rgba(0,183,255,0.30); -fx-text-fill: #6ddcff;"
                    : "-fx-background-color: rgba(156,163,175,0.12); -fx-border-color: rgba(156,163,175,0.30); -fx-text-fill: #cbd5e1;";
        } else {
            style = switch (v) {
                case "approved" -> "-fx-background-color: rgba(34,197,94,0.12); -fx-border-color: rgba(34,197,94,0.30); -fx-text-fill: #4ade80;";
                case "pending" -> "-fx-background-color: rgba(250,204,21,0.12); -fx-border-color: rgba(250,204,21,0.30); -fx-text-fill: #fde047;";
                case "rejected" -> "-fx-background-color: rgba(255,107,107,0.12); -fx-border-color: rgba(255,107,107,0.30); -fx-text-fill: #ff8b8b;";
                default -> "-fx-background-color: rgba(255,255,255,0.08); -fx-border-color: rgba(255,255,255,0.20); -fx-text-fill: white;";
            };
        }

        l.setStyle(style + " -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private Label activeBadge(boolean active) {
        Label l = new Label(active ? "Yes" : "No");
        l.setStyle(
                (active
                        ? "-fx-background-color: rgba(34,197,94,0.12); -fx-border-color: rgba(34,197,94,0.30); -fx-text-fill: #4ade80;"
                        : "-fx-background-color: rgba(156,163,175,0.12); -fx-border-color: rgba(156,163,175,0.30); -fx-text-fill: #cbd5e1;")
                        + " -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: bold;"
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

    private void handleView(Channel channel) {
        try {
            URL url = resolveUrl("AdminChannelDetail.fxml");
            if (url == null) {
                showError("AdminChannelDetail.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminChannelDetailController controller = loader.getController();
            controller.setChannel(channel);

            Stage stage = resolveStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            }

        } catch (Exception e) {
            showError("Failed to open details page: " + e.getMessage());
        }
    }

    private void handleEdit(Channel channel) {
        try {
            URL url = resolveUrl("AdminChannelForm.fxml");
            if (url == null) {
                showError("Failed to open edit form: AdminChannelForm.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminChannelFormController controller = loader.getController();
            controller.setModeEdit(channel, this::loadChannels);

            Stage popup = new Stage();
            popup.setTitle("Edit Channel");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (IOException e) {
            showError("Failed to open edit form: " + e.getMessage());
        }
    }

    private void handleApprove(Channel channel) {
        try {
            User admin = SessionManager.getCurrentUser();
            channelService.approve(channel.getId(), admin);
            loadChannels();
        } catch (Exception e) {
            showError("Failed to approve channel: " + e.getMessage());
        }
    }

    private void handleReject(Channel channel) {
        try {
            URL url = resolveUrl("RejectReasonDialog.fxml");
            if (url == null) {
                showError("Failed to open reject form: RejectReasonDialog.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            RejectReasonDialogController controller = loader.getController();

            Stage popup = new Stage();
            popup.setTitle("Reject Channel");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);

            controller.setStage(popup);
            popup.showAndWait();

            if (controller.isConfirmed()) {
                User admin = SessionManager.getCurrentUser();
                channelService.reject(channel.getId(), controller.getReason(), admin);
                loadChannels();
            }

        } catch (Exception e) {
            showError("Failed to reject channel: " + e.getMessage());
        }
    }

    private void handleToggle(Channel channel) {
        try {
            User admin = SessionManager.getCurrentUser();
            channelService.toggleActive(channel.getId(), admin);
            loadChannels();
        } catch (Exception e) {
            showError("Failed to toggle active state: " + e.getMessage());
        }
    }

    private void handleDelete(Channel channel) {
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
                    "Delete Channel",
                    "Delete channel \"" + channel.getName() + "\" ?",
                    "Delete"
            );

            Stage popup = new Stage();
            popup.setTitle("Delete Channel");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);

            controller.setStage(popup);
            popup.showAndWait();

            if (controller.isConfirmed()) {
                channelService.deleteByAdmin(channel.getId());
                loadChannels();
            }

        } catch (Exception e) {
            showError("Failed to delete channel: " + e.getMessage());
        }
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
        if (rowsContainer != null && rowsContainer.getScene() != null) {
            return (Stage) rowsContainer.getScene().getWindow();
        }
        if (tfSearch != null && tfSearch.getScene() != null) {
            return (Stage) tfSearch.getScene().getWindow();
        }
        return null;
    }

    private String toRgbaBorder(String color) {
        return switch (color.toLowerCase()) {
            case "#38bdf8" -> "rgba(56,189,248,0.45)";
            case "#fbbf24" -> "rgba(251,191,36,0.45)";
            case "#cbd5e1" -> "rgba(203,213,225,0.35)";
            case "#fb7185" -> "rgba(251,113,133,0.45)";
            case "#4ade80" -> "rgba(74,222,128,0.45)";
            case "#ff8b8b" -> "rgba(255,139,139,0.45)";
            default -> "rgba(255,255,255,0.20)";
        };
    }
}
