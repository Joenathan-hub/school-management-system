package com.school.sms.ui;

import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.PaymentService;
import com.school.sms.service.PrintService;
import com.school.sms.service.StudentService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PaymentScreen {

    public static void show(Stage stage, User user) {
        StudentService studentService = new StudentService();
        PaymentService paymentService = new PaymentService();
        PrintService printService = new PrintService();

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("card");

        Label sectionOne = new Label("Find Student");
        sectionOne.getStyleClass().add("section-label");

        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Enter Student ID (e.g. S-SHS202607-4821)");
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("secondary-button");

        Label studentInfo = new Label("No student loaded.");
        studentInfo.getStyleClass().add("field-label");
        studentInfo.setWrapText(true);

        Label sectionTwo = new Label("Payment Details");
        sectionTwo.getStyleClass().add("section-label");

        TextField termField = new TextField();
        termField.setPromptText("Term (e.g. Term 1 2026)");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount paid (UGX)");

        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optional)");

        Button recordBtn = new Button("Record Payment");
        recordBtn.getStyleClass().add("primary-button");
        Button printBtn = new Button("Print Last Receipt");
        printBtn.getStyleClass().add("accent-button");
        printBtn.setDisable(true);

        Label status = new Label();

        final Student[] loadedStudent = new Student[1];
        final PaymentService.PaymentResult[] lastResult = new PaymentService.PaymentResult[1];

        searchBtn.setOnAction(e -> {
            Student s = studentService.findByStudentId(studentIdField.getText().trim());
            if (s == null) {
                studentInfo.setText("No student found with that ID.");
                loadedStudent[0] = null;
            } else {
                loadedStudent[0] = s;
                studentInfo.setText(String.format("%s | %s | Class: %s",
                        s.getFullName(), s.getStudentId(), s.getStudentClass()));
            }
        });

        recordBtn.setOnAction(e -> {
            if (loadedStudent[0] == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Search for and load a student first.");
                return;
            }
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                PaymentService.PaymentResult result = paymentService.recordPayment(
                        loadedStudent[0], termField.getText(), amount, notesField.getText(), user.getId());

                lastResult[0] = result;
                printBtn.setDisable(false);

                status.getStyleClass().setAll("status-success");
                status.setText(String.format("Receipt #%d recorded. New balance: UGX %,.0f. SMS sent (or queued if offline).",
                        result.payment.getReceiptNumber(), result.newBalance));
            } catch (NumberFormatException ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Amount must be a number.");
            } catch (Exception ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Error: " + ex.getMessage());
            }
        });

        printBtn.setOnAction(e -> {
            if (lastResult[0] != null) {
                printService.printReceipt(lastResult[0].payment, loadedStudent[0], lastResult[0].newBalance);
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(
                sectionOne,
                new HBox(8, studentIdField, searchBtn),
                studentInfo,
                new Separator(),
                sectionTwo,
                termField, amountField, notesField,
                new HBox(8, recordBtn, printBtn),
                status,
                back
        );

        MainShell.setCenter(root, "Record Payment");
    }
}
