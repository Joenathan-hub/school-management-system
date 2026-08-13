package com.school.sms.ui;

import com.school.sms.dao.UserDAO;
import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.service.AuthService;
import com.school.sms.util.PasswordUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Administrator-only: create bursar/teacher/administrator accounts, and
 * issue admin-generated password reset codes for any existing account.
 * This is the screen that was missing before — without it there was no
 * way to create anyone but the very first admin.
 */
public class UserManagementScreen {

    public static void show(Stage stage, User adminUser) {
        UserDAO userDAO = new UserDAO();
        AuthService authService = new AuthService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        // --- Create account ---
        VBox createCard = new VBox(8);
        createCard.getStyleClass().add("card");
        Label createTitle = new Label("Create New Account");
        createTitle.getStyleClass().add("section-label");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full name");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Initial password");
        ComboBox<Role> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(Role.BURSAR, Role.TEACHER, Role.ADMINISTRATOR);
        roleBox.setValue(Role.BURSAR);
        TextField assignedClassField = new TextField();
        assignedClassField.setPromptText("Assigned class (teachers only, e.g. S1)");
        assignedClassField.setDisable(true);

        roleBox.valueProperty().addListener((obs, oldVal, newVal) ->
                assignedClassField.setDisable(newVal != Role.TEACHER));

        Button createBtn = new Button("Create Account");
        createBtn.getStyleClass().add("primary-button");
        Label createStatus = new Label();

        createBtn.setOnAction(e -> {
            if (fullNameField.getText().isBlank() || usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
                createStatus.getStyleClass().setAll("status-error");
                createStatus.setText("Full name, username, and password are required.");
                return;
            }
            if (userDAO.findByUsername(usernameField.getText().trim()) != null) {
                createStatus.getStyleClass().setAll("status-error");
                createStatus.setText("That username is already taken.");
                return;
            }

            User newUser = new User();
            newUser.setFullName(fullNameField.getText().trim());
            newUser.setUsername(usernameField.getText().trim());
            newUser.setPasswordHash(PasswordUtil.hash(passwordField.getText()));
            newUser.setRole(roleBox.getValue());
            newUser.setActive(true);
            if (roleBox.getValue() == Role.TEACHER) {
                newUser.setAssignedClass(assignedClassField.getText().trim());
            }
            userDAO.insert(newUser);

            createStatus.getStyleClass().setAll("status-success");
            createStatus.setText("Account created for " + newUser.getFullName() + " (" + newUser.getRole() + ").");
            fullNameField.clear(); usernameField.clear(); passwordField.clear(); assignedClassField.clear();
        });

        createCard.getChildren().addAll(createTitle, fullNameField, usernameField, passwordField,
                roleBox, assignedClassField, createBtn, createStatus);

        // --- Issue reset code ---
        VBox resetCard = new VBox(8);
        resetCard.getStyleClass().add("card");
        Label resetTitle = new Label("Issue Password Reset Code");
        resetTitle.getStyleClass().add("section-label");

        ListView<User> userList = new ListView<>();
        userList.setPrefHeight(140);
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : u.getFullName() + " (" + u.getUsername() + ", " + u.getRole() + ")");
            }
        });
        userList.getItems().setAll(userDAO.findAll());

        Button issueCodeBtn = new Button("Generate Reset Code");
        issueCodeBtn.getStyleClass().add("accent-button");
        Label codeResult = new Label();
        codeResult.setWrapText(true);

        issueCodeBtn.setOnAction(e -> {
            User selected = userList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                codeResult.getStyleClass().setAll("status-error");
                codeResult.setText("Select an account first.");
                return;
            }
            String code = authService.issueResetCode(adminUser.getId(), selected.getId());
            codeResult.getStyleClass().setAll("status-success");
            codeResult.setText("Reset code for " + selected.getUsername() + ": " + code +
                    "\nGive this to them directly — it's shown only once and works from the \"Forgot Password?\" link on the login screen.");
        });

        resetCard.getChildren().addAll(resetTitle, userList, issueCodeBtn, codeResult);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(createCard, resetCard, back);
        MainShell.setCenter(root, "Manage User Accounts");
    }
}
