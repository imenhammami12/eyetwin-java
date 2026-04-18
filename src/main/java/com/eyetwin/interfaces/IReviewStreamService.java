package com.eyetwin.interfaces;

import com.eyetwin.entities.ReviewStream;
import java.sql.SQLException;
import java.util.List;

public interface IReviewStreamService {
    void               save(ReviewStream review)                    throws SQLException;
    ReviewStream       findById(int id)                             throws SQLException;
    List<ReviewStream> findByLiveStreamId(int streamId)             throws SQLException;
    List<ReviewStream> findByCoachId(int coachId)                   throws SQLException; // ← nouveau
    double             getAverageRating(int streamId)               throws SQLException;
    double             getGlobalAverageRating(int coachId)          throws SQLException; // ← nouveau
}