package com.school.sms.service;

import com.school.sms.model.Payment;
import com.school.sms.model.Student;
import com.school.sms.util.AppConfig;
import javafx.geometry.Insets;
import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Handles printing for receipts and record lists (payments, expenditures,
 * workers, students). Uses JavaFX's built-in javafx.print API — no extra
 * dependency, and it opens the OS print dialog so the user can pick a
 * physical printer or "print to PDF" if their OS supports it.
 *
 * Call any print*() method from a JavaFX UI thread (button click handler).
 */
public class PrintService {

    private static final java.time.format.DateTimeFormatter DATE_FMT = com.school.sms.util.DateTimeUtil.DATE_TIME;

    /** Prints a single payment receipt. */
    public void printReceipt(Payment payment, Student student, double newBalance) {
        String schoolName = new com.school.sms.dao.SettingsDAO().get("school.name", "School");

        VBox content = new VBox(6);
        content.setPadding(new Insets(20));

        Label header = new Label(schoolName);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label subHeader = new Label("Official Fees Receipt");
        subHeader.setFont(Font.font("Arial", 14));

        content.getChildren().addAll(
                header, subHeader, new Separator(),
                row("Receipt No.", "#" + payment.getReceiptNumber()),
                row("Date", payment.getPaymentDate().format(DATE_FMT)),
                row("Student", student.getFullName()),
                row("Student ID", student.getStudentId()),
                row("Class", student.getStudentClass()),
                row("Term", payment.getTerm()),
                new Separator(),
                row("Amount Paid", String.format("UGX %,.0f", payment.getAmount())),
                row("Balance Remaining", String.format("UGX %,.0f", newBalance)),
                new Separator(),
                new Label(payment.getNotes() != null ? payment.getNotes() : "")
        );

        print(content);
    }

    /** Generic printer for lists: payments, expenditures, workers, students. */
    public void printList(String title, String[] headers, List<String[]> rows) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(20));

        Label header = new Label(title);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        content.getChildren().add(header);
        content.getChildren().add(new Separator());

        Label headerRow = new Label(String.join("   |   ", headers));
        headerRow.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        content.getChildren().add(headerRow);

        for (String[] row : rows) {
            Label rowLabel = new Label(String.join("   |   ", row));
            rowLabel.setFont(Font.font("Consolas", 11));
            content.getChildren().add(rowLabel);
        }

        print(content);
    }

    /** Prints an individual student report card: subjects, marks, grade, comments, and the computed total/average. */
    public void printReportCard(com.school.sms.model.Student student, String term,
                                 java.util.List<com.school.sms.model.Result> results,
                                 double average, int total) {
        String schoolName = new com.school.sms.dao.SettingsDAO().get("school.name", "School");

        VBox content = new VBox(6);
        content.setPadding(new Insets(20));

        Label header = new Label(schoolName);
        header.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        Label subHeader = new Label("Student Report Card — " + term);
        subHeader.setFont(Font.font("Arial", 14));

        content.getChildren().addAll(
                header, subHeader, new Separator(),
                row("Student", student.getFullName()),
                row("Student ID", student.getStudentId()),
                row("Class", student.getStudentClass()),
                new Separator()
        );

        Label colHeader = new Label(String.format("%-20s %-10s %-8s %s", "Subject", "Marks", "Grade", "Comment"));
        colHeader.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        content.getChildren().add(colHeader);

        for (com.school.sms.model.Result r : results) {
            Label rowLabel = new Label(String.format("%-20s %-10d %-8s %s",
                    r.getSubject(), r.getMarks(), r.getGrade(), r.getComment() != null ? r.getComment() : ""));
            rowLabel.setFont(Font.font("Consolas", 11));
            content.getChildren().add(rowLabel);
        }

        content.getChildren().addAll(
                new Separator(),
                row("Total Marks", String.valueOf(total)),
                row("Average", String.format("%.1f", average))
        );

        print(content);
    }

    private Label row(String label, String value) {
        Label l = new Label(label + ":  " + value);
        l.setFont(Font.font("Arial", 12));
        return l;
    }

    private void print(VBox content) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            System.err.println("No printer available on this system.");
            return;
        }
        boolean proceed = job.showPrintDialog(content.getScene() != null ? content.getScene().getWindow() : null);
        if (proceed) {
            Printer printer = job.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4,
                    javafx.print.PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            content.setPrefWidth(pageLayout.getPrintableWidth());
            boolean success = job.printPage(pageLayout, content);
            if (success) {
                job.endJob();
            } else {
                System.err.println("Printing failed.");
            }
        }
    }
}
