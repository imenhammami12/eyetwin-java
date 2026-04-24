package com.eyetwin.interfaces;

import com.eyetwin.entities.Match;
import java.util.List;

public interface IMatchService {
    void add(Match match);
    void update(Match match);
    void delete(int id);
    Match getById(int id);
    List<Match> getAll();
    List<Match> getByTournoi(int tournoiId);
}
