package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }

        if (email.equals("admin@example.com")) {
            navigateTo(event, "/org/example/tournoi-view.fxml", "Tournaments - EyeTwin Platform");
        } else if (email.equals("client@example.com")) {
            navigateTo(event, "/org/example/client-space.fxml", "Espace Client - EyeTwin Platform");
        } else {
            showError("Invalid email or user not found.");
        }
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1050, 700));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading page.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
