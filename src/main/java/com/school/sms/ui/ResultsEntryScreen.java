package com.school.sms.ui;

import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Result;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.ResultService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Teacher-only screen: search a student (pre-filtered to the teacher's
 * assigned class where possible), enter one subject's marks at a time.
 * Grade is computed automatically; the teacher supplies the comment.
 */
public class ResultsEntryScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        ResultService resultService = new ResultService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox searchCard = new VBox(8);
        searchCard.getStyleClass().add("card");
        Label sectionOne = new Label("Find Student");
        sectionOne.getStyleClass().add("section-label");

        TextField searchField = new TextField();
        if (user.getAssignedClass() != null && !user.getAssignedClass().isBlank()) {
            searchField.setPromptText("Search within " + user.getAssignedClass() + " (name or ID)");
        } else {
            searchField.setPromptText("Search student by name or Student ID");
        }
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

        searchBtn.setOnAction(e -> {
            List<Student> results = studentDAO.search(searchField.getText().trim());
            resultsList.getItems().setAll(results);
        });

        searchCard.getChildren().addAll(sectionOne, new HBox(8, searchField, searchBtn), resultsList);

        VBox entryCard = new VBox(8);
        entryCard.getStyleClass().add("card");
        Label sectionTwo = new Label("Enter Subject Result");
        sectionTwo.getStyleClass().add("section-label");

        TextField termField = new TextField();
        termField.setPromptText("Term (e.g. Term 1 2026)");
        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject (e.g. Mathematics)");
        TextField marksField = new TextField();
        marksField.setPromptText("Marks (0-100)");
        TextField commentField = new TextField();
        commentField.setPromptText("Comment (optional)");
        Button saveBtn = new Button("Save Result");
        saveBtn.getStyleClass().add("primary-button");
        Label status = new Label();

        saveBtn.setOnAction(e -> {
            Student selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Select a student from the search results first.");
                return;
            }
            try {
                int marks = Integer.parseInt(marksField.getText().trim());
                if (marks < 0 || marks > 100) {
                    status.getStyleClass().setAll("status-error");
                    status.setText("Marks must be between 0 and 100.");
                    return;
                }
                Result r = resultService.recordResult(selected.getId(), termField.getText(),
                        subjectField.getText(), marks, commentField.getText(), user.getId());
                status.getStyleClass().setAll("status-success");
                status.setText(String.format("Saved: %s — %d marks (Grade %s)", r.getSubject(), r.getMarks(), r.getGrade()));
                subjectField.clear();
                marksField.clear();
                commentField.clear();
            } catch (NumberFormatException ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Marks must be a whole number.");
            }
        });

        entryCard.getChildren().addAll(sectionTwo, termField, subjectField, marksField, commentField, saveBtn, status);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(searchCard, entryCard, back);
        MainShell.setCenter(root, "Enter Results");
    }
}