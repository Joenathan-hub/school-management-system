package com.school.sms.ui;

import com.school.sms.model.User;
import com.school.sms.service.DashboardKpiService;
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

public class StudentsNotClearedDetailScreen {

    public static void show(Stage stage, User user) {
        DashboardKpiService kpiService = new DashboardKpiService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        List<DashboardKpiService.NotClearedRow> rows = kpiService.getStudentsNotClearedDetails();
        Label totalLabel = new Label(rows.size() + " student(s) not fully cleared.");
        totalLabel.getStyleClass().add("section-label");
        card.getChildren().add(totalLabel);

        ListView<DashboardKpiService.NotClearedRow> list = new ListView<>();
        list.setPrefHeight(420);
        list.getItems().setAll(rows);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DashboardKpiService.NotClearedRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setText(null); return; }
                StringBuilder reason = new StringBuilder();
                if (row.balance > 0) reason.append(String.format("Balance: UGX %,.0f", row.balance));
                if (row.requirementsIncomplete) {
                    if (reason.length() > 0) reason.append(" | ");
                    reason.append("Requirements incomplete");
                }
                setText(String.format("%s (%s) | Class: %s | %s",
                        row.student.getFullName(), row.student.getStudentId(), row.student.getStudentClass(), reason));
            }
        });
        card.getChildren().add(list);

        Button printBtn = new Button("Print List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> printRows = new ArrayList<>();
            for (DashboardKpiService.NotClearedRow row : rows) {
                printRows.add(new String[]{row.student.getStudentId(), row.student.getFullName(), row.student.getStudentClass(),
                        row.balance > 0 ? String.format("UGX %,.0f", row.balance) : "Cleared",
                        row.requirementsIncomplete ? "Incomplete" : "Complete"});
            }
            printService.printList("Students Not Fully Cleared", new String[]{"ID", "Name", "Class", "Fee Balance", "Requirements"}, printRows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("students-not-cleared.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> exportRows = new ArrayList<>();
            for (DashboardKpiService.NotClearedRow row : rows) {
                exportRows.add(new String[]{row.student.getStudentId(), row.student.getFullName(), row.student.getStudentClass(),
                        row.balance > 0 ? String.format("%.0f", row.balance) : "0",
                        row.requirementsIncomplete ? "Incomplete" : "Complete"});
            }
            try {
                excelExportService.export("Not Cleared", new String[]{"ID", "Name", "Class", "Fee Balance", "Requirements"}, exportRows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(card, new HBox(10, printBtn, exportBtn), back);
        MainShell.setCenter(root, "Students Not Fully Cleared");
    }
}