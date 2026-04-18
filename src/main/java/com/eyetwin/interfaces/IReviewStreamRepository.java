package com.eyetwin.interfaces;

import com.eyetwin.entities.ReviewStream;
import java.util.List;

public interface IReviewStreamRepository {
    void save(ReviewStream review);
    ReviewStream findById(int id);
    List<ReviewStream> findByLiveStreamId(int liveStreamId);
    double getAverageRating(int liveStreamId);
}