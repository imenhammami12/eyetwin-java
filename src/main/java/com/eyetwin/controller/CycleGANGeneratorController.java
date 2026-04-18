package com.eyetwin.controller;

import com.eyetwin.entities.User;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

public class CycleGANGeneratorController {

    @FXML private Button generateBtn;
    @FXML private Button applyBtn;
    @FXML private ImageView originalImageView;
    @FXML private ImageView resultImageView;
    @FXML private Label statusLabel;
    @FXML private Label fileLabel;
    @FXML private VBox loadingBox;
    @FXML private VBox resultBox;

    private static final String CYCLEGAN_URL =
            "https://dermographic-shelba-nonresisting.ngrok-free.dev/transform";

    private File selectedFile;
    private byte[] generatedImageBytes;

    @FXML
    public void initialize() {
        if (loadingBox != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
        if (resultBox  != null) { resultBox.setVisible(false);  resultBox.setManaged(false);  }
        if (applyBtn   != null) applyBtn.setDisable(true);
        if (generateBtn != null) generateBtn.setDisable(true);
    }

    @FXML
    private void handlePickFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select your photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File f = fc.showOpenDialog(generateBtn.getScene().getWindow());
        if (f != null) {
            selectedFile = f;
            fileLabel.setText("📎 " + f.getName());
            Image preview = new Image(f.toURI().toString(), 180, 180, true, true);
            originalImageView.setImage(preview);
            generateBtn.setDisable(false);
            statusLabel.setText("Photo selected — click Generate!");
        }
    }

    @FXML
    private void handleGenerate() {
        if (selectedFile == null) return;
        setLoading(true);
        statusLabel.setText("Sending to CycleGAN API...");

        new Thread(() -> {
            try {
                byte[] imageBytes = callCycleGAN(selectedFile);
                Platform.runLater(() -> {
                    generatedImageBytes = imageBytes;
                    Image img = new Image(new ByteArrayInputStream(imageBytes));
                    resultImageView.setImage(img);
                    setLoading(false);
                    showResult(true);
                    applyBtn.setDisable(false);
                    statusLabel.setText("✔ Cartoon avatar generated!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    statusLabel.setText("❌ Error: " + e.getMessage());
                    System.err.println("[CycleGAN] Error: " + e.getMessage());
                });
            }
        }, "cyclegan-gen").start();
    }

    @FXML
    private void handleApply() {
        if (generatedImageBytes == null) return;
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return;

            String filename = "cartoon_" + user.getId() + "_"
                    + System.currentTimeMillis() + ".png";
            Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "profiles");
            Files.createDirectories(uploadsDir);
            Files.write(uploadsDir.resolve(filename), generatedImageBytes);

            UserServiceImpl userService = new UserServiceImpl();
            userService.saveProfilePicture(user.getId(), generatedImageBytes, filename);
            user.setProfilePicture(filename);
            SessionManager.refresh();

            statusLabel.setText("✔ Cartoon avatar applied!");
            statusLabel.setStyle("-fx-text-fill: #00e676;");

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    Stage stage = (Stage) applyBtn.getScene().getWindow();
                    stage.close();
                });
            }).start();

        } catch (Exception e) {
            statusLabel.setText("❌ Could not save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) generateBtn.getScene().getWindow();
        stage.close();
    }

    private byte[] callCycleGAN(File imageFile) throws Exception {
        String boundary = "----FormBoundary" + System.currentTimeMillis();
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"), true);

        pw.append("--").append(boundary).append("\r\n");
        pw.append("Content-Disposition: form-data; name=\"image\"; filename=\"")
                .append(imageFile.getName()).append("\"").append("\r\n");
        pw.append("Content-Type: image/jpeg").append("\r\n\r\n");
        pw.flush();
        baos.write(fileBytes);
        pw.append("\r\n--").append(boundary).append("--\r\n");
        pw.flush();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CYCLEGAN_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("ngrok-skip-browser-warning", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = client.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200)
            throw new Exception("API returned " + response.statusCode());

        return response.body();
    }

    private void setLoading(boolean loading) {
        if (loadingBox != null) { loadingBox.setVisible(loading); loadingBox.setManaged(loading); }
        generateBtn.setDisable(loading);
        generateBtn.setText(loading ? "Generating..." : "🤖  Generate Cartoon");
    }

    private void showResult(boolean show) {
        if (resultBox != null) { resultBox.setVisible(show); resultBox.setManaged(show); }
    }
}