package com.eyetwin.services;

import com.eyetwin.entities.CheckoutResult;
import com.eyetwin.entities.CoinPackage;
import com.eyetwin.tools.FlouciConfig;
import org.json.JSONObject;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FlouciService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Crée une session de paiement Flouci.
     * Montant en millimes (1 TND = 1000 millimes)
     */
    public CheckoutResult createPaymentSession(int userId, CoinPackage pkg) throws Exception {

        // Flouci travaille en millimes : on convertit les centimes EUR → millimes TND
        // Adapte le taux selon tes prix TND réels
        int amountMillimes = pkg.getPriceInCents() * 10; // ex: 500 centimes = 5000 millimes = 5 TND

        JSONObject body = new JSONObject();
        body.put("app_token",             FlouciConfig.getAppToken());
        body.put("app_secret",            FlouciConfig.getAppSecret());
        body.put("amount",                amountMillimes);
        body.put("accept_card",           true);
        body.put("session_timeout_secs",  1200); // 20 minutes
        body.put("success_link",          "https://eyetwin.com/payment-success");
        body.put("fail_link",             "https://eyetwin.com/payment-fail");
        body.put("developer_tracking_id", FlouciConfig.getTrackingId());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FlouciConfig.getApiUrl() + "/api/gateway/init/payment"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("[Flouci] Response: " + response.body());

        if (response.statusCode() != 200) {
            throw new Exception("Flouci API error " + response.statusCode()
                    + " : " + response.body());
        }

        JSONObject json = new JSONObject(response.body());

        if (!json.optBoolean("result", false)) {
            throw new Exception("Flouci payment init failed: " + response.body());
        }

        String paymentId  = json.getString("paymentInfo");
        String payUrl     = "https://developers.flouci.com/api/payment/" + paymentId;

        System.out.println("[Flouci] Session créée — paymentId=" + paymentId);
        return new CheckoutResult(payUrl, paymentId);
    }

    /**
     * Vérifie si le paiement est complété.
     */
    public boolean isPaymentCompleted(String paymentId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FlouciConfig.getApiUrl()
                            + "/api/gateway/payment-verify/" + paymentId))
                    .header("app_token", FlouciConfig.getAppToken())
                    .header("app_secret", FlouciConfig.getAppSecret())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return false;

            JSONObject json = new JSONObject(response.body());
            String status = json.optString("status", "");
            System.out.println("[Flouci] Statut paiement " + paymentId + " → " + status);

            return "SUCCESS".equalsIgnoreCase(status);

        } catch (Exception e) {
            System.err.println("[Flouci] Erreur vérification : " + e.getMessage());
            return false;
        }
    }

    public void openPaymentInBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }
}