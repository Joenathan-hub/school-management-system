package com.school.sms.model;

public class AttendanceRecord {
    private int id;
    private int studentId;
    private String term;
    private boolean returned;
    private int recordedByUserId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public int getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(int recordedByUserId) { this.recordedByUserId = recordedByUserId; }
}
