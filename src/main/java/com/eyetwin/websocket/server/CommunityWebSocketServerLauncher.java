package com.eyetwin.websocket.server;

import com.eyetwin.websocket.ChatWebSocketConfig;

import java.util.concurrent.CountDownLatch;

public class CommunityWebSocketServerLauncher {

    public static void main(String[] args) throws Exception {
        CommunityWebSocketServer server = new CommunityWebSocketServer(ChatWebSocketConfig.SERVER_PORT);
        server.start();

        Thread.sleep(1000);

        System.out.println("Community WebSocket server started on " + ChatWebSocketConfig.SERVER_URL);

        new CountDownLatch(1).await();
    }
}