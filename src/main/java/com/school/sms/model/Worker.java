package com.school.sms.model;

import java.time.LocalDate;

public class Worker {
    private int id;
    private String workerId;       // e.g. W-SHS2026-07-1193
    private String fullName;
    private String jobTitle;       // e.g. "Teacher", "Cleaner", "Cook"
    private String contact;
    private LocalDate dateJoined;
    private double monthlySalary;
    private boolean active;

    public Worker() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public LocalDate getDateJoined() { return dateJoined; }
    public void setDateJoined(LocalDate dateJoined) { this.dateJoined = dateJoined; }

    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
