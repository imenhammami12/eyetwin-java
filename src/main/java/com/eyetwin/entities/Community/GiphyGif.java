package com.eyetwin.entities.Community;

public class GiphyGif {
    private String id;
    private String title;
    private String previewUrl;
    private String sendUrl;

    public GiphyGif() {
    }

    public GiphyGif(String id, String title, String previewUrl, String sendUrl) {
        this.id = id;
        this.title = title;
        this.previewUrl = previewUrl;
        this.sendUrl = sendUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getSendUrl() {
        return sendUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public void setSendUrl(String sendUrl) {
        this.sendUrl = sendUrl;
    }
}