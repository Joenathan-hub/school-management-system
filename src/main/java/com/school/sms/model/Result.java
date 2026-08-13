package com.school.sms.model;

public class Result {
    private int id;
    private int studentId;
    private String term;
    private String subject;
    private int marks;
    private String grade;
    private String comment;
    private int enteredByUserId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getEnteredByUserId() { return enteredByUserId; }
    public void setEnteredByUserId(int enteredByUserId) { this.enteredByUserId = enteredByUserId; }
}
