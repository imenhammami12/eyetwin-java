package com.eyetwin.interfaces;

import com.eyetwin.entities.Tournoi;
import java.util.List;

public interface ITournoiService {
    void add(Tournoi tournoi);
    void update(Tournoi tournoi);
    void delete(int id);
    Tournoi getById(int id);
    List<Tournoi> getAll();
}
