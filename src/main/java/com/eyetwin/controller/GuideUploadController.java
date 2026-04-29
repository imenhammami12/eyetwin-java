package com.eyetwin.controller;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.entities.GuideVideo;
import com.eyetwin.entities.User;
import com.eyetwin.repository.AgentRepository;
import com.eyetwin.repository.GameRepository;
import com.eyetwin.repository.GuideVideoRepository;
import com.eyetwin.services.CloudinaryUploader;
import com.eyetwin.services.EmailService;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class GuideUploadController {

    @FXML private NavbarController navbarController;
    @FXML private Label formHeading;
    @FXML private Label formSubtitle;
    @FXML private VBox pendingNotice;

    // Form fields
    @FXML private TextField titleField;
    @FXML private TextArea  descriptionField;
    @FXML private ComboBox<String> gameCombo;
    @FXML private ComboBox<String> agentCombo;
    @FXML private TextField mapField;
    @FXML private TextField videoUrlField;
    @FXML private Label     videoFileLabel;
    @FXML private Label     thumbnailUrlField;
    @FXML private Label     thumbnailFileLabel;

    // Error labels
    @FXML private Label titleError;
    @FXML private Label videoError;
    @FXML private Label gameError;

    // Submit button
    @FXML private Button submitBtn;

    // Progress overlay
    @FXML private StackPane progressOverlay;
    @FXML private Label progressLabel;

    private final GameRepository        gameRepo         = new GameRepository();
    private final AgentRepository       agentRepo        = new AgentRepository();
    private final GuideVideoRepository  guideVideoRepo   = new GuideVideoRepository();
    private final CloudinaryUploader    cloudinaryUploader = new CloudinaryUploader();
    private final UserServiceImpl       userService      = new UserServiceImpl();

    private boolean isEdit = false;
    private GuideVideo guideToEdit;
    private File selectedVideoFile;
    private File selectedThumbnailFile;
    private List<Game>  games;
    private List<Agent> agents;

    // ═══════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════
    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("guides");
        if (progressOverlay != null) progressOverlay.setVisible(false);

        new Thread(this::loadFormData).start();
    }

    /** For edit mode — called by NavbarController */
    public void initData(GuideVideo guide) {
        this.isEdit      = true;
        this.guideToEdit = guide;

        if (formHeading  != null) formHeading.setText("Edit Guide");
        if (formSubtitle != null) formSubtitle.setText("Mettre à jour votre guide pour la communauté");
        if (pendingNotice != null) pendingNotice.setVisible(false);
        if (submitBtn != null) submitBtn.setText("Update Guide");

        // Pre-fill once form data has loaded
    }

    // ═══════════════════════════════════════════
    //  LOAD FORM DATA
    // ═══════════════════════════════════════════
    private void loadFormData() {
        try {
            games  = gameRepo.findAllOrderedByName();
            agents = agentRepo.findAll();

            Platform.runLater(() -> {
                populateGameCombo();
                populateAgentCombo(agents);
                populateMapSuggestions();

                if (isEdit && guideToEdit != null) {
                    prefillForm();
                }
            });
        } catch (Exception e) {
            System.err.println("[GuideUploadController] Load error: " + e.getMessage());
        }
    }

    private void populateGameCombo() {
        if (gameCombo == null) return;
        gameCombo.getItems().clear();
        games.forEach(g -> gameCombo.getItems().add(g.getName()));
        gameCombo.setOnAction(e -> {
            String selected = gameCombo.getValue();
            if (selected != null) {
                Game game = games.stream().filter(g -> g.getName().equals(selected)).findFirst().orElse(null);
                if (game != null) {
                    new Thread(() -> {
                        List<Agent> filtered = agentRepo.findByGame(game);
                        Platform.runLater(() -> populateAgentCombo(filtered));
                    }).start();
                }
            }
        });
    }

    private void populateAgentCombo(List<Agent> agentList) {
        if (agentCombo == null) return;
        agentCombo.getItems().clear();
        agentCombo.getItems().add("— None —");
        agentList.forEach(a -> agentCombo.getItems().add(a.getName()));
        agentCombo.getSelectionModel().selectFirst();
    }

    private void populateMapSuggestions() {
        // Maps list — matches Symfony GuideVideoType form choices
        List<String> knownMaps = List.of("All", "Ascent", "Bind", "Breeze", "Fracture",
                "Haven", "Icebox", "Lotus", "Pearl", "Split", "Sunset");
        if (mapField != null) {
            // Show suggestions via context menu or simply use a ComboBox in the FXML
            mapField.setPromptText("Ex: Ascent, Bind, All…");
        }
    }

    private void prefillForm() {
        GuideVideo g = guideToEdit;
        if (titleField       != null) titleField.setText(g.getTitle() != null ? g.getTitle() : "");
        if (descriptionField != null) descriptionField.setText(g.getDescription() != null ? g.getDescription() : "");
        if (videoUrlField    != null) videoUrlField.setText(g.getVideoUrl() != null ? g.getVideoUrl() : "");
        if (mapField         != null) mapField.setText(g.getMap() != null ? g.getMap() : "");

        if (gameCombo != null && g.getGame() != null)
            gameCombo.setValue(g.getGame().getName());

        if (agentCombo != null && g.getAgent() != null)
            agentCombo.setValue(g.getAgent().getName());
    }

    // ═══════════════════════════════════════════
    //  FILE PICKERS
    // ═══════════════════════════════════════════
    @FXML
    public void pickVideoFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une vidéo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Vidéo (MP4/WEBM/MOV/OGG)", "*.mp4", "*.webm", "*.mov", "*.ogg"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            selectedVideoFile = file;
            if (videoFileLabel != null) videoFileLabel.setText(file.getName());
        }
    }

    @FXML
    public void pickThumbnailFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une miniature");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image (JPG/PNG/GIF/WEBP)", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            selectedThumbnailFile = file;
            if (thumbnailFileLabel != null) thumbnailFileLabel.setText(file.getName());
        }
    }

    // ═══════════════════════════════════════════
    //  VALIDATE & SUBMIT
    // ═══════════════════════════════════════════
    @FXML
    public void submitForm() {
        clearErrors();

        boolean valid = true;

        String title = titleField != null ? titleField.getText().trim() : "";
        if (title.length() < 3) {
            showError(titleError, "Le titre doit contenir au moins 3 caractères.");
            valid = false;
        }

        String videoUrl  = videoUrlField != null ? videoUrlField.getText().trim() : "";
        boolean hasVideo = selectedVideoFile != null || !videoUrl.isEmpty();
        if (!hasVideo) {
            showError(videoError, "Veuillez fournir une URL vidéo ou uploader un fichier vidéo.");
            valid = false;
        }

        String gameName = gameCombo != null ? gameCombo.getValue() : null;
        if (gameName == null || gameName.isEmpty()) {
            showError(gameError, "Veuillez sélectionner un jeu.");
            valid = false;
        }

        if (!valid) return;

        showProgress("Uploading...");
        submitBtn.setDisable(true);

        new Thread(() -> {
            try {
                GuideVideo guide = isEdit ? guideToEdit : new GuideVideo();

                // Basic fields
                guide.setTitle(title);
                guide.setDescription(descriptionField != null ? descriptionField.getText().trim() : "");
                guide.setMap(mapField != null && !mapField.getText().trim().isEmpty()
                        ? mapField.getText().trim() : "All");

                // Game
                Game game = games.stream().filter(g -> g.getName().equals(gameName)).findFirst().orElse(null);
                guide.setGame(game);

                // Agent
                String agentName = agentCombo != null ? agentCombo.getValue() : null;
                if (agentName != null && !agentName.startsWith("—")) {
                    Agent agent = agents.stream().filter(a -> a.getName().equals(agentName)).findFirst().orElse(null);
                    guide.setAgent(agent);
                }

                // Video upload to Cloudinary
                if (selectedVideoFile != null) {
                    Platform.runLater(() -> updateProgress("Uploading video to Cloudinary..."));
                    var uploadResult = cloudinaryUploader.uploadVideo(selectedVideoFile);
                    if (uploadResult != null && uploadResult.containsKey("secure_url")) {
                        System.out.println("[GuideUploadController] Cloudinary upload result:");
                        System.out.println("  - secure_url: " + uploadResult.get("secure_url"));
                        System.out.println("  - public_id: " + uploadResult.get("public_id"));
                        guide.setVideoUrl((String) uploadResult.get("secure_url"));
                    } else {
                        Platform.runLater(() -> {
                            hideProgress();
                            showError(videoError, "L'upload Cloudinary a échoué.");
                            submitBtn.setDisable(false);
                        });
                        return;
                    }
                } else {
                    guide.setVideoUrl(videoUrl);
                }

                // Thumbnail
                if (selectedThumbnailFile != null) {
                    Platform.runLater(() -> updateProgress("Saving thumbnail..."));
                    Path uploadDir = Path.of(System.getProperty("user.dir"), "public", "uploads", "guides");
                    Files.createDirectories(uploadDir);

                    String fileName = "thumb_" + System.currentTimeMillis() + "_" + selectedThumbnailFile.getName();
                    Path destination = uploadDir.resolve(fileName);

                    Files.copy(selectedThumbnailFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                    guide.setThumbnail("/uploads/guides/" + fileName);
                }

                // Status & author
                if (!isEdit) {
                    User currentUser = SessionManager.getCurrentUser();
                    if (currentUser == null) {
                        Platform.runLater(() -> {
                            hideProgress();
                            showError(videoError, "Erreur : Utilisateur non authentifié. Veuillez vous reconnecter.");
                            submitBtn.setDisable(false);
                        });
                        return;
                    }
                    guide.setUploadedBy(currentUser);
                    guide.setStatus("pending");
                    System.out.println("[GuideUploadController] Setting uploadedBy to: " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
                }

                // Validate required fields before persistence
                if (guide.getGame() == null) {
                    Platform.runLater(() -> {
                        hideProgress();
                        showError(gameError, "Erreur : Veuillez sélectionner un jeu.");
                        submitBtn.setDisable(false);
                    });
                    return;
                }

                // Persist
                System.out.println("[GuideUploadController] About to save guide: " + guide.getTitle());
                Platform.runLater(() -> updateProgress(isEdit ? "Updating guide..." : "Saving guide..."));
                if (isEdit) {
                    guideVideoRepo.update(guide);
                    System.out.println("[GuideUploadController] Guide updated");
                } else {
                    GuideVideo savedGuide = guideVideoRepo.save(guide);
                    System.out.println("[GuideUploadController] Guide saved with ID: " + (savedGuide != null ? savedGuide.getId() : "null"));
                    
                    if (savedGuide == null || savedGuide.getId() == null) {
                        Platform.runLater(() -> {
                            hideProgress();
                            showError(videoError, "Erreur : Échec de la sauvegarde du guide en base de données.");
                            submitBtn.setDisable(false);
                        });
                        return;
                    }
                    
                    guide = savedGuide;  // Use the saved guide with ID
                }

                if (!isEdit) {
                    notifyAdminsAboutPendingGuide(guide);
                }

                Platform.runLater(() -> {
                    hideProgress();
                    String msg = isEdit
                            ? "Guide mis à jour avec succès !"
                            : "Guide uploadé ! En attente de validation par les admins.";
                    showSuccessAlert(msg);
                    navbarController.navigateTo("MyGuides.fxml");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    submitBtn.setDisable(false);
                    showError(videoError, "Erreur : " + e.getMessage());
                });
            }
        }).start();
    }

    private void notifyAdminsAboutPendingGuide(GuideVideo guide) {
        User uploader = guide != null ? guide.getUploadedBy() : null;
        if (uploader == null) {
            return;
        }

        List<User> admins = userService.getAllUsers().stream()
                .filter(user -> user != null && user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> user.isAdmin() || user.isSuperAdmin())
                .toList();

        if (admins.isEmpty()) {
            return;
        }

        String uploaderName = uploader.getFullName() != null && !uploader.getFullName().isBlank()
                ? uploader.getFullName()
                : uploader.getUsername();

        String gameName = guide.getGame() != null ? guide.getGame().getName() : null;
        String agentName = guide.getAgent() != null ? guide.getAgent().getName() : null;

        admins.forEach(admin -> EmailService.getInstance().sendGuideApprovalRequestEmail(
                admin.getEmail(),
                uploaderName,
                uploader.getEmail(),
                guide.getTitle(),
                gameName,
                agentName,
                guide.getMap()));
    }

    // ═══════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════
    private void clearErrors() {
        if (titleError != null) { titleError.setVisible(false); titleError.setManaged(false); }
        if (videoError != null) { videoError.setVisible(false); videoError.setManaged(false); }
        if (gameError  != null) { gameError.setVisible(false);  gameError.setManaged(false); }
    }

    private void showError(Label errorLabel, String message) {
        if (errorLabel == null) return;
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        // Shake animation
        Timeline shake = new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(errorLabel.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60),  new KeyValue(errorLabel.translateXProperty(), -8)),
                new KeyFrame(Duration.millis(120), new KeyValue(errorLabel.translateXProperty(), 8)),
                new KeyFrame(Duration.millis(180), new KeyValue(errorLabel.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(220), new KeyValue(errorLabel.translateXProperty(), 0))
        );
        shake.play();
    }

    private void showProgress(String msg) {
        if (progressOverlay != null) {
            if (progressLabel != null) progressLabel.setText(msg);
            progressOverlay.setVisible(true);
            progressOverlay.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(200), progressOverlay);
            ft.setToValue(1); ft.play();
        }
    }

    private void updateProgress(String msg) {
        if (progressLabel != null) progressLabel.setText(msg);
    }

    private void hideProgress() {
        if (progressOverlay != null) progressOverlay.setVisible(false);
        if (submitBtn != null) submitBtn.setDisable(false);
    }

    private void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle("Succès");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML public void goBack() {
        navbarController.navigateTo("MyGuides.fxml");
    }
}
