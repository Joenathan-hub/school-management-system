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
 * Bursar-only: create Teacher accounts. Deliberately narrower than
 * UserManagementScreen (admin's screen) — a bursar can only ever create
 * TEACHER accounts here, nothing else.
 */
public class TeacherAccountScreen {

    public static void show(Stage stage, User bursarUser) {
        UserDAO userDAO = new UserDAO();

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(380);

        Label title = new Label("Create Teacher Account");
        title.getStyleClass().add("section-label");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full name");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Initial password");
        TextField assignedClassField = new TextField();
        assignedClassField.setPromptText("Assigned class (e.g. S1)");

        Button createBtn = new Button("Create Teacher Account");
        createBtn.getStyleClass().add("primary-button");
        Label status = new Label();

        createBtn.setOnAction(e -> {
            if (fullNameField.getText().isBlank() || usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
                status.getStyleClass().setAll("status-error");
                status.setText("Full name, username, and password are required.");
                return;
            }
            if (userDAO.findByUsername(usernameField.getText().trim()) != null) {
                status.getStyleClass().setAll("status-error");
                status.setText("That username is already taken.");
                return;
            }

            User teacher = new User();
            teacher.setFullName(fullNameField.getText().trim());
            teacher.setUsername(usernameField.getText().trim());
            teacher.setPasswordHash(PasswordUtil.hash(passwordField.getText()));
            teacher.setRole(Role.TEACHER);
            teacher.setAssignedClass(assignedClassField.getText().trim());
            teacher.setActive(true);
            userDAO.insert(teacher);

            status.getStyleClass().setAll("status-success");
            status.setText("Teacher account created for " + teacher.getFullName() + ".");
            fullNameField.clear(); usernameField.clear(); passwordField.clear(); assignedClassField.clear();
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        card.getChildren().addAll(title, fullNameField, usernameField, passwordField, assignedClassField, createBtn, status, back);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(40));
        wrapper.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        MainShell.setCenter(wrapper, "Create Teacher Account");
    }
}