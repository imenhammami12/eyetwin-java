package com.eyetwin.entities;

public class CheckoutResult {
    public final String url;
    public final String sessionId;

    public CheckoutResult(String url, String sessionId) {
        this.url = url;
        this.sessionId = sessionId;
    }
}