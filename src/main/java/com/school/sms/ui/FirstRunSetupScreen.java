package com.school.sms.ui;

import com.school.sms.dao.UserDAO;
import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.util.PasswordUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Shown automatically on first launch, when the Users table is empty.
 * Creates the one and only account that can bootstrap everything else:
 * the first Administrator. After this, the admin logs in and uses
 * "Issue Reset Code" (add that screen next) or a direct DB insert to
 * create bursar/teacher accounts.
 */
public class FirstRunSetupScreen {

    public static void show(Stage stage) {
        UserDAO userDAO = new UserDAO();

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);

        Label intro = new Label("No administrator account exists yet. Create one to get started.");
        intro.setWrapText(true);
        intro.getStyleClass().add("field-label");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full name");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Choose a password");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");

        Label status = new Label();

        Button createBtn = new Button("Create Administrator Account");
        createBtn.getStyleClass().add("primary-button");

        createBtn.setOnAction(e -> {
            if (fullNameField.getText().isBlank() || usernameField.getText().isBlank()
                    || passwordField.getText().isBlank()) {
                status.getStyleClass().setAll("status-error");
                status.setText("All fields are required.");
                return;
            }
            if (!passwordField.getText().equals(confirmField.getText())) {
                status.getStyleClass().setAll("status-error");
                status.setText("Passwords do not match.");
                return;
            }
            if (passwordField.getText().length() < 6) {
                status.getStyleClass().setAll("status-error");
                status.setText("Password should be at least 6 characters.");
                return;
            }

            User admin = new User();
            admin.setUsername(usernameField.getText().trim());
            admin.setPasswordHash(PasswordUtil.hash(passwordField.getText()));
            admin.setRole(Role.ADMINISTRATOR);
            admin.setFullName(fullNameField.getText().trim());
            admin.setActive(true);
            userDAO.insert(admin);

            status.getStyleClass().setAll("status-success");
            status.setText("Administrator account created. You can now log in.");

            // small delay-free transition back to login
            com.school.sms.Main.showLogin(stage);
        });

        card.getChildren().addAll(intro, fullNameField, usernameField, passwordField, confirmField, createBtn, status);

        UIShell.wrap(stage, "First-Time Setup", center(card), 460, 480);
        stage.show();
    }

    private static VBox center(VBox card) {
        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(40));
        wrapper.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        return wrapper;
    }
}
