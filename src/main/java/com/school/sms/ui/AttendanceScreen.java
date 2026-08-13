package com.school.sms.ui;

import com.school.sms.dao.AttendanceDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.model.AttendanceRecord;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.AttendanceService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Teacher-only: loads the expected roll for a class (all active students in
 * it) and lets the teacher tick off who returned this term. Whoever isn't
 * ticked is implicitly "not yet returned".
 */
public class AttendanceScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        AttendanceDAO attendanceDAO = new AttendanceDAO();
        AttendanceService attendanceService = new AttendanceService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox loadCard = new VBox(8);
        loadCard.getStyleClass().add("card");
        Label sectionOne = new Label("Load Class Roll");
        sectionOne.getStyleClass().add("section-label");

        TextField classField = new TextField(user.getAssignedClass() != null ? user.getAssignedClass() : "");
        classField.setPromptText("Class (e.g. S1, Primary 4)");
        TextField termField = new TextField();
        termField.setPromptText("Term (e.g. Term 1 2026)");
        Button loadBtn = new Button("Load Roll");
        loadBtn.getStyleClass().add("primary-button");

        loadCard.getChildren().addAll(sectionOne, classField, termField, loadBtn);

        VBox rollCard = new VBox(6);
        rollCard.getStyleClass().add("card");

        loadBtn.setOnAction(e -> {
            rollCard.getChildren().clear();
            String className = classField.getText().trim();
            String term = termField.getText().trim();
            if (className.isBlank() || term.isBlank()) {
                Label err = new Label("Enter both class and term.");
                err.getStyleClass().add("status-error");
                rollCard.getChildren().add(err);
                return;
            }

            List<Student> roll = studentDAO.findByClass(className);
            if (roll.isEmpty()) {
                Label none = new Label("No students found in that class.");
                none.getStyleClass().add("field-label");
                rollCard.getChildren().add(none);
                return;
            }

            Label header = new Label(className + " — " + term + " (" + roll.size() + " students)");
            header.getStyleClass().add("section-label");
            rollCard.getChildren().add(header);

            for (Student s : roll) {
                CheckBox cb = new CheckBox(s.getFullName() + " (" + s.getStudentId() + ")");
                AttendanceRecord existing = attendanceDAO.findOne(s.getId(), term);
                cb.setSelected(existing != null && existing.isReturned());

                cb.setOnAction(ev -> attendanceService.setReturned(s.getId(), term, cb.isSelected(), user.getId(),
                        s.getFullName() + " (" + s.getStudentId() + ")"));

                rollCard.getChildren().add(cb);
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(loadCard, rollCard, back);
        MainShell.setCenter(root, "Attendance / Return Confirmation");
    }
}
