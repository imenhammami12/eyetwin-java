package com.eyetwin.services;

import com.eyetwin.config.ConfigLoader;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;

public class StripePaymentChecker {

    static {
        Stripe.apiKey = ConfigLoader.get("STRIPE_SECRET_KEY");
    }

    public boolean isSessionPaid(String sessionId) {
        try {
            return "paid".equals(Session.retrieve(sessionId).getPaymentStatus());
        } catch (Exception e) {
            System.err.println("[StripeChecker] Erreur : " + e.getMessage());
            return false;
        }
    }

    public String getSessionStatus(String sessionId) {
        try {
            return Session.retrieve(sessionId).getPaymentStatus();
        } catch (Exception e) {
            return "error";
        }
    }
}