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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class AvatarGeneratorController {

    // ── Tab: Describe ──
    @FXML private VBox        tabDescribeBox;
    @FXML private TextField   descriptionField;

    // ── Tab: Photo ──
    @FXML private VBox        tabPhotoBox;
    @FXML private Button      pickPhotoBtn;
    @FXML private Label       photoFileLabel;
    @FXML private ImageView   photoPreviewView;
    @FXML private Label       analyzedDescLabel;
    @FXML private VBox        analyzedBox;

    // ── Shared ──
    @FXML private Button      tabDescribeBtn;
    @FXML private Button      tabPhotoBtn2;
    @FXML private ComboBox<String> styleCombo;
    @FXML private Button      generateBtn;
    @FXML private Button      applyBtn;
    @FXML private ImageView   resultImageView;
    @FXML private Label       statusLabel;
    @FXML private VBox        loadingBox;
    @FXML private VBox        resultBox;

    private static final String HF_TOKEN = ConfigLoader.get("HF_API_KEY");

    // Vision model for image-to-text (Llama 3.2 Vision via HF router)
    private static final String VISION_URL =
            "https://router.huggingface.co/hf-inference/models/meta-llama/Llama-3.2-11B-Vision-Instruct/v1/chat/completions";

    private static final Map<String, String> STYLE_PROMPTS = Map.of(
            "anime",     "anime style portrait of [DESC], Studio Ghibli art style, detailed face, vibrant colors, soft shading, beautiful illustration, high quality",
            "cartoon",   "cartoon portrait of [DESC], Pixar 3D animation style, expressive face, clean bright colors, professional character design, high quality",
            "manga",     "manga portrait of [DESC], black and white ink drawing, detailed linework, shounen manga art style, professional illustration",
            "pixel art", "pixel art portrait of [DESC], 16-bit retro game character, colorful, detailed pixel art, RPG game sprite style",
            "fantasy",   "fantasy portrait of [DESC], epic digital painting, magical atmosphere, detailed face, professional concept art, vibrant dramatic colors",
            "realistic", "hyperrealistic portrait of [DESC], professional photography, sharp details, beautiful studio lighting, high quality, photorealistic"
    );

    private byte[] generatedImageBytes;
    private File   selectedPhotoFile;
    private String currentTab = "describe"; // "describe" or "photo"

    @FXML
    public void initialize() {
        styleCombo.getItems().addAll("anime", "cartoon", "manga", "pixel art", "fantasy", "realistic");
        styleCombo.setValue("anime");
        if (loadingBox   != null) { loadingBox.setVisible(false);   loadingBox.setManaged(false);   }
        if (resultBox    != null) { resultBox.setVisible(false);     resultBox.setManaged(false);    }
        if (applyBtn     != null) applyBtn.setDisable(true);
        if (analyzedBox  != null) { analyzedBox.setVisible(false);   analyzedBox.setManaged(false);  }
        if (tabPhotoBox  != null) { tabPhotoBox.setVisible(false);   tabPhotoBox.setManaged(false);  }
        if (tabDescribeBox != null) { tabDescribeBox.setVisible(true); tabDescribeBox.setManaged(true); }
        updateTabStyles();
    }

    // ── Tab switching ─────────────────────────────────────────────

    @FXML
    private void switchToDescribe() {
        currentTab = "describe";
        if (tabDescribeBox != null) { tabDescribeBox.setVisible(true);  tabDescribeBox.setManaged(true);  }
        if (tabPhotoBox    != null) { tabPhotoBox.setVisible(false);    tabPhotoBox.setManaged(false);    }
        updateTabStyles();
    }

    @FXML
    private void switchToPhoto() {
        currentTab = "photo";
        if (tabDescribeBox != null) { tabDescribeBox.setVisible(false); tabDescribeBox.setManaged(false); }
        if (tabPhotoBox    != null) { tabPhotoBox.setVisible(true);     tabPhotoBox.setManaged(true);     }
        updateTabStyles();
    }

    private void updateTabStyles() {
        String activeStyle   = "-fx-background-color: #6a5eea; -fx-text-fill: white; -fx-font-weight: 900; "
                + "-fx-font-size: 12; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-width: 0;";
        String inactiveStyle = "-fx-background-color: #13132a; -fx-text-fill: rgba(255,255,255,0.4); -fx-font-weight: 700; "
                + "-fx-font-size: 12; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-border-width: 0;";
        if (tabDescribeBtn != null) tabDescribeBtn.setStyle(currentTab.equals("describe") ? activeStyle : inactiveStyle);
        if (tabPhotoBtn2   != null) tabPhotoBtn2.setStyle(currentTab.equals("photo")    ? activeStyle : inactiveStyle);
    }

    // ── Photo picker ──────────────────────────────────────────────

    @FXML
    private void handlePickPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Your Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        Stage stage = resolveStage();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        selectedPhotoFile = file;
        if (photoFileLabel != null) photoFileLabel.setText("📎 " + file.getName());

        // Show preview
        try {
            Image preview = new Image(file.toURI().toString(), 120, 120, true, true);
            if (photoPreviewView != null) {
                photoPreviewView.setImage(preview);
                photoPreviewView.setVisible(true);
                photoPreviewView.setManaged(true);
            }
        } catch (Exception ignored) {}

        // Auto-analyze the photo
        analyzePhotoAsync(file);
    }

    private void analyzePhotoAsync(File file) {
        if (analyzedBox != null) { analyzedBox.setVisible(false); analyzedBox.setManaged(false); }
        Platform.runLater(() -> statusLabel.setText("🔍 Analyzing your photo..."));

        new Thread(() -> {
            try {
                String desc = callVisionModel(file);
                Platform.runLater(() -> {
                    if (analyzedDescLabel != null) analyzedDescLabel.setText(desc);
                    if (analyzedBox != null) { analyzedBox.setVisible(true); analyzedBox.setManaged(true); }
                    statusLabel.setText("✔ Photo analyzed — ready to generate!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String fallback = "person with friendly face";
                    if (analyzedDescLabel != null) analyzedDescLabel.setText(fallback);
                    if (analyzedBox != null) { analyzedBox.setVisible(true); analyzedBox.setManaged(true); }
                    statusLabel.setText("⚠ Could not analyze photo, using generic description.");
                    System.err.println("[Avatar] Vision failed: " + e.getMessage());
                });
            }
        }, "avatar-vision").start();
    }

    // ── Vision: image → text description ─────────────────────────

    private String callVisionModel(File imageFile) throws Exception {
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        String mime       = getMimeType(imageFile.getName());
        String b64        = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri    = "data:" + mime + ";base64," + b64;

        String body = "{"
                + "\"model\": \"meta-llama/Llama-3.2-11B-Vision-Instruct\","
                + "\"max_tokens\": 120,"
                + "\"messages\": [{"
                + "  \"role\": \"user\","
                + "  \"content\": ["
                + "    {\"type\": \"image_url\", \"image_url\": {\"url\": \"" + dataUri + "\"}},"
                + "    {\"type\": \"text\", \"text\": \"Describe the person in this photo in ONE short sentence for an avatar prompt. Focus on: gender, age, hair color and style, eye color if visible, skin tone, distinctive features (glasses, beard, etc), expression. Example: 'young woman with long black hair, brown eyes, light skin, wearing glasses, smiling'. Reply with ONLY the description, no extra text.\"}"
                + "  ]"
                + "}]}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VISION_URL))
                .header("Authorization", "Bearer " + HF_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(45))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.err.println("[Avatar] Vision HTTP=" + response.statusCode());

        if (response.statusCode() == 503) {
            Thread.sleep(15000);
            return callVisionModel(imageFile);
        }
        if (response.statusCode() != 200)
            throw new Exception("Vision API HTTP " + response.statusCode());

        // Parse: {"choices":[{"message":{"content":"..."}}]}
        String json    = response.body();
        String content = json.replaceAll(".*\"content\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        if (content.equals(json)) throw new Exception("Could not parse vision response");

        // Clean up
        content = content.trim().replaceAll("^['\"]|['\"]$", "");
        content = content.replaceAll("(?i)^(description:|here is|the person is|i see|i can see)\\s*", "");
        return content.trim();
    }

    // ── Generate avatar ───────────────────────────────────────────

    @FXML
    private void handleGenerate() {
        // Get description from active tab
        String desc;
        if (currentTab.equals("photo")) {
            desc = (analyzedDescLabel != null && !analyzedDescLabel.getText().isBlank()
                    && !analyzedDescLabel.getText().equals("—"))
                    ? analyzedDescLabel.getText().trim()
                    : "person with friendly face";
            if (selectedPhotoFile == null) {
                statusLabel.setText("Please upload a photo first.");
                return;
            }
        } else {
            desc = descriptionField != null ? descriptionField.getText().trim() : "";
            if (desc.length() < 3) {
                statusLabel.setText("Please enter a description (min 3 characters).");
                return;
            }
        }

        String prompt = buildPrompt(desc, styleCombo.getValue());
        setLoading(true);
        statusLabel.setText("Generating avatar...");

        new Thread(() -> {
            byte[] result = tryAllProviders(prompt);
            Platform.runLater(() -> {
                if (result != null) {
                    generatedImageBytes = result;
                    resultImageView.setImage(new Image(new ByteArrayInputStream(result)));
                    setLoading(false);
                    showResult(true);
                    applyBtn.setDisable(false);
                    statusLabel.setText("✔ Avatar generated!");
                } else {
                    setLoading(false);
                    statusLabel.setText("❌ All providers failed. Check your internet connection.");
                }
            });
        }, "avatar-gen").start();
    }

    private byte[] tryAllProviders(String prompt) {
        // Provider 1: Pollinations
        try {
            Platform.runLater(() -> statusLabel.setText("🎨 Trying Pollinations FLUX..."));
            return callPollinations(prompt);
        } catch (Exception e) {
            System.err.println("[Avatar] Pollinations failed: " + e.getMessage());
        }
        // Provider 2: HuggingFace Router (working!)
        try {
            Platform.runLater(() -> statusLabel.setText("🎨 Generating with FLUX.1..."));
            return callHuggingFaceRouter(prompt);
        } catch (Exception e) {
            System.err.println("[Avatar] HF Router failed: " + e.getMessage());
        }
        return null;
    }

    private byte[] callPollinations(String prompt) throws Exception {
        long seed = System.currentTimeMillis() % 99999;
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        String url = "https://image.pollinations.ai/prompt/" + encoded
                + "?width=512&height=512&nologo=true&model=flux&seed=" + seed;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpResponse<byte[]> resp = client.send(
                HttpRequest.newBuilder().uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 EyeTwin/1.0")
                        .GET().timeout(Duration.ofSeconds(60)).build(),
                HttpResponse.BodyHandlers.ofByteArray());

        System.err.println("[Avatar] Pollinations HTTP=" + resp.statusCode() + " size=" + resp.body().length);
        if (resp.statusCode() != 200) throw new Exception("HTTP " + resp.statusCode());
        if (!isValidImage(resp.body())) throw new Exception("Not a valid image");
        return resp.body();
    }

    private byte[] callHuggingFaceRouter(String prompt) throws Exception {
        String url  = "https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell";
        String body = "{\"inputs\": \"" + prompt.replace("\"", "'") + "\"}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<byte[]> resp = client.send(
                HttpRequest.newBuilder().uri(URI.create(url))
                        .header("Authorization", "Bearer " + HF_TOKEN)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(120)).build(),
                HttpResponse.BodyHandlers.ofByteArray());

        System.err.println("[Avatar] HF Router HTTP=" + resp.statusCode() + " size=" + resp.body().length);

        if (resp.statusCode() == 503) {
            int wait = parseEstimatedTime(new String(resp.body()), 20);
            Thread.sleep(wait * 1000L);
            return callHuggingFaceRouter(prompt);
        }
        if (resp.statusCode() != 200)
            throw new Exception("HTTP " + resp.statusCode() + ": "
                    + new String(resp.body(), 0, Math.min(200, resp.body().length)));
        if (!isValidImage(resp.body())) throw new Exception("Not a valid image");
        return resp.body();
    }

    // ── Apply / Close ─────────────────────────────────────────────

    @FXML
    private void handleApply() {
        if (generatedImageBytes == null) return;
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return;

            String filename = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "profiles");
            Files.createDirectories(uploadsDir);
            Files.write(uploadsDir.resolve(filename), generatedImageBytes);

            new UserServiceImpl().saveProfilePicture(user.getId(), generatedImageBytes, filename);
            user.setProfilePicture(filename);
            SessionManager.refresh();

            statusLabel.setText("✔ Avatar applied to your profile!");
            statusLabel.setStyle("-fx-text-fill: #00e676;");

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> resolveStage().close());
            }).start();

        } catch (Exception e) {
            statusLabel.setText("❌ Could not save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() { resolveStage().close(); }

    // ── Helpers ───────────────────────────────────────────────────

    private String buildPrompt(String desc, String style) {
        return STYLE_PROMPTS.getOrDefault(style, STYLE_PROMPTS.get("anime")).replace("[DESC]", desc);
    }

    private boolean isValidImage(byte[] d) {
        if (d.length < 12) return false;
        return (d[0] == (byte)0x89 && d[1] == 'P')
                || (d[0] == (byte)0xFF && d[1] == (byte)0xD8)
                || new String(d, 8, 4).equals("WEBP");
    }

    private int parseEstimatedTime(String body, int def) {
        try { return (int) Math.ceil(Double.parseDouble(
                body.replaceAll(".*\"estimated_time\":\\s*([0-9.]+).*", "$1")));
        } catch (Exception e) { return def; }
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
        generateBtn.setText(on ? "Generating..." : "✦  Generate Avatar");
    }

    private void showResult(boolean show) {
        if (resultBox != null) { resultBox.setVisible(show); resultBox.setManaged(show); }
    }

    private Stage resolveStage() {
        javafx.scene.Node[] nodes = { generateBtn, applyBtn, statusLabel };
        for (javafx.scene.Node n : nodes)
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        return null;
    }
}