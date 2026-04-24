package com.eyetwin.controller;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.Community.ChannelInvite;
import com.eyetwin.entities.Community.ChannelJoinRequest;
import com.eyetwin.services.Community.ChannelAccessService;
import com.eyetwin.services.Community.QrCodeService;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;

import javafx.scene.Node;

public class ChannelAccessManageController {

    @FXML private Label lblChannelName;
    @FXML private VBox pendingRequestsContainer;

    @FXML private ComboBox<String> cbInviteMode;
    @FXML private TextField tfMaxUses;
    @FXML private DatePicker dpExpiresAt;

    @FXML private TextArea lblInviteToken;
    @FXML private Label lblInviteInfo;
    @FXML private ImageView imgQrCode;

    private final ChannelAccessService accessService = new ChannelAccessService();
    private final QrCodeService qrCodeService = new QrCodeService();

    private Channel channel;
    private Runnable onChanged;

    private Image lastGeneratedQrImage;

    @FXML
    public void initialize() {
        cbInviteMode.getItems().addAll("Auto Join", "Requires Approval");
        cbInviteMode.setValue("Requires Approval");
    }

    public void setChannel(Channel channel, Runnable onChanged) {
        this.channel = channel;
        this.onChanged = onChanged;

        lblChannelName.setText(channel.getName());
        loadPendingRequests();
    }

    private void loadPendingRequests() {
        pendingRequestsContainer.getChildren().clear();

        try {
            List<ChannelJoinRequest> requests = accessService.findPendingRequestsForOwner(
                    channel.getId(),
                    SessionManager.getCurrentUser()
            );

            if (requests.isEmpty()) {
                Label empty = new Label("No pending access requests.");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");
                pendingRequestsContainer.getChildren().add(empty);
                return;
            }

            for (ChannelJoinRequest request : requests) {
                pendingRequestsContainer.getChildren().add(buildRequestCard(request));
            }

        } catch (Exception e) {
            showError("Failed to load pending requests: " + e.getMessage());
        }
    }

//    private VBox buildRequestCard(ChannelJoinRequest request) {
//        VBox card = new VBox(8);
//        card.setPadding(new Insets(12));
//        card.setStyle(
//                "-fx-background-color: rgba(255,255,255,0.03);" +
//                        "-fx-border-color: rgba(255,255,255,0.08);" +
//                        "-fx-border-radius: 12;" +
//                        "-fx-background-radius: 12;"
//        );
//
//        String displayName = request.getRequesterUsername() != null && !request.getRequesterUsername().isBlank()
//                ? request.getRequesterUsername()
//                : (request.getRequesterEmail() != null ? request.getRequesterEmail() : "Unknown user");
//
//        Label requester = new Label(displayName);
//        requester.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
//
//        Label email = new Label(request.getRequesterEmail() != null ? request.getRequesterEmail() : "");
//        email.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");
//
//        Label date = new Label(request.getRequestedAt() != null ? request.getRequestedAt().toString() : "");
//        date.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-size: 11px;");
//
//        HBox actions = new HBox(8);
//        actions.setAlignment(Pos.CENTER_LEFT);
//
//        Button btnApprove = new Button("Approve");
//        btnApprove.setStyle(
//                "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
//                        "-fx-text-fill: white;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-background-radius: 8;" +
//                        "-fx-cursor: hand;"
//        );
//        btnApprove.setOnAction(e -> {
//            try {
//                accessService.approveJoinRequest(request.getId(), SessionManager.getCurrentUser());
//                loadPendingRequests();
//                if (onChanged != null) onChanged.run();
//            } catch (Exception ex) {
//                showError("Failed to approve request: " + ex.getMessage());
//            }
//        });
//
//        Button btnDeny = new Button("Deny");
//        btnDeny.setStyle(
//                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
//                        "-fx-text-fill: white;" +
//                        "-fx-font-weight: bold;" +
//                        "-fx-background-radius: 8;" +
//                        "-fx-cursor: hand;"
//        );
//        btnDeny.setOnAction(e -> {
//            TextInputDialog dialog = new TextInputDialog();
//            dialog.setTitle("Deny Request");
//            dialog.setHeaderText("Reason for denial");
//            dialog.setContentText("Reason:");
//
//            dialog.showAndWait().ifPresent(reason -> {
//                try {
//                    accessService.denyJoinRequest(request.getId(), SessionManager.getCurrentUser(), reason);
//                    loadPendingRequests();
//                    if (onChanged != null) onChanged.run();
//                } catch (Exception ex) {
//                    showError("Failed to deny request: " + ex.getMessage());
//                }
//            });
//        });
//
//        actions.getChildren().addAll(btnApprove, btnDeny);
//
//        card.getChildren().addAll(requester, email, date, actions);
//        return card;
//    }

