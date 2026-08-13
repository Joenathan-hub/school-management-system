package com.school.sms.service;

import com.school.sms.dao.*;
import com.school.sms.model.*;
import com.school.sms.util.DateTimeUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardKpiService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ExpenditureDAO expenditureDAO = new ExpenditureDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final StudentService studentService = new StudentService();
    private final UserDAO userDAO = new UserDAO();
    private final RequirementDAO requirementDAO = new RequirementDAO();
    private final TermService termService = new TermService();
    private final SchoolEventDAO eventDAO = new SchoolEventDAO();

    public static class PaymentKpiRow {
        public String studentName;
        public double amount;
        public String recordedBy;
        public String time;
    }

    public static class ExpenditureKpiRow {
        public String description;
        public String category;
        public double amount;
        public String recordedBy;
        public String time;
    }

    private List<Payment> todaysPayments() {
        LocalDate today = LocalDate.now();
        return paymentDAO.findAll().stream()
                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().toLocalDate().equals(today))
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .collect(Collectors.toList());
    }

    public double getCollectedTodayTotal() {
        return todaysPayments().stream().mapToDouble(Payment::getAmount).sum();
    }

    public List<PaymentKpiRow> getCollectedTodayDetails() {
        List<PaymentKpiRow> rows = new ArrayList<>();
        for (Payment p : todaysPayments()) {
            PaymentKpiRow row = new PaymentKpiRow();
            Student s = studentDAO.findById(p.getStudentId());
            row.studentName = s != null ? s.getFullName() : ("Student #" + p.getStudentId());
            row.amount = p.getAmount();
            User u = userDAO.findById(p.getRecordedByUserId());
            row.recordedBy = u != null ? u.getFullName() : "Unknown";
            row.time = p.getPaymentDate().format(DateTimeUtil.TIME_ONLY);
            rows.add(row);
        }
        return rows;
    }

    private List<Expenditure> todaysExpenditure() {
        LocalDate today = LocalDate.now();
        return expenditureDAO.findAll().stream()
                .filter(x -> x.getDate() != null && x.getDate().toLocalDate().equals(today))
                .sorted(Comparator.comparing(Expenditure::getDate).reversed())
                .collect(Collectors.toList());
    }

    public double getExpenditureTodayTotal() {
        return todaysExpenditure().stream().mapToDouble(Expenditure::getAmount).sum();
    }

    public List<ExpenditureKpiRow> getExpenditureTodayDetails() {
        List<ExpenditureKpiRow> rows = new ArrayList<>();
        for (Expenditure x : todaysExpenditure()) {
            ExpenditureKpiRow row = new ExpenditureKpiRow();
            row.description = x.getDescription();
            row.category = x.getCategory();
            row.amount = x.getAmount();
            User u = userDAO.findById(x.getRecordedByUserId());
            row.recordedBy = u != null ? u.getFullName() : "Unknown";
            row.time = x.getDate().format(DateTimeUtil.TIME_ONLY);
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Double> getOutstandingByClass() {
        String term = termService.getActiveTermName();
        Map<String, Double> result = new LinkedHashMap<>();
        if (term == null || term.isBlank()) return result;
        for (Student s : studentDAO.findAll()) {
            double balance = Math.max(0, studentService.getBalance(s.getId(), term));
            result.merge(s.getStudentClass(), balance, Double::sum);
        }
        return result;
    }

    public double getOutstandingTotal() {
        return getOutstandingByClass().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public int getStudentsNotClearedCount() {
        String term = termService.getActiveTermName();
        if (term == null || term.isBlank()) return 0;
        Set<Integer> ids = new HashSet<>();
        for (Student s : studentDAO.findAll()) {
            double balance = studentService.getBalance(s.getId(), term);
            if (balance > 0) {
                ids.add(s.getId());
                continue;
            }
            List<Requirement> checklist = requirementDAO.findByStudentAndTerm(s.getId(), term);
            if (!checklist.isEmpty() && checklist.stream().anyMatch(r -> !r.isBrought())) {
                ids.add(s.getId());
            }
        }
        return ids.size();
    }

    public List<SchoolEvent> getUpcomingEvents(int limit) {
        return eventDAO.findUpcoming(limit);
    }

    public List<SchoolEvent> getAllUpcomingEvents() {
        return eventDAO.findUpcoming(Integer.MAX_VALUE);
    }

    public static class OutstandingRow {
        public Student student;
        public double balance;
    }

    public List<OutstandingRow> getOutstandingByStudentDetails() {
        String term = termService.getActiveTermName();
        List<OutstandingRow> rows = new ArrayList<>();
        if (term == null || term.isBlank()) return rows;
        for (Student s : studentDAO.findAll()) {
            double balance = Math.max(0, studentService.getBalance(s.getId(), term));
            if (balance > 0) {
                OutstandingRow row = new OutstandingRow();
                row.student = s;
                row.balance = balance;
                rows.add(row);
            }
        }
        rows.sort((a, b) -> Double.compare(b.balance, a.balance));
        return rows;
    }

    public static class NotClearedRow {
        public Student student;
        public double balance;
        public boolean requirementsIncomplete;
    }

    public List<NotClearedRow> getStudentsNotClearedDetails() {
        String term = termService.getActiveTermName();
        List<NotClearedRow> rows = new ArrayList<>();
        if (term == null || term.isBlank()) return rows;
        for (Student s : studentDAO.findAll()) {
            double balance = studentService.getBalance(s.getId(), term);
            List<Requirement> checklist = requirementDAO.findByStudentAndTerm(s.getId(), term);
            boolean reqIncomplete = !checklist.isEmpty() && checklist.stream().anyMatch(r -> !r.isBrought());
            if (balance > 0 || reqIncomplete) {
                NotClearedRow row = new NotClearedRow();
                row.student = s;
                row.balance = balance;
                row.requirementsIncomplete = reqIncomplete;
                rows.add(row);
            }
        }
        return rows;
    }
}