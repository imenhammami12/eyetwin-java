package com.eyetwin.controller;

import com.eyetwin.entities.CheckoutResult;
import com.eyetwin.entities.CoinPackage;
import com.eyetwin.entities.CoinPurchase;
import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoinService;
import com.eyetwin.services.CoinServiceImpl;
import com.eyetwin.services.StripePaymentChecker;
import com.eyetwin.services.StripeService;
import com.eyetwin.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur JavaFX pour la vue Coins.fxml
 * Miroir de CoinController.php + coins/index.html.twig (Symfony)
 *
 * Fonctionnalités :
 *  - Afficher le solde actuel
 *  - Afficher les 4 packages disponibles
 *  - Simuler un achat (demo / intégration future avec Stripe API)
 *  - Afficher l'historique des achats
 *  - Flash messages (succès / erreur)
 */
public class CoinsController {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private Label    balanceLabel;
    @FXML private HBox     packagesContainer;
    @FXML private VBox     historyContainer;
    @FXML private Label    flashLabel;
    @FXML private VBox     flashBox;

    // ── Service ───────────────────────────────────────────────
    private final ICoinService coinService = new CoinServiceImpl();
    private final StripeService stripeService = new StripeService();

    private final StripePaymentChecker paymentChecker = new StripePaymentChecker();

