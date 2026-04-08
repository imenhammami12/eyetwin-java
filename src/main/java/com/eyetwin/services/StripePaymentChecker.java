package com.eyetwin.services;

import com.eyetwin.tools.StripeConfig;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;

public class StripePaymentChecker {

    static {
        Stripe.apiKey = StripeConfig.getSecretKey();
    }

    /**
     * Interroge Stripe pour savoir si la session a été payée.
     * Retourne true si payment_status == "paid"
     */
    public boolean isSessionPaid(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return "paid".equals(session.getPaymentStatus());
        } catch (Exception e) {
            System.err.println("[StripeChecker] Erreur : " + e.getMessage());
            return false;
        }
    }

    public String getSessionStatus(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return session.getPaymentStatus(); // "paid" | "unpaid" | "no_payment_required"
        } catch (Exception e) {
            return "error";
        }
    }
}