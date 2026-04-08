package com.eyetwin.entities;

public enum PaymentMethod {
    STRIPE("Stripe — Carte internationale (EUR)"),
    FLOUCI("Flouci — Wallet & Carte tunisienne (TND)");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}