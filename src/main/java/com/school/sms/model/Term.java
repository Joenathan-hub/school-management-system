package com.school.sms.model;

import java.time.LocalDateTime;

public class Term {
    private int id;
    private String name;
    private String status; // "ACTIVE" or "CLOSED"
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}