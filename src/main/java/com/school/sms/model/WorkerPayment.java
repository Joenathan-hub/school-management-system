package com.school.sms.model;

import java.time.LocalDateTime;

public class WorkerPayment {
    private int id;
    private int workerId;
    private double amount;
    private String forMonth;
    private LocalDateTime paymentDateTime; // day/month/year + 24-hour time, per school policy
    private int recordedByUserId;
    private String notes;
    private java.time.LocalDateTime recordedAt;

    public java.time.LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(java.time.LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWorkerId() { return workerId; }
    public void setWorkerId(int workerId) { this.workerId = workerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getForMonth() { return forMonth; }
    public void setForMonth(String forMonth) { this.forMonth = forMonth; }

    public LocalDateTime getPaymentDateTime() { return paymentDateTime; }
    public void setPaymentDateTime(LocalDateTime paymentDateTime) { this.paymentDateTime = paymentDateTime; }

    public int getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(int recordedByUserId) { this.recordedByUserId = recordedByUserId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
