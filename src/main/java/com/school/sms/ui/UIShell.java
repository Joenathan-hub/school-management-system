package com.school.sms.ui;

import com.school.sms.dao.SettingsDAO;
import com.school.sms.ui.design.Components;
import com.school.sms.util.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The wrapper every screen calls to present itself.
 *
 * <p>There are two cases, and the decision is made here rather than in thirty
 * screens:</p>
 *
 * <ol>
 *   <li><b>Signed in.</b> The persistent shell is live, so the screen's content is
 *       dropped into the shell's content region and the navigation rail, top bar
 *       and overlays are left exactly as they were. This is what makes the
 *       application feel like one product rather than a series of windows.</li>
 *   <li><b>Not signed in.</b> Sign-in, first-run setup and password reset have no
 *       rail and no top bar to belong to, so they get a bare frame: the same
 *       canvas, the same two stylesheets, the same typography, with the card
 *       centred.</li>
 * </ol>
 *
 * <p>Either way the screen is given the identical stylesheet set — structural
 * layer plus the token sheet for the active theme — so no screen can drift out of
 * the theme.</p>
 */
public class UIShell {

    private UIShell() {
    }

    public static void wrap(Stage stage, String screenTitle, Node content, double width, double height) {
        if (MainShell.isLive()) {
            MainShell.setCenter(content, screenTitle);
            stage.setTitle(screenTitle + " \u2014 School Management System");
            return;
        }

        VBox region = new VBox(content);
        region.getStyleClass().add("content-region");
        region.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(region);
        scroll.getStyleClass().add("content-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        BorderPane frame = new BorderPane();
        frame.getStyleClass().add("app-frame");
        frame.setTop(bareHeader(screenTitle));
        frame.setCenter(scroll);

        StackPane root = new StackPane(frame);

        Scene scene = new Scene(root, width, height);
        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle(screenTitle + " \u2014 School Management System");
    }

    /** The heading used before sign-in, where there is no shell to carry a top bar. */
    private static Node bareHeader(String screenTitle) {
        String schoolName;
        try {
            schoolName = new SettingsDAO().get("school.name", "School Management System");
        } catch (Throwable ignored) {
            schoolName = "School Management System";
        }

        Label title = Components.pageTitle(screenTitle);
        Label subtitle = Components.pageSubtitle(schoolName);

        VBox titles = new VBox(2, title, subtitle);
        titles.setPadding(new javafx.geometry.Insets(24, 24, 0, 24));
        return titles;
    }
}