    // ══════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            navigateTo("login.fxml");
            return;
        }

        // Flash message venant d'une autre page (ex: après achat)
        String[] flash = SessionManager.consumeFlash();
        if (flash != null) showFlash(flash[0], flash[1]);

        refreshBalance(user);
        buildPackageCards(user);
        loadHistory(user);
    }

    // ══════════════════════════════════════════════════════════
    //  SOLDE
    // ══════════════════════════════════════════════════════════

    private void refreshBalance(User user) {
        if (balanceLabel != null) {
            balanceLabel.setText("🪙 " + user.getCoinBalance() + " coins");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PACKAGES — construction dynamique des cartes
    // ══════════════════════════════════════════════════════════

    /**
     * Construit les cartes de packages de façon programmatique
     * (miroir du bloc Twig {% for coins, package in packages %})
     */
    private void buildPackageCards(User user) {
        if (packagesContainer == null) return;
        packagesContainer.getChildren().clear();

        List<CoinPackage> packages = coinService.getAvailablePackages();

        for (CoinPackage pkg : packages) {
            VBox card = createPackageCard(pkg, user);
            packagesContainer.getChildren().add(card);
        }
    }

    private VBox createPackageCard(CoinPackage pkg, User user) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(180);
        card.setPadding(new Insets(20));

        // Bordure populaire
        if (pkg.isPopular()) {
            card.setStyle("""
                    -fx-background-color: linear-gradient(135deg, #1a1a2e, #1e1e3a);
                    -fx-background-radius: 12;
                    -fx-border-color: #667eea;
                    -fx-border-width: 2;
                    -fx-border-radius: 12;
                    """);
        } else {
            card.setStyle("""
                    -fx-background-color: rgba(255,255,255,0.04);
                    -fx-background-radius: 12;
                    -fx-border-color: rgba(255,255,255,0.08);
                    -fx-border-width: 1;
                    -fx-border-radius: 12;
                    """);
        }

        // Badge "MOST POPULAR"
        if (pkg.isPopular()) {
            Label badge = new Label("⭐ MOST POPULAR");
            badge.setStyle("""
                    -fx-background-color: linear-gradient(90deg,#667eea,#764ba2);
                    -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold;
                    -fx-background-radius: 20; -fx-padding: 3 10 3 10;
                    """);
            card.getChildren().add(badge);
        }

        // Icône + nombre de coins
        Label coinsIcon = new Label("🪙");
        coinsIcon.setStyle("-fx-font-size: 32;");

        Label coinsCount = new Label(String.valueOf(pkg.getCoins()));
        coinsCount.setStyle("-fx-font-size: 36; -fx-font-weight: 900; -fx-text-fill: white;");

        Label coinsWord = new Label("COINS");
        coinsWord.setStyle("-fx-font-size: 12; -fx-text-fill: #f6d860; -fx-font-weight: bold;");

        // Nom du pack
        Label packLabel = new Label(pkg.getLabel());
        packLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #c0c0d0; -fx-font-weight: 600;");

        // Prix
        Label priceLabel = new Label(String.format("%.2f €", pkg.getPriceInEuros()));
        priceLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: white;");

        // Coût par coin
        Label perCoin = new Label(String.format("≈ %.1f cts/coin", pkg.getCentPerCoin()));
        perCoin.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

        // Bouton Buy
        Button buyBtn = new Button(pkg.isPopular() ? "⚡ Buy Now" : "🛒 Buy Now");
        buyBtn.setPrefWidth(140);
        if (pkg.isPopular()) {
            buyBtn.setStyle("""
                    -fx-background-color: linear-gradient(135deg,#667eea,#764ba2);
                    -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;
                    -fx-background-radius: 20; -fx-padding: 10 20 10 20; -fx-cursor: hand;
                    """);
        } else {
            buyBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;
                    -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20;
                    -fx-background-radius: 20; -fx-padding: 10 20 10 20; -fx-cursor: hand;
                    """);
        }

        final int coinsToBy = pkg.getCoins();
        buyBtn.setOnAction(e -> handleBuyPackage(user, pkg));

        card.getChildren().addAll(coinsIcon, coinsCount, coinsWord, packLabel, priceLabel, perCoin, buyBtn);
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  ACHAT — miroir de checkout() + success() Symfony
    // ══════════════════════════════════════════════════════════

    /**
     * Simule l'achat d'un package.
     * En production : ouvrir un navigateur vers l'URL Stripe checkout.
     *
     * Pour une app desktop, deux approches possibles :
     *   1. Ouvrir l'URL Stripe dans le navigateur par défaut
     *      → java.awt.Desktop.getDesktop().browse(new URI(stripeCheckoutUrl))
     *   2. Appeler l'API REST Symfony /coins/checkout/{coins} en HTTP
     *      et récupérer l'URL de redirection.
     *
     * Ici on simule une confirmation directe (mode démo).
     */


    private void handleBuyPackage(User user, CoinPackage pkg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Purchase");
        confirm.setHeaderText(pkg.getLabel() + " — " + pkg.getCoins() + " coins");
        confirm.setContentText(String.format(
                "You will be redirected to Stripe to pay %.2f €.\n\nContinue?",
                pkg.getPriceInEuros()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    CheckoutResult checkout = stripeService.createCheckoutSession(user, pkg);

                    // Ouvrir le navigateur
                    stripeService.openCheckoutInBrowser(checkout.url);

                    showFlash("info", "🌐 Complete payment in your browser. Waiting for confirmation...");

                    // ✅ Démarrer le polling en arrière-plan
                    startPaymentPolling(user, pkg, checkout.sessionId);

                } catch (Exception e) {
                    showFlash("error", "❌ Stripe error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Poll Stripe toutes les 3 secondes pendant 5 minutes max.
     * Dès que le paiement est confirmé → crédite les coins + refresh UI.
     */
    private void startPaymentPolling(User user, CoinPackage pkg, String sessionId) {
        Thread pollThread = new Thread(() -> {
            int  maxAttempts = 100; // 100 × 3s = 5 minutes
            int  attempts    = 0;

            while (attempts < maxAttempts) {
                try {
                    Thread.sleep(3000); // attendre 3 secondes
                    attempts++;

                    System.out.println("[Polling] Vérification paiement — tentative " + attempts);

                    if (paymentChecker.isSessionPaid(sessionId)) {
                        // ✅ Paiement confirmé !
                        if (!coinService.isPurchaseAlreadyProcessed(sessionId)) {
                            coinService.recordPurchase(
                                    user, pkg.getCoins(),
                                    pkg.getPriceInEuros(), sessionId
                            );
                        }

                        // Mettre à jour l'UI sur le JavaFX thread
                        Platform.runLater(() -> {
                            refreshBalance(user);
                            loadHistory(user);
                            showFlash("success",
                                    "🎉 " + pkg.getCoins() + " EyeTwin Coins added to your account!");
                        });

                        return; // Arrêter le polling
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    System.err.println("[Polling] Erreur : " + e.getMessage());
                }
            }

            // Timeout — paiement pas détecté
            Platform.runLater(() ->
                    showFlash("warning", "⏱ Payment not detected. Check your balance later.")
            );
        });

        pollThread.setDaemon(true); // s'arrête avec l'app
        pollThread.start();
    }


    // ══════════════════════════════════════════════════════════
    //  HISTORIQUE DES ACHATS
    // ══════════════════════════════════════════════════════════

    private void loadHistory(User user) {
        if (historyContainer == null) return;
        historyContainer.getChildren().clear();

        List<CoinPurchase> history = coinService.getPurchaseHistory(user);

        if (history.isEmpty()) {
            Label empty = new Label("No purchases yet.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13;");
            historyContainer.getChildren().add(empty);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (CoinPurchase purchase : history) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("""
                    -fx-background-color: rgba(255,255,255,0.04);
                    -fx-background-radius: 8;
                    -fx-border-color: rgba(255,255,255,0.06);
                    -fx-border-radius: 8;
                    """);

            Label coins = new Label("🪙 +" + purchase.getCoinsAmount());
            coins.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f6d860;");
            coins.setPrefWidth(100);

            Label price = new Label(purchase.getPricePaid() + " €");
            price.setStyle("-fx-font-size: 13; -fx-text-fill: white;");
            price.setPrefWidth(80);

            String statusColor = "completed".equals(purchase.getStatus()) ? "#48bb78" : "#fc8181";
            Label status = new Label(purchase.getStatus().toUpperCase());
            status.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");
            status.setPrefWidth(90);

            Label date = new Label(purchase.getCompletedAt() != null
                    ? fmt.format(purchase.getCompletedAt()) : "—");
            date.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

            row.getChildren().addAll(coins, price, status, date);
            historyContainer.getChildren().add(row);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  FLASH MESSAGES  (miroir de addFlash() Symfony)
    // ══════════════════════════════════════════════════════════

    /**
     * Affiche un message flash dans la vue.
     * Types : "success" | "info" | "warning" | "error"
     */
    private void showFlash(String type, String message) {
        if (flashLabel == null) return;

        String bg = switch (type) {
            case "success" -> "rgba(72,187,120,0.15)";
            case "warning" -> "rgba(237,137,54,0.15)";
            case "error"   -> "rgba(245,101,101,0.15)";
            default        -> "rgba(102,126,234,0.15)";
        };
        String border = switch (type) {
            case "success" -> "#48bb78";
            case "warning" -> "#ed8936";
            case "error"   -> "#f56565";
            default        -> "#667eea";
        };

        flashLabel.setText(message);

        if (flashBox != null) {
            flashBox.setStyle(String.format("""
                    -fx-background-color: %s;
                    -fx-border-color: %s;
                    -fx-border-width: 0 0 0 4;
                    -fx-background-radius: 8; -fx-padding: 12 16 12 16;
                    """, bg, border));
            flashBox.setVisible(true);
            flashBox.setManaged(true);
        }
    }

    public void hideFlash() {
        if (flashBox != null) {
            flashBox.setVisible(false);
            flashBox.setManaged(false);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  NAVBAR — mise à jour des coins
    // ══════════════════════════════════════════════════════════

    private void updateNavbarCoins(User user) {
        // La navbar est un composant inclus — on cherche son contrôleur
        // via le SceneGraph si disponible
        if (balanceLabel == null) return;
        Scene scene = balanceLabel.getScene();
        if (scene == null) return;
        // Le NavbarController met à jour coinsNavLabel depuis son initialize()
        // Pour forcer une mise à jour : on peut dispatcher un event ou
        // recharger la page. Ici on met juste à jour le SessionManager.
        System.out.println("[CoinsController] Navbar coins → " + user.getCoinBalance());
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════

    @FXML
    public void goToProfile() {
        navigateTo("UserProfile.fxml");
    }

    @FXML
    public void goBack() {
        navigateTo("home.fxml");
    }

    private void navigateTo(String fxml) {
        String[] paths = {
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml
        };
        java.net.URL url = null;
        for (String path : paths) {
            url = getClass().getResource(path);
            if (url != null) break;
        }
        if (url == null) {
            System.err.println("[CoinsController] ❌ FXML introuvable : " + fxml);
            return;
        }
        try {
            Parent root  = FXMLLoader.load(url);
            Stage  stage = (Stage) (balanceLabel != null
                    ? balanceLabel.getScene().getWindow()
                    : null);
            if (stage == null) return;

            Scene newScene = new Scene(root, stage.getWidth(), stage.getHeight());
            Scene current  = stage.getScene();
            if (current != null) newScene.getStylesheets().addAll(current.getStylesheets());
            stage.setScene(newScene);

        } catch (IOException e) {
            System.err.println("[CoinsController] ❌ Erreur chargement : " + fxml);
            e.printStackTrace();
        }
    }
}
