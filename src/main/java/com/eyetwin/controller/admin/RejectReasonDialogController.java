package com.eyetwin.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class RejectReasonDialogController {

    @FXML private TextArea taReason;
    @FXML private Label lblError;

    private Stage stage;
    private boolean confirmed = false;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getReason() {
        return taReason.getText();
    }

    @FXML
    private void handleConfirm() {
        String reason = taReason.getText() == null ? "" : taReason.getText().trim();

        if (reason.isEmpty()) {
            lblError.setText("Reason is required.");
            return;
        }

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
