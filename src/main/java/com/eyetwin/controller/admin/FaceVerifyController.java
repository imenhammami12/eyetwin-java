package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.IUserService;
import com.eyetwin.services.UserServiceImpl;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * FaceVerifyController — Vérification faciale avant accès au dashboard admin.
 *
 * FIXES :
 *   1. Recharge l'user depuis DB à chaque vérification (badge toujours à jour)
 *   2. Seuil LBP relevé à 0.80 (LBP est moins précis que face-api.js)
 *   3. Prend la MEILLEURE distance sur 3 captures consécutives
 */
public class FaceVerifyController {

    @FXML private ImageView cameraPreview;
    @FXML private VBox      videoContainer;
    @FXML private VBox      statusBox;
    @FXML private Label     statusLabel;
    @FXML private Button    verifyBtn;
    @FXML private Button    cancelBtn;

    private VideoCapture      camera;
    private CascadeClassifier faceDetector;
    private Timer             previewTimer;
    private Timer             detectionTimer;
    private volatile boolean  cameraRunning = false;

    private final IUserService userService = new UserServiceImpl();

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if (verifyBtn != null) verifyBtn.setDisable(true);
        updateStatus("info", "⏳  Loading face detection...");

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                nu.pattern.OpenCV.loadLocally();
                String cascadePath = extractCascadeToTemp();
                faceDetector = new CascadeClassifier(cascadePath);
                if (faceDetector.empty())
                    throw new RuntimeException("haarcascade_frontalface_default.xml not found.");
                camera = new VideoCapture(0);
                camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  640);
                camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
                if (!camera.isOpened())
                    throw new RuntimeException("Cannot open webcam.");
                cameraRunning = true;
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (videoContainer != null) videoContainer.setVisible(true);
                    updateStatus("success", "✅  Camera active. Position your face and click Verify.");
                    if (verifyBtn != null) verifyBtn.setDisable(false);
                    startPreview();
                    startDetection();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                        updateStatus("danger", "❌  " + getException().getMessage())
                );
            }
        };
        new Thread(initTask, "FaceVerify-Init").start();
    }

    // ════════════════════════════════════════════════════════════
    //  PREVIEW
    // ════════════════════════════════════════════════════════════
    private void startPreview() {
        previewTimer = new Timer("FaceVerify-Preview", true);
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

    // ════════════════════════════════════════════════════════════
    //  DETECTION TEMPS RÉEL
    // ════════════════════════════════════════════════════════════
    private void startDetection() {
        detectionTimer = new Timer("FaceVerify-Detection", true);
        detectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (!cameraRunning || camera == null || faceDetector == null) { cancel(); return; }
                Mat frame = new Mat();
                if (camera.read(frame) && !frame.empty()) {
                    int count = countFaces(frame);
                    frame.release();
                    Platform.runLater(() -> {
                        if      (count == 1) updateStatus("success", "✅  Face detected — click Verify.");
                        else if (count > 1)  updateStatus("warning", "⚠️  Multiple faces — only one allowed.");
                        else                 updateStatus("info",    "🔍  Searching for your face...");
                    });
                }
            }
        }, 0, 100);
    }

    private void stopDetection() {
        if (detectionTimer != null) { detectionTimer.cancel(); detectionTimer = null; }
    }

    // ════════════════════════════════════════════════════════════
    //  VERIFY — FIX : 3 captures, meilleure distance, seuil 0.80
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleVerify() {
        if (verifyBtn != null) verifyBtn.setDisable(true);
        stopDetection();
        updateStatus("info", "🔬  Analysing your face (3 captures)...");

        Task<User> verifyTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // ── FIX 1 : Recharge l'user depuis DB (pas depuis la mémoire) ──
                User sessionUser = SessionManager.getCurrentUser();
                if (sessionUser == null) throw new RuntimeException("No user in session.");

                User freshUser = userService.findByEmail(sessionUser.getEmail());
                if (freshUser == null) throw new RuntimeException("User not found in DB.");

                String storedJson = freshUser.getFaceDescriptor();
                if (storedJson == null || storedJson.isBlank())
                    throw new RuntimeException("No face registered for this account.");

                float[] storedDescriptor = jsonToFloatArray(storedJson);
                if (storedDescriptor.length == 0)
                    throw new RuntimeException("Stored face descriptor is empty.");

                System.out.println("[FaceVerify] Stored descriptor length = " + storedDescriptor.length);

                // ── FIX 2 : 3 captures + prend la meilleure distance ──
                double bestDistance = Double.MAX_VALUE;

                for (int attempt = 0; attempt < 3; attempt++) {
                    Thread.sleep(200);

                    Mat frame = new Mat();
                    if (camera == null || !camera.read(frame) || frame.empty()) continue;

                    MatOfRect faces = new MatOfRect();
                    Mat       gray  = toGray(frame);
                    faceDetector.detectMultiScale(gray, faces, 1.1, 4);
                    Rect[] rects = faces.toArray();
                    frame.release();

                    if (rects.length != 1) {
                        gray.release();
                        continue;
                    }

                    Mat faceMat = new Mat(gray, rects[0]);
                    Imgproc.resize(faceMat, faceMat, new Size(100, 100));
                    float[] captured = extractLBPDescriptor(faceMat);
                    gray.release();

                    double dist = euclideanDistance(captured, storedDescriptor);
                    System.out.printf("[FaceVerify] Attempt %d — distance = %.4f%n", attempt + 1, dist);

                    if (dist < bestDistance) bestDistance = dist;
                }

                // ── FIX 3 : Seuil relevé à 0.80 (LBP est moins discriminant que face-api.js) ──
                double THRESHOLD = 0.80;
                System.out.printf("[FaceVerify] Best distance = %.4f | Threshold = %.2f | Match = %b%n",
                        bestDistance, THRESHOLD, bestDistance < THRESHOLD);

                if (bestDistance == Double.MAX_VALUE)
                    throw new RuntimeException("No face detected in 3 attempts. Please try again.");

                return bestDistance < THRESHOLD ? freshUser : null;
            }

            @Override
            protected void succeeded() {
                User matched = getValue();
                Platform.runLater(() -> {
                    if (matched != null) {
                        updateStatus("success", "✅  Identity confirmed! Opening dashboard...");
                        stopCamera();
                        // ── FIX 4 : Met à jour la session avec l'user rechargé depuis DB ──
                        SessionManager.setCurrentUser(matched);
                        new Thread(() -> {
                            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> navigateTo("Admin.fxml"));
                        }).start();
                    } else {
                        updateStatus("danger", "❌  Face not recognised. Please try again.");
                        if (verifyBtn != null) verifyBtn.setDisable(false);
                        startDetection();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("danger", "❌  " + getException().getMessage());
                    if (verifyBtn != null) verifyBtn.setDisable(false);
                    startDetection();
                });
            }
        };
        new Thread(verifyTask, "FaceVerify-Verify").start();
    }

    // ════════════════════════════════════════════════════════════
    //  CANCEL
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleCancel() {
        stopCamera();
        navigateTo("home.fxml");
    }

    // ════════════════════════════════════════════════════════════
    //  OPENCV HELPERS
    // ════════════════════════════════════════════════════════════
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
        int   rows = grayFace.rows(), cols = grayFace.cols();
        int[] dx   = {-1,-1, 0, 1, 1, 1, 0,-1};
        int[] dy   = { 0, 1, 1, 1, 0,-1,-1,-1};
        for (int r = 1; r < rows-1; r++) {
            for (int c = 1; c < cols-1; c++) {
                double center = grayFace.get(r, c)[0];
                int code = 0;
                for (int k = 0; k < 8; k++)
                    if (grayFace.get(r+dy[k], c+dx[k])[0] >= center) code |= (1 << k);
                hist[code]++;
            }
        }
        float[] desc = new float[128];
        for (int i = 0; i < 128; i++) desc[i] = hist[i] + hist[i+128];
        float sum = 0;
        for (float v : desc) sum += v*v;
        sum = (float) Math.sqrt(sum);
        if (sum > 0) for (int i = 0; i < 128; i++) desc[i] /= sum;
        return desc;
    }

    private double euclideanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0;
        for (int i = 0; i < a.length; i++) { double d = a[i] - b[i]; sum += d * d; }
        return Math.sqrt(sum);
    }

    private float[] jsonToFloatArray(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new float[0];
        String cleaned = json.trim().replaceAll("[\\[\\]]", "");
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { result[i] = Float.parseFloat(parts[i].trim()); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private Image matToImage(Mat mat) {
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, buf);
        return new Image(new ByteArrayInputStream(buf.toArray()));
    }

    private String extractCascadeToTemp() throws Exception {
        String[] resources = {
                "/haarcascades/haarcascade_frontalface_default.xml",
                "/org/opencv/data/haarcascades/haarcascade_frontalface_default.xml"
        };
        for (String res : resources) {
            var stream = getClass().getResourceAsStream(res);
            if (stream != null) {
                File tmp = File.createTempFile("haar_", ".xml");
                tmp.deleteOnExit();
                try (var out = new FileOutputStream(tmp)) { stream.transferTo(out); }
                return tmp.getAbsolutePath();
            }
        }
        throw new RuntimeException("haarcascade_frontalface_default.xml not found.");
    }

    // ════════════════════════════════════════════════════════════
    //  STOP CAMERA
    // ════════════════════════════════════════════════════════════
    private void stopCamera() {
        synchronized (this) {
            cameraRunning = false;
            stopDetection();
            if (previewTimer != null) {
                previewTimer.cancel();
                previewTimer = null;
            }
            if (camera != null) {
                if (camera.isOpened()) {
                    camera.release();
                    System.out.println("[FaceVerifyController] Camera released.");
                }
                camera = null;
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  STATUS
    // ════════════════════════════════════════════════════════════
    private void updateStatus(String type, String message) {
        if (statusLabel == null || statusBox == null) return;
        statusLabel.setText(message);
        String bg, border, fg;
        switch (type) {
            case "success" -> { bg="rgba(67,233,123,0.15)";  border="rgba(67,233,123,0.3)";  fg="#43e97b"; }
            case "danger"  -> { bg="rgba(255,60,100,0.15)";  border="rgba(255,60,100,0.3)";  fg="#ff3c64"; }
            case "warning" -> { bg="rgba(255,193,7,0.15)";   border="rgba(255,193,7,0.3)";   fg="#ffc107"; }
            default        -> { bg="rgba(90,103,216,0.15)";  border="rgba(90,103,216,0.3)";  fg="#818cf8"; }
        }
        statusBox.setStyle(
                "-fx-background-color:"+bg+";-fx-border-color:"+border+";" +
                        "-fx-border-radius:12;-fx-background-radius:12;-fx-border-width:1;-fx-padding:14 18 14 18;");
        statusLabel.setStyle("-fx-text-fill:"+fg+";-fx-font-size:13;");
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════
    private void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        URL url = null;
        for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
        if (url == null) { System.err.println("[FaceVerifyController] FXML not found: " + fxml); return; }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = resolveStage();
            if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException e) {
            System.err.println("[FaceVerifyController] Error: " + e.getMessage());
        }
    }

    private Stage resolveStage() {
        for (javafx.scene.Node n : new javafx.scene.Node[]{ verifyBtn, cancelBtn, statusBox }) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }
}
