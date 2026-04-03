package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;

    // ✅ IUserService au lieu de AuthService (qui est interdit)
    private final IUserService userService = new UserServiceImpl();

    @FXML
    public void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        errorLabel.setText("");

        // Vérification confirm password (côté client uniquement)
        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        try {
            boolean success = userService.register(fullName, email, password);

            if (!success) {
                errorLabel.setText("This email is already registered.");
                return;
            }

            // Succès → login
            MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");

        } catch (IllegalArgumentException e) {
            // Validation serveur (retournée par UserServiceImpl)
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML public void goToLogin() {
        MainApp.navigateTo("/com/eyetwin/views/login.fxml", "Login");
    }

    @FXML public void goToHome() {
        MainApp.navigateTo("/com/eyetwin/views/home.fxml", "Home");
    }
}