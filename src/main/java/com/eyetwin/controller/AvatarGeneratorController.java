package com.eyetwin.controller;

import com.eyetwin.config.ConfigLoader;
import com.eyetwin.entities.User;
import com.eyetwin.services.UserServiceImpl;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

public class AvatarGeneratorController {

    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> styleCombo;
    @FXML private Button generateBtn;
    @FXML private Button applyBtn;
    @FXML private ImageView resultImageView;
    @FXML private Label statusLabel;
    @FXML private VBox loadingBox;
    @FXML private VBox resultBox;

    private static final String HF_TOKEN = ConfigLoader.get("HF_API_KEY");
    private static final String HF_URL   =
            "https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell";

    private byte[] generatedImageBytes;

    @FXML
    public void initialize() {
        styleCombo.getItems().addAll(
                "anime", "cartoon", "manga", "pixel art", "fantasy", "realistic"
        );
        styleCombo.setValue("anime");

        if (loadingBox != null) { loadingBox.setVisible(false); loadingBox.setManaged(false); }
        if (resultBox  != null) { resultBox.setVisible(false);  resultBox.setManaged(false);  }
        if (applyBtn   != null) applyBtn.setDisable(true);
    }

    @FXML
    private void handleGenerate() {
        String desc = descriptionField.getText().trim();
        if (desc.length() < 3) {
            statusLabel.setText("Please enter a description (min 3 characters).");
            return;
        }

        String style = styleCombo.getValue();
        String prompt = buildPrompt(desc, style);

        setLoading(true);
        statusLabel.setText("Connecting to HuggingFace FLUX.1...");

        new Thread(() -> {
            try {
                byte[] imageBytes = callHuggingFace(prompt);
                Platform.runLater(() -> {
                    generatedImageBytes = imageBytes;
                    Image img = new Image(new ByteArrayInputStream(imageBytes));
                    resultImageView.setImage(img);
                    setLoading(false);
                    showResult(true);
                    applyBtn.setDisable(false);
                    statusLabel.setText("✔ Avatar generated!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    statusLabel.setText("❌ Error: " + e.getMessage());
                    System.err.println("[Avatar] Error: " + e.getMessage());
                });
            }
        }, "avatar-gen").start();
    }

    @FXML
    private void handleApply() {
        if (generatedImageBytes == null) return;
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return;

            // Sauvegarde le fichier localement
            String filename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "profiles");
            Files.createDirectories(uploadsDir);
            Path filePath = uploadsDir.resolve(filename);
            Files.write(filePath, generatedImageBytes);

            // Met à jour la base de données
            UserServiceImpl userService = new UserServiceImpl();
            userService.saveProfilePicture(user.getId(), generatedImageBytes, filename);
            user.setProfilePicture(filename);
            SessionManager.refresh();

            statusLabel.setText("✔ Avatar applied to your profile!");
            statusLabel.setStyle("-fx-text-fill: #00e676;");

            // Ferme le dialogue après 1.5s
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

    private byte[] callHuggingFace(String prompt) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        String body = "{\"inputs\": \"" + prompt.replace("\"", "'") + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_URL))
                .header("Authorization", "Bearer " + HF_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<byte[]> response = client.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 503) {
            // Model loading — retry after 20s
            Platform.runLater(() -> statusLabel.setText("⏳ Model warming up, retrying in 20s..."));
            Thread.sleep(20000);
            return callHuggingFace(prompt);
        }

        if (response.statusCode() != 200) {
            throw new Exception("HuggingFace returned " + response.statusCode()
                    + ": " + new String(response.body()));
        }

        return response.body();
    }

    private String buildPrompt(String desc, String style) {
        return switch (style) {
            case "anime"    -> "anime style portrait of " + desc + ", Studio Ghibli, detailed, colorful";
            case "cartoon"  -> "cartoon style portrait of " + desc + ", Pixar 3D, vibrant colors";
            case "manga"    -> "manga style portrait of " + desc + ", black and white ink, detailed";
            case "pixel art" -> "pixel art portrait of " + desc + ", 16-bit retro RPG style";
            case "fantasy"  -> "fantasy portrait of " + desc + ", epic, magical, detailed artwork";
            case "realistic" -> "photorealistic portrait of " + desc + ", professional photo quality";
            default         -> style + " portrait of " + desc;
        };
    }

    private void setLoading(boolean loading) {
        if (loadingBox != null) { loadingBox.setVisible(loading); loadingBox.setManaged(loading); }
        generateBtn.setDisable(loading);
        generateBtn.setText(loading ? "Generating..." : "Generate Avatar");
    }

    private void showResult(boolean show) {
        if (resultBox != null) { resultBox.setVisible(show); resultBox.setManaged(show); }
    }
}