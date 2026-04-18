package com.eyetwin.entities;

import java.time.LocalDateTime;

public class ReviewStream {

    private int           id;
    private User          author;
    private LiveStream    liveStream;
    private int           rating;
    private String        comment;
    private boolean       verified;
    private LocalDateTime createdAt;

    public ReviewStream() {
        this.createdAt = LocalDateTime.now();
        this.verified  = false;
    }

    public static ReviewStream fromFeedback(StreamFeedback fb) {
        ReviewStream r = new ReviewStream();
        r.setAuthor(fb.getSpectator());
        r.setLiveStream(fb.getLiveStream());
        r.setRating(fb.getRating());
        r.setComment(fb.getComment());
        r.setVerified(true);
        return r;
    }

    public int           getId()                        { return id; }
    public void          setId(int id)                  { this.id = id; }
    public User          getAuthor()                    { return author; }
    public void          setAuthor(User u)              { this.author = u; }
    public LiveStream    getLiveStream()                { return liveStream; }
    public void          setLiveStream(LiveStream ls)  { this.liveStream = ls; }
    public int           getRating()                    { return rating; }
    public void          setRating(int r)               { this.rating = Math.max(1, Math.min(5, r)); }
    public String        getComment()                   { return comment; }
    public void          setComment(String c)           { this.comment = c; }
    public boolean       isVerified()                   { return verified; }
    public void          setVerified(boolean v)         { this.verified = v; }
    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void          setCreatedAt(LocalDateTime d)  { this.createdAt = d; }
}