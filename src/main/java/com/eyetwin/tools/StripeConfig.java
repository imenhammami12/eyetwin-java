package com.eyetwin.tools;

import java.io.InputStream;
import java.util.Properties;

public class StripeConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = StripeConfig.class
                .getResourceAsStream("/stripe.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("[StripeConfig] Erreur chargement : " + e.getMessage());
        }
    }

    public static String getSecretKey() {
        return props.getProperty("stripe.secret.key", "");
    }

    public static String getPublishableKey() {
        return props.getProperty("stripe.publishable.key", "");
    }

    public static String getWebhookSecret() {
        return props.getProperty("stripe.webhook.secret", "");
    }
}