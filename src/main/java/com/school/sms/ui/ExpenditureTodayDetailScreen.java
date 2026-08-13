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

public class ExpenditureTodayDetailScreen {

    public static void show(Stage stage, User user) {
        DashboardKpiService kpiService = new DashboardKpiService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label totalLabel = new Label(String.format("Total Expenditure Today: UGX %,.0f", kpiService.getExpenditureTodayTotal()));
        totalLabel.getStyleClass().add("section-label");

        List<DashboardKpiService.ExpenditureKpiRow> rows = kpiService.getExpenditureTodayDetails();

        ListView<DashboardKpiService.ExpenditureKpiRow> list = new ListView<>();
        list.setPrefHeight(420);
        list.getItems().setAll(rows);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DashboardKpiService.ExpenditureKpiRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null :
                        String.format("%s [%s] — UGX %,.0f (recorded by %s at %s)", row.description, row.category, row.amount, row.recordedBy, row.time));
            }
        });

        card.getChildren().add(totalLabel);
        if (rows.isEmpty()) {
            Label emptyHint = new Label("No expenditure recorded yet today.");
            emptyHint.getStyleClass().add("field-label");
            card.getChildren().add(emptyHint);
        } else {
            card.getChildren().add(list);
        }

        Button printBtn = new Button("Print List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> printRows = new ArrayList<>();
            for (DashboardKpiService.ExpenditureKpiRow row : rows) {
                printRows.add(new String[]{row.description, row.category, String.format("%.0f", row.amount), row.recordedBy, row.time});
            }
            printService.printList("Expenditure Today", new String[]{"Description", "Category", "Amount", "Recorded By", "Time"}, printRows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("expenditure-today.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> exportRows = new ArrayList<>();
            for (DashboardKpiService.ExpenditureKpiRow row : rows) {
                exportRows.add(new String[]{row.description, row.category, String.format("%.0f", row.amount), row.recordedBy, row.time});
            }
            try {
                excelExportService.export("Expenditure Today", new String[]{"Description", "Category", "Amount", "Recorded By", "Time"}, exportRows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(card, new HBox(10, printBtn, exportBtn), back);
        MainShell.setCenter(root, "Expenditure Today");
    }
}