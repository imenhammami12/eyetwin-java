package com.eyetwin.controller.admin;

import com.eyetwin.tools.SessionManager;
import com.eyetwin.entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminTopbarController {

    @FXML private Label pageTitle;
    @FXML private Label usernameLabel;
    @FXML private Label userAvatarInitial;

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        String username = user.getUsername() != null ? user.getUsername() : "Admin";
        if (usernameLabel     != null) usernameLabel.setText(username);
        if (userAvatarInitial != null)
            userAvatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());
    }

    public void setTitle(String title) {
        if (pageTitle != null) pageTitle.setText(title);
    }

    // ── Navigation directe — plus besoin du sidebarController ──
    @FXML public void goToProfile() {
        navigateTo("AdminProfile.fxml");
    }

    @FXML public void goToFaceRegister() {
        navigateTo("AdminFaceRegister.fxml");
    }

    @FXML public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }

    public void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        URL url = null;
        for (String path : paths) {
            url = getClass().getResource(path);
            if (url != null) break;
        }
        if (url == null) {
            System.err.println("[AdminTopbar] ❌ FXML not found: " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage == null) {
                System.err.println("[AdminTopbar] ❌ Stage introuvable");
                return;
            }
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[AdminTopbar] ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage resolveStage() {
        // Utilise les labels pour récupérer le stage
        for (javafx.scene.Node n : new javafx.scene.Node[]{ pageTitle, usernameLabel, userAvatarInitial }) {
            if (n != null && n.getScene() != null)
                return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}