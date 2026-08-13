package com.school.sms.ui;

import com.school.sms.dao.UserDAO;
import com.school.sms.model.User;
import com.school.sms.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Reached from "Forgot Password?" on the login screen. The user enters
 * their username and the reset code an administrator gave them, plus a
 * new password. Works for any role, including another administrator.
 */
public class ResetPasswordScreen {

    public static void show(Stage stage) {
        UserDAO userDAO = new UserDAO();
        AuthService authService = new AuthService();

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setMaxWidth(360);

        Label intro = new Label("Enter your username and the reset code your administrator gave you.");
        intro.setWrapText(true);
        intro.getStyleClass().add("field-label");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField codeField = new TextField();
        codeField.setPromptText("Reset code");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm new password");

        Button resetBtn = new Button("Reset Password");
        resetBtn.getStyleClass().add("primary-button");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label();

        resetBtn.setOnAction(e -> {
            if (!newPasswordField.getText().equals(confirmField.getText())) {
                status.getStyleClass().setAll("status-error");
                status.setText("Passwords do not match.");
                return;
            }
            if (newPasswordField.getText().length() < 6) {
                status.getStyleClass().setAll("status-error");
                status.setText("Password should be at least 6 characters.");
                return;
            }

            User user = userDAO.findByUsername(usernameField.getText().trim());
            if (user == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("No account with that username.");
                return;
            }

            boolean success = authService.redeemResetCode(user.getId(), codeField.getText().trim(), newPasswordField.getText());
            if (success) {
                status.getStyleClass().setAll("status-success");
                status.setText("Password reset. You can now log in.");
            } else {
                status.getStyleClass().setAll("status-error");
                status.setText("Invalid or already-used reset code.");
            }
        });

        Button backToLogin = new Button("Back to Login");
        backToLogin.getStyleClass().add("secondary-button");
        backToLogin.setMaxWidth(Double.MAX_VALUE);
        backToLogin.setOnAction(e -> com.school.sms.Main.showLogin(stage));

        card.getChildren().addAll(intro, usernameField, codeField, newPasswordField, confirmField,
                resetBtn, backToLogin, status);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(50, 20, 20, 20));
        wrapper.setAlignment(Pos.TOP_CENTER);

        UIShell.wrap(stage, "Reset Password", wrapper, 440, 480);
        stage.show();
    }
}
