package com.eyetwin.websocket.client;

import com.eyetwin.websocket.model.SocketEnvelope;

public interface ChatSocketListener {

    void onConnected();

    void onMessageReceived(SocketEnvelope envelope);

    void onDisconnected(String reason);

    void onError(Exception ex);
}