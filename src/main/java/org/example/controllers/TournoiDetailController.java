package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.entities.Tournoi;

import java.io.File;

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
                    return;
                }
            } catch (Exception ignored) {}
        }
        tournoiImage.setVisible(false);
        imgPlaceholder.setVisible(true);
    }

    @FXML void onClose(ActionEvent event) {
        ((Stage) lblNom.getScene().getWindow()).close();
    }
}
