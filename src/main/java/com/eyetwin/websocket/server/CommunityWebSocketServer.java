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

    private final ConcurrentMap<String, Set<WebSocket>> roomSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentMap<WebSocket, Set<String>> socketRooms = new ConcurrentHashMap<>();

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
                case SocketEnvelope.TYPE_JOIN, SocketEnvelope.TYPE_SUBSCRIBE -> {
                    String roomKey = resolveRoomForSubscribe(envelope);
                    if (roomKey != null) {
                        subscribeConnection(conn, roomKey);
                    }
                }

                case SocketEnvelope.TYPE_LEAVE, SocketEnvelope.TYPE_UNSUBSCRIBE -> {
                    String roomKey = resolveRoomForSubscribe(envelope);
                    if (roomKey != null) {
                        unsubscribeConnection(conn, roomKey);
                    } else {
                        unregisterConnection(conn);
                    }
                }

                case SocketEnvelope.TYPE_NEW_MESSAGE,
                     SocketEnvelope.TYPE_EDIT_MESSAGE,
                     SocketEnvelope.TYPE_DELETE_MESSAGE,
                     SocketEnvelope.TYPE_REACTION_CHANGED,
                     SocketEnvelope.TYPE_NOTIFICATION_CHANGED,
                     SocketEnvelope.TYPE_ACCESS_CHANGED,
                     SocketEnvelope.TYPE_COMMUNITY_CHANGED -> {
                    String roomKey = resolveRoomForPublish(envelope);
                    if (roomKey != null) {
                        broadcastToRoom(roomKey, rawMessage);
                    }
                }

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

    private String resolveRoomForSubscribe(SocketEnvelope envelope) {
        if (envelope.getRoomKey() != null && !envelope.getRoomKey().isBlank()) {
            return envelope.getRoomKey().trim();
        }
        if (envelope.getChannelId() != null) {
            return "chat:" + envelope.getChannelId();
        }
        return null;
    }

    private String resolveRoomForPublish(SocketEnvelope envelope) {
        if (envelope.getRoomKey() != null && !envelope.getRoomKey().isBlank()) {
            return envelope.getRoomKey().trim();
        }

        return switch (envelope.getType()) {
            case SocketEnvelope.TYPE_NEW_MESSAGE,
                 SocketEnvelope.TYPE_EDIT_MESSAGE,
                 SocketEnvelope.TYPE_DELETE_MESSAGE ->
                    envelope.getChannelId() == null ? null : "chat:" + envelope.getChannelId();

            case SocketEnvelope.TYPE_ACCESS_CHANGED ->
                    envelope.getChannelId() == null ? null : "access:" + envelope.getChannelId();

            case SocketEnvelope.TYPE_NOTIFICATION_CHANGED ->
                    envelope.getUserId() == null ? null : "notif:" + envelope.getUserId();

            case SocketEnvelope.TYPE_COMMUNITY_CHANGED -> "community:all";

            default -> null;
        };
    }

    private void subscribeConnection(WebSocket conn, String roomKey) {
        roomSubscribers
                .computeIfAbsent(roomKey, id -> ConcurrentHashMap.newKeySet())
                .add(conn);

        socketRooms
                .computeIfAbsent(conn, id -> ConcurrentHashMap.newKeySet())
                .add(roomKey);

        System.out.println("Subscribed " + conn.getRemoteSocketAddress() + " to " + roomKey);
    }

    private void unsubscribeConnection(WebSocket conn, String roomKey) {
        Set<WebSocket> subscribers = roomSubscribers.get(roomKey);
        if (subscribers != null) {
            subscribers.remove(conn);
            if (subscribers.isEmpty()) {
                roomSubscribers.remove(roomKey);
            }
        }

        Set<String> rooms = socketRooms.get(conn);
        if (rooms != null) {
            rooms.remove(roomKey);
            if (rooms.isEmpty()) {
                socketRooms.remove(conn);
            }
        }
    }

    private void broadcastToRoom(String roomKey, String rawMessage) {
        Set<WebSocket> subscribers = roomSubscribers.get(roomKey);
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
        Set<String> rooms = socketRooms.remove(conn);
        if (rooms == null || rooms.isEmpty()) {
            return;
        }

        for (String roomKey : rooms) {
            Set<WebSocket> subscribers = roomSubscribers.get(roomKey);
            if (subscribers == null) continue;

            subscribers.remove(conn);
            if (subscribers.isEmpty()) {
                roomSubscribers.remove(roomKey);
            }
        }
    }
}