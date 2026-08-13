package com.school.sms.model;

/**
 * A student's fee obligation for a given term.
 * baseFee comes from the class-level fee structure; discount is applied
 * per-student by the administrator. amountOwed = baseFee - discount.
 */
public class FeeStructure {
    private int id;
    private int studentId;         // FK -> Student
    private String term;           // e.g. "Term 1 2026"
    private double baseFee;        // standard fee for the student's class
    private double discount;       // admin-applied discount, 0 if none
   private String discountReason;
    private double broughtForward; // balance carried over from the previous term, added automatically at term start

    public FeeStructure() {}

    public double getAmountOwed() {
        return baseFee - discount + broughtForward;
    }

    public double getBroughtForward() { return broughtForward; }
    public void setBroughtForward(double broughtForward) { this.broughtForward = broughtForward; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public double getBaseFee() { return baseFee; }
    public void setBaseFee(double baseFee) { this.baseFee = baseFee; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getDiscountReason() { return discountReason; }
    public void setDiscountReason(String discountReason) { this.discountReason = discountReason; }
}
