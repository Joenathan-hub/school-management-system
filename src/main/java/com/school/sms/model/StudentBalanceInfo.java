package com.school.sms.model;

/** Pairs a student with their computed balance for a term — a search-result DTO, not stored in the DB. */
public class StudentBalanceInfo {
    private final Student student;
    private final double balance;

    public StudentBalanceInfo(Student student, double balance) {
        this.student = student;
        this.balance = balance;
    }

    public Student getStudent() { return student; }
    public double getBalance() { return balance; }
}