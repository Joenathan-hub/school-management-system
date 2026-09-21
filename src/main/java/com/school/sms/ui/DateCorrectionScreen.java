package com.school.sms.ui;

import com.school.sms.dao.*;
import com.school.sms.model.*;
import com.school.sms.util.DateTimeUtil;
import com.school.sms.util.TransactionDateUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

/**
 * Corrects the transaction date on already-saved money records — for when a
 * payment or purchase is entered days after it actually happened.
 *
 * Only the transaction date changes. The "recorded at" stamp and the audit
 * trail are left intact, and every correction is itself written to the audit
 * log, so the history of what was changed stays visible.
 */
public class DateCorrectionScreen {

    private enum RecordType { PAYMENT, EXPENDITURE, WORKER_PAYMENT, TERM, STUDENT, WORKER }

    public static void show(Stage stage, User user) {
        PaymentDAO paymentDAO = new PaymentDAO();
        ExpenditureDAO expenditureDAO = new ExpenditureDAO();
        WorkerPaymentDAO workerPaymentDAO = new WorkerPaymentDAO();
        StudentDAO studentDAO = new StudentDAO();
        WorkerDAO workerDAO = new WorkerDAO();
        AuditLogDAO auditLog = new AuditLogDAO();

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label title = new Label("Correct a Record's Date");
        title.getStyleClass().add("section-label");

        Label helper = new Label("Use this when a payment or purchase was entered on a different day from when it actually happened. "
                + "Only the transaction date changes — who entered it and when stays on record.");
        helper.setWrapText(true);
        helper.getStyleClass().add("field-label");

        ComboBox<RecordType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RecordType.values());
        typeBox.setValue(RecordType.PAYMENT);
        typeBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(RecordType t) {
                if (t == null) return "";
                return switch (t) {
                    case PAYMENT -> "Student payment";
                    case EXPENDITURE -> "Expenditure / purchase";
                    case WORKER_PAYMENT -> "Worker payment";
                    case TERM -> "Term start";
                    case STUDENT -> "Student reporting";
                    case WORKER -> "Worker start";
                };
            }
            @Override public RecordType fromString(String s) { return null; }
        });

        ListView<Object> recordList = new ListView<>();
        recordList.setPrefHeight(320);
        recordList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                if (item instanceof Payment p) {
                    Student s = studentDAO.findById(p.getStudentId());
                    setText(String.format("Receipt #%d | %s | UGX %,.0f | Paid: %s | Entered: %s",
                            p.getReceiptNumber(),
                            s != null ? s.getFullName() : "Student #" + p.getStudentId(),
                            p.getAmount(),
                            p.getPaymentDate().format(DateTimeUtil.DATE_ONLY),
                            p.getRecordedAt() != null ? p.getRecordedAt().format(DateTimeUtil.DATE_ONLY) : "—"));
                } else if (item instanceof Expenditure x) {
                    setText(String.format("%s [%s] | UGX %,.0f | Purchased: %s | Entered: %s",
                            x.getDescription(), x.getCategory(), x.getAmount(),
                            x.getDate().format(DateTimeUtil.DATE_ONLY),
                            x.getRecordedAt() != null ? x.getRecordedAt().format(DateTimeUtil.DATE_ONLY) : "—"));
                } else if (item instanceof WorkerPayment w) {
                    Worker worker = workerDAO.findById(w.getWorkerId());
                    setText(String.format("%s | UGX %,.0f | For: %s | Paid: %s | Entered: %s",
                            worker != null ? worker.getFullName() : "Worker #" + w.getWorkerId(),
                            w.getAmount(), w.getForMonth(),
                            w.getPaymentDateTime().format(DateTimeUtil.DATE_ONLY),
                            w.getRecordedAt() != null ? w.getRecordedAt().format(DateTimeUtil.DATE_ONLY) : "—"));
                } else if (item instanceof Term t) {
                    setText(String.format("%s [%s] | Started: %s", t.getName(), t.getStatus(),
                            t.getStartDate() != null ? t.getStartDate().format(DateTimeUtil.DATE_ONLY) : "—"));
                } else if (item instanceof Student s) {
                    setText(String.format("%s | %s | Reported: %s", s.getFullName(), s.getStudentId(),
                            s.getAdmissionDate() != null ? s.getAdmissionDate().format(DateTimeUtil.DATE_ONLY) : "—"));
                } else if (item instanceof Worker w) {
                    setText(String.format("%s | %s | Started: %s", w.getFullName(), w.getWorkerId(),
                            w.getDateJoined() != null ? w.getDateJoined().format(DateTimeUtil.DATE_ONLY) : "—"));
                }
                setWrapText(true);
            }
        });

        DatePicker newDateField = new DatePicker();
        newDateField.setPromptText("Correct date");
        Button saveBtn = new Button("Save Corrected Date");
        saveBtn.getStyleClass().add("primary-button");
        Label status = new Label();
        status.setWrapText(true);

        Runnable reload = () -> {
            recordList.getItems().clear();
            switch (typeBox.getValue()) {
                case PAYMENT -> recordList.getItems().addAll(paymentDAO.findAll());
                case EXPENDITURE -> recordList.getItems().addAll(expenditureDAO.findAll());
                case WORKER_PAYMENT -> recordList.getItems().addAll(workerPaymentDAO.findAll());
                case TERM -> recordList.getItems().addAll(new TermDAO().findAll());
                case STUDENT -> recordList.getItems().addAll(studentDAO.findAll());
                case WORKER -> recordList.getItems().addAll(workerDAO.findAll());
            }
            newDateField.setValue(null);
        };

        typeBox.setOnAction(e -> reload.run());
        reload.run();

        recordList.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel instanceof Payment p) newDateField.setValue(p.getPaymentDate().toLocalDate());
            else if (newSel instanceof Expenditure x) newDateField.setValue(x.getDate().toLocalDate());
            else if (newSel instanceof WorkerPayment w) newDateField.setValue(w.getPaymentDateTime().toLocalDate());
            else if (newSel instanceof Term t) newDateField.setValue(t.getStartDate() != null ? t.getStartDate().toLocalDate() : null);
            else if (newSel instanceof Student s) newDateField.setValue(s.getAdmissionDate());
            else if (newSel instanceof Worker w) newDateField.setValue(w.getDateJoined());
        });

        saveBtn.setOnAction(e -> {
            Object selected = recordList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Select a record first.");
                return;
            }
            if (newDateField.getValue() == null) {
                status.getStyleClass().setAll("status-error");
                status.setText("Pick the correct date.");
                return;
            }
            if ((selected instanceof Term || selected instanceof Student || selected instanceof Worker)
                    && user.getRole() != Role.ADMINISTRATOR) {
                status.getStyleClass().setAll("status-error");
                status.setText("Only the administrator can correct term, student, or worker start dates.");
                return;
            }

            try {
                if (selected instanceof Payment p) {
                    LocalDateTime corrected = TransactionDateUtil.replaceDateKeepTime(p.getPaymentDate(), newDateField.getValue());
                    paymentDAO.updateTransactionDate(p.getId(), corrected);
                    auditLog.log(user.getId(), "CORRECT_PAYMENT_DATE",
                            String.format("Receipt #%d date changed from %s to %s", p.getReceiptNumber(),
                                    p.getPaymentDate().format(DateTimeUtil.DATE_ONLY),
                                    corrected.format(DateTimeUtil.DATE_ONLY)));
                } else if (selected instanceof Expenditure x) {
                    LocalDateTime corrected = TransactionDateUtil.replaceDateKeepTime(x.getDate(), newDateField.getValue());
                    expenditureDAO.updateTransactionDate(x.getId(), corrected);
                    auditLog.log(user.getId(), "CORRECT_EXPENDITURE_DATE",
                            String.format("Expenditure '%s' date changed from %s to %s", x.getDescription(),
                                    x.getDate().format(DateTimeUtil.DATE_ONLY),
                                    corrected.format(DateTimeUtil.DATE_ONLY)));
                } else if (selected instanceof WorkerPayment w) {
                    LocalDateTime corrected = TransactionDateUtil.replaceDateKeepTime(w.getPaymentDateTime(), newDateField.getValue());
                    workerPaymentDAO.updateTransactionDate(w.getId(), corrected);
                    auditLog.log(user.getId(), "CORRECT_WORKER_PAYMENT_DATE",
                            String.format("Worker payment #%d date changed from %s to %s", w.getId(),
                                    w.getPaymentDateTime().format(DateTimeUtil.DATE_ONLY),
                                    corrected.format(DateTimeUtil.DATE_ONLY)));
                } else if (selected instanceof Term t) {
                    LocalDateTime corrected = TransactionDateUtil.replaceDateKeepTime(t.getStartDate(), newDateField.getValue());
                    new TermDAO().updateStartDate(t.getId(), corrected);
                    auditLog.log(user.getId(), "CORRECT_TERM_START_DATE",
                            String.format("Term %s start changed to %s", t.getName(), corrected.format(DateTimeUtil.DATE_ONLY)));
                } else if (selected instanceof Student s) {
                    studentDAO.updateAdmissionDate(s.getId(), newDateField.getValue());
                    auditLog.log(user.getId(), "CORRECT_STUDENT_REPORTING_DATE",
                            String.format("Student %s reporting date changed to %s", s.getFullName(), newDateField.getValue().format(DateTimeUtil.DATE_ONLY)));
                } else if (selected instanceof Worker w) {
                    workerDAO.updateDateJoined(w.getId(), newDateField.getValue());
                    auditLog.log(user.getId(), "CORRECT_WORKER_START_DATE",
                            String.format("Worker %s start date changed to %s", w.getFullName(), newDateField.getValue().format(DateTimeUtil.DATE_ONLY)));
                }

                status.getStyleClass().setAll("status-success");
                status.setText("Date corrected.");
                reload.run();
            } catch (Exception ex) {
                status.getStyleClass().setAll("status-error");
                status.setText("Could not save: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(title, helper, typeBox, recordList,
                new HBox(10, newDateField, saveBtn), status);

        Button back = new Button("Back to Dashboard");
        back.getStyleClass().add("secondary-button");
        back.setOnAction(e -> MainShell.showHome());

        root.getChildren().addAll(card, back);
        MainShell.setCenter(root, "Correct Record Dates");
    }
}
