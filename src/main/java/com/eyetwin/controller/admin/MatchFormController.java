package com.eyetwin.controller.admin;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.eyetwin.entities.Match;
import com.eyetwin.entities.Tournoi;
import com.eyetwin.services.MatchServiceImpl;
import com.eyetwin.services.TournoiServiceImpl;
import com.eyetwin.interfaces.IMatchService;
import com.eyetwin.interfaces.ITournoiService;

import java.time.LocalDate;

public class MatchFormController {

    @FXML private Label formTitle;
    @FXML private Label formSubtitle;

    @FXML private TextField txtEquipe1;
    @FXML private TextField txtEquipe2;
    @FXML private TextField txtScore;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtPrix;
    @FXML private ComboBox<Tournoi> cbTournoi;
    @FXML private TextField txtMode;
    @FXML private TextField txtLocalisation;

    @FXML private Label errEquipe1;
    @FXML private Label errEquipe2;
    @FXML private Label errScore;
    @FXML private Label errDate;
    @FXML private Label errPrix;
    @FXML private Label errTournoiId;
    @FXML private Label errMode;
    @FXML private Label errLocalisation;

    private IMatchService matchService = new MatchServiceImpl();
    private ITournoiService tournoiService = new TournoiServiceImpl();
    private Match existingMatch = null;
    private Runnable onSavedCallback = null;

    @FXML
    public void initialize() {
        cbTournoi.setItems(FXCollections.observableArrayList(tournoiService.getAll()));
        setupValidationListeners();
        clearErrors();
    }

    private void setupValidationListeners() {
        txtEquipe1.textProperty().addListener((obs, old, val) -> setError(errEquipe1, null));
        txtEquipe2.textProperty().addListener((obs, old, val) -> setError(errEquipe2, null));
        txtScore.textProperty().addListener((obs, old, val) -> setError(errScore, null));
        txtPrix.textProperty().addListener((obs, old, val) -> setError(errPrix, null));
        txtMode.textProperty().addListener((obs, old, val) -> setError(errMode, null));
        txtLocalisation.textProperty().addListener((obs, old, val) -> setError(errLocalisation, null));
        
        dpDate.valueProperty().addListener((obs, old, val) -> setError(errDate, null));
        cbTournoi.valueProperty().addListener((obs, old, val) -> {
            setError(errTournoiId, null);
            if (dpDate.getValue() != null) setError(errDate, null);
        });
    }

    public void setMatch(Match m) {
        this.existingMatch = m;
        if (m != null) {
            formTitle.setText("Edit Match");
            formSubtitle.setText("Update the details of the match");
            txtEquipe1.setText(m.getEquipe1());
            txtEquipe2.setText(m.getEquipe2());
            txtScore.setText(String.valueOf(m.getScore()));
            dpDate.setValue(m.getDateMatch());
            txtPrix.setText(m.getPrix());
            txtMode.setText(m.getPlayMode());
            txtLocalisation.setText(m.getLocalisation());

            for (Tournoi t : cbTournoi.getItems()) {
                if (t.getId() == m.getTournoiId()) {
                    cbTournoi.setValue(t);
                    break;
                }
            }
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSavedCallback = callback;
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    private void setError(Label label, String msg) {
        if (msg == null || msg.isEmpty()) {
            label.setText(""); label.setVisible(false); label.setManaged(false);
        } else {
            label.setText(msg); label.setVisible(true); label.setManaged(true);
        }
    }

    private void clearErrors() {
        setError(errEquipe1, null); setError(errEquipe2, null); setError(errScore, null);
        setError(errDate, null);    setError(errPrix, null);    setError(errTournoiId, null);
        setError(errMode, null);    setError(errLocalisation, null);
    }

    private boolean isInputValid() {
        clearErrors();
        boolean ok = true;

        if (txtEquipe1.getText() == null || txtEquipe1.getText().trim().isEmpty()) {
            setError(errEquipe1, "Equipe 1 requise."); ok = false;
        }
        if (txtEquipe2.getText() == null || txtEquipe2.getText().trim().isEmpty()) {
            setError(errEquipe2, "Equipe 2 requise."); ok = false;
        }
        if (txtScore.getText() == null || txtScore.getText().trim().isEmpty()) {
            setError(errScore, "Score requis."); ok = false;
        } else {
            try {
                int sc = Integer.parseInt(txtScore.getText());
                if (sc < 0) { setError(errScore, "Score doit être positif."); ok = false; }
            } catch (NumberFormatException e) {
                setError(errScore, "Nombre valide requis."); ok = false;
            }
        }
        if (txtPrix.getText() == null || txtPrix.getText().trim().isEmpty()) {
            setError(errPrix, "Prix requis."); ok = false;
        } else {
            try {
                double p = Double.parseDouble(txtPrix.getText());
                if (p <= 0) { setError(errPrix, "Doit être > 0."); ok = false; }
            } catch (NumberFormatException e) {
                setError(errPrix, "Nombre valide requis."); ok = false;
            }
        }

        Tournoi sel = cbTournoi.getValue();
        if (sel == null) {
            setError(errTournoiId, "Veuillez choisir un tournoi."); ok = false;
        } else {
            LocalDate matchDate = dpDate.getValue();
            if (matchDate == null) {
                setError(errDate, "Sélectionnez une date."); ok = false;
            } else {
                LocalDate debut = sel.getDateDebut();
                LocalDate fin = sel.getDateFin();
                
                if (debut != null && matchDate.isBefore(debut)) {
                    setError(errDate, "Le match doit être après le " + debut);
                    ok = false;
                } else if (fin != null && matchDate.isAfter(fin)) {
                    setError(errDate, "Le match doit être avant le " + fin);
                    ok = false;
                }
            }
        }
        
        if (txtMode.getText() == null || txtMode.getText().trim().isEmpty()) {
            setError(errMode, "Mode requis."); ok = false;
        }
        if (txtLocalisation.getText() == null || txtLocalisation.getText().trim().isEmpty()) {
            setError(errLocalisation, "Localisation requise."); ok = false;
        }
        return ok;
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @FXML
    void onSave(ActionEvent event) {
        if (!isInputValid()) return;
        try {
            if (existingMatch == null) {
                Match m = new Match();
                m.setEquipe1(txtEquipe1.getText());
                m.setEquipe2(txtEquipe2.getText());
                m.setScore(Integer.parseInt(txtScore.getText()));
                m.setDateMatch(dpDate.getValue());
                m.setPrix(txtPrix.getText());
                m.setTournoiId(cbTournoi.getValue().getId());
                m.setPlayMode(txtMode.getText());
                m.setLocalisation(txtLocalisation.getText());
                matchService.add(m);
            } else {
                Match m = new Match();
                m.setId(existingMatch.getId());
                m.setEquipe1(txtEquipe1.getText());
                m.setEquipe2(txtEquipe2.getText());
                m.setScore(Integer.parseInt(txtScore.getText()));
                m.setDateMatch(dpDate.getValue());
                m.setPrix(txtPrix.getText());
                m.setTournoiId(cbTournoi.getValue().getId());
                m.setPlayMode(txtMode.getText());
                m.setLocalisation(txtLocalisation.getText());
                matchService.update(m);
            }

            if (onSavedCallback != null) onSavedCallback.run();
            closeWindow();
        } catch (Exception e) {
            System.err.println("Save error: " + e.getMessage());
        }
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) txtEquipe1.getScene().getWindow()).close();
    }
}
