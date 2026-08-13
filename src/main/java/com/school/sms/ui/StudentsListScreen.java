package com.school.sms.ui;

import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.ExcelExportService;
import com.school.sms.service.PrintService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StudentsListScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox listCard = new VBox(8);
        listCard.getStyleClass().add("card");
        Label title = new Label("All Students");
        title.getStyleClass().add("section-label");

        ListView<Student> listView = new ListView<>();
        listView.setPrefHeight(360);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null :
                        String.format("%s | %s | Class: %s | Age: %d", s.getStudentId(), s.getFullName(), s.getStudentClass(), s.getAge()));
            }
        });
        listView.getItems().setAll(studentDAO.findAll());

        Label status = new Label();

        Button printBtn = new Button("Print Student List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> rows = new ArrayList<>();
            for (Student s : listView.getItems()) {
                rows.add(new String[]{s.getStudentId(), s.getFullName(), s.getStudentClass(), String.valueOf(s.getAge()), s.getSex()});
            }
            printService.printList("Student List", new String[]{"ID", "Name", "Class", "Age", "Sex"}, rows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("students.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            List<String[]> rows = new ArrayList<>();
            for (Student s : listView.getItems()) {
                rows.add(new String[]{s.getStudentId(), s.getFullName(), s.getStudentClass(), String.valueOf(s.getAge()), s.getSex()});
            }
            try {
                excelExportService.export("Students", new String[]{"ID", "Name", "Class", "Age", "Sex"}, rows, file.getAbsolutePath());
                status.getStyleClass().setAll("status-success");
                status.setText("Exported to " + file.getAbsolutePath());
            } catch (Exception ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Export failed: " + ex.getMessage());
            }
        });

        listCard.getChildren().addAll(title, listView, new HBox(10, printBtn, exportBtn), status);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(listCard, back);
        MainShell.setCenter(root, "Students List");
    }
}
