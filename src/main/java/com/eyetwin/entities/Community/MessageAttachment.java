package com.eyetwin.entities.Community;

public class MessageAttachment {
    private int id;
    private String originalName;
    private String storedName;
    private String mimeType;
    private int size;
    private int messageId;
    private String url;
    private String publicId;
    private String cloudResourceType;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getCloudResourceType() {
        return cloudResourceType;
    }

    public void setCloudResourceType(String cloudResourceType) {
        this.cloudResourceType = cloudResourceType;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }
}