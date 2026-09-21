package com.school.sms.model;

import java.time.LocalDateTime;

public class Expenditure {
    private int id;
    private String description;    // what was purchased / who was paid
    private double amount;
    private String category;       // e.g. "Supplies", "Salaries", "Maintenance"
    private LocalDateTime date;
    private int recordedByUserId;  // audit trail
    private java.time.LocalDateTime recordedAt;

    public Expenditure() {}

    public java.time.LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(java.time.LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public int getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(int recordedByUserId) { this.recordedByUserId = recordedByUserId; }
}
