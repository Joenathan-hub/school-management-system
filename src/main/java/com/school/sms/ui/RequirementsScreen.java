package com.school.sms.ui;

import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Requirement;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.RequirementService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Reporting-day requirements checklist (mattress, books, etc.) — same
 * search-then-check pattern as UniformScreen, separate module since it
 * covers a different set of items. Bursar and administrator only.
 */
public class RequirementsScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        RequirementService requirementService = new RequirementService();

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

        searchBtn.setOnAction(e -> {
            List<Student> results = studentDAO.search(searchField.getText().trim());
            resultsList.getItems().setAll(results);
        });

        loadBtn.setOnAction(e -> {
            Student selected = resultsList.getSelectionModel().getSelectedItem();
            String term = termField.getText().trim();
            checklistCard.getChildren().clear();

            if (selected == null || term.isBlank()) {
                Label err = new Label(selected == null ? "Select a student first." : "Enter a term first.");
                err.getStyleClass().add("status-error");
                checklistCard.getChildren().add(err);
                return;
            }

            List<Requirement> items = requirementService.getOrCreateChecklist(selected.getId(), term);
            Label studentHeader = new Label(selected.getFullName() + " (" + selected.getStudentId() + ") — " + term);
            studentHeader.getStyleClass().add("section-label");
            checklistCard.getChildren().add(studentHeader);

            for (Requirement item : items) {
                CheckBox cb = new CheckBox(item.getItemName());
                cb.setSelected(item.isBrought());
                cb.setOnAction(ev -> requirementService.setItemBrought(item.getId(), cb.isSelected(), user.getId(),
                        selected.getFullName() + " (" + selected.getStudentId() + ")", item.getItemName()));
                checklistCard.getChildren().add(cb);
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(searchCard, checklistCard, back);
        MainShell.setCenter(root, "Requirements Checklist");
    }
}
