package com.school.sms.model;

public class Requirement {
    private int id;
    private int studentId;
    private String term;
    private String itemName;
    private boolean brought;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public boolean isBrought() { return brought; }
    public void setBrought(boolean brought) { this.brought = brought; }
}
