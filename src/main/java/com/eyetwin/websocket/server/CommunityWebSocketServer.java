package com.eyetwin.websocket.server;

import com.eyetwin.websocket.model.SocketEnvelope;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CommunityWebSocketServer extends WebSocketServer {

    private final Gson gson = new Gson();

    private final ConcurrentMap<Integer, Set<WebSocket>> channelSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentMap<WebSocket, Integer> socketChannelMap = new ConcurrentHashMap<>();

    public CommunityWebSocketServer(int port) {
        super(new InetSocketAddress("127.0.0.1", port));
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("WebSocket connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        unregisterConnection(conn);
        System.out.println("WebSocket closed: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String rawMessage) {
        try {
            SocketEnvelope envelope = gson.fromJson(rawMessage, SocketEnvelope.class);

            if (envelope == null || envelope.getType() == null) {
                return;
            }

            switch (envelope.getType()) {
                case SocketEnvelope.TYPE_JOIN -> handleJoin(conn, envelope);
                case SocketEnvelope.TYPE_LEAVE -> unregisterConnection(conn);
                case SocketEnvelope.TYPE_NEW_MESSAGE -> broadcastToChannel(envelope.getChannelId(), rawMessage);
                default -> System.out.println("Unknown message type: " + envelope.getType());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            unregisterConnection(conn);
        }
    }

    @Override
    public void onStart() {
        setConnectionLostTimeout(30);
        System.out.println("Community WebSocket server started on ws://localhost:" + getPort());
    }

    private void handleJoin(WebSocket conn, SocketEnvelope envelope) {
        if (envelope.getChannelId() == null) {
            return;
        }

        unregisterConnection(conn);

        channelSubscribers
                .computeIfAbsent(envelope.getChannelId(), id -> ConcurrentHashMap.newKeySet())
                .add(conn);

        socketChannelMap.put(conn, envelope.getChannelId());

        System.out.println(envelope.getUserEmail() + " joined channel " + envelope.getChannelId());
    }

    private void broadcastToChannel(Integer channelId, String rawMessage) {
        if (channelId == null) {
            return;
        }

        Set<WebSocket> subscribers = channelSubscribers.get(channelId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (WebSocket socket : subscribers) {
            if (socket != null && socket.isOpen()) {
                socket.send(rawMessage);
            }
        }
    }

    private void unregisterConnection(WebSocket conn) {
        Integer channelId = socketChannelMap.remove(conn);
        if (channelId == null) {
            return;
        }

        Set<WebSocket> subscribers = channelSubscribers.get(channelId);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(conn);

        if (subscribers.isEmpty()) {
            channelSubscribers.remove(channelId);
        }
    }
}