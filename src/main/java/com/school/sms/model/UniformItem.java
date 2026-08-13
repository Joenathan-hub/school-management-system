package com.school.sms.model;

import java.time.LocalDateTime;

public class UniformItem {
    private int id;
    private int studentId;
    private String term;
    private String itemName;
    private boolean received;
    private LocalDateTime dateReceived; // set the moment it's checked off
    private int recordedByUserId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public boolean isReceived() { return received; }
    public void setReceived(boolean received) { this.received = received; }

    public LocalDateTime getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDateTime dateReceived) { this.dateReceived = dateReceived; }

    public int getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(int recordedByUserId) { this.recordedByUserId = recordedByUserId; }
}
