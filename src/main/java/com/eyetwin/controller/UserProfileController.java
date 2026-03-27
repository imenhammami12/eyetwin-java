package com.eyetwin.controller;

import com.eyetwin.MainApp;
import com.eyetwin.dao.TeamDAO;
import com.eyetwin.dao.UserDAO;
import com.eyetwin.model.Team;
import com.eyetwin.model.User;
import com.eyetwin.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * UserProfileController — miroir de Symfony UserController (#[Route('/profile')])
 *
 * Gère les 3 vues :
 *   - UserProfile.fxml     → user_profile
 *   - UserEditProfile.fxml → user_edit_profile
 *   - UserStatistics.fxml  → user_statistics
 *
 * initialize() détecte quelle vue est chargée via les fx:id présents.
 * UserStatisticsController et UserEditProfileController héritent de cette classe.
 */
public class UserProfileController {

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm a");
    private static final String VIEWS = "/com/eyetwin/views/";

    // ── NAVBAR (commun aux 3 vues) ──
    @FXML protected Label    navUsername;
    @FXML protected Label    navAvatarInitial;
    @FXML protected Label    coinsNavLabel;
    @FXML protected MenuItem profileAdminItem;
    @FXML protected SeparatorMenuItem profileAdminSep;

    // ── UserProfile.fxml ──
    @FXML protected ImageView profileImageView;
    @FXML protected StackPane avatarPlaceholder;
    @FXML protected Label     avatarInitial;
    @FXML protected Label     usernameLabel;
    @FXML protected Label     emailLabel;
    @FXML protected Label     statusActiveBadge;
    @FXML protected Label     coachBadge;
    @FXML protected Label     adminBadge;
    @FXML protected Button    applyCoachBtn;
    @FXML protected VBox      bioCard;
    @FXML protected Label     bioLabel;
    @FXML protected VBox      coachCongratsBanner;
    @FXML protected VBox      coachApplicationCard;
    @FXML protected Label     coachApplicationStatus;
    @FXML protected Button    reapplyBtn;
    @FXML protected Label     coinBalanceLabel;
    @FXML protected Label     teamsCountLabel;
    @FXML protected Label     ownedTeamsLabel;
    @FXML protected Label     memberSinceLabel;
    @FXML protected Label     lastLoginLabel;
    @FXML protected Label     coinsInfoLabel;

    // ── UserStatistics.fxml ──
    @FXML protected Label statTeamsCount;
    @FXML protected Label statOwnedTeams;
    @FXML protected Label statNotifications;
    @FXML protected Label statAccountAgeDays;
    @FXML protected Label registrationDateLabel;
    @FXML protected Label accountAgeDaysLabel;
    @FXML protected Label accountStatusLabel;
    @FXML protected Label teamsJoinedBig;
    @FXML protected Label teamsCreatedBig;
    @FXML protected VBox  ownedTeamsListBox;
    @FXML protected Label coachRoleBadge;
    @FXML protected Label adminRoleBadge;

    // ── UserEditProfile.fxml ──
    @FXML protected TextField  usernameField;
    @FXML protected TextField  emailField;
    @FXML protected TextField  fullNameField;
    @FXML protected TextArea   bioField;
    @FXML protected Label      bioCharCounter;
    @FXML protected Label      emailErrorLabel;
    @FXML protected Label      fullNameErrorLabel;
    @FXML protected Label      bioErrorLabel;
    @FXML protected Button     saveBtn;
    @FXML protected VBox       flashSuccessBox;
    @FXML protected Label      flashSuccessLabel;
    @FXML protected VBox       flashErrorBox;
    @FXML protected Label      flashErrorLabel;
    @FXML protected ImageView  previewImageView;
    @FXML protected StackPane  previewPlaceholder;
    @FXML protected Label      previewInitial;
    @FXML protected Label      previewUsername;
    @FXML protected Label      previewEmail;
    @FXML protected Label      previewTeamsLabel;
    @FXML protected Label      previewMemberSince;
    @FXML protected Label      selectedFileLabel;

    // ── STATE ──
    protected final UserDAO userDAO = new UserDAO();
    protected final TeamDAO teamDAO = new TeamDAO();
    protected File selectedProfilePicture = null;

    // ─────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        fillNavbar(user);

        // Détection de la vue active via les fx:id
        if (usernameLabel  != null) initProfileView(user);
        if (statTeamsCount != null) initStatisticsView(user);
        if (emailField     != null) initEditProfileView(user);
    }

    // ─────────────────────────────────────────────────────────
    //  NAVBAR
    // ─────────────────────────────────────────────────────────
    private void fillNavbar(User user) {
        if (navUsername != null)
            navUsername.setText(user.getUsername() != null
                    ? user.getUsername().toUpperCase() : "PLAYER");
        if (navAvatarInitial != null && user.getUsername() != null && !user.getUsername().isEmpty())
            navAvatarInitial.setText(String.valueOf(user.getUsername().charAt(0)).toUpperCase());
        if (coinsNavLabel != null)
            coinsNavLabel.setText(String.valueOf(user.getCoinBalance()));

        boolean isAdmin = hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_SUPER_ADMIN");
        if (profileAdminItem != null) profileAdminItem.setVisible(isAdmin);
        if (profileAdminSep  != null) profileAdminSep.setVisible(isAdmin);
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW : UserProfile  (user_profile)
    // ─────────────────────────────────────────────────────────
    private void initProfileView(User user) {
        loadAvatar(user, profileImageView, avatarPlaceholder, avatarInitial, 48);
        setText(usernameLabel, user.getUsername());
        setText(emailLabel,    user.getEmail());

        boolean isCoach = hasRole(user, "ROLE_COACH");
        boolean isAdmin = hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_SUPER_ADMIN");
        show(coachBadge,         isCoach);
        show(adminBadge,         isAdmin);
        show(applyCoachBtn,      !isCoach && !isAdmin);
        show(coachCongratsBanner, isCoach);

        if (user.getBio() != null && !user.getBio().isBlank()) {
            show(bioCard, true);
            setText(bioLabel, user.getBio());
        }
        setText(coinBalanceLabel, String.valueOf(user.getCoinBalance()));
        setText(coinsInfoLabel,   String.valueOf(user.getCoinBalance()));
        if (user.getCreatedAt() != null)
            setText(memberSinceLabel, user.getCreatedAt().format(DATE_FMT));
        if (user.getLastLogin() != null)
            setText(lastLoginLabel, user.getLastLogin().format(DATETIME_FMT));

        // Stats async — utilise countMemberTeams + countOwnedTeams de TeamDAO
        new Thread(() -> {
            try {
                int joined = teamDAO.countMemberTeams(user.getId());
                int owned  = teamDAO.countOwnedTeams(user.getId());
                Platform.runLater(() -> {
                    setText(teamsCountLabel, String.valueOf(joined));
                    setText(ownedTeamsLabel, String.valueOf(owned));
                });
            } catch (SQLException e) {
                System.err.println("[Profile] Stats error: " + e.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW : UserStatistics  (user_statistics)
    // ─────────────────────────────────────────────────────────
    private void initStatisticsView(User user) {
        if (user.getCreatedAt() != null) {
            setText(registrationDateLabel, user.getCreatedAt().format(DATETIME_FMT));
            long days = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
            setText(statAccountAgeDays,  String.valueOf(days));
            setText(accountAgeDaysLabel, days + " days");
        }
        if (user.getLastLogin() != null)
            setText(lastLoginLabel, user.getLastLogin().format(DATETIME_FMT));

        if (accountStatusLabel != null) {
            accountStatusLabel.setText("Active");
            accountStatusLabel.setStyle(
                    "-fx-background-color: rgba(0,230,118,0.1);" +
                            "-fx-border-color: rgba(0,230,118,0.3); -fx-border-width: 1;" +
                            "-fx-border-radius: 20; -fx-background-radius: 20;" +
                            "-fx-text-fill: #00e676; -fx-font-size: 10;" +
                            "-fx-font-weight: bold; -fx-padding: 4 12;");
        }

        show(coachRoleBadge, hasRole(user, "ROLE_COACH"));
        show(adminRoleBadge, hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_SUPER_ADMIN"));

        // Stats async
        new Thread(() -> {
            try {
                int joined       = teamDAO.countMemberTeams(user.getId());
                int owned        = teamDAO.countOwnedTeams(user.getId());
                int notifs       = teamDAO.countUnreadNotifications(user.getId());
                List<Team> teams = teamDAO.getOwnedTeams(user.getId());

                Platform.runLater(() -> {
                    setText(statTeamsCount,    String.valueOf(joined));
                    setText(statOwnedTeams,    String.valueOf(owned));
                    setText(statNotifications, String.valueOf(notifs));
                    setText(teamsJoinedBig,    String.valueOf(joined));
                    setText(teamsCreatedBig,   String.valueOf(owned));

                    if (ownedTeamsListBox != null) {
                        ownedTeamsListBox.getChildren().clear();
                        for (Team t : teams)
                            ownedTeamsListBox.getChildren().add(buildTeamRow(t));
                    }
                });
            } catch (SQLException e) {
                System.err.println("[Statistics] Error: " + e.getMessage());
            }
        }).start();
    }

    private HBox buildTeamRow(Team t) {
        HBox row = new HBox();
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03);" +
                "-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1;" +
                "-fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 11 16;");
        Label name = new Label(t.getName());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label members = new Label(t.getActiveMembersCount() + " / " + t.getMaxMembers() + " members");
        members.setStyle("-fx-background-color: rgba(232,55,42,0.1);" +
                "-fx-border-color: rgba(232,55,42,0.25); -fx-border-width: 1;" +
                "-fx-border-radius: 20; -fx-background-radius: 20;" +
                "-fx-text-fill: #ff6b5b; -fx-font-size: 10; -fx-font-weight: bold;" +
                "-fx-padding: 4 12;");
        row.getChildren().addAll(name, spacer, members);
        return row;
    }

    // ─────────────────────────────────────────────────────────
    //  VIEW : UserEditProfile  (user_edit_profile)
    // ─────────────────────────────────────────────────────────
    private void initEditProfileView(User user) {
        if (usernameField != null) usernameField.setText(user.getUsername());
        if (emailField    != null) emailField.setText(user.getEmail());
        if (fullNameField != null) fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        if (bioField      != null) bioField.setText(user.getBio() != null ? user.getBio() : "");

        loadAvatar(user, previewImageView, previewPlaceholder, previewInitial, 45);
        setText(previewUsername, user.getUsername());
        setText(previewEmail,    user.getEmail());
        if (user.getCreatedAt() != null)
            setText(previewMemberSince, "Member since " + user.getCreatedAt().getYear());

        if (bioField != null && bioCharCounter != null) {
            updateBioCounter();
            bioField.textProperty().addListener((obs, o, n) -> updateBioCounter());
        }

        new Thread(() -> {
            try {
                int joined = teamDAO.countMemberTeams(user.getId());
                Platform.runLater(() -> setText(previewTeamsLabel, joined + " Teams"));
            } catch (SQLException e) {
                System.err.println("[EditProfile] Preview stats error: " + e.getMessage());
            }
        }).start();
    }

    private void updateBioCounter() {
        if (bioField == null || bioCharCounter == null) return;
        int len = bioField.getText().length();
        bioCharCounter.setText(len + " / 500 characters");
        bioCharCounter.setStyle(len > 500
                ? "-fx-text-fill: #f44a40; -fx-font-size: 11;"
                : "-fx-text-fill: rgba(255,255,255,0.28); -fx-font-size: 11;");
    }

    // ─────────────────────────────────────────────────────────
    //  ACTIONS — Edit Profile
    // ─────────────────────────────────────────────────────────
    @FXML
    public void handlePickProfilePicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Picture");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images",
                        "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = chooser.showOpenDialog(resolveStage());
        if (file != null) {
            selectedProfilePicture = file;
            if (selectedFileLabel != null) {
                selectedFileLabel.setText("📎 " + file.getName());
                selectedFileLabel.setVisible(true);
                selectedFileLabel.setManaged(true);
            }
            try {
                Image img = new Image(file.toURI().toString(), 90, 90, false, true);
                if (previewImageView != null) {
                    previewImageView.setImage(img);
                    previewImageView.setClip(new Circle(45, 45, 45));
                    show(previewImageView,  true);
                    show(previewPlaceholder, false);
                }
            } catch (Exception e) {
                System.err.println("[EditProfile] Preview error: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleSave() {
        hideFlash();
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String newEmail    = emailField    != null ? emailField.getText().trim()    : user.getEmail();
        String newFullName = fullNameField != null ? fullNameField.getText().trim() : "";
        String newBio      = bioField      != null ? bioField.getText().trim()      : "";

        boolean valid = true;
        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            showFieldError(emailErrorLabel, "Please enter a valid email address.");
            valid = false;
        }
        if (newBio.length() > 500) {
            showFieldError(bioErrorLabel, "Biography must be 500 characters or less.");
            valid = false;
        }
        if (!valid) return;

        if (saveBtn != null) { saveBtn.setDisable(true); saveBtn.setText("Saving…"); }
        File pictureFile = selectedProfilePicture;

        new Thread(() -> {
            try {
                if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                    User existing = userDAO.findByEmail(newEmail);
                    if (existing != null && existing.getId() != user.getId()) {
                        Platform.runLater(() -> {
                            showFieldError(emailErrorLabel,
                                    "This email address is already registered by another user.");
                            resetSaveBtn();
                        });
                        return;
                    }
                }
                if (pictureFile != null) {
                    byte[] bytes    = Files.readAllBytes(pictureFile.toPath());
                    String ext      = getExtension(pictureFile.getName());
                    String filename = "profile_" + user.getId() + "_"
                            + System.currentTimeMillis() + "." + ext;
                    userDAO.saveProfilePicture(user.getId(), bytes, filename);
                    user.setProfilePicture(filename);
                }
                user.setEmail(newEmail);
                user.setFullName(newFullName.isEmpty() ? null : newFullName);
                user.setBio(newBio.isEmpty() ? null : newBio);
                userDAO.update(user);
                SessionManager.refresh();

                Platform.runLater(() -> {
                    selectedProfilePicture = null;
                    resetSaveBtn();
                    // ── Redirect to profile page after successful save ──
                    navigateTo("UserProfile.fxml");
                });
            } catch (Exception e) {
                System.err.println("[EditProfile] Save error: " + e.getMessage());
                Platform.runLater(() -> {
                    showError("Error saving profile: " + e.getMessage());
                    resetSaveBtn();
                });
            }
        }).start();
    }
    private void resetSaveBtn() {
        if (saveBtn != null) { saveBtn.setDisable(false); saveBtn.setText("💾  Save Changes"); }
    }

    // ─────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────
    @FXML public void goToProfile()     { navigateTo("UserProfile.fxml"); }
    @FXML public void goToEditProfile() { navigateTo("UserEditProfile.fxml"); }
    @FXML public void goToStatistics()  { navigateTo("UserStatistics.fxml"); }
    @FXML public void goToApplyCoach()  { navigateTo("CoachApplication.fxml"); }
    @FXML public void goHome()          { MainApp.navigateTo(VIEWS + "home.fxml",      "Home"); }
    @FXML public void goToPlanning()    { MainApp.navigateTo(VIEWS + "Planning.fxml",  "Planning"); }
    @FXML public void goToTeams()       { MainApp.navigateTo(VIEWS + "Team.fxml",      "Teams"); }
    @FXML public void goToVideos()      { MainApp.navigateTo(VIEWS + "Videos.fxml",    "Videos"); }
    @FXML public void goToClips()       { MainApp.navigateTo(VIEWS + "Clips.fxml",     "Clips"); }
    @FXML public void goToGuides()      { MainApp.navigateTo(VIEWS + "Guides.fxml",    "Guides"); }
    @FXML public void goToCoins()       { MainApp.navigateTo(VIEWS + "Coins.fxml",     "Coins"); }
    @FXML public void goToSupport()     { MainApp.navigateTo(VIEWS + "Support.fxml",   "Support"); }
    @FXML public void goToAdmin()       { MainApp.navigateTo(VIEWS + "dashboard.fxml", "Admin"); }
    @FXML public void goTo2FA()         { navigateTo("TwoFactorSettings.fxml"); }
    @FXML public void handleLogout() {
        SessionManager.logout();
        navigateTo("login.fxml");
    }
    @FXML public void openAiAvatarDialog() { System.out.println("[Profile] AI Avatar — à implémenter"); }
    @FXML public void openCycleGANDialog() { System.out.println("[Profile] CycleGAN — à implémenter"); }

    // ─────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────
    private void loadAvatar(User user, ImageView imgView, StackPane placeholder,
                            Label initial, double radius) {
        if (user.getProfilePicture() != null && !user.getProfilePicture().isBlank()
                && imgView != null) {
            try {
                File f = new File(System.getProperty("user.dir"),
                        "uploads/profiles/" + user.getProfilePicture());
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), radius*2, radius*2, false, true);
                    imgView.setImage(img);
                    imgView.setClip(new Circle(radius, radius, radius));
                    show(imgView,     true);
                    show(placeholder, false);
                    return;
                }
            } catch (Exception e) {
                System.err.println("[Profile] Avatar error: " + e.getMessage());
            }
        }
        if (initial != null && user.getUsername() != null && !user.getUsername().isEmpty())
            initial.setText(String.valueOf(user.getUsername().charAt(0)).toUpperCase());
        show(imgView,     false);
        show(placeholder, true);
    }

    protected boolean hasRole(User user, String role) {
        return user.getRolesJson() != null && user.getRolesJson().contains(role);
    }

    protected void setText(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "—");
    }

    protected void show(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }

    private void showFieldError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showSuccess(String msg) {
        if (flashSuccessBox != null && flashSuccessLabel != null) {
            flashSuccessLabel.setText(msg);
            show(flashSuccessBox, true);
        }
        show(flashErrorBox, false);
    }

    private void showError(String msg) {
        if (flashErrorBox != null && flashErrorLabel != null) {
            flashErrorLabel.setText(msg);
            show(flashErrorBox, true);
        }
    }

    private void hideFlash() {
        show(flashSuccessBox,    false);
        show(flashErrorBox,      false);
        show(emailErrorLabel,    false);
        show(fullNameErrorLabel, false);
        show(bioErrorLabel,      false);
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }

    protected void navigateTo(String fxml) {
        String[] paths = { VIEWS + fxml, "/com/eyetwin/view/" + fxml };
        try {
            java.net.URL url = null;
            for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
            if (url == null) {
                System.err.println("[UserProfileController] FXML introuvable : " + fxml);
                return;
            }
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null)
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[UserProfileController] Nav error: " + e.getMessage());
            if (e.getCause() != null) e.getCause().printStackTrace();
        }
    }

    protected Stage resolveStage() {
        javafx.scene.Node[] candidates = {
                usernameLabel, emailField, statTeamsCount, navUsername, saveBtn
        };
        for (javafx.scene.Node n : candidates)
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        return null;
    }
}