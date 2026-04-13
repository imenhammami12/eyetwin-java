package com.eyetwin.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblMessage;

    private boolean confirmed = false;
    private Stage stage;

    public void setData(String title, String message) {
        lblTitle.setText(title);
        lblMessage.setText(message);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        if (stage != null) {
            stage.close();
        }
    }
}