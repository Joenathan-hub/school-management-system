package com.school.sms.ui;

import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Result;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.PrintService;
import com.school.sms.service.ResultService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * View and print a student's report card. Available to bursar and
 * administrator (per your spec: they can view a student's results/report
 * once it's ready, even though they can't enter results themselves).
 */
public class ReportCardScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        ResultService resultService = new ResultService();
        PrintService printService = new PrintService();

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
        Button loadBtn = new Button("Load Report Card");
        loadBtn.getStyleClass().add("primary-button");
        Button printBtn = new Button("Print Report Card");
        printBtn.getStyleClass().add("accent-button");
        printBtn.setDisable(true);

        searchCard.getChildren().addAll(sectionOne, new HBox(8, searchField, searchBtn), resultsList,
                new HBox(8, termField, loadBtn));

        VBox reportCard = new VBox(4);
        reportCard.getStyleClass().add("card");

        final Student[] loadedStudent = new Student[1];
        final List<Result>[] loadedResults = new List[1];
        final double[] loadedAverage = new double[1];
        final int[] loadedTotal = new int[1];

        searchBtn.setOnAction(e -> {
            List<Student> results = studentDAO.search(searchField.getText().trim());
            resultsList.getItems().setAll(results);
        });

        loadBtn.setOnAction(e -> {
            reportCard.getChildren().clear();
            printBtn.setDisable(true);

            Student selected = resultsList.getSelectionModel().getSelectedItem();
            String term = termField.getText().trim();
            if (selected == null || term.isBlank()) {
                Label err = new Label(selected == null ? "Select a student first." : "Enter a term first.");
                err.getStyleClass().add("status-error");
                reportCard.getChildren().add(err);
                return;
            }

            List<Result> results = resultService.getReportCard(selected.getId(), term);
            if (results.isEmpty()) {
                Label none = new Label("No results entered yet for this student/term.");
                none.getStyleClass().add("field-label");
                reportCard.getChildren().add(none);
                return;
            }

            double average = ResultService.average(results);
            int total = ResultService.total(results);

            loadedStudent[0] = selected;
            loadedResults[0] = results;
            loadedAverage[0] = average;
            loadedTotal[0] = total;

            Label header = new Label(selected.getFullName() + " (" + selected.getStudentId() + ") — " + term);
            header.getStyleClass().add("section-label");
            reportCard.getChildren().add(header);

            for (Result r : results) {
                Label row = new Label(String.format("%s: %d marks (Grade %s) — %s",
                        r.getSubject(), r.getMarks(), r.getGrade(), r.getComment() != null ? r.getComment() : ""));
                row.getStyleClass().add("field-label");
                reportCard.getChildren().add(row);
            }

            Label totalsLabel = new Label(String.format("Total: %d | Average: %.1f", total, average));
            totalsLabel.getStyleClass().add("section-label");
            reportCard.getChildren().add(totalsLabel);

            printBtn.setDisable(false);
        });

        printBtn.setOnAction(e -> {
            if (loadedStudent[0] != null) {
                printService.printReportCard(loadedStudent[0], termField.getText(), loadedResults[0], loadedAverage[0], loadedTotal[0]);
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(searchCard, reportCard, printBtn, back);
        MainShell.setCenter(root, "Report Card");
    }
}
