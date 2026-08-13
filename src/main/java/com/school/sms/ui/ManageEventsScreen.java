package com.school.sms.ui;

import com.school.sms.dao.SchoolEventDAO;
import com.school.sms.model.SchoolEvent;
import com.school.sms.model.User;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class ManageEventsScreen {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static void show(Stage stage, User user) {
        SchoolEventDAO eventDAO = new SchoolEventDAO();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox addCard = new VBox(8);
        addCard.getStyleClass().add("card");
        Label addTitle = new Label("Add School Event");
        addTitle.getStyleClass().add("section-label");

        TextField titleField = new TextField();
        titleField.setPromptText("Event title (e.g. Term 2 Opening, Sports Day)");
        DatePicker dateField = new DatePicker();
        dateField.setPromptText("Event date");
        TextField descField = new TextField();
        descField.setPromptText("Description (optional)");

        Button addBtn = new Button("Add Event");
        addBtn.getStyleClass().add("primary-button");
        Label status = new Label();

        addCard.getChildren().addAll(addTitle, titleField, dateField, descField, addBtn, status);

        VBox listCard = new VBox(8);
        listCard.getStyleClass().add("card");
        Label listTitle = new Label("All Events");
        listTitle.getStyleClass().add("section-label");

        ListView<SchoolEvent> eventList = new ListView<>();
        eventList.setPrefHeight(300);
        eventList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SchoolEvent ev, boolean empty) {
                super.updateItem(ev, empty);
                setText(empty || ev == null ? null :
                        String.format("%s — %s%s", ev.getTitle(),
                                ev.getEventDate() != null ? ev.getEventDate().format(DISPLAY_FMT) : "?",
                                ev.getDescription() != null && !ev.getDescription().isBlank() ? " (" + ev.getDescription() + ")" : ""));
            }
        });
        eventList.getItems().setAll(eventDAO.findAll());

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("danger-button");

        addBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            if (title.isBlank() || dateField.getValue() == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Title and date are required.");
                return;
            }
            SchoolEvent event = new SchoolEvent();
            event.setTitle(title);
            event.setEventDate(dateField.getValue());
            event.setDescription(descField.getText());
            event.setCreatedByUserId(user.getId());
            eventDAO.insert(event);

            eventList.getItems().setAll(eventDAO.findAll());
            titleField.clear();
            dateField.setValue(null);
            descField.clear();
            status.getStyleClass().setAll("status-success");
            status.setText("Event added.");
        });

        deleteBtn.setOnAction(e -> {
            SchoolEvent selected = eventList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Select an event to delete first.");
                return;
            }
            eventDAO.delete(selected.getId());
            eventList.getItems().setAll(eventDAO.findAll());
            status.getStyleClass().setAll("status-success");
            status.setText("Event deleted.");
        });

        listCard.getChildren().addAll(listTitle, eventList, deleteBtn);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(addCard, listCard, back);
        MainShell.setCenter(root, "Manage School Events");
    }
}