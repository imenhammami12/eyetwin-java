package com.eyetwin.controller.admin;

import com.eyetwin.entities.Community.Channel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminChannelDetailsDialogController {

    @FXML private Label lblName;
    @FXML private Label lblGame;
    @FXML private Label lblType;
    @FXML private Label lblStatus;
    @FXML private Label lblActive;
    @FXML private Label lblCreatedBy;
    @FXML private Label lblDescription;

    public void setChannel(Channel channel) {
        lblName.setText(safe(channel.getName()));
        lblGame.setText("Game: " + safe(channel.getGame()));
        lblType.setText("Type: " + safe(channel.getType()));
        lblStatus.setText("Status: " + safe(channel.getStatus()));
        lblActive.setText("Active: " + (channel.isActive() ? "Yes" : "No"));
        lblCreatedBy.setText("Created by: " + safe(channel.getCreatedBy()));
        lblDescription.setText(safe(channel.getDescription()));
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblName.getScene().getWindow();
        stage.close();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}