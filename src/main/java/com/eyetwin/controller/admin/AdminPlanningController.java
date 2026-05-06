package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.PlanningLevel;
import com.eyetwin.entities.PlanningType;
import com.eyetwin.interfaces.IPlanningService;
import com.eyetwin.services.PlanningServiceImpl;
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
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class AdminPlanningController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> levelCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private Label foundLabel;

    @FXML private TableView<Planning> planningTable;
    @FXML private TableColumn<Planning, Void>   colImage;
    @FXML private TableColumn<Planning, String> colActivity;
    @FXML private TableColumn<Planning, String> colDescription;
    @FXML private TableColumn<Planning, String> colDateTime;
    @FXML private TableColumn<Planning, String> colLocation;
    @FXML private TableColumn<Planning, String> colNeedPartner;
    @FXML private TableColumn<Planning, Void>   colActions;

    private final IPlanningService planningService = new PlanningServiceImpl();
    private final ObservableList<Planning> all = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) return;

        if (adminSidebarController != null) adminSidebarController.setActivePage("planning");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Planning");

        setupFilters();
        setupTable();
        loadPlannings();
    }

    private void setupFilters() {
        if (typeCombo != null) {
            typeCombo.getItems().add("All Types");
            typeCombo.getItems().addAll(java.util.Arrays.stream(PlanningType.values()).map(PlanningType::getDbValue).toList());
            typeCombo.setValue("All Types");
        }
        if (levelCombo != null) {
            levelCombo.getItems().add("All Levels");
            levelCombo.getItems().addAll(java.util.Arrays.stream(PlanningLevel.values()).map(PlanningLevel::getDbValue).toList());
            levelCombo.setValue("All Levels");
        }
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Date", "Type", "Level");
            sortCombo.setValue("Date");
        }
        if (orderCombo != null) {
            orderCombo.getItems().setAll("Descending", "Ascending");
            orderCombo.setValue("Descending");
        }
    }

    private void setupTable() {
        if (planningTable == null) return;

        // Match Symfony screenshot: no horizontal scroll, everything visible
        planningTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        planningTable.setFixedCellSize(70);

        if (colActivity != null) {
            colActivity.setCellValueFactory(d ->
                    new SimpleStringProperty(nvl(d.getValue().getType(), "—") + "\n" + nvl(d.getValue().getLevel(), "—")));
            colActivity.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-text-fill: white;");
                }
            });
        }

        if (colDescription != null) {
            colDescription.setCellValueFactory(d -> new SimpleStringProperty(snip(d.getValue().getDescription(), 90)));
        }
        if (colDateTime != null) {
            colDateTime.setCellValueFactory(d -> new SimpleStringProperty("x")); // placeholder; rendered via cellFactory
            colDateTime.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Planning p = getTableRow().getItem();
                    String date = p.getDate() != null ? p.getDate().format(DT_FMT) : "—";
                    String time = p.getTime() != null ? p.getTime().toString() : "—";

                    SVGPath calendar = new SVGPath();
                    calendar.getStyleClass().addAll("cell-svg", "cell-svg-calendar");
                    calendar.setContent("M7 2v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7zm14 8H5v10h16V10z");
                    Label dateLbl = new Label(date);
                    dateLbl.getStyleClass().add("cell-muted");

                    Label timeLbl = new Label(time);
                    timeLbl.getStyleClass().add("cell-muted");

                    HBox r1 = new HBox(8, calendar, dateLbl);
                    HBox r2 = new HBox(8, new Label(""), timeLbl);
                    VBox box = new VBox(4, r1, r2);
                    box.setStyle("-fx-alignment:center-left;");
                    setGraphic(box);
                    setText(null);
                }
            });
        }
        if (colLocation != null) {
            colLocation.setCellValueFactory(d -> new SimpleStringProperty("x")); // placeholder; rendered via cellFactory
            colLocation.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Planning p = getTableRow().getItem();
                    String loc = nvl(p.getLocalisation(), "—");
                    SVGPath pin = new SVGPath();
                    pin.getStyleClass().addAll("cell-svg", "cell-svg-pin");
                    pin.setContent("M12 22s7-4.35 7-10a7 7 0 1 0-14 0c0 5.65 7 10 7 10zm0-9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z");
                    Label text = new Label(loc);
                    text.setWrapText(true);
                    text.getStyleClass().add("cell-muted");
                    HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
                    text.setMaxWidth(Double.MAX_VALUE);
                    HBox box = new HBox(10, pin, text);
                    box.setStyle("-fx-alignment:center-left;");
                    setGraphic(box);
                    setText(null);
                }
            });
        }
        if (colNeedPartner != null) {
            colNeedPartner.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isNeedPartner() ? "Yes" : "No"));
            colNeedPartner.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("Yes".equalsIgnoreCase(item)) badge.getStyleClass().add("badge-yes");
                    else badge.getStyleClass().add("badge-no");
                    setGraphic(badge);
                    setText(null);
                }
            });
        }

        if (colImage != null) {
            colImage.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        return;
                    }
                    Planning p = getTableRow().getItem();
                    ImageView iv = new ImageView();
                    iv.setFitWidth(52);
                    iv.setFitHeight(36);
                    iv.setPreserveRatio(true);
                    try {
                        if (p.getImage() != null && !p.getImage().isBlank()) {
                            File f = new File(System.getProperty("user.dir"), "uploads/plannings/" + p.getImage());
                            if (f.exists()) iv.setImage(new Image(f.toURI().toString(), 52, 36, true, true));
                        }
                    } catch (Exception ignored) {}
                    StackPane thumb = new StackPane(iv);
                    thumb.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 8; -fx-padding: 6;");
                    setGraphic(thumb);
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = iconBtnSvg("eye", "icon-eye");
                private final Button editBtn = iconBtnSvg("edit", "icon-edit");
                private final Button sessionsBtn = iconBtnSvg("users", "icon-users");
                private final Button delBtn  = iconBtnSvg("trash", "icon-trash");
                // Order exactly like Symfony image: view, edit, participants, delete
                private final HBox box = new HBox(10, viewBtn, editBtn, sessionsBtn, delBtn);
                {
                    box.setStyle("-fx-alignment:center;");
                    viewBtn.setOnAction(e -> {
                        Planning p = rowItem();
                        if (p == null) return;
                        SessionManager.setSelectedPlanning(p);
                        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningDetail.fxml", "Session Details");
                    });
                    sessionsBtn.setOnAction(e -> {
                        Planning p = rowItem();
                        if (p == null) return;
                        SessionManager.setSelectedPlanning(p);
                        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningSessions.fxml", "Training Sessions");
                    });
                    editBtn.setOnAction(e -> {
                        Planning p = rowItem();
                        if (p == null) return;
                        SessionManager.setSelectedPlanning(p);
                        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningForm.fxml", "Edit Planning");
                    });
                    delBtn.setOnAction(e -> {
                        Planning p = rowItem();
                        if (p == null) return;
                        boolean ok = AdminDialogs.confirmDeleteSession();
                        if (!ok) return;
                        new Thread(() -> {
                            try {
                                planningService.deletePlanning(p.getIdPlanning());
                                SessionManager.setPendingFlash("success", "Planning deleted successfully.");
                                Platform.runLater(() -> AdminPlanningController.this.loadPlannings());
                            } catch (Exception ex) {
                                Platform.runLater(() -> alert("Error", ex.getMessage()));
                            }
                        }).start();
                    });
                }

                private Planning rowItem() {
                    return getTableRow() != null ? getTableRow().getItem() : null;
                }

                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        planningTable.setItems(all);

        // Column alignments (Symfony-like)
        if (colImage != null) colImage.setStyle("-fx-alignment: CENTER;");
        if (colDateTime != null) colDateTime.setStyle("-fx-alignment: CENTER-LEFT;");
        if (colNeedPartner != null) colNeedPartner.setStyle("-fx-alignment: CENTER;");
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
        // Simple inline SVG paths (no external libs) to avoid emoji rendering issues on Windows.
        return switch (kind) {
            case "eye" -> "M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8z";
            case "edit" -> "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm14.71-9.04a1.003 1.003 0 0 0 0-1.42l-2.5-2.5a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 2-1.66z";
            case "users" -> "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V20h14v-3.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V20h6v-3.5C24 14.17 19.33 13 16 13z";
            case "trash" -> "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zm3.5-9h1v9h-1V10zm4 0h1v9h-1V10zM15.5 4l-1-1h-5l-1 1H5v2h14V4z";
            default -> "M0 0h24v24H0z";
        };
    }

    private void loadPlannings() {
        new Thread(() -> {
            try {
                List<Planning> list = planningService.getAllPlannings();
                Platform.runLater(() -> {
                    all.setAll(list);
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> alert("Error", e.getMessage()));
            }
        }, "AdminPlanning-Load").start();
    }

    @FXML
    public void handleApplyFilters() {
        applyFilters();
    }

    @FXML
    public void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (typeCombo != null) typeCombo.setValue("All Types");
        if (levelCombo != null) levelCombo.setValue("All Levels");
        if (sortCombo != null) sortCombo.setValue("Date");
        if (orderCombo != null) orderCombo.setValue("Descending");
        applyFilters();
    }

    private void applyFilters() {
        List<Planning> base = List.copyOf(all);

        String q = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String type = typeCombo != null ? typeCombo.getValue() : "All Types";
        String level = levelCombo != null ? levelCombo.getValue() : "All Levels";

        List<Planning> filtered = base.stream().filter(p -> {
            if (q != null && !q.isBlank()) {
                String hay = (nvl(p.getDescription(), "") + " " + nvl(p.getLocalisation(), "") + " " + nvl(p.getType(), "")).toLowerCase();
                if (!hay.contains(q)) return false;
            }
            if (type != null && !"All Types".equals(type) && !type.equalsIgnoreCase(nvl(p.getType(), ""))) return false;
            if (level != null && !"All Levels".equals(level) && !level.equalsIgnoreCase(nvl(p.getLevel(), ""))) return false;
            return true;
        }).toList();

        Comparator<Planning> cmp = switch (nvl(sortCombo != null ? sortCombo.getValue() : "Date", "Date")) {
            case "Type" -> Comparator.comparing(p -> nvl(p.getType(), ""));
            case "Level" -> Comparator.comparing(p -> nvl(p.getLevel(), ""));
            default -> Comparator.comparing(Planning::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Planning::getTime, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        boolean desc = "Descending".equalsIgnoreCase(orderCombo != null ? orderCombo.getValue() : "Descending");
        if (desc) cmp = cmp.reversed();

        List<Planning> sorted = filtered.stream().sorted(cmp).toList();

        all.setAll(sorted);
        if (foundLabel != null) foundLabel.setText("Found " + sorted.size() + " plannings");
    }

    @FXML
    public void handleAddPlanning() {
        SessionManager.clearSelectedPlanning();
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanningForm.fxml", "Create Planning");
    }

    @FXML
    public void handleViewReviews() {
        MainApp.navigateTo("/com/eyetwin/views/AdminReviews.fxml", "Reviews");
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String snip(String s, int max) {
        if (s == null) return "—";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String nvl(String s, String fb) {
        return (s == null || s.isBlank()) ? fb : s;
    }
}

