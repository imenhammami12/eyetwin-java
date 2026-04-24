package com.eyetwin.controller;

import com.eyetwin.entities.User;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

public class CycleGANGeneratorController {

    @FXML private TextField  apiUrlField;
    @FXML private Button     generateBtn;
    @FXML private Button     applyBtn;
    @FXML private ImageView  originalImageView;
    @FXML private ImageView  resultImageView;
    @FXML private Label      fileLabel;
    @FXML private Label      statusLabel;
    @FXML private VBox       loadingBox;
    @FXML private VBox       resultBox;

    // Update this when your Colab tunnel restarts
    private static final String DEFAULT_API_URL =
            "https://witty-humans-smoke.loca.lt/transform";

    private File   selectedFile;
    private byte[] resultBytes;

    @FXML
    public void initialize() {
        if (apiUrlField != null) apiUrlField.setText(DEFAULT_API_URL);
        if (loadingBox  != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
        if (resultBox   != null) { resultBox.setVisible(false);  resultBox.setManaged(false);  }
        if (applyBtn    != null) applyBtn.setDisable(true);
        if (generateBtn != null) generateBtn.setDisable(true);
    }

    @FXML
    private void handlePickFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = chooser.showOpenDialog((Stage) generateBtn.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileLabel.setText("📎 " + file.getName());
            generateBtn.setDisable(false);
            try {
                originalImageView.setImage(new Image(file.toURI().toString(), 160, 160, true, true));
            } catch (Exception e) {
                System.err.println("[CycleGAN] Preview error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerate() {
        if (selectedFile == null) return;

        String apiUrl = (apiUrlField != null && !apiUrlField.getText().isBlank())
                ? apiUrlField.getText().trim()
                : DEFAULT_API_URL;
        // Ensure endpoint is /transform
        if (!apiUrl.endsWith("/transform"))
            apiUrl = apiUrl.replaceAll("/?$", "/transform");

        final String finalUrl = apiUrl;
        setLoading(true);
        statusLabel.setText("Sending to CycleGAN API...");

        new Thread(() -> {
            try {
                byte[] bytes = callCycleGAN(selectedFile, finalUrl);
                Platform.runLater(() -> {
                    resultBytes = bytes;
                    resultImageView.setImage(new Image(new ByteArrayInputStream(bytes)));
                    setLoading(false);
                    showResult(true);
                    applyBtn.setDisable(false);
                    statusLabel.setText("✔ Cartoon generated!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    statusLabel.setText("❌ " + e.getMessage());
                    System.err.println("[CycleGAN] Error: " + e.getMessage());
                });
            }
        }, "cyclegan-gen").start();
    }

    @FXML
    private void handleApply() {
        if (resultBytes == null) return;
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return;

            String filename = "cartoon_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "profiles");
            Files.createDirectories(uploadsDir);
            Files.write(uploadsDir.resolve(filename), resultBytes);

            new UserServiceImpl().saveProfilePicture(user.getId(), resultBytes, filename);
            user.setProfilePicture(filename);
            SessionManager.refresh();

            statusLabel.setText("✔ Avatar applied!");
            statusLabel.setStyle("-fx-text-fill: #00e676;");

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> ((Stage) applyBtn.getScene().getWindow()).close());
            }).start();

        } catch (Exception e) {
            statusLabel.setText("❌ Could not save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) generateBtn.getScene().getWindow()).close();
    }

    // ─────────────────────────────────────────────────────────────────

    private byte[] callCycleGAN(File imageFile, String apiUrl) throws Exception {
        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        String mime      = getMimeType(imageFile.getName());
        String boundary  = "----JavaBoundary" + System.currentTimeMillis();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\""
                + imageFile.getName() + "\"\r\n"
                + "Content-Type: " + mime + "\r\n\r\n";
        body.write(partHeader.getBytes());
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());

        // HTTP/1.1 — avoids GOAWAY with localtunnel/ngrok HTTP/2
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                // ✅ Bypasses localtunnel's browser confirmation page
                .header("bypass-tunnel-reminder", "true")
                .header("ngrok-skip-browser-warning", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        System.err.println("[CycleGAN] HTTP=" + response.statusCode()
                + " | size=" + response.body().length + " bytes");

        if (response.statusCode() == 404) {
            throw new Exception("API offline or URL outdated.\n"
                    + "Start Colab again and paste the new URL in the field above.\n"
                    + "Used: " + apiUrl);
        }
        if (response.statusCode() != 200) {
            throw new Exception("API returned HTTP " + response.statusCode()
                    + ": " + new String(response.body(), 0, Math.min(200, response.body().length)));
        }
        return response.body();
    }

    private String getMimeType(String name) {
        String l = name.toLowerCase();
        if (l.endsWith(".png"))  return "image/png";
        if (l.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private void setLoading(boolean on) {
        if (loadingBox != null) { loadingBox.setVisible(on); loadingBox.setManaged(on); }
        generateBtn.setDisable(on);
        generateBtn.setText(on ? "Generating..." : "🤖  Generate Cartoon");
    }

    private void showResult(boolean show) {
        if (resultBox != null) { resultBox.setVisible(show); resultBox.setManaged(show); }
    }
}