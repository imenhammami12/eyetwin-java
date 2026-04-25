package com.eyetwin.entities.Community;

import java.util.ArrayList;
import java.util.List;

public class MessageModerationResult {

    private final String originalContent;
    private final String maskedContent;
    private final List<String> matchedTerms;

    public MessageModerationResult(String originalContent, String maskedContent, List<String> matchedTerms) {
        this.originalContent = originalContent == null ? "" : originalContent;
        this.maskedContent = maskedContent == null ? "" : maskedContent;
        this.matchedTerms = matchedTerms == null ? new ArrayList<>() : matchedTerms;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getMaskedContent() {
        return maskedContent;
    }

    public List<String> getMatchedTerms() {
        return matchedTerms;
    }

    public boolean wasModified() {
        return !originalContent.equals(maskedContent);
    }

    public int getMatchedCount() {
        return matchedTerms == null ? 0 : matchedTerms.size();
    }

    public boolean isSevere() {
        return getMatchedCount() >= 2;
    }
}