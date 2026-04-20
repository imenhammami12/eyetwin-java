package com.eyetwin.websocket;

public final class ChatWebSocketConfig {

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 8887;
    public static final String SERVER_URL = "ws://" + SERVER_HOST + ":" + SERVER_PORT;

    private ChatWebSocketConfig() {
    }
}