package com.school.sms.ui;

import com.school.sms.model.Guardian;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.StudentService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Admission form: student details, father, mother/guardian, and the fee
 * structure for the current term (with optional admin-applied discount).
 * Both bursar and administrator can use this screen (per your spec).
 */
public class AdmissionFormScreen {

    public static void show(Stage stage, User user) {
        StudentService studentService = new StudentService();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20));
        ColumnConstraints c1 = new ColumnConstraints(140);
        grid.getColumnConstraints().add(c1);

        int row = 0;

        TextField nameField = new TextField();
        grid.add(sectionLabel("Student Full Name:"), 0, row); grid.add(nameField, 1, row++);

        TextField ageField = new TextField();
        grid.add(sectionLabel("Age:"), 0, row); grid.add(ageField, 1, row++);

        ComboBox<String> sexBox = new ComboBox<>();
        sexBox.getItems().addAll("Male", "Female");
        grid.add(sectionLabel("Sex:"), 0, row); grid.add(sexBox, 1, row++);

        TextField classField = new TextField();
        classField.setPromptText("e.g. S1, Primary 4");
        grid.add(sectionLabel("Class:"), 0, row); grid.add(classField, 1, row++);

        ComboBox<String> boardingBox = new ComboBox<>();
        boardingBox.getItems().addAll("Day", "Boarding");
        boardingBox.setValue("Day");
        grid.add(sectionLabel("Boarding Status:"), 0, row); grid.add(boardingBox, 1, row++);

        DatePicker admissionDateField = new DatePicker(java.time.LocalDate.now());
        grid.add(sectionLabel("Date Reported:"), 0, row); grid.add(admissionDateField, 1, row++);

        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(heading("Father's Details"), 0, row++, 2, 1);

        TextField fatherName = new TextField();
        grid.add(sectionLabel("Full Name:"), 0, row); grid.add(fatherName, 1, row++);
        TextField fatherOccupation = new TextField();
        grid.add(sectionLabel("Occupation:"), 0, row); grid.add(fatherOccupation, 1, row++);
        TextField fatherContact = new TextField();
        fatherContact.setPromptText("e.g. 0771234567");
        grid.add(sectionLabel("Contact:"), 0, row); grid.add(fatherContact, 1, row++);

        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(heading("Mother / Guardian Details"), 0, row++, 2, 1);

        TextField motherName = new TextField();
        grid.add(sectionLabel("Full Name:"), 0, row); grid.add(motherName, 1, row++);
        TextField motherOccupation = new TextField();
        grid.add(sectionLabel("Occupation:"), 0, row); grid.add(motherOccupation, 1, row++);
        TextField motherContact = new TextField();
        motherContact.setPromptText("e.g. 0771234567");
        grid.add(sectionLabel("Contact:"), 0, row); grid.add(motherContact, 1, row++);

        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(heading("Fee Structure"), 0, row++, 2, 1);

        TextField termField = new TextField();
        termField.setPromptText("e.g. Term 1 2026");
        grid.add(sectionLabel("Term:"), 0, row); grid.add(termField, 1, row++);

        TextField baseFeeField = new TextField();
        baseFeeField.setPromptText("Class fee, e.g. 500000");
        grid.add(sectionLabel("Base Fee (UGX):"), 0, row); grid.add(baseFeeField, 1, row++);

        TextField discountField = new TextField("0");
        grid.add(sectionLabel("Discount (UGX):"), 0, row); grid.add(discountField, 1, row++);

        TextField discountReasonField = new TextField();
        discountReasonField.setPromptText("optional, e.g. sibling discount");
        grid.add(sectionLabel("Discount Reason:"), 0, row); grid.add(discountReasonField, 1, row++);

        Label status = new Label();
        Button submit = new Button("Register Student");
        submit.getStyleClass().add("primary-button");
        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");

        submit.setOnAction(e -> {
            try {
                Guardian father = fatherName.getText().isBlank() ? null : new Guardian(0,
                        fatherName.getText(), "Father", fatherOccupation.getText(), fatherContact.getText());
                Guardian mother = motherName.getText().isBlank() ? null : new Guardian(0,
                        motherName.getText(), "Mother/Guardian", motherOccupation.getText(), motherContact.getText());

                Student student = studentService.admitStudent(
                        nameField.getText(),
                        Integer.parseInt(ageField.getText().trim()),
                        sexBox.getValue(),
                        classField.getText(),
                        boardingBox.getValue(),
                        father, mother,
                        termField.getText(),
                        Double.parseDouble(baseFeeField.getText().trim()),
                        Double.parseDouble(discountField.getText().trim()),
                        discountReasonField.getText(),
                        user.getId(), admissionDateField.getValue()
                );

                status.getStyleClass().setAll("status-success");
                status.setText("Registered! Student ID: " + student.getStudentId());
            } catch (NumberFormatException ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Age, Base Fee, and Discount must be numbers.");
            } catch (Exception ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Error: " + ex.getMessage());
            }
        });

        back.setOnAction(e -> MainShell.showHome());

        grid.add(status, 0, row++, 2, 1);
        HBox buttons = new HBox(10, submit, back);
        grid.add(buttons, 0, row++, 2, 1);

        grid.getStyleClass().add("card");
        MainShell.setCenter(grid, "Admit Student");
    }

    private static Label heading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
