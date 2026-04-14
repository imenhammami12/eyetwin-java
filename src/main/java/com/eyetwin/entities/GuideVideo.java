package com.eyetwin.entities;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GuideVideo {

    private Integer id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnail;
    private String map = "All";

    private int likes = 0;
    private int views = 0;

    private String status = "pending"; // pending, approved, rejected

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    // Relations
    private User uploadedBy;
    private Game game;
    private Agent agent;

    private List<User> likedBy;

    // Constructor
    public GuideVideo() {
        this.createdAt = LocalDateTime.now();
        this.likes = 0;
        this.views = 0;
        this.status = "pending";
        this.map = "All";
        this.likedBy = new ArrayList<>();
    }

    // Getters & Setters

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void addLike() {
        this.likes++;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public void addView() {
        this.views++;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isApproved() {
        return "approved".equals(status);
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isRejected() {
        return "rejected".equals(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public List<User> getLikedBy() {
        return likedBy;
    }

    public void addLikedBy(User user) {
        if (!likedBy.contains(user)) {
            likedBy.add(user);
            likes++;
        }
    }

    public void removeLikedBy(User user) {
        if (likedBy.remove(user)) {
            likes = Math.max(0, likes - 1);
        }
    }

    public boolean isLikedByUser(User user) {
        return likedBy.contains(user);
    }
}