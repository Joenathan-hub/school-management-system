package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.UniformDAO;
import com.school.sms.model.UniformItem;

import java.util.List;

public class UniformService {

    /**
     * Default uniform checklist items. Edit this list to match your school's
     * actual uniform requirements — everything else in this module adapts
     * automatically to whatever's listed here.
     */
    public static final String[] DEFAULT_ITEMS = {
            "School Shirt", "Trousers/Skirt", "Sweater", "Tie", "PE Kit", "Shoes", "Socks"
    };

    private final UniformDAO uniformDAO = new UniformDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();

    /** Gets the student's checklist for a term, creating it from the default item list if it doesn't exist yet. */
    public List<UniformItem> getOrCreateChecklist(int studentId, String term) {
        if (!uniformDAO.checklistExists(studentId, term)) {
            for (String item : DEFAULT_ITEMS) {
                uniformDAO.insert(studentId, term, item);
            }
        }
        return uniformDAO.findByStudentAndTerm(studentId, term);
    }

    /** Toggles one item on the checklist and timestamps the change (day/month/year, 24-hour time). */
    public void setItemReceived(int uniformItemId, boolean received, int recordedByUserId, String studentLabel, String itemName) {
        uniformDAO.setReceived(uniformItemId, received, recordedByUserId);
        auditLog.log(recordedByUserId, "UNIFORM_UPDATE",
                (received ? "Marked received: " : "Marked NOT received: ") + itemName + " for " + studentLabel);
    }
}
