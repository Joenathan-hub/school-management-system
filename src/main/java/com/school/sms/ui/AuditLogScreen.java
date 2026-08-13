package com.school.sms.ui;

import com.school.sms.model.AuditLogEntry;
import com.school.sms.model.User;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.ExcelExportService;
import com.school.sms.service.PrintService;
import com.school.sms.util.DateTimeUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrator-only: browse and search the full audit trail. Shows the
 * 200 most recent entries by default; search and date filters run against
 * the full history.
 */
public class AuditLogScreen {

    private static final int DEFAULT_LIMIT = 200;

    public static void show(Stage stage, User user) {
        AuditLogService auditLogService = new AuditLogService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox filterCard = new VBox(8);
        filterCard.getStyleClass().add("card");
        Label title = new Label("Audit Log");
        title.getStyleClass().add("section-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by action, details, or staff name");

        DatePicker fromDate = new DatePicker();
        fromDate.setPromptText("From date (optional)");
        DatePicker toDate = new DatePicker();
        toDate.setPromptText("To date (optional)");

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        Button clearBtn = new Button("Clear / Show Recent");
        clearBtn.getStyleClass().add("secondary-button");

        Label resultCount = new Label();
        resultCount.getStyleClass().add("field-label");

        filterCard.getChildren().addAll(title, searchField, new HBox(10, fromDate, toDate),
                new HBox(10, searchBtn, clearBtn), resultCount);

        VBox resultsCard = new VBox(8);
        resultsCard.getStyleClass().add("card");

        ListView<AuditLogEntry> resultsList = new ListView<>();
        resultsList.setPrefHeight(420);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AuditLogEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setText(null);
                } else {
                    String who = entry.getUserFullName() != null ? entry.getUserFullName() : "Unknown/removed user";
                    String when = entry.getTimestamp() != null ? entry.getTimestamp().format(DateTimeUtil.DATE_TIME) : "?";
                    setText(String.format("[%s] %s — %s: %s", when, who, entry.getAction(),
                            entry.getDetails() != null ? entry.getDetails() : ""));
                    setWrapText(true);
                }
            }
        });

        Runnable loadRecent = () -> {
            List<AuditLogEntry> recent = auditLogService.getRecent(DEFAULT_LIMIT);
            resultsList.getItems().setAll(recent);
            resultCount.setText("Showing " + recent.size() + " most recent entries.");
        };

        searchBtn.setOnAction(e -> {
            List<AuditLogEntry> results = auditLogService.search(searchField.getText(), fromDate.getValue(), toDate.getValue());
            resultsList.getItems().setAll(results);
            resultCount.setText(results.size() + " entr" + (results.size() == 1 ? "y" : "ies") + " found.");
        });

        clearBtn.setOnAction(e -> {
            searchField.clear();
            fromDate.setValue(null);
            toDate.setValue(null);
            loadRecent.run();
        });

        loadRecent.run();

        Button printBtn = new Button("Print List");
        printBtn.getStyleClass().add("secondary-button");
        printBtn.setOnAction(e -> {
            List<String[]> rows = new ArrayList<>();
            for (AuditLogEntry entry : resultsList.getItems()) {
                rows.add(new String[]{
                        entry.getTimestamp() != null ? entry.getTimestamp().format(DateTimeUtil.DATE_TIME) : "",
                        entry.getUserFullName() != null ? entry.getUserFullName() : "Unknown/removed user",
                        entry.getAction() != null ? entry.getAction() : "",
                        entry.getDetails() != null ? entry.getDetails() : ""
                });
            }
            printService.printList("Audit Log", new String[]{"Date/Time", "Staff", "Action", "Details"}, rows);
        });

        Button exportBtn = new Button("Export to Excel");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("audit-log.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;

            List<String[]> rows = new ArrayList<>();
            for (AuditLogEntry entry : resultsList.getItems()) {
                rows.add(new String[]{
                        entry.getTimestamp() != null ? entry.getTimestamp().format(DateTimeUtil.DATE_TIME) : "",
                        entry.getUserFullName() != null ? entry.getUserFullName() : "Unknown/removed user",
                        entry.getAction() != null ? entry.getAction() : "",
                        entry.getDetails() != null ? entry.getDetails() : ""
                });
            }
            try {
                excelExportService.export("Audit Log", new String[]{"Date/Time", "Staff", "Action", "Details"}, rows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        resultsCard.getChildren().addAll(resultsList, new HBox(10, printBtn, exportBtn));

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(filterCard, resultsCard, back);
        MainShell.setCenter(root, "Audit Log");
    }
}