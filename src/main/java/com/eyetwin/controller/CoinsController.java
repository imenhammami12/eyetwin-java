package com.eyetwin.controller;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.ICoinService;
import com.eyetwin.services.CoinServiceImpl;
import com.eyetwin.services.FlouciService;
import com.eyetwin.services.StripePaymentChecker;
import com.eyetwin.services.StripeService;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.animation.*;
import javafx.util.Duration;

public class CoinsController {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private Label  balanceLabel;
    @FXML private HBox   packagesContainer;
    @FXML private VBox   historyContainer;
    @FXML private Label  flashLabel;
    @FXML private VBox   flashBox;

    @FXML private ImageView heroCoinsImage;   // grand coin dans le hero
    @FXML private VBox      heroContent;      // VBox du contenu hero pour fade-in

    // ── Services ──────────────────────────────────────────────
    private final ICoinService         coinService    = new CoinServiceImpl();
    private final StripeService        stripeService  = new StripeService();
    private final StripePaymentChecker paymentChecker = new StripePaymentChecker();
    private final FlouciService        flouciService  = new FlouciService();

    // ══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { navigateTo("login.fxml"); return; }

        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlash(flash[0], flash[1]);

        refreshBalance(user);
        buildPackageCards(user);
        loadHistory(user);
        animateUI();
    }

    private void animateUI() {
        // 1. Fade-in + slide-up du hero content
        if (heroContent != null) {
            heroContent.setOpacity(0);
            heroContent.setTranslateY(24);
            FadeTransition fade = new FadeTransition(Duration.millis(650), heroContent);
            fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(650), heroContent);
            slide.setFromY(24); slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, slide).play();
        }

        // 2. Rotation lente du coin hero
        if (heroCoinsImage != null) {
            RotateTransition rot = new RotateTransition(Duration.seconds(12), heroCoinsImage);
            rot.setByAngle(360);
            rot.setCycleCount(Animation.INDEFINITE);
            rot.setInterpolator(Interpolator.LINEAR);
            rot.play();
        }

        // 3. Pulse doré sur le balanceLabel
        if (balanceLabel != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.seconds(2.2), balanceLabel);
            pulse.setFromX(1.0); pulse.setToX(1.03);
            pulse.setFromY(1.0); pulse.setToY(1.03);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.play();
        }

        // 4. Fade-in décalé des cards packages
        if (packagesContainer != null) {
            int delay = 0;
            for (javafx.scene.Node card : packagesContainer.getChildren()) {
                card.setOpacity(0);
                card.setTranslateY(20);
                FadeTransition f = new FadeTransition(Duration.millis(500), card);
                f.setFromValue(0); f.setToValue(1);
                f.setDelay(Duration.millis(120 + delay));
                TranslateTransition t = new TranslateTransition(Duration.millis(500), card);
                t.setFromY(20); t.setToY(0);
                t.setDelay(Duration.millis(120 + delay));
                t.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(f, t).play();
                delay += 80;
            }
        }
    }



    // ══════════════════════════════════════════════════════════
    //  BALANCE
    // ══════════════════════════════════════════════════════════
    private void refreshBalance(User user) {
        if (balanceLabel != null)
            balanceLabel.setText(user.getCoinBalance() + "  coins");
    }

    // ══════════════════════════════════════════════════════════
    //  PACKAGE CARDS
    // ══════════════════════════════════════════════════════════
    private void buildPackageCards(User user) {
        if (packagesContainer == null) return;
        packagesContainer.getChildren().clear();
        for (CoinPackage pkg : coinService.getAvailablePackages())
            packagesContainer.getChildren().add(createPackageCard(pkg, user));
    }

    /**
     * Crée une carte de package propre, sans emoji, avec bonne lisibilité.
     * Couleurs : texte #c0c0e0 minimum sur fond sombre, titres en blanc.
     */
    private VBox createPackageCard(CoinPackage pkg, User user) {
        boolean pop = pkg.isPopular();

        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(188);
        card.setMinHeight(290);

        // Glow violet sur la carte populaire
        if (pop) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#5248c8", 0.5));
            glow.setRadius(32);
            card.setEffect(glow);
            card.setStyle("""
                    -fx-background-color: #0d0d22;
                    -fx-background-radius: 14;
                    -fx-border-color: #5248c8;
                    -fx-border-width: 1.5;
                    -fx-border-radius: 14;
                    """);
        } else {
            card.setStyle("""
                    -fx-background-color: #0e0e1e;
                    -fx-background-radius: 14;
                    -fx-border-color: #1c1c38;
                    -fx-border-width: 1;
                    -fx-border-radius: 14;
                    """);
        }

        // ── Zone haute : coins ─────────────────────────────────
        VBox topZone = new VBox(4);
        topZone.setAlignment(Pos.CENTER);
        topZone.setPadding(new Insets(pop ? 22 : 20, 16, 16, 16));
        topZone.setStyle(pop
                ? "-fx-background-color: #121230; -fx-background-radius: 12 12 0 0;"
                : "-fx-background-color: transparent; -fx-background-radius: 12 12 0 0;");

        // Badge "MOST POPULAR" (texte uniquement, pas d'emoji)
        if (pop) {
            Label badge = new Label("MOST POPULAR");
            badge.setStyle("""
                    -fx-font-size: 9;
                    -fx-font-weight: bold;
                    -fx-text-fill: #a090ff;
                    -fx-letter-spacing: 2;
                    -fx-background-color: #1e1a50;
                    -fx-background-radius: 20;
                    -fx-padding: 4 12 4 12;
                    """);
            topZone.getChildren().add(badge);
        }

        // Nombre de coins — grand, lisible, doré
        Label coinsNum = new Label(String.valueOf(pkg.getCoins()));
        coinsNum.setStyle(
                "-fx-font-size: " + (pop ? "50" : "42") + ";" +
                        "-fx-font-weight: 900;" +
                        "-fx-text-fill: #f0c040;"
        );

        // Label "COINS"
        Label coinsWord = new Label("COINS");
        coinsWord.setStyle("""
                -fx-font-size: 10;
                -fx-font-weight: bold;
                -fx-text-fill: #8a7828;
                -fx-letter-spacing: 3;
                """);

        topZone.getChildren().addAll(coinsNum, coinsWord);

        // ── Séparateur ─────────────────────────────────────────
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: " + (pop ? "#20204a" : "#141428") + ";");

        // ── Zone basse : prix + bouton ─────────────────────────
        VBox bottomZone = new VBox(6);
        bottomZone.setAlignment(Pos.CENTER);
        bottomZone.setPadding(new Insets(16, 16, 20, 16));

        // Nom du pack
        Label packName = new Label(pkg.getLabel());
        packName.setStyle("""
                -fx-font-size: 12;
                -fx-text-fill: #6060a0;
                -fx-font-weight: 600;
                """);

        // Prix — blanc, grand, lisible
        Label price = new Label(String.format("%.2f EUR", pkg.getPriceInEuros()));
        price.setStyle("""
                -fx-font-size: 22;
                -fx-font-weight: 800;
                -fx-text-fill: #e8e8ff;
                """);

        // Coût par coin
        Label perCoin = new Label(String.format("%.1f cts / coin", pkg.getCentPerCoin()));
        perCoin.setStyle("""
                -fx-font-size: 11;
                -fx-text-fill: #383870;
                """);

        Region spacer = new Region();
        spacer.setPrefHeight(8);

        // Bouton Buy
        Button buyBtn = new Button(pop ? "BUY NOW" : "Buy Now");
        buyBtn.setPrefWidth(150);
        buyBtn.setPrefHeight(36);
        buyBtn.setCursor(Cursor.HAND);

        String btnStyleBase, btnStyleHover;
        if (pop) {
            btnStyleBase = """
                    -fx-background-color: #5248c8;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 12;
                    -fx-letter-spacing: 1;
                    -fx-background-radius: 8;
                    -fx-border-color: transparent;
                    """;
            btnStyleHover = """
                    -fx-background-color: #6258e0;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 12;
                    -fx-letter-spacing: 1;
                    -fx-background-radius: 8;
                    -fx-border-color: transparent;
                    """;
        } else {
            btnStyleBase = """
                    -fx-background-color: transparent;
                    -fx-text-fill: #7070b0;
                    -fx-font-size: 12;
                    -fx-font-weight: 600;
                    -fx-border-color: #252550;
                    -fx-border-width: 1;
                    -fx-border-radius: 8;
                    -fx-background-radius: 8;
                    """;
            btnStyleHover = """
                    -fx-background-color: #12122e;
                    -fx-text-fill: #a0a0e0;
                    -fx-font-size: 12;
                    -fx-font-weight: 600;
                    -fx-border-color: #3a3a70;
                    -fx-border-width: 1;
                    -fx-border-radius: 8;
                    -fx-background-radius: 8;
                    """;
        }

        buyBtn.setStyle(btnStyleBase);
        buyBtn.setOnMouseEntered(e -> buyBtn.setStyle(btnStyleHover));
        buyBtn.setOnMouseExited(e  -> buyBtn.setStyle(btnStyleBase));
        buyBtn.setOnAction(e -> handleBuyPackage(user, pkg));

        bottomZone.getChildren().addAll(packName, price, perCoin, spacer, buyBtn);
        card.getChildren().addAll(topZone, sep, bottomZone);

        card.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), card);
            s.setToX(1.035); s.setToY(1.035);
            s.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), card);
            s.setToX(1.0); s.setToY(1.0);
            s.play();
        });

        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  ACHAT
    // ══════════════════════════════════════════════════════════
    private void handleBuyPackage(User user, CoinPackage pkg) {
        ChoiceDialog<PaymentMethod> dialog = new ChoiceDialog<>(
                PaymentMethod.STRIPE, PaymentMethod.values());
        dialog.setTitle("Payment Method");
        dialog.setHeaderText("Choose your payment method");
        dialog.setContentText("Method:");

        Optional<PaymentMethod> chosen = dialog.showAndWait();
        if (chosen.isEmpty()) return;

        PaymentMethod method = chosen.get();
        String currency = method == PaymentMethod.STRIPE ? "EUR" : "TND";
        double price    = method == PaymentMethod.STRIPE
                ? pkg.getPriceInEuros() : pkg.getPriceInCents() / 100.0;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Purchase");
        confirm.setHeaderText(pkg.getLabel() + " — " + pkg.getCoins() + " coins");
        confirm.setContentText(String.format(
                "Method: %s\nAmount: %.2f %s\n\nContinue?",
                method.getLabel(), price, currency));

        confirm.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            try {
                if (method == PaymentMethod.STRIPE) handleStripePayment(user, pkg);
                else                                handleFlouciPayment(user, pkg);
            } catch (Exception ex) {
                showFlash("error", "Payment error: " + ex.getMessage());
            }
        });
    }

    // ── Stripe ────────────────────────────────────────────────
    private void handleStripePayment(User user, CoinPackage pkg) throws Exception {
        CheckoutResult c = stripeService.createCheckoutSession(user, pkg);
        stripeService.openCheckoutInBrowser(c.url);
        showFlash("info", "Stripe checkout opened. Waiting for payment...");
        startPolling(user, pkg, c.sessionId, false);
    }

    // ── Flouci ────────────────────────────────────────────────
    private void handleFlouciPayment(User user, CoinPackage pkg) throws Exception {
        CheckoutResult c = flouciService.createPaymentSession(user.getId(), pkg);
        flouciService.openPaymentInBrowser(c.url);
        showFlash("info", "Flouci page opened. Waiting for payment...");
        startPolling(user, pkg, c.sessionId, true);
    }

    // ── Polling unifié ────────────────────────────────────────
    private void startPolling(User user, CoinPackage pkg, String sessionId, boolean isFlouci) {
        Thread t = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try { Thread.sleep(3000); } catch (InterruptedException e) { return; }

                boolean paid = isFlouci
                        ? flouciService.isPaymentCompleted(sessionId)
                        : paymentChecker.isSessionPaid(sessionId);

                if (paid) {
                    if (!coinService.isPurchaseAlreadyProcessed(sessionId))
                        coinService.recordPurchase(user, pkg.getCoins(),
                                pkg.getPriceInEuros(), sessionId);
                    Platform.runLater(() -> {
                        refreshBalance(user);
                        loadHistory(user);
                        showFlash("success",
                                pkg.getCoins() + " coins added to your account!");
                    });
                    return;
                }
            }
            Platform.runLater(() ->
                    showFlash("warning", "Payment not detected. Check your balance later."));
        });
        t.setDaemon(true);
        t.start();
    }

    // ══════════════════════════════════════════════════════════
    //  HISTORIQUE
    // ══════════════════════════════════════════════════════════
    private void loadHistory(User user) {
        if (historyContainer == null) return;
        historyContainer.getChildren().clear();

        List<CoinPurchase> history = coinService.getPurchaseHistory(user);
        if (history.isEmpty()) {
            Label empty = new Label("No purchases yet.");
            empty.setStyle("-fx-text-fill: #404070; -fx-font-size: 13; -fx-padding: 8 0 0 0;");
            historyContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

        for (CoinPurchase p : history) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefHeight(50);
            row.setStyle("""
                    -fx-background-color: #0b0b1c;
                    -fx-background-radius: 10;
                    -fx-border-color: #141430;
                    -fx-border-width: 1;
                    -fx-border-radius: 10;
                    """);

            // Bande colorée gauche (status indicator)
            boolean completed = "completed".equalsIgnoreCase(p.getStatus());
            Rectangle strip = new Rectangle(4, 50);
            strip.setFill(Color.web(completed ? "#3a8a60" : "#8a6820"));
            strip.setArcWidth(8);
            strip.setArcHeight(8);

            // Contenu intérieur
            HBox inner = new HBox(16);
            inner.setAlignment(Pos.CENTER_LEFT);
            inner.setPadding(new Insets(0, 20, 0, 18));
            HBox.setHgrow(inner, Priority.ALWAYS);

            // Montant coins — lisible, doré
            Label coins = new Label("+" + p.getCoinsAmount() + " coins");
            coins.setStyle("""
                    -fx-font-size: 14;
                    -fx-font-weight: 700;
                    -fx-text-fill: #d4a820;
                    """);
            coins.setPrefWidth(130);

            // Prix — blanc
            Label price = new Label(p.getPricePaid() + " EUR");
            price.setStyle("-fx-font-size: 13; -fx-text-fill: #c0c0e0;");
            price.setPrefWidth(90);

            // Statut badge
            String bgColor   = completed ? "rgba(40,120,70,0.20)"  : "rgba(140,90,20,0.20)";
            String textColor = completed ? "#50c090"               : "#c09040";
            Label status = new Label(p.getStatus().toUpperCase());
            status.setStyle(
                    "-fx-font-size: 10; -fx-font-weight: bold; -fx-letter-spacing: 1;" +
                            "-fx-text-fill: " + textColor + ";" +
                            "-fx-background-color: " + bgColor + ";" +
                            "-fx-background-radius: 20; -fx-padding: 5 14 5 14;"
            );
            status.setPrefWidth(110);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Date
            Label date = new Label(p.getCompletedAt() != null
                    ? fmt.format(p.getCompletedAt()) : "--");
            date.setStyle("-fx-font-size: 11; -fx-text-fill: #383870;");

            inner.getChildren().addAll(coins, price, status, spacer, date);
            row.getChildren().addAll(strip, inner);
            historyContainer.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  FLASH MESSAGES
    // ══════════════════════════════════════════════════════════
    private void showFlash(String type, String message) {
        if (flashLabel == null) return;

        String bg, border, textColor;
        switch (type) {
            case "success" -> { bg = "rgba(40,120,70,0.12)";  border = "#3a8a60"; textColor = "#70d0a0"; }
            case "warning" -> { bg = "rgba(160,100,20,0.12)"; border = "#9a6a20"; textColor = "#d09040"; }
            case "error"   -> { bg = "rgba(160,40,40,0.12)";  border = "#9a3030"; textColor = "#e07070"; }
            default        -> { bg = "rgba(60,50,180,0.12)";  border = "#4040c0"; textColor = "#9090e0"; }
        }

        flashLabel.setText(message);
        flashLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 13;");

        if (flashBox != null) {
            flashBox.setStyle(String.format("""
                    -fx-background-color: %s;
                    -fx-border-color: %s;
                    -fx-border-width: 0 0 0 3;
                    -fx-background-radius: 8;
                    -fx-padding: 14 20 14 20;
                    """, bg, border));
            flashBox.setVisible(true);
            flashBox.setManaged(true);
        }
    }

    public void hideFlash() {
        if (flashBox != null) { flashBox.setVisible(false); flashBox.setManaged(false); }
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════
    @FXML public void goToProfile() { navigateTo("UserProfile.fxml"); }
    @FXML public void goBack()      { navigateTo("home.fxml"); }

    private void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        java.net.URL url = null;
        for (String p : paths) { url = getClass().getResource(p); if (url != null) break; }
        if (url == null) { System.err.println("[CoinsController] FXML not found: " + fxml); return; }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = (Stage) (balanceLabel != null
                    ? balanceLabel.getScene().getWindow() : null);
            if (stage == null) return;
            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            Scene current  = stage.getScene();
            if (current != null) newScene.getStylesheets().addAll(current.getStylesheets());
            stage.setScene(newScene);
        } catch (IOException e) {
            System.err.println("[CoinsController] Load error: " + fxml);
            e.printStackTrace();
        }
    }
}
