package com.eyetwin.controller;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.ChannelServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CommunityController {

    @FXML private BorderPane rootContainer;
    @FXML private Label lblPageTitle;
    @FXML private Button btnNewChannel;

    @FXML private Label lblVisibleChannelsCount;
    @FXML private Label lblPublicChannelsCount;
    @FXML private Label lblPrivateChannelsCount;
    @FXML private Label lblMyPendingChannelsCount;

    @FXML private FlowPane approvedChannelsContainer;
    @FXML private FlowPane pendingChannelsContainer;
    @FXML private FlowPane rejectedChannelsContainer;
    @FXML private VBox pendingSectionBox;
    @FXML private VBox rejectedSectionBox;

    private final ChannelServiceImpl channelService = new ChannelServiceImpl();

    @FXML
    public void initialize() {
        btnNewChannel.setVisible(SessionManager.canManageCommunityChannels());
        btnNewChannel.setManaged(SessionManager.canManageCommunityChannels());
        loadChannels();
    }

//    private void loadChannels() {
//        channelsContainer.getChildren().clear();
//
//        try {
//            User currentUser = SessionManager.getCurrentUser();
//            List<Channel> channels = channelService.findVisibleChannels(currentUser);
//
//            if (channels.isEmpty()) {
//                VBox emptyBox = new VBox(12);
//                emptyBox.setPadding(new Insets(28));
//                emptyBox.setPrefWidth(420);
//                emptyBox.setStyle(
//                        "-fx-background-color: rgba(6,5,16,0.88);" +
//                                "-fx-border-color: rgba(255,255,255,0.065);" +
//                                "-fx-border-radius:14;" +
//                                "-fx-background-radius:14;"
//                );
//
//                Label emptyTitle = new Label("No channels yet");
//                emptyTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
//
//                Label emptyText = new Label("Create your first channel or wait for approved channels to appear.");
//                emptyText.setWrapText(true);
//                emptyText.setStyle("-fx-text-fill: rgba(255,255,255,0.38); -fx-font-size: 13px;");
//
//                emptyBox.getChildren().addAll(emptyTitle, emptyText);
//                channelsContainer.getChildren().add(emptyBox);
//                return;
//            }
//
//            for (Channel channel : channels) {
//                channelsContainer.getChildren().add(buildChannelCard(channel));
//            }
//
//        } catch (SQLException e) {
//            showError("Failed to load channels: " + e.getMessage());
//        }
//    }

//    private void loadChannels() {
//        approvedChannelsContainer.getChildren().clear();
//        pendingChannelsContainer.getChildren().clear();
//
//        try {
//            User currentUser = SessionManager.getCurrentUser();
//
//            List<Channel> approvedChannels = channelService.findApprovedCommunityChannels(currentUser);
//            List<Channel> myPendingChannels = channelService.findOwnPendingChannels(currentUser);
//
//            // Stats
//            setLabel(lblVisibleChannelsCount, String.valueOf(approvedChannels.size()));
//            setLabel(lblPublicChannelsCount, String.valueOf(countByType(approvedChannels, Channel.TYPE_PUBLIC)));
//            setLabel(lblPrivateChannelsCount, String.valueOf(countByType(approvedChannels, Channel.TYPE_PRIVATE)));
//            setLabel(lblMyPendingChannelsCount, String.valueOf(myPendingChannels.size()));
//
//            // Approved section
//            if (approvedChannels.isEmpty()) {
//                approvedChannelsContainer.getChildren().add(buildEmptyStateCard(
//                        "No approved channels yet",
//                        "Approved channels will appear here once they become available."
//                ));
//            } else {
//                for (Channel channel : approvedChannels) {
//                    approvedChannelsContainer.getChildren().add(buildChannelCard(channel, false));
//                }
//            }
//
//            // Pending section visible only for owner
//            boolean hasPending = !myPendingChannels.isEmpty();
//            pendingSectionBox.setVisible(hasPending);
//            pendingSectionBox.setManaged(hasPending);
//
//            if (hasPending) {
//                for (Channel channel : myPendingChannels) {
//                    pendingChannelsContainer.getChildren().add(buildChannelCard(channel, true));
//                }
//            }
//
//        } catch (SQLException e) {
//            showError("Failed to load channels: " + e.getMessage());
//        }
//    }

    private void loadChannels() {
        approvedChannelsContainer.getChildren().clear();
        pendingChannelsContainer.getChildren().clear();
        rejectedChannelsContainer.getChildren().clear();

        try {
            User currentUser = SessionManager.getCurrentUser();

            List<Channel> approvedChannels = channelService.findApprovedCommunityChannels(currentUser);
            List<Channel> myPendingChannels = channelService.findOwnPendingChannels(currentUser);
            List<Channel> myRejectedChannels = channelService.findOwnRejectedChannels(currentUser);

            // Stats
            setLabel(lblVisibleChannelsCount, String.valueOf(approvedChannels.size()));
            setLabel(lblPublicChannelsCount, String.valueOf(countByType(approvedChannels, Channel.TYPE_PUBLIC)));
            setLabel(lblPrivateChannelsCount, String.valueOf(countByType(approvedChannels, Channel.TYPE_PRIVATE)));
            setLabel(lblMyPendingChannelsCount, String.valueOf(myPendingChannels.size() + myRejectedChannels.size()));

            // Approved section
            if (approvedChannels.isEmpty()) {
                approvedChannelsContainer.getChildren().add(buildEmptyStateCard(
                        "No approved channels yet",
                        "Approved channels will appear here once they become available."
                ));
            } else {
                for (Channel channel : approvedChannels) {
                    approvedChannelsContainer.getChildren().add(buildChannelCard(channel, "approved"));
                }
            }

            // Pending section
            boolean hasPending = !myPendingChannels.isEmpty();
            pendingSectionBox.setVisible(hasPending);
            pendingSectionBox.setManaged(hasPending);

            if (hasPending) {
                for (Channel channel : myPendingChannels) {
                    pendingChannelsContainer.getChildren().add(buildChannelCard(channel, "pending"));
                }
            }

            // Rejected section
            boolean hasRejected = !myRejectedChannels.isEmpty();
            rejectedSectionBox.setVisible(hasRejected);
            rejectedSectionBox.setManaged(hasRejected);

            if (hasRejected) {
                for (Channel channel : myRejectedChannels) {
                    rejectedChannelsContainer.getChildren().add(buildChannelCard(channel, "rejected"));
                }
            }

        } catch (SQLException e) {
            showError("Failed to load channels: " + e.getMessage());
        }
    }

    private VBox buildChannelCard(Channel channel, String sectionType) {
        VBox card = new VBox(10);
        card.setPrefWidth(250);
        card.setMinHeight(210);
        card.setPadding(new Insets(18));

        String cardStyle;
        switch (sectionType.toLowerCase()) {
            case "pending" -> cardStyle =
                    "-fx-background-color: linear-gradient(to bottom, rgba(28,20,10,0.96), rgba(15,10,18,0.96));" +
                            "-fx-border-color: rgba(246,216,96,0.28);" +
                            "-fx-border-radius:16;" +
                            "-fx-background-radius:16;";
            case "rejected" -> cardStyle =
                    "-fx-background-color: linear-gradient(to bottom, rgba(30,10,10,0.96), rgba(18,8,12,0.96));" +
                            "-fx-border-color: rgba(232,55,42,0.32);" +
                            "-fx-border-radius:16;" +
                            "-fx-background-radius:16;";
            default -> cardStyle =
                    "-fx-background-color: linear-gradient(to bottom, rgba(18,10,18,0.96), rgba(8,8,16,0.96));" +
                            "-fx-border-color: rgba(255,255,255,0.09);" +
                            "-fx-border-radius:16;" +
                            "-fx-background-radius:16;";
        }

        card.setStyle(cardStyle);

        Label name = new Label(channel.getName());
        name.setWrapText(true);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label game = new Label(channel.getGame() == null ? "" : channel.getGame().toUpperCase());
        game.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 11px;");

        Label type = new Label(channel.getType().toUpperCase());
        type.setStyle(
                "-fx-text-fill: #ff6b2b;" +
                        "-fx-border-color: rgba(255,107,43,0.55);" +
                        "-fx-border-radius: 18;" +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 4 12 4 12;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;"
        );

        Label status = new Label(channel.getStatus().toUpperCase());
        String statusStyle = switch (channel.getStatus().toLowerCase()) {
            case "approved" ->
                    "-fx-text-fill:#00e676; -fx-border-color:rgba(0,230,118,0.55); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:4 12 4 12; -fx-font-size:10px; -fx-font-weight:bold;";
            case "pending" ->
                    "-fx-text-fill:#f6d860; -fx-border-color:rgba(246,216,96,0.55); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:4 12 4 12; -fx-font-size:10px; -fx-font-weight:bold;";
            case "rejected" ->
                    "-fx-text-fill:#ff4d3d; -fx-border-color:rgba(232,55,42,0.55); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:4 12 4 12; -fx-font-size:10px; -fx-font-weight:bold;";
            default ->
                    "-fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.35); -fx-border-radius:18; -fx-background-radius:18; -fx-padding:4 12 4 12; -fx-font-size:10px; -fx-font-weight:bold;";
        };
        status.setStyle(statusStyle);

        HBox badges = new HBox(8, type, status);

        String descText = (channel.getDescription() == null || channel.getDescription().trim().isEmpty())
                ? "No description."
                : channel.getDescription();

        Label description = new Label(descText);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 11px;");

        Pane spacer = new Pane();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.10);");

        HBox actions = new HBox(8);

        Button openButton = new Button("Open");
        openButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;"
        );

        User currentUser = SessionManager.getCurrentUser();

        boolean canOpen = currentUser != null
                && Channel.STATUS_APPROVED.equalsIgnoreCase(channel.getStatus())
                && channel.isActive();

        openButton.setDisable(!canOpen);
        openButton.setOpacity(canOpen ? 1.0 : 0.45);

        if (canOpen) {
            openButton.setOnAction(e -> openChannel(channel));
        }

        actions.getChildren().add(openButton);

        boolean isOwner = currentUser != null
                && channel.getCreatedBy() != null
                && channel.getCreatedBy().equalsIgnoreCase(currentUser.getEmail());

        if (isOwner) {
            Button editButton = new Button("Edit");
            editButton.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-border-color: rgba(255,255,255,0.10);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 9;" +
                            "-fx-border-radius: 9;" +
                            "-fx-padding: 8 16 8 16;" +
                            "-fx-cursor: hand;"
            );
            editButton.setOnAction(e -> handleEditOwnChannel(channel));

            Button deleteButton = new Button(
                    Channel.STATUS_PENDING.equalsIgnoreCase(channel.getStatus()) ? "Undo" : "Delete"
            );
            deleteButton.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-border-color: rgba(255,255,255,0.10);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 9;" +
                            "-fx-border-radius: 9;" +
                            "-fx-padding: 8 16 8 16;" +
                            "-fx-cursor: hand;"
            );
            deleteButton.setOnAction(e -> handleDeleteOwnChannel(channel));

            actions.getChildren().add(editButton);
            actions.getChildren().add(deleteButton);
        }

        if (channel.getRejectionReason() != null && !channel.getRejectionReason().isBlank()) {
            Label rejection = new Label("Reason: " + channel.getRejectionReason());
            rejection.setWrapText(true);
            rejection.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11px;");
            card.getChildren().addAll(name, game, badges, description, rejection, spacer, separator, actions);
        } else {
            card.getChildren().addAll(name, game, badges, description, spacer, separator, actions);        }

        return card;
    }

    private void handleEditOwnChannel(Channel channel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/CommunityChannelForm.fxml"));
            Parent root = loader.load();

            CommunityChannelFormController controller = loader.getController();
            controller.setModeEdit(channel, this::loadChannels);

            Stage popup = new Stage();
            popup.setTitle("Edit Channel");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner((Stage) rootContainer.getScene().getWindow());            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (IOException e) {
            showError("Failed to open edit channel popup: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowCreateForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/CommunityChannelForm.fxml"));
            Parent root = loader.load();

            CommunityChannelFormController controller = loader.getController();
            controller.setOnCreated(this::loadChannels);

            Stage popup = new Stage();
            popup.setTitle("Create Channel");
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner((Stage) btnNewChannel.getScene().getWindow());
            popup.setScene(new Scene(root));
            popup.setResizable(false);
            popup.showAndWait();

        } catch (IOException e) {
            showError("Failed to open create channel popup: " + e.getMessage());
        }
    }

    private void handleDeleteOwnChannel(Channel channel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/ConfirmDialog.fxml"));
            Parent root = loader.load();

            ConfirmDialogController controller = loader.getController();

            String title = Channel.STATUS_PENDING.equalsIgnoreCase(channel.getStatus())
                    ? "Undo Channel"
                    : "Delete Channel";

            String message = Channel.STATUS_PENDING.equalsIgnoreCase(channel.getStatus())
                    ? "Do you want to delete this pending channel request?"
                    : "Do you want to delete this channel?";

            controller.setData(title, message);

            Stage popup = new Stage();
            popup.setTitle(title);
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner((Stage) rootContainer.getScene().getWindow());            popup.setScene(new Scene(root));
            popup.setResizable(false);

            controller.setStage(popup);
            popup.showAndWait();

            if (controller.isConfirmed()) {
                User currentUser = SessionManager.getCurrentUser();
                channelService.deleteByOwner(channel.getId(), currentUser);
                loadChannels();
            }

        } catch (Exception e) {
            showError("Failed to delete channel: " + e.getMessage());
        }
    }

    private void openChannel(Channel channel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eyetwin/views/CommunityChat.fxml"));
            Parent root = loader.load();

            CommunityChatController controller = loader.getController();
            controller.setChannel(channel);

            Stage stage = (Stage) rootContainer.getScene().getWindow();            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showError("Failed to open channel: " + e.getMessage());
        }
    }

    private VBox buildEmptyStateCard(String titleText, String bodyText) {
        VBox emptyBox = new VBox(12);
        emptyBox.setPadding(new Insets(24));
        emptyBox.setPrefWidth(420);
        emptyBox.setStyle(
                "-fx-background-color: rgba(10,10,18,0.92);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius:16;" +
                        "-fx-background-radius:16;"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label text = new Label(bodyText);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 13px;");

        emptyBox.getChildren().addAll(title, text);
        return emptyBox;
    }

    private int countByType(List<Channel> channels, String type) {
        int count = 0;
        for (Channel channel : channels) {
            if (channel.getType() != null && channel.getType().equalsIgnoreCase(type)) {
                count++;
            }
        }
        return count;
    }

    private void setLabel(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}