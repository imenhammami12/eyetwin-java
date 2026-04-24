package com.eyetwin.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import com.eyetwin.entities.Tournoi;
import com.eyetwin.entities.Match;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.services.MatchServiceImpl;

import java.io.File;
import java.util.List;

public class TournoiDetailController {

    @FXML private Label     idLabel;
    @FXML private Label     lblNom;
    @FXML private Label     lblType;
    @FXML private Label     lblDateDebut;
    @FXML private Label     lblDateFin;
    @FXML private Label     lblDescription;
    @FXML private Label     lblPrix;
    @FXML private ImageView tournoiImage;
    @FXML private Label     imgPlaceholder;
    @FXML private VBox      matchesList;
    @FXML private Label     matchCountLabel;
    @FXML private Label     noMatchesLabel;

    private IMatchService matchService = new MatchServiceImpl();

    public void setTournoi(Tournoi t) {
        idLabel.setText("ID #" + t.getId());
        lblNom.setText(t.getNom() != null ? t.getNom() : "—");
        lblType.setText(t.getTypeTournoi() != null ? t.getTypeTournoi().toString() : "N/A");
        lblDateDebut.setText(t.getDateDebut() != null ? t.getDateDebut().toString() : "—");
        lblDateFin.setText(t.getDateFin()     != null ? t.getDateFin().toString()   : "—");
        lblDescription.setText(t.getDescription() != null ? t.getDescription() : "—");
        lblPrix.setText(t.getPrix() + " DT");

        // Load image
        if (t.getImage() != null && !t.getImage().isEmpty()) {
            try {
                File f = new File(t.getImage());
                if (f.exists()) {
                    tournoiImage.setImage(new Image(f.toURI().toString(), 598, 200, false, true));
                    tournoiImage.setVisible(true);
                    imgPlaceholder.setVisible(false);
                } else {
                    tournoiImage.setVisible(false);
                    imgPlaceholder.setVisible(true);
                }
            } catch (Exception e) {
                tournoiImage.setVisible(false);
                imgPlaceholder.setVisible(true);
            }
        } else {
            tournoiImage.setVisible(false);
            imgPlaceholder.setVisible(true);
        }

        loadMatches(t.getId());
    }

    private void loadMatches(int tournoiId) {
        System.out.println("[TournoiDetail] Loading matches for tournament ID: " + tournoiId);
        matchesList.getChildren().clear();
        List<Match> matches = matchService.getByTournoi(tournoiId);
        System.out.println("[TournoiDetail] Matches found: " + matches.size());
        
        if (matches.isEmpty()) {
            matchesList.getChildren().add(noMatchesLabel);
            matchCountLabel.setText("0 matches");
            return;
        }

        matchCountLabel.setText(matches.size() + " matches");
        for (Match m : matches) {
            matchesList.getChildren().add(createMatchRow(m));
        }
    }

    private HBox createMatchRow(Match m) {
        HBox row = new HBox(12);
        row.getStyleClass().add("detail-section");
        row.setStyle("-fx-padding: 10 15; -fx-background-color: #0b0b18;");
        row.setAlignment(Pos.CENTER_LEFT);

        Label teams = new Label(m.getEquipe1() + " vs " + m.getEquipe2());
        teams.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        teams.setPrefWidth(220);

        Label score = new Label(String.valueOf(m.getScore()));
        score.getStyleClass().add("card-badge");
        score.setStyle("-fx-font-size: 11px; -fx-min-width: 30; -fx-alignment: CENTER;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label("📅 " + m.getDateMatch());
        date.setStyle("-fx-text-fill: #555566; -fx-font-size: 11px;");

        row.getChildren().addAll(teams, score, spacer, date);
        return row;
    }

    @FXML void onClose(ActionEvent event) {
        ((Stage) lblNom.getScene().getWindow()).close();
    }
}
