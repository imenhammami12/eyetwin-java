//package com.eyetwin.websocket.client;
//
//import com.eyetwin.websocket.model.SocketEnvelope;
//import com.google.gson.Gson;
//import org.java_websocket.client.WebSocketClient;
//import org.java_websocket.handshake.ServerHandshake;
//
//import java.net.URI;
//import java.time.LocalDateTime;
//
//import java.util.concurrent.TimeUnit;
//
//public class CommunityWebSocketClient extends WebSocketClient {
//
//    private final Gson gson = new Gson();
//    private final ChatSocketListener listener;
//
//    private SocketEnvelope pendingJoinEnvelope;
//
//    public CommunityWebSocketClient(URI serverUri, ChatSocketListener listener) {
//        super(serverUri);
//        this.listener = listener;
//    }
//
//    @Override
//    public void onOpen(ServerHandshake handshakeData) {
////        if (pendingJoinEnvelope != null) {
////            send(gson.toJson(pendingJoinEnvelope));
////        }
//
//        if (listener != null) {
//            listener.onConnected();
//        }
//    }
//
//    @Override
//    public void onMessage(String message) {
//        try {
//            SocketEnvelope envelope = gson.fromJson(message, SocketEnvelope.class);
//            if (listener != null && envelope != null) {
//                listener.onMessageReceived(envelope);
//            }
//        } catch (Exception ex) {
//            if (listener != null) {
//                listener.onError(ex);
//            }
//        }
//    }
//
//    @Override
//    public void onClose(int code, String reason, boolean remote) {
//        if (listener != null) {
//            listener.onDisconnected(reason);
//        }
//    }
//
//    @Override
//    public void onError(Exception ex) {
//        if (listener != null) {
//            listener.onError(ex);
//        }
//    }
//
////    public void connectAndJoin(int channelId, int userId, String userName, String userEmail) {
////        pendingJoinEnvelope = buildEnvelope(
////                SocketEnvelope.TYPE_JOIN,
////                channelId,
////                userId,
////                userName,
////                userEmail,
////                null
////        );
////
////        if (isOpen()) {
////            send(gson.toJson(pendingJoinEnvelope));
////            return;
////        }
////
////        if (isClosed()) {
////            connect();
////        }else if (!isOpen() && isClosing()){
////            connect();
////        }
////    }
//
//    public boolean connectAndJoin(int channelId, int userId, String userName, String userEmail) {
//        pendingJoinEnvelope = buildEnvelope(
//                SocketEnvelope.TYPE_JOIN,
//                channelId,
//                userId,
//                userName,
//                userEmail,
//                null
//        );
//
//        try {
//            if (!isOpen()) {
//                boolean connected;
//
//                if (isClosed()) {
//                    connected = reconnectBlocking();
//                } else {
//                    connected = connectBlocking(3, TimeUnit.SECONDS);
//                }
//
//                if (!connected || !isOpen()) {
//                    return false;
//                }
//            }
//
//            send(gson.toJson(pendingJoinEnvelope));
//            return true;
//
//        } catch (InterruptedException ex) {
//            Thread.currentThread().interrupt();
//
//            if (listener != null) {
//                listener.onError(ex);
//            }
//
//            return false;
//        }
//    }
//
//    public void leaveChannel(int channelId, int userId, String userName, String userEmail) {
//        if (!isOpen()) {
//            return;
//        }
//
//        SocketEnvelope envelope = buildEnvelope(
//                SocketEnvelope.TYPE_LEAVE,
//                channelId,
//                userId,
//                userName,
//                userEmail,
//                null
//        );
//
//        send(gson.toJson(envelope));
//    }
//
//    public void publishMessage(int channelId, int userId, String userName, String userEmail, String content) {
//        if (!isOpen()) {
//            return;
//        }
//
//        SocketEnvelope envelope = buildEnvelope(
//                SocketEnvelope.TYPE_NEW_MESSAGE,
//                channelId,
//                userId,
//                userName,
//                userEmail,
//                content
//        );
//
//        send(gson.toJson(envelope));
//    }
//
//    private SocketEnvelope buildEnvelope(
//            String type,
//            int channelId,
//            int userId,
//            String userName,
//            String userEmail,
//            String content
//    ) {
//        SocketEnvelope envelope = new SocketEnvelope();
//        envelope.setType(type);
//        envelope.setChannelId(channelId);
//        envelope.setUserId(userId);
//        envelope.setUserName(userName);
//        envelope.setUserEmail(userEmail);
//        envelope.setContent(content);
//        envelope.setSentAt(LocalDateTime.now().toString());
//        return envelope;
//    }
//
//    public void publishEditEvent(int channelId, int userId, String userName, String userEmail) {
//        if (!isOpen()) {
//            return;
//        }
//
//        SocketEnvelope envelope = buildEnvelope(
//                SocketEnvelope.TYPE_EDIT_MESSAGE,
//                channelId,
//                userId,
//                userName,
//                userEmail,
//                null
//        );
//
//        send(gson.toJson(envelope));
//    }
//
//    public void publishDeleteEvent(int channelId, int userId, String userName, String userEmail) {
//        if (!isOpen()) {
//            return;
//        }
//
//        SocketEnvelope envelope = buildEnvelope(
//                SocketEnvelope.TYPE_DELETE_MESSAGE,
//                channelId,
//                userId,
//                userName,
//                userEmail,
//                null
//        );
//
//        send(gson.toJson(envelope));
//    }
//}

