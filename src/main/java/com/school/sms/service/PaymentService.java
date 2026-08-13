package com.school.sms.service;

import com.school.sms.dao.AuditLogDAO;
import com.school.sms.dao.FeeStructureDAO;
import com.school.sms.dao.GuardianDAO;
import com.school.sms.dao.PaymentDAO;
import com.school.sms.model.*;

import java.time.LocalDateTime;

/**
 * The heart of the fees module: record a payment, work out the new balance,
 * and notify the parent by SMS — all in one call, so a bursar recording a
 * payment can't forget the notification step.
 */
public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final FeeStructureDAO feeDAO = new FeeStructureDAO();
    private final GuardianDAO guardianDAO = new GuardianDAO();
    private final AuditLogDAO auditLog = new AuditLogDAO();
    private final SmsService smsService = new SmsService();

    public static class PaymentResult {
        public Payment payment;
        public double newBalance;
    }

    public PaymentResult recordPayment(Student student, String term, double amount,
                                        String notes, int recordedByUserId) {

        Payment payment = new Payment();
        payment.setReceiptNumber(paymentDAO.nextReceiptNumber());
        payment.setStudentId(student.getId());
        payment.setAmount(amount);
        payment.setTerm(term);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setRecordedByUserId(recordedByUserId);
        payment.setNotes(notes);

        int paymentDbId = paymentDAO.insert(payment);
        payment.setId(paymentDbId);

        FeeStructure fee = feeDAO.findByStudentAndTerm(student.getId(), term);
        double owed = (fee != null) ? fee.getAmountOwed() : 0.0;
        double totalPaid = feeDAO.totalPaid(student.getId(), term);
        double newBalance = owed - totalPaid;

        auditLog.log(recordedByUserId, "RECORD_PAYMENT",
                String.format("Receipt #%d: %s paid UGX %,.0f for %s. New balance: UGX %,.0f",
                        payment.getReceiptNumber(), student.getFullName(), amount, term, newBalance));

        // Notify parent — tries father first, falls back to mother/guardian.
        Guardian recipient = null;
        if (student.getFatherId() > 0) recipient = guardianDAO.findById(student.getFatherId());
        if (recipient == null && student.getMotherId() > 0) recipient = guardianDAO.findById(student.getMotherId());

        if (recipient != null && recipient.getContact() != null && !recipient.getContact().isBlank()) {
            String message = newBalance <= 0
                    ? SmsService.buildFullyClearedMessage(student.getFullName())
                    : SmsService.buildPaymentReceivedMessage(student.getFullName(), amount, newBalance);
            smsService.notify(recipient.getContact(), message);
        }

        PaymentResult result = new PaymentResult();
        result.payment = payment;
        result.newBalance = newBalance;
        return result;
    }
}
