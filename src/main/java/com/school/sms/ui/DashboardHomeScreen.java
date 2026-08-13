package com.school.sms.ui;

import com.school.sms.model.Role;
import com.school.sms.model.SchoolEvent;
import com.school.sms.model.User;
import com.school.sms.service.DashboardKpiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardHomeScreen {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Timeline clockTimeline;

    public static VBox build(User user) {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        if (user.getRole() == Role.ADMINISTRATOR || user.getRole() == Role.BURSAR) {
            return buildKpiDashboard(user);
        }
        return buildSimpleWelcome(user);
    }

    private static VBox buildSimpleWelcome(User user) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("dashboard-right-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setPrefHeight(600);

        Label welcome = new Label("Welcome, " + user.getFullName());
        welcome.getStyleClass().add("dashboard-welcome");

        Label timeLabel = new Label();
        timeLabel.getStyleClass().add("clock-time");
        Label dateLabel = new Label();
        dateLabel.getStyleClass().add("clock-date");

        updateClock(dateLabel, timeLabel);
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock(dateLabel, timeLabel)));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();

        panel.getChildren().addAll(welcome, timeLabel, dateLabel);

        DashboardKpiService kpiService = new DashboardKpiService();
        VBox eventsTile = buildEventsTile(kpiService, user);
        eventsTile.setMaxWidth(420);
        panel.getChildren().add(eventsTile);

        return panel;
    }

    private static VBox buildKpiDashboard(User user) {
        DashboardKpiService kpiService = new DashboardKpiService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        HBox header = new HBox();
        Label welcome = new Label("Welcome, " + user.getFullName());
        welcome.getStyleClass().add("dashboard-welcome");
        HBox.setHgrow(welcome, Priority.ALWAYS);

        Label timeLabel = new Label();
        timeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label dateLabel = new Label();
        dateLabel.getStyleClass().add("field-label");
        updateClock(dateLabel, timeLabel);
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock(dateLabel, timeLabel)));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
        VBox clockStack = new VBox(2, timeLabel, dateLabel);
        clockStack.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(welcome, clockStack);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        grid.add(buildCollectedTodayTile(kpiService, user), 0, 0);
        grid.add(buildOutstandingTile(kpiService, user), 1, 0);
        grid.add(buildNotClearedTile(kpiService, user), 0, 1);
        grid.add(buildExpenditureTodayTile(kpiService, user), 1, 1);
        grid.add(buildEventsTile(kpiService, user), 0, 2, 2, 1);

        for (int col = 0; col < 2; col++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
        }

        root.getChildren().addAll(header, grid);
        return root;
    }

    private static VBox makeClickableTile(Runnable onClick) {
        VBox tile = new VBox(8);
        tile.getStyleClass().addAll("summary-card", "summary-card-clickable");
        tile.setOnMouseClicked(e -> onClick.run());
        return tile;
    }

    private static Label hintLabel() {
        Label hint = new Label("Click for full details →");
        hint.getStyleClass().add("field-label");
        return hint;
    }

    private static VBox buildCollectedTodayTile(DashboardKpiService kpiService, User user) {
        VBox tile = makeClickableTile(() -> CollectedTodayDetailScreen.show(MainShell.getStage(), user));

        Label caption = new Label("COLLECTED TODAY");
        caption.getStyleClass().add("summary-caption");
        Label figure = new Label(String.format("UGX %,.0f", kpiService.getCollectedTodayTotal()));
        figure.getStyleClass().add("summary-figure");

        int count = kpiService.getCollectedTodayDetails().size();
        Label subtext = new Label(count + " payment(s) today");
        subtext.getStyleClass().add("field-label");

        tile.getChildren().addAll(caption, figure, subtext, hintLabel());
        return tile;
    }

    private static VBox buildExpenditureTodayTile(DashboardKpiService kpiService, User user) {
        VBox tile = makeClickableTile(() -> ExpenditureTodayDetailScreen.show(MainShell.getStage(), user));

        Label caption = new Label("EXPENDITURE TODAY");
        caption.getStyleClass().add("summary-caption");
        Label figure = new Label(String.format("UGX %,.0f", kpiService.getExpenditureTodayTotal()));
        figure.getStyleClass().add("summary-figure");

        int count = kpiService.getExpenditureTodayDetails().size();
        Label subtext = new Label(count + " entr" + (count == 1 ? "y" : "ies") + " today");
        subtext.getStyleClass().add("field-label");

        tile.getChildren().addAll(caption, figure, subtext, hintLabel());
        return tile;
    }

    private static VBox buildOutstandingTile(DashboardKpiService kpiService, User user) {
        VBox tile = makeClickableTile(() -> OutstandingBalanceDetailScreen.show(MainShell.getStage(), user));

        Label caption = new Label("OUTSTANDING BALANCE (ALL CLASSES)");
        caption.getStyleClass().add("summary-caption");
        Label figure = new Label(String.format("UGX %,.0f", kpiService.getOutstandingTotal()));
        figure.getStyleClass().add("summary-figure");

        int classCount = kpiService.getOutstandingByClass().size();
        Label subtext = new Label(classCount > 0 ? "Across " + classCount + " class(es)" : "No active term set");
        subtext.getStyleClass().add("field-label");

        tile.getChildren().addAll(caption, figure, subtext, hintLabel());
        return tile;
    }

    private static VBox buildNotClearedTile(DashboardKpiService kpiService, User user) {
        VBox tile = makeClickableTile(() -> StudentsNotClearedDetailScreen.show(MainShell.getStage(), user));

        Label caption = new Label("STUDENTS NOT FULLY CLEARED");
        caption.getStyleClass().add("summary-caption");
        Label figure = new Label(String.valueOf(kpiService.getStudentsNotClearedCount()));
        figure.getStyleClass().add("summary-figure");

        Label subtext = new Label("Fees or requirements outstanding");
        subtext.getStyleClass().add("field-label");

        tile.getChildren().addAll(caption, figure, subtext, hintLabel());
        return tile;
    }

    private static VBox buildEventsTile(DashboardKpiService kpiService, User user) {
        VBox tile = makeClickableTile(() -> UpcomingEventsDetailScreen.show(MainShell.getStage(), user));

        Label caption = new Label("UPCOMING SCHOOL EVENTS");
        caption.getStyleClass().add("summary-caption");

        List<SchoolEvent> events = kpiService.getUpcomingEvents(3);
        VBox list = new VBox(4);
        if (events.isEmpty()) {
            Label none = new Label("No upcoming events scheduled.");
            none.getStyleClass().add("field-label");
            list.getChildren().add(none);
        } else {
            for (SchoolEvent ev : events) {
                Label row = new Label(String.format("%s — %s", ev.getTitle(),
                        ev.getEventDate() != null ? ev.getEventDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "?"));
                row.getStyleClass().add("field-label");
                list.getChildren().add(row);
            }
        }

        tile.getChildren().addAll(caption, list, hintLabel());
        return tile;
    }

    private static void updateClock(Label dateLabel, Label timeLabel) {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DATE_FMT));
        timeLabel.setText(now.format(TIME_FMT));
    }
}