package com.school.sms.model;

import java.time.LocalDateTime;

public class Payment {
    private int id;
    private int receiptNumber;     // sequential, for auditing (suggestion #2)
    private int studentId;
    private double amount;
    private String term;
    private LocalDateTime paymentDate;
    private int recordedByUserId;  // audit trail (suggestion #5)
    private String notes;

    public Payment() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(int receiptNumber) { this.receiptNumber = receiptNumber; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public int getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(int recordedByUserId) { this.recordedByUserId = recordedByUserId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
