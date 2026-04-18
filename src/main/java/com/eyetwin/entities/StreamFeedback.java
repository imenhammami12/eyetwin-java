package com.eyetwin.entities;

import java.time.LocalDateTime;

public class StreamFeedback {

    public enum FeedbackType { REVIEW, COMPLAINT, BOTH }

    private int           id;
    private LiveStream    liveStream;
    private User          spectator;
    private int           rating;        // 1–5
    private String        comment;
    private FeedbackType  feedbackType;
    private boolean       processed;
    private LocalDateTime submittedAt;

    private Complaint generatedComplaint;
    private ReviewStream generatedReview;


    public ReviewStream getGeneratedReview()              { return generatedReview; }

    public void         setGeneratedReview(ReviewStream r){ this.generatedReview = r; }

    public StreamFeedback() {
        this.submittedAt  = LocalDateTime.now();
        this.processed    = false;
        this.feedbackType = FeedbackType.REVIEW;
    }

    public void inferFeedbackType() {
        if (rating <= 2)      this.feedbackType = FeedbackType.COMPLAINT;
        else if (rating == 3) this.feedbackType = FeedbackType.BOTH;
        else                  this.feedbackType = FeedbackType.REVIEW;
    }

    public boolean shouldGenerateReview()    {
        return feedbackType == FeedbackType.REVIEW    || feedbackType == FeedbackType.BOTH;
    }
    public boolean shouldGenerateComplaint() {
        return feedbackType == FeedbackType.COMPLAINT || feedbackType == FeedbackType.BOTH;
    }

    public int           getId()                              { return id; }
    public void          setId(int id)                        { this.id = id; }
    public LiveStream    getLiveStream()                      { return liveStream; }
    public void          setLiveStream(LiveStream ls)         { this.liveStream = ls; }
    public User          getSpectator()                       { return spectator; }
    public void          setSpectator(User u)                 { this.spectator = u; }
    public int           getRating()                          { return rating; }
    public void          setRating(int r)                     { this.rating = Math.max(1, Math.min(5, r)); }
    public String        getComment()                         { return comment; }
    public void          setComment(String c)                 { this.comment = c; }
    public FeedbackType  getFeedbackType()                    { return feedbackType; }
    public void          setFeedbackType(FeedbackType t)      { this.feedbackType = t; }
    public boolean       isProcessed()                        { return processed; }
    public void          setProcessed(boolean p)              { this.processed = p; }
    public LocalDateTime getSubmittedAt()                     { return submittedAt; }
    public void          setSubmittedAt(LocalDateTime d)      { this.submittedAt = d; }
    public Complaint     getGeneratedComplaint()              { return generatedComplaint; }
    public void          setGeneratedComplaint(Complaint c)   { this.generatedComplaint = c; }
}