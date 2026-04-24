package com.eyetwin.controller;

import com.eyetwin.services.Community.ChannelAccessService;
import com.eyetwin.services.Community.QrCodeService;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class JoinWithQrController {

    @FXML private TextField tfInviteToken;
    @FXML private Label lblJoinResult;

    private final ChannelAccessService accessService = new ChannelAccessService();
    private final QrCodeService qrCodeService = new QrCodeService();

    private Runnable onChanged;

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @FXML
    private void handleLoadQrImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose QR image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = chooser.showOpenDialog(tfInviteToken.getScene().getWindow());
        if (file == null) return;

        try {
            String token = qrCodeService.decodeQrFromFile(file);
            tfInviteToken.setText(token);
            lblJoinResult.setText("QR decoded successfully.");
        } catch (Exception e) {
            lblJoinResult.setText("Could not decode QR: " + e.getMessage());
        }
    }

    @FXML
    private void handleJoinWithInvite() {
        try {
            String token = tfInviteToken.getText() == null ? "" : tfInviteToken.getText().trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Invite token is required.");
            }

            String result = accessService.joinWithInvite(token, SessionManager.getCurrentUser());

            switch (result) {
                case "AUTO_JOINED" -> lblJoinResult.setText("You joined the private channel successfully.");
                case "REQUEST_CREATED" -> lblJoinResult.setText("Join request sent to the channel owner.");
                case "ALREADY_MEMBER" -> lblJoinResult.setText("You already have access to this channel.");
                case "ALREADY_PENDING" -> lblJoinResult.setText("You already have a pending request for this channel.");
                default -> lblJoinResult.setText("Done.");
            }

            if (onChanged != null) {
                onChanged.run();
            }

        } catch (Exception e) {
            lblJoinResult.setText("Failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tfInviteToken.getScene().getWindow();
        stage.close();
    }
}