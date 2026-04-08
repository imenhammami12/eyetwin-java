package com.eyetwin.services;

import com.eyetwin.entities.Complaint;

public class ComplaintManager {

    public boolean validate(Complaint complaint) {
        String subject = complaint.getSubject() == null ? "" : complaint.getSubject().trim();
        String description = complaint.getDescription() == null ? "" : complaint.getDescription().trim();

        if (subject.isEmpty())
            throw new IllegalArgumentException("Subject is required.");
        if (subject.length() < 5)
            throw new IllegalArgumentException("Subject must contain at least 5 characters.");
        if (description.length() < 10)
            throw new IllegalArgumentException("Description must contain at least 10 characters.");
        if (complaint.getCategory() == null)
            throw new IllegalArgumentException("Category is required.");

        return true;
    }
}