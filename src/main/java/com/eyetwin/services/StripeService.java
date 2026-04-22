package com.eyetwin.services;

import com.eyetwin.entities.CheckoutResult;
import com.eyetwin.entities.CoinPackage;
import com.eyetwin.entities.User;
import com.eyetwin.tools.StripeConfig;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import com.eyetwin.entities.Tournoi;
import java.awt.Desktop;
import java.net.URI;

public class StripeService {

    static {
        Stripe.apiKey = StripeConfig.getSecretKey();
    }

    /**
     * Crée une Stripe Checkout Session et ouvre le navigateur.
     * Miroir de checkout() dans CoinController.php
     */
    public CheckoutResult createCheckoutSession(User user, CoinPackage pkg) throws Exception {

        // ✅ success_url pointe vers une page fictive — on ne dépend plus de Symfony
        String successUrl = "https://eyetwin.com/payment-success"
                + "?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl  = "https://eyetwin.com/payment-cancelled";

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) pkg.getPriceInCents())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("EyeTwin Coins — " + pkg.getLabel())
                                                                .setDescription(pkg.getCoins() + " EyeTwin Coins")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("user_id",      String.valueOf(user.getId()))
                .putMetadata("coins_amount", String.valueOf(pkg.getCoins()))
                .build();

        Session session = Session.create(params);

        // Retourner l'URL ET le sessionId pour polling
        return new CheckoutResult(session.getUrl(), session.getId());
    }

    /**
     * Crée une Stripe Checkout Session pour l'inscription à un tournoi.
     */
    public CheckoutResult createTournamentCheckoutSession(User user, Tournoi tournoi) throws Exception {

        String successUrl = "https://eyetwin.com/payment-success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl  = "https://eyetwin.com/payment-cancelled";

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) (tournoi.getPrix() * 100)) // Prix en cents
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Inscription Tournoi — " + tournoi.getNom())
                                                                .setDescription("Accès au tournoi " + tournoi.getNom() + " sur EyeTwin")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("user_id",    String.valueOf(user.getId()))
                .putMetadata("tournoi_id", String.valueOf(tournoi.getId()))
                .putMetadata("type",       "tournament_registration")
                .build();

        Session session = Session.create(params);

        return new CheckoutResult(session.getUrl(), session.getId());
    }

    /**
     * Ouvre l'URL Stripe dans le navigateur par défaut.
     */
    public void openCheckoutInBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            // Fallback Linux/headless
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }
}