    private VBox buildRequestCard(ChannelJoinRequest request) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setMinHeight(120);
        card.setPrefWidth(800);
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.NEVER);

        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        String displayName = request.getRequesterUsername() != null && !request.getRequesterUsername().isBlank()
                ? request.getRequesterUsername()
                : (request.getRequesterEmail() != null ? request.getRequesterEmail() : "Unknown user");

        Label requester = new Label(displayName);
        requester.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label email = new Label(request.getRequesterEmail() != null ? request.getRequesterEmail() : "");
        email.setStyle("-fx-text-fill: rgba(255,255,255,0.62); -fx-font-size: 13px;");

        Label date = new Label(request.getRequestedAt() != null ? request.getRequestedAt().toString() : "");
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.42); -fx-font-size: 12px;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnApprove = new Button("Approve");
        btnApprove.setStyle(
                "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        btnApprove.setOnAction(e -> {
            try {
                accessService.approveJoinRequest(request.getId(), SessionManager.getCurrentUser());
                loadPendingRequests();
                if (onChanged != null) onChanged.run();
            } catch (Exception ex) {
                showError("Failed to approve request: " + ex.getMessage());
            }
        });

        Button btnDeny = new Button("Deny");
        btnDeny.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        btnDeny.setOnAction(e -> openDenyReasonDialog(request));

        actions.getChildren().addAll(btnApprove, btnDeny);

        card.getChildren().addAll(requester, email, date, actions);
        return card;
    }

    private void openDenyReasonDialog(ChannelJoinRequest request) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Deny Request");
        dialog.setHeaderText(null);

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        Label title = new Label("Reason for denial");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label reasonLabel = new Label("Reason:");
        reasonLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");

        TextField input = new TextField();
        input.setPromptText("Enter a reason");
        input.setStyle(
                "-fx-background-color: #14192b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.35);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: rgba(255,255,255,0.08);"
        );

        VBox box = new VBox(12, title, reasonLabel, input);
        box.setPadding(new Insets(10, 0, 0, 0));

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(box);
        pane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #070b1f, #050816);" +
                        "-fx-border-color: rgba(232,55,42,0.22);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        Node confirmButton = pane.lookupButton(confirmButtonType);
        confirmButton.setDisable(true);
        confirmButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;"
        );

        Node cancelButton = pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.08);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;"
            );
        }

        input.textProperty().addListener((obs, oldVal, newVal) ->
                confirmButton.setDisable(newVal == null || newVal.trim().isEmpty())
        );

        dialog.setResultConverter(button -> button == confirmButtonType ? input.getText().trim() : null);

        dialog.showAndWait().ifPresent(reason -> {
            try {
                accessService.denyJoinRequest(request.getId(), SessionManager.getCurrentUser(), reason);
                loadPendingRequests();
                if (onChanged != null) onChanged.run();
            } catch (Exception ex) {
                showError("Failed to deny request: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void handleGenerateInvite() {
        try {
            String mode = "Auto Join".equalsIgnoreCase(cbInviteMode.getValue())
                    ? ChannelInvite.MODE_AUTO_JOIN
                    : ChannelInvite.MODE_REQUIRES_APPROVAL;

            Integer maxUses = null;
            if (tfMaxUses.getText() != null && !tfMaxUses.getText().trim().isEmpty()) {
                maxUses = Integer.parseInt(tfMaxUses.getText().trim());
                if (maxUses <= 0) {
                    throw new IllegalArgumentException("Max uses must be greater than 0.");
                }
            }

            Timestamp expiresAt = null;
            LocalDate selectedDate = dpExpiresAt.getValue();
            if (selectedDate != null) {
                expiresAt = Timestamp.valueOf(LocalDateTime.of(selectedDate, LocalTime.of(23, 59, 59)));
            }

            ChannelInvite invite = accessService.createInvite(
                    channel,
                    SessionManager.getCurrentUser(),
                    mode,
                    expiresAt,
                    maxUses
            );

            lblInviteToken.setText(invite.getToken());
            lblInviteInfo.setText(
                    "Mode: " + invite.getMode()
                            + (invite.getMaxUses() != null ? " | Max uses: " + invite.getMaxUses() : " | Unlimited uses")
            );

            lastGeneratedQrImage = qrCodeService.generateQrImage(invite.getToken(), 260, 260);
            imgQrCode.setImage(lastGeneratedQrImage);

        } catch (Exception e) {
            showError("Failed to generate invite: " + e.getMessage());
        }
    }

    @FXML
    private void handleCopyToken() {
        String token = lblInviteToken.getText();
        if (token == null || token.isBlank()) {
            showError("Generate an invite first.");
            return;
        }

        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(token);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        lblInviteInfo.setText("Invite token copied to clipboard.");
    }

    @FXML
    private void handleDownloadQr() {
        try {
            if (lastGeneratedQrImage == null) {
                showError("Generate an invite first.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save QR Code");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG Image", "*.png")
            );
            chooser.setInitialFileName("channel-invite-" + channel.getName().replaceAll("[^a-zA-Z0-9-_]", "_") + ".png");

            File file = chooser.showSaveDialog(lblChannelName.getScene().getWindow());
            if (file == null) return;

            ImageIO.write(SwingFXUtils.fromFXImage(lastGeneratedQrImage, null), "png", file);
            lblInviteInfo.setText("QR code saved: " + file.getAbsolutePath());

        } catch (Exception e) {
            showError("Failed to save QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblChannelName.getScene().getWindow();
        stage.close();
    }

//    private void showError(String message) {
//        Alert alert = new Alert(Alert.AlertType.ERROR);
//        alert.setTitle("Error");
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane pane = alert.getDialogPane();
        pane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #070b1f, #050816);" +
                        "-fx-border-color: rgba(232,55,42,0.22);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        pane.lookup(".content.label").setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        Node okButton = pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ff416c, #ff5a36);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;"
            );
        }

        alert.showAndWait();
    }
}