package com.eyetwin.interfaces;

import com.eyetwin.entities.StreamFeedback;
import java.util.List;

public interface IFeedbackRepository {
    void save(StreamFeedback feedback);
    StreamFeedback findById(int id);
    List<StreamFeedback> findByLiveStreamId(int liveStreamId);
    List<StreamFeedback> findUnprocessed();
    void markAsProcessed(int feedbackId);
}