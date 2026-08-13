package com.school.sms.ui;

import com.school.sms.dao.SettingsDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The very first screen on a brand-new install: capture the school's name
 * and initials (used in Student/Worker IDs and on every receipt/report)
 * before anything else happens. Only shown once — after this, Main routes
 * straight to FirstRunSetupScreen (create admin) and then to login.
 */
public class SchoolSetupScreen {

    public static void show(Stage stage) {
        SettingsDAO settingsDAO = new SettingsDAO();

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(380);

        Label intro = new Label("Welcome! Let's set up your school before creating the administrator account.");
        intro.setWrapText(true);
        intro.getStyleClass().add("field-label");

        TextField schoolNameField = new TextField();
        schoolNameField.setPromptText("Full school name, e.g. \"St. Mary's High School\"");

        TextField initialsField = new TextField();
        initialsField.setPromptText("Short initials for IDs, e.g. \"SMHS\"");
        initialsField.textProperty().addListener((obs, oldVal, newVal) -> {
            String upper = newVal.toUpperCase();
            if (!upper.equals(newVal)) initialsField.setText(upper);
        });

        Label helper = new Label("Initials appear in every Student/Worker ID, e.g. S-SMHS202607-4821. " +
                "Keep them short (3-6 letters) — you can't easily change them later without affecting old IDs.");
        helper.setWrapText(true);
        helper.getStyleClass().add("field-label");

        Label status = new Label();

        Button continueBtn = new Button("Save & Continue");
        continueBtn.getStyleClass().add("primary-button");
        continueBtn.setMaxWidth(Double.MAX_VALUE);

        continueBtn.setOnAction(e -> {
            String name = schoolNameField.getText().trim();
            String initials = initialsField.getText().trim();

            if (name.isBlank()) {
                status.getStyleClass().setAll("status-error");
                status.setText("School name is required.");
                return;
            }
            if (initials.isBlank()) {
                status.getStyleClass().setAll("status-error");
                status.setText("School initials are required (used in ID numbers).");
                return;
            }

            settingsDAO.set("school.name", name);
            settingsDAO.set("school.initials", initials);

            FirstRunSetupScreen.show(stage);
        });

        card.getChildren().addAll(intro, schoolNameField, initialsField, helper, continueBtn, status);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(50, 20, 20, 20));
        wrapper.setAlignment(Pos.TOP_CENTER);

        UIShell.wrap(stage, "School Setup", wrapper, 460, 460);
        stage.show();
    }
}
