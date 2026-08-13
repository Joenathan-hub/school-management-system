package com.school.sms.ui;

import com.school.sms.model.User;
import com.school.sms.service.UpdateService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;

public class UpdateScreen {

    public static void show(Stage stage, User user) {
        UpdateService updateService = new UpdateService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label("Software Updates");
        title.getStyleClass().add("section-label");

        Label currentVersionLabel = new Label("Current version: " + UpdateService.CURRENT_VERSION);
        currentVersionLabel.getStyleClass().add("field-label");

        Button checkBtn = new Button("Check for Updates");
        checkBtn.getStyleClass().add("primary-button");

        Label status = new Label();
        status.setWrapText(true);

        TextArea releaseNotesArea = new TextArea();
        releaseNotesArea.setEditable(false);
        releaseNotesArea.setPrefHeight(160);
        releaseNotesArea.setVisible(false);
        releaseNotesArea.setManaged(false);

        Button downloadBtn = new Button("Download and Install Update");
        downloadBtn.getStyleClass().add("accent-button");
        downloadBtn.setVisible(false);
        downloadBtn.setManaged(false);

        final UpdateService.UpdateInfo[] latestInfo = new UpdateService.UpdateInfo[1];

        checkBtn.setOnAction(e -> {
            status.getStyleClass().setAll("field-label");
            status.setText("Checking...");
            releaseNotesArea.setVisible(false);
            releaseNotesArea.setManaged(false);
            downloadBtn.setVisible(false);
            downloadBtn.setManaged(false);

            new Thread(() -> {
                UpdateService.UpdateInfo info = updateService.checkForUpdate();
                Platform.runLater(() -> {
                    if (info == null) {
                        status.getStyleClass().setAll("status-success");
                        status.setText("You're running the latest version.");
                    } else {
                        latestInfo[0] = info;
                        status.getStyleClass().setAll("status-success");
                        status.setText("Version " + info.latestVersion + " is available.");
                        releaseNotesArea.setText(info.releaseNotes != null && !info.releaseNotes.isBlank()
                                ? info.releaseNotes : "(no release notes provided)");
                        releaseNotesArea.setVisible(true);
                        releaseNotesArea.setManaged(true);
                        if (info.downloadUrl != null) {
                            downloadBtn.setVisible(true);
                            downloadBtn.setManaged(true);
                        }
                    }
                });
            }).start();
        });

        downloadBtn.setOnAction(e -> {
            if (latestInfo[0] == null || latestInfo[0].downloadUrl == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "This will download the update and close the app to install it. Continue?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    status.getStyleClass().setAll("field-label");
                    status.setText("Downloading update...");
                    downloadBtn.setDisable(true);

                    new Thread(() -> {
                        Path installer = updateService.downloadInstaller(latestInfo[0].downloadUrl);
                        Platform.runLater(() -> {
                            if (installer == null) {
                                status.getStyleClass().setAll("status-error");
                                status.setText("Download failed. Check your internet connection and try again.");
                                downloadBtn.setDisable(false);
                                return;
                            }
                            boolean launched = updateService.launchInstaller(installer);
                            if (launched) {
                                Platform.exit();
                                System.exit(0);
                            } else {
                                status.getStyleClass().setAll("status-error");
                                status.setText("Could not launch the installer. Download saved to: " + installer);
                                downloadBtn.setDisable(false);
                            }
                        });
                    }).start();
                }
            });
        });

        card.getChildren().addAll(title, currentVersionLabel, checkBtn, status, releaseNotesArea, downloadBtn);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(ev -> MainShell.showHome());

        root.getChildren().addAll(card, back);
        MainShell.setCenter(root, "Software Updates");
    }
}