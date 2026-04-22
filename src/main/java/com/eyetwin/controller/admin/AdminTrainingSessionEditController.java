package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.TrainingSession;
import com.eyetwin.entities.User;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.services.TrainingSessionServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class AdminTrainingSessionEditController {

    private static final DateTimeFormatter REG_DATE = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter REG_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private Label avatarLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label regDateLabel;
    @FXML private Label regTimeLabel;

    @FXML private Label planningDesc;
    @FXML private Label planningType;
    @FXML private Label planningLevel;
    @FXML private Label planningMeta;
    @FXML private Label planningLocation;

    @FXML private ComboBox<String> statusCombo;
    @FXML private Label errorLabel;

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

        if (statusCombo != null) {
            statusCombo.setItems(FXCollections.observableArrayList("Pending", "Confirmed", "Cancelled"));
        }

        new Thread(() -> {
            try {
                TrainingSession full = trainingService.getSessionById(selected.getIdTraining());
                Planning p = planningService.getPlanningById(selectedPlanning.getIdPlanning());
                Platform.runLater(() -> setData(full != null ? full : selected, p));
            } catch (Exception e) {
                Platform.runLater(() -> setData(selected, selectedPlanning));
            }
        }, "AdminTraining-EditLoad").start();
    }

    private void setData(TrainingSession ts, Planning p) {
        this.session = ts;
        this.planning = p;

        User u = ts.getUser();
        String uname = (u != null) ? nvl(u.getUsername(), "—") : "—";
        if (avatarLabel != null) avatarLabel.setText(uname.isBlank() || "—".equals(uname) ? "U" : uname.substring(0, 1).toUpperCase());
        if (usernameLabel != null) usernameLabel.setText(uname);
        if (emailLabel != null) emailLabel.setText(u != null ? nvl(u.getEmail(), "—") : "—");

        if (regDateLabel != null) regDateLabel.setText(ts.getJoinedAt() != null ? ts.getJoinedAt().format(REG_DATE) : "—");
        if (regTimeLabel != null) regTimeLabel.setText(ts.getJoinedAt() != null ? ts.getJoinedAt().format(REG_TIME) : "—");

        if (p != null) {
            if (planningDesc != null) planningDesc.setText(nvl(p.getDescription(), "—"));
            if (planningType != null) planningType.setText(nvl(p.getType(), "—"));
            if (planningLevel != null) planningLevel.setText(nvl(p.getLevel(), "—"));
            if (planningMeta != null) {
                String meta = "";
                if (p.getDate() != null) meta += p.getDate().toString();
                if (p.getTime() != null) meta += "  " + p.getTime().toString();
                planningMeta.setText(meta.isBlank() ? "—" : meta);
            }
            if (planningLocation != null) planningLocation.setText(nvl(p.getLocalisation(), "—"));
        }

        if (statusCombo != null) {
            statusCombo.setValue(toUiStatus(nvl(ts.getStatus(), "Pending")));
        }
        if (errorLabel != null) errorLabel.setText("");
    }

    private String toUiStatus(String db) {
        String s = db == null ? "" : db.trim().toLowerCase();
        if (s.contains("cancel")) return "Cancelled";
        if (s.contains("active") || s.contains("confirm")) return "Confirmed";
        return "Pending";
    }

    private String toDbStatus(String ui) {
        if (ui == null) return "en attente";
        return switch (ui) {
            case "Confirmed" -> "ACTIVE";
            case "Cancelled" -> "CANCELLED";
            default -> "en attente";
        };
    }

    @FXML
    public void handleUpdate() {
        if (session == null || statusCombo == null) return;
        String dbStatus = toDbStatus(statusCombo.getValue());

        new Thread(() -> {
            try {
                trainingService.updateSessionStatus(session.getIdTraining(), dbStatus);
                session.setStatus(dbStatus);
                SessionManager.setSelectedTrainingSession(session);
                Platform.runLater(() -> MainApp.navigateTo("/com/eyetwin/views/AdminTrainingSessionDetail.fxml", "Training Session Details"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (errorLabel != null) errorLabel.setText(e.getMessage());
                });
            }
        }, "AdminTraining-UpdateStatus").start();
    }

    @FXML
    public void handleCancel() {
        MainApp.navigateTo("/com/eyetwin/views/AdminTrainingSessionDetail.fxml", "Training Session Details");
    }

    private String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}

