package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.RequirementDAO;
import com.school.sms.dao.RequirementTemplateDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.model.Requirement;
import com.school.sms.model.RequirementTemplate;
import com.school.sms.model.Student;

import java.util.List;
import java.util.stream.Collectors;

public class RequirementService {

    /** Used only if the admin hasn't configured any templates yet for a category — keeps the app usable out of the box. */
    private static final String[] FALLBACK_ITEMS = {
            "Mattress", "Beddings", "Textbooks", "Exercise Books", "Toiletries", "Personal Effects"
    };

    private final RequirementDAO requirementDAO = new RequirementDAO();
    private final RequirementTemplateDAO templateDAO = new RequirementTemplateDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();

    /** Picks Boarding or Day's item list automatically based on the student's boarding status. */
    public List<Requirement> getOrCreateChecklist(int studentId, String term) {
        if (!requirementDAO.checklistExists(studentId, term)) {
            Student student = studentDAO.findById(studentId);
            String category = (student != null && student.getBoardingStatus() != null && !student.getBoardingStatus().isBlank())
                    ? student.getBoardingStatus() : "Day";

            List<String> items = templateDAO.findByCategory(category).stream()
                    .map(RequirementTemplate::getItemName)
                    .collect(Collectors.toList());

            if (items.isEmpty()) {
                for (String item : FALLBACK_ITEMS) {
                    requirementDAO.insert(studentId, term, item);
                }
            } else {
                for (String item : items) {
                    requirementDAO.insert(studentId, term, item);
                }
            }
        }
        return requirementDAO.findByStudentAndTerm(studentId, term);
    }

    public void setItemBrought(int requirementId, boolean brought, int recordedByUserId, String studentLabel, String itemName) {
        requirementDAO.setBrought(requirementId, brought);
        auditLog.log(recordedByUserId, "REQUIREMENT_UPDATE",
                (brought ? "Marked brought: " : "Marked NOT brought: ") + itemName + " for " + studentLabel);
    }
}