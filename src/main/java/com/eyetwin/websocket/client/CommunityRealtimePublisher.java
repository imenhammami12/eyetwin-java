package com.eyetwin.websocket.client;

import com.eyetwin.websocket.ChatWebSocketConfig;
import com.eyetwin.websocket.model.SocketEnvelope;

import java.net.URI;

public class CommunityRealtimePublisher {

    public void publishNotificationChanged(int userId) {
        publish(SocketEnvelope.TYPE_NOTIFICATION_CHANGED, "notif:" + userId, null, userId);
    }

    public void publishAccessChanged(int channelId) {
        publish(SocketEnvelope.TYPE_ACCESS_CHANGED, "access:" + channelId, channelId, null);
    }

    public void publishCommunityChanged() {
        publish(SocketEnvelope.TYPE_COMMUNITY_CHANGED, "community:all", null, null);
    }

    private void publish(String type, String roomKey, Integer channelId, Integer userId) {
        CommunityWebSocketClient client = new CommunityWebSocketClient(
                URI.create(ChatWebSocketConfig.SERVER_URL),
                null
        );

        try {
            if (client.ensureConnected()) {
                client.publishSystemEvent(type, roomKey, channelId, userId);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                client.closeBlocking();
            } catch (Exception ignored) {
                client.close();
            }
        }
    }
}