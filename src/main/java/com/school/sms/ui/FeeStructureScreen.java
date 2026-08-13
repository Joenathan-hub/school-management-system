package com.school.sms.ui;

import com.school.sms.dao.FeeStructureDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.model.FeeStructure;
import com.school.sms.model.Student;
import com.school.sms.model.User;
import com.school.sms.service.TermService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FeeStructureScreen {

    public static void show(Stage stage, User user) {
        StudentDAO studentDAO = new StudentDAO();
        FeeStructureDAO feeDAO = new FeeStructureDAO();
        TermService termService = new TermService();

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

        searchBtn.setOnAction(e -> resultsList.getItems().setAll(studentDAO.search(searchField.getText().trim())));

        searchCard.getChildren().addAll(sectionOne, new HBox(8, searchField, searchBtn), resultsList);

        VBox feeCard = new VBox(8);
        feeCard.getStyleClass().add("card");
        Label sectionTwo = new Label("Set Fee for Term");
        sectionTwo.getStyleClass().add("section-label");

        TextField termField = new TextField(termService.getActiveTermName());
        termField.setPromptText("Term (e.g. Term 1 2026)");

        Label broughtForwardLabel = new Label("Brought forward from previous term: UGX 0");
        broughtForwardLabel.getStyleClass().add("field-label");

        TextField baseFeeField = new TextField();
        baseFeeField.setPromptText("Base fee for this term (UGX)");
        TextField discountField = new TextField("0");
        discountField.setPromptText("Discount (UGX)");
        TextField discountReasonField = new TextField();
        discountReasonField.setPromptText("Discount reason (optional)");

        Button loadBtn = new Button("Load Current Fee");
        loadBtn.getStyleClass().add("secondary-button");
        Button saveBtn = new Button("Save Fee");
        saveBtn.getStyleClass().add("primary-button");
        Label status = new Label();

        final Student[] loadedStudent = new Student[1];

        loadBtn.setOnAction(e -> {
            Student selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Select a student first.");
                return;
            }
            loadedStudent[0] = selected;
            FeeStructure existing = feeDAO.findByStudentAndTerm(selected.getId(), termField.getText().trim());
            if (existing != null) {
                baseFeeField.setText(String.valueOf((long) existing.getBaseFee()));
                discountField.setText(String.valueOf((long) existing.getDiscount()));
                discountReasonField.setText(existing.getDiscountReason() != null ? existing.getDiscountReason() : "");
                broughtForwardLabel.setText(String.format("Brought forward from previous term: UGX %,.0f", existing.getBroughtForward()));
            } else {
                baseFeeField.clear();
                discountField.setText("0");
                discountReasonField.clear();
                broughtForwardLabel.setText("Brought forward from previous term: UGX 0 (no fee record yet for this term)");
            }
            status.getStyleClass().setAll("status-success");
            status.setText("Loaded " + selected.getFullName() + "'s fee record for " + termField.getText().trim() + ".");
        });

        saveBtn.setOnAction(e -> {
            if (loadedStudent[0] == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Load a student's fee record first.");
                return;
            }
            try {
                double baseFee = Double.parseDouble(baseFeeField.getText().trim());
                double discount = Double.parseDouble(discountField.getText().trim());
                feeDAO.upsertBaseFeeAndDiscount(loadedStudent[0].getId(), termField.getText().trim(),
                        baseFee, discount, discountReasonField.getText());
                status.getStyleClass().setAll("status-success");
                status.setText("Fee saved for " + loadedStudent[0].getFullName() + ".");
            } catch (NumberFormatException ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Base fee and discount must be numbers.");
            }
        });

        feeCard.getChildren().addAll(sectionTwo, termField, broughtForwardLabel, baseFeeField, discountField,
                discountReasonField, new HBox(10, loadBtn, saveBtn), status);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(searchCard, feeCard, back);
        MainShell.setCenter(root, "Set Term Fees");
    }
}