package com.eyetwin.tools;

import com.eyetwin.entities.Community.Channel;

public class CommunityValidator {

    public static String validateChannel(Channel channel) {
        if (channel == null) return "Channel data is missing.";

        String nameError = validateChannelName(channel.getName());
        if (nameError != null) return nameError;

        String gameError = validateGame(channel.getGame());
        if (gameError != null) return gameError;

        String descriptionError = validateDescription(channel.getDescription());
        if (descriptionError != null) return descriptionError;

        String typeError = validateType(channel.getType());
        if (typeError != null) return typeError;

        String imageError = validateImageUrl(channel.getImageUrl());
        if (imageError != null) return imageError;

        return null;
    }

    public static String validateChannelName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Channel name is required.";
        }

        String v = value.trim();

        if (v.length() < 3 || v.length() > 100) {
            return "Channel name must be between 3 and 100 characters.";
        }

        if (!v.matches("[A-Za-z0-9 _-]+")) {
            return "Channel name can only contain letters, numbers, spaces, - and _.";
        }

        return null;
    }

    public static String validateGame(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Game is required.";
        }

        String v = value.trim();

        if (v.length() < 2 || v.length() > 100) {
            return "Game must be between 2 and 100 characters.";
        }

        return null;
    }

    public static String validateDescription(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String v = value.trim();

        if (v.length() > 500) {
            return "Description must not exceed 500 characters.";
        }

        return null;
    }

    public static String validateType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Channel type is required.";
        }

        String v = value.trim().toLowerCase();

        if (!Channel.TYPE_PUBLIC.equals(v) && !Channel.TYPE_PRIVATE.equals(v)) {
            return "Channel type must be public or private.";
        }

        return null;
    }

    public static String validateImageUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String v = value.trim();

        if (!(v.startsWith("http://") || v.startsWith("https://"))) {
            return "Image URL must start with http:// or https://";
        }

        if (v.length() > 255) {
            return "Image URL is too long.";
        }

        return null;
    }

    public static String validateMessageContent(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Message cannot be empty.";
        }

        String v = value.trim();

        if (v.length() > 1000) {
            return "Message must not exceed 1000 characters.";
        }

        return null;
    }
}