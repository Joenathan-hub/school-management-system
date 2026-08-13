package com.school.sms.ui;

import com.school.sms.model.Role;
import com.school.sms.model.User;
import com.school.sms.model.Worker;
import com.school.sms.service.ExcelExportService;
import com.school.sms.service.PrintService;
import com.school.sms.service.WorkerService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorkerScreen {

    public static void show(Stage stage, User user) {
        WorkerService workerService = new WorkerService();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        // --- Add worker (admin only) ---
        if (user.getRole() == Role.ADMINISTRATOR) {
            VBox addCard = new VBox(8);
            addCard.getStyleClass().add("card");
            Label addTitle = new Label("Add Worker");
            addTitle.getStyleClass().add("section-label");
            TextField nameField = new TextField();
            nameField.setPromptText("Full name");
            TextField titleField = new TextField();
            titleField.setPromptText("Job title (e.g. Teacher, Cleaner)");
            TextField contactField = new TextField();
            contactField.setPromptText("Contact, e.g. 0771234567");
            TextField salaryField = new TextField();
            salaryField.setPromptText("Monthly salary (UGX)");
            Button addBtn = new Button("Add Worker");
            addBtn.getStyleClass().add("primary-button");
            Label addStatus = new Label();

            addBtn.setOnAction(e -> {
                try {
                    Worker w = workerService.addWorker(user, nameField.getText(), titleField.getText(),
                            contactField.getText(), Double.parseDouble(salaryField.getText().trim()));
                    addStatus.getStyleClass().setAll("status-success");
                    addStatus.setText("Added! Worker ID: " + w.getWorkerId());
                } catch (NumberFormatException ex) {
                    addStatus.getStyleClass().setAll("status-error");
                    addStatus.setText("Salary must be a number.");
                } catch (Exception ex) {
                    addStatus.getStyleClass().setAll("status-error");
                    addStatus.setText("Error: " + ex.getMessage());
                }
            });

            addCard.getChildren().addAll(addTitle, nameField, titleField, contactField, salaryField, addBtn, addStatus);
            root.getChildren().add(addCard);
        }

        // --- Search + pay worker (bursar and admin) ---
        VBox payCard = new VBox(8);
        payCard.getStyleClass().add("card");
        Label payTitle = new Label("Search & Pay Worker");
        payTitle.getStyleClass().add("section-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or Worker ID");
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("secondary-button");
        ListView<Worker> resultsList = new ListView<>();
        resultsList.setPrefHeight(100);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Worker w, boolean empty) {
                super.updateItem(w, empty);
                setText(empty || w == null ? null :
                        String.format("%s | %s | %s | UGX %,.0f/mo", w.getWorkerId(), w.getFullName(), w.getJobTitle(), w.getMonthlySalary()));
            }
        });

        searchBtn.setOnAction(e -> {
            List<Worker> results = new com.school.sms.dao.WorkerDAO().search(searchField.getText().trim());
            resultsList.getItems().setAll(results);
        });

        TextField forMonthField = new TextField();
        forMonthField.setPromptText("For month, e.g. June 2026");
        TextField payAmountField = new TextField();
        payAmountField.setPromptText("Amount to pay (UGX)");
        Button payBtn = new Button("Pay Worker");
        payBtn.getStyleClass().add("primary-button");
        Button removeBtn = new Button("Remove Worker");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setVisible(user.getRole() == Role.ADMINISTRATOR);
        Label payStatus = new Label();

        payBtn.setOnAction(e -> {
            Worker selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                payStatus.getStyleClass().setAll("status-error");
                payStatus.setText("Select a worker from the list first.");
                return;
            }
            try {
                double amount = Double.parseDouble(payAmountField.getText().trim());
                WorkerService.WorkerPaymentResult result =
                        workerService.payWorker(user, selected, amount, forMonthField.getText(), "");
                payStatus.getStyleClass().setAll("status-success");
                payStatus.setText(String.format("Paid at %s. Balance for %s: UGX %,.0f",
                        result.payment.getPaymentDateTime().format(com.school.sms.util.DateTimeUtil.DATE_TIME),
                        forMonthField.getText(), result.balance));
            } catch (NumberFormatException ex) {
                payStatus.getStyleClass().setAll("status-error");
                payStatus.setText("Amount must be a number.");
            } catch (SecurityException ex) {
                payStatus.getStyleClass().setAll("status-error");
                payStatus.setText(ex.getMessage());
            }
        });

        removeBtn.setOnAction(e -> {
            Worker selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                payStatus.getStyleClass().setAll("status-error");
                payStatus.setText("Select a worker from the list first.");
                return;
            }
            workerService.removeWorker(user, selected.getId());
            resultsList.getItems().remove(selected);
            payStatus.getStyleClass().setAll("status-success");
            payStatus.setText("Worker removed.");
        });

        Button printWorkers = new Button("Print Worker List");
        printWorkers.getStyleClass().add("secondary-button");
        printWorkers.setOnAction(e -> {
            List<Worker> all = new com.school.sms.dao.WorkerDAO().findAll();
            List<String[]> rows = new ArrayList<>();
            for (Worker w : all) {
                rows.add(new String[]{w.getWorkerId(), w.getFullName(), w.getJobTitle(),
                        String.format("UGX %,.0f", w.getMonthlySalary())});
            }
            printService.printList("Worker List", new String[]{"ID", "Name", "Title", "Salary"}, rows);
        });

        Button exportWorkers = new Button("Export to Excel");
        exportWorkers.getStyleClass().add("accent-button");
        exportWorkers.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("workers.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<Worker> all = new com.school.sms.dao.WorkerDAO().findAll();
            List<String[]> rows = new ArrayList<>();
            for (Worker w : all) {
                rows.add(new String[]{w.getWorkerId(), w.getFullName(), w.getJobTitle(), String.format("%.0f", w.getMonthlySalary())});
            }
            try {
                excelExportService.export("Workers", new String[]{"ID", "Name", "Title", "Salary"}, rows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        payCard.getChildren().addAll(
                payTitle,
                new HBox(8, searchField, searchBtn),
                resultsList,
                forMonthField, payAmountField,
                new HBox(10, payBtn, removeBtn),
                payStatus,
                new HBox(10, printWorkers, exportWorkers)
        );
        root.getChildren().add(payCard);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());
        root.getChildren().add(back);

        MainShell.setCenter(root, "Workers & Payroll");
    }
}
