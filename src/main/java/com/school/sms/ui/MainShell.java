package com.school.sms.ui;

import com.school.sms.model.User;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Persistent application shell. The sidebar has a fixed header (name/role)
 * and a scrollable button area below it. The center content swaps out via
 * setCenter(). The currently-open screen's nav button stays highlighted
 * via setActiveButton(), so it's always clear which screen is open.
 */
public class MainShell {

    private static BorderPane rootPane;
    private static Label headerTitle;
    private static User currentUser;
    private static Button activeButton;
    private static Button dashboardHomeButton;
    private static Stage appStage;

    public static void initialize(Stage primaryStage, User user) {
        currentUser = user;
        appStage = primaryStage;

        rootPane = new BorderPane();
        rootPane.getStyleClass().add("root");

        VBox sidebarHeader = SidebarBuilder.buildHeader(user);
        SidebarBuilder.SidebarResult sidebarResult = SidebarBuilder.buildNavButtons(primaryStage, user);
        VBox sidebarButtons = sidebarResult.container;
        dashboardHomeButton = sidebarResult.dashboardHomeButton;

        ScrollPane sidebarScroll = new ScrollPane(sidebarButtons);
        sidebarScroll.getStyleClass().add("side-nav-scroll");
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(sidebarScroll, Priority.ALWAYS);

        VBox sidebarContainer = new VBox();
        sidebarContainer.getStyleClass().add("side-nav");
        sidebarContainer.setPrefWidth(240);
        sidebarContainer.setMinWidth(240);
        sidebarContainer.setMaxWidth(240);
        sidebarContainer.getChildren().addAll(sidebarHeader, sidebarScroll);

        rootPane.setLeft(sidebarContainer);

        VBox header = new VBox();
        header.getStyleClass().add("header-bar");
        headerTitle = new Label("Dashboard");
        headerTitle.getStyleClass().add("header-title");
        header.getChildren().add(headerTitle);
        rootPane.setTop(header);

        showHome();

        Scene scene = new Scene(rootPane, 1100, 750);
        com.school.sms.util.ThemeManager.applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle("School Management System");
        primaryStage.show();

        startAutoBackupTimer(user);
    }

    /** Re-checks connectivity every 15 minutes for the rest of the session — catches "was offline at login, came online later." Throttled to once per day regardless of how often this fires. */
    private static void startAutoBackupTimer(User user) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.minutes(15), e ->
                        new Thread(() -> new com.school.sms.service.BackupService().maybeAutoBackup(user.getId())).start())
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }

    public static void setCenter(Node content, String screenTitle) {
        headerTitle.setText(screenTitle);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        rootPane.setCenter(scroll);
    }

    /** Highlights the given sidebar button as active, un-highlighting whatever was active before. */
    public static void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("side-nav-button-active");
        }
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().add("side-nav-button-active");
        }
    }

    /** "Back to Dashboard" buttons call this — returns home and re-highlights Dashboard Home. */
    public static void showHome() {
        setActiveButton(dashboardHomeButton);
        setCenter(DashboardHomeScreen.build(currentUser), "Dashboard");
    }

    /** Lets home-screen tiles and other content launch other screens without needing the Stage threaded through every constructor. */
    public static Stage getStage() {
        return appStage;
    }
}