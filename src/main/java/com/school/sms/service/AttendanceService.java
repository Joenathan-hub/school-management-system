package com.school.sms.service;

import com.school.sms.dao.AttendanceDAO;
import com.school.sms.dao.AuditLogDAO;
import com.school.sms.model.AttendanceRecord;

public class AttendanceService {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();

    public void setReturned(int studentId, String term, boolean returned, int recordedByUserId, String studentLabel) {
        AttendanceRecord record = new AttendanceRecord();
        record.setStudentId(studentId);
        record.setTerm(term);
        record.setReturned(returned);
        record.setRecordedByUserId(recordedByUserId);
        attendanceDAO.upsert(record);

        auditLog.log(recordedByUserId, "ATTENDANCE_UPDATE",
                (returned ? "Marked returned: " : "Marked NOT returned: ") + studentLabel + " for " + term);
    }

    public AttendanceRecord getStatus(int studentId, String term) {
        return attendanceDAO.findOne(studentId, term);
    }
}
