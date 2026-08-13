package com.school.sms.ui;

import com.school.sms.dao.SettingsDAO;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Wraps every screen's content in a consistent executive-style header +
 * loads the shared theme.css stylesheet. Call UIShell.wrap(stage, "Screen
 * Title", contentNode) as the last step in any screen instead of building
 * the Scene by hand — keeps every screen visually consistent.
 */
public class UIShell {

    public static void wrap(Stage stage, String screenTitle, javafx.scene.Node content, double width, double height) {
        VBox header = new VBox(2);
        header.getStyleClass().add("header-bar");
        Label titleLabel = new Label(screenTitle);
        titleLabel.getStyleClass().add("header-title");
        String schoolName = new SettingsDAO().get("school.name", "School Management System");
        Label subtitle = new Label(schoolName);
        subtitle.getStyleClass().add("header-subtitle");
        header.getChildren().addAll(titleLabel, subtitle);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroll);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, width, height);
        com.school.sms.util.ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle(screenTitle);
    }
}
