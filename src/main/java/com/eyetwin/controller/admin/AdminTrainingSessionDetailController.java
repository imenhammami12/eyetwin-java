package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.TrainingSession;
import com.eyetwin.entities.User;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.services.TrainingSessionServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class AdminTrainingSessionDetailController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter JOIN_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy - HH:mm");

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private Label idLabel;
    @FXML private Label avatarLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label joinedAtLabel;
    @FXML private Label statusBadge;

    @FXML private Label pType;
    @FXML private Label pLevel;
    @FXML private Label pDesc;
    @FXML private Label pDate;
    @FXML private Label pTime;
    @FXML private Label pLoc;

    private final TrainingSessionServiceImpl trainingService = new TrainingSessionServiceImpl();
    private final PlanningServiceImpl planningService = new PlanningServiceImpl();

    private TrainingSession session;
    private Planning planning;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) return;
        if (adminSidebarController != null) adminSidebarController.setActivePage("planning");
        if (adminTopbarController != null) adminTopbarController.setTitle("Training Sessions");

        TrainingSession selected = SessionManager.getSelectedTrainingSession();
        Planning selectedPlanning = SessionManager.getSelectedPlanning();
        if (selected == null || selectedPlanning == null) {
            MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
            return;
        }

        new Thread(() -> {
            try {
                TrainingSession full = trainingService.getSessionById(selected.getIdTraining());
                Planning p = planningService.getPlanningById(selectedPlanning.getIdPlanning());
                Platform.runLater(() -> setData(full != null ? full : selected, p));
            } catch (Exception e) {
                Platform.runLater(() -> setData(selected, selectedPlanning));
            }
        }, "AdminTraining-DetailLoad").start();
    }

    private void setData(TrainingSession ts, Planning p) {
        this.session = ts;
        this.planning = p;

        if (idLabel != null) idLabel.setText("#" + ts.getIdTraining());

        User u = ts.getUser();
        String uname = (u != null) ? nvl(u.getUsername(), "—") : "—";
        if (avatarLabel != null) avatarLabel.setText(uname.isBlank() || "—".equals(uname) ? "U" : uname.substring(0, 1).toUpperCase());
        if (usernameLabel != null) usernameLabel.setText(uname);
        if (emailLabel != null) emailLabel.setText(u != null ? nvl(u.getEmail(), "—") : "—");
        if (joinedAtLabel != null) {
            joinedAtLabel.setText(ts.getJoinedAt() != null ? ts.getJoinedAt().format(JOIN_FMT) : "—");
        }

        if (statusBadge != null) {
            String s = nvl(ts.getStatus(), "—");
            statusBadge.setText(s);
            String low = s.toLowerCase();
            String bg = "rgba(148,163,184,0.18)";
            String fg = "#e2e8f0";
            if (low.contains("attente") || low.contains("pending")) { bg = "rgba(234,179,8,0.18)"; fg = "#fde68a"; }
            if (low.contains("active") || low.contains("confirm")) { bg = "rgba(34,197,94,0.18)"; fg = "#86efac"; }
            if (low.contains("cancel")) { bg = "rgba(239,68,68,0.18)"; fg = "#fda4af"; }
            statusBadge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-padding: 4 10; -fx-background-radius: 999; -fx-font-weight: 900; -fx-font-size: 11;");
        }

        if (p == null) return;
        if (pType != null) pType.setText(nvl(p.getType(), "—"));
        if (pLevel != null) pLevel.setText(nvl(p.getLevel(), "—"));
        if (pDesc != null) pDesc.setText(nvl(p.getDescription(), "—"));
        if (pDate != null) pDate.setText(p.getDate() != null ? p.getDate().format(DATE_FMT) : "—");
        if (pTime != null) pTime.setText(p.getTime() != null ? p.getTime().toString() : "—");
        if (pLoc != null) pLoc.setText(nvl(p.getLocalisation(), "—"));
    }

    @FXML
    public void handleEdit() {
        if (session == null) return;
        SessionManager.setSelectedTrainingSession(session);
        MainApp.navigateTo("/com/eyetwin/views/AdminTrainingSessionEdit.fxml", "Edit Session");
    }

    @FXML
    public void handleBack() {
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningSessions.fxml", "Training Sessions");
    }

    private String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}

