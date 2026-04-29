package com.eyetwin.controller;

import com.eyetwin.entities.Agent;
import com.eyetwin.entities.Game;
import com.eyetwin.entities.GuideVideo;
import com.eyetwin.repository.GuideVideoRepository;
import com.eyetwin.tools.SessionManager;
import com.eyetwin.entities.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgentVideosController {

    @FXML private NavbarController navbarController;
    @FXML private ImageView agentHeaderImage;
    @FXML private Label agentNameLabel;
    @FXML private Label gameNameLabel;
    @FXML private ComboBox<String> mapFilter;
    @FXML private TextField searchField;
    @FXML private FlowPane videosGrid;
    @FXML private Label guidesTotalLabel;
    @FXML private Label guidesVisibleLabel;
    @FXML private Label guidesLikesLabel;
    @FXML private Label resultsNote;
    // Modal overlay
    @FXML private StackPane videoModalOverlay;
    @FXML private Label modalTitleLabel;
    @FXML private VBox mediaContainer;

    private final GuideVideoRepository guideVideoRepository = new GuideVideoRepository();
    private Game currentGame;
    private Agent currentAgent;
    private List<GuideVideo> allGuides;
    private MediaPlayer activePlayer;

    // ═══════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════
    @FXML
    public void initialize() {
        if (navbarController != null) navbarController.setActivePage("guides");
        if (videoModalOverlay != null) videoModalOverlay.setVisible(false);
    }

    /** Called by NavbarController after navigateToWithData */
    public void initData(Object[] data) {
        this.currentGame  = (Game)  data[0];
        this.currentAgent = (Agent) data[1];

        if (agentNameLabel  != null) agentNameLabel.setText(currentAgent.getName());
        if (gameNameLabel   != null) gameNameLabel.setText(currentGame.getName());
        if (agentHeaderImage != null && currentAgent.getImage() != null) {
            try { agentHeaderImage.setImage(new Image(currentAgent.getImage(), true)); } catch (Exception ignored) {}
        }

        new Thread(this::loadGuides).start();
    }

    // ═══════════════════════════════════════════
    //  DATA
    // ═══════════════════════════════════════════
    private void loadGuides() {
        try {
            allGuides = guideVideoRepository.findApprovedByGameAndAgent(currentGame, currentAgent);
            Platform.runLater(() -> {
                buildMapFilter();
                applyFilters();
                bindListeners();
            });
        } catch (Exception e) {
            System.err.println("[AgentVideosController] Error: " + e.getMessage());
        }
    }

    private void buildMapFilter() {
        List<String> maps = new ArrayList<>();
        maps.add("All");
        allGuides.forEach(g -> {
            if (g.getMap() != null && !g.getMap().equals("All") && !maps.contains(g.getMap()))
                maps.add(g.getMap());
        });
        if (mapFilter != null) {
            mapFilter.getItems().setAll(maps);
            mapFilter.getSelectionModel().selectFirst();
        }
    }

    private void bindListeners() {
        if (mapFilter  != null) mapFilter.setOnAction(e -> applyFilters());
        if (searchField != null) searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void applyFilters() {
        String selectedMap = (mapFilter != null && mapFilter.getValue() != null)
                ? mapFilter.getValue().toLowerCase() : "all";
        String query = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim().toLowerCase() : "";

        List<GuideVideo> filtered = allGuides.stream().filter(g -> {
            String gMap = g.getMap() == null ? "all" : g.getMap().toLowerCase();
            boolean mapMatch = selectedMap.equals("all") || gMap.equals(selectedMap) || gMap.equals("all");
            boolean searchMatch = query.isEmpty()
                    || g.getTitle().toLowerCase().contains(query)
                    || (g.getUploadedBy() != null && g.getUploadedBy().getUsername().toLowerCase().contains(query));
            return mapMatch && searchMatch;
        }).toList();

        long totalLikes = filtered.stream().mapToLong(GuideVideo::getLikes).sum();
        if (guidesTotalLabel  != null) guidesTotalLabel.setText(String.valueOf(allGuides.size()));
        if (guidesVisibleLabel != null) guidesVisibleLabel.setText(String.valueOf(filtered.size()));
        if (guidesLikesLabel  != null) guidesLikesLabel.setText(String.valueOf(totalLikes));
        if (resultsNote != null) resultsNote.setText(filtered.size() + " vidéo(s) affichée(s)");

        renderVideos(filtered);
    }

    // ═══════════════════════════════════════════
    //  RENDER VIDEO CARDS
    // ═══════════════════════════════════════════
    private void renderVideos(List<GuideVideo> guides) {
        videosGrid.getChildren().clear();

        if (guides.isEmpty()) {
            Label empty = new Label("Aucune vidéo disponible pour cet agent");
            empty.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 15;");
            StackPane emptyPane = new StackPane(empty);
            emptyPane.setPrefWidth(600);
            videosGrid.getChildren().add(emptyPane);
            return;
        }

        User currentUser = SessionManager.getCurrentUser();

        for (int i = 0; i < guides.size(); i++) {
            VBox card = buildVideoCard(guides.get(i), currentUser);
            card.setOpacity(0);
            card.setTranslateY(20);
            int delay = i * 70;
            Timeline anim = new Timeline(
                    new KeyFrame(Duration.millis(350 + delay),
                            new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT))
            );
            anim.play();
            videosGrid.getChildren().add(card);
        }
    }

    private VBox buildVideoCard(GuideVideo video, User currentUser) {
        VBox card = new VBox(0);
        card.setPrefWidth(270);
        card.setMaxWidth(270);
        card.setStyle(
                "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.85),rgba(11,17,31,0.85));" +
                "-fx-border-color: rgba(255,0,0,0.12);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;" +
                "-fx-cursor: hand;"
        );
        card.setEffect(new DropShadow(25, Color.web("#000000", 0.35)));

        // ── Thumbnail ──
        StackPane thumbContainer = new StackPane();
        thumbContainer.setPrefHeight(152);
        thumbContainer.setStyle("-fx-background-color: #000; -fx-background-radius: 14 14 0 0;");

        Rectangle clip = new Rectangle(270, 152);
        clip.setArcWidth(0); clip.setArcHeight(0);

        ImageView thumb = new ImageView();
        thumb.setFitWidth(270); thumb.setFitHeight(152);
        thumb.setPreserveRatio(false); thumb.setSmooth(true);
        String thumbSrc = video.getThumbnail() != null ? video.getThumbnail()
                : "https://via.placeholder.com/270x152?text=Guide";
        try { thumb.setImage(new Image(thumbSrc, true)); } catch (Exception ignored) {}

        // Play overlay
        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(60, 60);
        playBtn.setMaxSize(60, 60);
        playBtn.setStyle(
                "-fx-background-color: rgba(255,0,0,0.8);" +
                "-fx-background-radius: 30;"
        );
        playBtn.setEffect(new DropShadow(20, Color.web("#ff0000", 0.5)));
        Label playIcon = new Label("▶");
        playIcon.setStyle("-fx-text-fill: white; -fx-font-size: 22;");
        playBtn.getChildren().add(playIcon);
        playBtn.setOpacity(0);
        playBtn.setCursor(Cursor.HAND);
        playBtn.setOnMouseClicked(e -> { e.consume(); openVideoModal(video); });

        thumbContainer.getChildren().addAll(thumb, playBtn);

        // ── Info ──
        VBox info = new VBox(10);
        info.setPadding(new Insets(16, 16, 16, 16));
        info.setStyle("-fx-background-radius: 0 0 14 14;");

        Label title = new Label(video.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        title.setWrapText(true);
        title.setMaxHeight(40);

        // Metadata row
        HBox meta = new HBox();
        meta.setAlignment(Pos.CENTER_LEFT);
        String uploaderName = video.getUploadedBy() != null ? video.getUploadedBy().getUsername() : "Unknown";
        Label uploader = new Label(uploaderName.toUpperCase());
        uploader.setStyle("-fx-text-fill: #4cd3e3; -fx-font-size: 11; -fx-font-weight: bold;");
        HBox.setHgrow(uploader, Priority.ALWAYS);

        // Like button
        boolean alreadyLiked = currentUser != null && video.isLikedByUser(currentUser);
        Button likeBtn = new Button("♥ " + video.getLikes());
        likeBtn.setStyle(buildLikeStyle(alreadyLiked));
        if (currentUser == null) {
            likeBtn.setDisable(true);
            likeBtn.setTooltip(new Tooltip("Connectez-vous pour liker"));
        } else {
            likeBtn.setOnAction(e -> handleLike(video, likeBtn, currentUser));
        }
        meta.getChildren().addAll(uploader, likeBtn);

        // Separator
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        // Watch button
        Button watchBtn = new Button("▶  Regarder");
        watchBtn.setMaxWidth(Double.MAX_VALUE);
        watchBtn.setStyle(
                "-fx-background-color: linear-gradient(135deg,rgba(76,211,227,0.15),rgba(76,211,227,0.05));" +
                "-fx-border-color: rgba(76,211,227,0.4);" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #4cd3e3;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 11;" +
                "-fx-padding: 10 0 10 0;" +
                "-fx-cursor: hand;"
        );
        watchBtn.setOnAction(e -> openVideoModal(video));

        info.getChildren().addAll(title, meta, sep, watchBtn);
        card.getChildren().addAll(thumbContainer, info);

        // Hover
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.95),rgba(11,17,31,0.95));" +
                    "-fx-border-color: #ff0000;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;" +
                    "-fx-cursor: hand;"
            );
            card.setEffect(new DropShadow(40, Color.web("#ff0000", 0.2)));
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(card.translateYProperty(), -8, Interpolator.EASE_OUT),
                            new KeyValue(playBtn.opacityProperty(), 1)));
            tl.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: linear-gradient(135deg,rgba(26,31,46,0.85),rgba(11,17,31,0.85));" +
                    "-fx-border-color: rgba(255,0,0,0.12);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;" +
                    "-fx-cursor: hand;"
            );
            card.setEffect(new DropShadow(25, Color.web("#000000", 0.35)));
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT),
                            new KeyValue(playBtn.opacityProperty(), 0)));
            tl.play();
        });

        return card;
    }

    // ═══════════════════════════════════════════
    //  LIKE
    // ═══════════════════════════════════════════
    private void handleLike(GuideVideo video, Button likeBtn, User user) {
        likeBtn.setDisable(true);
        new Thread(() -> {
            try {
                boolean wasLiked = video.isLikedByUser(user);
                if (wasLiked) {
                    video.removeLikedBy(user);
                } else {
                    video.addLikedBy(user);
                }
                guideVideoRepository.saveLike(video);

                Platform.runLater(() -> {
                    likeBtn.setText("♥ " + video.getLikes());
                    likeBtn.setStyle(buildLikeStyle(!wasLiked));
                    likeBtn.setDisable(false);

                    // pulse animation
                    ScaleTransition pulse = new ScaleTransition(Duration.millis(150), likeBtn);
                    pulse.setFromX(1); pulse.setFromY(1);
                    pulse.setToX(1.3); pulse.setToY(1.3);
                    pulse.setAutoReverse(true); pulse.setCycleCount(2);
                    pulse.play();
                });
            } catch (Exception e) {
                Platform.runLater(() -> likeBtn.setDisable(false));
            }
        }).start();
    }

    private String buildLikeStyle(boolean liked) {
        if (liked) {
            return "-fx-background-color: rgba(255,0,0,0.18); -fx-border-color: transparent;" +
                   "-fx-text-fill: #ff0000; -fx-font-weight: bold; -fx-font-size: 11;" +
                   "-fx-background-radius: 8; -fx-padding: 5 10 5 10; -fx-cursor: hand;";
        }
        return "-fx-background-color: transparent; -fx-border-color: transparent;" +
               "-fx-text-fill: #aeb8c9; -fx-font-size: 11;" +
               "-fx-background-radius: 8; -fx-padding: 5 10 5 10; -fx-cursor: hand;";
    }

    // ═══════════════════════════════════════════
    //  VIDEO MODAL
    // ═══════════════════════════════════════════
    private void openVideoModal(GuideVideo video) {
        if (videoModalOverlay == null) return;

        stopActivePlayer();

        if (modalTitleLabel != null) modalTitleLabel.setText(video.getTitle());
        if (mediaContainer != null) mediaContainer.getChildren().clear();

        String url = video.getVideoUrl();
        boolean isLocal = url != null && url.startsWith("/uploads/guide-videos/");
        boolean isCloudinary = isCloudinaryMediaUrl(url);
        boolean isCloudflare = isCloudflareStream(url);

        if ((isLocal || isCloudinary) && url != null) {
            if (!playWithMediaPlayer(toLocalFileMediaUrl(url), null)) {
                showEmbeddedVideoFallback(url);
            }
        } else if (isCloudflare) {
            String hlsUrl = toCloudflareHlsUrl(url);
            String mp4Url = toCloudflareMp4Url(url);
            if (!playWithMediaCandidates(List.of(hlsUrl, mp4Url), url)) {
                showEmbeddedVideoFallback(url);
            }
        } else {
            if (!playWithMediaPlayer(url, url)) {
                showEmbeddedVideoFallback(url);
            }
        }

        videoModalOverlay.setVisible(true);
        videoModalOverlay.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(250), videoModalOverlay);
        ft.setToValue(1);
        ft.play();
    }

    private boolean playWithMediaPlayer(String mediaUrl, String fallbackUrl) {
        if (mediaContainer == null || mediaUrl == null || mediaUrl.isBlank()) return false;

        try {
            Media media = new Media(mediaUrl);
            activePlayer = new MediaPlayer(media);

            MediaView mediaView = new MediaView(activePlayer);
            mediaView.setFitWidth(860);
            mediaView.setPreserveRatio(true);

            Label loading = new Label("Chargement de la video...");
            loading.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 13;");

            activePlayer.setOnReady(() -> {
                mediaContainer.getChildren().setAll(mediaView);
                activePlayer.play();
            });

            activePlayer.setOnError(() -> {
                stopActivePlayer();
                if (fallbackUrl != null && !fallbackUrl.isBlank()) {
                    showEmbeddedVideoFallback(fallbackUrl);
                } else {
                    Label err = new Label("Impossible de lire cette video dans le lecteur interne.");
                    err.setStyle("-fx-text-fill: #ff8a8a; -fx-font-size: 13;");
                    mediaContainer.getChildren().setAll(err);
                }
            });

            mediaContainer.getChildren().setAll(loading);
            return true;
        } catch (Exception e) {
            stopActivePlayer();
            return false;
        }
    }

    private boolean playWithMediaCandidates(List<String> mediaUrls, String fallbackUrl) {
        if (mediaContainer == null || mediaUrls == null) return false;

        List<String> candidates = mediaUrls.stream()
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .toList();

        if (candidates.isEmpty()) {
            return false;
        }

        Label loading = new Label("Chargement de la video...");
        loading.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 13;");
        mediaContainer.getChildren().setAll(loading);

        attemptPlayCandidate(candidates, 0, fallbackUrl);
        return true;
    }

    private void attemptPlayCandidate(List<String> candidates, int index, String fallbackUrl) {
        if (mediaContainer == null) return;

        if (index >= candidates.size()) {
            if (fallbackUrl != null && !fallbackUrl.isBlank()) {
                showEmbeddedVideoFallback(fallbackUrl);
            } else {
                Label err = new Label("Impossible de lire cette video.");
                err.setStyle("-fx-text-fill: #ff8a8a; -fx-font-size: 13;");
                mediaContainer.getChildren().setAll(err);
            }
            return;
        }

        try {
            stopActivePlayer();

            Media media = new Media(candidates.get(index));
            activePlayer = new MediaPlayer(media);

            MediaView mediaView = new MediaView(activePlayer);
            mediaView.setFitWidth(860);
            mediaView.setPreserveRatio(true);

            activePlayer.setOnReady(() -> {
                mediaContainer.getChildren().setAll(mediaView);
                activePlayer.play();
            });

            activePlayer.setOnError(() -> {
                stopActivePlayer();
                attemptPlayCandidate(candidates, index + 1, fallbackUrl);
            });

        } catch (Exception e) {
            attemptPlayCandidate(candidates, index + 1, fallbackUrl);
        }
    }

    private String toLocalFileMediaUrl(String relativeUploadPath) {
        String normalized = relativeUploadPath.replace("/", java.io.File.separator);
        if (normalized.startsWith(java.io.File.separator)) {
            normalized = normalized.substring(1);
        }
        java.io.File file = new java.io.File(System.getProperty("user.dir"), normalized);
        return file.toURI().toString();
    }

    private boolean isCloudinaryMediaUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("res.cloudinary.com") || lower.contains("cloudinary.com");
    }

    private boolean isCloudflareStream(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("videodelivery.net");
    }

    private String toCloudflareHlsUrl(String url) {
        if (url == null || url.isBlank()) return url;
        String id = extractCloudflareVideoId(url);
        if (id == null || id.isBlank()) return url;
        return "https://videodelivery.net/" + id + "/manifest/video.m3u8";
    }

    private String toCloudflareMp4Url(String url) {
        if (url == null || url.isBlank()) return url;
        String id = extractCloudflareVideoId(url);
        if (id == null || id.isBlank()) return url;
        return "https://videodelivery.net/" + id + "/downloads/default.mp4";
    }

    private String extractCloudflareVideoId(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank()) return null;
            String[] parts = path.split("/");
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    return part;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showEmbeddedVideoFallback(String url) {
        if (mediaContainer == null) return;

        if (url == null || url.isBlank()) {
            Label lbl = new Label("Aucune source vidéo disponible");
            lbl.setStyle("-fx-text-fill: #aeb8c9; -fx-font-size: 13;");
            mediaContainer.getChildren().add(lbl);
            return;
        }

        WebView webView = new WebView();
        webView.setPrefSize(720, 405);
        webView.setMinSize(720, 405);
        webView.setMaxSize(960, 540);
        webView.setStyle("-fx-background-color: black;");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

                String embedUrl = normalizeEmbedUrl(url);
                String html;

                if (isCloudinaryMediaUrl(url) || url.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
                    html = """
                                <!doctype html>
                                <html>
                                    <head>
                                        <meta charset=\"utf-8\" />
                                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                                        <style>
                                            html, body {
                                                margin: 0;
                                                padding: 0;
                                                width: 100%%;
                                                height: 100%%;
                                                background: #000;
                                                overflow: hidden;
                                            }
                                            .wrap {
                                                position: absolute;
                                                inset: 0;
                                            }
                                            video {
                                                width: 100%%;
                                                height: 100%%;
                                                object-fit: contain;
                                                background: #000;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <div class=\"wrap\">
                                            <video controls autoplay playsinline>
                                                <source src=\"%s\" type=\"video/mp4\" />
                                                Your browser does not support the video tag.
                                            </video>
                                        </div>
                                    </body>
                                </html>
                                """.formatted(escapeHtml(embedUrl));
                } else {
                    html = """
                                <!doctype html>
                                <html>
                                    <head>
                                        <meta charset=\"utf-8\" />
                                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                                        <style>
                                            html, body {
                                                margin: 0;
                                                padding: 0;
                                                width: 100%%;
                                                height: 100%%;
                                                background: #000;
                                                overflow: hidden;
                                            }
                                            .wrap {
                                                position: absolute;
                                                inset: 0;
                                            }
                                            iframe {
                                                border: 0;
                                                width: 100%%;
                                                height: 100%%;
                                                display: block;
                                                background: #000;
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <div class=\"wrap\">
                                            <iframe
                                                src=\"%s\"
                                                allow=\"autoplay; fullscreen; picture-in-picture; encrypted-media\"
                                                allowfullscreen
                                                referrerpolicy=\"strict-origin-when-cross-origin\"></iframe>
                                        </div>
                                    </body>
                                </html>
                                """.formatted(escapeHtml(embedUrl));
                }

                engine.loadContent(html, "text/html");

        mediaContainer.getChildren().add(webView);
    }

        private String normalizeEmbedUrl(String url) {
                String clean = url.trim();
                String lower = clean.toLowerCase(Locale.ROOT);

                if (lower.contains("iframe.videodelivery.net") && !lower.contains("autoplay=")) {
                        if (clean.contains("?")) {
                                return clean + "&autoplay=true";
                        }
                        return clean + "?autoplay=true";
                }

                return clean;
        }

        private String escapeHtml(String value) {
                return value
                                .replace("&", "&amp;")
                                .replace("\"", "&quot;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;");
        }

    @FXML
    public void closeVideoModal() {
        if (videoModalOverlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(200), videoModalOverlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            videoModalOverlay.setVisible(false);
            stopActivePlayer();
            if (mediaContainer != null) mediaContainer.getChildren().clear();
        });
        ft.play();
    }

    @FXML
    public void consumeMouseEvent(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
    }

    private void stopActivePlayer() {
        if (activePlayer != null) {
            activePlayer.stop();
            activePlayer.dispose();
            activePlayer = null;
        }
    }

    // ═══════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════
    @FXML public void goBackToAgents() {
        stopActivePlayer();
        navbarController.navigateToWithData("AgentsList.fxml", "game", currentGame);
    }

    @FXML public void goBackToGames() {
        stopActivePlayer();
        navbarController.navigateTo("GamesSelection.fxml");
    }
}
