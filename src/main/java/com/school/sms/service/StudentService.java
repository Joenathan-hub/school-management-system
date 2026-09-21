package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.FeeStructureDAO;
import com.school.sms.dao.GuardianDAO;
import com.school.sms.dao.SettingsDAO;
import com.school.sms.dao.StudentDAO;
import com.school.sms.model.FeeStructure;
import com.school.sms.model.Guardian;
import com.school.sms.model.Student;
import com.school.sms.util.IdGenerator;

import java.time.LocalDate;

public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();
    private final GuardianDAO guardianDAO = new GuardianDAO();
    private final FeeStructureDAO feeDAO = new FeeStructureDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();

    public Student admitStudent(String fullName, int age, String sex, String studentClass, String boardingStatus,
                                 Guardian father, Guardian mother,
                                 String term, double baseFee, double discount, String discountReason,
                                 int recordedByUserId) {
        return admitStudent(fullName, age, sex, studentClass, boardingStatus, father, mother,
                term, baseFee, discount, discountReason, recordedByUserId, LocalDate.now());
    }

    public Student admitStudent(String fullName, int age, String sex, String studentClass, String boardingStatus,
                                Guardian father, Guardian mother,
                                String term, double baseFee, double discount, String discountReason,
                                int recordedByUserId, LocalDate admissionDate) {

        int fatherId = father != null ? guardianDAO.insert(father) : 0;
        int motherId = mother != null ? guardianDAO.insert(mother) : 0;

        String schoolInitials = settingsDAO.get("school.initials", "SCH");
        String studentId = IdGenerator.generate(
                IdGenerator.Type.STUDENT,
                schoolInitials,
                LocalDate.now(),
                studentDAO::studentIdExists
        );

        Student student = new Student();
        student.setStudentId(studentId);
        student.setFullName(fullName);
        student.setAge(age);
        student.setSex(sex);
        student.setStudentClass(studentClass);
        student.setBoardingStatus(boardingStatus);
        student.setAdmissionDate(admissionDate != null ? admissionDate : LocalDate.now());
        student.setFatherId(fatherId);
        student.setMotherId(motherId);
        student.setActive(true);

        int dbId = studentDAO.insert(student);
        student.setId(dbId);

        FeeStructure fee = new FeeStructure();
        fee.setStudentId(dbId);
        fee.setTerm(term);
        fee.setBaseFee(baseFee);
        fee.setDiscount(discount);
        fee.setDiscountReason(discountReason);
        feeDAO.insert(fee);

        auditLog.log(recordedByUserId, "ADMIT_STUDENT",
                "Admitted " + fullName + " (" + studentId + ") to " + studentClass + " [" + boardingStatus + "]");

        return student;
    }

    public double getBalance(int studentId, String term) {
        FeeStructure fee = feeDAO.findByStudentAndTerm(studentId, term);
        if (fee == null) return 0.0;
        double paid = feeDAO.totalPaid(studentId, term);
        return fee.getAmountOwed() - paid;
    }

    public Student findByStudentId(String studentId) {
        return studentDAO.findByStudentId(studentId);
    }
}
