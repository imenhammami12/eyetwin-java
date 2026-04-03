package com.eyetwin.entities;

import java.time.LocalDateTime;

/**
 * CoachApplication — miroir de l'entité Symfony CoachApplication
 */
public class CoachApplication {

    private int             id;
    private int             userId;
    private ApplicationStatus status = ApplicationStatus.PENDING;
    private String          certifications;
    private String          experience;
    private String          cvFile;           // nullable
    private LocalDateTime   submittedAt;
    private LocalDateTime   reviewedAt;       // nullable
    private String          reviewComment;    // nullable

    public CoachApplication() {
        this.submittedAt = LocalDateTime.now();
        this.status      = ApplicationStatus.PENDING;
    }

    // ── Getters / Setters ──

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public ApplicationStatus getStatus()                        { return status; }
    public void setStatus(ApplicationStatus status)             { this.status = status; }

    public String getCertifications()                           { return certifications; }
    public void setCertifications(String certifications)        { this.certifications = certifications; }

    public String getExperience()                               { return experience; }
    public void setExperience(String experience)                { this.experience = experience; }

    public String getCvFile()                                   { return cvFile; }
    public void setCvFile(String cvFile)                        { this.cvFile = cvFile; }

    public LocalDateTime getSubmittedAt()                       { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt)       { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt()                        { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt)         { this.reviewedAt = reviewedAt; }

    public String getReviewComment()                            { return reviewComment; }
    public void setReviewComment(String reviewComment)          { this.reviewComment = reviewComment; }
}
