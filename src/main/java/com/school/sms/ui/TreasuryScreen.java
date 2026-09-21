package com.school.sms.ui;

import com.school.sms.dao.ExpenditureDAO;
import com.school.sms.dao.PaymentDAO;
import com.school.sms.model.Expenditure;
import com.school.sms.model.Payment;
import com.school.sms.model.User;
import com.school.sms.service.ExcelExportService;
import com.school.sms.service.PrintService;
import com.school.sms.service.TreasuryService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TreasuryScreen {

    private static final java.time.format.DateTimeFormatter DATE_FMT = com.school.sms.util.DateTimeUtil.DATE_TIME;

    public static void show(Stage stage, User user) {
        TreasuryService treasuryService = new TreasuryService();
        PaymentDAO paymentDAO = new PaymentDAO();
        ExpenditureDAO expenditureDAO = new ExpenditureDAO();
        PrintService printService = new PrintService();
        ExcelExportService excelExportService = new ExcelExportService();

        TreasuryService.TreasurySummary summary = treasuryService.getSummary();

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        // --- Summary cards ---
        VBox collectedCard = summaryCard("Total Collected", summary.totalCollected);
        VBox spentCard = summaryCard("Total Expenditure", summary.totalExpenditure);
        VBox remainingCard = summaryCard("Remaining Balance", summary.remainingBalance);
        HBox summaryRow = new HBox(14, collectedCard, spentCard, remainingCard);

        Label collectedFigure = (Label) collectedCard.getChildren().get(1);
        Label spentFigure = (Label) spentCard.getChildren().get(1);
        Label remainingFigure = (Label) remainingCard.getChildren().get(1);

        // --- Record new expenditure ---
        VBox expCard = new VBox(8);
        expCard.getStyleClass().add("card");
        Label expTitle = new Label("Record Expenditure");
        expTitle.getStyleClass().add("section-label");
        TextField descField = new TextField();
        descField.setPromptText("What was purchased / who was paid");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount (UGX)");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Supplies", "Salaries", "Maintenance", "Utilities", "Other");
        categoryBox.setValue("Other");
        DatePicker expDateField = new DatePicker(java.time.LocalDate.now());
        Label expDateLabel = new Label("Date purchase/payment was made:");
        expDateLabel.getStyleClass().add("field-label");

        Button recordExpBtn = new Button("Record Expenditure");
        recordExpBtn.getStyleClass().add("primary-button");
        Label expStatus = new Label();

        recordExpBtn.setOnAction(e -> {
            try {
                Expenditure exp = new Expenditure();
                exp.setDescription(descField.getText());
                exp.setAmount(Double.parseDouble(amountField.getText().trim()));
                exp.setCategory(categoryBox.getValue());
                exp.setDate(com.school.sms.util.TransactionDateUtil.toTransactionDateTime(expDateField.getValue()));
                exp.setRecordedByUserId(user.getId());
                expenditureDAO.insert(exp);
                expStatus.getStyleClass().setAll("status-success");
                expStatus.setText("Expenditure recorded at " + java.time.LocalDateTime.now().format(DATE_FMT));

                TreasuryService.TreasurySummary refreshed = treasuryService.getSummary();
                collectedFigure.setText(String.format("UGX %,.0f", refreshed.totalCollected));
                spentFigure.setText(String.format("UGX %,.0f", refreshed.totalExpenditure));
                remainingFigure.setText(String.format("UGX %,.0f", refreshed.remainingBalance));
            } catch (NumberFormatException ex) {
                expStatus.getStyleClass().setAll("status-error");
                expStatus.setText("Amount must be a number.");
            }
        });

        expCard.getChildren().addAll(expTitle, descField, amountField, categoryBox, expDateLabel, expDateField, recordExpBtn, expStatus);

        // --- Print buttons ---
        Button printPayments = new Button("Print Payments List");
        printPayments.getStyleClass().add("secondary-button");
        printPayments.setOnAction(e -> {
            List<Payment> payments = paymentDAO.findAll();
            List<String[]> rows = new ArrayList<>();
            for (Payment p : payments) {
                rows.add(new String[]{
                        "#" + p.getReceiptNumber(),
                        p.getPaymentDate().format(DATE_FMT),
                        "Student #" + p.getStudentId(),
                        String.format("UGX %,.0f", p.getAmount()),
                        p.getTerm()
                });
            }
            printService.printList("Payments List", new String[]{"Receipt", "Date", "Student", "Amount", "Term"}, rows);
        });

        Button printExpenditures = new Button("Print Expenditures List");
        printExpenditures.getStyleClass().add("secondary-button");
        printExpenditures.setOnAction(e -> {
            List<Expenditure> expenditures = expenditureDAO.findAll();
            List<String[]> rows = new ArrayList<>();
            for (Expenditure exp : expenditures) {
                rows.add(new String[]{
                        exp.getDate().format(DATE_FMT),
                        exp.getCategory(),
                        exp.getDescription(),
                        String.format("UGX %,.0f", exp.getAmount())
                });
            }
            printService.printList("Expenditures List", new String[]{"Date", "Category", "Description", "Amount"}, rows);
        });

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        Button exportPayments = new Button("Export Payments (Excel)");
        exportPayments.getStyleClass().add("accent-button");
        exportPayments.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("payments.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> rows = new ArrayList<>();
            for (Payment p : paymentDAO.findAll()) {
                rows.add(new String[]{"#" + p.getReceiptNumber(), p.getPaymentDate().format(DATE_FMT),
                        "Student #" + p.getStudentId(), String.format("%.0f", p.getAmount()), p.getTerm()});
            }
            try {
                excelExportService.export("Payments", new String[]{"Receipt", "Date", "Student", "Amount", "Term"}, rows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        Button exportExpenditures = new Button("Export Expenditures (Excel)");
        exportExpenditures.getStyleClass().add("accent-button");
        exportExpenditures.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("expenditures.xlsx");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            List<String[]> rows = new ArrayList<>();
            for (Expenditure exp : expenditureDAO.findAll()) {
                rows.add(new String[]{exp.getDate().format(DATE_FMT), exp.getCategory(), exp.getDescription(),
                        String.format("%.0f", exp.getAmount())});
            }
            try {
                excelExportService.export("Expenditures", new String[]{"Date", "Category", "Description", "Amount"}, rows, file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("Excel export failed: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                summaryRow,
                expCard,
                new Separator(),
                new HBox(10, printPayments, printExpenditures),
                new HBox(10, exportPayments, exportExpenditures),
                back
        );

        MainShell.setCenter(root, "Treasury Dashboard");
    }

    private static VBox summaryCard(String caption, double figure) {
        VBox card = new VBox(4);
        card.getStyleClass().add("summary-card");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("summary-caption");
        Label figureLabel = new Label(String.format("UGX %,.0f", figure));
        figureLabel.getStyleClass().add("summary-figure");
        card.getChildren().addAll(captionLabel, figureLabel);
        return card;
    }
}
