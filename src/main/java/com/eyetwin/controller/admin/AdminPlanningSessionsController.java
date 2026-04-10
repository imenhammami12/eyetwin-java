package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.TrainingSession;
import com.eyetwin.entities.User;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.services.TrainingSessionServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPlanningSessionsController {

    private static final DateTimeFormatter JOIN_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private ImageView planningImage;
    @FXML private Label planningDesc;
    @FXML private Label planningType;
    @FXML private Label planningLevel;
    @FXML private Label planningDate;
    @FXML private Label planningLocation;
    @FXML private Label participantsTitle;

    @FXML private TableView<TrainingSession> sessionsTable;
    @FXML private TableColumn<TrainingSession, String> colParticipant;
    @FXML private TableColumn<TrainingSession, String> colEmail;
    @FXML private TableColumn<TrainingSession, String> colJoinedAt;
    @FXML private TableColumn<TrainingSession, String> colStatus;
    @FXML private TableColumn<TrainingSession, Void> colActions;

    private final PlanningServiceImpl planningService = new PlanningServiceImpl();
    private final TrainingSessionServiceImpl trainingService = new TrainingSessionServiceImpl();
    private final ObservableList<TrainingSession> rows = FXCollections.observableArrayList();

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

        setupTable();
        load(selected.getIdPlanning());
    }

    private void setupTable() {
        if (sessionsTable == null) return;

        if (colParticipant != null) {
            colParticipant.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? nvl(u.getUsername(), "—") : "—");
            });
            colParticipant.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    TrainingSession ts = getTableRow().getItem();
                    User u = ts.getUser();

                    Label avatar = new Label(u != null && u.getUsername() != null && !u.getUsername().isBlank()
                            ? u.getUsername().substring(0, 1).toUpperCase()
                            : "U");
                    avatar.setStyle("-fx-background-color: rgba(99,102,241,0.25); -fx-text-fill:white; -fx-font-weight:900; -fx-alignment:center; -fx-min-width:34; -fx-min-height:34; -fx-background-radius: 999;");

                    Label name = new Label(item);
                    name.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: 800;");

                    HBox box = new HBox(10, avatar, name);
                    box.setStyle("-fx-alignment:center-left;");
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        if (colEmail != null) {
            colEmail.setCellValueFactory(d -> {
                User u = d.getValue().getUser();
                return new SimpleStringProperty(u != null ? nvl(u.getEmail(), "—") : "—");
            });
        }

        if (colJoinedAt != null) {
            colJoinedAt.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getJoinedAt() != null ? d.getValue().getJoinedAt().format(JOIN_FMT) : "—"
            ));
        }

        if (colStatus != null) {
            colStatus.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStatus(), "—")));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Label badge = new Label(item == null ? "—" : item);
                    String s = item == null ? "" : item.toLowerCase();
                    String bg = "rgba(148,163,184,0.18)";
                    String fg = "#e2e8f0";
                    if (s.contains("attente") || s.contains("pending")) { bg = "rgba(234,179,8,0.18)"; fg = "#fde68a"; }
                    if (s.contains("active")) { bg = "rgba(34,197,94,0.18)"; fg = "#86efac"; }
                    if (s.contains("cancel")) { bg = "rgba(239,68,68,0.18)"; fg = "#fda4af"; }
                    badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-padding: 4 10; -fx-background-radius: 999; -fx-font-weight: 900; -fx-font-size: 11;");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = iconBtnSvg("eye", "icon-eye");
                private final Button editBtn = iconBtnSvg("edit", "icon-edit");
                private final Button delBtn  = iconBtnSvg("trash", "icon-trash");
                private final HBox box = new HBox(6, viewBtn, editBtn, delBtn);
                {
                    box.setStyle("-fx-alignment:center;");
                    viewBtn.setOnAction(e -> {
                        TrainingSession ts = rowItem();
                        if (ts == null) return;
                        SessionManager.setSelectedTrainingSession(ts);
                        MainApp.navigateTo("/com/eyetwin/views/AdminTrainingSessionDetail.fxml", "Training Session Details");
                    });
                    editBtn.setOnAction(e -> {
                        TrainingSession ts = rowItem();
                        if (ts == null) return;
                        SessionManager.setSelectedTrainingSession(ts);
                        MainApp.navigateTo("/com/eyetwin/views/AdminTrainingSessionEdit.fxml", "Edit Session");
                    });
                    delBtn.setOnAction(e -> {
                        TrainingSession ts = rowItem();
                        if (ts == null) return;
                        boolean ok = AdminDialogs.confirmDeleteSession();
                        if (!ok) return;
                        new Thread(() -> {
                            try {
                                trainingService.deleteSession(ts.getIdTraining());
                                Platform.runLater(() -> load(planning.getIdPlanning()));
                            } catch (Exception ignored) {}
                        }, "AdminTraining-Delete").start();
                    });
                }

                private TrainingSession rowItem() {
                    return getTableRow() != null ? getTableRow().getItem() : null;
                }

                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        sessionsTable.setItems(rows);

        if (colJoinedAt != null) colJoinedAt.setStyle("-fx-alignment: CENTER;");
        if (colStatus != null) colStatus.setStyle("-fx-alignment: CENTER;");
        if (colActions != null) colActions.setStyle("-fx-alignment: CENTER;");
    }

    private Button iconBtn(String t, String extraClass) {
        Button b = new Button(t);
        b.getStyleClass().add("icon-btn");
        if (extraClass != null && !extraClass.isBlank()) b.getStyleClass().add(extraClass);
        return b;
    }

    private Button iconBtnSvg(String kind, String extraClass) {
        Button b = new Button();
        b.getStyleClass().add("icon-btn");
        if (extraClass != null && !extraClass.isBlank()) b.getStyleClass().add(extraClass);

        SVGPath svg = new SVGPath();
        svg.getStyleClass().add("icon-svg");
        svg.setContent(svgPath(kind));
        b.setGraphic(svg);
        b.setText(null);
        b.setFocusTraversable(false);
        return b;
    }

    private String svgPath(String kind) {
        return switch (kind) {
            case "eye" -> "M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8z";
            case "edit" -> "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm14.71-9.04a1.003 1.003 0 0 0 0-1.42l-2.5-2.5a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 2-1.66z";
            case "trash" -> "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3.5-9h1v9h-1V10zm4 0h1v9h-1V10zM15.5 4l-1-1h-5l-1 1H5v2h14V4z";
            default -> "M0 0h24v24H0z";
        };
    }

    private void load(int idPlanning) {
        new Thread(() -> {
            try {
                Planning p = planningService.getPlanningById(idPlanning);
                List<TrainingSession> list = trainingService.getSessionsByPlanning(idPlanning);
                Platform.runLater(() -> {
                    setPlanning(p);
                    rows.setAll(list);
                    if (participantsTitle != null) participantsTitle.setText("Registered Participants (" + list.size() + ")");
                });
            } catch (Exception ignored) {
            }
        }, "AdminPlanningSessions-Load").start();
    }

    private void setPlanning(Planning p) {
        if (p == null) return;
        this.planning = p;

        if (planningDesc != null) planningDesc.setText(nvl(p.getDescription(), "—"));
        if (planningType != null) planningType.setText(nvl(p.getType(), "—"));
        if (planningLevel != null) planningLevel.setText(nvl(p.getLevel(), "—"));
        if (planningDate != null) {
            String d = p.getDate() != null ? p.getDate().format(DATE_FMT) : "—";
            String t = p.getTime() != null ? p.getTime().toString() : "—";
            planningDate.setText(d + " " + t);
        }
        if (planningLocation != null) planningLocation.setText(nvl(p.getLocalisation(), "—"));

        if (planningImage != null) {
            Image img = null;
            try {
                if (p.getImage() != null && !p.getImage().isBlank()) {
                    File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + p.getImage());
                    if (f.exists()) img = new Image(f.toURI().toString(), 140, 90, true, true);
                }
            } catch (Exception ignored) {}
            planningImage.setImage(img);
        }
    }

    @FXML
    public void handleBackToPlanning() {
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningDetail.fxml", "Session Details");
    }

    private String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}

