package com.school.sms.model;

import java.time.LocalDate;

public class Student {
    private int id;
    private String studentId;
    private String fullName;
    private int age;
    private String sex;
    private String studentClass;
    private String boardingStatus; // "Boarding" or "Day" — determines which requirements checklist applies
    private LocalDate admissionDate;
    private int fatherId;
    private int motherId;
    private boolean active;

    public Student() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }

    public String getBoardingStatus() { return boardingStatus; }
    public void setBoardingStatus(String boardingStatus) { this.boardingStatus = boardingStatus; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public int getFatherId() { return fatherId; }
    public void setFatherId(int fatherId) { this.fatherId = fatherId; }

    public int getMotherId() { return motherId; }
    public void setMotherId(int motherId) { this.motherId = motherId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}