package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.FeeStructureDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.dao.TermDAO;
import com.school.sms.model.Student;
import com.school.sms.model.Term;
import com.school.sms.model.User;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * Handles starting and closing terms. Starting a new term automatically:
 *   - closes whichever term is currently active (if any)
 *   - carries each active student's outstanding fee balance forward
 *   - re-registers each student's requirements checklist for the new term
 */
public class TermService {

    private final TermDAO termDAO = new TermDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final FeeStructureDAO feeDAO = new FeeStructureDAO();
    private final RequirementService requirementService = new RequirementService();
    private final AuditLogDAO auditLog = new AuditLogDAO();
    private final StudentService studentService = new StudentService();

    public Term getActiveTerm() {
        return termDAO.findActive();
    }

    /** Convenience for pre-filling term fields elsewhere. Returns "" if no term has been started yet. */
    public String getActiveTermName() {
        Term active = termDAO.findActive();
        return active != null ? active.getName() : "";
    }

    public List<Term> getAllTerms() {
        return termDAO.findAll();
    }

    public static class RolloverSummary {
        public String previousTermName;
        public String newTermName;
        public int studentsProcessed;
        public double totalBalanceCarriedForward;
    }

    public RolloverSummary startNewTerm(String newTermName, User admin) {
        return startNewTerm(newTermName, admin, LocalDate.now());
    }

    public RolloverSummary startNewTerm(String newTermName, User admin, LocalDate startDate) {
        Term currentActive = termDAO.findActive();
        String previousTermName = currentActive != null ? currentActive.getName() : null;

        if (currentActive != null) {
            termDAO.close(currentActive.getId());
        }

        Term newTerm = new Term();
        newTerm.setName(newTermName);
        newTerm.setStatus("ACTIVE");
        newTerm.setStartDate(com.school.sms.util.TransactionDateUtil.toTransactionDateTime(startDate));
        termDAO.insert(newTerm);

        RolloverSummary summary = new RolloverSummary();
        summary.previousTermName = previousTermName;
        summary.newTermName = newTermName;

        List<Student> students = studentDAO.findAll();
        for (Student s : students) {
            double carriedBalance = 0;
            if (previousTermName != null) {
                carriedBalance = studentService.getBalance(s.getId(), previousTermName);
            }

            feeDAO.upsertBroughtForward(s.getId(), newTermName, carriedBalance);
            requirementService.getOrCreateChecklist(s.getId(), newTermName);

            summary.studentsProcessed++;
            summary.totalBalanceCarriedForward += carriedBalance;
        }

        auditLog.log(admin.getId(), "START_TERM",
                String.format("Started %s (closed %s). %d students processed, UGX %,.0f carried forward.",
                        newTermName, previousTermName != null ? previousTermName : "none",
                        summary.studentsProcessed, summary.totalBalanceCarriedForward));

        return summary;
    }
}
