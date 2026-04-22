package com.eyetwin.entities;

import java.time.LocalDateTime;

/**
 * CoachApplication — miroir de l'entité Symfony CoachApplication
 * Version enrichie avec l'objet User complet (pour l'affichage admin)
 */
public class CoachApplication {

    private int               id;
    private int               userId;
    private User              user;           // chargé par JOIN dans le DAO
    private ApplicationStatus status         = ApplicationStatus.PENDING;
    private String            certifications;
    private String            experience;
    private String            cvFile;
    private String            documents;
    private LocalDateTime     submittedAt;
    private LocalDateTime     reviewedAt;
    private String            reviewComment;

    public CoachApplication() {
        this.submittedAt = LocalDateTime.now();
        this.status      = ApplicationStatus.PENDING;
    }

    // ── Helpers (miroir Symfony) ──────────────────────────────────

    public void approve(String comment) {
        this.status        = ApplicationStatus.APPROVED;
        this.reviewedAt    = LocalDateTime.now();
        this.reviewComment = comment;
        if (this.user != null) {
            String json = this.user.getRolesJson();
            if (json != null && !json.contains("ROLE_COACH")) {
                this.user.setRolesJson(json.replace("]", ",\"ROLE_COACH\"]"));
            }
        }
    }

    public void reject(String comment) {
        this.status        = ApplicationStatus.REJECTED;
        this.reviewedAt    = LocalDateTime.now();
        this.reviewComment = comment;
    }

    public boolean isPending()    { return status == ApplicationStatus.PENDING; }
    public boolean isApproved()   { return status == ApplicationStatus.APPROVED; }
    public boolean isRejected()   { return status == ApplicationStatus.REJECTED; }
    public boolean isUnderReview(){ return status == ApplicationStatus.UNDER_REVIEW; }
    public boolean canBeReviewed(){ return status == ApplicationStatus.PENDING || status == ApplicationStatus.UNDER_REVIEW; }

    // ── Getters / Setters ─────────────────────────────────────────

    public int               getId()                           { return id; }
    public void              setId(int id)                     { this.id = id; }

    public int               getUserId()                       { return userId; }
    public void              setUserId(int userId)             { this.userId = userId; }

    public User              getUser()                         { return user; }
    public void              setUser(User user)                { this.user = user; if (user != null) this.userId = user.getId(); }

    public ApplicationStatus getStatus()                       { return status; }
    public void              setStatus(ApplicationStatus s)    { this.status = s; }

    public String            getCertifications()               { return certifications; }
    public void              setCertifications(String c)       { this.certifications = c; }

    public String            getExperience()                   { return experience; }
    public void              setExperience(String e)           { this.experience = e; }

    public String            getCvFile()                       { return cvFile; }
    public void              setCvFile(String cvFile)          { this.cvFile = cvFile; }

    public String            getDocuments()                    { return documents; }
    public void              setDocuments(String documents)    { this.documents = documents; }

    public LocalDateTime     getSubmittedAt()                  { return submittedAt; }
    public void              setSubmittedAt(LocalDateTime d)   { this.submittedAt = d; }

    public LocalDateTime     getReviewedAt()                   { return reviewedAt; }
    public void              setReviewedAt(LocalDateTime d)    { this.reviewedAt = d; }

    public String            getReviewComment()                { return reviewComment; }
    public void              setReviewComment(String c)        { this.reviewComment = c; }
}