package com.eyetwin.entities;

/**
 * Représente un package de coins disponible à l'achat.
 * Miroir de COIN_PACKAGES dans CoinController.php
 */
public class CoinPackage {

    private int coins;
    private int priceInCents; // prix en centimes (comme Stripe)
    private String label;
    private boolean popular;

    public CoinPackage(int coins, int priceInCents, String label, boolean popular) {
        this.coins        = coins;
        this.priceInCents = priceInCents;
        this.label        = label;
        this.popular      = popular;
    }

    // ── Getters ────────────────────────────────────────────────

    public int getCoins() { return coins; }
    public int getPriceInCents() { return priceInCents; }
    public String getLabel() { return label; }
    public boolean isPopular() { return popular; }

    /** Prix en euros (double pour l'affichage) */
    public double getPriceInEuros() {
        return priceInCents / 100.0;
    }

    /** Coût par coin en centimes */
    public double getCentPerCoin() {
        return (double) priceInCents / coins;
    }

    @Override
    public String toString() {
        return label + " — " + coins + " coins @ " + getPriceInEuros() + " €";
    }
}