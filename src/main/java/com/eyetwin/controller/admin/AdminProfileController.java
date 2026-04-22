package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.DatabaseConfig;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Timer;
import java.util.TimerTask;

public class AdminProfileController {

    @FXML private Button    navAuditLogs;
    @FXML private Label     pageTitle;
    @FXML private Label     usernameLabel;
    @FXML private Label     userAvatarInitial;
    @FXML private Label     avatarInitialLarge;
    @FXML private Label     fullNameLabel;
    @FXML private Label     emailLabel;
    @FXML private Label     usernameDisplayLabel;
    @FXML private Label     memberSinceLabel;
    @FXML private VBox      generalTab;
    @FXML private VBox      securityTab;
    @FXML private Button    tabGeneralBtn;
    @FXML private Button    tabSecurityBtn;
    @FXML private Label     faceAuthStatusBadge;
    @FXML private Button    faceAuthActionBtn;
    @FXML private VBox      cameraPanel;
    @FXML private VBox      cameraStatusBox;
    @FXML private Label     cameraStatusLabel;
    @FXML private ImageView cameraPreview;
    @FXML private VBox      cameraVideoBox;
    @FXML private Button    captureBtn;
    @FXML private Button    cameraCancelBtn;

    private final IUserService userService = new UserServiceImpl();
    private VideoCapture      camera;
    private CascadeClassifier faceDetector;
    private Timer             previewTimer;
    private Timer             detectionTimer;
    private volatile boolean  cameraRunning = false;

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE — recharge toujours depuis DB
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {


        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }

        // Recharge depuis DB pour avoir face_descriptor à jour
        User sessionUser = SessionManager.getCurrentUser();
        if (sessionUser != null) {
            User freshUser = userService.findByEmail(sessionUser.getEmail());
            if (freshUser != null) {
                SessionManager.setCurrentUser(freshUser);
                sessionUser = freshUser;
                System.out.println("[AdminProfile] User rechargé. face_descriptor = "
                        + (freshUser.getFaceDescriptor() != null ? "SET ✅" : "NULL ❌"));
            }
        }

        User user = SessionManager.getCurrentUser();
        setupTopBar(user);
        setupSidebar();
        setupProfileHeader(user);
        setupSecurityTab(user);
        hideCameraPanel();

