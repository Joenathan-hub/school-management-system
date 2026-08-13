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

public class CollectedTodayDetailScreen {

    public static void show(Stage stage, User user) {
        DashboardKpiService kpiService = new DashboardKpiService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label totalLabel = new Label(String.format("Total Collected Today: UGX %,.0f", kpiService.getCollectedTodayTotal()));
        totalLabel.getStyleClass().add("section-label");

        List<DashboardKpiService.PaymentKpiRow> rows = kpiService.getCollectedTodayDetails();

        ListView<DashboardKpiService.PaymentKpiRow> list = new ListView<>();
        list.setPrefHeight(420);
        list.getItems().setAll(rows);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DashboardKpiService.PaymentKpiRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null :
                        String.format("%s — UGX %,.0f (recorded by %s at %s)", row.studentName, row.amount, row.recordedBy, row.time));
            }
        });

        card.getChildren().add(totalLabel);
        if (rows.isEmpty()) {
            Label emptyHint = new Label("No payments recorded yet today.");
            emptyHint.getStyleClass().add("field-label");
            card.getChildren().add(emptyHint);
        } else {
            card.getChildren().add(list);
        }

        Button printBtn = new Button("Print List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> printRows = new ArrayList<>();
            for (DashboardKpiService.PaymentKpiRow row : rows) {
                printRows.add(new String[]{row.studentName, String.format("%.0f", row.amount), row.recordedBy, row.time});
            }
            printService.printList("Payments Collected Today", new String[]{"Student", "Amount", "Recorded By", "Time"}, printRows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("collected-today.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> exportRows = new ArrayList<>();
            for (DashboardKpiService.PaymentKpiRow row : rows) {
                exportRows.add(new String[]{row.studentName, String.format("%.0f", row.amount), row.recordedBy, row.time});
            }
            try {
                excelExportService.export("Collected Today", new String[]{"Student", "Amount", "Recorded By", "Time"}, exportRows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(card, new HBox(10, printBtn, exportBtn), back);
        MainShell.setCenter(root, "Collected Today");
    }
}