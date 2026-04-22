package com.eyetwin.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.eyetwin.entities.Match;
import com.eyetwin.entities.Tournoi;
import com.eyetwin.services.TournoiServiceImpl;
import com.eyetwin.interfaces.ITournoiService;

public class MatchDetailController {

    @FXML private Label idLabel;
    @FXML private Label lblMode;
    @FXML private Label lblEquipe1;
    @FXML private Label lblEquipe2;
    @FXML private Label lblScore;
    @FXML private Label lblDate;
    @FXML private Label lblLocalisation;
    @FXML private Label lblTournoi;
    @FXML private Label lblPrix;

    private ITournoiService tournoiService = new TournoiServiceImpl();

    public void setMatch(Match m) {
        idLabel.setText("ID #" + m.getId());
        lblMode.setText(m.getPlayMode() != null ? m.getPlayMode().toUpperCase() : "N/A");
        lblEquipe1.setText(m.getEquipe1() != null ? m.getEquipe1() : "Team 1");
        lblEquipe2.setText(m.getEquipe2() != null ? m.getEquipe2() : "Team 2");
        
        lblScore.setText(String.valueOf(m.getScore()));
        
        lblDate.setText(m.getDateMatch() != null ? m.getDateMatch().toString() : "—");
        lblLocalisation.setText(m.getLocalisation() != null ? m.getLocalisation() : "—");
        
        Tournoi t = tournoiService.getById(m.getTournoiId());
        lblTournoi.setText(t != null ? t.getNom() : "N/A");
        
        lblPrix.setText(m.getPrix() + " DT");
    }

    @FXML
    void onClose(ActionEvent event) {
        ((Stage) idLabel.getScene().getWindow()).close();
    }
}
