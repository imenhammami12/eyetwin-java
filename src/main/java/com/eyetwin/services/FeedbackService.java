package com.eyetwin.services;

import com.eyetwin.entities.*;
import com.eyetwin.interfaces.IFeedbackRepository;
import com.eyetwin.interfaces.IReviewStreamRepository;  // ← renommé
import com.eyetwin.interfaces.IComplaintService;

public class FeedbackService {

    private final IFeedbackRepository    feedbackRepo;
    private final IReviewStreamRepository reviewRepo;   // ← renommé
    private final IComplaintService   complaintRepo;

    public FeedbackService(IFeedbackRepository feedbackRepo,
                           IReviewStreamRepository reviewRepo,
                           IComplaintService complaintRepo) {
        this.feedbackRepo  = feedbackRepo;
        this.reviewRepo    = reviewRepo;
        this.complaintRepo = complaintRepo;
    }

    public void processFeedback(StreamFeedback feedback) {
        if (feedback.isProcessed())
            throw new IllegalStateException("Feedback déjà traité : " + feedback.getId());

        feedback.inferFeedbackType();
        feedbackRepo.save(feedback);

        if (feedback.shouldGenerateReview()) {
            ReviewStream review = ReviewStream.fromFeedback(feedback);  // ← renommé
            reviewRepo.save(review);
            feedback.setGeneratedReview(review);
        }

        if (feedback.shouldGenerateComplaint()) {
            Complaint complaint = buildComplaint(feedback);
            try {
                complaintRepo.create(complaint);
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("Erreur save Complaint depuis feedback", e);
            }
            feedback.setGeneratedComplaint(complaint);
        }

        feedbackRepo.markAsProcessed(feedback.getId());
    }

    private Complaint buildComplaint(StreamFeedback fb) {
        Complaint c = new Complaint();
        c.setSubmittedBy(fb.getSpectator());
        c.setSubject("Feedback négatif – Stream : " + fb.getLiveStream().getTitle());
        c.setDescription(fb.getComment());
        c.setCategory(ComplaintCategory.OTHER);
        c.setPriority(switch (fb.getRating()) {
            case 1  -> ComplaintPriority.HIGH;
            case 2  -> ComplaintPriority.MEDIUM;
            default -> ComplaintPriority.LOW;
        });
        return c;
    }
}