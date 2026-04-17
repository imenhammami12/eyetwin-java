package com.eyetwin.controller.admin;

import com.eyetwin.entities.Community.Channel;
import com.eyetwin.entities.User;
import com.eyetwin.services.Community.ChannelServiceImpl;
import com.eyetwin.tools.CommunityValidator;
import com.eyetwin.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AdminChannelFormController {

    @FXML private Label lblTitle;
    @FXML private TextField tfName;
    @FXML private TextField tfGame;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbType;
    @FXML private CheckBox chkActive;
    @FXML private Label lblError;
    @FXML private Button btnSave;

    private final ChannelServiceImpl channelService = new ChannelServiceImpl();

    private Channel editingChannel;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        cbType.getItems().addAll(Channel.TYPE_PUBLIC, Channel.TYPE_PRIVATE);
        cbType.setValue(Channel.TYPE_PUBLIC);
    }

    public void setModeCreate(Runnable onSaved) {
        this.onSaved = onSaved;
        this.editingChannel = null;
        lblTitle.setText("Create Channel");
        chkActive.setSelected(true);
    }

    public void setModeEdit(Channel channel, Runnable onSaved) {
        this.onSaved = onSaved;
        this.editingChannel = channel;
        lblTitle.setText("Edit Channel");
        btnSave.setText("Update Channel"); //////////////////////////

        tfName.setText(channel.getName());
        tfGame.setText(channel.getGame());
        taDescription.setText(channel.getDescription());
        cbType.setValue(channel.getType());
        chkActive.setSelected(channel.isActive());
    }

    @FXML
    private void handleSave() {
        try {
            User admin = SessionManager.getCurrentUser();

            Channel channel = (editingChannel == null) ? new Channel() : editingChannel;
            channel.setName(tfName.getText());
            channel.setGame(tfGame.getText());
            channel.setDescription(taDescription.getText());
            channel.setType(cbType.getValue());
            channel.setActive(chkActive.isSelected());
            channel.setImageUrl(null);

            String validation = CommunityValidator.validateChannel(channel);
            if (validation != null) {
                lblError.setText(validation);
                return;
            }

            if (editingChannel == null) {
                channelService.createByAdmin(channel, admin);
            } else {
                channelService.updateByAdmin(channel, admin);
            }

            if (onSaved != null) {
                onSaved.run();
            }

            close();

        } catch (Exception e) {
            lblError.setText(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) tfName.getScene().getWindow();
        stage.close();
    }
}