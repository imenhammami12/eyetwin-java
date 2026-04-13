package com.eyetwin.controller;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.ChannelServiceImpl;
import com.eyetwin.tools.CommunityValidator;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CommunityChannelFormController {

    @FXML private TextField tfName;
    @FXML private TextField tfGame;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblFormError;

    private final ChannelServiceImpl channelService = new ChannelServiceImpl();
    private Runnable onCreated;

    private Channel editingChannel;
    private Runnable onUpdated;
    private boolean editMode = false;

    @FXML
    public void initialize() {
        cbType.getItems().addAll(Channel.TYPE_PUBLIC, Channel.TYPE_PRIVATE);
        cbType.setValue(Channel.TYPE_PUBLIC);
    }

    public void setOnCreated(Runnable onCreated) {
        this.onCreated = onCreated;
    }

//    @FXML
//    private void handleCreate() {
//        try {
//            if (!SessionManager.canManageCommunityChannels()) {
//                lblFormError.setText("You are not allowed to create a channel.");
//                return;
//            }
//
//            User currentUser = SessionManager.getCurrentUser();
//            if (currentUser == null) {
//                lblFormError.setText("You must be logged in.");
//                return;
//            }
//
//            Channel channel = new Channel();
//            channel.setName(tfName.getText());
//            channel.setGame(tfGame.getText());
//            channel.setDescription(taDescription.getText());
//            channel.setType(cbType.getValue());
//            channel.setImageUrl(null);
//
//            String validationError = CommunityValidator.validateChannel(channel);
//            if (validationError != null) {
//                lblFormError.setText(validationError);
//                return;
//            }
//
//            if (SessionManager.isAdmin()) {
//                channelService.createByAdmin(channel, currentUser);
//            } else if (SessionManager.isPlainPlayer()) {
//                channelService.createByPlayer(channel, currentUser);
//            } else {
//                lblFormError.setText("Only admin or player can create channels.");
//                return;
//            }
//
//            if (onCreated != null) {
//                onCreated.run();
//            }
//
//            closeWindow();
//
//        } catch (Exception e) {
//            lblFormError.setText(e.getMessage());
//        }
//    }

    @FXML
    private void handleCreate() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                lblFormError.setText("You must be logged in.");
                return;
            }

            Channel channel = editMode ? editingChannel : new Channel();

            channel.setName(tfName.getText());
            channel.setGame(tfGame.getText());
            channel.setDescription(taDescription.getText());
            channel.setType(cbType.getValue());
            channel.setImageUrl(null);

            String validationError = CommunityValidator.validateChannel(channel);
            if (validationError != null) {
                lblFormError.setText(validationError);
                return;
            }

            if (editMode) {
                channelService.updateByOwner(channel, currentUser);

                if (onUpdated != null) {
                    onUpdated.run();
                }
            } else {
                if (!SessionManager.canManageCommunityChannels()) {
                    lblFormError.setText("You are not allowed to create a channel.");
                    return;
                }

                if (SessionManager.isAdmin()) {
                    channelService.createByAdmin(channel, currentUser);
                } else if (SessionManager.isPlainPlayer()) {
                    channelService.createByPlayer(channel, currentUser);
                } else {
                    lblFormError.setText("Only admin or player can create channels.");
                    return;
                }

                if (onCreated != null) {
                    onCreated.run();
                }
            }

            closeWindow();

        } catch (Exception e) {
            lblFormError.setText(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) tfName.getScene().getWindow();
        stage.close();
    }

    public void setModeEdit(Channel channel, Runnable onUpdated) {
        this.editingChannel = channel;
        this.onUpdated = onUpdated;
        this.editMode = true;

        tfName.setText(channel.getName());
        tfGame.setText(channel.getGame());
        taDescription.setText(channel.getDescription());
        cbType.setValue(channel.getType());
    }
}