package com.eyetwin.websocket.client;

import com.eyetwin.websocket.model.SocketEnvelope;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class CommunityWebSocketClient extends WebSocketClient {

    private final Gson gson = new Gson();
    private final ChatSocketListener listener;

    public CommunityWebSocketClient(URI serverUri, ChatSocketListener listener) {
        super(serverUri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            SocketEnvelope envelope = gson.fromJson(message, SocketEnvelope.class);
            if (listener != null && envelope != null) {
                listener.onMessageReceived(envelope);
            }
        } catch (Exception ex) {
            if (listener != null) {
                listener.onError(ex);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    @Override
    public void onError(Exception ex) {
        if (listener != null) {
            listener.onError(ex);
        }
    }

    public boolean ensureConnected() {
        try {
            if (isOpen()) {
                return true;
            }

            boolean connected;
            if (isClosed() || isClosing()) {
                connected = reconnectBlocking();
            } else {
                connected = connectBlocking(3, TimeUnit.SECONDS);
            }

            return connected && isOpen();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            if (listener != null) {
                listener.onError(ex);
            }
            return false;
        }
    }

    public boolean connectAndJoin(int channelId, int userId, String userName, String userEmail) {
        if (!ensureConnected()) {
            return false;
        }

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_JOIN,
                "chat:" + channelId,
                channelId,
                userId,
                userName,
                userEmail,
                null
        );

        send(gson.toJson(envelope));
        return true;
    }

    public void leaveChannel(int channelId, int userId, String userName, String userEmail) {
        if (!ensureConnected()) {
            return;
        }

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_LEAVE,
                "chat:" + channelId,
                channelId,
                userId,
                userName,
                userEmail,
                null
        );

        send(gson.toJson(envelope));
    }

    public void subscribeRoom(String roomKey) {
        if (roomKey == null || roomKey.isBlank()) return;
        if (!ensureConnected()) return;

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_SUBSCRIBE,
                roomKey,
                null,
                null,
                null,
                null,
                null
        );

        send(gson.toJson(envelope));
    }

    public void unsubscribeRoom(String roomKey) {
        if (roomKey == null || roomKey.isBlank()) return;
        if (!ensureConnected()) return;

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_UNSUBSCRIBE,
                roomKey,
                null,
                null,
                null,
                null,
                null
        );

        send(gson.toJson(envelope));
    }

    public void publishMessage(int channelId, int userId, String userName, String userEmail, String content) {
        if (!ensureConnected()) {
            return;
        }

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_NEW_MESSAGE,
                "chat:" + channelId,
                channelId,
                userId,
                userName,
                userEmail,
                content
        );

        send(gson.toJson(envelope));
    }

    public void publishEditEvent(int channelId, int userId, String userName, String userEmail) {
        if (!ensureConnected()) {
            return;
        }

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_EDIT_MESSAGE,
                "chat:" + channelId,
                channelId,
                userId,
                userName,
                userEmail,
                null
        );

        send(gson.toJson(envelope));
    }

    public void publishDeleteEvent(int channelId, int userId, String userName, String userEmail) {
        if (!ensureConnected()) {
            return;
        }

        SocketEnvelope envelope = buildEnvelope(
                SocketEnvelope.TYPE_DELETE_MESSAGE,
                "chat:" + channelId,
                channelId,
                userId,
                userName,
                userEmail,
                null
        );

        send(gson.toJson(envelope));
    }

    public void publishSystemEvent(String type, String roomKey, Integer channelId, Integer userId) {
        if (!ensureConnected()) {
            return;
        }

        SocketEnvelope envelope = buildEnvelope(
                type,
                roomKey,
                channelId,
                userId,
                null,
                null,
                null
        );

        send(gson.toJson(envelope));
    }

    private SocketEnvelope buildEnvelope(
            String type,
            String roomKey,
            Integer channelId,
            Integer userId,
            String userName,
            String userEmail,
            String content
    ) {
        SocketEnvelope envelope = new SocketEnvelope();
        envelope.setType(type);
        envelope.setRoomKey(roomKey);
        envelope.setChannelId(channelId);
        envelope.setUserId(userId);
        envelope.setUserName(userName);
        envelope.setUserEmail(userEmail);
        envelope.setContent(content);
        envelope.setSentAt(LocalDateTime.now().toString());
        return envelope;
    }
}