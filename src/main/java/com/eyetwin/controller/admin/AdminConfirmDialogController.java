package com.eyetwin.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminConfirmDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblMessage;
    @FXML private Button btnConfirm;

    private Stage stage;
    private boolean confirmed = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(String title, String message, String confirmText) {
        lblTitle.setText(title);
        lblMessage.setText(message);
        btnConfirm.setText(confirmText);
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        if (stage != null) stage.close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        if (stage != null) stage.close();
    }
}
