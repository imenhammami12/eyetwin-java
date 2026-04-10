package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AdminDialogs {

    private AdminDialogs() {}

    public static boolean confirmDeleteSession() {
        AtomicBoolean confirmed = new AtomicBoolean(false);

        Stage owner = MainApp.getPrimaryStage();
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Confirmation");

        // Root overlay
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setPadding(new Insets(20));

        VBox card = new VBox(0);
        card.getStyleClass().add("confirm-card");
        card.setMaxWidth(760);
        card.setPrefWidth(760);

        // Header
        HBox header = new HBox(12);
        header.getStyleClass().add("confirm-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Confirmation");
        title.getStyleClass().add("confirm-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("✕");
        close.getStyleClass().add("confirm-close");
        close.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, spacer, close);

        // Body
        VBox body = new VBox(14);
        body.getStyleClass().add("confirm-body");
        body.setAlignment(Pos.CENTER);

        Label icon = new Label("🗑");
        icon.getStyleClass().add("confirm-icon");

        Label msg = new Label("Are you sure you want to delete this session?");
        msg.getStyleClass().add("confirm-message");

        Label warn = new Label(
                "Warning: This action is permanent and cannot be undone. Once deleted, this planning session and all associated data will be irretrievably lost."
        );
        warn.setWrapText(true);
        warn.getStyleClass().add("confirm-warning");
        warn.setMaxWidth(560);

        body.getChildren().addAll(icon, msg, warn);

        // Footer buttons
        HBox actions = new HBox(14);
        actions.getStyleClass().add("confirm-actions");
        actions.setAlignment(Pos.CENTER);

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("outline-btn", "confirm-cancel");
        cancel.setPrefWidth(170);
        cancel.setOnAction(e -> dialog.close());

        Button del = new Button("Delete Permanently");
        del.getStyleClass().addAll("danger-btn", "confirm-delete");
        del.setPrefWidth(240);
        del.setOnAction(e -> {
            confirmed.set(true);
            dialog.close();
        });

        actions.getChildren().addAll(cancel, del);

        VBox content = new VBox(0, header, body, actions);
        content.getStyleClass().add("confirm-content");

        card.getChildren().add(content);
        overlay.getChildren().add(card);

        Scene scene = new Scene(overlay);
        scene.setFill(null);
        scene.getStylesheets().add(AdminDialogs.class.getResource("/com/eyetwin/styles/admin-planning.css").toExternalForm());
        dialog.setScene(scene);

        dialog.showAndWait();
        return confirmed.get();
    }
}

