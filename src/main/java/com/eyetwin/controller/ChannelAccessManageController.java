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

public class ChannelAccessManageController {

    @FXML private Label lblChannelName;
    @FXML private VBox pendingRequestsContainer;

    @FXML private ComboBox<String> cbInviteMode;
    @FXML private TextField tfMaxUses;
    @FXML private DatePicker dpExpiresAt;

    @FXML private Label lblInviteToken;
    @FXML private Label lblInviteInfo;
    @FXML private ImageView imgQrCode;

    private final ChannelAccessService accessService = new ChannelAccessService();
    private final QrCodeService qrCodeService = new QrCodeService();

    private Channel channel;
    private Runnable onChanged;

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

    private VBox buildRequestCard(ChannelJoinRequest request) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
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
        requester.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label email = new Label(request.getRequesterEmail() != null ? request.getRequesterEmail() : "");
        email.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;");

        Label date = new Label(request.getRequestedAt() != null ? request.getRequestedAt().toString() : "");
        date.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-size: 11px;");

        HBox actions = new HBox(8);
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
        btnDeny.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Deny Request");
            dialog.setHeaderText("Reason for denial");
            dialog.setContentText("Reason:");

            dialog.showAndWait().ifPresent(reason -> {
                try {
                    accessService.denyJoinRequest(request.getId(), SessionManager.getCurrentUser(), reason);
                    loadPendingRequests();
                    if (onChanged != null) onChanged.run();
                } catch (Exception ex) {
                    showError("Failed to deny request: " + ex.getMessage());
                }
            });
        });

        actions.getChildren().addAll(btnApprove, btnDeny);

        card.getChildren().addAll(requester, email, date, actions);
        return card;
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
            imgQrCode.setImage(qrCodeService.generateQrImage(invite.getToken(), 260, 260));

        } catch (Exception e) {
            showError("Failed to generate invite: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblChannelName.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}