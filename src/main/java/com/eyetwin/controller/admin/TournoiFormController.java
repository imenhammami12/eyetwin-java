package com.eyetwin.controller.admin;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.eyetwin.entities.Tournoi;
import com.eyetwin.entities.TypeTournoi;
import com.eyetwin.services.TournoiServiceImpl;
import com.eyetwin.interfaces.ITournoiService;

import java.io.File;
import java.time.LocalDate;

public class TournoiFormController {

    @FXML private Label    formTitle;
    @FXML private Label    formSubtitle;
    @FXML private TextField  txtNom;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField  txtDescription;
    @FXML private TextField  txtImage;
    @FXML private ComboBox<TypeTournoi> cbType;
    @FXML private TextField  txtPrix;

    @FXML private Label errNom;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;
    @FXML private Label errDescription;
    @FXML private Label errImage;
    @FXML private Label errType;
    @FXML private Label errPrix;

    private ITournoiService tournoiService = new TournoiServiceImpl();
    private Tournoi existingTournoi = null;
    private Runnable onSavedCallback = null;

    @FXML public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(TypeTournoi.values()));
    }

    public void setTournoi(Tournoi t) {
        this.existingTournoi = t;
        if (t != null) {
            formTitle.setText("Edit Tournament");
            formSubtitle.setText("Update the tournament details below");
            txtNom.setText(t.getNom());
            dpDateDebut.setValue(t.getDateDebut());
            dpDateFin.setValue(t.getDateFin());
            txtDescription.setText(t.getDescription());
            txtImage.setText(t.getImage());
            cbType.setValue(t.getTypeTournoi());
            txtPrix.setText(String.valueOf(t.getPrix()));
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSavedCallback = callback;
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    private void setError(Label lbl, String msg) {
        if (msg == null || msg.isEmpty()) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        } else {
            lbl.setText(msg);
            lbl.setVisible(true);
            lbl.setManaged(true);
        }
    }

    private void clearErrors() {
        setError(errNom, null);
        setError(errDateDebut, null);
        setError(errDateFin, null);
        setError(errDescription, null);
        setError(errImage, null);
        setError(errType, null);
        setError(errPrix, null);
    }

    private boolean isValid() {
        clearErrors();
        boolean ok = true;
        if (txtNom.getText().trim().isEmpty()) {
            setError(errNom, "Nom obligatoire.");
            ok = false;
        }
        LocalDate now = LocalDate.now();
        LocalDate debut = dpDateDebut.getValue(), fin = dpDateFin.getValue();
        if (debut == null) {
            setError(errDateDebut, "Date de début requise.");
            ok = false;
        } else if (debut.isBefore(now) && (existingTournoi == null || !debut.equals(existingTournoi.getDateDebut()))) {
            // Only validate "future" if it's a new tournament or the date was changed
            setError(errDateDebut, "Doit être >= aujourd'hui.");
            ok = false;
        }
        if (fin == null) {
            setError(errDateFin, "Date de fin requise.");
            ok = false;
        } else if (debut != null && fin.isBefore(debut)) {
            setError(errDateFin, "Doit être >= début.");
            ok = false;
        }
        if (txtDescription.getText().trim().isEmpty()) {
            setError(errDescription, "Description obligatoire.");
            ok = false;
        }
        if (txtImage.getText().trim().isEmpty()) {
            setError(errImage, "Image obligatoire.");
            ok = false;
        }
        if (cbType.getValue() == null) {
            setError(errType, "Type obligatoire.");
            ok = false;
        }
        if (txtPrix.getText().trim().isEmpty()) {
            setError(errPrix, "Prix obligatoire.");
            ok = false;
        } else {
            try {
                if (Double.parseDouble(txtPrix.getText()) <= 0) {
                    setError(errPrix, "Doit être > 0.");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                setError(errPrix, "Nombre valide requis.");
                ok = false;
            }
        }
        return ok;
    }

    // ─── Actions ───────────────────────────────────────────────────────────────

    @FXML void onBrowseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg","*.png","*.jpeg","*.gif"));
        File f = fc.showOpenDialog(txtNom.getScene().getWindow());
        if (f != null) {
            txtImage.setText(f.getAbsolutePath());
        }
    }

    @FXML void onSave(ActionEvent event) {
        if (!isValid()) return;
        try {
            double prix = Double.parseDouble(txtPrix.getText());
            if (existingTournoi == null) {
                tournoiService.add(new Tournoi(txtNom.getText(), dpDateDebut.getValue(), dpDateFin.getValue(),
                    txtDescription.getText(), txtImage.getText(), cbType.getValue(), prix));
            } else {
                tournoiService.update(new Tournoi(existingTournoi.getId(), txtNom.getText(),
                    dpDateDebut.getValue(), dpDateFin.getValue(),
                    txtDescription.getText(), txtImage.getText(), cbType.getValue(), prix));
            }
            if (onSavedCallback != null) onSavedCallback.run();
            closeWindow();
        } catch (Exception e) {
            System.err.println("Save error: " + e.getMessage());
        }
    }

    @FXML void onCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) txtNom.getScene().getWindow()).close();
    }
}
