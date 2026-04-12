package com.eyetwin.controller.admin;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.ChannelServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminChannelDetailController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private Label lblAvatar;
    @FXML private Label lblName;
    @FXML private Label lblGameSmall;
    @FXML private Label lblTypeBadge;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblDescription;

    @FXML private Label lblInfoName;
    @FXML private Label lblInfoGame;
    @FXML private Label lblInfoType;
    @FXML private Label lblInfoStatus;
    @FXML private Label lblInfoActive;
    @FXML private Label lblInfoCreatedBy;
    @FXML private Label lblInfoCreatedAt;
    @FXML private Label lblInfoApprovedBy;

    @FXML private Button btnApprove;
    @FXML private Button btnReject;

    private final ChannelServiceImpl channelService = new ChannelServiceImpl();
    private Channel channel;

    @FXML
    public void initialize() {
        if (adminSidebarController != null) {
            adminSidebarController.setActivePage("channels");
        }
        if (adminTopbarController != null) {
            adminTopbarController.setTitle("Channel Details");
        }
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        refreshView();
    }

    private void refreshView() {
        if (channel == null) return;

        String initials = safe(channel.getName()).length() >= 2
                ? safe(channel.getName()).substring(0, 2).toUpperCase()
                : safe(channel.getName()).toUpperCase();

        lblAvatar.setText(initials);
        lblName.setText(safe(channel.getName()));
        lblGameSmall.setText(safe(channel.getGame()));
        lblDescription.setText(safe(channel.getDescription()));

        lblInfoName.setText(safe(channel.getName()));
        lblInfoGame.setText(safe(channel.getGame()));
        lblInfoType.setText(safe(channel.getType()));
        lblInfoStatus.setText(safe(channel.getStatus()));
        lblInfoActive.setText(channel.isActive() ? "Yes" : "No");
        lblInfoCreatedBy.setText(safe(channel.getCreatedBy()));
        lblInfoCreatedAt.setText(channel.getCreatedAt() == null ? "-" : channel.getCreatedAt().toString());
        lblInfoApprovedBy.setText(safe(channel.getApprovedBy()));

        lblTypeBadge.setText(cap(channel.getType()));
        lblStatusBadge.setText(cap(channel.getStatus()));

        if ("public".equalsIgnoreCase(channel.getType())) {
            lblTypeBadge.setStyle(
                    "-fx-background-color: rgba(0,183,255,0.12);" +
                            "-fx-border-color: rgba(0,183,255,0.30);" +
                            "-fx-text-fill: #6ddcff;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-weight: bold;"
            );
        } else {
            lblTypeBadge.setStyle(
                    "-fx-background-color: rgba(156,163,175,0.12);" +
                            "-fx-border-color: rgba(156,163,175,0.30);" +
                            "-fx-text-fill: #cbd5e1;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-weight: bold;"
            );
        }

        switch (safe(channel.getStatus()).toLowerCase()) {
            case "approved" -> lblStatusBadge.setStyle(
                    "-fx-background-color: rgba(34,197,94,0.12);" +
                            "-fx-border-color: rgba(34,197,94,0.30);" +
                            "-fx-text-fill: #4ade80;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-weight: bold;"
            );
            case "pending" -> lblStatusBadge.setStyle(
                    "-fx-background-color: rgba(250,204,21,0.12);" +
                            "-fx-border-color: rgba(250,204,21,0.30);" +
                            "-fx-text-fill: #fde047;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-weight: bold;"
            );
            case "rejected" -> lblStatusBadge.setStyle(
                    "-fx-background-color: rgba(255,107,107,0.12);" +
                            "-fx-border-color: rgba(255,107,107,0.30);" +
                            "-fx-text-fill: #ff8b8b;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;" +
                            "-fx-padding: 6 14 6 14;" +
                            "-fx-font-weight: bold;"
            );
        }

        boolean pending = "pending".equalsIgnoreCase(channel.getStatus());
        btnApprove.setVisible(pending);
        btnApprove.setManaged(pending);
        btnReject.setVisible(pending);
        btnReject.setManaged(pending);
    }

    @FXML
    private void handleBack() {
        goToChannelsList();
    }

    @FXML
    private void handleApprove() {
        try {
            User admin = SessionManager.getCurrentUser();
            channelService.approve(channel.getId(), admin);
            channel = channelService.findById(channel.getId());
            refreshView();
        } catch (Exception e) {
            showError("Failed to approve channel: " + e.getMessage());
        }
    }

    @FXML
    private void handleReject() {
        try {
            URL url = resolveUrl("RejectReasonDialog.fxml");
            if (url == null) {
                showError("RejectReasonDialog.fxml not found");
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
                channel = channelService.findById(channel.getId());
                refreshView();
            }

        } catch (Exception e) {
            showError("Failed to reject channel: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggle() {
        try {
            User admin = SessionManager.getCurrentUser();
            channelService.toggleActive(channel.getId(), admin);
            channel = channelService.findById(channel.getId());
            refreshView();
        } catch (Exception e) {
            showError("Failed to toggle active state: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        try {
            URL url = resolveUrl("AdminChannelForm.fxml");
            if (url == null) {
                showError("AdminChannelForm.fxml not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            AdminChannelFormController controller = loader.getController();
            controller.setModeEdit(channel, () -> {
                try {
                    this.channel = channelService.findById(channel.getId());
                    refreshView();
                } catch (Exception ignored) {
                }
            });

            Stage popup = new Stage();
            popup.setTitle("Edit Channel");
            popup.initOwner(resolveStage());
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (Exception e) {
            showError("Failed to open edit form: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
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
                goToChannelsList();
            }

        } catch (Exception e) {
            showError("Failed to delete channel: " + e.getMessage());
        }
    }

    private void goToChannelsList() {
        try {
            URL url = resolveUrl("AdminChannels.fxml");
            if (url == null) {
                showError("AdminChannels.fxml not found");
                return;
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = resolveStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (Exception e) {
            showError("Failed to go back: " + e.getMessage());
        }
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
        return (Stage) lblName.getScene().getWindow();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String cap(String s) {
        String v = safe(s);
        if ("-".equals(v)) return v;
        return v.substring(0, 1).toUpperCase() + v.substring(1).toLowerCase();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}