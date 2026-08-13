package com.school.sms.ui;

import com.school.sms.model.Role;
import com.school.sms.model.SchoolEvent;
import com.school.sms.model.User;
import com.school.sms.service.DashboardKpiService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UpcomingEventsDetailScreen {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");

    public static void show(Stage stage, User user) {
        DashboardKpiService kpiService = new DashboardKpiService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        Label title = new Label("Upcoming Events");
        title.getStyleClass().add("section-label");
        card.getChildren().add(title);

        List<SchoolEvent> events = kpiService.getAllUpcomingEvents();
        if (events.isEmpty()) {
            Label none = new Label("No upcoming events scheduled.");
            none.getStyleClass().add("field-label");
            card.getChildren().add(none);
        } else {
            ListView<SchoolEvent> list = new ListView<>();
            list.setPrefHeight(420);
            list.getItems().setAll(events);
            list.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(SchoolEvent ev, boolean empty) {
                    super.updateItem(ev, empty);
                    setText(empty || ev == null ? null :
                            String.format("%s — %s%s", ev.getTitle(),
                                    ev.getEventDate() != null ? ev.getEventDate().format(DISPLAY_FMT) : "?",
                                    ev.getDescription() != null && !ev.getDescription().isBlank() ? "\n" + ev.getDescription() : ""));
                    setWrapText(true);
                }
            });
            card.getChildren().add(list);
        }

        if (user.getRole() == Role.ADMINISTRATOR) {
            Button manageBtn = new Button("Manage Events");
            manageBtn.getStyleClass().add("primary-button");
            manageBtn.setOnAction(e -> ManageEventsScreen.show(stage, user));
            card.getChildren().add(manageBtn);
        }

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(card, back);
        MainShell.setCenter(root, "Upcoming Events");
    }
}