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
import java.util.Map;

public class OutstandingBalanceDetailScreen {

    public static void show(Stage stage, User user) {
        DashboardKpiService kpiService = new DashboardKpiService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox summaryCard = new VBox(8);
        summaryCard.getStyleClass().add("card");
        Label totalLabel = new Label(String.format("Total Outstanding: UGX %,.0f", kpiService.getOutstandingTotal()));
        totalLabel.getStyleClass().add("section-label");
        summaryCard.getChildren().add(totalLabel);

        Map<String, Double> byClass = kpiService.getOutstandingByClass();
        if (byClass.isEmpty()) {
            Label none = new Label("No active term set, or no outstanding balances.");
            none.getStyleClass().add("field-label");
            summaryCard.getChildren().add(none);
        } else {
            byClass.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        Label row = new Label(String.format("%s: UGX %,.0f", entry.getKey(), entry.getValue()));
                        row.getStyleClass().add("field-label");
                        summaryCard.getChildren().add(row);
                    });
        }

        VBox listCard = new VBox(8);
        listCard.getStyleClass().add("card");
        Label listTitle = new Label("By Student");
        listTitle.getStyleClass().add("section-label");

        List<DashboardKpiService.OutstandingRow> rows = kpiService.getOutstandingByStudentDetails();
        ListView<DashboardKpiService.OutstandingRow> list = new ListView<>();
        list.setPrefHeight(360);
        list.getItems().setAll(rows);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DashboardKpiService.OutstandingRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null :
                        String.format("%s (%s) | Class: %s | Balance: UGX %,.0f",
                                row.student.getFullName(), row.student.getStudentId(), row.student.getStudentClass(), row.balance));
            }
        });
        listCard.getChildren().addAll(listTitle, list);

        Button printBtn = new Button("Print List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> printRows = new ArrayList<>();
            for (DashboardKpiService.OutstandingRow row : rows) {
                printRows.add(new String[]{row.student.getStudentId(), row.student.getFullName(),
                        row.student.getStudentClass(), String.format("%.0f", row.balance)});
            }
            printService.printList("Outstanding Balances", new String[]{"ID", "Name", "Class", "Balance"}, printRows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("outstanding-balances.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> exportRows = new ArrayList<>();
            for (DashboardKpiService.OutstandingRow row : rows) {
                exportRows.add(new String[]{row.student.getStudentId(), row.student.getFullName(),
                        row.student.getStudentClass(), String.format("%.0f", row.balance)});
            }
            try {
                excelExportService.export("Outstanding Balances", new String[]{"ID", "Name", "Class", "Balance"}, exportRows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(summaryCard, listCard, new HBox(10, printBtn, exportBtn), back);
        MainShell.setCenter(root, "Outstanding Balance");
    }
}