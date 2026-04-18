package com.eyetwin.interfaces;

import com.eyetwin.entities.StreamFeedback;
import java.sql.SQLException;
import java.util.List;

public interface IFeedbackService {
    void              save(StreamFeedback feedback)          throws SQLException;
    StreamFeedback    findById(int id)                       throws SQLException;
    List<StreamFeedback> findByLiveStreamId(int streamId)   throws SQLException;
    List<StreamFeedback> findUnprocessed()                   throws SQLException;
    void              markAsProcessed(int feedbackId)        throws SQLException;
    void              processFeedback(StreamFeedback feedback) throws SQLException;
}