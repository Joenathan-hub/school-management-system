package com.school.sms;

import com.school.sms.dao.SettingsDAO;
import com.school.sms.dao.UserDAO;
import com.school.sms.model.User;
import com.school.sms.service.AuthService;
import com.school.sms.service.SmsService;
import com.school.sms.ui.DashboardScreen;
import com.school.sms.ui.FirstRunSetupScreen;
import com.school.sms.ui.SchoolSetupScreen;
import com.school.sms.ui.UIShell;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private static final AuthService authService = new AuthService();
    private static final SmsService smsService = new SmsService();

    @Override
    public void start(Stage primaryStage) {
        applyAppIcon(primaryStage);

        // Loads the bundled fonts, installs the base theme as the user agent
        // stylesheet and starts watching for new windows (so dialogs and secondary
        // stages get the same stylesheet set as the main window). Must happen
        // before the first window is shown.
        com.school.sms.util.ThemeManager.bootstrap();

        if (new UserDAO().countUsers() == 0) {
            if (new SettingsDAO().get("school.name") == null) {
                SchoolSetupScreen.show(primaryStage);
            } else {
                // School info was saved but no admin exists yet (e.g. app was
                // closed mid-setup last time) — resume at admin creation.
                FirstRunSetupScreen.show(primaryStage);
            }
        } else {
            showLogin(primaryStage);
        }
    }

    /**
     * Sets the window/taskbar icon. This matters even in the packaged app —
     * jpackage's --icon only sets the .exe file's own icon, not necessarily
     * what shows while the app is running — so setting it here covers dev
     * mode (mvn javafx:run) and the packaged app consistently.
     */
    private static void applyAppIcon(Stage stage) {
        int[] sizes = {16, 32, 64, 128, 256};
        for (int size : sizes) {
            var stream = Main.class.getResourceAsStream("/icon/app-icon-" + size + ".png");
            if (stream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(stream));
            }
        }
    }

    /** Public so other screens (first-run setup, logout) can navigate back here. */
    public static void showLogin(Stage stage) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(340);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Log In");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label();

        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.setOnAction(e -> com.school.sms.ui.ResetPasswordScreen.show(stage));

        loginButton.setOnAction(e -> {
            User user = authService.login(usernameField.getText(), passwordField.getText());
            if (user == null) {
                statusLabel.getStyleClass().setAll("status-error");
                statusLabel.setText("Invalid username or password.");
            } else {
                // Try to flush any SMS that queued up while offline, now that
                // we have an active session (best-effort, non-blocking to the UI).
               DashboardScreen.show(stage, user);
                stage.show();
                runBackgroundTasksSequentially(user);
            }
        });

        card.getChildren().addAll(usernameField, passwordField, loginButton, forgotPassword, statusLabel);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(60, 20, 20, 20));
        wrapper.setAlignment(Pos.TOP_CENTER);

        UIShell.wrap(stage, "Log In", wrapper, 420, 380);
        stage.show();
    }

    /**
     * Runs everything that needs to happen quietly after login — flushing
     * queued SMS, checking for updates, and auto-backup — one after another
     * in a SINGLE background thread. Deliberately sequential, not parallel:
     * every DAO call closes the shared database connection when it
     * finishes, so running these concurrently causes intermittent
     * "connection does not exist" errors when two threads hit the database
     * at the same moment.
     */
    private static void runBackgroundTasksSequentially(User user) {
        new Thread(() -> {
            smsService.sendPendingQueue();

            if (user.getRole() == com.school.sms.model.Role.ADMINISTRATOR) {
                com.school.sms.service.UpdateService updateService = new com.school.sms.service.UpdateService();
                com.school.sms.service.UpdateService.UpdateInfo info = updateService.checkForUpdate();
                if (info != null) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION,
                                "Version " + info.latestVersion + " is available.\n\nOpen the Check for Updates screen to view release notes and install it.",
                                javafx.scene.control.ButtonType.OK);
                        alert.setHeaderText("Update Available");
                        alert.show();
                    });
                }
            }

            new com.school.sms.service.BackupService().maybeAutoBackup(user.getId());
        }).start();
    }

    /** Runs in the background right after login — silently backs up if online and not already done today. Manual backup stays available anytime via the sidebar button, online or offline. */
    private static void runAutoBackupSilently(User user) {
        new Thread(() -> new com.school.sms.service.BackupService().maybeAutoBackup(user.getId())).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}