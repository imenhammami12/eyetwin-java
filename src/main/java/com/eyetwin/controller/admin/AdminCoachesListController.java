package com.eyetwin.controller.admin;

import com.eyetwin.entities.User;
import com.eyetwin.interfaces.ICoachApplicationService;
import com.eyetwin.services.CoachApplicationServiceImpl;
import com.eyetwin.tools.DatabaseConfig;
import com.eyetwin.tools.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminCoachesListController {

    @FXML private AdminSidebarController adminSidebarController;
    @FXML private AdminTopbarController  adminTopbarController;
    @FXML private FlowPane  coachesGrid;
    @FXML private Label     coachCountBadge;
    @FXML private TextField searchField;
    @FXML private VBox      emptyState;

    private ICoachApplicationService appService;
    private List<User> allCoaches;

    @FXML
    public void initialize() {
        if (!SessionManager.isAdmin()) { navigateTo("AdminLogin.fxml"); return; }
        appService = new CoachApplicationServiceImpl();
        if (adminSidebarController != null) adminSidebarController.setActivePage("coachApplications");
        if (adminTopbarController  != null) adminTopbarController.setTitle("Certified Coaches");
        loadCoaches();
    }

    private void loadCoaches() {
        new Thread(() -> {
            try {
                List<User> coaches = appService.getAllCoaches();
                Platform.runLater(() -> { allCoaches = coaches; renderCards(coaches); });
            } catch (Exception e) {
                System.err.println("[CoachesList] loadCoaches: " + e.getMessage());
            }
        }, "LoadCoaches").start();
    }

    @FXML
    public void handleSearch() {
        if (allCoaches == null) return;
        String q = searchField.getText().toLowerCase().trim();
        List<User> filtered = q.isBlank() ? allCoaches : allCoaches.stream().filter(u ->
                (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                        || (u.getEmail()    != null && u.getEmail().toLowerCase().contains(q))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
        ).collect(Collectors.toList());
        renderCards(filtered);
    }

    private void renderCards(List<User> coaches) {
        coachesGrid.getChildren().clear();
        boolean empty = coaches == null || coaches.isEmpty();
        showNode(emptyState, empty);
        if (empty) { setLabelText(coachCountBadge, "0"); return; }
        setLabelText(coachCountBadge, String.valueOf(coaches.size()));
        for (User coach : coaches) coachesGrid.getChildren().add(buildCard(coach));
    }

    private VBox buildCard(User coach) {
        // ── Avatar ──────────────────────────────────────
        String initials = coach.getUsername() != null && coach.getUsername().length() >= 2
                ? coach.getUsername().substring(0, 2).toUpperCase() : "??";
        Label avatar = new Label(initials);
        avatar.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#667eea,#764ba2);"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:22;"
                        + "-fx-min-width:72;-fx-min-height:72;-fx-max-width:72;-fx-max-height:72;"
                        + "-fx-background-radius:36;-fx-alignment:center;");
        StackPane avatarPane = new StackPane(avatar);
        avatarPane.setAlignment(Pos.CENTER);

        // ── Name / username ──────────────────────────────
        Label nameLabel = new Label(coach.getFullName() != null ? coach.getFullName() : coach.getUsername());
        nameLabel.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:white;"
                + "-fx-wrap-text:true;-fx-text-alignment:center;");
        Label usernameLabel = new Label("@" + coach.getUsername());
        usernameLabel.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.45);");

        // ── Coach badge ──────────────────────────────────
        Label badge = new Label("⚡ Coach");
        badge.setStyle(
                "-fx-background-color:rgba(255,193,7,0.15);-fx-border-color:rgba(255,193,7,0.40);"
                        + "-fx-border-radius:20;-fx-background-radius:20;"
                        + "-fx-text-fill:#ffd54f;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:3 12;");

        // ── Bio ──────────────────────────────────────────
        String bioText = coach.getBio() != null && !coach.getBio().isBlank()
                ? (coach.getBio().length() > 100 ? coach.getBio().substring(0, 100) + "…" : coach.getBio())
                : "No bio provided";
        Label bioLabel = new Label(bioText);
        bioLabel.setWrapText(true);
        bioLabel.setMaxWidth(230);
        bioLabel.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.45);-fx-text-alignment:center;");
        VBox bioBox = new VBox(bioLabel);
        bioBox.setAlignment(Pos.CENTER);
        bioBox.setStyle("-fx-background-color:rgba(255,255,255,0.03);"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10;-fx-min-height:60;");

        // ── Email ────────────────────────────────────────
        Label emailLabel = new Label("✉  " + coach.getEmail());
        emailLabel.setStyle("-fx-font-size:11;-fx-text-fill:#4facfe;");

        // ── Member since ─────────────────────────────────
        String since = coach.getCreatedAt() != null
                ? coach.getCreatedAt().toString().substring(0, 10) : "—";
        Label sinceLabel = new Label("📅  Since " + since);
        sinceLabel.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.40);");

        // ── View Profile button ──────────────────────────
        Button viewBtn = new Button("👁  View Profile");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle(
                "-fx-background-color:linear-gradient(to right,#ff3c64,#c0132f);"
                        + "-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;"
                        + "-fx-border-color:transparent;-fx-padding:9;-fx-cursor:hand;");
        viewBtn.setOnMouseEntered(e -> viewBtn.setOpacity(0.85));
        viewBtn.setOnMouseExited(e  -> viewBtn.setOpacity(1.0));
        viewBtn.setOnAction(e -> { SessionManager.setSelectedUser(coach); navigateTo("AdminUserDetail.fxml"); });

        // ── Revoke Coach button ──────────────────────────
        Button revokeBtn = new Button("✕  Revoke Coach Role");
        revokeBtn.setMaxWidth(Double.MAX_VALUE);
        revokeBtn.setStyle(
                "-fx-background-color:rgba(255,60,100,0.08);"
                        + "-fx-border-color:rgba(255,60,100,0.40);-fx-border-radius:8;"
                        + "-fx-background-radius:8;-fx-text-fill:#ff6b7a;"
                        + "-fx-font-weight:bold;-fx-padding:9;-fx-cursor:hand;");
        revokeBtn.setOnMouseEntered(e -> revokeBtn.setOpacity(0.75));
        revokeBtn.setOnMouseExited(e  -> revokeBtn.setOpacity(1.0));
        revokeBtn.setOnAction(e -> handleRevokeCoach(coach));

        // ── Card assembly ────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.07);");

        VBox card = new VBox(12,
                avatarPane, nameLabel, usernameLabel, badge,
                bioBox, sep, emailLabel, sinceLabel, viewBtn, revokeBtn);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color:rgba(255,255,255,0.02);"
                        + "-fx-border-color:rgba(255,255,255,0.07);"
                        + "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:24;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:rgba(255,60,100,0.06);"
                        + "-fx-border-color:rgba(255,60,100,0.30);"
                        + "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:24;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:rgba(255,255,255,0.02);"
                        + "-fx-border-color:rgba(255,255,255,0.07);"
                        + "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:24;"));
        return card;
    }

    // ═══════════════════════════════════════════════════════
    //  REVOKE COACH ROLE
    // ═══════════════════════════════════════════════════════
    private void handleRevokeCoach(User coach) {
        String name = coach.getFullName() != null ? coach.getFullName() : coach.getUsername();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Revoke Coach Role");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Remove COACH role from \"" + name + "\"?\n\n"
                        + "They will become a regular user (ROLE_USER).\n"
                        + "This action can be reversed by approving a new application.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        new Thread(() -> {
            try (Connection con = DatabaseConfig.getConnection()) {
                // Build new roles_json: remove ROLE_COACH
                String currentRoles = coach.getRolesJson();
                String newRoles;
                if (currentRoles == null || currentRoles.isBlank()) {
                    newRoles = "[\"ROLE_USER\"]";
                } else {
                    newRoles = currentRoles
                            .replace(",\"ROLE_COACH\"", "")
                            .replace("\"ROLE_COACH\",", "")
                            .replace("\"ROLE_COACH\"",  "\"ROLE_USER\"");
                    // Ensure ROLE_USER is present
                    if (!newRoles.contains("ROLE_USER")) {
                        newRoles = newRoles.replace("[", "[\"ROLE_USER\",");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE user SET roles_json = ? WHERE id = ?")) {
                    ps.setString(1, newRoles);
                    ps.setInt(2, coach.getId());
                    ps.executeUpdate();
                }

                Platform.runLater(() -> {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setHeaderText(null);
                    ok.setContentText("✅  \"" + name + "\" is now a regular user.");
                    ok.showAndWait();
                    loadCoaches(); // refresh the grid
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setHeaderText(null);
                    err.setContentText("Failed to revoke role: " + e.getMessage());
                    err.showAndWait();
                });
            }
        }, "RevokeCoach").start();
    }

    // ═══════════════════════════════════════════════════════
    //  EXPORT EXCEL — styled with logo
    // ═══════════════════════════════════════════════════════
    @FXML
    public void handleExportExcel() {
        if (allCoaches == null || allCoaches.isEmpty()) {
            showAlert("No coaches to export."); return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Coaches Excel");
        chooser.setInitialFileName("eyetwin_coaches_" + java.time.LocalDate.now() + ".xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = chooser.showSaveDialog(resolveStage());
        if (file == null) return;

        new Thread(() -> {
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                XSSFSheet sheet = wb.createSheet("Coaches");
                sheet.setColumnWidth(0, 6  * 256);
                sheet.setColumnWidth(1, 28 * 256);
                sheet.setColumnWidth(2, 22 * 256);
                sheet.setColumnWidth(3, 32 * 256);
                sheet.setColumnWidth(4, 18 * 256);
                sheet.setColumnWidth(5, 16 * 256);

                // ── Logo ──────────────────────────────────
                int logoHeight = 0;
                try {
                    URL logoUrl = getClass().getResource(
                            "/com/eyetwin/assets/img/eyetwin-logo.png");
                    if (logoUrl != null) {
                        try (InputStream is = logoUrl.openStream()) {
                            byte[] imgBytes = is.readAllBytes();
                            int picIdx = wb.addPicture(imgBytes, Workbook.PICTURE_TYPE_PNG);
                            Drawing<?> drawing = sheet.createDrawingPatriarch();
                            ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
                            anchor.setCol1(0); anchor.setRow1(0);
                            anchor.setCol2(2); anchor.setRow2(4);
                            drawing.createPicture(anchor, picIdx);
                            logoHeight = 4;
                        }
                    }
                } catch (Exception ignored) {}

                // ── Title row ─────────────────────────────
                int titleRow = logoHeight;
                XSSFRow rTitle = sheet.createRow(titleRow);
                rTitle.setHeightInPoints(32);
                XSSFCell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("EYETWIN — Certified Coaches");
                cTitle.setCellStyle(makeTitleStyle(wb));
                sheet.addMergedRegion(new CellRangeAddress(titleRow, titleRow, 0, 5));

                // ── Subtitle (date) ────────────────────────
                int subRow = titleRow + 1;
                XSSFRow rSub = sheet.createRow(subRow);
                rSub.setHeightInPoints(18);
                XSSFCell cSub = rSub.createCell(0);
                cSub.setCellValue("Exported on " + java.time.LocalDate.now()
                        + "   |   Total coaches: " + allCoaches.size());
                cSub.setCellStyle(makeSubtitleStyle(wb));
                sheet.addMergedRegion(new CellRangeAddress(subRow, subRow, 0, 5));

                // ── Empty spacer ──────────────────────────
                sheet.createRow(subRow + 1).setHeightInPoints(8);

                // ── Header row ────────────────────────────
                int hRow = subRow + 2;
                XSSFRow rHeader = sheet.createRow(hRow);
                rHeader.setHeightInPoints(22);
                String[] headers = {"#", "Full Name", "Username", "Email", "Member Since", "Status"};
                XSSFCellStyle headerStyle = makeHeaderStyle(wb);
                for (int i = 0; i < headers.length; i++) {
                    XSSFCell c = rHeader.createCell(i);
                    c.setCellValue(headers[i]);
                    c.setCellStyle(headerStyle);
                }

                // ── Data rows ─────────────────────────────
                XSSFCellStyle evenStyle = makeDataStyle(wb, false);
                XSSFCellStyle oddStyle  = makeDataStyle(wb, true);
                int rowNum = hRow + 1;
                int idx = 1;
                for (User coach : allCoaches) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(18);
                    XSSFCellStyle style = (idx % 2 == 0) ? evenStyle : oddStyle;

                    XSSFCell c0 = row.createCell(0); c0.setCellValue(idx++);       c0.setCellStyle(style);
                    XSSFCell c1 = row.createCell(1); c1.setCellValue(nvl(coach.getFullName(), "—")); c1.setCellStyle(style);
                    XSSFCell c2 = row.createCell(2); c2.setCellValue("@" + coach.getUsername()); c2.setCellStyle(style);
                    XSSFCell c3 = row.createCell(3); c3.setCellValue(nvl(coach.getEmail(), "—")); c3.setCellStyle(style);
                    XSSFCell c4 = row.createCell(4); c4.setCellValue(coach.getCreatedAt() != null
                            ? coach.getCreatedAt().toString().substring(0, 10) : "—"); c4.setCellStyle(style);
                    XSSFCell c5 = row.createCell(5); c5.setCellValue(
                            coach.getAccountStatus() != null ? coach.getAccountStatus().toString() : "ACTIVE");
                    c5.setCellStyle(style);
                }

                // ── Footer ────────────────────────────────
                sheet.createRow(rowNum).setHeightInPoints(8);
                XSSFRow rFooter = sheet.createRow(rowNum + 1);
                XSSFCell cFooter = rFooter.createCell(0);
                cFooter.setCellValue("© " + java.time.LocalDate.now().getYear()
                        + " EyeTwin E-Sport Platform — Confidential");
                cFooter.setCellStyle(makeFooterStyle(wb));
                sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, 0, 5));

                try (FileOutputStream out = new FileOutputStream(file)) { wb.write(out); }

                Platform.runLater(() -> showAlert("✅  Exported " + allCoaches.size()
                        + " coaches to:\n" + file.getAbsolutePath()));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("❌  Export failed: " + e.getMessage()));
            }
        }, "ExportExcel").start();
    }

    // ── Excel style helpers ──────────────────────────────────

    private XSSFCellStyle makeTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)10, (byte)5, (byte)20}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 18);
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)60, (byte)100}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle makeSubtitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)22, (byte)10, (byte)34}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)160, (byte)140, (byte)180}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle makeHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)60, (byte)100}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)192, (byte)19, (byte)47}, null));
        XSSFFont f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle makeDataStyle(XSSFWorkbook wb, boolean odd) {
        XSSFCellStyle s = wb.createCellStyle();
        byte v = odd ? (byte)26 : (byte)18;
        s.setFillForegroundColor(new XSSFColor(new byte[]{v, (byte)10, (byte)38}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)50, (byte)30, (byte)70}, null));
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)220, (byte)210, (byte)235}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle makeFooterStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)10, (byte)5, (byte)20}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setItalic(true); f.setFontHeightInPoints((short) 9);
        f.setColor(new XSSFColor(new byte[]{(byte)100, (byte)80, (byte)120}, null));
        s.setFont(f);
        return s;
    }

    // ─────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────
    @FXML public void goBackToApplications() { navigateTo("AdminCoachApplications.fxml"); }

    private void setLabelText(Label l, String v) { if (l != null) l.setText(v); }
    private void showNode(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }
    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private String nvl(String s, String fallback) {
        return s != null && !s.isBlank() ? s : fallback;
    }

    private void navigateTo(String fxml) {
        for (String p : new String[]{
                "/com/eyetwin/views/" + fxml,
                "/com/eyetwin/view/"  + fxml,
                "/com/eyetwin/"       + fxml}) {
            URL url = getClass().getResource(p);
            if (url != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(url);
                    loader.setClassLoader(getClass().getClassLoader());
                    Parent root = loader.load();
                    Stage stage = resolveStage();
                    if (stage != null) stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
                } catch (Exception e) {
                    System.err.println("[CoachesList] Nav error: " + e.getMessage());
                }
                return;
            }
        }
    }

    private Stage resolveStage() {
        javafx.scene.Node[] candidates = {coachesGrid, searchField, coachCountBadge};
        for (javafx.scene.Node n : candidates) {
            if (n != null && n.getScene() != null) return (Stage) n.getScene().getWindow();
        }
        return null;
    }

    @FXML public void goToDashboard()         { navigateTo("Admin.fxml"); }
    @FXML public void goToUsers()             { navigateTo("AdminUsers.fxml"); }
    @FXML public void goToTeams()             { navigateTo("AdminTeams.fxml"); }
    @FXML public void goToPlanning()          { navigateTo("AdminPlanning.fxml"); }
    @FXML public void goToTournaments()       { navigateTo("AdminTournois.fxml"); }
    @FXML public void goToVideos()            { navigateTo("AdminVideos.fxml"); }
    @FXML public void goToCoachApplications() { navigateTo("AdminCoachApplications.fxml"); }
    @FXML public void goToChannels()          { navigateTo("AdminChannels.fxml"); }
    @FXML public void goToComplaints()        { navigateTo("AdminComplaints.fxml"); }
    @FXML public void goToMessages()          { navigateTo("AdminMessages.fxml"); }
    @FXML public void goToSite()              { navigateTo("home.fxml"); }
    @FXML public void goToProfile()           { navigateTo("AdminProfile.fxml"); }
    @FXML public void goToAuditLogs()         { if (!SessionManager.isSuperAdmin()) return; navigateTo("AdminAuditLogs.fxml"); }
    @FXML public void handleLogout()          { SessionManager.logout(); navigateTo("AdminLogin.fxml"); }
}