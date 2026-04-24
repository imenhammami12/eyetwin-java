package com.eyetwin.interfaces;

import com.eyetwin.entities.TournoiInscription;
import java.util.List;

public interface ITournoiInscriptionService {
    void add(TournoiInscription inscription);
    void updateStatus(int id, String status);
    void updateStatusBySession(String sessionId, String status);
    TournoiInscription getById(int id);
    TournoiInscription getBySession(String sessionId);
    List<TournoiInscription> getByUser(int userId);
    List<TournoiInscription> getByTournoi(int tournoiId);
    boolean isUserRegistered(int userId, int tournoiId);
}
