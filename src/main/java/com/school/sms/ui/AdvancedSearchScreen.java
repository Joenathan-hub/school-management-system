package com.school.sms.ui;

import com.school.sms.model.Student;
import com.school.sms.model.StudentBalanceInfo;
import com.school.sms.model.User;
import com.school.sms.service.AdvancedSearchService;
import com.school.sms.service.TermService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class AdvancedSearchScreen {

    public static void show(Stage stage, User user) {
        AdvancedSearchService searchService = new AdvancedSearchService();
        TermService termService = new TermService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox balanceCard = new VBox(8);
        balanceCard.getStyleClass().add("card");
        Label balanceTitle = new Label("Search by Fee Balance");
        balanceTitle.getStyleClass().add("section-label");

        TextField balanceTermField = new TextField(termService.getActiveTermName());
        balanceTermField.setPromptText("Term (e.g. Term 1 2026)");

        ComboBox<AdvancedSearchService.BalanceMode> balanceModeBox = new ComboBox<>();
        balanceModeBox.getItems().addAll(AdvancedSearchService.BalanceMode.values());
        balanceModeBox.setValue(AdvancedSearchService.BalanceMode.NOT_CLEARED);
        balanceModeBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(AdvancedSearchService.BalanceMode m) {
                if (m == null) return "";
                return switch (m) {
                    case NOT_CLEARED -> "Not cleared (balance > 0)";
                    case FULLY_CLEARED -> "Fully cleared (balance = 0)";
                    case AT_OR_BELOW -> "At or below amount";
                    case AT_OR_ABOVE -> "At or above amount";
                };
            }
            @Override public AdvancedSearchService.BalanceMode fromString(String s) { return null; }
        });

        TextField balanceAmountField = new TextField();
        balanceAmountField.setPromptText("Amount (UGX) — only needed for 'at or below/above'");

        Button balanceSearchBtn = new Button("Search");
        balanceSearchBtn.getStyleClass().add("primary-button");

        ListView<StudentBalanceInfo> balanceResults = new ListView<>();
        balanceResults.setPrefHeight(200);
        balanceResults.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(StudentBalanceInfo info, boolean empty) {
                super.updateItem(info, empty);
                setText(empty || info == null ? null : String.format("%s | %s | Class: %s | Balance: UGX %,.0f",
                        info.getStudent().getStudentId(), info.getStudent().getFullName(),
                        info.getStudent().getStudentClass(), info.getBalance()));
            }
        });

        Label balanceStatus = new Label();

        balanceSearchBtn.setOnAction(e -> {
            try {
                double amount = balanceAmountField.getText().isBlank() ? 0 : Double.parseDouble(balanceAmountField.getText().trim());
                List<StudentBalanceInfo> results = searchService.searchByFeeBalance(
                        balanceTermField.getText().trim(), balanceModeBox.getValue(), amount);
                balanceResults.getItems().setAll(results);
                balanceStatus.getStyleClass().setAll("status-success");
                balanceStatus.setText(results.size() + " student(s) found.");
            } catch (NumberFormatException ex) {
                balanceStatus.getStyleClass().setAll("status-error");
                balanceStatus.setText("Amount must be a number.");
            }
        });

        balanceCard.getChildren().addAll(balanceTitle, balanceTermField, balanceModeBox, balanceAmountField,
                balanceSearchBtn, balanceStatus, balanceResults);

        VBox reqCard = new VBox(8);
        reqCard.getStyleClass().add("card");
        Label reqTitle = new Label("Search by Requirements Status");
        reqTitle.getStyleClass().add("section-label");

        TextField reqTermField = new TextField(termService.getActiveTermName());
        reqTermField.setPromptText("Term (e.g. Term 1 2026)");

        ComboBox<AdvancedSearchService.RequirementMode> reqModeBox = new ComboBox<>();
        reqModeBox.getItems().addAll(AdvancedSearchService.RequirementMode.values());
        reqModeBox.setValue(AdvancedSearchService.RequirementMode.NOT_FULLY_CLEARED);
        reqModeBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(AdvancedSearchService.RequirementMode m) {
                if (m == null) return "";
                return switch (m) {
                    case MISSING_ITEM -> "Didn't bring a specific item";
                    case NOT_FULLY_CLEARED -> "Hasn't cleared all requirements";
                };
            }
            @Override public AdvancedSearchService.RequirementMode fromString(String s) { return null; }
        });

        TextField reqItemField = new TextField();
        reqItemField.setPromptText("Item name — only needed for 'didn't bring a specific item'");

        Button reqSearchBtn = new Button("Search");
        reqSearchBtn.getStyleClass().add("primary-button");

        ListView<Student> reqResults = new ListView<>();
        reqResults.setPrefHeight(200);
        reqResults.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : String.format("%s | %s | Class: %s",
                        s.getStudentId(), s.getFullName(), s.getStudentClass()));
            }
        });

        Label reqStatus = new Label();

        reqSearchBtn.setOnAction(e -> {
            List<Student> results = searchService.searchByRequirements(
                    reqTermField.getText().trim(), reqModeBox.getValue(), reqItemField.getText().trim());
            reqResults.getItems().setAll(results);
            reqStatus.getStyleClass().setAll("status-success");
            reqStatus.setText(results.size() + " student(s) found.");
        });

        reqCard.getChildren().addAll(reqTitle, reqTermField, reqModeBox, reqItemField, reqSearchBtn, reqStatus, reqResults);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(balanceCard, reqCard, back);
        MainShell.setCenter(root, "Search Students");
    }
}