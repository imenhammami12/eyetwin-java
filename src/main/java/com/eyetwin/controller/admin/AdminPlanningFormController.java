package com.eyetwin.controller.admin;

import com.eyetwin.MainApp;
import com.eyetwin.entities.Planning;
import com.eyetwin.entities.PlanningLevel;
import com.eyetwin.entities.PlanningType;
import com.eyetwin.services.PlanningServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class AdminPlanningFormController {

    private static final DateTimeFormatter TIME_FMT_24 = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT);
    private static final DateTimeFormatter TIME_FMT_12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final String UPLOAD_DIR = "uploads/plannings/";

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController adminTopbarController;

    @FXML private Label pageTitleLabel;
    @FXML private StackPane uploadPane;
    @FXML private Label uploadHintLabel;
    @FXML private ImageView coverPreview;
    @FXML private Label selectedImageLabel;

    @FXML private ComboBox<PlanningType> typeCombo;
    @FXML private ComboBox<PlanningLevel> levelCombo;

    @FXML private RadioButton onlineRadio;
    @FXML private RadioButton onSiteRadio;
    @FXML private TextField locationDetailsField;

    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private Button timePickerBtn;
    @FXML private TextArea descriptionArea;
    @FXML private CheckBox needPartnerCheck;

    @FXML private Button saveBtn;
    @FXML private Label errorLabel;

    private final PlanningServiceImpl planningService = new PlanningServiceImpl();

    private Planning editing; // null => create
    private byte[] selectedImageBytes;
    private String selectedImageExt;
    private ContextMenu timeMenu;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) return;

        if (adminSidebarController != null) adminSidebarController.setActivePage("planning");
        if (adminTopbarController != null) adminTopbarController.setTitle("Planning");

        setupCombos();
        setupLocationToggle();
        setupUpload();

        editing = SessionManager.getSelectedPlanning();
        if (editing == null) {
            setCreateMode();
        } else {
            setEditMode(editing);
        }
    }

    private void setupCombos() {
        if (typeCombo != null) {
            typeCombo.getItems().setAll(PlanningType.values());
            typeCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(PlanningType object) { return object == null ? "" : object.getLabel(); }
                @Override public PlanningType fromString(String string) { return null; }
            });
        }
        if (levelCombo != null) {
            levelCombo.getItems().setAll(PlanningLevel.values());
            levelCombo.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(PlanningLevel object) { return object == null ? "" : object.getLabel(); }
                @Override public PlanningLevel fromString(String string) { return null; }
            });
        }
    }

    private void setupLocationToggle() {
        ToggleGroup g = new ToggleGroup();
        if (onlineRadio != null) onlineRadio.setToggleGroup(g);
        if (onSiteRadio != null) onSiteRadio.setToggleGroup(g);
        if (onlineRadio != null) onlineRadio.setSelected(true);

        g.selectedToggleProperty().addListener((obs, o, n) -> updateLocationUi());
        updateLocationUi();
    }

    private void updateLocationUi() {
        boolean onSite = onSiteRadio != null && onSiteRadio.isSelected();
        if (onSite) {
            if (locationDetailsField != null) {
                locationDetailsField.setDisable(false);
                // When switching from Online -> On Site, clear the "Online" placeholder value like Symfony
                if ("Online".equalsIgnoreCase(locationDetailsField.getText())) {
                    locationDetailsField.clear();
                }
            }
        }
        if (!onSite && locationDetailsField != null) {
            locationDetailsField.setText("Online");
            locationDetailsField.setDisable(true);
        }
    }

    private void setupUpload() {
        if (uploadPane == null) return;
        uploadPane.setOnMouseClicked(e -> chooseImage());
        if (coverPreview != null) {
            coverPreview.setVisible(false);
            coverPreview.setManaged(false);
        }
    }

    private void setCreateMode() {
        if (pageTitleLabel != null) pageTitleLabel.setText("Create New Session");
        if (saveBtn != null) saveBtn.setText("Create Session");
        if (selectedImageLabel != null) selectedImageLabel.setText("");
        if (datePicker != null) datePicker.setValue(LocalDate.now());
        if (timeField != null) timeField.setText("--:-- --");
        if (locationDetailsField != null) {
            locationDetailsField.setText("Online");
            locationDetailsField.setDisable(true);
        }
        clearError();
    }

    private void setEditMode(Planning p) {
        if (pageTitleLabel != null) pageTitleLabel.setText("Edit Session");
        if (saveBtn != null) saveBtn.setText("Save Changes");

        if (typeCombo != null) typeCombo.setValue(p.getTypeEnum().orElse(null));
        if (levelCombo != null) levelCombo.setValue(p.getLevelEnum().orElse(null));
        if (datePicker != null) datePicker.setValue(p.getDate());
        if (timeField != null) timeField.setText(p.getTime() != null ? format12(p.getTime()) : "--:-- --");
        if (descriptionArea != null) descriptionArea.setText(p.getDescription());
        if (needPartnerCheck != null) needPartnerCheck.setSelected(p.isNeedPartner());

        // location (we store address in localisation; Online keeps "Online")
        String loc = p.getLocalisation();
        if (loc != null && onSiteRadio != null && onlineRadio != null) {
            if (loc.equalsIgnoreCase("Online")) {
                onlineRadio.setSelected(true);
            } else {
                onSiteRadio.setSelected(true);
            }
        }
        if (locationDetailsField != null) {
            locationDetailsField.setText(loc != null ? loc : "");
            boolean onSite = onSiteRadio != null && onSiteRadio.isSelected();
            locationDetailsField.setDisable(!onSite);
        }

        // preview existing image if present
        if (p.getImage() != null && !p.getImage().isBlank()) {
            if (selectedImageLabel != null) selectedImageLabel.setText(p.getImage());
            try {
                Path imgPath = Paths.get(UPLOAD_DIR, p.getImage());
                if (Files.exists(imgPath)) {
                    Image img = new Image(imgPath.toUri().toString(), true);
                    showPreview(img);
                }
            } catch (Exception ignored) {
            }
        }

        clearError();
    }

    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select cover image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File f = fc.showOpenDialog(MainApp.getPrimaryStage());
        if (f == null) return;

        try {
            long max = 2L * 1024L * 1024L;
            if (f.length() > max) {
                setError("Image too large (max 2MB).");
                return;
            }
            selectedImageBytes = Files.readAllBytes(f.toPath());
            selectedImageExt = extOf(f.getName());

            if (selectedImageLabel != null) selectedImageLabel.setText(f.getName());
            Image img = new Image(f.toURI().toString(), true);
            showPreview(img);
            clearError();
        } catch (Exception ex) {
            setError("Failed to read image: " + ex.getMessage());
        }
    }

    private void showPreview(Image img) {
        if (coverPreview == null) return;
        coverPreview.setImage(img);
        coverPreview.setVisible(true);
        coverPreview.setManaged(true);
        if (uploadHintLabel != null) uploadHintLabel.setVisible(false);
    }

    private String extOf(String name) {
        if (name == null) return "png";
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "png";
        String ext = name.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (ext.equals("jpeg")) return "jpg";
        return ext;
    }

    private void clearError() {
        if (errorLabel != null) errorLabel.setText("");
    }

    private void setError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg == null ? "" : msg);
    }

    private String locationValue() {
        boolean onSite = onSiteRadio != null && onSiteRadio.isSelected();
        if (!onSite) return "Online";
        return locationDetailsField != null ? locationDetailsField.getText() : "";
    }

    private LocalTime parseTime(String input) {
        if (input == null) throw new DateTimeParseException("Time required", "", 0);
        String t = input.trim();
        if (t.isEmpty() || t.equals("--:-- --")) throw new DateTimeParseException("Time required", t, 0);
        try {
            // Accept "11:59 AM"
            return LocalTime.parse(t.toUpperCase(Locale.ROOT), TIME_FMT_12);
        } catch (Exception ignored) {
        }
        // Fallback accept "23:15"
        return LocalTime.parse(t, TIME_FMT_24);
    }

    private String format12(LocalTime time) {
        return TIME_FMT_12.format(time);
    }

    @FXML
    public void handlePickTime() {
        if (timePickerBtn == null) return;
        if (timeMenu == null) timeMenu = buildTimeMenu();
        if (timeMenu.isShowing()) {
            timeMenu.hide();
        } else {
            timeMenu.show(timePickerBtn, Side.BOTTOM, 0, 6);
        }
    }

    private ContextMenu buildTimeMenu() {
        ListView<String> hours = new ListView<>();
        ListView<String> mins = new ListView<>();
        ListView<String> ampm = new ListView<>();

        hours.getItems().setAll(
                "01","02","03","04","05","06","07","08","09","10","11","12"
        );
        for (int i = 0; i < 60; i++) mins.getItems().add(String.format(Locale.ROOT, "%02d", i));
        ampm.getItems().setAll("AM", "PM");

        hours.setPrefWidth(90);
        mins.setPrefWidth(90);
        ampm.setPrefWidth(90);
        hours.setPrefHeight(260);
        mins.setPrefHeight(260);
        ampm.setPrefHeight(260);

        hours.getSelectionModel().select(10); // default 11
        mins.getSelectionModel().select(0);
        ampm.getSelectionModel().select(0);

        // Seed from current field value if present
        try {
            LocalTime t = parseTime(timeField != null ? timeField.getText() : null);
            int h24 = t.getHour();
            int m = t.getMinute();
            boolean isPm = h24 >= 12;
            int h12 = h24 % 12;
            if (h12 == 0) h12 = 12;
            hours.getSelectionModel().select(String.format(Locale.ROOT, "%02d", h12));
            mins.getSelectionModel().select(String.format(Locale.ROOT, "%02d", m));
            ampm.getSelectionModel().select(isPm ? "PM" : "AM");
        } catch (Exception ignored) {
        }

        Runnable sync = () -> {
            String h = hours.getSelectionModel().getSelectedItem();
            String m = mins.getSelectionModel().getSelectedItem();
            String a = ampm.getSelectionModel().getSelectedItem();
            if (h == null || m == null || a == null) return;
            if (timeField != null) timeField.setText(h + ":" + m + " " + a);
            clearError();
        };

        hours.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> sync.run());
        mins.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> sync.run());
        ampm.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> sync.run());

        SplitPane pane = new SplitPane(hours, mins, ampm);
        pane.setDividerPositions(0.33, 0.66);
        pane.setPrefWidth(290);
        pane.setPrefHeight(260);

        CustomMenuItem item = new CustomMenuItem(pane, false);
        ContextMenu menu = new ContextMenu(item);
        menu.setAutoHide(true);
        menu.setHideOnEscape(true);
        return menu;
    }

    @FXML
    public void handleSave() {
        clearError();

        PlanningType type = typeCombo != null ? typeCombo.getValue() : null;
        PlanningLevel level = levelCombo != null ? levelCombo.getValue() : null;
        LocalDate date = datePicker != null ? datePicker.getValue() : null;
        String desc = descriptionArea != null ? descriptionArea.getText() : null;
        boolean needPartner = needPartnerCheck != null && needPartnerCheck.isSelected();

        if (type == null) { setError("Game type is required."); return; }
        if (level == null) { setError("Skill level is required."); return; }
        if (date == null) { setError("Date is required."); return; }
        if (desc == null || desc.isBlank()) { setError("Description is required."); return; }

        // Location rules
        boolean onSite = onSiteRadio != null && onSiteRadio.isSelected();
        String loc = locationValue();
        if (onSite) {
            if (loc == null || loc.isBlank() || "Online".equalsIgnoreCase(loc)) {
                setError("Location details are required for On Site.");
                return;
            }
        } else {
            loc = "Online";
        }

        LocalTime time;
        try {
            time = parseTime(timeField != null ? timeField.getText() : null);
        } catch (DateTimeParseException ex) {
            setError("Time is required.");
            return;
        }

        // Image is mandatory (create) and also mandatory on edit if no existing image
        boolean hasExistingImage = editing != null && editing.getImage() != null && !editing.getImage().isBlank();
        boolean hasNewImage = selectedImageBytes != null && selectedImageBytes.length > 0 && selectedImageExt != null && !selectedImageExt.isBlank();
        if (!hasNewImage && !hasExistingImage) {
            setError("Cover image is required.");
            return;
        }

        // Date+Time must be in the future (planning)
        LocalDateTime scheduled = LocalDateTime.of(date, time);
        if (!scheduled.isAfter(LocalDateTime.now())) {
            setError("Date & time must be in the future.");
            return;
        }

        Planning p = (editing != null) ? editing : new Planning();
        p.setDate(date);
        p.setTime(time);
        p.setLocalisation(loc);
        p.setDescription(desc.trim());
        p.setNeedPartner(needPartner);
        p.setTypeEnum(type);
        p.setLevelEnum(level);

        byte[] imgBytes = selectedImageBytes;
        String imgExt = selectedImageExt;

        new Thread(() -> {
            try {
                if (editing == null) {
                    if (imgBytes != null && imgExt != null) planningService.createPlanning(p, imgBytes, imgExt);
                    else planningService.createPlanning(p);
                    SessionManager.setPendingFlash("success", "Planning created successfully.");
                } else {
                    if (imgBytes != null && imgExt != null) planningService.updatePlanning(p, imgBytes, imgExt);
                    else planningService.updatePlanning(p);
                    SessionManager.setPendingFlash("success", "Planning updated successfully.");
                }
                Platform.runLater(() -> {
                    SessionManager.clearSelectedPlanning();
                    MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> setError(ex.getMessage()));
            }
        }, "AdminPlanning-Save").start();
    }

    @FXML
    public void handleCancel() {
        SessionManager.clearSelectedPlanning();
        MainApp.navigateTo("/com/eyetwin/views/AdminPlanning.fxml", "Planning");
    }

    @FXML
    public void handleBackToList() {
        handleCancel();
    }
}

