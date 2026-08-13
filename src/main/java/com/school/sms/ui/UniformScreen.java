package com.school.sms.ui;

import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Student;
import com.school.sms.model.UniformItem;
import com.school.sms.model.User;
import com.school.sms.service.UniformService;
import com.school.sms.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Uniform checklist: search for a student, then tick off which uniform
 * items they've received. Available to bursar and administrator only.
 */
public class UniformScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        UniformService uniformService = new UniformService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox searchCard = new VBox(8);
        searchCard.getStyleClass().add("card");
        Label sectionOne = new Label("Find Student");
        sectionOne.getStyleClass().add("section-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Search student by name or Student ID");
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("secondary-button");

        ListView<Student> resultsList = new ListView<>();
        resultsList.setPrefHeight(110);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null :
                        String.format("%s | %s | Class: %s", s.getStudentId(), s.getFullName(), s.getStudentClass()));
            }
        });

        TextField termField = new TextField();
        termField.setPromptText("Term (e.g. Term 1 2026)");
        Button loadBtn = new Button("Load Checklist");
        loadBtn.getStyleClass().add("primary-button");

        searchCard.getChildren().addAll(sectionOne, new HBox(8, searchField, searchBtn), resultsList,
                new HBox(8, termField, loadBtn));

        VBox checklistCard = new VBox(6);
        checklistCard.getStyleClass().add("card");
        Label checklistStatus = new Label();

        searchBtn.setOnAction(e -> {
            List<Student> results = studentDAO.search(searchField.getText().trim());
            resultsList.getItems().setAll(results);
        });

        loadBtn.setOnAction(e -> {
            Student selected = resultsList.getSelectionModel().getSelectedItem();
            String term = termField.getText().trim();
            checklistCard.getChildren().clear();
            checklistStatus.setText("");

            if (selected == null) {
                checklistStatus.getStyleClass().setAll("status-error");
                checklistStatus.setText("Select a student from the search results first.");
                checklistCard.getChildren().add(checklistStatus);
                return;
            }
            if (term.isBlank()) {
                checklistStatus.getStyleClass().setAll("status-error");
                checklistStatus.setText("Enter a term first.");
                checklistCard.getChildren().add(checklistStatus);
                return;
            }

            List<UniformItem> items = uniformService.getOrCreateChecklist(selected.getId(), term);
            Label studentHeader = new Label(selected.getFullName() + " (" + selected.getStudentId() + ") — " + term);
            studentHeader.getStyleClass().add("section-label");
            checklistCard.getChildren().add(studentHeader);

            for (UniformItem item : items) {
                CheckBox cb = new CheckBox(item.getItemName());
                cb.setSelected(item.isReceived());

                Label timestamp = new Label(item.isReceived() && item.getDateReceived() != null
                        ? "received " + item.getDateReceived().format(DateTimeUtil.DATE_TIME)
                        : "not yet received");
                timestamp.getStyleClass().add("field-label");

                cb.setOnAction(ev -> {
                    boolean nowReceived = cb.isSelected();
                    uniformService.setItemReceived(item.getId(), nowReceived, user.getId(),
                            selected.getFullName() + " (" + selected.getStudentId() + ")", item.getItemName());
                    timestamp.setText(nowReceived
                            ? "received " + java.time.LocalDateTime.now().format(DateTimeUtil.DATE_TIME)
                            : "not yet received");
                });

                HBox row = new HBox(10, cb, timestamp);
                checklistCard.getChildren().add(row);
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(searchCard, checklistCard, back);

        MainShell.setCenter(root, "Uniform Checklist");
    }
}
