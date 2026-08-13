package com.school.sms.service;

import com.school.sms.dao.RequirementDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Requirement;
import com.school.sms.model.Student;
import com.school.sms.model.StudentBalanceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Powers the "Search Students" screen: filter by fee balance and by
 * requirements status. Filtering happens in Java rather than SQL since
 * Access/UCanAccess handles complex joins poorly and school roll sizes
 * are small enough that this stays fast.
 */
public class AdvancedSearchService {

    private final StudentDAO studentDAO = new StudentDAO();
    private final StudentService studentService = new StudentService();
    private final RequirementDAO requirementDAO = new RequirementDAO();

    public enum BalanceMode {
        NOT_CLEARED,
        FULLY_CLEARED,
        AT_OR_BELOW,
        AT_OR_ABOVE
    }

    public List<StudentBalanceInfo> searchByFeeBalance(String term, BalanceMode mode, double amount) {
        List<StudentBalanceInfo> results = new ArrayList<>();
        for (Student s : studentDAO.findAll()) {
            double balance = studentService.getBalance(s.getId(), term);
            boolean match = switch (mode) {
                case NOT_CLEARED -> balance > 0;
                case FULLY_CLEARED -> balance <= 0;
                case AT_OR_BELOW -> balance <= amount;
                case AT_OR_ABOVE -> balance >= amount;
            };
            if (match) {
                results.add(new StudentBalanceInfo(s, balance));
            }
        }
        return results;
    }

    public enum RequirementMode {
        MISSING_ITEM,
        NOT_FULLY_CLEARED
    }

    public List<Student> searchByRequirements(String term, RequirementMode mode, String itemName) {
        List<Student> results = new ArrayList<>();
        for (Student s : studentDAO.findAll()) {
            List<Requirement> checklist = requirementDAO.findByStudentAndTerm(s.getId(), term);
            if (checklist.isEmpty()) continue;

            boolean match;
            if (mode == RequirementMode.MISSING_ITEM) {
                match = checklist.stream()
                        .anyMatch(r -> r.getItemName().equalsIgnoreCase(itemName) && !r.isBrought());
            } else {
                match = checklist.stream().anyMatch(r -> !r.isBrought());
            }
            if (match) results.add(s);
        }
        return results;
    }
}