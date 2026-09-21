package com.school.sms.ui;

import com.school.sms.model.User;
import com.school.sms.service.BackupService;
import com.school.sms.ui.design.Components;
import com.school.sms.util.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The persistent application shell: a navigation rail on the left, a top bar
 * across the top of the content area, and a scrolling content region.
 *
 * <p>The shell is built once per session and never rebuilt. Screens reach it
 * through {@link UIShell#wrap}, which routes into {@link #setCenter} whenever the
 * shell is live, so a screen keeps the same rail, the same top bar and the same
 * scroll position it had when it was left.</p>
 *
 * <p>Two overlays live above the frame: the toast layer, which stacks transient
 * messages bottom-right, and the command palette, which covers the window with a
 * scrim while it is open.</p>
 */
public class MainShell {

    private static Stage appStage;
    private static User currentUser;
    private static StackPane root;
    private static BorderPane frame;
    private static RailNav railNav;
    private static ScrollPane contentScroll;
    private static VBox contentRegion;
    private static VBox toastLayer;
    private static CommandPalette palette;
    private static boolean live;
    private static String currentTitle = "Dashboard";

    private MainShell() {
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Builds the shell for a signed-in user. Calling it again re-opens the dashboard. */
    public static void initialize(Stage primaryStage, User user) {
        if (live && primaryStage == appStage) {
            showHome();
            return;
        }
        appStage = primaryStage;
        currentUser = user;

        railNav = new RailNav(user);
        contentRegion = new VBox();
        contentRegion.getStyleClass().add("content-region");

        contentScroll = new ScrollPane(contentRegion);
        contentScroll.getStyleClass().add("content-scroll");
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        frame = new BorderPane();
        frame.getStyleClass().add("app-frame");
        frame.setLeft(railNav.node());
        frame.setTop(TopBar.build(user, MainShell::openCommandPalette));
        frame.setCenter(contentScroll);

        toastLayer = new VBox();
        toastLayer.getStyleClass().add("toast-layer");
        toastLayer.setAlignment(Pos.BOTTOM_RIGHT);
        toastLayer.setMouseTransparent(true);

        root = new StackPane(frame, toastLayer);
        StackPane.setAlignment(toastLayer, Pos.BOTTOM_RIGHT);

        Scene scene = new Scene(root, 1280, 840);
        ThemeManager.applyTheme(scene);
        installShortcuts(scene);

        appStage.setScene(scene);
        appStage.setTitle("School Management System");
        live = true;

        showHome();
        appStage.show();
        startAutoBackupTimer(user);
    }

    /** Tears the shell down so the next sign-in rebuilds it for that user. */
    public static void reset() {
        live = false;
        railNav = null;
        toastLayer = null;
        contentRegion = null;
        contentScroll = null;
        frame = null;
        root = null;
    }

    public static boolean isLive() {
        return live;
    }

    public static Stage getStage() {
        return appStage;
    }

    public static User getUser() {
        return currentUser;
    }

    public static String getCurrentTitle() {
        return currentTitle;
    }

    // ------------------------------------------------------------------
    // Content
    // ------------------------------------------------------------------

    /**
     * Shows a screen in the content region. The rail is pointed at the matching
     * navigation item when there is one, so the user can always see where they are.
     */
    public static void setCenter(Node content, String screenTitle) {
        if (!live) {
            return;
        }
        currentTitle = screenTitle == null ? "" : screenTitle;
        contentRegion.getChildren().setAll(content);
        contentScroll.setVvalue(0);
        if (railNav != null) {
            railNav.setActiveByTitle(currentTitle);
        }
        if (appStage != null && !currentTitle.isBlank()) {
            appStage.setTitle(currentTitle + " \u2014 School Management System");
        }
    }

    /** Returns to the dashboard, which is the one screen the shell owns itself. */
    public static void showHome() {
        if (currentUser == null || !live) {
            return;
        }
        if (railNav != null) {
            railNav.setActive("Dashboard");
        }
        setCenter(DashboardHomeScreen.build(currentUser), "Dashboard");
    }

    // ------------------------------------------------------------------
    // Overlays
    // ------------------------------------------------------------------

    /** Stacks a transient message in the bottom-right corner of the window. */
    public static void toast(String text, Components.Tone tone) {
        if (toastLayer == null) {
            return;
        }
        while (toastLayer.getChildren().size() >= 3) {
            toastLayer.getChildren().remove(0);
        }
        javafx.scene.layout.HBox toast = Components.toast(text, tone, null);
        toast.setOnMouseClicked(e -> toastLayer.getChildren().remove(toast));
        toastLayer.getChildren().add(toast);
        com.school.sms.ui.design.Motion.fadeIn(toast, com.school.sms.ui.design.Motion.TOAST_IN);

        PauseTransition linger = new PauseTransition(Duration.seconds(4));
        linger.setOnFinished(e -> com.school.sms.ui.design.Motion.fadeOut(toast,
                com.school.sms.ui.design.Motion.TOAST_OUT,
                () -> toastLayer.getChildren().remove(toast)));
        linger.play();
    }

    /** Opens the command palette over the whole window. */
    public static void openCommandPalette() {
        if (!live || root == null) {
            return;
        }
        if (palette != null && palette.isOpen()) {
            palette.focus();
            return;
        }
        palette = new CommandPalette(currentUser, MainShell::setCenter, MainShell::showHome);
        root.getChildren().add(palette.overlay());
        palette.open();
    }

    static void closeCommandPalette() {
        if (palette != null && root != null) {
            palette.close();
            root.getChildren().remove(palette.overlay());
            palette = null;
        }
    }

    private static void installShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.K && event.isShortcutDown()) {
                openCommandPalette();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE && palette != null && palette.isOpen()) {
                closeCommandPalette();
                event.consume();
            }
        });
    }

    // ------------------------------------------------------------------
    // Maintenance
    // ------------------------------------------------------------------

    /** Runs a manual backup off the UI thread and reports the outcome as a toast. */
    public static void backupDatabase() {
        toast("Backing up the database\u2026", Components.Tone.INFO);
        Thread worker = new Thread(() -> {
            try {
                String report = new BackupService().backupNow();
                Platform.runLater(() -> toast(report == null ? "Backup complete." : "Backup complete: " + report,
                        Components.Tone.SUCCESS));
            } catch (Exception failure) {
                Platform.runLater(() -> toast("Backup failed: " + failure.getMessage(), Components.Tone.DANGER));
            }
        }, "manual-backup");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Re-checks connectivity every 15 minutes so a machine that was offline at
     * login backs up later. Throttled to once a day by the service itself.
     */
    private static void startAutoBackupTimer(User user) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.minutes(15), e ->
                        new Thread(() -> new BackupService().maybeAutoBackup(user.getId())).start()));
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
}
