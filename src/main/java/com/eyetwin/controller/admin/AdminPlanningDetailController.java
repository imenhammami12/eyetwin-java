package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class AdminPlanningDetailController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private ImageView coverImage;
    @FXML private Label typeLabel;
    @FXML private Label levelLabel;
    @FXML private Label partnerBadge;

    @FXML private Label descValue;
    @FXML private Label dateValue;
    @FXML private Label timeValue;
    @FXML private Label locValue;

    private final PlanningServiceImpl planningService = new PlanningServiceImpl();
    private Planning planning;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) return;
        if (adminSidebarController != null) adminSidebarController.setActivePage("planning");
        if (adminTopbarController != null) adminTopbarController.setTitle("Planning");

        Planning selected = SessionManager.getSelectedPlanning();
        if (selected == null) {
            MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
            return;
        }

        new Thread(() -> {
            try {
                Planning full = planningService.getPlanningWithDetails(selected.getIdPlanning());
                Platform.runLater(() -> setPlanning(full != null ? full : selected));
            } catch (Exception e) {
                Platform.runLater(() -> setPlanning(selected));
            }
        }, "AdminPlanning-DetailLoad").start();
    }

    private void setPlanning(Planning p) {
        this.planning = p;

        if (typeLabel != null) typeLabel.setText(p.getType() != null ? p.getType() : "—");
        if (levelLabel != null) levelLabel.setText(p.getLevel() != null ? p.getLevel() : "—");

        boolean needPartner = p.isNeedPartner();
        if (partnerBadge != null) {
            partnerBadge.setVisible(needPartner);
            partnerBadge.setManaged(needPartner);
        }

        if (descValue != null) descValue.setText(p.getDescription() != null ? p.getDescription() : "—");
        if (dateValue != null) dateValue.setText(p.getDate() != null ? p.getDate().format(DATE_FMT) : "—");
        if (timeValue != null) timeValue.setText(p.getTime() != null ? p.getTime().toString() : "—");
        if (locValue != null) locValue.setText(p.getLocalisation() != null ? p.getLocalisation() : "—");

        if (coverImage != null) {
            Image img = null;
            try {
                if (p.getImage() != null && !p.getImage().isBlank()) {
                    File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + p.getImage());
                    if (f.exists()) img = new Image(f.toURI().toString(), 96, 96, true, true);
                }
            } catch (Exception ignored) {}
            coverImage.setImage(img);
        }
    }

    @FXML
    public void handleBackToList() {
        SessionManager.clearSelectedPlanning();
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
    }

    @FXML
    public void handleEdit() {
        if (planning == null) return;
        SessionManager.setSelectedPlanning(planning);
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningForm.fxml", "Edit Planning");
    }

    @FXML
    public void handleTrainingSessions() {
        if (planning == null) return;
        SessionManager.setSelectedPlanning(planning);
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningSessions.fxml", "Training Sessions");
    }

    @FXML
    public void handleDelete() {
        if (planning == null) return;
        boolean ok = AdminDialogs.confirmDeleteSession();
        if (!ok) return;

        new Thread(() -> {
            try {
                planningService.deletePlanning(planning.getIdPlanning());
                SessionManager.setPendingFlash("success", "Planning deleted successfully.");
                Platform.runLater(this::handleBackToList);
            } catch (Exception ex) {
                // keep simple; you already show errors elsewhere
            }
        }, "AdminPlanning-Delete").start();
    }
}