        boolean openSecurity = SessionManager.consumeOpenSecurityTab();
        if (openSecurity) showSecurityTab(); else showGeneralTab();

        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlashAlert(flash[0], flash[1]);
    }

    private void setupTopBar(User user) {
        if (pageTitle != null) pageTitle.setText("My Profile");
        if (user != null) {
            String u = user.getUsername() != null ? user.getUsername() : "Admin";
            if (usernameLabel     != null) usernameLabel.setText(u);
            if (userAvatarInitial != null)
                userAvatarInitial.setText(String.valueOf(u.charAt(0)).toUpperCase());
        }
    }

    private void setupSidebar() {
        if (navAuditLogs != null) {
            navAuditLogs.setVisible(SessionManager.isSuperAdmin());
            navAuditLogs.setManaged(SessionManager.isSuperAdmin());
        }
    }

    private void setupProfileHeader(User user) {
        if (user == null) return;
        String username = user.getUsername() != null ? user.getUsername() : "?";
        String initials = username.length() >= 2
                ? username.substring(0, 2).toUpperCase()
                : username.substring(0, 1).toUpperCase();
        if (avatarInitialLarge   != null) avatarInitialLarge.setText(initials);
        if (fullNameLabel        != null) fullNameLabel.setText(
                user.getFullName() != null ? user.getFullName() : username);
        if (emailLabel           != null) emailLabel.setText("✉  " + user.getEmail());
        if (usernameDisplayLabel != null) usernameDisplayLabel.setText("@  " + username);
        if (memberSinceLabel     != null) memberSinceLabel.setText(
                "📅  Member since " + (user.getCreatedAt() != null
                        ? user.getCreatedAt().toLocalDate() : "—"));
    }

    // ════════════════════════════════════════════════════════════
    //  SECURITY TAB
    // ════════════════════════════════════════════════════════════
    private void setupSecurityTab(User user) {
        if (user == null) return;
        boolean hasFace = user.getFaceDescriptor() != null && !user.getFaceDescriptor().isBlank();

        if (faceAuthStatusBadge != null) {
            faceAuthStatusBadge.setText(hasFace ? "✅  Enabled" : "❌  Disabled");
            faceAuthStatusBadge.setStyle(hasFace
                    ? "-fx-background-color:rgba(67,233,123,0.2);-fx-text-fill:#43e97b;-fx-background-radius:6;-fx-padding:4 10 4 10;-fx-font-size:11;-fx-font-weight:bold;"
                    : "-fx-background-color:rgba(255,60,100,0.2);-fx-text-fill:#ff3c64;-fx-background-radius:6;-fx-padding:4 10 4 10;-fx-font-size:11;-fx-font-weight:bold;");
        }

        if (faceAuthActionBtn != null) {
            if (hasFace) {
                faceAuthActionBtn.setText("🚫  Disable");
                faceAuthActionBtn.setStyle(
                        "-fx-background-color:rgba(255,255,255,0.08);-fx-border-color:rgba(255,255,255,0.2);" +
                                "-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1;" +
                                "-fx-text-fill:rgba(255,255,255,0.8);-fx-font-size:12;-fx-cursor:hand;-fx-padding:10 18 10 18;");
                faceAuthActionBtn.setOnAction(e -> handleDisableFace());
            } else {
                faceAuthActionBtn.setText("📷  Enable");
                faceAuthActionBtn.setStyle(
                        "-fx-background-color:linear-gradient(135deg,#ff3c64,#ff1744);" +
                                "-fx-background-radius:8;-fx-border-color:transparent;" +
                                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12;-fx-cursor:hand;-fx-padding:10 18 10 18;");
                faceAuthActionBtn.setOnAction(e -> handleEnableFace());
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DISABLE — SQL direct face_descriptor = NULL
    // ════════════════════════════════════════════════════════════
    private void handleDisableFace() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to disable facial recognition?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Disable Face Recognition");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            User user = SessionManager.getCurrentUser();
            if (user == null) return;

            if (faceAuthActionBtn != null) {
                faceAuthActionBtn.setDisable(true);
                faceAuthActionBtn.setText("Saving...");
            }

            Task<Void> task = new Task<>() {
                @Override protected Void call() {
                    updateFaceDescriptorInDB(user.getId(), null);
                    user.setFaceDescriptor(null);
                    user.setFaceImage(null);
                    return null;
                }
                @Override protected void succeeded() {
                    Platform.runLater(() -> {
                        SessionManager.setCurrentUser(user);
                        SessionManager.setPendingFlash("success", "Facial recognition has been disabled.");
                        SessionManager.setOpenSecurityTab(true);
                        navigateTo("AdminProfile.fxml");
                    });
                }
                @Override protected void failed() {
                    Platform.runLater(() -> {
                        showFlashAlert("error", "Failed: " + getException().getMessage());
                        if (faceAuthActionBtn != null) faceAuthActionBtn.setDisable(false);
                        setupSecurityTab(user);
                    });
                }
            };
            new Thread(task, "DisableFace").start();
        });
    }

    // ════════════════════════════════════════════════════════════
    //  ENABLE
    // ════════════════════════════════════════════════════════════
    private void handleEnableFace() {
        showSecurityTab();
        showCameraPanel();
        startFaceCapture();
    }

    private void startFaceCapture() {
        if (captureBtn != null) captureBtn.setDisable(true);
        updateCameraStatus("info", "⏳  Initializing camera...");

        Task<Void> initTask = new Task<>() {
            @Override protected Void call() throws Exception {
                nu.pattern.OpenCV.loadLocally();
                String cascadePath = extractCascadeToTemp();
                faceDetector = new CascadeClassifier(cascadePath);
                if (faceDetector.empty())
                    throw new RuntimeException("haarcascade_frontalface_default.xml not found.");
                camera = new VideoCapture(0);
                camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
                camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
                if (!camera.isOpened()) throw new RuntimeException("Cannot open webcam.");
                cameraRunning = true;
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    if (cameraVideoBox != null) { cameraVideoBox.setVisible(true); cameraVideoBox.setManaged(true); }
                    updateCameraStatus("success", "✅  Camera active. Position your face.");
                    if (captureBtn != null) captureBtn.setDisable(false);
                    startCameraPreview();
                    startFaceDetection();
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> updateCameraStatus("danger", "❌  " + getException().getMessage()));
            }
        };
        new Thread(initTask, "FaceEnable-Init").start();
    }

    private void startCameraPreview() {
        previewTimer = new Timer("FaceEnable-Preview", true);
        previewTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!cameraRunning || camera == null) { cancel(); return; }
                Mat frame = new Mat();
                if (camera.read(frame) && !frame.empty()) {
                    Image img = matToImage(frame);
                    Platform.runLater(() -> { if (cameraPreview != null) cameraPreview.setImage(img); });
                    frame.release();
                }
            }
        }, 0, 33);
    }

    private void startFaceDetection() {
        if (detectionTimer != null) detectionTimer.cancel();
        detectionTimer = new Timer("FaceEnable-Detection", true);
        detectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!cameraRunning || camera == null || faceDetector == null) { cancel(); return; }
                Mat frame = new Mat();
                if (camera.read(frame) && !frame.empty()) {
                    int count = countFaces(frame);
                    frame.release();
                    Platform.runLater(() -> {
                        if      (count == 1) updateCameraStatus("success", "✅  Face detected! Click Capture.");
                        else if (count > 1)  updateCameraStatus("warning", "⚠️  Multiple faces.");
                        else                 updateCameraStatus("info",    "🔍  Searching for your face...");
                    });
                }
            }
        }, 0, 100);
    }

    private void stopDetection() {
        if (detectionTimer != null) { detectionTimer.cancel(); detectionTimer = null; }
    }

    // ════════════════════════════════════════════════════════════
    //  CAPTURE — SQL direct, pas userService.update()
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleCapture() {
        if (captureBtn != null) captureBtn.setDisable(true);
        stopDetection();
        updateCameraStatus("info", "🔬  Analysing face...");

        Task<Void> captureTask = new Task<>() {
            @Override protected Void call() throws Exception {
                Thread.sleep(300);
                Mat frame = new Mat();
                if (camera == null || !camera.read(frame) || frame.empty())
                    throw new RuntimeException("Could not capture frame.");

                MatOfRect faces = new MatOfRect();
                Mat       gray  = toGray(frame);
                faceDetector.detectMultiScale(gray, faces, 1.1, 4);
                Rect[] rects = faces.toArray();
                frame.release();

                if (rects.length == 0) throw new RuntimeException("No face detected. Try again.");
                if (rects.length > 1)  throw new RuntimeException("Multiple faces. Only one allowed.");

                Mat faceMat = new Mat(gray, rects[0]);
                Imgproc.resize(faceMat, faceMat, new Size(100, 100));
                float[] descriptor     = extractLBPDescriptor(faceMat);
                String  descriptorJson = floatArrayToJson(descriptor);
                gray.release();

                System.out.println("[AdminProfile] Descriptor: " + descriptorJson.length() + " chars");

                User user = SessionManager.getCurrentUser();

                // UPDATE SQL direct — uniquement face_descriptor
                updateFaceDescriptorInDB(user.getId(), descriptorJson);

                // Vérifie en DB
                User freshUser = userService.findByEmail(user.getEmail());
                if (freshUser == null || freshUser.getFaceDescriptor() == null)
                    throw new RuntimeException("Save failed — face_descriptor is null in DB!");

                System.out.println("[AdminProfile] ✅ Saved in DB. length = "
                        + freshUser.getFaceDescriptor().length());
                SessionManager.setCurrentUser(freshUser);
                return null;
            }

            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    updateCameraStatus("success", "✅  Face registered successfully!");
                    stopCamera();
                    new Thread(() -> {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            SessionManager.setPendingFlash("success", "Facial recognition has been enabled.");
                            SessionManager.setOpenSecurityTab(true);
                            navigateTo("AdminProfile.fxml");
                        });
                    }).start();
                });
            }

            @Override protected void failed() {
                Platform.runLater(() -> {
                    updateCameraStatus("danger", "❌  " + getException().getMessage());
                    if (captureBtn != null) captureBtn.setDisable(false);
                    startFaceDetection();
                });
            }
        };
        new Thread(captureTask, "FaceEnable-Capture").start();
    }

    // ════════════════════════════════════════════════════════════
    //  SQL DIRECT — face_descriptor uniquement
    // ════════════════════════════════════════════════════════════
    private void updateFaceDescriptorInDB(int userId, String descriptorJson) {
        String sql = "UPDATE `user` SET face_descriptor = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (descriptorJson != null) stmt.setString(1, descriptorJson);
            else stmt.setNull(1, Types.VARCHAR);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            System.out.println("[AdminProfile] face_descriptor UPDATE — " + rows + " row(s) | "
                    + (descriptorJson != null ? "SET ✅" : "NULL ❌"));
            if (rows == 0) throw new RuntimeException("0 rows updated! userId=" + userId);
        } catch (SQLException e) {
            System.err.println("❌ updateFaceDescriptorInDB: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DB error: " + e.getMessage());
        }
    }

    @FXML public void handleCameraCancel() {
        stopCamera(); hideCameraPanel();
        User user = SessionManager.getCurrentUser();
        if (user != null) setupSecurityTab(user);
    }

    private void showCameraPanel() {
        if (cameraPanel    != null) { cameraPanel.setVisible(true);    cameraPanel.setManaged(true);    }
        if (cameraVideoBox != null) { cameraVideoBox.setVisible(false); cameraVideoBox.setManaged(false);}
        if (faceAuthActionBtn != null) faceAuthActionBtn.setDisable(true);
    }

    private void hideCameraPanel() {
        if (cameraPanel != null) { cameraPanel.setVisible(false); cameraPanel.setManaged(false); }
        if (faceAuthActionBtn != null) faceAuthActionBtn.setDisable(false);
    }

    private void stopCamera() {
        cameraRunning = false; stopDetection();
        if (previewTimer != null) { previewTimer.cancel(); previewTimer = null; }
        if (camera != null && camera.isOpened()) { camera.release(); camera = null; }
    }

    private int countFaces(Mat frame) {
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(toGray(frame), faces, 1.1, 4);
        return faces.toArray().length;
    }

    private Mat toGray(Mat src) {
        Mat gray = new Mat();
        if (src.channels() > 1) Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        else src.copyTo(gray);
        return gray;
    }

    private float[] extractLBPDescriptor(Mat grayFace) {
        int[] hist = new int[256];
        int rows = grayFace.rows(), cols = grayFace.cols();
        int[] dx = {-1,-1, 0, 1, 1, 1, 0,-1}, dy = { 0, 1, 1, 1, 0,-1,-1,-1};
        for (int r = 1; r < rows-1; r++)
            for (int c = 1; c < cols-1; c++) {
                double center = grayFace.get(r, c)[0]; int code = 0;
                for (int k = 0; k < 8; k++)
                    if (grayFace.get(r+dy[k], c+dx[k])[0] >= center) code |= (1 << k);
                hist[code]++;
            }
        float[] desc = new float[128];
        for (int i = 0; i < 128; i++) desc[i] = hist[i] + hist[i+128];
        float sum = 0; for (float v : desc) sum += v*v; sum = (float)Math.sqrt(sum);
        if (sum > 0) for (int i = 0; i < 128; i++) desc[i] /= sum;
        return desc;
    }

    private String floatArrayToJson(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) { sb.append(arr[i]); if (i < arr.length-1) sb.append(","); }
        return sb.append("]").toString();
    }

    private Image matToImage(Mat mat) {
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, buf);
        return new Image(new java.io.ByteArrayInputStream(buf.toArray()));
    }

    private String extractCascadeToTemp() throws Exception {
        String[] resources = {
                "/haarcascades/haarcascade_frontalface_default.xml",
                "/org/opencv/data/haarcascades/haarcascade_frontalface_default.xml"
        };
        for (String res : resources) {
            var stream = getClass().getResourceAsStream(res);
            if (stream != null) {
                File tmp = File.createTempFile("haar_", ".xml"); tmp.deleteOnExit();
                try (var out = new FileOutputStream(tmp)) { stream.transferTo(out); }
                return tmp.getAbsolutePath();
            }
        }
        throw new RuntimeException("haarcascade_frontalface_default.xml not found.");
    }

    private void updateCameraStatus(String type, String message) {
        if (cameraStatusLabel == null || cameraStatusBox == null) return;
        cameraStatusLabel.setText(message);
        String bg, border, fg;
        switch (type) {
            case "success" -> { bg="rgba(67,233,123,0.15)"; border="rgba(67,233,123,0.3)"; fg="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.15)"; border="rgba(255,60,100,0.3)"; fg="#ff3c64"; }
            case "warning" -> { bg="rgba(255,193,7,0.15)";  border="rgba(255,193,7,0.3)";  fg="#ffc107"; }
            default        -> { bg="rgba(90,103,216,0.15)"; border="rgba(90,103,216,0.3)"; fg="#818cf8"; }
        }
        cameraStatusBox.setStyle("-fx-background-color:"+bg+";-fx-border-color:"+border+
                ";-fx-border-radius:10;-fx-background-radius:10;-fx-border-width:1;-fx-padding:12 16 12 16;");
        cameraStatusLabel.setStyle("-fx-text-fill:"+fg+";-fx-font-size:13;");
    }

    private void showFlashAlert(String type, String message) {
        Alert.AlertType t = type.equals("success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert a = new Alert(t, message, ButtonType.OK);
        a.setTitle(type.equals("success") ? "Success" : "Error");
        a.setHeaderText(null); a.show();
    }

    @FXML public void showGeneralTab() {
        if (generalTab  != null) { generalTab.setVisible(true);  generalTab.setManaged(true);  }
        if (securityTab != null) { securityTab.setVisible(false); securityTab.setManaged(false); }
        hideCameraPanel(); stopCamera();
        setTabActive(tabGeneralBtn, true); setTabActive(tabSecurityBtn, false);
    }

    @FXML public void showSecurityTab() {
        if (generalTab  != null) { generalTab.setVisible(false); generalTab.setManaged(false); }
        if (securityTab != null) { securityTab.setVisible(true);  securityTab.setManaged(true);  }
        setTabActive(tabGeneralBtn, false); setTabActive(tabSecurityBtn, true);
    }

    private void setTabActive(Button btn, boolean active) {
        if (btn == null) return;
        btn.setStyle(active
                ? "-fx-background-color:rgba(255,60,100,0.12);-fx-border-color:rgba(255,60,100,0.4) rgba(255,60,100,0.4) transparent rgba(255,60,100,0.4);-fx-border-width:1 1 0 1;-fx-border-radius:8 8 0 0;-fx-background-radius:8 8 0 0;-fx-text-fill:#ff3c64;-fx-font-weight:bold;-fx-font-size:12;-fx-padding:10 20 10 20;-fx-cursor:hand;"
                : "-fx-background-color:transparent;-fx-border-color:transparent;-fx-text-fill:rgba(255,255,255,0.5);-fx-font-size:12;-fx-padding:10 20 10 20;-fx-cursor:hand;");
    }

    @FXML public void goToDashboard()         { stopCamera(); navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { stopCamera(); navigateTo("AdminUsers.fxml"); }
    @FXML public void goToPlanning()          { stopCamera(); navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { stopCamera(); navigateTo("AdminTournois.fxml"); }
    @FXML public void goToVideos()            { stopCamera(); navigateTo("AdminVideos.fxml"); }
    @FXML public void goToCoachApplications() { stopCamera(); navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { stopCamera(); navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { stopCamera(); navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { stopCamera(); navigateTo("AdminMessages.fxml"); }
    @FXML public void goToTeams()             { stopCamera(); navigateTo("AdminTeams.fxml"); }
    @FXML public void goToSite()              { stopCamera(); navigateTo("home.fxml"); }
    @FXML public void goToProfile()           { stopCamera(); navigateTo("AdminProfile.fxml"); }
    @FXML public void goToFaceRegister() {
        stopCamera(); SessionManager.setOpenSecurityTab(true); navigateTo("AdminProfile.fxml");
    }
    @FXML public void goToAuditLogs() {
        if (!SessionManager.isSuperAdmin()) return;
        stopCamera(); navigateTo("AdminAuditLogs.fxml");
    }
    @FXML public void handleLogout() {
        stopCamera(); SessionManager.logout(); navigateTo("AdminLogin.fxml");
    }

    private void navigateTo(String fxml) {
        String[] paths = { "/com/eyetwin/views/"+fxml, "/com/eyetwin/view/"+fxml, "/com/eyetwin/"+fxml };
        URL url = null;
        for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
        if (url == null) { System.err.println("[AdminProfileController] FXML not found: " + fxml); return; }
        try {
            Parent root = FXMLLoader.load(url);
            Stage stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{
                pageTitle, usernameLabel, fullNameLabel, generalTab, securityTab }) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}