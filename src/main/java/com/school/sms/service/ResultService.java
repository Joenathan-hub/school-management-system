package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.ResultDAO;
import com.school.sms.model.Result;

import java.util.List;

public class ResultService {

    private final ResultDAO resultDAO = new ResultDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();

    /** Standard percentage-based grading scale. Adjust the cutoffs here if your school uses a different scale. */
    public static String computeGrade(int marks) {
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 50) return "D";
        if (marks >= 40) return "E";
        return "F";
    }

    /** Teacher enters/updates one subject's marks for a student; grade is computed automatically, comment is free text. */
    public Result recordResult(int studentId, String term, String subject, int marks, String comment, int enteredByUserId) {
        Result r = new Result();
        r.setStudentId(studentId);
        r.setTerm(term);
        r.setSubject(subject);
        r.setMarks(marks);
        r.setGrade(computeGrade(marks));
        r.setComment(comment);
        r.setEnteredByUserId(enteredByUserId);
        resultDAO.upsert(r);

        auditLog.log(enteredByUserId, "ENTER_RESULT",
                String.format("%s: %d marks (%s) for student #%d, %s", subject, marks, r.getGrade(), studentId, term));
        return r;
    }

    public List<Result> getReportCard(int studentId, String term) {
        return resultDAO.findByStudentAndTerm(studentId, term);
    }

    public static double average(List<Result> results) {
        if (results.isEmpty()) return 0.0;
        return results.stream().mapToInt(Result::getMarks).average().orElse(0.0);
    }

    public static int total(List<Result> results) {
        return results.stream().mapToInt(Result::getMarks).sum();
    }
}
